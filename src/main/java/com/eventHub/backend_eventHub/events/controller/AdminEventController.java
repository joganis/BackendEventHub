// Nuevo: AdminEventController
package com.eventHub.backend_eventHub.events.controller;

import com.eventHub.backend_eventHub.events.entities.Event;
import com.eventHub.backend_eventHub.events.service.AdminEventService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@Tag(name = "Administración de Eventos", description = "Endpoints para administradores")
@RestController
@RequestMapping("/api/admin/events")
@CrossOrigin(origins = "*")
public class AdminEventController {

    @Autowired
    private AdminEventService adminEventService;

    @Operation(summary = "Listar todos los eventos",
            description = "Lista todos los eventos para administradores (incluye bloqueados)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUBADMIN')")
    @GetMapping
    public ResponseEntity<Page<Event>> getAllEvents(@RequestParam(required = false) String status,
                                                    Pageable pageable) {
        try {
            Page<Event> events = adminEventService.listAllEventsForAdmin(status, pageable);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al listar eventos: " + e.getMessage());
        }
    }

    @Operation(summary = "Eventos por estado de bloqueo",
            description = "Lista eventos bloqueados o desbloqueados")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUBADMIN')")
    @GetMapping("/by-block-status")
    public ResponseEntity<List<Event>> getEventsByBlockStatus(@RequestParam boolean bloqueado) {
        try {
            List<Event> events = adminEventService.listEventsByBlockStatus(bloqueado);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener eventos: " + e.getMessage());
        }
    }

    @Operation(summary = "Bloquear/Desbloquear evento",
            description = "Cambia el estado de bloqueo de un evento")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUBADMIN')")
    @PatchMapping("/{id}/toggle-block")
    public ResponseEntity<Event> toggleEventBlock(@PathVariable String id) {
        try {
            Event event = adminEventService.toggleEventBlock(id);
            return ResponseEntity.ok(event);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al cambiar estado de bloqueo: " + e.getMessage());
        }
    }

    @Operation(summary = "Cambiar estado de evento",
            description = "Cambia el estado de un evento (Active, Inactive, etc.)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<Event> changeEventStatus(@PathVariable String id,
                                                   @RequestParam String newStatus) {
        try {
            Event event = adminEventService.changeEventStatus(id, newStatus);
            return ResponseEntity.ok(event);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al cambiar estado: " + e.getMessage());
        }
    }

    @Operation(summary = "Estadísticas de eventos",
            description = "Obtiene estadísticas generales de eventos para dashboard")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUBADMIN')")
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getEventStatistics() {
        try {
            Map<String, Object> stats = adminEventService.getEventStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener estadísticas: " + e.getMessage());
        }
    }
}