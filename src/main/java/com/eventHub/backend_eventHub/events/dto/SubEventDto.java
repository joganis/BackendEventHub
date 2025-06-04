// Nuevo: SubEventDto
package com.eventHub.backend_eventHub.events.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class SubEventDto {
    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String eventoPrincipalId;

    @Valid @NotNull
    private LocationDto location;

    @NotNull
    private Instant start;

    @NotNull
    private Instant end;

    @NotBlank
    private String type;

    @NotBlank
    private String privacy;

    @NotBlank
    private String ticketType;

    @Valid
    private PriceDto price;

    @Min(1)
    private Integer maxAttendees;

    private List<MediaDto> mainImages;

    @Valid
    private OtherDataDto otherData;
}