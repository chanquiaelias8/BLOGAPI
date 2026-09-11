package com.debatiendo.blogapi.dto;

import java.util.Set;

/**
 * Respuesta de /auth/login.
 *
 * Se devuelven las authorities para que el front pueda mostrar u ocultar acciones sin
 * tener que adivinarlas. No son la fuente de autorizacion: el backend las recalcula
 * desde la base en cada request. Confiar en este campo del lado del cliente para
 * decidir accesos seria el clasico control de acceso del lado del cliente.
 *
 * @param expiresIn segundos hasta la expiracion, para que el cliente sepa cuando renovar.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String username,
        Set<String> authorities
) {

    public static AuthResponse bearer(String token, long expiresIn, String username,
                                      Set<String> authorities) {
        return new AuthResponse(token, "Bearer", expiresIn, username, authorities);
    }
}
