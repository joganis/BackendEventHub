package com.eventHub.backend_eventHub.admin.dto;


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

    public @NotBlank(message = "El nombre de usuario es obligatorio") String getUserName() {
        return userName;
    }

    public void setUserName(@NotBlank(message = "El nombre de usuario es obligatorio") String userName) {
        this.userName = userName;
    }

    public @NotBlank(message = "El email es obligatorio") @Email(message = "Debe ser un email válido") String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank(message = "El email es obligatorio") @Email(message = "Debe ser un email válido") String email) {
        this.email = email;
    }

    public @NotBlank(message = "La contraseña es obligatoria") String getPassword() {
        return password;
    }

    public void setPassword(@NotBlank(message = "La contraseña es obligatoria") String password) {
        this.password = password;
    }

    public @NotBlank(message = "El rol es obligatorio") String getRole() {
        return role;
    }

    public void setRole(@NotBlank(message = "El rol es obligatorio") String role) {
        this.role = role;
    }
}
