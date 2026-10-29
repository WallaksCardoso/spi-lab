package io.github.wallakscardoso.spilab.iso20022;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.time.Instant;

/**
 * Ordem de pagamento instantâneo ({@code pacs.008.spi.1.16}), com o subconjunto de campos da v0.1:
 * um Pix por chave, de conta para conta.
 */
public record Pacs008(Cabecalho cabecalho, String endToEndId, long valorCentavos, Instant aceitaEm,
                      Pagador pagador, Recebedor recebedor, String descricao) {

    public static final String NAMESPACE = "https://www.bcb.gov.br/pi/pacs.008/1.16";
    public static final String TIPO = "pacs.008.spi.1.16";

    public record Pagador(String nome, String documento, String ispb, String agencia, String conta, String tipoConta) {
    }

    public record Recebedor(String documento, String ispb, String agencia, String conta, String tipoConta, String chave) {
    }

    /** Monta o XML e confere contra o XSD antes de devolver. */
    public String paraXml() {
        Xml xml = Xml.novo(NAMESPACE);
        Element envelope = xml.raiz("Envelope");
        cabecalho.escrever(xml, envelope);

        Element doc = xml.filho(xml.filho(envelope, "Document"), "FIToFICstmrCdtTrf");
        Element grp = xml.filho(doc, "GrpHdr");
        xml.filho(grp, "MsgId", cabecalho.msgId());
        xml.filho(grp, "CreDtTm", Xml.dataHora(cabecalho.criadaEm()));
        xml.filho(grp, "NbOfTxs", "1");
        xml.caminho(grp, "SttlmInf/SttlmMtd", "CLRG");
        Element pmtTp = xml.filho(grp, "PmtTpInf");
        xml.filho(pmtTp, "InstrPrty", "HIGH");
        xml.caminho(pmtTp, "SvcLvl/Prtry", "PAGPRI");

        Element tx = xml.filho(doc, "CdtTrfTxInf");
        xml.caminho(tx, "PmtId/EndToEndId", endToEndId);
        xml.filho(tx, "IntrBkSttlmAmt", Xml.valorEmReais(valorCentavos)).setAttribute("Ccy", "BRL");
        xml.filho(tx, "AccptncDtTm", Xml.dataHora(aceitaEm));
        xml.filho(tx, "ChrgBr", "SLEV");
        xml.caminho(tx, "MndtRltdInf/Tp/LclInstrm/Prtry", "DICT");

        Element dbtr = xml.filho(tx, "Dbtr");
        xml.filho(dbtr, "Nm", pagador.nome());
        xml.caminho(dbtr, "Id/PrvtId/Othr/Id", pagador.documento());
        conta(xml, xml.filho(tx, "DbtrAcct"), pagador.conta(), pagador.agencia(), pagador.tipoConta());
        xml.caminho(tx, "DbtrAgt/FinInstnId/ClrSysMmbId/MmbId", pagador.ispb());

        xml.caminho(tx, "CdtrAgt/FinInstnId/ClrSysMmbId/MmbId", recebedor.ispb());
        xml.caminho(xml.filho(tx, "Cdtr"), "Id/PrvtId/Othr/Id", recebedor.documento());
        Element cdtrAcct = xml.filho(tx, "CdtrAcct");
        conta(xml, cdtrAcct, recebedor.conta(), recebedor.agencia(), recebedor.tipoConta());
        xml.caminho(cdtrAcct, "Prxy/Id", recebedor.chave());

        xml.caminho(tx, "Purp/Cd", "IPAY");
        if (descricao != null && !descricao.isBlank()) {
            xml.caminho(tx, "RmtInf/Ustrd", descricao);
        }

        ValidadorXsd.validar(xml.documento());
        return Xml.texto(xml.documento());
    }

    /** Lê e valida contra o XSD; qualquer desvio vira {@link MensagemInvalidaException}. */
    public static Pacs008 deXml(String texto) {
        Document doc = Xml.ler(texto);
        ValidadorXsd.validar(doc);
        Element envelope = doc.getDocumentElement();
        Element tx = Xml.primeiroFilho(
                Xml.primeiroFilho(Xml.primeiroFilho(envelope, "Document"), "FIToFICstmrCdtTrf"), "CdtTrfTxInf");
        return new Pacs008(
                Cabecalho.ler(envelope),
                Xml.obrigatorio(tx, "PmtId/EndToEndId"),
                Xml.centavos(Xml.obrigatorio(tx, "IntrBkSttlmAmt")),
                Xml.dataHora(Xml.obrigatorio(tx, "AccptncDtTm")),
                new Pagador(
                        Xml.obrigatorio(tx, "Dbtr/Nm"),
                        Xml.obrigatorio(tx, "Dbtr/Id/PrvtId/Othr/Id"),
                        Xml.obrigatorio(tx, "DbtrAgt/FinInstnId/ClrSysMmbId/MmbId"),
                        Xml.valor(tx, "DbtrAcct/Id/Othr/Issr"),
                        Xml.obrigatorio(tx, "DbtrAcct/Id/Othr/Id"),
                        Xml.obrigatorio(tx, "DbtrAcct/Tp/Cd")),
                new Recebedor(
                        Xml.obrigatorio(tx, "Cdtr/Id/PrvtId/Othr/Id"),
                        Xml.obrigatorio(tx, "CdtrAgt/FinInstnId/ClrSysMmbId/MmbId"),
                        Xml.valor(tx, "CdtrAcct/Id/Othr/Issr"),
                        Xml.obrigatorio(tx, "CdtrAcct/Id/Othr/Id"),
                        Xml.obrigatorio(tx, "CdtrAcct/Tp/Cd"),
                        Xml.valor(tx, "CdtrAcct/Prxy/Id")),
                Xml.valor(tx, "RmtInf/Ustrd"));
    }

    private static void conta(Xml xml, Element acct, String numero, String agencia, String tipo) {
        Element othr = xml.filho(xml.filho(acct, "Id"), "Othr");
        xml.filho(othr, "Id", numero);
        xml.filho(othr, "Issr", agencia);
        xml.caminho(acct, "Tp/Cd", tipo);
    }
}
