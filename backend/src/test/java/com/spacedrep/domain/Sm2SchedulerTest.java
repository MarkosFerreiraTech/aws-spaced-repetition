package com.spacedrep.domain;

import com.spacedrep.domain.model.ReviewQuality;
import com.spacedrep.domain.model.ReviewState;
import com.spacedrep.domain.scheduler.ScheduleOutcome;
import com.spacedrep.domain.scheduler.Sm2Scheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes do coracao do produto (SPEC secao 4).
 * Escritos ANTES da implementacao: se o algoritmo quebrar, o produto quebra.
 */
@DisplayName("SM-2 Scheduler")
class Sm2SchedulerTest {

    private final Sm2Scheduler scheduler = new Sm2Scheduler();
    private final LocalDate today = LocalDate.of(2026, 11, 4);

    @Nested
    @DisplayName("Primeira revisao")
    class FirstReview {

        @Test
        @DisplayName("acerto rapido: intervalo 1 dia e EF sobe para 2.60")
        void fastCorrectAnswer() {
            ScheduleOutcome outcome = scheduler.schedule(
                    ReviewState.initial(), ReviewQuality.PERFECT, today);

            assertThat(outcome.intervalDays()).isEqualTo(1);
            assertThat(outcome.repetitions()).isEqualTo(1);
            assertThat(outcome.easeFactor()).isEqualTo(2.60);
            assertThat(outcome.dueDate()).isEqualTo(today.plusDays(1));
        }

        @Test
        @DisplayName("acerto normal: EF se mantem em 2.50")
        void normalCorrectAnswer() {
            ScheduleOutcome outcome = scheduler.schedule(
                    ReviewState.initial(), ReviewQuality.CORRECT, today);

            assertThat(outcome.intervalDays()).isEqualTo(1);
            assertThat(outcome.easeFactor()).isEqualTo(2.50);
        }

        @Test
        @DisplayName("erro na primeira revisao: repetitions volta a 0 e prazo e 1 dia")
        void firstAnswerWrong() {
            ScheduleOutcome outcome = scheduler.schedule(
                    ReviewState.initial(), ReviewQuality.INCORRECT, today);

            assertThat(outcome.repetitions()).isZero();
            assertThat(outcome.intervalDays()).isEqualTo(1);
            assertThat(outcome.easeFactor()).isEqualTo(2.18);
        }
    }

    @Nested
    @DisplayName("Sequencia de acertos")
    class SuccessSequence {

        @Test
        @DisplayName("segundo acerto: intervalo salta para 6 dias")
        void secondCorrectAnswer() {
            ScheduleOutcome outcome = scheduler.schedule(
                    new ReviewState(2.50, 1, 1), ReviewQuality.CORRECT, today);

            assertThat(outcome.intervalDays()).isEqualTo(6);
            assertThat(outcome.repetitions()).isEqualTo(2);
        }

        @Test
        @DisplayName("terceiro acerto: intervalo = intervalo anterior x EF")
        void thirdCorrectAnswer() {
            ScheduleOutcome outcome = scheduler.schedule(
                    new ReviewState(2.50, 6, 2), ReviewQuality.CORRECT, today);

            assertThat(outcome.intervalDays()).isEqualTo(15); // round(6 * 2.50)
            assertThat(outcome.repetitions()).isEqualTo(3);
        }

        @Test
        @DisplayName("quartos acertos seguidos crescem exponencialmente")
        void longSequenceGrows() {
            ReviewState state = ReviewState.initial();
            for (int i = 0; i < 4; i++) {
                ScheduleOutcome outcome = scheduler.schedule(state, ReviewQuality.CORRECT, today);
                state = new ReviewState(outcome.easeFactor(), outcome.intervalDays(), outcome.repetitions());
            }

            assertThat(state.repetitions()).isEqualTo(4);
            assertThat(state.intervalDays()).isGreaterThan(30); // 1 -> 6 -> 15 -> 38
        }
    }

    @Nested
    @DisplayName("Lapso (erro depois de acertos)")
    class Lapse {

        @Test
        @DisplayName("erro zera a sequencia e derruba o ease factor")
        void lapseResetsRepetitions() {
            ScheduleOutcome outcome = scheduler.schedule(
                    new ReviewState(2.60, 15, 3), ReviewQuality.INCORRECT, today);

            assertThat(outcome.repetitions()).isZero();
            assertThat(outcome.intervalDays()).isEqualTo(1);
            assertThat(outcome.easeFactor()).isEqualTo(2.28); // 2.60 - 0.32
        }

        @Test
        @DisplayName("ease factor nunca cai abaixo do piso de 1.30")
        void easeFactorHasFloor() {
            ReviewState state = ReviewState.initial();
            for (int i = 0; i < 10; i++) {
                ScheduleOutcome outcome = scheduler.schedule(state, ReviewQuality.INCORRECT, today);
                assertThat(outcome.easeFactor()).isGreaterThanOrEqualTo(ReviewState.MIN_EASE_FACTOR);
                state = new ReviewState(outcome.easeFactor(), outcome.intervalDays(), outcome.repetitions());
            }
            assertThat(state.easeFactor()).isEqualTo(1.30);
        }
    }

    @Nested
    @DisplayName("Acerto fraco")
    class WeakCorrect {

        @Test
        @DisplayName("qualidade 3 conta como acerto e NAO reinicia a sequencia")
        void weakCorrectDoesNotReset() {
            ScheduleOutcome outcome = scheduler.schedule(
                    new ReviewState(2.50, 6, 2), ReviewQuality.CORRECT_WITH_DIFFICULTY, today);

            assertThat(outcome.repetitions()).isEqualTo(3);
            // intervalo = round(6 * EF_atualizado) = round(6 * 2.36) = 14
            assertThat(outcome.intervalDays()).isEqualTo(14);
            assertThat(outcome.easeFactor()).isEqualTo(2.36);
        }

        @Test
        @DisplayName("acerto lento (qualidade 3) encurta o crescimento do intervalo")
        void weakCorrectGrowsSlower() {
            ScheduleOutcome strong = scheduler.schedule(
                    new ReviewState(2.60, 6, 2), ReviewQuality.PERFECT, today);
            ScheduleOutcome weak = scheduler.schedule(
                    new ReviewState(2.60, 6, 2), ReviewQuality.CORRECT_WITH_DIFFICULTY, today);

            assertThat(weak.intervalDays()).isLessThan(strong.intervalDays());
        }
    }

    @Test
    @DisplayName("o vencimento e sempre hoje + intervalo")
    void dueDateIsTodayPlusInterval() {
        ScheduleOutcome outcome = scheduler.schedule(
                new ReviewState(2.50, 6, 2), ReviewQuality.CORRECT, today);

        assertThat(outcome.dueDate()).isEqualTo(today.plusDays(outcome.intervalDays()));
    }
}
