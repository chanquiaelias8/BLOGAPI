package com.debatiendo.blogapi.config;

import com.debatiendo.blogapi.common.Roles;
import com.debatiendo.blogapi.entity.Role;
import com.debatiendo.blogapi.entity.User;
import com.debatiendo.blogapi.entity.enums.AuthProvider;
import com.debatiendo.blogapi.repository.RoleRepository;
import com.debatiendo.blogapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

/**
 * Crea el primer ADMIN.
 *
 * El problema que resuelve: la consigna pide que los usuarios se den de alta por la API,
 * pero /auth/register no puede otorgar ADMIN (seria una escalacion de privilegios abierta)
 * y POST /api/users exige ya estar autenticado como ADMIN. Sin un bootstrap fuera de banda
 * no habria forma de crear el primero. Es el mismo patron que usa cualquier sistema con
 * roles: una cuenta semilla provista por el entorno, no por la red.
 *
 * Solo corre si no existe ningun usuario con rol ADMIN, asi que es idempotente y no
 * puede pisar una cuenta existente.
 */
@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.username:admin}")
    private String adminUsername;

    @Value("${app.bootstrap.admin.email:admin@debatiendo.local}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRoles_Name(Roles.ADMIN)) {
            log.debug("Ya existe un ADMIN; se omite el bootstrap");
            return;
        }

        boolean generated = !StringUtils.hasText(adminPassword);
        String password = generated ? generateRandomPassword() : adminPassword;

        Role adminRole = roleRepository.findByName(Roles.ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "El rol ADMIN no existe; el seeder de autorizacion no corrio"));

        User admin = User.builder()
                .username(adminUsername)
                .email(adminEmail)
                .password(passwordEncoder.encode(password))
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .accountLocked(false)
                .roles(Set.of(adminRole))
                .build();

        userRepository.save(admin);

        if (generated) {
            // Loguear una password es mala practica y no deberia sobrevivir a produccion.
            // Se acepta solo aca porque es de un solo uso, se genera en el arranque y sin
            // ella la instancia quedaria inadministrable. En un entorno real se define
            // APP_BOOTSTRAP_ADMIN_PASSWORD y esta rama no se ejecuta nunca.
            log.warn("""

                    ===============================================================
                     ADMIN creado con password GENERADA (solo para desarrollo)
                     usuario : {}
                     password: {}
                     Definí APP_BOOTSTRAP_ADMIN_PASSWORD para fijarla vos.
                     Cambiala antes de exponer esta instancia.
                    ===============================================================
                    """, adminUsername, password);
        } else {
            log.info("ADMIN inicial creado con la password provista por el entorno: {}", adminUsername);
        }
    }

    private String generateRandomPassword() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
