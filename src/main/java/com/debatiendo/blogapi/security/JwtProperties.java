package com.debatiendo.blogapi.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuracion del emisor de tokens, bajo el prefijo app.jwt.
 *
 * Se modela como @ConfigurationProperties y no con @Value sueltos para que la validacion
 * corra al arrancar: si falta el secreto, la aplicacion no levanta en vez de fallar
 * en el primer login.
 *
 * @param secret       clave HMAC en Base64. Debe decodificar a >= 32 bytes (256 bits) para HS256.
 * @param issuer       claim "iss"; se exige al validar, para que un token emitido por otro
 *                     sistema que comparta el secreto por accidente no sea aceptado aca.
 * @param accessTokenTtl vida del access token.
 */
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public record JwtProperties(
        @NotBlank String secret,
        @NotBlank String issuer,
        @NotNull Duration accessTokenTtl
) {
}
