package io.github.wallakscardoso.spilab.config;

import io.github.wallakscardoso.spilab.dict.ChaveInvalidaException;
import io.github.wallakscardoso.spilab.dict.ChaveNaoEncontradaException;
import io.github.wallakscardoso.spilab.iso20022.MensagemInvalidaException;
import io.github.wallakscardoso.spilab.psp.ContaClienteNaoEncontradaException;
import io.github.wallakscardoso.spilab.psp.PixNaoEncontradoException;
import io.github.wallakscardoso.spilab.spi.OrdemDuplicadaException;
import io.github.wallakscardoso.spilab.razao.LancamentoInvalidoException;
import io.github.wallakscardoso.spilab.razao.SaldoInsuficienteException;
import io.github.wallakscardoso.spilab.spi.ParticipanteNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratamentoDeErros {

    @ExceptionHandler({ParticipanteNaoEncontradoException.class, ContaClienteNaoEncontradaException.class,
            ChaveNaoEncontradaException.class, PixNaoEncontradoException.class})
    ProblemDetail naoEncontrado(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MensagemInvalidaException.class)
    ProblemDetail mensagemInvalida(MensagemInvalidaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(OrdemDuplicadaException.class)
    ProblemDetail duplicada(OrdemDuplicadaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler({LancamentoInvalidoException.class, SaldoInsuficienteException.class,
            ChaveInvalidaException.class})
    ProblemDetail regraDeNegocio(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }
}
