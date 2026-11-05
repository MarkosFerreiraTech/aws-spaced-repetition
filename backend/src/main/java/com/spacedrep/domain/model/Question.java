package com.spacedrep.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Questao de simulado com suas alternativas.
 *
 * <p>Regras de correcao ficam aqui: o servico nao sabe comparar alternativas.</p>
 */
public final class Question {

    private final Long id;
    private final String topic;
    private final String stem;
    private final String explanation;
    private final boolean multiSelect;
    private final List<AnswerOption> options;

    private Question(Long id, String topic, String stem, String explanation,
                     boolean multiSelect, List<AnswerOption> options) {
        this.id = id;
        this.topic = topic;
        this.stem = Objects.requireNonNull(stem, "stem is required");
        this.explanation = explanation;
        this.multiSelect = multiSelect;
        this.options = List.copyOf(options);
        if (this.options.size() < 2) {
            throw new IllegalArgumentException("a question needs at least 2 options");
        }
        // Conta direto nas alternativas: um rascunho vindo do importador ainda
        // nao tem ids (a questao so e persistida depois), e validar por
        // correctOptionIds() amarraria o dominio a identidade de persistencia.
        long correctCount = this.options.stream().filter(AnswerOption::correct).count();
        if (correctCount == 0) {
            throw new IllegalArgumentException("a question needs at least 1 correct option");
        }
    }

    public static Question rehydrate(Long id, String topic, String stem, String explanation,
                                     boolean multiSelect, List<AnswerOption> options) {
        return new Question(id, topic, stem, explanation, multiSelect, options);
    }

    public Set<Long> correctOptionIds() {
        return options.stream()
                .filter(AnswerOption::correct)
                .map(AnswerOption::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Correcao: o conjunto selecionado precisa ser exatamente o conjunto correto. */
    public boolean isCorrect(Set<Long> selectedOptionIds) {
        return correctOptionIds().equals(Set.copyOf(selectedOptionIds));
    }

    /** Acertou parte das corretas, sem marcar nenhuma errada (so faz sentido em multi-select). */
    public boolean isPartialSelection(Set<Long> selectedOptionIds) {
        if (!multiSelect || selectedOptionIds.isEmpty()) {
            return false;
        }
        Set<Long> correct = correctOptionIds();
        Set<Long> selected = Set.copyOf(selectedOptionIds);
        return correct.containsAll(selected) && !selected.containsAll(correct);
    }

    /** Uma selecao so e valida se nao estiver vazia e usar alternativas da propria questao. */
    public boolean isValidSelection(Set<Long> selectedOptionIds) {
        if (selectedOptionIds == null || selectedOptionIds.isEmpty()) {
            return false;
        }
        Set<Long> known = options.stream().map(AnswerOption::id).collect(Collectors.toSet());
        return known.containsAll(selectedOptionIds);
    }

    public Long id() {
        return id;
    }

    public String topic() {
        return topic;
    }

    public String stem() {
        return stem;
    }

    public String explanation() {
        return explanation;
    }

    public boolean multiSelect() {
        return multiSelect;
    }

    public List<AnswerOption> options() {
        return options;
    }
}
