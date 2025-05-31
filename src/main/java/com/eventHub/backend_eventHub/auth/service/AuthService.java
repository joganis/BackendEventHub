
package com.eventHub.backend_eventHub.auth.service;

import com.eventHub.backend_eventHub.auth.dto.NewUserDto;
import com.eventHub.backend_eventHub.domain.entities.State;
import com.eventHub.backend_eventHub.domain.enums.StateList;
import com.eventHub.backend_eventHub.domain.repositories.StateRepository;
import com.eventHub.backend_eventHub.utils.emails.dto.EmailDto;
import com.eventHub.backend_eventHub.domain.entities.Role;
import com.eventHub.backend_eventHub.domain.entities.Users;
import com.eventHub.backend_eventHub.domain.enums.RoleList;
import com.eventHub.backend_eventHub.auth.jwt.JwtUtil;
import com.eventHub.backend_eventHub.domain.repositories.RoleRepository;
import com.eventHub.backend_eventHub.utils.emails.service.EmailService;
import com.eventHub.backend_eventHub.auth.exceptions.UserAlreadyExistsException;
import com.eventHub.backend_eventHub.auth.exceptions.AuthenticationFailedException;
import com.eventHub.backend_eventHub.auth.exceptions.ResourceNotFoundException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de autenticación y registro de usuarios mejorado.
 *
 * Responsabilidades:
 * - Autenticación de usuarios
 * - Registro de nuevos usuarios
 * - Generación de tokens JWT
 * - Validación de business rules
 */
@Slf4j
@Service
public class AuthService {

    private final UserAuthService userAuthService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final StateRepository stateRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Autowired
    public AuthService(UserAuthService userAuthService,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       StateRepository stateRepository,
                       JwtUtil jwtUtil,
                       EmailService emailService,
                       AuthenticationManager authenticationManager) {
        this.userAuthService = userAuthService;
        this.roleRepository = roleRepository;
        this.stateRepository = stateRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Autentica al usuario y genera un token JWT.
     *
     * @param username Nombre de usuario
     * @param password Contraseña
     * @return Token JWT generado
     * @throws AuthenticationFailedException si las credenciales son inválidas
     */
    public String authenticate(String username, String password) {
        log.info("Attempting authentication for user: {}", username);

        try {
            // Validar que el usuario existe y está activo
            Users user = validateUserForAuthentication(username);

            // Autenticar
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(username, password);
            Authentication authResult = authenticationManager.authenticate(authToken);

            // Establecer contexto de seguridad
            SecurityContextHolder.getContext().setAuthentication(authResult);

            // Generar token
            String role = user.getRole().getNombreRol().toString();
            String token = jwtUtil.generateToken(authResult, role);

            log.info("Authentication successful for user: {}", username);
            return token;

        } catch (AuthenticationException e) {
            log.warn("Authentication failed for user: {}. Reason: {}", username, e.getMessage());
            throw new AuthenticationFailedException("Credenciales inválidas", e);
        }
    }

    /**
     * Registra un nuevo usuario con validaciones mejoradas.
     *
     * @param newUserDto Datos del nuevo usuario
     * @return Token JWT del usuario registrado
     * @throws UserAlreadyExistsException si el usuario ya existe
     * @throws ResourceNotFoundException si no se encuentran roles/estados requeridos
     */
    @Transactional
    public String registerUser(NewUserDto newUserDto) {
        log.info("Attempting to register new user: {}", newUserDto.getUserName());

        // Validar que el usuario no existe
        validateUserDoesNotExist(newUserDto);

        // Crear usuario
        Users user = createNewUser(newUserDto);

        // Guardar usuario
        userAuthService.save(user);
        log.info("User registered successfully: {}", user.getUserName());

        // Enviar email de confirmación (async para no bloquear el registro)
        sendWelcomeEmailAsync(newUserDto.getEmail());

        // Autenticar y retornar token
        return authenticate(newUserDto.getUserName(), newUserDto.getPassword());
    }

    /**
     * Valida que el usuario existe y puede autenticarse.
     */
    private Users validateUserForAuthentication(String username) {
        Users user = userAuthService.findByUserName(username)
                .orElseThrow(() -> new AuthenticationFailedException("Usuario no encontrado"));

        // Validar que el usuario está en estado activo
        if (user.getState() == null ||
                !StateList.Active.equals(user.getState().getNameState())) {
            log.warn("Authentication attempted for inactive user: {}", username);
            throw new AuthenticationFailedException("Usuario inactivo");
        }

        return user;
    }

    /**
     * Valida que el usuario no existe (por username y email).
     */
    private void validateUserDoesNotExist(NewUserDto newUserDto) {
        if (userAuthService.existsByUserName(newUserDto.getUserName())) {
            throw new UserAlreadyExistsException("El nombre de usuario ya existe");
        }


    }

    /**
     * Crea una nueva instancia de usuario con roles y estados por defecto.
     */
    private Users createNewUser(NewUserDto newUserDto) {
        Role defaultRole = roleRepository.findByNombreRol(RoleList.ROLE_USUARIO)
                .orElseThrow(() -> new ResourceNotFoundException("Rol por defecto no encontrado"));

        State activeState = stateRepository.findByNameState(StateList.Active)
                .orElseThrow(() -> new ResourceNotFoundException("Estado por defecto no encontrado"));

        return new Users(
                newUserDto.getUserName(),
                newUserDto.getEmail(),
                passwordEncoder.encode(newUserDto.getPassword()),
                defaultRole,
                activeState
        );
    }

    /**
     * Envía email de bienvenida de forma asíncrona.
     */
    private void sendWelcomeEmailAsync(String email) {
        try {
            EmailDto emailDto = createWelcomeEmail(email);
            emailService.sendEmail(emailDto);
            log.info("Welcome email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}. Error: {}", email, e.getMessage());
            // No lanzar excepción para no afectar el registro
        }
    }

    /**
     * Crea el DTO del email de bienvenida.
     */
    private EmailDto createWelcomeEmail(String email) {
        EmailDto emailDto = new EmailDto();
        emailDto.setRecipientEmail(email);
        emailDto.setSubject("¡Bienvenido a EventHub!");
        emailDto.setBody("Tu cuenta ha sido creada exitosamente. Ahora puedes iniciar sesión en la plataforma.");
        return emailDto;
    }
}