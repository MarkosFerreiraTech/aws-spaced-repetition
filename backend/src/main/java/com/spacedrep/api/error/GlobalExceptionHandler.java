package com.spacedrep.api.error;

import com.spacedrep.domain.exception.CardNotFoundException;
import com.spacedrep.domain.exception.InvalidAnswerException;
import com.spacedrep.domain.exception.NotCardOwnerException;
import com.spacedrep.domain.exception.QuestionNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Erros no padrao RFC 9457 (Problem Details). Prefixo "type" identifica a classe do erro
 * de forma estavel para o front.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_TYPE = "https://spacedrep.local/errors/";

    @ExceptionHandler({CardNotFoundException.class, QuestionNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Recurso nao encontrado");
        problem.setType(java.net.URI.create(BASE_TYPE + "not-found"));
        return problem;
    }

    @ExceptionHandler(NotCardOwnerException.class)
    public ProblemDetail handleForbidden(NotCardOwnerException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "O card informado nao pertence ao usuario autenticado.");
        problem.setTitle("Acesso negado");
        problem.setType(java.net.URI.create(BASE_TYPE + "forbidden"));
        return problem;
    }

    @ExceptionHandler({InvalidAnswerException.class, IllegalArgumentException.class})
    public ProblemDetail handleBadRequest(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Requisicao invalida");
        problem.setType(java.net.URI.create(BASE_TYPE + "bad-request"));
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                ex.getConstraintViolations().stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .reduce((a, b) -> a + "; " + b)
                        .orElse("parametros invalidos"));
        problem.setTitle("Parametros invalidos");
        problem.setType(java.net.URI.create(BASE_TYPE + "bad-request"));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Um ou mais campos sao invalidos.");
        problem.setTitle("Erro de validacao");
        problem.setType(java.net.URI.create(BASE_TYPE + "validation"));
        problem.setProperty("errors", errors);
        return problem;
    }
}
