package io.github.wallakscardoso.spilab.psp;

import io.github.wallakscardoso.spilab.razao.Partida;
import io.github.wallakscardoso.spilab.razao.Razao;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/** Passos do PSP que mexem no banco, cada um numa transação curta, fora da espera pela rede. */
@Component
class PspRegistro {

    private final PixRepository pixes;
    private final Razao razao;
    private final Clock relogio;

    PspRegistro(PixRepository pixes, Razao razao, Clock relogio) {
        this.pixes = pixes;
        this.razao = razao;
        this.relogio = relogio;
    }

    /** Tira o valor da conta do cliente e reserva em trânsito. Sem saldo, nada é gravado. */
    @Transactional
    Pix reservarEnvio(String e2e, ContaCliente pagador, long valor, String chave, String ispbRecebedor) {
        if (valor > 0) {
            razao.lancar(Psp.livro(pagador.getIspb()), "Pix enviado " + e2e, List.of(
                    Partida.sai(Psp.contaCliente(pagador.getAgencia(), pagador.getNumero()), valor),
                    Partida.entra(Psp.CONTA_TRANSITO, valor)));
        }
        return pixes.save(new Pix(e2e, pagador.getIspb(), Pix.Direcao.ENVIADO, pagador.getAgencia(),
                pagador.getNumero(), valor, chave, ispbRecebedor, relogio.instant()));
    }

    /** Liquidado: o valor sai do trânsito para o SPI. Rejeitado: volta para o cliente. */
    @Transactional
    Pix concluirEnvio(String e2e, String ispb, boolean liquidado, String motivo) {
        Pix pix = pixes.findByEndToEndIdAndIspb(e2e, ispb).orElseThrow();
        String livro = Psp.livro(ispb);
        if (pix.getValorCentavos() > 0) {
            String destino = liquidado ? Psp.CONTA_SPI : Psp.contaCliente(pix.getAgencia(), pix.getNumero());
            razao.lancar(livro, (liquidado ? "Pix liquidado " : "Pix estornado ") + e2e, List.of(
                    Partida.sai(Psp.CONTA_TRANSITO, pix.getValorCentavos()),
                    Partida.entra(destino, pix.getValorCentavos())));
        }
        pix.concluir(liquidado ? Pix.Status.LIQUIDADO : Pix.Status.REJEITADO, motivo, relogio.instant());
        return pix;
    }

    @Transactional
    void registrarRecebido(String e2e, ContaCliente recebedor, long valor, String chave, String ispbPagador) {
        pixes.save(new Pix(e2e, recebedor.getIspb(), Pix.Direcao.RECEBIDO, recebedor.getAgencia(),
                recebedor.getNumero(), valor, chave, ispbPagador, relogio.instant()));
    }

    /** O SPI confirmou (ACCC): credita o cliente. Se rejeitou depois do aceite, só marca. */
    @Transactional
    void concluirRecebido(String e2e, String ispb, boolean liquidado, String motivo) {
        Pix pix = pixes.findByEndToEndIdAndIspb(e2e, ispb).orElseThrow();
        if (pix.getStatus() != Pix.Status.EM_ANDAMENTO) {
            return;
        }
        if (liquidado) {
            razao.lancar(Psp.livro(ispb), "Pix recebido " + e2e, List.of(
                    Partida.sai(Psp.CONTA_SPI, pix.getValorCentavos()),
                    Partida.entra(Psp.contaCliente(pix.getAgencia(), pix.getNumero()), pix.getValorCentavos())));
        }
        pix.concluir(liquidado ? Pix.Status.LIQUIDADO : Pix.Status.REJEITADO, motivo, relogio.instant());
    }
}
