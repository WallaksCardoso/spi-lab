package io.github.wallakscardoso.spilab.psp;

/** Nomes do livro de cada PSP e das suas contas no razão. */
public final class Psp {

    /** Contrapartida dos depósitos: o dinheiro que o cliente trouxe de fora do Pix. */
    public static final String CONTA_CAIXA = "CAIXA";

    /** Pix enviados que ainda esperam o SPI: o valor sai do cliente na hora e fica aqui reservado. */
    public static final String CONTA_TRANSITO = "PIX-TRANSITO";

    /** Contrapartida do que o PSP liquidou no SPI: sobe com o que enviou, desce com o que recebeu. */
    public static final String CONTA_SPI = "SPI";

    private Psp() {
    }

    public static String livro(String ispb) {
        return "PSP-" + ispb;
    }

    public static String contaCliente(String agencia, String numero) {
        return "CLI-" + agencia + "-" + numero;
    }
}
