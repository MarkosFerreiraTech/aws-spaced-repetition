package com.spacedrep.application;

import com.spacedrep.application.command.AnswerCommand;
import com.spacedrep.application.view.AnswerResult;
import com.spacedrep.application.view.DueQueue;
import com.spacedrep.domain.exception.CardNotFoundException;
import com.spacedrep.domain.exception.InvalidAnswerException;
import com.spacedrep.domain.exception.NotCardOwnerException;
import com.spacedrep.domain.model.AnswerOption;
import com.spacedrep.domain.model.Question;
import com.spacedrep.domain.model.ReviewAttempt;
import com.spacedrep.domain.model.ReviewCard;
import com.spacedrep.domain.model.ReviewQuality;
import com.spacedrep.domain.model.ReviewState;
import com.spacedrep.domain.port.QuestionRepository;
import com.spacedrep.domain.port.ReviewAttemptRepository;
import com.spacedrep.domain.port.ReviewCardRepository;
import com.spacedrep.domain.scheduler.Sm2Scheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService (caso de uso de estudo)")
class ReviewServiceTest {

    private static final long USER_ID = 1L;
    private static final long CARD_ID = 7L;
    private static final long QUESTION_ID = 100L;
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-11-04T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);

    @Mock
    private ReviewCardRepository cards;

    @Mock
    private QuestionRepository questions;

    @Mock
    private ReviewAttemptRepository attempts;

    private ReviewService service;

    @BeforeEach
    void setUp() {
        service = new ReviewService(cards, questions, attempts, new Sm2Scheduler(), FIXED_CLOCK);
    }

    private static Question singleChoiceQuestion() {
        return Question.rehydrate(QUESTION_ID, "IAM", "enunciado", "porque sim", false, List.of(
                new AnswerOption(10L, 1, "correta", true),
                new AnswerOption(11L, 2, "errada", false)));
    }

    private static Question multiChoiceQuestion() {
        return Question.rehydrate(QUESTION_ID, "VPC", "enunciado", "porque sim", true, List.of(
                new AnswerOption(10L, 1, "correta 1", true),
                new AnswerOption(11L, 2, "errada", false),
                new AnswerOption(12L, 3, "correta 2", true)));
    }

    @Test
    @DisplayName("a fila do dia devolve cards com suas questoes e o quanto falta")
    void dueQueueListsCardsWithQuestions() {
        ReviewCard card = ReviewCard.rehydrate(CARD_ID, USER_ID, QUESTION_ID,
                ReviewState.initial(), TODAY, null);
        when(cards.findDue(USER_ID, TODAY, 20)).thenReturn(List.of(card));
        when(cards.countDue(USER_ID, TODAY)).thenReturn(5L);
        when(questions.findById(QUESTION_ID)).thenReturn(Optional.of(singleChoiceQuestion()));

        DueQueue queue = service.getDueQueue(USER_ID, 20);

        assertThat(queue.cards()).hasSize(1);
        assertThat(queue.cards().get(0).question().stem()).isEqualTo("enunciado");
        assertThat(queue.remaining()).isEqualTo(4L);
    }

    @Test
    @DisplayName("limite fora da faixa e normalizado (1..100)")
    void limitIsClamped() {
        when(cards.findDue(eq(USER_ID), eq(TODAY), anyInt())).thenReturn(List.of());
        when(cards.countDue(USER_ID, TODAY)).thenReturn(0L);

        service.getDueQueue(USER_ID, 5_000);

        verify(cards).findDue(USER_ID, TODAY, 100);
    }

    @Test
    @DisplayName("acerto reagenda o card, registra a tentativa e calcula o proximo vencimento")
    void correctAnswerReschedules() {
        ReviewCard card = ReviewCard.rehydrate(CARD_ID, USER_ID, QUESTION_ID,
                ReviewState.initial(), TODAY, null);
        when(cards.findById(CARD_ID)).thenReturn(Optional.of(card));
        when(questions.findById(QUESTION_ID)).thenReturn(Optional.of(singleChoiceQuestion()));
        when(cards.save(any(ReviewCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnswerResult result = service.answer(USER_ID, CARD_ID, new AnswerCommand(Set.of(10L), 3_000));

        assertThat(result.correct()).isTrue();
        assertThat(result.correctOptionIds()).containsExactly(10L);
        assertThat(result.schedule().intervalDays()).isEqualTo(1);
        assertThat(result.schedule().dueDate()).isEqualTo(TODAY.plusDays(1));

        ArgumentCaptor<ReviewAttempt> attempt = ArgumentCaptor.forClass(ReviewAttempt.class);
        verify(attempts).save(attempt.capture());
        assertThat(attempt.getValue().quality()).isEqualTo(ReviewQuality.PERFECT);
        assertThat(attempt.getValue().correct()).isTrue();
        assertThat(attempt.getValue().answeredAt()).isEqualTo(FIXED_CLOCK.instant());
    }

    @Test
    @DisplayName("erro reinicia a sequencia e devolve a explicacao")
    void wrongAnswerResetsSequence() {
        ReviewCard card = ReviewCard.rehydrate(CARD_ID, USER_ID, QUESTION_ID,
                new ReviewState(2.50, 15, 3), TODAY, null);
        when(cards.findById(CARD_ID)).thenReturn(Optional.of(card));
        when(questions.findById(QUESTION_ID)).thenReturn(Optional.of(singleChoiceQuestion()));
        when(cards.save(any(ReviewCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnswerResult result = service.answer(USER_ID, CARD_ID, new AnswerCommand(Set.of(11L), 9_000));

        assertThat(result.correct()).isFalse();
        assertThat(result.explanation()).isEqualTo("porque sim");
        assertThat(result.schedule().repetitions()).isZero();
        assertThat(result.schedule().intervalDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("multi-select parcial vira qualidade 3, nao lapso")
    void partialMultiSelectIsNotLapse() {
        ReviewCard card = ReviewCard.rehydrate(CARD_ID, USER_ID, QUESTION_ID,
                ReviewState.initial(), TODAY, null);
        when(cards.findById(CARD_ID)).thenReturn(Optional.of(card));
        when(questions.findById(QUESTION_ID)).thenReturn(Optional.of(multiChoiceQuestion()));
        when(cards.save(any(ReviewCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.answer(USER_ID, CARD_ID, new AnswerCommand(Set.of(10L), 4_000));

        ArgumentCaptor<ReviewAttempt> attempt = ArgumentCaptor.forClass(ReviewAttempt.class);
        verify(attempts).save(attempt.capture());
        assertThat(attempt.getValue().quality()).isEqualTo(ReviewQuality.CORRECT_WITH_DIFFICULTY);
    }

    @Test
    @DisplayName("card de outro usuario gera 403 e nao toca no banco")
    void otherUsersCardIsForbidden() {
        ReviewCard card = ReviewCard.rehydrate(CARD_ID, 999L, QUESTION_ID,
                ReviewState.initial(), TODAY, null);
        when(cards.findById(CARD_ID)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> service.answer(USER_ID, CARD_ID, new AnswerCommand(Set.of(10L), 1_000)))
                .isInstanceOf(NotCardOwnerException.class);

        verify(cards, never()).save(any());
        verify(attempts, never()).save(any());
    }

    @Test
    @DisplayName("card inexistente gera 404")
    void unknownCardIsNotFound() {
        when(cards.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.answer(USER_ID, CARD_ID, new AnswerCommand(Set.of(10L), 1_000)))
                .isInstanceOf(CardNotFoundException.class);
    }

    @Test
    @DisplayName("alternativa que nao pertence a questao e rejeitada antes de calcular")
    void invalidSelectionIsRejected() {
        ReviewCard card = ReviewCard.rehydrate(CARD_ID, USER_ID, QUESTION_ID,
                ReviewState.initial(), TODAY, null);
        when(cards.findById(CARD_ID)).thenReturn(Optional.of(card));
        when(questions.findById(QUESTION_ID)).thenReturn(Optional.of(singleChoiceQuestion()));

        assertThatThrownBy(() -> service.answer(USER_ID, CARD_ID, new AnswerCommand(Set.of(999L), 1_000)))
                .isInstanceOf(InvalidAnswerException.class);
    }
}
