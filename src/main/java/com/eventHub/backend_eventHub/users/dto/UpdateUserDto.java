// UpdateUserDto.java - Mejorado
package com.eventHub.backend_eventHub.users.dto;

import com.eventHub.backend_eventHub.users.validation.ValidBirthDate;
import com.eventHub.backend_eventHub.users.validation.ValidPhoneNumber;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "DTO para actualización de datos de usuario")
public class UpdateUserDto {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    @Size(max = 255, message = "El email no puede exceder 255 caracteres")
    @Schema(description = "Email del usuario", example = "usuario@example.com")
    private String email;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 130, message = "El nombre debe tener entre 2 y 130 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo puede contener letras y espacios")
    @Schema(description = "Nombre del usuario", example = "Juan Carlos")
    private String name;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 130, message = "El apellido debe tener entre 2 y 130 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido solo puede contener letras y espacios")
    @Schema(description = "Apellido del usuario", example = "Pérez García")
    private String lastName;

    @NotBlank(message = "La identificación es obligatoria")
    @Pattern(regexp = "^\\d{8,12}$", message = "La identificación debe contener entre 8 y 12 dígitos")
    @Schema(description = "Número de identificación", example = "12345678")
    private String identification;

    @NotBlank(message = "La fecha de nacimiento es obligatoria")
    @ValidBirthDate
    @Schema(description = "Fecha de nacimiento en formato YYYY-MM-DD", example = "1990-05-15")
    private String birthDate;

    @NotBlank(message = "El teléfono es obligatorio")
    @ValidPhoneNumber
    @Schema(description = "Número de teléfono", example = "3001234567")
    private String phone;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(min = 10, max = 255, message = "La dirección debe tener entre 10 y 255 caracteres")
    @Schema(description = "Dirección de residencia", example = "Calle 123 # 45-67")
    private String homeAddress;

    @Size(max = 100, message = "El país no puede exceder 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "El país solo puede contener letras y espacios")
    @Schema(description = "País de residencia", example = "Colombia")
    private String country;

    @Size(max = 100, message = "La ciudad no puede exceder 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "La ciudad solo puede contener letras y espacios")
    @Schema(description = "Ciudad de residencia", example = "Bogotá")
    private String city;

    @Size(max = 500, message = "La URL de la foto no puede exceder 500 caracteres")
    @Pattern(regexp = "^(https?://)?[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?$|^$",
            message = "La URL de la foto no es válida")
    @Schema(description = "URL de la foto de perfil", example = "https://example.com/photo.jpg")
    private String photo;
}