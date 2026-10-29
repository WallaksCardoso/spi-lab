package io.github.wallakscardoso.spilab.spi;

/** Mesma EndToEndId enviada de novo. O tratamento de reenvio (idempotência) fica para a v0.2. */
public class OrdemDuplicadaException extends RuntimeException {

    public OrdemDuplicadaException(String endToEndId) {
        super("Ordem já recebida: " + endToEndId);
    }
}
