// Nuevo: EventSummaryDto
package com.eventHub.backend_eventHub.events.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventSummaryDto {
    private String id;
    private String title;
    private String description;
    private String ubicacion;
    private String fechaInicio;
    private String fechaFin;
    private String categoria;
    private String tipo;
    private boolean esPago;
    private Double precio;
    private String moneda;
    private Integer maxAttendees;
    private Integer currentAttendees;
    private boolean disponible;
    private String imagenPrincipal;
    private boolean destacado;
}