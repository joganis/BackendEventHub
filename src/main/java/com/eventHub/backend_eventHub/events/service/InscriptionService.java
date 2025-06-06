// InscriptionService CORREGIDO
package com.eventHub.backend_eventHub.events.service;

import com.eventHub.backend_eventHub.events.dto.InscriptionDto;
import com.eventHub.backend_eventHub.events.entities.*;
import com.eventHub.backend_eventHub.events.repository.*;
import com.eventHub.backend_eventHub.domain.entities.Users;
import com.eventHub.backend_eventHub.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InscriptionService {
    @Autowired private InscriptionRepository inscriptionRepo;
    @Autowired private EventRepository eventRepo;
    @Autowired private SubEventRepository subEventRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private EventService eventService;

    /**
     * Inscribe un usuario a un evento principal CON VALIDACIONES MEJORADAS
     */
    @Transactional
    public Inscription registerToEvent(String username, InscriptionDto dto) {
        // 1. Obtener usuario
        Users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));

        // 2. Obtener y validar evento
        Event event = eventRepo.findById(dto.getEventoId())
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado con ID: " + dto.getEventoId()));

        // 3. ✅ VALIDACIÓN MEJORADA - Verificar si ya está inscrito (MÁS ESPECÍFICA)
        boolean yaEstaInscrito = inscriptionRepo.findAll().stream()
                .anyMatch(inscription ->
                        inscription.getUsuario() != null &&
                                inscription.getUsuario().getId().equals(user.getId()) &&
                                inscription.getEvento() != null &&
                                inscription.getEvento().getId().equals(event.getId()) &&
                                "confirmada".equals(inscription.getEstado()) &&
                                "evento_principal".equals(inscription.getTipoInscripcion())
                );

        if (yaEstaInscrito) {
            throw new IllegalArgumentException("Ya estás inscrito en este evento");
        }

        // 4. Validar todas las condiciones del evento
        validateEventForRegistration(event, user);

        // 5. Crear inscripción
        Inscription inscription = Inscription.builder()
                .usuario(user)
                .evento(event)
                .fechaInscripcion(Instant.now())
                .estado("confirmada")
                .tipoInscripcion("evento_principal")
                .build();

        // 6. Guardar y actualizar contador
        inscription = inscriptionRepo.save(inscription);
        updateEventAttendeeCount(event);

        return inscription;
    }

    /**
     * ✅ VALIDACIONES MEJORADAS DEL EVENTO
     */
    private void validateEventForRegistration(Event event, Users user) {
        // Validar que el evento esté activo
        if (event.getStatus() == null || !"Active".equals(event.getStatus().getNameState().name())) {
            throw new IllegalArgumentException("Solo puedes inscribirte a eventos activos");
        }

        // Validar que el evento no esté bloqueado
        if (event.isBloqueado()) {
            throw new IllegalArgumentException("Este evento está bloqueado y no permite inscripciones");
        }

        // Validar que las inscripciones estén permitidas
        if (!event.isPermitirInscripciones()) {
            throw new IllegalArgumentException("Las inscripciones están cerradas para este evento");
        }

        // Validar que no haya pasado la fecha límite
        if (event.getFechaLimiteInscripcion() != null &&
                Instant.now().isAfter(event.getFechaLimiteInscripcion())) {
            throw new IllegalArgumentException("La fecha límite de inscripción ya pasó");
        }

        // Validar que el evento no haya comenzado
        if (event.getStart() != null && Instant.now().isAfter(event.getStart())) {
            throw new IllegalArgumentException("No puedes inscribirte a un evento que ya comenzó");
        }

        // Validar capacidad disponible
        if (event.getMaxAttendees() != null &&
                event.getCurrentAttendees() >= event.getMaxAttendees()) {
            throw new IllegalArgumentException("Este evento ya alcanzó su capacidad máxima");
        }

        // NUEVA VALIDACIÓN - No permitir inscribirse a su propio evento
        if (event.getCreator() != null && event.getCreator().getId().equals(user.getId())) {
            throw new IllegalArgumentException("No puedes inscribirte a tu propio evento");
        }

        // ✅ NUEVA VALIDACIÓN - Para eventos privados, verificar que tenga acceso
        if ("private".equals(event.getPrivacy())) {
            boolean hasAccess = eventService.hasUserAccessToPrivateEvent(user.getUserName(), event);
            if (!hasAccess) {
                throw new IllegalArgumentException("No tienes acceso a este evento privado. Necesitas una invitación para poder inscribirte.");
            }
        }
    }

    /**
     * Inscribe un usuario a un sub-evento CON VALIDACIONES MEJORADAS
     */
    @Transactional
    public Inscription registerToSubEvent(String username, InscriptionDto dto) {
        Users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        SubEvent subEvent = subEventRepo.findById(dto.getSubeventoId())
                .orElseThrow(() -> new IllegalArgumentException("Sub-evento no encontrado"));

        Event mainEvent = subEvent.getEventoPrincipal();

        // ✅ VALIDACIÓN MEJORADA - Verificar si ya está inscrito al sub-evento
        boolean yaEstaInscritoSubEvento = inscriptionRepo.findAll().stream()
                .anyMatch(inscription ->
                        inscription.getUsuario() != null &&
                                inscription.getUsuario().getId().equals(user.getId()) &&
                                dto.getSubeventoId().equals(inscription.getSubeventoId()) &&
                                "confirmada".equals(inscription.getEstado()) &&
                                "subevento".equals(inscription.getTipoInscripcion())
                );

        if (yaEstaInscritoSubEvento) {
            throw new IllegalArgumentException("Ya estás inscrito en este sub-evento");
        }

        // Verificar que esté inscrito al evento principal
        boolean estaInscritoEventoPrincipal = inscriptionRepo.findAll().stream()
                .anyMatch(inscription ->
                        inscription.getUsuario() != null &&
                                inscription.getUsuario().getId().equals(user.getId()) &&
                                inscription.getEvento() != null &&
                                inscription.getEvento().getId().equals(mainEvent.getId()) &&
                                "confirmada".equals(inscription.getEstado()) &&
                                "evento_principal".equals(inscription.getTipoInscripcion())
                );

        if (!estaInscritoEventoPrincipal) {
            throw new IllegalArgumentException("Debes estar inscrito al evento principal para inscribirte a sus sub-eventos");
        }

        // Validar disponibilidad del sub-evento
        validateSubEventForRegistration(subEvent);

        // Crear inscripción al sub-evento
        Inscription inscription = Inscription.builder()
                .usuario(user)
                .evento(mainEvent)
                .subeventoId(dto.getSubeventoId())
                .fechaInscripcion(Instant.now())
                .estado("confirmada")
                .tipoInscripcion("subevento")
                .build();

        inscription = inscriptionRepo.save(inscription);
        updateSubEventAttendeeCount(subEvent);

        return inscription;
    }

    /**
     * ✅ VALIDACIONES DEL SUB-EVENTO
     */
    private void validateSubEventForRegistration(SubEvent subEvent) {
        // Verificar que el sub-evento esté activo
        if (subEvent.getStatus() == null || !"Active".equals(subEvent.getStatus().getNameState().name())) {
            throw new IllegalArgumentException("Solo puedes inscribirte a sub-eventos activos");
        }

        // Verificar capacidad
        if (subEvent.getMaxAttendees() != null &&
                subEvent.getCurrentAttendees() >= subEvent.getMaxAttendees()) {
            throw new IllegalArgumentException("Este sub-evento ya alcanzó su capacidad máxima");
        }

        // Verificar que no haya comenzado
        if (subEvent.getStart() != null && Instant.now().isAfter(subEvent.getStart())) {
            throw new IllegalArgumentException("No puedes inscribirte a un sub-evento que ya comenzó");
        }
    }

    /**
     * Cancela inscripción a un evento principal
     */
    @Transactional
    public void cancelRegistration(String username, String eventoId) {
        Users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // ✅ BÚSQUEDA MEJORADA de la inscripción
        Optional<Inscription> inscriptionOpt = inscriptionRepo.findAll().stream()
                .filter(inscription ->
                        inscription.getUsuario() != null &&
                                inscription.getUsuario().getId().equals(user.getId()) &&
                                inscription.getEvento() != null &&
                                inscription.getEvento().getId().equals(eventoId) &&
                                "confirmada".equals(inscription.getEstado()) &&
                                "evento_principal".equals(inscription.getTipoInscripcion())
                )
                .findFirst();

        if (inscriptionOpt.isEmpty()) {
            throw new IllegalArgumentException("No tienes una inscripción activa en este evento");
        }

        Inscription inscription = inscriptionOpt.get();
        Event event = inscription.getEvento();

        // Validar que se puede cancelar (con margen de tiempo)
        if (event.getStart() != null) {
            Instant twoHoursBefore = event.getStart().minusSeconds(2 * 60 * 60); // 2 horas antes
            if (Instant.now().isAfter(twoHoursBefore)) {
                throw new IllegalArgumentException("No puedes cancelar la inscripción menos de 2 horas antes del evento");
            }
        }

        // Cancelar inscripción
        inscription.setEstado("cancelada");
        inscriptionRepo.save(inscription);

        // Actualizar contador de asistentes
        updateEventAttendeeCount(event);

        // ✅ Cancelar automáticamente las inscripciones a sub-eventos
        cancelUserSubEventRegistrations(user.getId(), eventoId);
    }

    /**
     * ✅ MÉTODO AUXILIAR - Cancela inscripciones a sub-eventos cuando se cancela el evento principal
     */
    private void cancelUserSubEventRegistrations(String userId, String eventoId) {
        List<Inscription> subEventInscriptions = inscriptionRepo.findAll().stream()
                .filter(inscription ->
                        inscription.getUsuario() != null &&
                                inscription.getUsuario().getId().equals(userId) &&
                                inscription.getEvento() != null &&
                                inscription.getEvento().getId().equals(eventoId) &&
                                "confirmada".equals(inscription.getEstado()) &&
                                "subevento".equals(inscription.getTipoInscripcion())
                )
                .collect(Collectors.toList());

        for (Inscription subInscription : subEventInscriptions) {
            subInscription.setEstado("cancelada");
            inscriptionRepo.save(subInscription);

            // Actualizar contador del sub-evento
            if (subInscription.getSubeventoId() != null) {
                subEventRepo.findById(subInscription.getSubeventoId())
                        .ifPresent(this::updateSubEventAttendeeCount);
            }
        }
    }

    /**
     * Cancela inscripción a un sub-evento
     */
    @Transactional
    public void cancelSubEventRegistration(String username, String subeventoId) {
        Users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // ✅ BÚSQUEDA MEJORADA de la inscripción al sub-evento
        Optional<Inscription> inscriptionOpt = inscriptionRepo.findAll().stream()
                .filter(inscription ->
                        inscription.getUsuario() != null &&
                                inscription.getUsuario().getId().equals(user.getId()) &&
                                subeventoId.equals(inscription.getSubeventoId()) &&
                                "confirmada".equals(inscription.getEstado()) &&
                                "subevento".equals(inscription.getTipoInscripcion())
                )
                .findFirst();

        if (inscriptionOpt.isEmpty()) {
            throw new IllegalArgumentException("No tienes una inscripción activa en este sub-evento");
        }

        Inscription inscription = inscriptionOpt.get();
        SubEvent subEvent = subEventRepo.findById(subeventoId)
                .orElseThrow(() -> new IllegalArgumentException("Sub-evento no encontrado"));

        // Validar que se puede cancelar
        if (subEvent.getStart() != null) {
            Instant oneHourBefore = subEvent.getStart().minusSeconds(60 * 60); // 1 hora antes
            if (Instant.now().isAfter(oneHourBefore)) {
                throw new IllegalArgumentException("No puedes cancelar la inscripción menos de 1 hora antes del sub-evento");
            }
        }

        // Cancelar inscripción
        inscription.setEstado("cancelada");
        inscriptionRepo.save(inscription);

        // Actualizar contador del sub-evento
        updateSubEventAttendeeCount(subEvent);
    }

    /**
     * ✅ ACTUALIZA CONTADOR DE ASISTENTES DEL EVENTO
     */
    private void updateEventAttendeeCount(Event event) {
        long confirmedCount = inscriptionRepo.findAll().stream()
                .filter(inscription ->
                        inscription.getEvento() != null &&
                                inscription.getEvento().getId().equals(event.getId()) &&
                                "confirmada".equals(inscription.getEstado()) &&
                                "evento_principal".equals(inscription.getTipoInscripcion())
                )
                .count();

        event.setCurrentAttendees((int) confirmedCount);
        eventRepo.save(event);
    }

    /**
     * ✅ ACTUALIZA CONTADOR DE ASISTENTES DEL SUB-EVENTO
     */
    private void updateSubEventAttendeeCount(SubEvent subEvent) {
        long confirmedCount = inscriptionRepo.findAll().stream()
                .filter(inscription ->
                        subEvent.getId().equals(inscription.getSubeventoId()) &&
                                "confirmada".equals(inscription.getEstado()) &&
                                "subevento".equals(inscription.getTipoInscripcion())
                )
                .count();

        subEvent.setCurrentAttendees((int) confirmedCount);
        subEventRepo.save(subEvent);
    }

    // ================ MÉTODOS DE CONSULTA MEJORADOS ================

    /**
     * Lista eventos donde el usuario está inscrito
     */
    @Transactional(readOnly = true)
    public List<Inscription> getUserRegistrations(String username) {
        Users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return inscriptionRepo.findAll().stream()
                .filter(inscription ->
                        inscription.getUsuario() != null &&
                                inscription.getUsuario().getId().equals(user.getId()) &&
                                "confirmada".equals(inscription.getEstado()) &&
                                "evento_principal".equals(inscription.getTipoInscripcion())
                )
                .collect(Collectors.toList());
    }

    /**
     * Lista sub-eventos donde el usuario está inscrito
     */
    @Transactional(readOnly = true)
    public List<Inscription> getUserSubEventRegistrations(String username) {
        Users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return inscriptionRepo.findAll().stream()
                .filter(inscription ->
                        inscription.getUsuario() != null &&
                                inscription.getUsuario().getId().equals(user.getId()) &&
                                "confirmada".equals(inscription.getEstado()) &&
                                "subevento".equals(inscription.getTipoInscripcion())
                )
                .collect(Collectors.toList());
    }

    /**
     * Lista inscripciones de un evento (para organizadores)
     */
    @Transactional(readOnly = true)
    public List<Inscription> getEventRegistrations(String eventoId) {
        return inscriptionRepo.findAll().stream()
                .filter(inscription ->
                        inscription.getEvento() != null &&
                                inscription.getEvento().getId().equals(eventoId) &&
                                "confirmada".equals(inscription.getEstado()) &&
                                "evento_principal".equals(inscription.getTipoInscripcion())
                )
                .collect(Collectors.toList());
    }

    /**
     * Lista inscripciones de un sub-evento (para organizadores)
     */
    @Transactional(readOnly = true)
    public List<Inscription> getSubEventRegistrations(String subeventoId) {
        return inscriptionRepo.findAll().stream()
                .filter(inscription ->
                        subeventoId.equals(inscription.getSubeventoId()) &&
                                "confirmada".equals(inscription.getEstado()) &&
                                "subevento".equals(inscription.getTipoInscripcion())
                )
                .collect(Collectors.toList());
    }

    /**
     * Verifica si un usuario está inscrito en un evento
     */
    @Transactional(readOnly = true)
    public boolean isUserRegistered(String username, String eventoId) {
        Users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return inscriptionRepo.findAll().stream()
                .anyMatch(inscription ->
                        inscription.getUsuario() != null &&
                                inscription.getUsuario().getId().equals(user.getId()) &&
                                inscription.getEvento() != null &&
                                inscription.getEvento().getId().equals(eventoId) &&
                                "confirmada".equals(inscription.getEstado()) &&
                                "evento_principal".equals(inscription.getTipoInscripcion())
                );
    }

    /**
     * Verifica si un usuario está inscrito en un sub-evento
     */
    @Transactional(readOnly = true)
    public boolean isUserRegisteredToSubEvent(String username, String subeventoId) {
        Users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return inscriptionRepo.findAll().stream()
                .anyMatch(inscription ->
                        inscription.getUsuario() != null &&
                                inscription.getUsuario().getId().equals(user.getId()) &&
                                subeventoId.equals(inscription.getSubeventoId()) &&
                                "confirmada".equals(inscription.getEstado()) &&
                                "subevento".equals(inscription.getTipoInscripcion())
                );
    }

    // ================ ESTADÍSTICAS (MANTENIDAS) ================

    @Transactional(readOnly = true)
    public InscriptionStatsDto getEventInscriptionStats(String eventoId) {
        Event event = eventRepo.findById(eventoId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        long confirmed = inscriptionRepo.findAll().stream()
                .filter(inscription ->
                        inscription.getEvento() != null &&
                                inscription.getEvento().getId().equals(eventoId) &&
                                "confirmada".equals(inscription.getEstado()) &&
                                "evento_principal".equals(inscription.getTipoInscripcion())
                )
                .count();

        long canceled = inscriptionRepo.findAll().stream()
                .filter(inscription ->
                        inscription.getEvento() != null &&
                                inscription.getEvento().getId().equals(eventoId) &&
                                "cancelada".equals(inscription.getEstado()) &&
                                "evento_principal".equals(inscription.getTipoInscripcion())
                )
                .count();

        long available = Math.max(0, event.getMaxAttendees() - confirmed);
        double occupancyRate = event.getMaxAttendees() > 0 ?
                (double) confirmed / event.getMaxAttendees() * 100 : 0;

        return new InscriptionStatsDto(
                (int) confirmed,
                (int) canceled,
                (int) available,
                event.getMaxAttendees(),
                occupancyRate
        );
    }

    @Transactional(readOnly = true)
    public InscriptionStatsDto getSubEventInscriptionStats(String subeventoId) {
        SubEvent subEvent = subEventRepo.findById(subeventoId)
                .orElseThrow(() -> new IllegalArgumentException("Sub-evento no encontrado"));

        long confirmed = inscriptionRepo.findAll().stream()
                .filter(inscription ->
                        subeventoId.equals(inscription.getSubeventoId()) &&
                                "confirmada".equals(inscription.getEstado()) &&
                                "subevento".equals(inscription.getTipoInscripcion())
                )
                .count();

        long canceled = inscriptionRepo.findAll().stream()
                .filter(inscription ->
                        subeventoId.equals(inscription.getSubeventoId()) &&
                                "cancelada".equals(inscription.getEstado()) &&
                                "subevento".equals(inscription.getTipoInscripcion())
                )
                .count();

        long available = Math.max(0, subEvent.getMaxAttendees() - confirmed);
        double occupancyRate = subEvent.getMaxAttendees() > 0 ?
                (double) confirmed / subEvent.getMaxAttendees() * 100 : 0;

        return new InscriptionStatsDto(
                (int) confirmed,
                (int) canceled,
                (int) available,
                subEvent.getMaxAttendees(),
                occupancyRate
        );
    }

    // DTO para estadísticas (mantenido igual)
    public static class InscriptionStatsDto {
        private int confirmed;
        private int canceled;
        private int available;
        private int maxAttendees;
        private double occupancyRate;

        public InscriptionStatsDto(int confirmed, int canceled, int available,
                                   int maxAttendees, double occupancyRate) {
            this.confirmed = confirmed;
            this.canceled = canceled;
            this.available = available;
            this.maxAttendees = maxAttendees;
            this.occupancyRate = occupancyRate;
        }

        // Getters y setters
        public int getConfirmed() { return confirmed; }
        public void setConfirmed(int confirmed) { this.confirmed = confirmed; }

        public int getCanceled() { return canceled; }
        public void setCanceled(int canceled) { this.canceled = canceled; }

        public int getAvailable() { return available; }
        public void setAvailable(int available) { this.available = available; }

        public int getMaxAttendees() { return maxAttendees; }
        public void setMaxAttendees(int maxAttendees) { this.maxAttendees = maxAttendees; }

        public double getOccupancyRate() { return occupancyRate; }
        public void setOccupancyRate(double occupancyRate) { this.occupancyRate = occupancyRate; }
    }
}