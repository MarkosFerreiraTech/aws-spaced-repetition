package com.spacedrep.api.error;

import com.spacedrep.domain.exception.CardNotFoundException;
import com.spacedrep.domain.exception.DuplicateImportException;
import com.spacedrep.domain.exception.ImportTooLargeException;
import com.spacedrep.domain.exception.InvalidAnswerException;
import com.spacedrep.domain.exception.InvalidCredentialsException;
import com.spacedrep.domain.exception.InvalidImportException;
import com.spacedrep.domain.exception.InvalidLlmResponseException;
import com.spacedrep.domain.exception.LlmUnavailableException;
import com.spacedrep.domain.exception.NotCardOwnerException;
import com.spacedrep.domain.exception.QuestionNotFoundException;
import com.spacedrep.domain.exception.RateLimitExceededException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Erros no padrao RFC 9457 (Problem Details).
 *
 * <p>Sem stacktrace e sem SQL na resposta: detalhe tecnico vai para o log,
 * o cliente recebe um identificador estavel em {@code type}.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_TYPE = "https://spacedrep.local/errors/";

    private static ProblemDetail problem(HttpStatus status, String title, String type, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(BASE_TYPE + type));
        return problem;
    }

    @ExceptionHandler({CardNotFoundException.class, QuestionNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return problem(HttpStatus.NOT_FOUND, "Recurso nao encontrado", "not-found", ex.getMessage());
    }

    @ExceptionHandler(NotCardOwnerException.class)
    public ProblemDetail handleForbidden(NotCardOwnerException ex) {
        return problem(HttpStatus.FORBIDDEN, "Acesso negado", "forbidden",
                "O card informado nao pertence ao usuario autenticado.");
    }

    @ExceptionHandler({InvalidCredentialsException.class,
            AuthenticationCredentialsNotFoundException.class})
    public ProblemDetail handleUnauthorized(RuntimeException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Nao autenticado", "unauthorized",
                "Credenciais invalidas.");
    }

    @ExceptionHandler(DuplicateImportException.class)
    public ProblemDetail handleDuplicate(DuplicateImportException ex) {
        return problem(HttpStatus.CONFLICT, "Importacao duplicada", "duplicate-import",
                "Este simulado ja foi importado. Para reimportar, remova o anterior.");
    }

    @ExceptionHandler(ImportTooLargeException.class)
    public ProblemDetail handleTooLarge(ImportTooLargeException ex) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "Texto muito grande", "payload-too-large",
                ex.getMessage());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ProblemDetail handleRateLimit(RateLimitExceededException ex) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "Limite de requisicoes", "rate-limited",
                ex.getMessage());
    }

    @ExceptionHandler(InvalidLlmResponseException.class)
    public ProblemDetail handleLlmResponse(InvalidLlmResponseException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Nao foi possivel estruturar o simulado",
                "unprocessable-llm-response", ex.getMessage());
    }

    @ExceptionHandler(LlmUnavailableException.class)
    public ProblemDetail handleLlmUnavailable(LlmUnavailableException ex) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Provedor de IA indisponivel",
                "llm-unavailable", ex.getMessage());
    }

    @ExceptionHandler({InvalidAnswerException.class, InvalidImportException.class,
            IllegalArgumentException.class})
    public ProblemDetail handleBadRequest(RuntimeException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Requisicao invalida", "bad-request", ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("parametros invalidos");
        return problem(HttpStatus.BAD_REQUEST, "Parametros invalidos", "bad-request", detail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Erro de validacao", "validation",
                "Um ou mais campos sao invalidos.");
        problem.setProperty("errors", errors);
        return problem;
    }
}
