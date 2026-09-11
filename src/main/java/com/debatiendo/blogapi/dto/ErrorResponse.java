package com.debatiendo.blogapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Cuerpo uniforme de error para toda la API.
 * Los mensajes son deliberadamente genericos en los casos de seguridad: no se filtra
 * si un usuario existe, ni el motivo exacto por el que un token fue rechazado.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null);
    }

    public static ErrorResponse validation(int status, String message, String path,
                                           Map<String, String> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, "Bad Request", message, path, fieldErrors);
    }
}
