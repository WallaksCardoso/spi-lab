package io.github.wallakscardoso.spilab.spi;

import io.github.wallakscardoso.spilab.config.EntidadeComIdAtribuido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "participante")
public class Participante extends EntidadeComIdAtribuido<String> {

    @Id
    private String ispb;

    @Column(nullable = false)
    private String nome;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Participante() {
    }

    Participante(String ispb, String nome, Instant criadoEm) {
        this.ispb = ispb;
        this.nome = nome;
        this.criadoEm = criadoEm;
    }

    @Override
    public String getId() {
        return ispb;
    }

    public String getIspb() {
        return ispb;
    }

    public String getNome() {
        return nome;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
