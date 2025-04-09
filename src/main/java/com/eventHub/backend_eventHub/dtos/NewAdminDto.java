package com.eventHub.backend_eventHub.dtos;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para el registro de un nuevo administrador o subadministrador.
 *
 * Este DTO es usado en endpoints protegidos y permite especificar el rol deseado (ROLE_ADMIN o ROLE_SUBADMIN).
 */
@Data
public class NewAdminDto {
    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String userName;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Debe ser un email válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    @NotBlank(message = "El rol es obligatorio")
    private String role;

}
