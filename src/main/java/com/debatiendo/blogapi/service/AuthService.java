package com.debatiendo.blogapi.service;

import com.debatiendo.blogapi.common.Roles;
import com.debatiendo.blogapi.dto.AuthResponse;
import com.debatiendo.blogapi.dto.LoginRequest;
import com.debatiendo.blogapi.dto.RegisterRequest;
import com.debatiendo.blogapi.dto.UserResponse;
import com.debatiendo.blogapi.entity.Author;
import com.debatiendo.blogapi.entity.Role;
import com.debatiendo.blogapi.entity.User;
import com.debatiendo.blogapi.entity.enums.AuthProvider;
import com.debatiendo.blogapi.exception.DuplicateResourceException;
import com.debatiendo.blogapi.repository.AuthorRepository;
import com.debatiendo.blogapi.repository.RoleRepository;
import com.debatiendo.blogapi.repository.UserRepository;
import com.debatiendo.blogapi.security.JwtService;
import com.debatiendo.blogapi.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthorRepository authorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    /**
     * Delega la verificacion de credenciales al AuthenticationManager en vez de comparar
     * el hash a mano. Asi se aprovechan el chequeo de enabled/locked, el hash dummy contra
     * timing attacks y los eventos de autenticacion que publica Spring Security, que son
     * el gancho natural para la auditoria de accesos.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.login(), request.password()));

        SecurityUser principal = (SecurityUser) authentication.getPrincipal();
        String token = jwtService.generateAccessToken(principal);

        Set<String> authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        log.info("Login exitoso para el usuario {}", principal.getUsername());

        return AuthResponse.bearer(token, jwtService.getAccessTokenTtlSeconds(),
                principal.getUsername(), authorities);
    }

    /**
     * Alta publica. Crea la cuenta y su perfil de Author en la MISMA transaccion:
     * si fallara la segunda parte, quedaria un usuario con rol AUTHOR sin perfil y
     * todos los endpoints de publicacion le romperian.
     *
     * Los roles salen de Roles.DEFAULT_SIGNUP_ROLES, nunca del request.
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("El username ya esta en uso");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("El email ya esta registrado");
        }

        Set<Role> roles = Arrays.stream(Roles.DEFAULT_SIGNUP_ROLES)
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new IllegalStateException(
                                "Falta el rol " + name + " en la base; reviso el seeder?")))
                .collect(Collectors.toSet());

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .accountLocked(false)
                .roles(roles)
                .build();

        User saved = userRepository.save(user);

        Author author = Author.builder()
                .nombre(request.nombre())
                .bio(request.bio())
                .user(saved)
                .build();
        authorRepository.save(author);

        log.info("Usuario registrado: {} con roles {}", saved.getUsername(), Roles.DEFAULT_SIGNUP_ROLES);

        return userMapper.toResponse(saved);
    }
}
