package com.spacedrep.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Questao estruturada mas ainda NAO persistida.
 *
 * <p>Existe porque o importador tem duas etapas: o LLM devolve rascunhos, o usuario
 * revisa, e so depois viram {@link Question} no banco (ADR-004). A validacao aqui
 * roda nas duas etapas — nunca confiar no que vem do LLM nem do navegador.</p>
 */
public record DraftQuestion(String stem, String topic, boolean multiSelect,
                            String explanation, List<DraftOption> options) {

    public DraftQuestion {
        if (stem == null || stem.isBlank()) {
            throw new IllegalArgumentException("enunciado vazio");
        }
        Objects.requireNonNull(options, "options e obrigatorio");
        if (options.size() < 2) {
            throw new IllegalArgumentException("questao precisa de ao menos 2 alternativas");
        }
        long correct = options.stream().filter(DraftOption::correct).count();
        if (correct == 0) {
            throw new IllegalArgumentException("questao sem alternativa correta");
        }
        if (!multiSelect && correct != 1) {
            throw new IllegalArgumentException(
                    "questao de resposta unica deve ter exatamente 1 alternativa correta");
        }
        if (multiSelect && correct < 2) {
            throw new IllegalArgumentException(
                    "questao multi-resposta deve ter ao menos 2 alternativas corretas");
        }
        boolean hasBlank = options.stream().anyMatch(o -> o.text() == null || o.text().isBlank());
        if (hasBlank) {
            throw new IllegalArgumentException("alternativa vazia");
        }
    }

    public List<DraftOption> correctOptions() {
        return options.stream().filter(DraftOption::correct).toList();
    }
}
