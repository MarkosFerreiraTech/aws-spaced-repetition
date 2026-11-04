package com.spacedrep.api.dto;

/** Alternativa exposta ao cliente. Nunca revela qual e a correta antes da resposta. */
public record OptionResponse(Long id, int position, String text) {
}
