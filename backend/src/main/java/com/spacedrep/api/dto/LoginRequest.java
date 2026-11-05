package com.spacedrep.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email(message = "e-mail invalido") String email,
        @NotBlank(message = "senha obrigatoria") String password) {
}
