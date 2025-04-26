package com.eventHub.backend_eventHub.auth.dto;

import com.eventHub.backend_eventHub.domain.entities.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para el registro de un nuevo usuario.
 *
 * No incluye el campo de rol para que se asigne automáticamente el rol por defecto (ROLE_USER).
 */
@Data
public class NewUserDto {
    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String userName;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Debe ser un email válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    private Role role;
}
