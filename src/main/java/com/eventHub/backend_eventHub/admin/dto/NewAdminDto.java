

/**
 * DTO para el registro de un nuevo administrador o subadministrador.
 *
 * Este DTO es usado en endpoints protegidos y permite especificar el rol deseado (ROLE_ADMIN o ROLE_SUBADMIN).
 */

package com.eventHub.backend_eventHub.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NewAdminDto {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String userName;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Debe ser un email válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    @NotBlank(message = "El rol es obligatorio")
    private String role;

    @Size(min = 3, max = 130, message = "El nombre debe tener entre 3 y 130 caracteres")
    private String name;

    private String lastName;

    @Size(max = 10, message = "La identificación no debe superar 10 caracteres")
    private String identification;

    private String birthDate;

    private String phone;

    private String homeAddress;

    private String country;

    private String city;

    private String photo;
}
