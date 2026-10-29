package io.github.wallakscardoso.spilab.psp;

import io.github.wallakscardoso.spilab.dict.ChavePix;
import io.github.wallakscardoso.spilab.dict.Dict;
import io.github.wallakscardoso.spilab.iso20022.Cabecalho;
import io.github.wallakscardoso.spilab.iso20022.Identificadores;
import io.github.wallakscardoso.spilab.iso20022.Pacs002;
import io.github.wallakscardoso.spilab.iso20022.Pacs008;
import io.github.wallakscardoso.spilab.rede.Mensagem;
import io.github.wallakscardoso.spilab.rede.Rede;
import io.github.wallakscardoso.spilab.spi.Spi;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/** Os dois papéis de um PSP no Pix: quem envia (pagador) e quem recebe (recebedor). */
@Service
public class PixService {

    public record Resultado(Pix pix, List<Mensagem> mensagens) {
    }

    private final ClienteService clientes;
    private final Dict dict;
    private final PspRegistro registro;
    private final PixRepository pixes;
    private final Rede rede;
    private final Clock relogio;

    public PixService(ClienteService clientes, Dict dict, PspRegistro registro, PixRepository pixes, Rede rede, Clock relogio) {
        this.clientes = clientes;
        this.dict = dict;
        this.registro = registro;
        this.pixes = pixes;
        this.rede = rede;
        this.relogio = relogio;
    }

    /**
     * Pagador: consulta a chave, reserva o valor, manda a pacs.008 ao SPI e conclui conforme a
     * pacs.002 que volta. Chave inexistente ou falta de saldo param antes de qualquer mensagem sair.
     */
    public Pix enviar(String ispb, String agencia, String numero, String chave, long valorCentavos, String descricao) {
        ContaCliente pagador = clientes.buscar(ispb, agencia, numero);
        ChavePix destino = dict.consultar(chave);
        Instant agora = relogio.instant();
        String e2e = Identificadores.endToEndId(ispb, agora);

        registro.reservarEnvio(e2e, pagador, valorCentavos, chave, destino.getIspb());

        Pacs008 ordem = new Pacs008(
                new Cabecalho(ispb, Spi.ISPB, Identificadores.msgId(ispb), Pacs008.TIPO, agora),
                e2e, valorCentavos, agora,
                new Pacs008.Pagador(pagador.getTitularNome(), pagador.getTitularDocumento(), ispb,
                        pagador.getAgencia(), pagador.getNumero(), pagador.getTipo().name()),
                new Pacs008.Recebedor(destino.getTitularDocumento(), destino.getIspb(), destino.getAgencia(),
                        destino.getNumero(), destino.getTipoConta().name(), chave),
                descricao);

        Pacs002 resposta = Pacs002.deXml(rede.enviar("/spi/pacs008", ordem.paraXml()));
        boolean liquidado = resposta.status() == Pacs002.Status.ACSC;
        return registro.concluirEnvio(e2e, ispb, liquidado, liquidado ? null : resposta.motivo());
    }

    /** Recebedor: confere a conta de destino e aceita (ACSP) ou rejeita com AC03. */
    public String receberOrdem(String ispb, String xml) {
        Pacs008 ordem = Pacs008.deXml(xml);
        String e2e = ordem.endToEndId();
        Pacs008.Recebedor destino = ordem.recebedor();
        Cabecalho resposta = new Cabecalho(ispb, Spi.ISPB, Identificadores.msgId(ispb), Pacs002.TIPO, relogio.instant());

        ContaCliente conta;
        try {
            conta = clientes.buscar(ispb, destino.agencia(), destino.conta());
        } catch (ContaClienteNaoEncontradaException e) {
            return Pacs002.rejeicao(resposta, e2e, "AC03", "Conta do recebedor inexistente").paraXml();
        }
        registro.registrarRecebido(e2e, conta, ordem.valorCentavos(), destino.chave(), ordem.pagador().ispb());
        return Pacs002.aceite(resposta, e2e).paraXml();
    }

    /** Recebedor: aviso final do SPI. ACCC credita o cliente; RJCT (ex. AM04) só encerra. */
    public void receberAviso(String ispb, String xml) {
        Pacs002 aviso = Pacs002.deXml(xml);
        registro.concluirRecebido(aviso.endToEndId(), ispb, aviso.status() == Pacs002.Status.ACCC, aviso.motivo());
    }

    public Resultado consultar(String ispb, String endToEndId) {
        Pix pix = pixes.findByEndToEndIdAndIspb(endToEndId, ispb)
                .orElseThrow(() -> new PixNaoEncontradoException(ispb, endToEndId));
        return new Resultado(pix, rede.trocadas(endToEndId));
    }
}
