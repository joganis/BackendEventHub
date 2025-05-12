package com.eventHub.backend_eventHub.users.dto;


import com.eventHub.backend_eventHub.domain.entities.Role;
import com.eventHub.backend_eventHub.domain.entities.State;
import lombok.Data;

@Data
public class UserProfileDto {
    private String id;
    private String userName;
    private String email;
    private String password;
    private String role;
    private String name;
    private String lastName;
    private String identification;
    private String birthDate;
    private String phone;
    private String homeAddress;
    private String country;
    private String city;
    private String state;
    private String photo;
}
