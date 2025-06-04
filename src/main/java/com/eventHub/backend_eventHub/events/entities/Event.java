package com.eventHub.backend_eventHub.events.entities;

import com.eventHub.backend_eventHub.domain.entities.Category;
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
    private String type;          // e.g. "simple", "conferencia", "concierto"
    private String privacy;       // e.g. "public", "private"
    private String ticketType;    // e.g. "paid", "free"
    private Price price;

    private Integer maxAttendees;
    private Integer currentAttendees = 0; // Contador actual de inscritos

    // Categoría (una sola categoría principal)
    @DBRef
    private Category categoria;

    // Multimedia
    @Field("mainImages")
    private List<Media> mainImages;

    @Field("galleryImages")
    private List<Media> galleryImages;

    private List<Media> videos;
    private List<Media> documents;

    // Datos adicionales: organizador/contacto/notas
    private OtherData otherData;

    // Relaciones a subeventos (ahora manejados como entidad separada)
    private List<String> subeventIds;

    // Historial de cambios
    private List<HistoryRecord> history;

    // Estado global y creador
    @DBRef
    private State status;

    @DBRef
    private Users creator; // Usuario que creó el evento (organizador_id en BD)

    // Campos de auditoría
    private Instant createdAt;
    private Instant updatedAt;

    // Campos adicionales para funcionalidad
    private boolean destacado = false; // Para eventos promocionados
    private boolean bloqueado = false; // Para control administrativo

    // Configuración de inscripciones
    private boolean permitirInscripciones = true;
    private Instant fechaLimiteInscripcion; // Opcional

    // Tags adicionales para búsqueda
    private List<String> tags;

    // Lista de usuarios invitados para eventos privados
    private List<String> invitedUsers; // Usernames de usuarios invitados
}