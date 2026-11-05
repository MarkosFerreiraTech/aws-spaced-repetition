package com.spacedrep.domain.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String detail) {
        super(detail == null ? "Credenciais invalidas." : detail);
    }

    public InvalidCredentialsException() {
        this("Credenciais invalidas.");
    }
}
