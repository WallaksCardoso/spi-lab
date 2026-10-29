package io.github.wallakscardoso.spilab.razao;

import io.github.wallakscardoso.spilab.config.EntidadeComIdAtribuido;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transacao")
public class Transacao extends EntidadeComIdAtribuido<UUID> {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String livro;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @OneToMany(mappedBy = "transacao", cascade = CascadeType.PERSIST)
    private List<Lancamento> lancamentos = new ArrayList<>();

    protected Transacao() {
    }

    Transacao(String livro, String descricao, Instant criadaEm) {
        this.id = UUID.randomUUID();
        this.livro = livro;
        this.descricao = descricao;
        this.criadaEm = criadaEm;
    }

    void adicionar(Conta conta, long valorCentavos) {
        lancamentos.add(new Lancamento(this, conta, valorCentavos));
    }

    public UUID getId() {
        return id;
    }

    public String getLivro() {
        return livro;
    }

    public String getDescricao() {
        return descricao;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public List<Lancamento> getLancamentos() {
        return Collections.unmodifiableList(lancamentos);
    }
}
