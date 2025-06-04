// EventDto actualizado
package com.eventHub.backend_eventHub.events.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.Instant;
import java.util.*;

@Data
public class EventDto {
    @NotBlank
    private String title;

    private String description;

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

    @NotBlank
    private String categoriaId; // ID de la categoría seleccionada

    private List<MediaDto> mainImages;
    private List<MediaDto> galleryImages;
    private List<MediaDto> videos;
    private List<MediaDto> documents;

    @Valid
    private OtherDataDto otherData;

    private boolean destacado = false;
    private boolean permitirInscripciones = true;
    private Instant fechaLimiteInscripcion;

    private List<@NotBlank String> tags;
}
