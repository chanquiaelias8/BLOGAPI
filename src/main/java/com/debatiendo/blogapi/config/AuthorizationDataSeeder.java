package com.debatiendo.blogapi.config;

import com.debatiendo.blogapi.common.Permissions;
import com.debatiendo.blogapi.common.Roles;
import com.debatiendo.blogapi.entity.Permission;
import com.debatiendo.blogapi.entity.Role;
import com.debatiendo.blogapi.repository.PermissionRepository;
import com.debatiendo.blogapi.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Siembra el catalogo de roles y permisos.
 *
 * Por que en codigo y no en data.sql: las PK son UUID BINARY(16); en SQL habria que
 * escribirlas a mano con UUID_TO_BIN() y mantener sincronizadas las tablas intermedias.
 * Aca el mapeo rol -> permisos queda declarativo y legible.
 *
 * Es idempotente: solo inserta lo que falta, asi que corre sin problemas en cada arranque
 * y permite agregar un permiso nuevo mas adelante sin tocar la base a mano.
 */
@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class AuthorizationDataSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    /** Fuente de verdad del modelo de autorizacion. */
    private static Map<String, List<String>> rolePermissions() {
        Map<String, List<String>> map = new LinkedHashMap<>();

        map.put(Roles.ADMIN, List.of(
                Permissions.POST_CREATE, Permissions.POST_READ, Permissions.POST_UPDATE, Permissions.POST_DELETE,
                Permissions.AUTHOR_CREATE, Permissions.AUTHOR_READ, Permissions.AUTHOR_UPDATE, Permissions.AUTHOR_DELETE,
                Permissions.USER_CREATE, Permissions.USER_READ, Permissions.USER_UPDATE, Permissions.USER_DELETE,
                Permissions.ROLE_MANAGE, Permissions.PERMISSION_MANAGE
        ));

        // AUTHOR puede crear y editar, pero el alcance (solo lo propio) no lo decide
        // el permiso sino la comprobacion de ownership en @PreAuthorize.
        map.put(Roles.AUTHOR, List.of(
                Permissions.POST_CREATE, Permissions.POST_READ, Permissions.POST_UPDATE,
                Permissions.AUTHOR_READ, Permissions.AUTHOR_UPDATE
        ));

        map.put(Roles.USER, List.of(
                Permissions.POST_READ, Permissions.AUTHOR_READ
        ));

        return map;
    }

    private static final Map<String, String> ROLE_DESCRIPTIONS = Map.of(
            Roles.ADMIN, "Acceso total a posts, autores, usuarios, roles y permisos",
            Roles.AUTHOR, "Publica y edita sus propios posts",
            Roles.USER, "Lectura de posts publicados y de autores"
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, List<String>> catalog = rolePermissions();

        catalog.values().stream()
                .flatMap(List::stream)
                .distinct()
                .forEach(this::ensurePermission);

        catalog.forEach(this::ensureRoleWithPermissions);

        log.info("Seed de autorizacion completo: {} roles, {} permisos",
                roleRepository.count(), permissionRepository.count());
    }

    private Permission ensurePermission(String name) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> {
                    log.info("Creando permiso {}", name);
                    return permissionRepository.save(
                            Permission.builder().name(name).description(name).build());
                });
    }

    private void ensureRoleWithPermissions(String roleName, List<String> permissionNames) {
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    log.info("Creando rol {}", roleName);
                    return Role.builder()
                            .name(roleName)
                            .description(ROLE_DESCRIPTIONS.get(roleName))
                            .build();
                });

        // Reconcilia: agrega los permisos que falten sin pisar los que un ADMIN
        // haya podido asignar a mano desde la API.
        permissionNames.stream()
                .map(this::ensurePermission)
                .filter(p -> !role.getPermissions().contains(p))
                .forEach(role::addPermission);

        roleRepository.save(role);
    }
}
