// ================================
// EventController CORREGIDO - Para gestión de eventos
// ================================

package com.eventHub.backend_eventHub.events.controller;

import com.eventHub.backend_eventHub.events.dto.*;
import com.eventHub.backend_eventHub.events.entities.Event;
import com.eventHub.backend_eventHub.events.entities.EventRole;
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
import java.util.List;

@Tag(name = "Eventos", description = "Gestión completa de eventos")
@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    @Autowired
    private EventService eventService;

    // ====== ENDPOINTS PÚBLICOS (Sin autenticación) ======

    @Operation(summary = "Búsqueda de eventos públicos",
            description = "Busca eventos públicos con filtros opcionales")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de eventos obtenida correctamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/search")
    public ResponseEntity<List<EventSummaryDto>> searchEvents(@RequestBody(required = false) EventFilterDto filter) {
        try {
            List<EventSummaryDto> events = eventService.listPublicEvents(filter);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al buscar eventos: " + e.getMessage());
        }
    }

    @Operation(summary = "Eventos destacados", description = "Lista eventos marcados como destacados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de eventos destacados"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/featured")
    public ResponseEntity<List<EventSummaryDto>> getFeaturedEvents() {
        try {
            List<EventSummaryDto> events = eventService.listFeaturedEvents();
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener eventos destacados: " + e.getMessage());
        }
    }

    @Operation(summary = "Próximos eventos", description = "Lista eventos que están por suceder")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de próximos eventos"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/upcoming")
    public ResponseEntity<List<EventSummaryDto>> getUpcomingEvents() {
        try {
            List<EventSummaryDto> events = eventService.listUpcomingEvents();
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener próximos eventos: " + e.getMessage());
        }
    }

    @Operation(summary = "Eventos recientes", description = "Lista eventos creados recientemente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de eventos recientes"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/recent")
    public ResponseEntity<List<EventSummaryDto>> getRecentEvents() {
        try {
            List<EventSummaryDto> events = eventService.listRecentEvents();
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener eventos recientes: " + e.getMessage());
        }
    }

    @Operation(summary = "Detalle de evento público", description = "Obtiene información completa de un evento público")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento encontrado"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado o no es público"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventDetails(@PathVariable String id) {
        try {
            Event event = eventService.getEventDetails(id);
            return ResponseEntity.ok(event);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener evento: " + e.getMessage());
        }
    }

    // ====== ENDPOINTS AUTENTICADOS ======

    @Operation(summary = "Búsqueda de eventos para usuario autenticado",
            description = "Busca eventos accesibles para el usuario (públicos + privados con acceso)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de eventos accesibles"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/search-authenticated")
    @PreAuthorize("hasRole('USUARIO')")
    public ResponseEntity<List<EventSummaryDto>> searchEventsAuthenticated(
            Principal principal,
            @RequestBody(required = false) EventFilterDto filter) {
        try {
            List<EventSummaryDto> events = eventService.listAccessibleEvents(principal.getName(), filter);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al buscar eventos: " + e.getMessage());
        }
    }

    @Operation(summary = "Detalle de evento para usuario autenticado",
            description = "Obtiene información completa de un evento (incluye privados si tiene acceso)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado o sin acceso"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/{id}/authenticated")
    @PreAuthorize("hasRole('USUARIO')")
    public ResponseEntity<Event> getEventDetailsAuthenticated(@PathVariable String id, Principal principal) {
        try {
            Event event = eventService.getEventDetails(id, principal.getName());
            return ResponseEntity.ok(event);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener evento: " + e.getMessage());
        }
    }

    // ====== GESTIÓN DE EVENTOS ======

    @Operation(summary = "Crear evento", description = "Crea un nuevo evento (requiere autenticación)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Evento creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de evento inválidos"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @PostMapping
    public ResponseEntity<Event> createEvent(Principal principal, @Valid @RequestBody EventDto dto) {
        try {
            Event event = eventService.createEvent(principal.getName(), dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(event);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al crear evento: " + e.getMessage());
        }
    }

    @Operation(summary = "Mis eventos creados", description = "Lista eventos creados por el usuario autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de eventos creados"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @GetMapping("/my-created")
    public ResponseEntity<List<Event>> getMyCreatedEvents(Principal principal) {
        try {
            List<Event> events = eventService.listMyCreatedEvents(principal.getName());
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener mis eventos: " + e.getMessage());
        }
    }

    @Operation(summary = "Eventos como subcreador",
            description = "Lista eventos donde el usuario es subcreador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de eventos como subcreador"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @GetMapping("/as-subcreator")
    public ResponseEntity<List<Event>> getEventsAsSubcreator(Principal principal) {
        try {
            List<Event> events = eventService.listEventsAsSubcreator(principal.getName());
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener eventos como subcreador: " + e.getMessage());
        }
    }

    @Operation(summary = "Actualizar evento",
            description = "Actualiza un evento (solo creador o subcreador)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de actualización inválidos o sin permisos"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable String id,
                                             Principal principal,
                                             @Valid @RequestBody UpdateEventDto dto) {
        try {
            Event event = eventService.updateEvent(id, principal.getName(), dto);
            return ResponseEntity.ok(event);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al actualizar evento: " + e.getMessage());
        }
    }

    @Operation(summary = "Eliminar evento", description = "Elimina un evento (solo creador)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Evento eliminado correctamente"),
            @ApiResponse(responseCode = "400", description = "Sin permisos para eliminar"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable String id, Principal principal) {
        try {
            eventService.deleteEvent(id, principal.getName());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al eliminar evento: " + e.getMessage());
        }
    }

    // ====== GESTIÓN DE SUBCREADORES ======

    @Operation(summary = "Invitar subcreador", description = "Invita a un usuario como subcreador del evento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitación enviada correctamente"),
            @ApiResponse(responseCode = "400", description = "Error en los datos o sin permisos"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @PostMapping("/{id}/invite-subcreator")
    public ResponseEntity<String> inviteSubcreator(@PathVariable String id,
                                                   Principal principal,
                                                   @Valid @RequestBody EventRoleDto dto) {
        try {
            eventService.inviteSubcreator(id, principal.getName(), dto);
            return ResponseEntity.ok("Invitación enviada correctamente");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al invitar subcreador: " + e.getMessage());
        }
    }

    @Operation(summary = "Aceptar invitación", description = "Acepta invitación como subcreador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitación aceptada correctamente"),
            @ApiResponse(responseCode = "400", description = "Invitación inválida o expirada"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @PostMapping("/accept-invitation/{invitationId}")
    public ResponseEntity<String> acceptInvitation(@PathVariable String invitationId,
                                                   Principal principal) {
        try {
            eventService.acceptSubcreatorInvitation(principal.getName(), invitationId);
            return ResponseEntity.ok("Invitación aceptada correctamente");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al aceptar invitación: " + e.getMessage());
        }
    }
}