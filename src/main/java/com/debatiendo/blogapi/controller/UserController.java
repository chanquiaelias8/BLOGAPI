package com.debatiendo.blogapi.controller;

import com.debatiendo.blogapi.dto.UserResponse;
import com.debatiendo.blogapi.security.SecurityUser;
import com.debatiendo.blogapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Autorizacion por anotacion, no por if dentro del metodo.
     *
     * hasAuthority y no hasRole: la regla es "hace falta poder leer usuarios", no "hace
     * falta ser ADMIN". Si manana un rol AUDITOR necesita leerlos, se le agrega el
     * permiso en la base y este codigo no se toca ni se redespliega.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<List<UserResponse>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    /**
     * Datos del usuario autenticado. Solo pide estar autenticado: no hay decision de
     * autorizacion que tomar, porque cada uno accede exclusivamente a lo suyo.
     * El principal lo inyecta Spring Security desde el SecurityContext que dejo el filtro.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(userService.findById(principal.getId()));
    }
}
