package io.github.wallakscardoso.spilab.iso20022;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.time.Instant;
import java.time.ZoneId;

/** Relatório de status de uma ordem ({@code pacs.002.spi.1.18}). */
public record Pacs002(Cabecalho cabecalho, String endToEndId, Status status, String motivo,
                      String informacaoAdicional, Instant liquidadaEm) {

    public static final String NAMESPACE = "https://www.bcb.gov.br/pi/pacs.002/1.18";
    public static final String TIPO = "pacs.002.spi.1.18";

    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    /** Status da tabela de domínios do catálogo 5.13. */
    public enum Status {
        /** O PSP recebedor aceitou a ordem. */
        ACSP,
        /** Rejeitada pelo SPI ou pelo PSP recebedor; vem com o código do motivo. */
        RJCT,
        /** O SPI avisa o PSP pagador que liquidou. */
        ACSC,
        /** O SPI avisa o PSP recebedor que liquidou. */
        ACCC;

        boolean liquidada() {
            return this == ACSC || this == ACCC;
        }
    }

    public Pacs002 {
        if (status == Status.RJCT && (motivo == null || motivo.isBlank())) {
            throw new IllegalArgumentException("Rejeição precisa do código do motivo");
        }
        if (status.liquidada() && liquidadaEm == null) {
            throw new IllegalArgumentException("Confirmação de liquidação precisa da data e hora");
        }
    }

    public static Pacs002 aceite(Cabecalho cabecalho, String endToEndId) {
        return new Pacs002(cabecalho, endToEndId, Status.ACSP, null, null, null);
    }

    public static Pacs002 rejeicao(Cabecalho cabecalho, String endToEndId, String motivo, String informacao) {
        return new Pacs002(cabecalho, endToEndId, Status.RJCT, motivo, informacao, null);
    }

    public static Pacs002 liquidacao(Cabecalho cabecalho, String endToEndId, Status status, Instant liquidadaEm) {
        return new Pacs002(cabecalho, endToEndId, status, null, null, liquidadaEm);
    }

    public String paraXml() {
        Xml xml = Xml.novo(NAMESPACE);
        Element envelope = xml.raiz("Envelope");
        cabecalho.escrever(xml, envelope);

        Element rpt = xml.filho(xml.filho(envelope, "Document"), "FIToFIPmtStsRpt");
        Element grp = xml.filho(rpt, "GrpHdr");
        xml.filho(grp, "MsgId", cabecalho.msgId());
        xml.filho(grp, "CreDtTm", Xml.dataHora(cabecalho.criadaEm()));

        Element tx = xml.filho(rpt, "TxInfAndSts");
        xml.filho(tx, "OrgnlInstrId", endToEndId);
        xml.filho(tx, "OrgnlEndToEndId", endToEndId);
        xml.filho(tx, "TxSts", status.name());
        if (status == Status.RJCT) {
            Element sts = xml.filho(tx, "StsRsnInf");
            xml.caminho(sts, "Rsn/Cd", motivo);
            if (informacaoAdicional != null && !informacaoAdicional.isBlank()) {
                xml.filho(sts, "AddtlInf", informacaoAdicional);
            }
        }
        if (status.liquidada()) {
            xml.caminho(tx, "FctvIntrBkSttlmDt/DtTm", Xml.dataHora(liquidadaEm));
            xml.caminho(tx, "OrgnlTxRef/IntrBkSttlmDt", liquidadaEm.atZone(BRASILIA).toLocalDate().toString());
        }

        ValidadorXsd.validar(xml.documento());
        return Xml.texto(xml.documento());
    }

    public static Pacs002 deXml(String texto) {
        Document doc = Xml.ler(texto);
        ValidadorXsd.validar(doc);
        Element envelope = doc.getDocumentElement();
        Element tx = Xml.primeiroFilho(
                Xml.primeiroFilho(Xml.primeiroFilho(envelope, "Document"), "FIToFIPmtStsRpt"), "TxInfAndSts");
        String liquidada = Xml.valor(tx, "FctvIntrBkSttlmDt/DtTm");
        return new Pacs002(
                Cabecalho.ler(envelope),
                Xml.obrigatorio(tx, "OrgnlEndToEndId"),
                Status.valueOf(Xml.obrigatorio(tx, "TxSts")),
                Xml.valor(tx, "StsRsnInf/Rsn/Cd"),
                Xml.valor(tx, "StsRsnInf/AddtlInf"),
                liquidada == null ? null : Xml.dataHora(liquidada));
    }
}
