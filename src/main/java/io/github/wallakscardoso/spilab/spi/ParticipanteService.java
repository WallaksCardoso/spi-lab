package io.github.wallakscardoso.spilab.spi;

import io.github.wallakscardoso.spilab.razao.LancamentoInvalidoException;
import io.github.wallakscardoso.spilab.razao.Partida;
import io.github.wallakscardoso.spilab.razao.Razao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class ParticipanteService {

    private final ParticipanteRepository participantes;
    private final Razao razao;
    private final Clock relogio;

    public ParticipanteService(ParticipanteRepository participantes, Razao razao, Clock relogio) {
        this.participantes = participantes;
        this.razao = razao;
        this.relogio = relogio;
    }

    /** Cadastra o participante, abre a conta PI dele no SPI e faz o aporte inicial vindo do STR. */
    @Transactional
    public Participante cadastrar(String ispb, String nome, long aporteInicialCentavos) {
        if (participantes.existsById(ispb)) {
            throw new LancamentoInvalidoException("Participante já cadastrado: " + ispb);
        }
        Participante participante = participantes.save(new Participante(ispb, nome, relogio.instant()));
        razao.abrirContaSeNaoExistir(Spi.LIVRO, Spi.CONTA_STR, "Reservas bancárias (origem dos aportes)", true);
        razao.abrirConta(Spi.LIVRO, Spi.contaPi(ispb), "Conta PI de " + nome, false);
        if (aporteInicialCentavos > 0) {
            aportar(ispb, aporteInicialCentavos);
        }
        return participante;
    }

    /** Transfere recursos do STR para a conta PI do participante. */
    @Transactional
    public void aportar(String ispb, long valorCentavos) {
        exigir(ispb);
        razao.lancar(Spi.LIVRO, "Aporte na conta PI de " + ispb, List.of(
                Partida.entra(Spi.contaPi(ispb), valorCentavos),
                Partida.sai(Spi.CONTA_STR, valorCentavos)));
    }

    @Transactional(readOnly = true)
    public long saldoPi(String ispb) {
        exigir(ispb);
        return razao.saldo(Spi.LIVRO, Spi.contaPi(ispb));
    }

    @Transactional(readOnly = true)
    public Participante exigir(String ispb) {
        return participantes.findById(ispb).orElseThrow(() -> new ParticipanteNaoEncontradoException(ispb));
    }
}
