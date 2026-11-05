package com.spacedrep.domain.model;

/** Alternativa de um rascunho vindo do importador (ainda sem id). */
public record DraftOption(int position, String text, boolean correct) {
}
