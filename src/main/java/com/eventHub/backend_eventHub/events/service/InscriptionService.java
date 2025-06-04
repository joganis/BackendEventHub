// InscriptionService completo
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

    /**
     * Inscribe un usuario a un evento principal
     */
    @Transactional
    public Inscription registerToEvent(String username, InscriptionDto dto) {
        Users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Verificar si ya está inscrito
        Optional<Inscription> existingInscription = inscriptionRepo
                .findByUsuarioUserNameAndEventoIdAndEstado(username, dto.getEventoId(), "confirmada");

        if (existingInscription.isPresent()) {
            throw new IllegalArgumentException("Ya estás inscrito en este evento");
        }

        Event event = eventRepo.findById(dto.getEventoId())
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        // Verificar disponibilidad
        if (event.getCurrentAttendees() >= event.getMaxAttendees()) {
            throw new IllegalArgumentException("Evento lleno - no hay cupos disponibles");
        }

        if (!event.isPermitirInscripciones()) {
            throw new IllegalArgumentException("Las inscripciones están cerradas para este evento");
        }

        if (event.isBloqueado()) {
            throw new IllegalArgumentException("Este evento no está disponible");
        }

        // Verificar que el evento esté activo
        if (!"Active".equals(event.getStatus().getNameState().name())) {
            throw new IllegalArgumentException("Solo puedes inscribirte a eventos activos");
        }

        // Verificar fecha límite
        if (event.getFechaLimiteInscripcion() != null &&
                Instant.now().isAfter(event.getFechaLimiteInscripcion())) {
            throw new IllegalArgumentException("La fecha límite de inscripción ha expirado");
        }

        // Verificar que el evento no haya comenzado
        if (Instant.now().isAfter(event.getStart())) {
            throw new IllegalArgumentException("No puedes inscribirte a un evento que ya ha comenzado");
        }

        // Crear inscripción
        Inscription inscription = Inscription.builder()
                .usuario(user)
                .evento(event)
                .fechaInscripcion(Instant.now())
                .estado("confirmada")
                .tipoInscripcion("evento_principal")
                .build();

        inscription = inscriptionRepo.save(inscription);

        // Actualizar contador de asistentes
        event.setCurrentAttendees(event.getCurrentAttendees() + 1);
        eventRepo.save(event);

        return inscription;
    }

    /**
     * Inscribe un usuario a un sub-evento
     */
    @Transactional
    public Inscription registerToSubEvent(String username, InscriptionDto dto) {
        Users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Verificar si ya está inscrito al sub-evento
        Optional<Inscription> existingInscription = inscriptionRepo
                .findByUsuarioUserNameAndSubeventoIdAndEstado(username, dto.getSubeventoId(), "confirmada");

        if (existingInscription.isPresent()) {
            throw new IllegalArgumentException("Ya estás inscrito en este sub-evento");
        }

        SubEvent subEvent = subEventRepo.findById(dto.getSubeventoId())
                .orElseThrow(() -> new IllegalArgumentException("Sub-evento no encontrado"));

        Event mainEvent = subEvent.getEventoPrincipal();

        // Verificar que esté inscrito al evento principal
        Optional<Inscription> mainEventInscription = inscriptionRepo
                .findByUsuarioUserNameAndEventoIdAndEstado(username, mainEvent.getId(), "confirmada");

        if (mainEventInscription.isEmpty()) {
            throw new IllegalArgumentException("Debes estar inscrito al evento principal para inscribirte a sus sub-eventos");
        }

        // Verificar disponibilidad del sub-evento
        if (subEvent.getCurrentAttendees() >= subEvent.getMaxAttendees()) {
            throw new IllegalArgumentException("Sub-evento lleno - no hay cupos disponibles");
        }

        // Verificar que el sub-evento esté activo
        if (!"Active".equals(subEvent.getStatus().getNameState().name())) {
            throw new IllegalArgumentException("Solo puedes inscribirte a sub-eventos activos");
        }

        // Verificar que el sub-evento no haya comenzado
        if (Instant.now().isAfter(subEvent.getStart())) {
            throw new IllegalArgumentException("No puedes inscribirte a un sub-evento que ya ha comenzado");
        }

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

        // Actualizar contador de asistentes del sub-evento
        subEvent.setCurrentAttendees(subEvent.getCurrentAttendees() + 1);
        subEventRepo.save(subEvent);

        return inscription;
    }

    /**
     * Cancela inscripción a un evento principal
     */
    @Transactional
    public void cancelRegistration(String username, String eventoId) {
        Inscription inscription = inscriptionRepo
                .findByUsuarioUserNameAndEventoIdAndEstado(username, eventoId, "confirmada")
                .orElseThrow(() -> new IllegalArgumentException("No tienes una inscripción activa en este evento"));

        // Verificar que el evento no haya comenzado
        Event event = eventRepo.findById(eventoId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        if (Instant.now().isAfter(event.getStart())) {
            throw new IllegalArgumentException("No puedes cancelar la inscripción de un evento que ya ha comenzado");
        }

        // Cancelar inscripción
        inscription.setEstado("cancelada");
        inscriptionRepo.save(inscription);

        // Restaurar contador de asistentes
        event.setCurrentAttendees(Math.max(0, event.getCurrentAttendees() - 1));
        eventRepo.save(event);

        // Cancelar automáticamente las inscripciones a sub-eventos
        List<Inscription> subEventInscriptions = inscriptionRepo
                .findByUsuarioUserNameAndEventoIdAndEstado(username, eventoId, "confirmada")
                .stream()
                .filter(insc -> "subevento".equals(insc.getTipoInscripcion()))
                .collect(Collectors.toList());

        for (Inscription subInscription : subEventInscriptions) {
            cancelSubEventRegistration(username, subInscription.getSubeventoId());
        }
    }

    /**
     * Cancela inscripción a un sub-evento
     */
    @Transactional
    public void cancelSubEventRegistration(String username, String subeventoId) {
        Inscription inscription = inscriptionRepo
                .findByUsuarioUserNameAndSubeventoIdAndEstado(username, subeventoId, "confirmada")
                .orElseThrow(() -> new IllegalArgumentException("No tienes una inscripción activa en este sub-evento"));

        // Verificar que el sub-evento no haya comenzado
        SubEvent subEvent = subEventRepo.findById(subeventoId)
                .orElseThrow(() -> new IllegalArgumentException("Sub-evento no encontrado"));

        if (Instant.now().isAfter(subEvent.getStart())) {
            throw new IllegalArgumentException("No puedes cancelar la inscripción de un sub-evento que ya ha comenzado");
        }

        // Cancelar inscripción
        inscription.setEstado("cancelada");
        inscriptionRepo.save(inscription);

        // Restaurar contador de asistentes del sub-evento
        subEvent.setCurrentAttendees(Math.max(0, subEvent.getCurrentAttendees() - 1));
        subEventRepo.save(subEvent);
    }

    /**
     * Lista eventos donde el usuario está inscrito
     */
    @Transactional(readOnly = true)
    public List<Inscription> getUserRegistrations(String username) {
        return inscriptionRepo.findByUsuarioUserNameAndEstado(username, "confirmada")
                .stream()
                .filter(inscription -> "evento_principal".equals(inscription.getTipoInscripcion()))
                .collect(Collectors.toList());
    }

    /**
     * Lista sub-eventos donde el usuario está inscrito
     */
    @Transactional(readOnly = true)
    public List<Inscription> getUserSubEventRegistrations(String username) {
        return inscriptionRepo.findByUsuarioUserNameAndEstado(username, "confirmada")
                .stream()
                .filter(inscription -> "subevento".equals(inscription.getTipoInscripcion()))
                .collect(Collectors.toList());
    }

    /**
     * Lista todas las inscripciones de un usuario (eventos y sub-eventos)
     */
    @Transactional(readOnly = true)
    public List<Inscription> getAllUserRegistrations(String username) {
        return inscriptionRepo.findByUsuarioUserNameAndEstado(username, "confirmada");
    }

    /**
     * Lista inscripciones de un evento (para organizadores)
     */
    @Transactional(readOnly = true)
    public List<Inscription> getEventRegistrations(String eventoId) {
        return inscriptionRepo.findByEventoIdAndEstado(eventoId, "confirmada")
                .stream()
                .filter(inscription -> "evento_principal".equals(inscription.getTipoInscripcion()))
                .collect(Collectors.toList());
    }

    /**
     * Lista inscripciones de un sub-evento (para organizadores)
     */
    @Transactional(readOnly = true)
    public List<Inscription> getSubEventRegistrations(String subeventoId) {
        return inscriptionRepo.findBySubeventoIdAndEstado(subeventoId, "confirmada");
    }

    /**
     * Verifica si un usuario está inscrito en un evento
     */
    @Transactional(readOnly = true)
    public boolean isUserRegistered(String username, String eventoId) {
        return inscriptionRepo.findByUsuarioUserNameAndEventoIdAndEstado(username, eventoId, "confirmada")
                .stream()
                .anyMatch(inscription -> "evento_principal".equals(inscription.getTipoInscripcion()));
    }

    /**
     * Verifica si un usuario está inscrito en un sub-evento
     */
    @Transactional(readOnly = true)
    public boolean isUserRegisteredToSubEvent(String username, String subeventoId) {
        return inscriptionRepo.findByUsuarioUserNameAndSubeventoIdAndEstado(username, subeventoId, "confirmada")
                .isPresent();
    }

    /**
     * Obtiene estadísticas de inscripciones para un evento
     */
    @Transactional(readOnly = true)
    public InscriptionStatsDto getEventInscriptionStats(String eventoId) {
        Event event = eventRepo.findById(eventoId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        long confirmed = inscriptionRepo.countByEventoIdAndEstado(eventoId, "confirmada");
        long canceled = inscriptionRepo.countByEventoIdAndEstado(eventoId, "cancelada");
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

    /**
     * Obtiene estadísticas de inscripciones para un sub-evento
     */
    @Transactional(readOnly = true)
    public InscriptionStatsDto getSubEventInscriptionStats(String subeventoId) {
        SubEvent subEvent = subEventRepo.findById(subeventoId)
                .orElseThrow(() -> new IllegalArgumentException("Sub-evento no encontrado"));

        long confirmed = inscriptionRepo.countBySubeventoIdAndEstado(subeventoId, "confirmada");
        long canceled = inscriptionRepo.countBySubeventoIdAndEstado(subeventoId, "cancelada");
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

    // DTO para estadísticas de inscripciones
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