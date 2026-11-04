package com.spacedrep.domain.model;

import com.spacedrep.domain.scheduler.ScheduleOutcome;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Card de revisao: liga um usuario a uma questao e carrega o estado de agendamento.
 * Entidade rica: o reagendamento e comportamento do proprio card.
 */
public final class ReviewCard {

    private final Long id;
    private final long userId;
    private final long questionId;
    private ReviewState state;
    private LocalDate dueDate;
    private Instant lastReviewedAt;

    private ReviewCard(Long id, long userId, long questionId,
                       ReviewState state, LocalDate dueDate, Instant lastReviewedAt) {
        this.id = id;
        this.userId = userId;
        this.questionId = questionId;
        this.state = Objects.requireNonNull(state, "state is required");
        this.dueDate = Objects.requireNonNull(dueDate, "dueDate is required");
        this.lastReviewedAt = lastReviewedAt;
    }

    public static ReviewCard newCard(long userId, long questionId, LocalDate today) {
        return new ReviewCard(null, userId, questionId, ReviewState.initial(), today, null);
    }

    public static ReviewCard rehydrate(Long id, long userId, long questionId,
                                       ReviewState state, LocalDate dueDate, Instant lastReviewedAt) {
        return new ReviewCard(id, userId, questionId, state, dueDate, lastReviewedAt);
    }

    /** Aplica o resultado do scheduler. Unico caminho para alterar o agendamento. */
    public void applySchedule(ScheduleOutcome outcome, Instant reviewedAt) {
        Objects.requireNonNull(outcome, "outcome is required");
        this.state = new ReviewState(outcome.easeFactor(), outcome.intervalDays(), outcome.repetitions());
        this.dueDate = outcome.dueDate();
        this.lastReviewedAt = Objects.requireNonNull(reviewedAt, "reviewedAt is required");
    }

    public boolean isDueOn(LocalDate date) {
        return !dueDate.isAfter(date);
    }

    public boolean belongsTo(long userId) {
        return this.userId == userId;
    }

    public Long id() {
        return id;
    }

    public long userId() {
        return userId;
    }

    public long questionId() {
        return questionId;
    }

    public ReviewState state() {
        return state;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public Instant lastReviewedAt() {
        return lastReviewedAt;
    }
}
