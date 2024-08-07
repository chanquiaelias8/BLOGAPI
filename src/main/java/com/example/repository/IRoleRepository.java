package com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.model.Role;

@Repository
public interface IRoleRepository extends JpaRepository<Role, Long>{}