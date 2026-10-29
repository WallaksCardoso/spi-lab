package io.github.wallakscardoso.spilab.spi;

public class ParticipanteNaoEncontradoException extends RuntimeException {

    public ParticipanteNaoEncontradoException(String ispb) {
        super("Participante não encontrado: " + ispb);
    }
}
