package io.github.wallakscardoso.spilab.dict;

import io.github.wallakscardoso.spilab.psp.ClienteService;
import io.github.wallakscardoso.spilab.psp.ContaCliente;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/** Diretório de chaves Pix simulado, com as regras de formato e de limite do Manual Operacional do DICT. */
@Service
public class Dict {

    static final int LIMITE_CHAVES_CONTA_CPF = 5;
    static final int LIMITE_CHAVES_CONTA_CNPJ = 20;

    private final ChavePixRepository chaves;
    private final ClienteService clientes;
    private final Clock relogio;

    public Dict(ChavePixRepository chaves, ClienteService clientes, Clock relogio) {
        this.chaves = chaves;
        this.clientes = clientes;
        this.relogio = relogio;
    }

    /**
     * Vincula uma chave à conta. Na chave aleatória (EVP) quem gera o valor é o DICT, então
     * {@code chave} vem vazia.
     */
    @Transactional
    public ChavePix registrar(TipoChave tipo, String chave, String ispb, String agencia, String numero) {
        ContaCliente conta = clientes.buscar(ispb, agencia, numero);
        String valor = tipo == TipoChave.EVP ? gerarEvp(chave) : chave;

        if (!tipo.aceita(valor)) {
            throw new ChaveInvalidaException("Chave fora do formato de " + tipo + ": " + valor);
        }
        if ((tipo == TipoChave.CPF || tipo == TipoChave.CNPJ) && !valor.equals(conta.getTitularDocumento())) {
            throw new ChaveInvalidaException("Chave " + tipo + " só pode ser registrada pelo próprio titular da conta");
        }
        if (chaves.existsById(valor)) {
            throw new ChaveInvalidaException("Chave já registrada: " + valor);
        }
        int limite = conta.getTitularDocumento().length() == 11 ? LIMITE_CHAVES_CONTA_CPF : LIMITE_CHAVES_CONTA_CNPJ;
        if (chaves.countByIspbAndAgenciaAndNumero(ispb, agencia, numero) >= limite) {
            throw new ChaveInvalidaException("A conta já tem o máximo de " + limite + " chaves");
        }

        return chaves.save(new ChavePix(valor, tipo, ispb, agencia, numero, conta.getTipo(),
                conta.getTitularNome(), conta.getTitularDocumento(), relogio.instant()));
    }

    @Transactional(readOnly = true)
    public ChavePix consultar(String chave) {
        return chaves.findById(chave).orElseThrow(() -> new ChaveNaoEncontradaException(chave));
    }

    private static String gerarEvp(String informada) {
        if (informada != null && !informada.isBlank()) {
            throw new ChaveInvalidaException("Chave aleatória é gerada pelo DICT; não informe o valor");
        }
        return UUID.randomUUID().toString();
    }

    /** Como o DICT devolve o documento do recebedor: CPF parcialmente oculto, CNPJ aberto. */
    static String mascarar(String documento) {
        if (documento.length() != 11) {
            return documento;
        }
        return "***." + documento.substring(3, 6) + "." + documento.substring(6, 9) + "-**";
    }
}
