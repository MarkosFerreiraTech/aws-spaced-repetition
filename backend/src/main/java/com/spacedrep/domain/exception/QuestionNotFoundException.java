package com.spacedrep.domain.exception;

public class QuestionNotFoundException extends RuntimeException {

    public QuestionNotFoundException(long questionId) {
        super("Question not found: " + questionId);
    }
}
