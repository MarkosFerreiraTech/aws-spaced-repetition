package com.spacedrep.domain.scheduler;

import java.time.LocalDate;

/** Resultado do calculo de agendamento para um card. */
public record ScheduleOutcome(int intervalDays, double easeFactor, int repetitions, LocalDate dueDate) {
}
