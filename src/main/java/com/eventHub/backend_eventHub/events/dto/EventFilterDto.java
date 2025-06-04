// Nuevo: EventFilterDto
package com.eventHub.backend_eventHub.events.dto;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class EventFilterDto {
    private String status;
    private String categoriaId;
    private String type;
    private String privacy;
    private String ticketType;
    private String searchText;
    private Instant startDate;
    private Instant endDate;
    private Boolean destacado;
    private Boolean conDisponibilidad;
    private List<String> tags;
    private String ubicacionTipo;
    private Double latitude;
    private Double longitude;
    private Integer radiusKm;
}