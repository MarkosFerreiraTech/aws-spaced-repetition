package com.spacedrep.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "answer_option")
public class AnswerOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    @Column(name = "position", nullable = false)
    private short position;

    @Column(name = "text", nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    protected AnswerOptionEntity() {
    }

    public Long getId() {
        return id;
    }

    public QuestionEntity getQuestion() {
        return question;
    }

    public short getPosition() {
        return position;
    }

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return correct;
    }
}
