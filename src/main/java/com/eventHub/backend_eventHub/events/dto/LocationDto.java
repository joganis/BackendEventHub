package com.eventHub.backend_eventHub.events.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LocationDto {
    @NotBlank private String address;
    @NotBlank private String type;
    @NotNull  private Double latitude;
    @NotNull  private Double longitude;
}