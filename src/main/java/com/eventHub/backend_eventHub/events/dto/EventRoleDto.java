// Nuevo: EventRoleDto
package com.eventHub.backend_eventHub.events.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EventRoleDto {
    @NotBlank
    private String eventoId;

    @Email
    @NotBlank
    private String emailInvitacion;

    @NotBlank
    private String rol = "SUBCREADOR";
}