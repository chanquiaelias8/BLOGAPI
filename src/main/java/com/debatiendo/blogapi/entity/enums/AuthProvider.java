package com.debatiendo.blogapi.entity.enums;

/**
 * Origen de la credencial del usuario.
 * LOCAL  -> se autentica con username + password (BCrypt).
 * GOOGLE -> se autentica por OAuth2; no tiene password en nuestra base.
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE
}
