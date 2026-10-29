package io.github.wallakscardoso.spilab.dict;

public class ChaveNaoEncontradaException extends RuntimeException {

    public ChaveNaoEncontradaException(String chave) {
        super("Chave Pix não encontrada: " + chave);
    }
}
