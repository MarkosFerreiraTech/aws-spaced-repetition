package com.spacedrep.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "review_card")
public class ReviewCardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "question_id", nullable = false)
    private long questionId;

    @Column(name = "ease_factor", nullable = false, precision = 4, scale = 2)
    private BigDecimal easeFactor;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays;

    @Column(name = "repetitions", nullable = false)
    private int repetitions;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    protected ReviewCardEntity() {
    }

    public ReviewCardEntity(Long id, long userId, long questionId, BigDecimal easeFactor,
                            int intervalDays, int repetitions, LocalDate dueDate, Instant lastReviewedAt) {
        this.id = id;
        this.userId = userId;
        this.questionId = questionId;
        this.easeFactor = easeFactor;
        this.intervalDays = intervalDays;
        this.repetitions = repetitions;
        this.dueDate = dueDate;
        this.lastReviewedAt = lastReviewedAt;
    }

    public Long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public long getQuestionId() {
        return questionId;
    }

    public BigDecimal getEaseFactor() {
        return easeFactor;
    }

    public int getIntervalDays() {
        return intervalDays;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Instant getLastReviewedAt() {
        return lastReviewedAt;
    }
}
