package io.github.wallakscardoso.spilab.psp;

import io.github.wallakscardoso.spilab.rede.Mensagem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** O app do banco: o cliente manda Pix por chave e acompanha o resultado. */
@RestController
@RequestMapping("/psps/{ispb}/pix")
public class PixController {

    private final PixService servico;

    public PixController(PixService servico) {
        this.servico = servico;
    }

    public record NovoPix(@NotBlank String agencia, @NotBlank String numero, @NotBlank String chave,
                          @PositiveOrZero long valorCentavos, @Size(max = 140) String descricao) {
    }

    public record PixResponse(String endToEndId, Pix.Direcao direcao, Pix.Status status, String motivo,
                              long valorCentavos, String chave, String ispbContraparte, List<MensagemResponse> mensagens) {

        static PixResponse de(Pix pix, List<Mensagem> mensagens) {
            return new PixResponse(pix.getEndToEndId(), pix.getDirecao(), pix.getStatus(), pix.getMotivo(),
                    pix.getValorCentavos(), pix.getChave(), pix.getIspbContraparte(),
                    mensagens.stream().map(MensagemResponse::de).toList());
        }
    }

    public record MensagemResponse(String tipo, String de, String para, String status, Instant em, String xml) {

        static MensagemResponse de(Mensagem m) {
            return new MensagemResponse(m.getTipo(), m.getDe(), m.getPara(), m.getStatus(), m.getCriadaEm(), m.getXml());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PixResponse enviar(@PathVariable String ispb, @Valid @RequestBody NovoPix novo) {
        Pix pix = servico.enviar(ispb, novo.agencia(), novo.numero(), novo.chave(), novo.valorCentavos(), novo.descricao());
        return PixResponse.de(pix, List.of());
    }

    @GetMapping("/{endToEndId}")
    public PixResponse consultar(@PathVariable String ispb, @PathVariable String endToEndId) {
        PixService.Resultado r = servico.consultar(ispb, endToEndId);
        return PixResponse.de(r.pix(), r.mensagens());
    }
}
