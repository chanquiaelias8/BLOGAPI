package com.debatiendo.blogapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Traduce el header Authorization: Bearer <token> en un Authentication dentro del
 * SecurityContext.
 *
 * POR QUE UN FILTRO Y NO UN HandlerInterceptor:
 *
 * 1. Momento. Un interceptor de Spring MVC corre dentro del DispatcherServlet, DESPUES
 *    de que la cadena de filtros ya termino. Para entonces authorizeHttpRequests ya
 *    decidio si el request pasaba, y lo habria visto como anonimo: cualquier endpoint
 *    protegido devolveria 401 antes de que el interceptor llegue a autenticar.
 *
 * 2. Alcance. Los filtros son del contenedor de servlets, asi que cubren tambien lo que
 *    no pasa por MVC (recursos estaticos, el dispatch a /error, otros servlets). Un
 *    interceptor solo ve los requests que llegan a un @Controller.
 *
 * 3. Contrato. Spring Security ES una cadena de filtros. Insertar un eslabon mas es
 *    trabajar con el framework; un interceptor seria un mecanismo de autenticacion
 *    paralelo al que el framework no conoce.
 *
 * OncePerRequestFilter y no Filter a secas porque un FORWARD o un dispatch a /error
 * reejecutan la cadena: sin la garantia de "una sola vez" el trabajo se duplicaria.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractBearerToken(request);

        // Sin token seguimos igual: el request continua como anonimo y es
        // authorizeHttpRequests quien decide si eso alcanza. Este filtro autentica,
        // no autoriza; mezclar las dos cosas es lo que termina en 401 donde iba 403.
        if (token == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = jwtService.extractUsername(token);
        if (username != null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(token, userDetails) && userDetails.isEnabled()
                        && userDetails.isAccountNonLocked()) {

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(authentication);
                    SecurityContextHolder.setContext(context);
                }
            } catch (UsernameNotFoundException ex) {
                // Token con firma valida de un usuario que ya no existe. Se ignora
                // en silencio: queda anonimo y responde el AuthenticationEntryPoint.
                logger.debug("Token valido para un usuario inexistente");
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
