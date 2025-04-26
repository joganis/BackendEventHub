package com.eventHub.backend_eventHub.admin.service;


import com.eventHub.backend_eventHub.admin.dto.NewAdminDto;
import com.eventHub.backend_eventHub.domain.entities.Role;
import com.eventHub.backend_eventHub.domain.entities.Users;
import com.eventHub.backend_eventHub.enums.RoleList;
import com.eventHub.backend_eventHub.domain.repositories.RoleRepository;
import com.eventHub.backend_eventHub.users.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para la administración y gestión de usuarios con roles elevados (administrador y subadministrador).
 *
 * Este servicio es utilizado por AdminController para registrar usuarios con roles elevados.
 */
@Service
public class AdminService {

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AdminService(UserService userService, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra un nuevo administrador o subadministrador.
     *
     * Valida que el rol proporcionado sea ROLE_ADMIN o ROLE_SUBADMIN.
     *
     * @param newAdminDto Datos del nuevo administrador.
     */
    @Transactional
    public void registerAdmin(NewAdminDto newAdminDto) {
        if (userService.existsByUserName(newAdminDto.getUserName())) {
            throw new IllegalArgumentException("El nombre de usuario ya existe");
        }
        // Validar y convertir el rol proporcionado
        RoleList roleRequested;
        try {
            roleRequested = RoleList.valueOf(newAdminDto.getRole());
            if (roleRequested != RoleList.ROLE_ADMIN && roleRequested != RoleList.ROLE_SUBADMIN) {
                throw new IllegalArgumentException("Rol no permitido para registro de admin");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol inválido: " + newAdminDto.getRole());
        }
        Role roleAdmin = roleRepository.findByNombreRol(roleRequested)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        Users user = new Users(
                newAdminDto.getUserName(),
                newAdminDto.getEmail(),
                passwordEncoder.encode(newAdminDto.getPassword()),
                roleAdmin
        );
        userService.save(user);
    }
}
