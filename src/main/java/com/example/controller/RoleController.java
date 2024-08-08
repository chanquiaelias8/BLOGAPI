package com.example.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.model.Permission;
import com.example.model.Role;
import com.example.service.IPermissionService;
import com.example.service.IRoleService;



@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private IRoleService roleService;

    @Autowired
    private IPermissionService permiService;

    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        List<Role> roles = roleService.findAll();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Role> getRoleById(@PathVariable Long id) {
        Optional<Role> role = roleService.findById(id);
        return role.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Role> createRole(@RequestBody Role role) {
        Set<Permission> permiList = new HashSet<Permission>();
        Permission readPermission;

        // Recuperar la Permission/s por su ID
        for (Permission per : role.getPermissionsList()) {
            readPermission = permiService.findById(per.getId()).orElse(null);
            if (readPermission != null) {
                //si encuentro, guardo en la lista
                permiList.add(readPermission);
            }
        }

        role.setPermissionsList(permiList);
        Role newRole = roleService.save(role);
        return ResponseEntity.ok(newRole);
    }

    // UPDATE 
    @PutMapping("/{id}")
    public ResponseEntity<Role> updateRole(@PathVariable Long id, @RequestBody Role updateRole){
        // Encontrar el Role, o lanzar una excepción si no se encuentra
        Role roleToUpdate = roleService.findById(id)
                                    .orElseThrow(() -> new RuntimeException("Role not found with id: " + id));

        // Recuperar y asignar los permisos
        Set<Permission> permiList = updateRole.getPermissionsList().stream()
            .map(permission -> permiService.findById(permission.getId()).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        // Actualizar los campos del Role
        roleToUpdate.setRole(updateRole.getRole());
        roleToUpdate.setPermissionsList(permiList);

        // Guardar y retornar la respuesta
        Role updatedRole = roleService.update(roleToUpdate);
        return ResponseEntity.ok(updatedRole);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRole(@PathVariable Long id){
        roleService.deleteById(id);
        return ResponseEntity.ok("Permission deleted successfully");
    }
}