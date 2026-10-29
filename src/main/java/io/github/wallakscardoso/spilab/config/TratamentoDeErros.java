package io.github.wallakscardoso.spilab.config;

import io.github.wallakscardoso.spilab.psp.ContaClienteNaoEncontradaException;
import io.github.wallakscardoso.spilab.razao.LancamentoInvalidoException;
import io.github.wallakscardoso.spilab.razao.SaldoInsuficienteException;
import io.github.wallakscardoso.spilab.spi.ParticipanteNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratamentoDeErros {

    @ExceptionHandler({ParticipanteNaoEncontradoException.class, ContaClienteNaoEncontradaException.class})
    ProblemDetail naoEncontrado(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({LancamentoInvalidoException.class, SaldoInsuficienteException.class})
    ProblemDetail regraDeNegocio(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }
}
