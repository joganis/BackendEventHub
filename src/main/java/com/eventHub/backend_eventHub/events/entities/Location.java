package com.eventHub.backend_eventHub.events.entities;


import lombok.*;
import org.springframework.data.mongodb.core.mapping.Field;

/** Detalle de ubicación geográfica */
@Data @NoArgsConstructor @AllArgsConstructor
@Builder
public class Location {
    private String address;
    private String type;       // "presencial", "virtual", etc.
    private Double latitude;
    private Double longitude;
}