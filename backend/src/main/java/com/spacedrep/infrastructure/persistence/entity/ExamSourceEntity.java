package com.spacedrep.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "exam_source")
public class ExamSourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "raw_text_hash", nullable = false, length = 64)
    private String rawTextHash;

    @Column(name = "imported_at", nullable = false, insertable = false, updatable = false)
    private Instant importedAt;

    protected ExamSourceEntity() {
    }

    public ExamSourceEntity(long userId, String name, String provider, String rawTextHash) {
        this.userId = userId;
        this.name = name;
        this.provider = provider;
        this.rawTextHash = rawTextHash;
    }

    public Long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getProvider() {
        return provider;
    }

    public String getRawTextHash() {
        return rawTextHash;
    }

    public Instant getImportedAt() {
        return importedAt;
    }
}
