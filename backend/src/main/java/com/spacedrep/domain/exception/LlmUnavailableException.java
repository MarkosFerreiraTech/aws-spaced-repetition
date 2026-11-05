package com.spacedrep.domain.exception;

public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String detail) {
        super(detail == null ? "O provedor de IA esta indisponivel no momento. Tente novamente." : detail);
    }

    public LlmUnavailableException() {
        this("O provedor de IA esta indisponivel no momento. Tente novamente.");
    }
}
