package com.spacedrep.domain.model;

/**
 * Qualidade da resposta na escala SM-2 (0 a 5).
 *
 * <p>O mapeamento resposta -> qualidade e regra de negocio (SPEC secao 4),
 * por isso vive no dominio e nao no controller.</p>
 */
public enum ReviewQuality {

    BLACKOUT(0),
    INCORRECT_HARD(1),
    INCORRECT(2),
    CORRECT_WITH_DIFFICULTY(3),
    CORRECT(4),
    PERFECT(5);

    /** Abaixo disso a resposta e considerada lapso (reinicia a repeticao). */
    private static final int LAPSE_THRESHOLD = 3;
    private static final int FAST_ANSWER_MS = 15_000;
    private static final int SLOW_ANSWER_MS = 60_000;

    private final int value;

    ReviewQuality(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public boolean isLapse() {
        return value < LAPSE_THRESHOLD;
    }

    /**
     * Converte a resposta do usuario em qualidade SM-2.
     *
     * @param correct            acertou todas as alternativas corretas
     * @param partialMultiSelect acertou apenas parte de uma questao multi-resposta
     * @param durationMs         tempo de resposta em milissegundos
     */
    public static ReviewQuality evaluate(boolean correct, boolean partialMultiSelect, int durationMs) {
        // Decisao de produto (SPEC secao 4): em multi-resposta, acertar PARTE das
        // corretas e progresso, nao lapso — zera-la puniria quem esta quase la.
        if (partialMultiSelect) {
            return CORRECT_WITH_DIFFICULTY;
        }
        if (!correct) {
            return INCORRECT;
        }
        if (durationMs < FAST_ANSWER_MS) {
            return PERFECT;
        }
        if (durationMs > SLOW_ANSWER_MS) {
            return CORRECT_WITH_DIFFICULTY;
        }
        return CORRECT;
    }
}
