package com.debatiendo.blogapi.service;

import com.debatiendo.blogapi.dto.UserResponse;
import com.debatiendo.blogapi.entity.Role;
import com.debatiendo.blogapi.entity.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Mapeo entidad -> DTO a mano en lugar de MapStruct.
 * Con cinco entidades el generador aporta poco y complica el annotation processing
 * (hay que encadenar lombok-mapstruct-binding). Si el numero de DTOs crece, se migra.
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                user.getProvider().name(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                user.getCreatedAt());
    }
}
