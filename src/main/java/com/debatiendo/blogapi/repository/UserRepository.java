package com.debatiendo.blogapi.repository;

import com.debatiendo.blogapi.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Carga para autenticacion. El EntityGraph trae roles y permisos en el mismo SELECT:
     * sin el, construir las authorities dispara 1 query por rol (N+1) en CADA request,
     * porque el filtro JWT recarga el usuario en cada llamada.
     *
     * Son dos colecciones anidadas, o sea un producto cartesiano roles x permisos.
     * Es aceptable aca (un usuario tiene 1-3 roles y ~14 permisos como techo) pero no
     * seria el patron correcto para colecciones grandes.
     */
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    @Query("select u from User u where u.username = :login or u.email = :login")
    Optional<User> findForAuthentication(@Param("login") String login);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByProviderAndProviderId(
            com.debatiendo.blogapi.entity.enums.AuthProvider provider, String providerId);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByRoles_Name(String roleName);
}
