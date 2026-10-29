package io.github.wallakscardoso.spilab.iso20022;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.random.RandomGenerator;

/** Identificadores do Pix no formato do catálogo do SPI. */
public final class Identificadores {

    private static final String ALFANUMERICOS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final DateTimeFormatter AAAAMMDDHHMM =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneOffset.UTC);
    private static final RandomGenerator ALEATORIO = new SecureRandom();

    private Identificadores() {
    }

    /** {@code E} + ISPB do pagador + data e hora UTC até o minuto + 11 caracteres = 32. */
    public static String endToEndId(String ispbPagador, Instant agora) {
        return "E" + ispbPagador + AAAAMMDDHHMM.format(agora) + aleatorio(11);
    }

    /** {@code M} + ISPB de quem envia a mensagem + 23 caracteres = 32. */
    public static String msgId(String ispbEmissor) {
        return "M" + ispbEmissor + aleatorio(23);
    }

    private static String aleatorio(int tamanho) {
        StringBuilder sb = new StringBuilder(tamanho);
        for (int i = 0; i < tamanho; i++) {
            sb.append(ALFANUMERICOS.charAt(ALEATORIO.nextInt(ALFANUMERICOS.length())));
        }
        return sb.toString();
    }
}
