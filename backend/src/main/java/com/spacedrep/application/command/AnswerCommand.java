package com.spacedrep.application.command;

import java.util.Set;

/** Intencao de responder um card. */
public record AnswerCommand(Set<Long> selectedOptionIds, int durationMs) {
}
