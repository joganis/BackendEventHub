package com.eventHub.backend_eventHub.auth.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para la solicitud de recuperación de contraseña.
 * Se requiere el email del usuario para enviar el enlace de restablecimiento.
 */
@Data
public class RecoveryRequestDto {
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Debe ser un email válido")
    private String email;
}
