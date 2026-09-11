package com.debatiendo.blogapi;

import com.debatiendo.blogapi.dto.LoginRequest;
import com.debatiendo.blogapi.dto.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recorre la cadena real: filtro JWT -> AuthenticationManager -> UserDetailsService ->
 * authorities desde la base -> @PreAuthorize. Sin mocks de seguridad, para que el test
 * falle si alguno de esos eslabones se rompe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PASSWORD = "password-segura-123";
    private String username;

    @BeforeEach
    void setUp() {
        username = "autor" + System.nanoTime();
    }

    private void register(String user, String email) throws Exception {
        RegisterRequest request = new RegisterRequest(
                user, email, PASSWORD, "Nombre Visible", "Bio de prueba");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private String loginAndGetToken(String login) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(login, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    @Test
    @DisplayName("El registro crea la cuenta, nunca devuelve la password y asigna USER + AUTHOR")
    void registroAsignaRolesFijos() throws Exception {
        RegisterRequest request = new RegisterRequest(
                username, username + "@test.com", PASSWORD, "Nombre Visible", null);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.provider").value("LOCAL"))
                .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.containsInAnyOrder("USER", "AUTHOR")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("Un username repetido da 409, no 500")
    void registroDuplicadoDevuelve409() throws Exception {
        register(username, username + "@test.com");

        RegisterRequest duplicado = new RegisterRequest(
                username, "otro" + username + "@test.com", PASSWORD, "Otro", null);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicado)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("La validacion de Bean Validation devuelve 400 con el detalle por campo")
    void passwordCortaDevuelve400ConFieldErrors() throws Exception {
        RegisterRequest invalido = new RegisterRequest(
                username, "no-es-un-email", "corta", "Nombre", null);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("El login emite un JWT con las authorities combinadas de rol + permisos")
    void loginDevuelveTokenYAuthorities() throws Exception {
        register(username, username + "@test.com");

        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn().getResponse().getContentAsString();

        JsonNode authorities = objectMapper.readTree(body).get("authorities");
        assertThat(authorities).isNotNull();
        assertThat(authorities.toString())
                .contains("ROLE_USER")
                .contains("ROLE_AUTHOR")
                .contains("POST_CREATE")
                .contains("POST_UPDATE")
                .contains("AUTHOR_READ");

        // El token es un JWT de tres partes y no filtra nada legible de mas.
        String token = objectMapper.readTree(body).get("accessToken").asText();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Se puede entrar con el email en vez del username")
    void loginConEmailFunciona() throws Exception {
        String email = username + "@test.com";
        register(username, email);
        assertThat(loginAndGetToken(email)).isNotBlank();
    }

    @Test
    @DisplayName("Password incorrecta da 401 con mensaje generico, sin revelar si el usuario existe")
    void passwordIncorrectaDevuelve401Generico() throws Exception {
        register(username, username + "@test.com");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(username, "password-incorrecta"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales invalidas"));
    }

    @Test
    @DisplayName("Un usuario inexistente devuelve exactamente la misma respuesta que una password mala")
    void usuarioInexistenteNoSeDistingue() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("no-existe-nadie", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales invalidas"));
    }

    @Test
    @DisplayName("Sin token, un endpoint protegido responde 401 en JSON y no redirige al login")
    void sinTokenDevuelve401Json() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Editar el payload para hacerse pasar por admin invalida la firma: 401")
    void payloadManipuladoDevuelve401() throws Exception {
        register(username, username + "@test.com");
        String[] parts = loginAndGetToken(username).split("\\.");

        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String manipulado = payload.replace("\"sub\":\"" + username + "\"", "\"sub\":\"admin\"");
        assertThat(manipulado).isNotEqualTo(payload);

        String forjado = parts[0] + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(manipulado.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + forjado))
                .andExpect(status().isUnauthorized());
    }

    /**
     * El ataque "alg: none": el atacante reemplaza el algoritmo por none y borra la firma,
     * confiando en que la libreria acepte un token sin verificar. Fue una vulnerabilidad
     * real y extendida en implementaciones de JWT. parseSignedClaims lo rechaza, pero el
     * test lo fija para que nadie lo afloje despues sin darse cuenta.
     */
    @Test
    @DisplayName("Un token sin firma (alg: none) se rechaza con 401")
    void tokenSinFirmaDevuelve401() throws Exception {
        register(username, username + "@test.com");
        String[] parts = loginAndGetToken(username).split("\\.");

        String header = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String sinFirma = header + "." + parts[1] + ".";

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + sinFirma))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un token firmado con otra clave se rechaza con 401")
    void tokenConOtraClaveDevuelve401() throws Exception {
        register(username, username + "@test.com");
        String[] parts = loginAndGetToken(username).split("\\.");

        SecretKey claveAjena = Keys.hmacShaKeyFor(
                "una-clave-completamente-distinta-de-32-bytes".getBytes(StandardCharsets.UTF_8));
        String ajeno = Jwts.builder()
                .subject(username)
                .issuer("debatiendo-blog-api-test")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(claveAjena, Jwts.SIG.HS256)
                .compact();

        assertThat(ajeno.split("\\.")[2]).isNotEqualTo(parts[2]);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + ajeno))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Con token valido, /api/users/me devuelve al usuario autenticado")
    void conTokenValidoAccedeAMe() throws Exception {
        register(username, username + "@test.com");
        String token = loginAndGetToken(username);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    @DisplayName("Autenticado pero sin USER_READ: 403, no 401. La distincion importa")
    void sinPermisoDevuelve403() throws Exception {
        register(username, username + "@test.com");
        String token = loginAndGetToken(username);

        // El usuario es USER + AUTHOR; ninguno de los dos roles incluye USER_READ.
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("El ADMIN bootstrapeado si tiene USER_READ y lista usuarios")
    void adminAccedeAlListado() throws Exception {
        // La password del ADMIN la fija application-test.yml.
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("admin", "admin-de-test-1234"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(body).get("accessToken").asText();

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").exists());
    }
}
