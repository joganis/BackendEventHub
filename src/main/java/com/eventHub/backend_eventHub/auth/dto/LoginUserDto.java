package com.eventHub.backend_eventHub.auth.dto;


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

    public @NotBlank(message = "El nombre de usuario es obligatorio") String getUserName() {
        return userName;
    }

    public void setUserName(@NotBlank(message = "El nombre de usuario es obligatorio") String userName) {
        this.userName = userName;
    }

    public @NotBlank(message = "La contraseña es obligatoria") String getPassword() {
        return password;
    }

    public void setPassword(@NotBlank(message = "La contraseña es obligatoria") String password) {
        this.password = password;
    }
}
