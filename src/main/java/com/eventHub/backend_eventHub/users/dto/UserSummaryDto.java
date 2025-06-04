// UserSummaryDto.java - Nuevo DTO para listados
package com.eventHub.backend_eventHub.users.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO resumido para listados de usuarios")
public class UserSummaryDto {

    @Schema(description = "ID único del usuario")
    private String id;

    @Schema(description = "Nombre de usuario único")
    private String userName;

    @Schema(description = "Email del usuario")
    private String email;

    @Schema(description = "Nombre completo del usuario")
    private String fullName;

    @Schema(description = "Rol del usuario")
    private String role;

    @Schema(description = "Estado actual del usuario")
    private String state;

    @Schema(description = "URL de la foto de perfil")
    private String photo;

    @JsonProperty("isActive")
    @Schema(description = "Indica si el usuario está activo")
    public boolean isActive() {
        return "Active".equalsIgnoreCase(state) || "Activo".equalsIgnoreCase(state);
    }

    @JsonProperty("isAdmin")
    @Schema(description = "Indica si el usuario es administrador")
    public boolean isAdmin() {
        return "ROLE_ADMIN".equalsIgnoreCase(role);
    }
}