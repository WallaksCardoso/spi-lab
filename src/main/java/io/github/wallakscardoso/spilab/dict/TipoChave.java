package io.github.wallakscardoso.spilab.dict;

import java.util.regex.Pattern;

/** Tipos e formatos de chave do Manual Operacional do DICT (versão 8.5). */
public enum TipoChave {

    CPF("\\d{11}"),
    CNPJ("[0-9A-Z]{12}\\d{2}"),
    EMAIL("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+"),
    TELEFONE("\\+[1-9]\\d{1,14}"),
    EVP("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    static final int TAMANHO_MAXIMO_EMAIL = 77;

    private final Pattern formato;

    TipoChave(String regex) {
        this.formato = Pattern.compile(regex);
    }

    boolean aceita(String chave) {
        if (chave == null || !formato.matcher(chave).matches()) {
            return false;
        }
        return this != EMAIL || chave.length() <= TAMANHO_MAXIMO_EMAIL;
    }
}
