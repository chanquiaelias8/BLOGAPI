package com.debatiendo.blogapi.service;

import com.debatiendo.blogapi.dto.UserResponse;
import com.debatiendo.blogapi.exception.ResourceNotFoundException;
import com.debatiendo.blogapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    // Sin paginar por ahora; se agrega Pageable cuando se complete el CRUD de usuarios.
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(userMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", id));
    }
}
