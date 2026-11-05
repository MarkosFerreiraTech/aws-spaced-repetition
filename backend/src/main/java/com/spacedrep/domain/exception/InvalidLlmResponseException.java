package com.spacedrep.domain.exception;

public class InvalidLlmResponseException extends RuntimeException {

    public InvalidLlmResponseException(String detail) {
        super(detail == null ? "Nao foi possivel estruturar a resposta do provedor de IA." : detail);
    }

    public InvalidLlmResponseException() {
        this("Nao foi possivel estruturar a resposta do provedor de IA.");
    }
}
