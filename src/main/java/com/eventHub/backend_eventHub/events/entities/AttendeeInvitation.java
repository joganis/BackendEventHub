// ================================
// 1. NUEVA ENTIDAD - AttendeeInvitation
// ================================

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
@Document(collection = "attendee_invitations")
public class AttendeeInvitation {
    @Id
    private String id;

    @DBRef
    private Event evento;

    @DBRef
    private Users invitadoPor; // Usuario que envía la invitación (creador/subcreador)

    // Para invitar por email (usuario puede no existir aún)
    private String emailInvitado;

    // Si el usuario ya existe en el sistema
    @DBRef
    private Users usuarioInvitado;

    private Instant fechaInvitacion;
    private Instant fechaExpiracion; // 7 días por defecto

    private String estado; // "pendiente", "aceptada", "rechazada", "expirada"

    private String mensaje; // Mensaje personalizado del organizador

    // Token único para la invitación (para enlaces seguros)
    private String token;

    // Campos de seguimiento
    private Instant fechaRespuesta;
    private Instant fechaUltimoEnvio;
    private int vecesEnviada = 1; // Controlar reenvíos
}
