// Nuevo: AdminEventService
package com.eventHub.backend_eventHub.events.service;

import com.eventHub.backend_eventHub.events.entities.Event;
import com.eventHub.backend_eventHub.events.entities.HistoryRecord;
import com.eventHub.backend_eventHub.events.repository.EventRepository;
import com.eventHub.backend_eventHub.domain.entities.State;
import com.eventHub.backend_eventHub.domain.repositories.StateRepository;
import com.eventHub.backend_eventHub.domain.enums.StateList;
import jakarta.annotation.PostConstruct;
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

    // CACHE de IDs de estados
    private Map<StateList, String> stateIdsCache = new HashMap<>();

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

        // Total de eventos
        stats.put("total", eventRepo.count());

        // Estadísticas por estado usando IDs
        stats.put("active", eventRepo.countByStatusId(getStateId(StateList.Active)));
        stats.put("canceled", eventRepo.countByStatusId(getStateId(StateList.Canceled)));
        stats.put("pending", eventRepo.countByStatusId(getStateId(StateList.Pending)));
        stats.put("inactive", eventRepo.countByStatusId(getStateId(StateList.Inactive)));
        stats.put("blocked_state", eventRepo.countByStatusId(getStateId(StateList.Blocked)));

        // Eventos bloqueados por campo directo
        stats.put("blocked", eventRepo.countByBloqueado(true));

        //  Estadísticas adicionales útiles
        stats.put("public_events", eventRepo.countPublicEventsByStatusId(getStateId(StateList.Active)));
        stats.put("private_events", eventRepo.countPrivateEventsByStatusId(getStateId(StateList.Active)));
        stats.put("featured_events", eventRepo.countFeaturedEventsByStatusId(getStateId(StateList.Active)));
        return stats;
    }

        /**
         * Obtiene el ID de un estado específico (con cache)
         */
    private String getStateId(StateList stateEnum) {
        if (!stateIdsCache.containsKey(stateEnum)) {
            State state = stateRepo.findByNameState(stateEnum)
                    .orElseThrow(() -> new RuntimeException("Estado " + stateEnum + " no encontrado"));
            stateIdsCache.put(stateEnum, state.getId());
            System.out.println("✅ Estado " + stateEnum + " ID cacheado: " + state.getId());
        }
        return stateIdsCache.get(stateEnum);
    }
    /**
     * Inicializa todos los IDs de estados de una vez (opcional)
     */
    @PostConstruct
    private void initializeStateCache() {
        System.out.println("🚀 Inicializando cache de estados...");

        // Cargar todos los estados de una vez
        List<State> allStates = stateRepo.findAll();
        for (State state : allStates) {
            stateIdsCache.put(state.getNameState(), state.getId());
            System.out.println("✅ Estado " + state.getNameState() + " cacheado: " + state.getId());
        }

        System.out.println("🎯 Cache de estados completado: " + stateIdsCache.size() + " estados");
    }
}