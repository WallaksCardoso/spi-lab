package io.github.wallakscardoso.spilab.psp;

import io.github.wallakscardoso.spilab.config.EntidadeComIdAtribuido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "conta_cliente")
public class ContaCliente extends EntidadeComIdAtribuido<UUID> {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String ispb;

    @Column(nullable = false)
    private String agencia;

    @Column(nullable = false)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoConta tipo;

    @Column(name = "titular_nome", nullable = false)
    private String titularNome;

    @Column(name = "titular_documento", nullable = false)
    private String titularDocumento;

    @Column(name = "conta_id", nullable = false)
    private UUID contaRazaoId;

    protected ContaCliente() {
    }

    ContaCliente(String ispb, String agencia, String numero, TipoConta tipo,
                 String titularNome, String titularDocumento, UUID contaRazaoId) {
        this.id = UUID.randomUUID();
        this.ispb = ispb;
        this.agencia = agencia;
        this.numero = numero;
        this.tipo = tipo;
        this.titularNome = titularNome;
        this.titularDocumento = titularDocumento;
        this.contaRazaoId = contaRazaoId;
    }

    public UUID getId() {
        return id;
    }

    public String getIspb() {
        return ispb;
    }

    public String getAgencia() {
        return agencia;
    }

    public String getNumero() {
        return numero;
    }

    public TipoConta getTipo() {
        return tipo;
    }

    public String getTitularNome() {
        return titularNome;
    }

    public String getTitularDocumento() {
        return titularDocumento;
    }
}
