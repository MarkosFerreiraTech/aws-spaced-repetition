package com.spacedrep.application.view;

import com.spacedrep.domain.scheduler.ScheduleOutcome;

import java.util.Set;

/** Resultado de uma resposta, ja com o reagendamento calculado. */
public record AnswerResult(boolean correct, String explanation, Set<Long> correctOptionIds,
                           ScheduleOutcome schedule) {
}
