package com.spacedrep.domain.exception;

public class ImportTooLargeException extends RuntimeException {

    public ImportTooLargeException(String detail) {
        super(detail == null ? "O texto enviado excede o limite permitido pelo importador." : detail);
    }

    public ImportTooLargeException() {
        this("O texto enviado excede o limite permitido pelo importador.");
    }
}
