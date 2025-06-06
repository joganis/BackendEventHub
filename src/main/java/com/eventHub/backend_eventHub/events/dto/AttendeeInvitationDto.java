// ================================
// 2. DTO PARA INVITACIONES DE ASISTENTES
// ================================

package com.eventHub.backend_eventHub.events.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AttendeeInvitationDto {
    @NotBlank(message = "El ID del evento es obligatorio")
    private String eventoId;

    @Email(message = "Debe ser un email válido")
    @NotBlank(message = "El email es obligatorio")
    private String emailInvitado;

    @Size(max = 500, message = "El mensaje no puede exceder 500 caracteres")
    private String mensaje;
}