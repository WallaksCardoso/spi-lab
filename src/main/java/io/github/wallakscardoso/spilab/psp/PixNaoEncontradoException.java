package io.github.wallakscardoso.spilab.psp;

public class PixNaoEncontradoException extends RuntimeException {

    public PixNaoEncontradoException(String ispb, String endToEndId) {
        super("Pix não encontrado no PSP %s: %s".formatted(ispb, endToEndId));
    }
}
