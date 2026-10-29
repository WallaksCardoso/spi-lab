package io.github.wallakscardoso.spilab.spi;

import io.github.wallakscardoso.spilab.razao.Partida;
import io.github.wallakscardoso.spilab.razao.Razao;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Passos do SPI que mexem no banco de dados, cada um na sua transação curta: nenhuma transação
 * fica aberta enquanto o SPI espera a resposta de um participante.
 */
@Component
class SpiRegistro {

    private final OrdemRepository ordens;
    private final Razao razao;
    private final Clock relogio;

    SpiRegistro(OrdemRepository ordens, Razao razao, Clock relogio) {
        this.ordens = ordens;
        this.razao = razao;
        this.relogio = relogio;
    }

    @Transactional
    boolean abrir(String endToEndId, String ispbPagador, String ispbRecebedor, long valorCentavos) {
        if (ordens.existsById(endToEndId)) {
            return false;
        }
        ordens.save(new Ordem(endToEndId, ispbPagador, ispbRecebedor, valorCentavos, relogio.instant()));
        return true;
    }

    @Transactional
    void rejeitar(String endToEndId, String motivo) {
        ordens.findById(endToEndId).ifPresent(o -> o.mudar(Ordem.Status.REJEITADA, motivo, relogio.instant()));
    }

    @Transactional
    void aceitar(String endToEndId) {
        ordens.findById(endToEndId).ifPresent(o -> o.mudar(Ordem.Status.ACEITA, null, relogio.instant()));
    }

    /**
     * Move o valor da conta PI do pagador para a do recebedor. Sem saldo, lança
     * {@link SaldoInsuficienteException} e a transação inteira é desfeita.
     */
    @Transactional
    void liquidar(String endToEndId) {
        Ordem ordem = ordens.findById(endToEndId).orElseThrow();
        razao.lancar(Spi.LIVRO, "Pix " + endToEndId, List.of(
                Partida.sai(Spi.contaPi(ordem.getIspbPagador()), ordem.getValorCentavos()),
                Partida.entra(Spi.contaPi(ordem.getIspbRecebedor()), ordem.getValorCentavos())));
        ordem.mudar(Ordem.Status.LIQUIDADA, null, relogio.instant());
    }
}
