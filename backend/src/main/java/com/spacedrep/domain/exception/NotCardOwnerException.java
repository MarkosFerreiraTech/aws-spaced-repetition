package com.spacedrep.domain.exception;

public class NotCardOwnerException extends RuntimeException {

    public NotCardOwnerException(long cardId) {
        super("Review card does not belong to the current user: " + cardId);
    }
}
