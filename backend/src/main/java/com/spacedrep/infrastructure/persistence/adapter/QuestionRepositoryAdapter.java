package com.spacedrep.infrastructure.persistence.adapter;

import com.spacedrep.domain.model.AnswerOption;
import com.spacedrep.domain.model.Question;
import com.spacedrep.domain.port.QuestionRepository;
import com.spacedrep.infrastructure.persistence.entity.AnswerOptionEntity;
import com.spacedrep.infrastructure.persistence.repository.QuestionJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class QuestionRepositoryAdapter implements QuestionRepository {

    private final QuestionJpaRepository jpa;

    public QuestionRepositoryAdapter(QuestionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Question> findById(long questionId) {
        return jpa.findWithOptionsById(questionId).map(entity -> {
            List<AnswerOption> options = entity.getOptions().stream()
                    .map(QuestionRepositoryAdapter::toDomain)
                    .toList();
            return Question.rehydrate(
                    entity.getId(), entity.getTopic(), entity.getStem(),
                    entity.getExplanation(), entity.isMultiSelect(), options);
        });
    }

    private static AnswerOption toDomain(AnswerOptionEntity entity) {
        return new AnswerOption(entity.getId(), entity.getPosition(), entity.getText(), entity.isCorrect());
    }
}
