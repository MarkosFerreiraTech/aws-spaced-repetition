package com.spacedrep.domain;

import com.spacedrep.domain.model.ReviewQuality;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Mapeamento resposta -> qualidade SM-2")
class ReviewQualityTest {

    @Test
    @DisplayName("erro em questao de resposta unica sempre vira lapso (q < 3)")
    void wrongAnswerIsLapse() {
        assertThat(ReviewQuality.evaluate(false, false, 3_000)).isEqualTo(ReviewQuality.INCORRECT);
        assertThat(ReviewQuality.evaluate(false, false, 3_000).isLapse()).isTrue();
    }

    @Test
    @DisplayName("erro parcial em multi-resposta NAO e lapso: conta como acerto fraco")
    void partialWrongIsWeakNotLapse() {
        ReviewQuality quality = ReviewQuality.evaluate(false, true, 3_000);

        assertThat(quality).isEqualTo(ReviewQuality.CORRECT_WITH_DIFFICULTY);
        assertThat(quality.isLapse()).isFalse();
    }

    @Test
    @DisplayName("acerto rapido vira qualidade maxima")
    void fastAnswerIsPerfect() {
        assertThat(ReviewQuality.evaluate(true, false, 10_000)).isEqualTo(ReviewQuality.PERFECT);
    }

    @Test
    @DisplayName("acerto no tempo normal vira qualidade 4")
    void normalAnswer() {
        assertThat(ReviewQuality.evaluate(true, false, 30_000)).isEqualTo(ReviewQuality.CORRECT);
    }

    @Test
    @DisplayName("acerto lento NAO vira lapso, apenas acerto fraco")
    void slowAnswerIsWeakNotLapse() {
        ReviewQuality quality = ReviewQuality.evaluate(true, false, 90_000);

        assertThat(quality).isEqualTo(ReviewQuality.CORRECT_WITH_DIFFICULTY);
        assertThat(quality.isLapse()).isFalse();
    }

    @Test
    @DisplayName("multi-select parcial e acerto fraco mesmo se rapido")
    void partialMultiSelectIsWeak() {
        assertThat(ReviewQuality.evaluate(true, true, 5_000))
                .isEqualTo(ReviewQuality.CORRECT_WITH_DIFFICULTY);
    }
}
