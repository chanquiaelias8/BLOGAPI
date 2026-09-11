package com.debatiendo.blogapi.controller;

import com.debatiendo.blogapi.dto.AuthResponse;
import com.debatiendo.blogapi.dto.LoginRequest;
import com.debatiendo.blogapi.dto.RegisterRequest;
import com.debatiendo.blogapi.dto.UserResponse;
import com.debatiendo.blogapi.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unicos endpoints publicos de la API.
 * No llevan @PreAuthorize porque su acceso ya esta resuelto en la SecurityFilterChain
 * con permitAll: poner las dos cosas duplicaria la regla en dos lugares.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Devuelve 201 con el usuario creado, no un token: registrarse y autenticarse son
     * operaciones distintas. El cliente hace login despues, que ademas es el flujo que
     * vamos a ejercitar en la coleccion de Postman.
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }
}
