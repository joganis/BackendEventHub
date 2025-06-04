// UserProfileDto.java - Mejorado
package com.eventHub.backend_eventHub.users.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO con información del perfil de usuario")
public class UserProfileDto {

    @Schema(description = "ID único del usuario", example = "507f1f77bcf86cd799439011")
    private String id;

    @Schema(description = "Nombre de usuario único", example = "johndoe")
    private String userName;

    @Schema(description = "Email del usuario", example = "john@example.com")
    private String email;

    @Schema(description = "Rol del usuario", example = "ROLE_USUARIO")
    private String role;

    @Schema(description = "Nombre del usuario", example = "Juan Carlos")
    private String name;

    @Schema(description = "Apellido del usuario", example = "Pérez García")
    private String lastName;

    @Schema(description = "Número de identificación", example = "12345678")
    private String identification;

    @Schema(description = "Fecha de nacimiento", example = "1990-05-15")
    private String birthDate;

    @Schema(description = "Número de teléfono", example = "3001234567")
    private String phone;

    @Schema(description = "Dirección de residencia", example = "Calle 123 # 45-67")
    private String homeAddress;

    @Schema(description = "País de residencia", example = "Colombia")
    private String country;

    @Schema(description = "Ciudad de residencia", example = "Bogotá")
    private String city;

    @Schema(description = "Estado actual del usuario", example = "Active")
    private String state;

    @Schema(description = "URL de la foto de perfil", example = "https://example.com/photo.jpg")
    private String photo;

    // Propiedades calculadas
    @JsonProperty("fullName")
    @Schema(description = "Nombre completo del usuario", example = "Juan Carlos Pérez García")
    public String getFullName() {
        if (name != null && lastName != null) {
            return name.trim() + " " + lastName.trim();
        }
        return name != null ? name.trim() : (lastName != null ? lastName.trim() : null);
    }

    @JsonProperty("isActive")
    @Schema(description = "Indica si el usuario está activo", example = "true")
    public boolean isActive() {
        return "Active".equalsIgnoreCase(state) || "Activo".equalsIgnoreCase(state);
    }

    @JsonProperty("isAdmin")
    @Schema(description = "Indica si el usuario es administrador", example = "false")
    public boolean isAdmin() {
        return "ROLE_ADMIN".equalsIgnoreCase(role);
    }
}