package com.eventHub.backend_eventHub.admin.service;


import com.eventHub.backend_eventHub.admin.dto.NewAdminDto;
import com.eventHub.backend_eventHub.admin.exception.CustomException;
import com.eventHub.backend_eventHub.domain.entities.Role;
import com.eventHub.backend_eventHub.domain.entities.State;
import com.eventHub.backend_eventHub.domain.entities.Users;
import com.eventHub.backend_eventHub.domain.enums.RoleList;
import com.eventHub.backend_eventHub.domain.enums.StateList;
import com.eventHub.backend_eventHub.domain.repositories.RoleRepository;
import com.eventHub.backend_eventHub.auth.service.UserAuthService;
import com.eventHub.backend_eventHub.domain.repositories.StateRepository;
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

    private final UserAuthService userAuthService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final StateRepository stateRepository;

    @Autowired
    public AdminService(UserAuthService userAuthService, RoleRepository roleRepository, PasswordEncoder passwordEncoder, StateRepository stateRepository) {
        this.userAuthService = userAuthService;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.stateRepository = stateRepository;
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
        if (userAuthService.existsByUserName(newAdminDto.getUserName())) {
            throw new CustomException("El nombre de usuario ya está en uso");
        }

        if (userAuthService.existsByEmail(newAdminDto.getEmail())) {
            throw new CustomException("El correo electrónico ya está registrado");
        }
        // Validar y convertir el rol proporcionado
        RoleList roleRequested;
        try {
            roleRequested = RoleList.valueOf(newAdminDto.getRole());
            if (roleRequested != RoleList.ROLE_ADMIN && roleRequested != RoleList.ROLE_SUBADMIN) {
                throw new CustomException("Rol no permitido. Solo se permite ADMIN o SUBADMIN");
            }
        } catch (IllegalArgumentException e) {
            throw new CustomException("Rol inválido: " + newAdminDto.getRole());
        }
        Role roleAdmin = roleRepository.findByNombreRol(roleRequested)
                .orElseThrow(() -> new CustomException("Rol no encontrado en base de datos"));
        State activeState = stateRepository.findByNameState(StateList.Active)
                .orElseThrow(() -> new CustomException("Estado 'ACTIVO' no encontrado"));
        Users user = new Users(
                newAdminDto.getUserName(),
                newAdminDto.getEmail(),
                passwordEncoder.encode(newAdminDto.getPassword()),
                roleAdmin,
                activeState
        );
        user.setName(newAdminDto.getName());
        user.setLastName(newAdminDto.getLastName());
        user.setIdentification(newAdminDto.getIdentification());
        user.setBirthDate(newAdminDto.getBirthDate());
        user.setPhone(newAdminDto.getPhone());
        user.setHomeAddress(newAdminDto.getHomeAddress());
        user.setCountry(newAdminDto.getCountry());
        user.setCity(newAdminDto.getCity());
        user.setPhoto(newAdminDto.getPhoto());
        userAuthService.save(user);
    }
}