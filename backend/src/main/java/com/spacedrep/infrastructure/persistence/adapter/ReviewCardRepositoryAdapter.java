package com.spacedrep.infrastructure.persistence.adapter;

import com.spacedrep.domain.model.ReviewCard;
import com.spacedrep.domain.model.ReviewState;
import com.spacedrep.domain.port.ReviewCardRepository;
import com.spacedrep.infrastructure.persistence.entity.ReviewCardEntity;
import com.spacedrep.infrastructure.persistence.repository.ReviewCardJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Adaptador: traduz porta de dominio para Spring Data. */
@Component
public class ReviewCardRepositoryAdapter implements ReviewCardRepository {

    private final ReviewCardJpaRepository jpa;

    public ReviewCardRepositoryAdapter(ReviewCardJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<ReviewCard> findDue(long userId, LocalDate date, int limit) {
        return jpa.findDue(userId, date, PageRequest.of(0, limit)).stream()
                .map(ReviewCardRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public long countDue(long userId, LocalDate date) {
        return jpa.countByUserIdAndDueDateLessThanEqual(userId, date);
    }

    @Override
    public Optional<ReviewCard> findById(long cardId) {
        return jpa.findById(cardId).map(ReviewCardRepositoryAdapter::toDomain);
    }

    @Override
    public ReviewCard save(ReviewCard card) {
        ReviewCardEntity saved = jpa.save(toEntity(card));
        return toDomain(saved);
    }

    private static ReviewCard toDomain(ReviewCardEntity entity) {
        ReviewState state = new ReviewState(
                entity.getEaseFactor().doubleValue(),
                entity.getIntervalDays(),
                entity.getRepetitions());
        return ReviewCard.rehydrate(
                entity.getId(), entity.getUserId(), entity.getQuestionId(),
                state, entity.getDueDate(), entity.getLastReviewedAt());
    }

    private static ReviewCardEntity toEntity(ReviewCard card) {
        return new ReviewCardEntity(
                card.id(),
                card.userId(),
                card.questionId(),
                BigDecimal.valueOf(card.state().easeFactor()).setScale(2, RoundingMode.HALF_UP),
                card.state().intervalDays(),
                card.state().repetitions(),
                card.dueDate(),
                card.lastReviewedAt());
    }
}
