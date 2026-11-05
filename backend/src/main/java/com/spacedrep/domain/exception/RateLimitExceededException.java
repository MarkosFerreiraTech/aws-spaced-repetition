package com.spacedrep.domain.exception;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String detail) {
        super(detail == null ? "Limite de requisicoes excedido. Aguarde um instante." : detail);
    }

    public RateLimitExceededException() {
        this("Limite de requisicoes excedido. Aguarde um instante.");
    }
}
