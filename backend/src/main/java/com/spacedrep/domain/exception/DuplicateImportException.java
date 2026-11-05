package com.spacedrep.domain.exception;

public class DuplicateImportException extends RuntimeException {

    public DuplicateImportException(String detail) {
        super(detail == null ? "Este simulado ja foi importado anteriormente." : detail);
    }

    public DuplicateImportException() {
        this("Este simulado ja foi importado anteriormente.");
    }
}
