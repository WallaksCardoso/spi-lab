package io.github.wallakscardoso.spilab.razao;

import io.github.wallakscardoso.spilab.config.EntidadeComIdAtribuido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "lancamento")
public class Lancamento extends EntidadeComIdAtribuido<UUID> {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transacao_id")
    private Transacao transacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_id")
    private Conta conta;

    @Column(name = "valor_centavos", nullable = false)
    private long valorCentavos;

    protected Lancamento() {
    }

    Lancamento(Transacao transacao, Conta conta, long valorCentavos) {
        this.id = UUID.randomUUID();
        this.transacao = transacao;
        this.conta = conta;
        this.valorCentavos = valorCentavos;
    }

    public UUID getId() {
        return id;
    }

    public Conta getConta() {
        return conta;
    }

    public long getValorCentavos() {
        return valorCentavos;
    }
}
