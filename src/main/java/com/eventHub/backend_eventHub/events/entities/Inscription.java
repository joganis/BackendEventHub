// Entidad Inscription
package com.eventHub.backend_eventHub.events.entities;

import com.eventHub.backend_eventHub.domain.entities.Users;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "inscripciones")
public class Inscription {
    @Id
    private String id;

    @DBRef
    private Users usuario;

    @DBRef
    private Event evento;

    private Instant fechaInscripcion;

    private String estado; // "confirmada", "cancelada", "pendiente"

    private String tipoInscripcion; // "evento_principal", "subevento"

    // Para subeventos
    private String subeventoId;
}