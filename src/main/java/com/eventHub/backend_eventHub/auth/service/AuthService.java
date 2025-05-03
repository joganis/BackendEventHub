package com.eventHub.backend_eventHub.auth.service;


import com.eventHub.backend_eventHub.auth.dto.NewUserDto;
import com.eventHub.backend_eventHub.utils.emails.dto.EmailDto;

import com.eventHub.backend_eventHub.domain.entities.Role;
import com.eventHub.backend_eventHub.domain.entities.Users;
import com.eventHub.backend_eventHub.domain.enums.RoleList;
import com.eventHub.backend_eventHub.auth.jwt.JwtUtil;
import com.eventHub.backend_eventHub.domain.repositories.RoleRepository;
import com.eventHub.backend_eventHub.utils.emails.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de autenticación y registro de usuarios.
 *
 * Gestiona el inicio de sesión, registro de usuarios con rol por defecto (ROLE_USER)
 * y el envío de correos de confirmación.
 */
@Service
public class   AuthService {

    private final UserAuthService userAuthService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final EmailService emailService;

    @Autowired
    public AuthService(UserAuthService userAuthService,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       EmailService emailService,
                       AuthenticationManagerBuilder authenticationManagerBuilder) {
        this.userAuthService = userAuthService;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
    }

    /**
     * Autentica al usuario y genera un token JWT.
     *
     * @param username Nombre de usuario.
     * @param password Contraseña.
     * @return Token JWT generado.
     */
    public String authenticate(String username, String password) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, password);
        AuthenticationManager authManager = authenticationManagerBuilder.getObject();
        if (authManager == null) {
            throw new IllegalStateException("Error en la configuración de autenticación");
        }
        Authentication authResult = authManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authResult);
        Users user = userAuthService.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        String role = user.getRole().getNombreRol().toString();
        return jwtUtil.generateToken(authResult, role);
    }

    /**
     * Registra un nuevo usuario asignándole automáticamente el rol por defecto (ROLE_USER).
     *
     * @param newUserDto Datos del nuevo usuario.
     */
    @Transactional
    public void registerUser(NewUserDto newUserDto) {
        if (userAuthService.existsByUserName(newUserDto.getUserName())) {
            throw new IllegalArgumentException("El nombre de usuario ya existe");
        }
        // Asignación automática del rol por defecto (ROLE_USER)
        Role roleUser = roleRepository.findByNombreRol(RoleList.ROLE_USUARIO)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        Users user = new Users(
                newUserDto.getUserName(),
                newUserDto.getEmail(),
                passwordEncoder.encode(newUserDto.getPassword()),roleUser
        );
        userAuthService.save(user);
        // Enviar correo de confirmación
        EmailDto emailDto = new EmailDto();
        emailDto.setRecipientEmail(newUserDto.getEmail());
        emailDto.setSubject("¡Bienvenido a nuestra plataforma!");
        emailDto.setBody("Tu cuenta ha sido creada exitosamente. Ahora puedes iniciar sesión en la plataforma.");
        try {
            emailService.sendEmail(emailDto);
            System.out.println("✅ Correo de confirmación enviado correctamente.");
        } catch (Exception e) {
            System.err.println("❌ Error al enviar el correo: " + e.getMessage());
        }
    }
}
