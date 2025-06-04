// Entidad EventRole (para manejar roles específicos de evento)
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
@Document(collection = "evento_roles")
public class EventRole {
    @Id
    private String id;

    @DBRef
    private Users usuario;

    @DBRef
    private Event evento;

    private String rol; // "CREADOR", "SUBCREADOR"

    private Instant fechaAsignacion;

    private String emailInvitacion; // Para subcreadores invitados

    private boolean activo = true;
}