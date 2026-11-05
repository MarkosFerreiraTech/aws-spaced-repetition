package com.spacedrep.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParseImportRequest(

        @NotBlank(message = "informe um nome para o simulado")
        @Size(max = 200)
        String sourceName,

        @NotBlank(message = "cole o texto do simulado")
        @Size(max = 100_000, message = "texto excede 100.000 caracteres")
        String rawText) {
}
