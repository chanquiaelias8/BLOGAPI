package com.example.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.model.UserSec;

public interface IUserRepository extends JpaRepository<UserSec, Long>{

    // Crear la sentencia en base al nombre en ingles del metodo
    // Tmb se puede hacer mediante Query pero en este caso no es necesario
    Optional<UserSec> findUserEntityByUsername(String username);
}
