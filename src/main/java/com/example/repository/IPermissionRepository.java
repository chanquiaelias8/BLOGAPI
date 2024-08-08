package com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.model.Permission;

import jakarta.transaction.Transactional;

@Repository
public interface IPermissionRepository extends JpaRepository<Permission, Long>{

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM roles_permissions WHERE permission_id = :permissionId", nativeQuery = true)
    void deleteRolePermissionsByPermissionId(@Param("permissionId") Long permissionId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Permission p WHERE p.id = :permissionId")
    void deleteById(@Param("permissionId") Long permissionId);
}