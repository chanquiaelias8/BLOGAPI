package com.debatiendo.blogapi.security;

import com.debatiendo.blogapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Punto unico donde la base de datos se convierte en authorities.
 * Tanto el login (DaoAuthenticationProvider) como cada request con JWT pasan por aca,
 * asi que los permisos son siempre los que estan hoy en la base: si un ADMIN le saca
 * POST_CREATE a un rol, el efecto es inmediato y no hay que esperar a que expire nada.
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        return userRepository.findForAuthentication(login)
                .map(SecurityUser::new)
                // Mensaje deliberadamente generico: distinguir "no existe" de "password
                // incorrecta" permite enumerar usuarios validos desde afuera.
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales invalidas"));
    }
}
