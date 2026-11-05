package com.spacedrep.domain;

import com.spacedrep.domain.model.DraftOption;
import com.spacedrep.domain.model.DraftQuestion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Validacao do rascunho vindo do LLM")
class DraftQuestionTest {

    private static DraftOption correct(int pos) {
        return new DraftOption(pos, "correta", true);
    }

    private static DraftOption wrong(int pos) {
        return new DraftOption(pos, "errada", false);
    }

    @Test
    @DisplayName("aceita questao de resposta unica com 1 correta")
    void validSingleChoice() {
        DraftQuestion draft = new DraftQuestion("enunciado", "IAM", false, "exp",
                List.of(correct(1), wrong(2)));

        assertThat(draft.correctOptions()).hasSize(1);
    }

    @Test
    @DisplayName("aceita multi-resposta com 2 corretas")
    void validMultiSelect() {
        DraftQuestion draft = new DraftQuestion("enunciado", "VPC", true, "exp",
                List.of(correct(1), wrong(2), correct(3)));

        assertThat(draft.correctOptions()).hasSize(2);
    }

    @Test
    @DisplayName("rejeita questao sem alternativa correta")
    void rejectsWithoutCorrectOption() {
        assertThatThrownBy(() -> new DraftQuestion("enunciado", "IAM", false, null,
                List.of(wrong(1), wrong(2))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sem alternativa correta");
    }

    @Test
    @DisplayName("rejeita resposta unica com duas corretas")
    void rejectsAmbiguousSingleChoice() {
        assertThatThrownBy(() -> new DraftQuestion("enunciado", "IAM", false, null,
                List.of(correct(1), correct(2))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exatamente 1");
    }

    @Test
    @DisplayName("rejeita multi-resposta com apenas 1 correta")
    void rejectsFakeMultiSelect() {
        assertThatThrownBy(() -> new DraftQuestion("enunciado", "VPC", true, null,
                List.of(correct(1), wrong(2))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ao menos 2");
    }

    @Test
    @DisplayName("rejeita alternativa vazia e enunciado vazio")
    void rejectsBlankContent() {
        assertThatThrownBy(() -> new DraftQuestion("  ", "IAM", false, null,
                List.of(correct(1), wrong(2))))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new DraftQuestion("enunciado", "IAM", false, null,
                List.of(correct(1), new DraftOption(2, "  ", false))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
