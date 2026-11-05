package com.spacedrep.domain.exception;

public class InvalidImportException extends RuntimeException {

    public InvalidImportException(String detail) {
        super(detail == null ? "A importacao contem questoes invalidas." : detail);
    }

    public InvalidImportException() {
        this("A importacao contem questoes invalidas.");
    }
}
