package com.spacedrep.domain.scheduler;

import com.spacedrep.domain.model.ReviewQuality;
import com.spacedrep.domain.model.ReviewState;

import java.time.LocalDate;

/**
 * Porta do algoritmo de repeticao espacada.
 *
 * <p>Inverter a dependencia aqui permite trocar SM-2 por FSRS (roadmap M2)
 * sem tocar em servico, controller ou banco.</p>
 */
public interface Scheduler {

    ScheduleOutcome schedule(ReviewState current, ReviewQuality quality, LocalDate today);
}
