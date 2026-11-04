package com.spacedrep.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "review_attempt")
public class ReviewAttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false)
    private long cardId;

    @Column(name = "quality", nullable = false)
    private short quality;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "answered_at", nullable = false)
    private Instant answeredAt;

    protected ReviewAttemptEntity() {
    }

    public ReviewAttemptEntity(Long id, long cardId, short quality, boolean correct,
                               Integer durationMs, Instant answeredAt) {
        this.id = id;
        this.cardId = cardId;
        this.quality = quality;
        this.correct = correct;
        this.durationMs = durationMs;
        this.answeredAt = answeredAt;
    }

    public Long getId() {
        return id;
    }

    public long getCardId() {
        return cardId;
    }

    public short getQuality() {
        return quality;
    }

    public boolean isCorrect() {
        return correct;
    }

    public Integer getDurationMs() {
        return durationMs;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }
}
