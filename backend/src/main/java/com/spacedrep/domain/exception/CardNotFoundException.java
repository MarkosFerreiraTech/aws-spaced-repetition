package com.spacedrep.domain.exception;

public class CardNotFoundException extends RuntimeException {

    public CardNotFoundException(long cardId) {
        super("Review card not found: " + cardId);
    }
}
