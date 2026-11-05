package com.spacedrep.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ConfirmImportRequest(

        @NotBlank @Size(max = 200) String sourceName,

        @NotBlank @Size(min = 64, max = 64, message = "rawTextHash deve ser SHA-256 (64 hex)")
        String rawTextHash,

        @NotEmpty(message = "nenhuma questao para importar")
        @Size(max = 500, message = "maximo de 500 questoes por importacao")
        List<@Valid DraftQuestionDto> questions) {
}
