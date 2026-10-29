package io.github.wallakscardoso.spilab.rede;

import io.github.wallakscardoso.spilab.config.EntidadeComIdAtribuido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Cópia de cada XML que passou pela rede, para estudo e conferência. */
@Entity
@Table(name = "mensagem")
public class Mensagem extends EntidadeComIdAtribuido<UUID> {

    @Id
    private UUID id;

    @Column(name = "end_to_end_id")
    private String endToEndId;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private String de;

    @Column(nullable = false)
    private String para;

    private String status;

    @Column(nullable = false)
    private String xml;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    protected Mensagem() {
    }

    Mensagem(String endToEndId, String tipo, String de, String para, String status, String xml, Instant criadaEm) {
        this.id = UUID.randomUUID();
        this.endToEndId = endToEndId;
        this.tipo = tipo;
        this.de = de;
        this.para = para;
        this.status = status;
        this.xml = xml;
        this.criadaEm = criadaEm;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public String getEndToEndId() {
        return endToEndId;
    }

    public String getTipo() {
        return tipo;
    }

    public String getDe() {
        return de;
    }

    public String getPara() {
        return para;
    }

    public String getStatus() {
        return status;
    }

    public String getXml() {
        return xml;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }
}
