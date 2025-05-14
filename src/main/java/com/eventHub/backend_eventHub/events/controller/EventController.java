package com.eventHub.backend_eventHub.events.controller;


import com.eventHub.backend_eventHub.events.dto.*;

import com.eventHub.backend_eventHub.events.entities.Event;
import com.eventHub.backend_eventHub.events.service.EventService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Instant;
import java.util.HashMap;

import java.util.List;
import java.util.Map;

@Tag(name = "Eventos", description = "Gestión completa de eventos")
@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    @Autowired
    private EventService eventService;

    @Operation(summary = "Listar eventos", description = "Permite filtrar opcionalmente por estado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de eventos obtenida correctamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping
    public ResponseEntity<List<Event>> list(
            @RequestParam(required = false) String status) {
        try {
            List<Event> events = eventService.listAll(status);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al listar eventos: " + e.getMessage());
        }
    }

    @Operation(summary = "Ver detalles de un evento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento encontrado"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Event> getById(@PathVariable String id) {
        try {
            Event event = eventService.getById(id);
            return ResponseEntity.ok(event);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener evento: " + e.getMessage());
        }
    }

    @Operation(summary = "Crear nuevo evento", description = "Requiere rol USUARIO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Evento creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de evento inválidos"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @PostMapping
    public ResponseEntity<Event> create(
            Principal principal,
            @Valid @RequestBody EventDto dto) {
        try {
            Event event = eventService.create(principal.getName(), dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(event);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al crear evento: " + e.getMessage());
        }
    }

    @Operation(summary = "Actualizar evento existente",
            description = "Solo permitido al creador o administradores del evento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de actualización inválidos"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Prohibido - No es creador ni administrador"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("@eventService.isCreatorOrAdmin(#id, authentication.name) or hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Event> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateEventDto dto) {
        try {
            Event event = eventService.update(id, dto);
            return ResponseEntity.ok(event);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al actualizar evento: " + e.getMessage());
        }
    }

    @Operation(summary = "Cambiar estado de un evento",
            description = "Solo permitido a administradores")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado cambiado correctamente"),
            @ApiResponse(responseCode = "400", description = "Estado inválido"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Prohibido - No es administrador"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<Event> changeStatus(
            @PathVariable String id,
            @RequestParam String newStatus) {
        try {
            Event event = eventService.changeStatus(id, newStatus);
            return ResponseEntity.ok(event);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al cambiar estado: " + e.getMessage());
        }
    }

    @Operation(summary = "Obtener resumen de eventos",
            description = "Muestra contadores de eventos por estado, tipo, etc.")
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getEventsSummary() {
        try {
            Map<String, Object> summary = new HashMap<>();

            // Contar eventos por estado
            summary.put("active", eventService.listAll("Active").size());
            summary.put("canceled", eventService.listAll("Canceled").size());
            summary.put("completed", eventService.listAll("Completed").size());

            // Total de eventos
            summary.put("total", eventService.listAll(null).size());

            // Próximos eventos (a partir de hoy)
            List<Event> upcomingEvents = eventService.listAll("Active")
                    .stream()
                    .filter(event -> event.getStart().isAfter(Instant.now()))
                    .toList();
            summary.put("upcoming", upcomingEvents.size());

            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener resumen: " + e.getMessage());
        }
    }
}