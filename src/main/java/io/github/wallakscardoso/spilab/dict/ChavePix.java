package io.github.wallakscardoso.spilab.dict;

import io.github.wallakscardoso.spilab.config.EntidadeComIdAtribuido;
import io.github.wallakscardoso.spilab.psp.TipoConta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Vínculo de uma chave Pix com uma conta transacional, como o DICT guarda. */
@Entity
@Table(name = "chave_pix")
public class ChavePix extends EntidadeComIdAtribuido<String> {

    @Id
    private String chave;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoChave tipo;

    @Column(nullable = false)
    private String ispb;

    @Column(nullable = false)
    private String agencia;

    @Column(nullable = false)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_conta", nullable = false)
    private TipoConta tipoConta;

    @Column(name = "titular_nome", nullable = false)
    private String titularNome;

    @Column(name = "titular_documento", nullable = false)
    private String titularDocumento;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    protected ChavePix() {
    }

    ChavePix(String chave, TipoChave tipo, String ispb, String agencia, String numero, TipoConta tipoConta,
             String titularNome, String titularDocumento, Instant criadaEm) {
        this.chave = chave;
        this.tipo = tipo;
        this.ispb = ispb;
        this.agencia = agencia;
        this.numero = numero;
        this.tipoConta = tipoConta;
        this.titularNome = titularNome;
        this.titularDocumento = titularDocumento;
        this.criadaEm = criadaEm;
    }

    @Override
    public String getId() {
        return chave;
    }

    public String getChave() {
        return chave;
    }

    public TipoChave getTipo() {
        return tipo;
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

    public TipoConta getTipoConta() {
        return tipoConta;
    }

    public String getTitularNome() {
        return titularNome;
    }

    public String getTitularDocumento() {
        return titularDocumento;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }
}
