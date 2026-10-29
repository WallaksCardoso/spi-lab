package io.github.wallakscardoso.spilab.psp;

import io.github.wallakscardoso.spilab.config.EntidadeComIdAtribuido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Um Pix do ponto de vista de um PSP: o que ele enviou ou recebeu para um cliente seu. */
@Entity
@Table(name = "pix")
public class Pix extends EntidadeComIdAtribuido<UUID> {

    public enum Direcao { ENVIADO, RECEBIDO }

    public enum Status { EM_ANDAMENTO, LIQUIDADO, REJEITADO }

    @Id
    private UUID id;

    @Column(name = "end_to_end_id", nullable = false)
    private String endToEndId;

    @Column(nullable = false)
    private String ispb;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direcao direcao;

    @Column(nullable = false)
    private String agencia;

    @Column(nullable = false)
    private String numero;

    @Column(name = "valor_centavos", nullable = false)
    private long valorCentavos;

    private String chave;

    @Column(name = "ispb_contraparte", nullable = false)
    private String ispbContraparte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private String motivo;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Pix() {
    }

    Pix(String endToEndId, String ispb, Direcao direcao, String agencia, String numero, long valorCentavos,
        String chave, String ispbContraparte, Instant agora) {
        this.id = UUID.randomUUID();
        this.endToEndId = endToEndId;
        this.ispb = ispb;
        this.direcao = direcao;
        this.agencia = agencia;
        this.numero = numero;
        this.valorCentavos = valorCentavos;
        this.chave = chave;
        this.ispbContraparte = ispbContraparte;
        this.status = Status.EM_ANDAMENTO;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    void concluir(Status novo, String motivo, Instant agora) {
        this.status = novo;
        this.motivo = motivo;
        this.atualizadoEm = agora;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public String getEndToEndId() {
        return endToEndId;
    }

    public String getIspb() {
        return ispb;
    }

    public Direcao getDirecao() {
        return direcao;
    }

    public String getAgencia() {
        return agencia;
    }

    public String getNumero() {
        return numero;
    }

    public long getValorCentavos() {
        return valorCentavos;
    }

    public String getChave() {
        return chave;
    }

    public String getIspbContraparte() {
        return ispbContraparte;
    }

    public Status getStatus() {
        return status;
    }

    public String getMotivo() {
        return motivo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
