package io.github.wallakscardoso.spilab.psp;

import io.github.wallakscardoso.spilab.razao.Conta;
import io.github.wallakscardoso.spilab.razao.LancamentoInvalidoException;
import io.github.wallakscardoso.spilab.razao.Partida;
import io.github.wallakscardoso.spilab.razao.Razao;
import io.github.wallakscardoso.spilab.spi.ParticipanteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    public record NovaConta(String agencia, String numero, TipoConta tipo,
                            String titularNome, String titularDocumento, long depositoInicialCentavos) {
    }

    private final ContaClienteRepository contasCliente;
    private final ParticipanteService participantes;
    private final Razao razao;

    public ClienteService(ContaClienteRepository contasCliente, ParticipanteService participantes, Razao razao) {
        this.contasCliente = contasCliente;
        this.participantes = participantes;
        this.razao = razao;
    }

    /** Abre a conta do cliente no livro do PSP e registra o depósito inicial contra o caixa. */
    @Transactional
    public ContaCliente abrir(String ispb, NovaConta nova) {
        participantes.exigir(ispb);
        if (contasCliente.existsByIspbAndAgenciaAndNumero(ispb, nova.agencia(), nova.numero())) {
            throw new LancamentoInvalidoException("Conta já existe: %s %s-%s".formatted(ispb, nova.agencia(), nova.numero()));
        }
        String livro = Psp.livro(ispb);
        String codigo = Psp.contaCliente(nova.agencia(), nova.numero());
        razao.abrirContaSeNaoExistir(livro, Psp.CONTA_CAIXA, "Caixa (origem dos depósitos)", true);
        razao.abrirContaSeNaoExistir(livro, Psp.CONTA_TRANSITO, "Pix enviados aguardando o SPI", false);
        razao.abrirContaSeNaoExistir(livro, Psp.CONTA_SPI, "Liquidação via SPI", true);
        Conta conta = razao.abrirConta(livro, codigo, "Conta de " + nova.titularNome(), false);
        ContaCliente contaCliente = contasCliente.save(new ContaCliente(ispb, nova.agencia(), nova.numero(),
                nova.tipo(), nova.titularNome(), nova.titularDocumento(), conta.getId()));
        if (nova.depositoInicialCentavos() > 0) {
            razao.lancar(livro, "Depósito inicial em " + codigo, List.of(
                    Partida.entra(codigo, nova.depositoInicialCentavos()),
                    Partida.sai(Psp.CONTA_CAIXA, nova.depositoInicialCentavos())));
        }
        return contaCliente;
    }

    @Transactional(readOnly = true)
    public ContaCliente buscar(String ispb, String agencia, String numero) {
        return contasCliente.findByIspbAndAgenciaAndNumero(ispb, agencia, numero)
                .orElseThrow(() -> new ContaClienteNaoEncontradaException(ispb, agencia, numero));
    }

    @Transactional(readOnly = true)
    public long saldo(ContaCliente conta) {
        return razao.saldo(Psp.livro(conta.getIspb()), Psp.contaCliente(conta.getAgencia(), conta.getNumero()));
    }
}
