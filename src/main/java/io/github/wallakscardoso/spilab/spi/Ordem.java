package io.github.wallakscardoso.spilab.spi;

import io.github.wallakscardoso.spilab.config.EntidadeComIdAtribuido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Uma ordem de pagamento como o SPI a acompanha. */
@Entity
@Table(name = "ordem")
public class Ordem extends EntidadeComIdAtribuido<String> {

    public enum Status { ENVIADA_RECEBEDOR, ACEITA, LIQUIDADA, REJEITADA }

    @Id
    @Column(name = "end_to_end_id")
    private String endToEndId;

    @Column(name = "ispb_pagador", nullable = false)
    private String ispbPagador;

    @Column(name = "ispb_recebedor", nullable = false)
    private String ispbRecebedor;

    @Column(name = "valor_centavos", nullable = false)
    private long valorCentavos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private String motivo;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    protected Ordem() {
    }

    Ordem(String endToEndId, String ispbPagador, String ispbRecebedor, long valorCentavos, Instant agora) {
        this.endToEndId = endToEndId;
        this.ispbPagador = ispbPagador;
        this.ispbRecebedor = ispbRecebedor;
        this.valorCentavos = valorCentavos;
        this.status = Status.ENVIADA_RECEBEDOR;
        this.criadaEm = agora;
        this.atualizadaEm = agora;
    }

    void mudar(Status novo, String motivo, Instant agora) {
        this.status = novo;
        this.motivo = motivo;
        this.atualizadaEm = agora;
    }

    @Override
    public String getId() {
        return endToEndId;
    }

    public String getEndToEndId() {
        return endToEndId;
    }

    public String getIspbPagador() {
        return ispbPagador;
    }

    public String getIspbRecebedor() {
        return ispbRecebedor;
    }

    public long getValorCentavos() {
        return valorCentavos;
    }

    public Status getStatus() {
        return status;
    }

    public String getMotivo() {
        return motivo;
    }
}
