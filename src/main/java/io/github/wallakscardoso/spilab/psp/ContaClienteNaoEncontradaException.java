package io.github.wallakscardoso.spilab.psp;

public class ContaClienteNaoEncontradaException extends RuntimeException {

    public ContaClienteNaoEncontradaException(String ispb, String agencia, String numero) {
        super("Conta não encontrada: ISPB %s, agência %s, conta %s".formatted(ispb, agencia, numero));
    }
}
