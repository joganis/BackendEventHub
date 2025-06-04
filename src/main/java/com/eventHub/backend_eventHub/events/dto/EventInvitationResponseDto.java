package com.eventHub.backend_eventHub.events.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// DTO para responder invitaciones
@Data
class EventInvitationResponseDto {
    @NotBlank
    private String eventoId;

    @NotBlank
    private String respuesta; // "aceptar" o "rechazar"
}
