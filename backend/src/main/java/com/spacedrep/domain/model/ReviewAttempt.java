package com.spacedrep.domain.model;

import java.time.Instant;
import java.util.Objects;

/** Registro historico de uma resposta (alimenta a metrica de acerto e futura analise). */
public record ReviewAttempt(Long id, long cardId, ReviewQuality quality, boolean correct,
                            int durationMs, Instant answeredAt) {

    public ReviewAttempt {
        Objects.requireNonNull(quality, "quality is required");
        Objects.requireNonNull(answeredAt, "answeredAt is required");
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs must be >= 0");
        }
    }

    public static ReviewAttempt record(long cardId, ReviewQuality quality, boolean correct,
                                       int durationMs, Instant answeredAt) {
        return new ReviewAttempt(null, cardId, quality, correct, durationMs, answeredAt);
    }
}
