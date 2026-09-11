package com.debatiendo.blogapi.repository;

import com.debatiendo.blogapi.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    /** EntityGraph para traer los permisos en el mismo SELECT y evitar el N+1. */
    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findByName(String name);

    boolean existsByName(String name);
}
