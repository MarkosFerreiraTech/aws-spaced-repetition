package com.spacedrep.domain.scheduler;

import com.spacedrep.domain.model.ReviewQuality;
import com.spacedrep.domain.model.ReviewState;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Implementacao do SM-2 (SuperMemo 2, Piotr Wozniak).
 *
 * <pre>
 * EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)), com piso em 1.30
 * q &lt; 3  -> lapse: repetitions = 0, interval = 1
 * q &gt;= 3 -> repetitions += 1
 *            interval = 1 se repetitions == 1
 *                     = 6 se repetitions == 2
 *                     = round(interval * EF') se repetitions &gt;= 3
 * </pre>
 *
 * <p>Classe pura (sem anotacao de framework): registrada como bean em
 * {@code infrastructure.config.SchedulerConfig}.</p>
 */
public final class Sm2Scheduler implements Scheduler {

    static final int FIRST_INTERVAL_DAYS = 1;
    static final int SECOND_INTERVAL_DAYS = 6;
    static final int LAPSE_INTERVAL_DAYS = 1;

    @Override
    public ScheduleOutcome schedule(ReviewState current, ReviewQuality quality, LocalDate today) {
        Objects.requireNonNull(current, "current state is required");
        Objects.requireNonNull(quality, "quality is required");
        Objects.requireNonNull(today, "today is required");

        double easeFactor = roundToHundredths(
                Math.max(ReviewState.MIN_EASE_FACTOR, nextEaseFactor(current.easeFactor(), quality)));

        if (quality.isLapse()) {
            return new ScheduleOutcome(
                    LAPSE_INTERVAL_DAYS, easeFactor, 0, today.plusDays(LAPSE_INTERVAL_DAYS));
        }

        int repetitions = current.repetitions() + 1;
        int interval = switch (repetitions) {
            case 1 -> FIRST_INTERVAL_DAYS;
            case 2 -> SECOND_INTERVAL_DAYS;
            default -> Math.max(SECOND_INTERVAL_DAYS,
                    (int) Math.round(current.intervalDays() * easeFactor));
        };

        return new ScheduleOutcome(interval, easeFactor, repetitions, today.plusDays(interval));
    }

    private static double nextEaseFactor(double current, ReviewQuality quality) {
        int delta = 5 - quality.value();
        return current + (0.1 - delta * (0.08 + delta * 0.02));
    }

    private static double roundToHundredths(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
