package com.spacedrep.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Set;

public record AnswerRequest(

        @NotEmpty(message = "selecione ao menos uma alternativa")
        Set<Long> selectedOptionIds,

        @NotNull(message = "durationMs e obrigatorio")
        @PositiveOrZero(message = "durationMs nao pode ser negativo")
        Integer durationMs) {
}
