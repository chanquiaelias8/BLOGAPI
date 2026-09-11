package com.debatiendo.blogapi.common;

/**
 * Nombres de rol tal como se guardan en la tabla roles (sin prefijo).
 * El prefijo ROLE_ lo agrega la capa de seguridad al construir las authorities.
 */
public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String AUTHOR = "AUTHOR";
    public static final String USER = "USER";

    /** Roles que recibe automaticamente toda cuenta creada por /auth/register. */
    public static final String[] DEFAULT_SIGNUP_ROLES = {USER, AUTHOR};

    private Roles() {
    }
}
