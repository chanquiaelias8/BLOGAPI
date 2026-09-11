package com.debatiendo.blogapi.config;

import com.debatiendo.blogapi.security.AppUserDetailsService;
import com.debatiendo.blogapi.security.JwtAuthenticationFilter;
import com.debatiendo.blogapi.security.JwtProperties;
import com.debatiendo.blogapi.security.RestAccessDeniedHandler;
import com.debatiendo.blogapi.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
// prePostEnabled viene en true por defecto: habilita @PreAuthorize / @PostAuthorize.
// Es lo que permite que la autorizacion viva en anotaciones y no en ifs dentro del metodo.
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AppUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // --- 1. CSRF ---
                // Se desactiva porque el ataque CSRF depende de que el navegador adjunte la
                // credencial solo: cookie de sesion, Basic guardado. Un JWT en el header
                // Authorization lo pone el JavaScript del cliente a mano, asi que un form
                // malicioso en otro dominio no puede incluirlo.
                // Esto deja de ser cierto si algun dia guardamos el token en una cookie:
                // en ese caso CSRF vuelve a hacer falta.
                .csrf(AbstractHttpConfigurer::disable)

                // --- 2. CORS ---
                // Habilitado y acotado por configuracion. Sin esto el front (otro origen)
                // no puede leer las respuestas ni mandar el header Authorization.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // --- 3. Sesiones ---
                // STATELESS: no se crea ni se consulta HttpSession, y no se emite JSESSIONID.
                // Cada request se autentica sola con su token. Es lo que permite escalar
                // horizontalmente sin sesion compartida entre instancias.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // --- 4. Respuestas de error ---
                // Sin esto, un request sin token recibe un redirect al formulario de login.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                // --- 5. Headers de seguridad ---
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'")))

                // --- 6. Reglas de acceso por URL ---
                // Grano grueso a proposito. El grano fino (que permiso hace falta para cada
                // operacion, si el post es propio o ajeno) vive en @PreAuthorize sobre el
                // metodo del controller: ahi esta el contexto para decidirlo.
                // Las reglas se evaluan EN ORDEN, asi que anyRequest va siempre ultimo.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/register").permitAll()
                        // Callback de OAuth2; se completa en la proxima entrega.
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Todo lo demas exige autenticacion, tal como pide la consigna.
                        .anyRequest().authenticated())

                // --- 7. El filtro JWT ---
                // Antes de UsernamePasswordAuthenticationFilter, que es el que procesa el
                // login por formulario. Si el token ya autentico el request, ese filtro no
                // tiene nada que hacer; si no hay token, el contexto queda vacio y la
                // decision cae en authorizeHttpRequests.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * DaoAuthenticationProvider es el que usa /auth/login: carga el usuario con el
     * UserDetailsService y compara la password con el PasswordEncoder.
     *
     * Se declara explicito (y no se deja al autoconfig) para dejar a la vista que el
     * login local es UN metodo de autenticacion entre varios: cuando entre OAuth2, se
     * suma otro provider a este mismo ProviderManager.
     */
    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
        // Desde Spring Security 6.4 el UserDetailsService va por constructor: el setter
        // permitia dejar el provider a medio configurar y quedo deprecado.
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        // Con true, un usuario inexistente produce BadCredentialsException igual que una
        // password incorrecta, y el provider ejecuta un hash dummy para igualar los tiempos.
        // Sin eso, la diferencia de latencia permite enumerar usuarios validos.
        provider.setHideUserNotFoundExceptions(true);
        return new ProviderManager(provider);
    }

    /**
     * BCrypt con strength 12 en vez del default 10: son 4x mas iteraciones, del orden de
     * 250ms por hash en hardware actual. Es un costo despreciable en un login y encarece
     * mucho un ataque de fuerza bruta offline si alguna vez se filtra la tabla.
     * El costo queda embebido en el hash, asi que subirlo despues no invalida los viejos.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
