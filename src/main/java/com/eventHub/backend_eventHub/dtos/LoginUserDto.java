package com.eventHub.backend_eventHub.dtos;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para el proceso de login.
 *
 * Contiene las credenciales necesarias para iniciar sesión.
 */
@Data
public class LoginUserDto {
    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String userName;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
