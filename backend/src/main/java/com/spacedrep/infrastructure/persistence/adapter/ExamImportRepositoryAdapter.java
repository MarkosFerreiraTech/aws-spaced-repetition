package com.spacedrep.infrastructure.persistence.adapter;

import com.spacedrep.domain.model.AnswerOption;
import com.spacedrep.domain.model.ExamSource;
import com.spacedrep.domain.model.Question;
import com.spacedrep.domain.model.ReviewState;
import com.spacedrep.domain.port.ExamImportRepository;
import com.spacedrep.infrastructure.persistence.entity.ExamSourceEntity;
import com.spacedrep.infrastructure.persistence.entity.QuestionEntity;
import com.spacedrep.infrastructure.persistence.entity.ReviewCardEntity;
import com.spacedrep.infrastructure.persistence.repository.ExamSourceJpaRepository;
import com.spacedrep.infrastructure.persistence.repository.QuestionJpaRepository;
import com.spacedrep.infrastructure.persistence.repository.ReviewCardJpaRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExamImportRepositoryAdapter implements ExamImportRepository {

    private final ExamSourceJpaRepository examSources;
    private final QuestionJpaRepository questions;
    private final ReviewCardJpaRepository cards;

    public ExamImportRepositoryAdapter(ExamSourceJpaRepository examSources,
                                       QuestionJpaRepository questions,
                                       ReviewCardJpaRepository cards) {
        this.examSources = examSources;
        this.questions = questions;
        this.cards = cards;
    }

    @Override
    public boolean existsByRawTextHash(String rawTextHash) {
        return examSources.existsByRawTextHash(rawTextHash);
    }

    @Override
    public long save(ExamSource source, List<Question> questionList, LocalDate initialDueDate) {
        ExamSourceEntity savedSource = examSources.save(
                new ExamSourceEntity(source.userId(), source.name(), source.provider(), source.rawTextHash()));

        List<ReviewCardEntity> newCards = new ArrayList<>(questionList.size());
        for (Question question : questionList) {
            QuestionEntity entity = new QuestionEntity(
                    savedSource.getId(), question.topic(), question.stem(),
                    question.explanation(), question.multiSelect());

            for (AnswerOption option : question.options()) {
                entity.addOption((short) option.position(), option.text(), option.correct());
            }

            QuestionEntity savedQuestion = questions.save(entity);
            newCards.add(new ReviewCardEntity(
                    null, source.userId(), savedQuestion.getId(),
                    BigDecimal.valueOf(ReviewState.INITIAL_EASE_FACTOR), 0, 0, initialDueDate, null));
        }
        cards.saveAll(newCards);

        return savedSource.getId();
    }
}
