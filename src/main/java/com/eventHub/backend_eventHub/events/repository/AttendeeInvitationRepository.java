// ================================
// AttendeeInvitationRepository OPTIMIZADO
// ================================

package com.eventHub.backend_eventHub.events.repository;

import com.eventHub.backend_eventHub.events.entities.AttendeeInvitation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendeeInvitationRepository extends MongoRepository<AttendeeInvitation, String> {

    // ✅ OPTIMIZADO - Buscar por token (solo campos necesarios)
    @Query(value = "{'token': ?0}",
            fields = "{'id': 1, 'evento': 1, 'emailInvitado': 1, 'usuarioInvitado': 1, 'estado': 1, 'fechaExpiracion': 1, 'fechaRespuesta': 1, 'invitadoPor': 1, 'mensaje': 1}")
    Optional<AttendeeInvitation> findByToken(String token);

    // ✅ OPTIMIZADO - Buscar invitaciones de un evento (usa evento.$id para referenciar por ID)
    @Query(value = "{'evento.$id': ?0}",
            fields = "{'id': 1, 'emailInvitado': 1, 'usuarioInvitado': 1, 'estado': 1, 'fechaInvitacion': 1, 'fechaRespuesta': 1, 'fechaExpiracion': 1, 'invitadoPor': 1, 'mensaje': 1}")
    List<AttendeeInvitation> findByEventoId(String eventoId);

    // ✅ OPTIMIZADO - Buscar invitaciones pendientes por email (sin cargar referencias pesadas)
    @Query(value = "{'emailInvitado': ?0, 'estado': ?1}",
            fields = "{'id': 1, 'evento': 1, 'emailInvitado': 1, 'estado': 1, 'fechaExpiracion': 1, 'fechaInvitacion': 1, 'mensaje': 1, 'invitadoPor': 1}")
    List<AttendeeInvitation> findByEmailInvitadoAndEstado(String email, String estado);

    // ✅ OPTIMIZADO - Buscar invitaciones por usuario registrado (usa usuarioInvitado.$id)
    @Query(value = "{'usuarioInvitado.$id': ?0, 'estado': ?1}",
            fields = "{'id': 1, 'evento': 1, 'emailInvitado': 1, 'estado': 1, 'fechaExpiracion': 1, 'fechaInvitacion': 1, 'mensaje': 1, 'invitadoPor': 1}")
    List<AttendeeInvitation> findByUsuarioInvitadoIdAndEstado(String usuarioId, String estado);

    // ✅ OPTIMIZADO - Verificar existencia de invitación activa (solo retorna IDs para verificación)
    @Query(value = "{'evento.$id': ?0, 'emailInvitado': ?1, 'estado': {$in: ?2}}",
            fields = "{'id': 1}")
    List<AttendeeInvitation> findActiveInvitationCheck(String eventoId, String email, List<String> estados);

    // Método helper para boolean check
    default boolean existsByEventoIdAndEmailInvitadoAndEstadoIn(String eventoId, String email, List<String> estados) {
        return !findActiveInvitationCheck(eventoId, email, estados).isEmpty();
    }

    // ✅ OPTIMIZADO - Buscar invitaciones expiradas (para cleanup automático)
    @Query(value = "{'estado': ?0, 'fechaExpiracion': {$lt: ?1}}",
            fields = "{'id': 1, 'estado': 1, 'fechaExpiracion': 1, 'emailInvitado': 1}")
    List<AttendeeInvitation> findByEstadoAndFechaExpiracionBefore(String estado, Instant fecha);

    // ✅ OPTIMIZADO - Contar invitaciones por estado (solo IDs para conteo)
    @Query(value = "{'evento.$id': ?0, 'estado': ?1}",
            fields = "{'id': 1}")
    List<AttendeeInvitation> findForEventStateCounting(String eventoId, String estado);

    // Método helper para conteo
    default long countByEventoIdAndEstado(String eventoId, String estado) {
        return findForEventStateCounting(eventoId, estado).size();
    }

    // ✅ NUEVO - Verificar acceso por invitación aceptada (para hasUserAccessToPrivateEvent)
    @Query(value = "{'evento.$id': ?0, 'emailInvitado': ?1, 'estado': 'aceptada'}",
            fields = "{'id': 1}")
    List<AttendeeInvitation> findAcceptedInvitationForAccessCheck(String eventoId, String email);

    // Método helper para verificación de acceso
    default boolean hasAcceptedInvitationForEvent(String eventoId, String email) {
        return !findAcceptedInvitationForAccessCheck(eventoId, email).isEmpty();
    }

    // ✅ NUEVO - Buscar invitación específica para aceptar/rechazar (con validaciones)
    @Query(value = "{'token': ?0, 'estado': 'pendiente'}",
            fields = "{'id': 1, 'evento': 1, 'emailInvitado': 1, 'usuarioInvitado': 1, 'estado': 1, 'fechaExpiracion': 1, 'invitadoPor': 1}")
    Optional<AttendeeInvitation> findPendingInvitationByToken(String token);

    // ✅ NUEVO - Buscar todas las invitaciones de un usuario (por email y usuario registrado)
    @Query(value = "{'$or': [{'emailInvitado': ?0}, {'usuarioInvitado.$id': ?1}], 'estado': ?2}",
            fields = "{'id': 1, 'evento': 1, 'emailInvitado': 1, 'estado': 1, 'fechaExpiracion': 1, 'fechaInvitacion': 1, 'mensaje': 1, 'invitadoPor': 1}")
    List<AttendeeInvitation> findAllUserInvitationsByEmailOrId(String email, String userId, String estado);

    // ✅ NUEVO - Buscar invitaciones que requieren notificación (para recordatorios)
    @Query(value = "{'estado': 'pendiente', 'fechaExpiracion': {$gte: ?0, $lte: ?1}}",
            fields = "{'id': 1, 'emailInvitado': 1, 'evento': 1, 'fechaExpiracion': 1, 'vecesEnviada': 1}")
    List<AttendeeInvitation> findInvitationsForReminder(Instant fromDate, Instant toDate);

    // ✅ NUEVO - Estadísticas de invitaciones de un organizador
    @Query(value = "{'invitadoPor.$id': ?0}",
            fields = "{'id': 1, 'estado': 1, 'fechaInvitacion': 1}")
    List<AttendeeInvitation> findInvitationsByOrganizer(String organizerId);
}