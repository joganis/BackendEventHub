package com.eventHub.backend_eventHub.events.entities;



import com.eventHub.backend_eventHub.domain.entities.State;
import com.eventHub.backend_eventHub.domain.entities.Users;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.*;
import java.util.*;

/**
 * Representa un evento con toda su información compuesta.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "evento")
public class Event {
    @Id
    private String id;

    @NotBlank
    private String title;
    private String description;

    // Ubicación compleja
    private Location location;

    // Fechas de inicio y fin
    private Instant start;
    private Instant end;

    // Tipo, privacidad y entradas
    private String type;          // e.g. "simple"
    private String privacy;       // e.g. "public"
    private String ticketType;    // e.g. "paid" / "free"
    private Price price;

    private Integer maxAttendees;

    // Categorías
    private List<String> categories;

    // Multimedia
    @Field("mainImages")   private List<Media> mainImages;
    @Field("galleryImages")private List<Media> galleryImages;
    private List<Media> videos;
    private List<Media> documents;

    // Datos adicionales: organizador/contacto/notas
    private OtherData otherData;

    // Relaciones a subeventos e historial de cambios
    private List<String> subeventIds;
    private List<HistoryRecord> history;

    // Estado global y creador
    @DBRef
    private State status;
    @DBRef private Users creator;
}