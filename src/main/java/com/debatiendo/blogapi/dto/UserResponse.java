package com.debatiendo.blogapi.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Vista publica de un usuario. Nunca expone la password, ni siquiera hasheada:
 * un hash BCrypt filtrado sigue siendo material para un ataque offline.
 */
public record UserResponse(
        UUID id,
        String username,
        String email,
        boolean enabled,
        String provider,
        Set<String> roles,
        Instant createdAt
) {
}
