package io.github.wallakscardoso.spilab.razao;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Razão em partidas dobradas. Cada livro (o SPI e cada PSP) tem suas contas; toda transação
 * movimenta duas ou mais contas do mesmo livro e a soma das partidas é sempre zero, então a
 * soma dos saldos de um livro também é sempre zero.
 */
@Service
public class Razao {

    private final ContaRepository contas;
    private final TransacaoRepository transacoes;
    private final Clock relogio;

    public Razao(ContaRepository contas, TransacaoRepository transacoes, Clock relogio) {
        this.contas = contas;
        this.transacoes = transacoes;
        this.relogio = relogio;
    }

    @Transactional
    public Conta abrirConta(String livro, String codigo, String descricao, boolean permiteNegativo) {
        if (contas.existsByLivroAndCodigo(livro, codigo)) {
            throw new LancamentoInvalidoException("Conta já existe: %s/%s".formatted(livro, codigo));
        }
        return contas.save(new Conta(livro, codigo, descricao, permiteNegativo));
    }

    @Transactional
    public Conta abrirContaSeNaoExistir(String livro, String codigo, String descricao, boolean permiteNegativo) {
        return contas.findByLivroAndCodigo(livro, codigo)
                .orElseGet(() -> contas.save(new Conta(livro, codigo, descricao, permiteNegativo)));
    }

    @Transactional
    public Transacao lancar(String livro, String descricao, List<Partida> partidas) {
        validar(partidas);
        Transacao transacao = new Transacao(livro, descricao, relogio.instant());
        for (Partida partida : partidas) {
            Conta conta = contas.findByLivroAndCodigo(livro, partida.codigoConta())
                    .orElseThrow(() -> new LancamentoInvalidoException(
                            "Conta inexistente: %s/%s".formatted(livro, partida.codigoConta())));
            conta.movimentar(partida.valorCentavos());
            transacao.adicionar(conta, partida.valorCentavos());
        }
        return transacoes.save(transacao);
    }

    @Transactional(readOnly = true)
    public long saldo(String livro, String codigoConta) {
        return contas.findByLivroAndCodigo(livro, codigoConta)
                .map(Conta::getSaldoCentavos)
                .orElseThrow(() -> new LancamentoInvalidoException(
                        "Conta inexistente: %s/%s".formatted(livro, codigoConta)));
    }

    /** Soma de todos os lançamentos do livro: tem que ser zero. */
    @Transactional(readOnly = true)
    public long somaDoLivro(String livro) {
        return transacoes.somaDosLancamentos(livro);
    }

    private static void validar(List<Partida> partidas) {
        if (partidas.size() < 2) {
            throw new LancamentoInvalidoException("Uma transação precisa de pelo menos duas partidas");
        }
        long soma = 0;
        Set<String> contasUsadas = new HashSet<>();
        for (Partida partida : partidas) {
            if (partida.valorCentavos() == 0) {
                throw new LancamentoInvalidoException("Partida com valor zero na conta " + partida.codigoConta());
            }
            if (!contasUsadas.add(partida.codigoConta())) {
                throw new LancamentoInvalidoException("Conta repetida na transação: " + partida.codigoConta());
            }
            soma = Math.addExact(soma, partida.valorCentavos());
        }
        if (soma != 0) {
            throw new LancamentoInvalidoException("As partidas não fecham: soma = " + soma + " centavos");
        }
    }
}
