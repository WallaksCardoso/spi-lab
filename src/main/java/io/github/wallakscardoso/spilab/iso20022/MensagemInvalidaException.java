package io.github.wallakscardoso.spilab.iso20022;

public class MensagemInvalidaException extends RuntimeException {

    public MensagemInvalidaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public MensagemInvalidaException(String mensagem) {
        super(mensagem);
    }
}
