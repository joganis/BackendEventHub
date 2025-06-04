
// DTO para manejar invitaciones a eventos privados
package com.eventHub.backend_eventHub.events.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class EventInvitationDto {
    @NotBlank
    private String eventoId;

    @NotEmpty(message = "Debe especificar al menos un email para invitar")
    private List<@Email @NotBlank String> emails;

    private String mensaje; // Mensaje opcional para la invitación
}

