package io.github.wallakscardoso.spilab.razao;

/** Uma linha de uma transação: quanto entra (positivo) ou sai (negativo) de uma conta do livro. */
public record Partida(String codigoConta, long valorCentavos) {

    public static Partida entra(String codigoConta, long valorCentavos) {
        return new Partida(codigoConta, valorCentavos);
    }

    public static Partida sai(String codigoConta, long valorCentavos) {
        return new Partida(codigoConta, Math.negateExact(valorCentavos));
    }
}
