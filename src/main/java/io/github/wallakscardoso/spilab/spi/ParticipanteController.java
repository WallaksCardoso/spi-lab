package io.github.wallakscardoso.spilab.spi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/spi/participantes")
public class ParticipanteController {

    private final ParticipanteService servico;

    public ParticipanteController(ParticipanteService servico) {
        this.servico = servico;
    }

    public record NovoParticipante(
            @NotBlank @Pattern(regexp = "\\d{8}", message = "ISPB tem 8 dígitos") String ispb,
            @NotBlank @Size(max = 120) String nome,
            @PositiveOrZero long aporteInicialCentavos) {
    }

    public record Saldo(String ispb, long saldoCentavos) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Saldo cadastrar(@Valid @RequestBody NovoParticipante novo) {
        servico.cadastrar(novo.ispb(), novo.nome(), novo.aporteInicialCentavos());
        return new Saldo(novo.ispb(), servico.saldoPi(novo.ispb()));
    }

    @GetMapping("/{ispb}/saldo")
    public Saldo saldo(@PathVariable String ispb) {
        return new Saldo(ispb, servico.saldoPi(ispb));
    }
}
