package com.debatiendo.blogapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Emision y validacion de access tokens (HS256).
 *
 * DECISION: el token NO transporta roles ni permisos, solo el subject y el id de usuario.
 *
 * Meter las authorities adentro del JWT lo vuelve 100% stateless, pero tiene un costo que
 * en un contexto bancario no se paga: si a un usuario se le revoca un permiso o se le
 * bloquea la cuenta, el token viejo sigue siendo criptograficamente valido y sigue
 * autorizando hasta que expira. Con las authorities cargadas desde la base en cada
 * request, la revocacion es inmediata y ademas el token se mantiene chico.
 *
 * El costo es un SELECT por request; esta acotado por el EntityGraph de UserRepository
 * (una sola query) y es el candidato natural a una cache si alguna vez molesta.
 */
@Service
@Slf4j
public class JwtService {

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        byte[] keyBytes = Decoders.BASE64.decode(properties.secret());
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret debe decodificar a al menos 32 bytes para HS256; decodifico "
                            + keyBytes.length + " bytes");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.properties = properties;
    }

    public String generateAccessToken(SecurityUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())          // jti: identificador unico del token
                .subject(user.getUsername())
                .claim("uid", user.getId().toString())     // para trazabilidad en logs y auditoria
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtl())))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public long getAccessTokenTtlSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    /** Devuelve el subject, o null si el token es invalido, expirado o esta manipulado. */
    public String extractUsername(String token) {
        Claims claims = parseClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * La verificacion de firma, expiracion e issuer la hace parseClaims: si algo falla,
     * jjwt tira excepcion. Aca solo queda comprobar que el token corresponde al usuario
     * que efectivamente cargamos de la base.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username != null && username.equals(userDetails.getUsername());
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)   // parseSignedClaims rechaza tokens sin firma (alg: none)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            // A nivel DEBUG: un token invalido es un evento esperable, no un error del sistema.
            log.debug("Token JWT rechazado: {}", ex.getMessage());
            return null;
        }
    }
}
