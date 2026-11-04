package com.spacedrep.api.dto;

import java.time.LocalDate;
import java.util.Set;

/** Feedback apos responder: correcao + explicacao + novo agendamento. */
public record AnswerResponse(boolean correct, Set<Long> correctOptionIds, String explanation,
                             LocalDate nextDueDate, int intervalDays, double easeFactor,
                             int repetitions) {
}
