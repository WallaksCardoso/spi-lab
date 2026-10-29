package io.github.wallakscardoso.spilab.razao;

import io.github.wallakscardoso.spilab.config.EntidadeComIdAtribuido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

/**
 * Uma conta do razão. O saldo é a soma dos lançamentos da conta: valor positivo aumenta,
 * negativo diminui. Contas que representam dinheiro de alguém (conta PI, conta de cliente)
 * não podem ficar negativas; as contrapartidas externas (STR, caixa) podem.
 */
@Entity
@Table(name = "conta")
public class Conta extends EntidadeComIdAtribuido<UUID> {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String livro;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "permite_negativo", nullable = false)
    private boolean permiteNegativo;

    @Column(name = "saldo_centavos", nullable = false)
    private long saldoCentavos;

    @Version
    private long versao;

    protected Conta() {
    }

    Conta(String livro, String codigo, String descricao, boolean permiteNegativo) {
        this.id = UUID.randomUUID();
        this.livro = livro;
        this.codigo = codigo;
        this.descricao = descricao;
        this.permiteNegativo = permiteNegativo;
    }

    void movimentar(long valorCentavos) {
        long novoSaldo = Math.addExact(saldoCentavos, valorCentavos);
        if (novoSaldo < 0 && !permiteNegativo) {
            throw new SaldoInsuficienteException(livro, codigo, saldoCentavos, valorCentavos);
        }
        saldoCentavos = novoSaldo;
    }

    public UUID getId() {
        return id;
    }

    public String getLivro() {
        return livro;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isPermiteNegativo() {
        return permiteNegativo;
    }

    public long getSaldoCentavos() {
        return saldoCentavos;
    }
}
