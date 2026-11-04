package com.spacedrep.domain;

import com.spacedrep.domain.model.AnswerOption;
import com.spacedrep.domain.model.Question;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Regras de correcao da questao")
class QuestionTest {

    private static Question single() {
        return Question.rehydrate(1L, "IAM", "enunciado", "explicacao", false, List.of(
                new AnswerOption(10L, 1, "A", true),
                new AnswerOption(11L, 2, "B", false)));
    }

    private static Question multi() {
        return Question.rehydrate(2L, "VPC", "enunciado", "explicacao", true, List.of(
                new AnswerOption(20L, 1, "A", true),
                new AnswerOption(21L, 2, "B", false),
                new AnswerOption(22L, 3, "C", true)));
    }

    @Test
    @DisplayName("acerta quando seleciona exatamente as corretas")
    void correctExactSelection() {
        assertThat(single().isCorrect(Set.of(10L))).isTrue();
        assertThat(multi().isCorrect(Set.of(20L, 22L))).isTrue();
    }

    @Test
    @DisplayName("erra quando marca a correta junto com uma errada")
    void wrongWhenInvalidOptionSelected() {
        assertThat(single().isCorrect(Set.of(10L, 11L))).isFalse();
    }

    @Test
    @DisplayName("multi-select incompleto nao e acerto, mas conta como parcial")
    void partialMultiSelect() {
        Question question = multi();

        assertThat(question.isCorrect(Set.of(20L))).isFalse();
        assertThat(question.isPartialSelection(Set.of(20L))).isTrue();
    }

    @Test
    @DisplayName("marcar uma errada invalida o credito parcial")
    void partialRequiresOnlyCorrectOptions() {
        assertThat(multi().isPartialSelection(Set.of(20L, 21L))).isFalse();
    }

    @Test
    @DisplayName("questao de resposta unica nunca concede credito parcial")
    void singleSelectNeverPartial() {
        assertThat(single().isPartialSelection(Set.of())).isFalse();
    }

    @Test
    @DisplayName("selecao com alternativa inexistente e invalida")
    void unknownOptionIsInvalid() {
        assertThat(single().isValidSelection(Set.of(999L))).isFalse();
        assertThat(single().isValidSelection(Set.of())).isFalse();
        assertThat(single().isValidSelection(null)).isFalse();
    }

    @Test
    @DisplayName("questao sem alternativa correta nao pode ser criada")
    void questionRequiresCorrectOption() {
        List<AnswerOption> allWrong = List.of(
                new AnswerOption(1L, 1, "A", false),
                new AnswerOption(2L, 2, "B", false));

        assertThatThrownBy(() -> Question.rehydrate(3L, "S3", "x", "y", false, allWrong))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
