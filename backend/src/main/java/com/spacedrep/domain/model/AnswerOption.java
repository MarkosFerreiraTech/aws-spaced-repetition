package com.spacedrep.domain.model;

/** Alternativa de uma questao. */
public record AnswerOption(Long id, int position, String text, boolean correct) {
}
