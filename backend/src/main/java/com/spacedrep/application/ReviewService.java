package com.spacedrep.application;

import com.spacedrep.application.command.AnswerCommand;
import com.spacedrep.application.view.AnswerResult;
import com.spacedrep.application.view.DueCard;
import com.spacedrep.application.view.DueQueue;
import com.spacedrep.domain.exception.CardNotFoundException;
import com.spacedrep.domain.exception.InvalidAnswerException;
import com.spacedrep.domain.exception.NotCardOwnerException;
import com.spacedrep.domain.exception.QuestionNotFoundException;
import com.spacedrep.domain.model.Question;
import com.spacedrep.domain.model.ReviewAttempt;
import com.spacedrep.domain.model.ReviewCard;
import com.spacedrep.domain.model.ReviewQuality;
import com.spacedrep.domain.port.QuestionRepository;
import com.spacedrep.domain.port.ReviewAttemptRepository;
import com.spacedrep.domain.port.ReviewCardRepository;
import com.spacedrep.domain.scheduler.ScheduleOutcome;
import com.spacedrep.domain.scheduler.Scheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Caso de uso: estudar (fila do dia e resposta).
 *
 * <p>Orquestra dominio e portas. Nao contem regra de correcao (Question)
 * nem regra de intervalo (Scheduler) — apenas as combina.</p>
 */
@Service
public class ReviewService {

    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 100;

    private final ReviewCardRepository cards;
    private final QuestionRepository questions;
    private final ReviewAttemptRepository attempts;
    private final Scheduler scheduler;
    private final Clock clock;

    public ReviewService(ReviewCardRepository cards,
                         QuestionRepository questions,
                         ReviewAttemptRepository attempts,
                         Scheduler scheduler,
                         Clock clock) {
        this.cards = cards;
        this.questions = questions;
        this.attempts = attempts;
        this.scheduler = scheduler;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DueQueue getDueQueue(long userId, int limit) {
        int safeLimit = Math.max(MIN_LIMIT, Math.min(MAX_LIMIT, limit));
        LocalDate today = LocalDate.now(clock);

        List<ReviewCard> due = cards.findDue(userId, today, safeLimit);
        List<DueCard> result = new ArrayList<>(due.size());
        for (ReviewCard card : due) {
            questions.findById(card.questionId())
                    .map(question -> new DueCard(card, question))
                    .ifPresent(result::add);
        }

        long remaining = Math.max(0, cards.countDue(userId, today) - result.size());
        return new DueQueue(result, remaining);
    }

    @Transactional
    public AnswerResult answer(long userId, long cardId, AnswerCommand command) {
        ReviewCard card = cards.findById(cardId).orElseThrow(() -> new CardNotFoundException(cardId));
        if (!card.belongsTo(userId)) {
            throw new NotCardOwnerException(cardId);
        }

        Question question = questions.findById(card.questionId())
                .orElseThrow(() -> new QuestionNotFoundException(card.questionId()));

        if (!question.isValidSelection(command.selectedOptionIds())) {
            throw new InvalidAnswerException("selectedOptionIds must reference options of question "
                    + question.id());
        }

        boolean correct = question.isCorrect(command.selectedOptionIds());
        ReviewQuality quality = ReviewQuality.evaluate(
                correct, question.isPartialSelection(command.selectedOptionIds()), command.durationMs());

        LocalDate today = LocalDate.now(clock);
        Instant now = clock.instant();
        ScheduleOutcome outcome = scheduler.schedule(card.state(), quality, today);

        card.applySchedule(outcome, now);
        cards.save(card);
        attempts.save(ReviewAttempt.record(cardId, quality, correct, command.durationMs(), now));

        return new AnswerResult(correct, question.explanation(), question.correctOptionIds(), outcome);
    }
}
