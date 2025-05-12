package com.eventHub.backend_eventHub.users.dto;

import com.eventHub.backend_eventHub.domain.entities.Role;
import com.eventHub.backend_eventHub.domain.entities.State;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class UpdateUserDto {
    @NotBlank
    @Email
    private String email;
    @NotBlank (message ="este campo es obligatorio")
    @Size(max = 130)
    private String name;
    @NotBlank(message = "este campo es obligatorio")
    @Size(max = 130)
    private String lastName;
    @NotBlank(message = "este capo es obligatorio")
    @Pattern(regexp = "\\d{10}")
    private String identification;
    @NotBlank(message = "Este campo es obligatorio")
    private String birthDate;
    @NotBlank(message = " Este campo es obligtorio")
    @Pattern(regexp = "\\d{7,10}")
    private String phone;
    @NotBlank(message = "este campo es obligatorio")
    private String homeAddress;
    private String country;
    private String city;

    private String photo;

}
