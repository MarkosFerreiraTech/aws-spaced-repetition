package com.spacedrep.domain.model;

/** Estado de agendamento de um card (valor imutavel). */
public record ReviewState(double easeFactor, int intervalDays, int repetitions) {

    public static final double INITIAL_EASE_FACTOR = 2.50;
    public static final double MIN_EASE_FACTOR = 1.30;

    public ReviewState {
        if (easeFactor < MIN_EASE_FACTOR) {
            throw new IllegalArgumentException("easeFactor must be >= " + MIN_EASE_FACTOR);
        }
        if (intervalDays < 0) {
            throw new IllegalArgumentException("intervalDays must be >= 0");
        }
        if (repetitions < 0) {
            throw new IllegalArgumentException("repetitions must be >= 0");
        }
    }

    public static ReviewState initial() {
        return new ReviewState(INITIAL_EASE_FACTOR, 0, 0);
    }
}
