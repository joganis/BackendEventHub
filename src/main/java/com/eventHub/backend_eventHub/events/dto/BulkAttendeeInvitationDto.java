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
public class BulkAttendeeInvitationDto {
    @NotBlank(message = "El ID del evento es obligatorio")
    private String eventoId;

    private List<@Email String> emails;

    @Size(max = 500, message = "El mensaje no puede exceder 500 caracteres")
    private String mensaje;
}