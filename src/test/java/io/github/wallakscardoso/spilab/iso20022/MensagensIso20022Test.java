package io.github.wallakscardoso.spilab.iso20022;

import io.github.wallakscardoso.spilab.iso20022.Pacs002.Status;
import io.github.wallakscardoso.spilab.iso20022.Pacs008.Pagador;
import io.github.wallakscardoso.spilab.iso20022.Pacs008.Recebedor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MensagensIso20022Test {

    static final String ISPB_SPI = "00038166";
    static final Instant AGORA = Instant.parse("2026-11-10T14:30:12.345Z");

    static String exemplo(String nome) throws IOException {
        try (InputStream in = MensagensIso20022Test.class.getResourceAsStream("/bcb/exemplos/" + nome)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static Pacs008 ordem(String endToEndId, long centavos) {
        return new Pacs008(
                new Cabecalho("11111111", ISPB_SPI, Identificadores.msgId("11111111"), Pacs008.TIPO, AGORA),
                endToEndId, centavos, AGORA,
                new Pagador("João Souza", "11122233344", "11111111", "0001", "1001", "CACC"),
                new Recebedor("12ABC34501DE35", "22222222", "0001", "2002", "SVGS", "maria@exemplo.com.br"),
                "Almoço de sexta");
    }

    @Test
    void leOExemploOficialDaPacs008() throws IOException {
        Pacs008 lida = Pacs008.deXml(exemplo("pacs.008_END_1_msg.xml"));

        assertThat(lida.endToEndId()).isEqualTo("E9999901012341234123412345678900");
        assertThat(lida.valorCentavos()).isEqualTo(100_000);
        assertThat(lida.pagador().nome()).isEqualTo("Fulano da Silva");
        assertThat(lida.pagador().ispb()).isEqualTo("10000000");
        assertThat(lida.recebedor().ispb()).isEqualTo("20000000");
        assertThat(lida.recebedor().chave()).isEqualTo("idContaTransacional0@bc.com.br");
        assertThat(lida.cabecalho().tipoMensagem()).isEqualTo("pacs.008.spi.1.16");
    }

    @ParameterizedTest
    @ValueSource(strings = {"pacs.002_PSP_1_msg.xml", "pacs.002_SPI_1_msg.xml", "pacs.002_ERRO_1_msg.xml"})
    void leOsExemplosOficiaisDaPacs002(String arquivo) throws IOException {
        Pacs002 lida = Pacs002.deXml(exemplo(arquivo));
        assertThat(lida.endToEndId()).isEqualTo("E9999901012341234123412345678900");
    }

    @Test
    void exemploOficialDeErroTrazStatusEMotivo() throws IOException {
        Pacs002 erro = Pacs002.deXml(exemplo("pacs.002_ERRO_1_msg.xml"));
        assertThat(erro.status()).isEqualTo(Status.RJCT);
        assertThat(erro.motivo()).isEqualTo("AB03");
    }

    @Test
    void pacs008GeradaPassaNoXsdEVoltaIgual() {
        Pacs008 original = ordem(Identificadores.endToEndId("11111111", AGORA), 10_000);

        String xml = original.paraXml();

        assertThat(xml).contains("<IntrBkSttlmAmt Ccy=\"BRL\">100.00</IntrBkSttlmAmt>");
        assertThat(Pacs008.deXml(xml)).isEqualTo(original);
    }

    @Test
    void valoresPequenosEGrandesNaoPerdemCentavo() {
        for (long centavos : new long[]{1, 99, 100, 123_456_789}) {
            String xml = ordem(Identificadores.endToEndId("11111111", AGORA), centavos).paraXml();
            assertThat(Pacs008.deXml(xml).valorCentavos()).isEqualTo(centavos);
        }
    }

    @Test
    void pacs002GeradasPassamNoXsd() {
        String e2e = Identificadores.endToEndId("11111111", AGORA);
        Cabecalho doRecebedor = new Cabecalho("22222222", ISPB_SPI, Identificadores.msgId("22222222"), Pacs002.TIPO, AGORA);
        Cabecalho doSpi = new Cabecalho(ISPB_SPI, "11111111", Identificadores.msgId(ISPB_SPI), Pacs002.TIPO, AGORA);

        Pacs002 aceite = Pacs002.deXml(Pacs002.aceite(doRecebedor, e2e).paraXml());
        Pacs002 rejeicao = Pacs002.deXml(Pacs002.rejeicao(doRecebedor, e2e, "AC03", "Conta inexistente").paraXml());
        Pacs002 liquidada = Pacs002.deXml(Pacs002.liquidacao(doSpi, e2e, Status.ACSC, AGORA).paraXml());

        assertThat(aceite.status()).isEqualTo(Status.ACSP);
        assertThat(rejeicao.status()).isEqualTo(Status.RJCT);
        assertThat(rejeicao.motivo()).isEqualTo("AC03");
        assertThat(rejeicao.informacaoAdicional()).isEqualTo("Conta inexistente");
        assertThat(liquidada.status()).isEqualTo(Status.ACSC);
        assertThat(liquidada.liquidadaEm()).isEqualTo(Instant.parse("2026-11-10T14:30:12.345Z"));
    }

    @Test
    void identificadoresTemOFormatoDoCatalogo() {
        String e2e = Identificadores.endToEndId("11111111", AGORA);
        assertThat(e2e).hasSize(32).startsWith("E11111111202611101430")
                .matches("[E][0-9A-Z]{8}[0-9]{4}[0-1][0-9][0-3][0-9][0-2][0-9][0-5][0-9][a-zA-Z0-9]{11}");
        assertThat(Identificadores.msgId("11111111")).hasSize(32).matches("[M][0-9A-Z]{8}[a-zA-Z0-9]{23}");
    }

    @Test
    void endToEndIdMalFormadoNaoPassa() {
        assertThatThrownBy(() -> ordem("E1111111120261110143", 100).paraXml())
                .isInstanceOf(MensagemInvalidaException.class)
                .hasMessageContaining("XSD");
    }

    @Test
    void motivoForaDoCatalogoNaoPassa() {
        Cabecalho c = new Cabecalho("22222222", ISPB_SPI, Identificadores.msgId("22222222"), Pacs002.TIPO, AGORA);
        assertThatThrownBy(() -> Pacs002.rejeicao(c, Identificadores.endToEndId("11111111", AGORA), "XX99", null).paraXml())
                .isInstanceOf(MensagemInvalidaException.class);
    }

    @Test
    void valorComTresCasasDecimaisNaoPassa() throws IOException {
        String adulterado = exemplo("pacs.008_END_1_msg.xml").replace(">1000.00<", ">1000.001<");
        assertThatThrownBy(() -> Pacs008.deXml(adulterado)).isInstanceOf(MensagemInvalidaException.class);
    }

    @Test
    void rejeicaoSemMotivoNemChegaAVirarXml() {
        Cabecalho c = new Cabecalho("22222222", ISPB_SPI, Identificadores.msgId("22222222"), Pacs002.TIPO, AGORA);
        assertThatThrownBy(() -> Pacs002.rejeicao(c, "E11111111202611101430abcdefghijk", " ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
