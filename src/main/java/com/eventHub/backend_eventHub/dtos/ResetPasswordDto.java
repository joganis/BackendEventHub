package com.eventHub.backend_eventHub.dtos;



import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para restablecer la contraseña.
 * Contiene el token de recuperación y la nueva contraseña.
 */
@Data
public class ResetPasswordDto {
    @NotBlank(message = "El token es obligatorio")
    private String token;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    private String newPassword;
}
