package io.github.wallakscardoso.spilab.psp;

/** Nomes do livro de cada PSP e das suas contas no razão. */
public final class Psp {

    /** Contrapartida dos depósitos: o dinheiro que o cliente trouxe de fora do Pix. */
    public static final String CONTA_CAIXA = "CAIXA";

    private Psp() {
    }

    public static String livro(String ispb) {
        return "PSP-" + ispb;
    }

    public static String contaCliente(String agencia, String numero) {
        return "CLI-" + agencia + "-" + numero;
    }
}
