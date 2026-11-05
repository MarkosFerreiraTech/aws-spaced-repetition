package com.spacedrep.api.dto;

import java.util.List;

/**
 * Rascunho para revisao humana. Nada foi gravado ainda (ADR-004).
 * O {@code rawTextHash} deve voltar no confirm — e o que impede importar o
 * mesmo simulado duas vezes.
 */
public record ParseImportResponse(List<DraftQuestionDto> questions, String rawTextHash,
                                  int questionCount) {
}
