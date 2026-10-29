package io.github.wallakscardoso.spilab.razao;

public class SaldoInsuficienteException extends RuntimeException {

    private final String livro;
    private final String codigoConta;

    public SaldoInsuficienteException(String livro, String codigoConta, long saldoCentavos, long valorCentavos) {
        super("Saldo insuficiente na conta %s/%s: saldo %d, movimento %d (centavos)"
                .formatted(livro, codigoConta, saldoCentavos, valorCentavos));
        this.livro = livro;
        this.codigoConta = codigoConta;
    }

    public String getLivro() {
        return livro;
    }

    public String getCodigoConta() {
        return codigoConta;
    }
}
