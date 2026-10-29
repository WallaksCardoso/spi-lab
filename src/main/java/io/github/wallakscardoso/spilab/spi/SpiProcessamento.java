package io.github.wallakscardoso.spilab.spi;

import io.github.wallakscardoso.spilab.iso20022.Cabecalho;
import io.github.wallakscardoso.spilab.iso20022.Identificadores;
import io.github.wallakscardoso.spilab.iso20022.Pacs002;
import io.github.wallakscardoso.spilab.iso20022.Pacs008;
import io.github.wallakscardoso.spilab.razao.SaldoInsuficienteException;
import io.github.wallakscardoso.spilab.rede.Rede;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * O caminho de uma ordem dentro do SPI: valida, repassa ao PSP recebedor, liquida nas contas PI
 * e avisa os dois lados com {@code pacs.002}. Na v0.1 tudo é síncrono: a resposta ao PSP pagador
 * só sai depois da liquidação.
 */
@Service
public class SpiProcessamento {

    private static final DateTimeFormatter AAAAMMDDHHMM = DateTimeFormatter.ofPattern("uuuuMMddHHmm");

    private final SpiRegistro registro;
    private final ParticipanteRepository participantes;
    private final Rede rede;
    private final Clock relogio;

    public SpiProcessamento(SpiRegistro registro, ParticipanteRepository participantes, Rede rede, Clock relogio) {
        this.registro = registro;
        this.participantes = participantes;
        this.rede = rede;
        this.relogio = relogio;
    }

    /** Recebe a pacs.008 do PSP pagador e devolve a pacs.002 final para ele. */
    public String receber(String xml) {
        Pacs008 ordem = Pacs008.deXml(xml);
        String e2e = ordem.endToEndId();
        String pagador = ordem.pagador().ispb();
        String recebedor = ordem.recebedor().ispb();

        String motivo = validar(ordem);
        if (!registro.abrir(e2e, pagador, recebedor, ordem.valorCentavos())) {
            throw new OrdemDuplicadaException(e2e);
        }
        if (motivo != null) {
            registro.rejeitar(e2e, motivo);
            return rejeicao(pagador, e2e, motivo);
        }

        Pacs008 repassada = new Pacs008(cabecalho(recebedor, Pacs008.TIPO), e2e, ordem.valorCentavos(),
                ordem.aceitaEm(), ordem.pagador(), ordem.recebedor(), ordem.descricao());
        Pacs002 resposta = Pacs002.deXml(rede.enviar("/psps/" + recebedor + "/spi/pacs008", repassada.paraXml()));

        if (resposta.status() == Pacs002.Status.RJCT) {
            registro.rejeitar(e2e, resposta.motivo());
            return rejeicao(pagador, e2e, resposta.motivo());
        }
        registro.aceitar(e2e);

        try {
            registro.liquidar(e2e);
        } catch (SaldoInsuficienteException e) {
            registro.rejeitar(e2e, "AM04");
            avisar(recebedor, Pacs002.rejeicao(cabecalho(recebedor, Pacs002.TIPO), e2e, "AM04", null));
            return rejeicao(pagador, e2e, "AM04");
        }

        avisar(recebedor, Pacs002.liquidacao(cabecalho(recebedor, Pacs002.TIPO), e2e, Pacs002.Status.ACCC, relogio.instant()));
        return Pacs002.liquidacao(cabecalho(pagador, Pacs002.TIPO), e2e, Pacs002.Status.ACSC, relogio.instant()).paraXml();
    }

    /** Validações do SPI antes de repassar. Devolve o código de rejeição do catálogo, ou null. */
    private String validar(Pacs008 ordem) {
        String pagador = ordem.pagador().ispb();
        if (!pagador.equals(ordem.cabecalho().de()) || !participantes.existsById(pagador)) {
            return "RC09";
        }
        if (!participantes.existsById(ordem.recebedor().ispb())) {
            return "RC10";
        }
        if (ordem.valorCentavos() == 0) {
            return "AM01";
        }
        if (!endToEndIdCoerente(ordem.endToEndId(), pagador)) {
            return "FF08";
        }
        return null;
    }

    /** O XSD só confere o desenho; aqui o ISPB tem que ser o do pagador e a data, uma data de verdade. */
    private static boolean endToEndIdCoerente(String e2e, String ispbPagador) {
        if (!e2e.substring(1, 9).equals(ispbPagador)) {
            return false;
        }
        try {
            LocalDateTime.parse(e2e.substring(9, 21), AAAAMMDDHHMM);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private void avisar(String ispb, Pacs002 aviso) {
        rede.enviar("/psps/" + ispb + "/spi/pacs002", aviso.paraXml());
    }

    private String rejeicao(String para, String e2e, String motivo) {
        return Pacs002.rejeicao(cabecalho(para, Pacs002.TIPO), e2e, motivo, null).paraXml();
    }

    private Cabecalho cabecalho(String para, String tipo) {
        return new Cabecalho(Spi.ISPB, para, Identificadores.msgId(Spi.ISPB), tipo, relogio.instant());
    }
}
