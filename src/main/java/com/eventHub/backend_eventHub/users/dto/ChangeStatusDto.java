// ChangeStatusDto.java - Mejorado
package com.eventHub.backend_eventHub.users.dto;

import com.eventHub.backend_eventHub.users.validation.ValidUserState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "DTO para cambio de estado de usuario")
public class ChangeStatusDto {

    @NotBlank(message = "El estado no puede estar vacío")
    @ValidUserState
    @Schema(
            description = "Nuevo estado del usuario",
            example = "Active",
            allowableValues = {"Active", "Inactive", "Pending", "Canceled", "Blocked", "Activo", "Bloqueado"}
    )
    private String state;
}