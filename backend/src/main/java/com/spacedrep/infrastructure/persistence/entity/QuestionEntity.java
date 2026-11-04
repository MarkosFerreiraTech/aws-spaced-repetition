package com.spacedrep.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "question")
public class QuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_source_id", nullable = false)
    private long examSourceId;

    @Column(name = "topic")
    private String topic;

    @Column(name = "stem", nullable = false, columnDefinition = "text")
    private String stem;

    @Column(name = "explanation", columnDefinition = "text")
    private String explanation;

    @Column(name = "multi_select", nullable = false)
    private boolean multiSelect;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<AnswerOptionEntity> options = new ArrayList<>();

    protected QuestionEntity() {
    }

    public Long getId() {
        return id;
    }

    public long getExamSourceId() {
        return examSourceId;
    }

    public String getTopic() {
        return topic;
    }

    public String getStem() {
        return stem;
    }

    public String getExplanation() {
        return explanation;
    }

    public boolean isMultiSelect() {
        return multiSelect;
    }

    public List<AnswerOptionEntity> getOptions() {
        return options;
    }
}
