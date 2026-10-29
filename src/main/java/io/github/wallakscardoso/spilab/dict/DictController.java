package io.github.wallakscardoso.spilab.dict;

import io.github.wallakscardoso.spilab.psp.TipoConta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dict/chaves")
public class DictController {

    private final Dict dict;

    public DictController(Dict dict) {
        this.dict = dict;
    }

    public record NovaChave(@NotNull TipoChave tipo, String chave,
                            @NotBlank String ispb, @NotBlank String agencia, @NotBlank String numero) {
    }

    public record Vinculo(String chave, TipoChave tipo, String ispb, String agencia, String numero,
                          TipoConta tipoConta, String titularNome, String titularDocumento) {

        static Vinculo de(ChavePix c) {
            return new Vinculo(c.getChave(), c.getTipo(), c.getIspb(), c.getAgencia(), c.getNumero(),
                    c.getTipoConta(), c.getTitularNome(), Dict.mascarar(c.getTitularDocumento()));
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Vinculo registrar(@Valid @RequestBody NovaChave nova) {
        return Vinculo.de(dict.registrar(nova.tipo(), nova.chave(), nova.ispb(), nova.agencia(), nova.numero()));
    }

    @GetMapping("/{chave}")
    public Vinculo consultar(@PathVariable String chave) {
        return Vinculo.de(dict.consultar(chave));
    }
}
