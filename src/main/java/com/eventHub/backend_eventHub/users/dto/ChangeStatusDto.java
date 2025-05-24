package com.eventHub.backend_eventHub.users.dto;

import com.eventHub.backend_eventHub.domain.entities.State;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;


/**
 * DTO para cambiar el estado de un usuario.
 * Ajustado para aceptar los valores que se utilizan en la UI y mapearlos
 * correctamente al enum StateList en el servicio.
 */
@Data
public class ChangeStatusDto {
    @NotBlank(message = "El estado no puede estar vac?o")
    @Pattern(
            regexp = "Active|Inactive|Pending|Canceled|Blocked|Activo|Bloqueado",
            message = "Estado no v?lido. Debe ser uno de: Active, Inactive, Pending, Canceled, Blocked, Activo, Bloqueado"
    )
    private String state;
}