package com.eventHub.backend_eventHub.events.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.Instant;

@Data
public class MediaDto {
    @NotBlank
    private String url;
    private String description;
    @NotNull
    private Instant uploadedAt;
    private String mediaType;
}