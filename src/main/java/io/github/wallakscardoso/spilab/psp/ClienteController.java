package io.github.wallakscardoso.spilab.psp;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

@RestController
@RequestMapping("/psps/{ispb}/clientes")
public class ClienteController {

    private final ClienteService servico;

    public ClienteController(ClienteService servico) {
        this.servico = servico;
    }

    public record NovaContaRequest(
            @NotBlank @Pattern(regexp = "\\d{1,4}", message = "agência tem até 4 dígitos") String agencia,
            @NotBlank @Pattern(regexp = "\\d{1,20}", message = "conta tem até 20 dígitos") String numero,
            @NotNull TipoConta tipo,
            @NotBlank @Size(max = 140) String titularNome,
            @NotBlank @Pattern(regexp = "\\d{11}|[0-9A-Z]{12}\\d{2}",
                    message = "CPF com 11 dígitos ou CNPJ alfanumérico com 14 caracteres") String titularDocumento,
            @PositiveOrZero long depositoInicialCentavos) {
    }

    public record ContaResponse(String ispb, String agencia, String numero, TipoConta tipo,
                                String titularNome, long saldoCentavos) {

        static ContaResponse de(ContaCliente conta, long saldo) {
            return new ContaResponse(conta.getIspb(), conta.getAgencia(), conta.getNumero(), conta.getTipo(),
                    conta.getTitularNome(), saldo);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContaResponse abrir(@PathVariable String ispb, @Valid @RequestBody NovaContaRequest req) {
        ContaCliente conta = servico.abrir(ispb, new ClienteService.NovaConta(req.agencia(), req.numero(), req.tipo(),
                req.titularNome(), req.titularDocumento(), req.depositoInicialCentavos()));
        return ContaResponse.de(conta, servico.saldo(conta));
    }

    @GetMapping("/{agencia}/{numero}")
    public ContaResponse buscar(@PathVariable String ispb, @PathVariable String agencia, @PathVariable String numero) {
        ContaCliente conta = servico.buscar(ispb, agencia, numero);
        return ContaResponse.de(conta, servico.saldo(conta));
    }
}
