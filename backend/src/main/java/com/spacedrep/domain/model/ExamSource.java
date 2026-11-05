package com.spacedrep.domain.model;

/** Origem de um conjunto de questoes importadas (um simulado colado). */
public record ExamSource(Long id, long userId, String name, String provider, String rawTextHash) {

    public static ExamSource newSource(long userId, String name, String rawTextHash) {
        return new ExamSource(null, userId, name, "UDEMY", rawTextHash);
    }
}
