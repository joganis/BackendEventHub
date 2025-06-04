// Nuevo: AdminEventService
package com.eventHub.backend_eventHub.events.service;

import com.eventHub.backend_eventHub.events.entities.Event;
import com.eventHub.backend_eventHub.events.entities.HistoryRecord;
import com.eventHub.backend_eventHub.events.repository.EventRepository;
import com.eventHub.backend_eventHub.domain.entities.State;
import com.eventHub.backend_eventHub.domain.repositories.StateRepository;
import com.eventHub.backend_eventHub.domain.enums.StateList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminEventService {
    @Autowired private EventRepository eventRepo;
    @Autowired private StateRepository stateRepo;

    /**
     * Lista todos los eventos para administradores (incluye bloqueados)
     */
    @Transactional(readOnly = true)
    public Page<Event> listAllEventsForAdmin(String status, Pageable pageable) {
        if (status == null) {
            return eventRepo.findAll(pageable);
        }
        return eventRepo.findByStatusNameStateIgnoreCase(status, pageable);
    }

    /**
     * Lista eventos por estado de bloqueo
     */
    @Transactional(readOnly = true)
    public List<Event> listEventsByBlockStatus(boolean bloqueado) {
        return eventRepo.findByBloqueado(bloqueado);
    }

    /**
     * Bloquea o desbloquea un evento
     */
    @Transactional
    public Event toggleEventBlock(String eventId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        event.setBloqueado(!event.isBloqueado());

        // Agregar al historial
        if (event.getHistory() == null) {
            event.setHistory(new ArrayList<>());
        }

        String action = event.isBloqueado() ? "blocked" : "unblocked";
        event.getHistory().add(HistoryRecord.builder()
                .field("bloqueado")
                .oldValue(String.valueOf(!event.isBloqueado()))
                .newValue(String.valueOf(event.isBloqueado()))
                .changedAt(Instant.now())
                .build());

        return eventRepo.save(event);
    }

    /**
     * Cambia el estado de un evento
     */
    @Transactional
    public Event changeEventStatus(String eventId, String newStatus) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        try {
            StateList stateEnum = StateList.valueOf(newStatus);
            State state = stateRepo.findByNameState(stateEnum)
                    .orElseThrow(() -> new IllegalArgumentException("Estado no encontrado: " + newStatus));

            String oldStatus = event.getStatus() != null ? event.getStatus().getNameState().name() : "null";
            event.setStatus(state);

            // Agregar al historial
            if (event.getHistory() == null) {
                event.setHistory(new ArrayList<>());
            }

            event.getHistory().add(HistoryRecord.builder()
                    .field("status")
                    .oldValue(oldStatus)
                    .newValue(newStatus)
                    .changedAt(Instant.now())
                    .build());

            return eventRepo.save(event);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado inválido: " + newStatus);
        }
    }

    /**
     * Obtiene estadísticas de eventos para dashboard admin
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getEventStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("total", eventRepo.count());
        stats.put("active", eventRepo.countByStatusNameStateIgnoreCase("Active"));
        stats.put("blocked", eventRepo.countByBloqueado(true));
        stats.put("canceled", eventRepo.countByStatusNameStateIgnoreCase("Canceled"));
        stats.put("pending", eventRepo.countByStatusNameStateIgnoreCase("Pending"));

        return stats;
    }
}