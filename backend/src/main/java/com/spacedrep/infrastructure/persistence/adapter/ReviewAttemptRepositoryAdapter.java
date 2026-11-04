package com.spacedrep.infrastructure.persistence.adapter;

import com.spacedrep.domain.model.ReviewAttempt;
import com.spacedrep.domain.model.ReviewQuality;
import com.spacedrep.domain.port.ReviewAttemptRepository;
import com.spacedrep.infrastructure.persistence.entity.ReviewAttemptEntity;
import com.spacedrep.infrastructure.persistence.repository.ReviewAttemptJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class ReviewAttemptRepositoryAdapter implements ReviewAttemptRepository {

    private final ReviewAttemptJpaRepository jpa;

    public ReviewAttemptRepositoryAdapter(ReviewAttemptJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ReviewAttempt save(ReviewAttempt attempt) {
        ReviewAttemptEntity entity = new ReviewAttemptEntity(
                attempt.id(),
                attempt.cardId(),
                (short) attempt.quality().value(),
                attempt.correct(),
                attempt.durationMs(),
                attempt.answeredAt());
        ReviewAttemptEntity saved = jpa.save(entity);
        return new ReviewAttempt(saved.getId(), saved.getCardId(),
                ReviewQuality.values()[saved.getQuality()],
                saved.isCorrect(), saved.getDurationMs(), saved.getAnsweredAt());
    }
}
