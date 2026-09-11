package com.debatiendo.blogapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta publica de cuenta.
 *
 * Notar que NO hay campo roles. Es a proposito: si el cliente pudiera mandar roles,
 * cualquiera se registraria como ADMIN. Los roles los fija el servidor
 * (Roles.DEFAULT_SIGNUP_ROLES = USER + AUTHOR). Para crear un usuario con roles
 * arbitrarios esta POST /api/users, protegido con USER_CREATE.
 */
public record RegisterRequest(
        @NotBlank(message = "El username es obligatorio")
        @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                message = "El username solo admite letras, numeros, punto, guion y guion bajo")
        String username,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato valido")
        @Size(max = 150)
        String email,

        @NotBlank(message = "La password es obligatoria")
        @Size(min = 8, max = 72, message = "La password debe tener entre 8 y 72 caracteres")
        String password,

        @NotBlank(message = "El nombre para mostrar es obligatorio")
        @Size(max = 120)
        String nombre,

        @Size(max = 2000)
        String bio
) {
}
