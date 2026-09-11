package com.debatiendo.blogapi.dto;

import jakarta.validation.constraints.NotBlank;

/** Acepta username o email en el campo login. */
public record LoginRequest(
        @NotBlank(message = "El login es obligatorio")
        String login,

        @NotBlank(message = "La password es obligatoria")
        String password
) {
}
