package com.eventHub.backend_eventHub.auth.controller;

import com.eventHub.backend_eventHub.auth.dto.LoginUserDto;
import com.eventHub.backend_eventHub.auth.dto.NewUserDto;
import com.eventHub.backend_eventHub.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST mejorado para autenticación y registro de usuarios.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Endpoint para login de usuarios.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginUserDto loginUserDto) {
        log.info("Login attempt for user: {}", loginUserDto.getUserName());

        String jwt = authService.authenticate(loginUserDto.getUserName(), loginUserDto.getPassword());

        AuthResponse response = new AuthResponse(jwt, "Login exitoso");
        return ResponseEntity.ok(response);

    }

    /**
     * Endpoint para registro de usuarios.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody NewUserDto newUserDto) {
        log.info("Registration attempt for user: {}", newUserDto.getUserName());

        String jwt = authService.registerUser(newUserDto);

        AuthResponse response = new AuthResponse(jwt, "Usuario registrado exitosamente");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint para verificar el estado de autenticación.
     */
    @GetMapping("/check-auth")
    public ResponseEntity<AuthStatusResponse> checkAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthStatusResponse(false, null, "No autenticado"));
        }

        return ResponseEntity.ok(
                new AuthStatusResponse(true, authentication.getName(), "Autenticado")
        );
    }

    // DTOs para respuestas
    public static class AuthResponse {
        private String token;
        private String message;
        private long timestamp;

        public AuthResponse(String token, String message) {
            this.token = token;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters y setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    public static class AuthStatusResponse {
        private boolean authenticated;
        private String username;
        private String message;

        public AuthStatusResponse(boolean authenticated, String username, String message) {
            this.authenticated = authenticated;
            this.username = username;
            this.message = message;
        }

        // Getters y setters
        public boolean isAuthenticated() { return authenticated; }
        public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}