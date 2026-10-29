package io.github.wallakscardoso.spilab.spi;

/** Nomes do livro do SPI e das suas contas no razão. */
public final class Spi {

    public static final String LIVRO = "SPI";

    /** Contrapartida dos aportes: o dinheiro vem das reservas bancárias (STR). */
    public static final String CONTA_STR = "STR";

    private Spi() {
    }

    public static String contaPi(String ispb) {
        return "PI-" + ispb;
    }
}
