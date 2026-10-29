package io.github.wallakscardoso.spilab.razao;

import io.github.wallakscardoso.spilab.PostgresDeTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(PostgresDeTeste.class)
class RazaoTest {

    @Autowired
    Razao razao;

    String livro;

    @BeforeEach
    void livroNovo() {
        livro = "T-" + UUID.randomUUID().toString().substring(0, 8);
        razao.abrirConta(livro, "ORIGEM", "Contrapartida", true);
        razao.abrirConta(livro, "A", "Conta A", false);
        razao.abrirConta(livro, "B", "Conta B", false);
    }

    @Test
    void transacaoMovimentaAsContasEOLivroFechaEmZero() {
        razao.lancar(livro, "aporte", List.of(Partida.entra("A", 10_000), Partida.sai("ORIGEM", 10_000)));
        razao.lancar(livro, "transferência", List.of(Partida.sai("A", 3_333), Partida.entra("B", 3_333)));

        assertThat(razao.saldo(livro, "A")).isEqualTo(6_667);
        assertThat(razao.saldo(livro, "B")).isEqualTo(3_333);
        assertThat(razao.saldo(livro, "ORIGEM")).isEqualTo(-10_000);
        assertThat(razao.somaDoLivro(livro)).isZero();
    }

    @Test
    void recusaPartidasQueNaoFecham() {
        assertThatThrownBy(() -> razao.lancar(livro, "errado",
                List.of(Partida.entra("A", 100), Partida.sai("ORIGEM", 99))))
                .isInstanceOf(LancamentoInvalidoException.class)
                .hasMessageContaining("não fecham");
        assertThat(razao.saldo(livro, "A")).isZero();
    }

    @Test
    void recusaTransacaoComUmaPartidaSoOuValorZero() {
        assertThatThrownBy(() -> razao.lancar(livro, "sozinha", List.of(Partida.entra("A", 100))))
                .isInstanceOf(LancamentoInvalidoException.class);
        assertThatThrownBy(() -> razao.lancar(livro, "zero",
                List.of(Partida.entra("A", 0), Partida.sai("ORIGEM", 0))))
                .isInstanceOf(LancamentoInvalidoException.class);
    }

    @Test
    void saldoInsuficienteNaoDeixaNadaPelaMetade() {
        razao.lancar(livro, "aporte", List.of(Partida.entra("A", 500), Partida.sai("ORIGEM", 500)));

        assertThatThrownBy(() -> razao.lancar(livro, "acima do saldo",
                List.of(Partida.entra("B", 501), Partida.sai("A", 501))))
                .isInstanceOf(SaldoInsuficienteException.class);

        assertThat(razao.saldo(livro, "A")).isEqualTo(500);
        assertThat(razao.saldo(livro, "B")).isZero();
        assertThat(razao.somaDoLivro(livro)).isZero();
    }

    @Test
    void recusaContaDeOutroLivroOuInexistente() {
        assertThatThrownBy(() -> razao.lancar(livro, "conta fantasma",
                List.of(Partida.entra("A", 100), Partida.sai("NAO-EXISTE", 100))))
                .isInstanceOf(LancamentoInvalidoException.class)
                .hasMessageContaining("inexistente");
        assertThat(razao.saldo(livro, "A")).isZero();
    }
}
