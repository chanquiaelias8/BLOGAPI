package com.debatiendo.blogapi.security;

import com.debatiendo.blogapi.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responde 403 cuando el usuario esta autenticado pero le falta la authority requerida.
 *
 * La distincion 401 vs 403 no es cosmetica: 401 le dice al cliente "reintenta con
 * credenciales", 403 le dice "no reintentes, no alcanza con quien sos". Confundirlas
 * hace que los clientes entren en loops de refresh de token que nunca van a funcionar.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "No tenes permisos suficientes para esta operacion",
                request.getRequestURI()));
    }
}
