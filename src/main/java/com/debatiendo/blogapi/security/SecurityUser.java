package com.debatiendo.blogapi.security;

import com.debatiendo.blogapi.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Adaptador entre la entidad User y el contrato UserDetails de Spring Security.
 *
 * Existe para que la entidad JPA no implemente UserDetails: si User implementara la
 * interfaz, la capa de persistencia quedaria acoplada al framework de seguridad y
 * metodos como isAccountNonExpired() terminarian como columnas o como logica de negocio
 * disfrazada. Ademas la entidad viaja por servicios que no tienen nada que ver con auth.
 */
@Getter
public class SecurityUser implements UserDetails {

    private final transient User user;
    private final Set<GrantedAuthority> authorities;

    public SecurityUser(User user) {
        this.user = user;
        this.authorities = buildAuthorities(user);
    }

    /**
     * Aplana roles + permisos a un unico Set de authorities, que es como Spring Security
     * modela ambos conceptos internamente.
     *
     * El rol lleva prefijo ROLE_ (es lo que espera hasRole()) y el permiso no (es lo que
     * espera hasAuthority()). Esa es toda la diferencia tecnica entre los dos: la
     * distincion semantica la pone el diseno, no el framework.
     */
    private static Set<GrantedAuthority> buildAuthorities(User user) {
        Stream<String> roleAuthorities = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getName());

        Stream<String> permissionAuthorities = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getName());

        return Stream.concat(roleAuthorities, permissionAuthorities)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    public UUID getId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonLocked() {
        return !user.isAccountLocked();
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    /** No modelamos expiracion de cuenta ni de credenciales; se documenta devolviendo true. */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
