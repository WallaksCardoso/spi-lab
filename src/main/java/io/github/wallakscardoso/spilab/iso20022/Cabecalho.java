package io.github.wallakscardoso.spilab.iso20022;

import org.w3c.dom.Element;

import java.time.Instant;

/** Cabeçalho de negócio ({@code AppHdr}) que vai em todo envelope do SPI. */
public record Cabecalho(String de, String para, String msgId, String tipoMensagem, Instant criadaEm) {

    void escrever(Xml xml, Element envelope) {
        Element hdr = xml.filho(envelope, "AppHdr");
        xml.caminho(hdr, "Fr/FIId/FinInstnId/Othr/Id", de);
        xml.caminho(hdr, "To/FIId/FinInstnId/Othr/Id", para);
        xml.filho(hdr, "BizMsgIdr", msgId);
        xml.filho(hdr, "MsgDefIdr", tipoMensagem);
        xml.filho(hdr, "CreDt", Xml.dataHora(criadaEm));
        xml.filho(hdr, "Sgntr");
    }

    static Cabecalho ler(Element envelope) {
        Element hdr = Xml.primeiroFilho(envelope, "AppHdr");
        if (hdr == null) {
            throw new MensagemInvalidaException("Envelope sem AppHdr");
        }
        return new Cabecalho(
                Xml.obrigatorio(hdr, "Fr/FIId/FinInstnId/Othr/Id"),
                Xml.obrigatorio(hdr, "To/FIId/FinInstnId/Othr/Id"),
                Xml.obrigatorio(hdr, "BizMsgIdr"),
                Xml.obrigatorio(hdr, "MsgDefIdr"),
                Xml.dataHora(Xml.obrigatorio(hdr, "CreDt")));
    }
}
