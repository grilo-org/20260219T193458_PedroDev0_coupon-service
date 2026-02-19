package com.pedro.dev.couponservice.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

/**
 * Manipulador global de exceções da API REST.
 *
 * <p>Intercepta exceções não tratadas nos controllers e as converte em respostas
 * padronizadas no formato {@link ProblemDetail} (RFC 9457).</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Trata exceções de argumento inválido lançadas pelas regras de negócio do domínio.
     *
     * @param e exceção contendo a mensagem de violação da regra de negócio
     * @return {@link ProblemDetail} com status 400 e detalhes do erro
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());

        problemDetail.setTitle("Regra de Negócio Violada");
        problemDetail.setType(URI.create("https://pedro.dev/errors/business-rule"));

        return problemDetail;
    }

    /**
     * Trata violações de integridade de dados, como tentativa de cadastrar um cupom com código duplicado.
     *
     * @param e exceção de violação de integridade lançada pelo banco de dados
     * @return {@link ProblemDetail} com status 409 e mensagem de conflito
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Já existe um cupom cadastrado com este código.");

        problemDetail.setTitle("Conflito de Dados");
        problemDetail.setType(URI.create("https://pedro.dev/errors/data-conflict"));

        return problemDetail;
    }
}