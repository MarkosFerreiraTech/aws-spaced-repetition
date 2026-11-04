package com.spacedrep.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Card da fila do dia.
 *
 * <p>Decisao de seguranca/UX: o campo {@code correct} das alternativas NAO sai aqui.
 * O cliente so descobre a resposta depois de responder.</p>
 */
public record CardResponse(Long cardId, Long questionId, String topic, String stem,
                           boolean multiSelect, List<OptionResponse> options,
                           int repetitions, int intervalDays, LocalDate dueDate) {
}
