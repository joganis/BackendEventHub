// SubEventController limpio y completo
package com.eventHub.backend_eventHub.events.controller;

import com.eventHub.backend_eventHub.events.dto.SubEventDto;
import com.eventHub.backend_eventHub.events.entities.SubEvent;
import com.eventHub.backend_eventHub.events.service.SubEventService;
import com.eventHub.backend_eventHub.events.service.InscriptionService;
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

@Tag(name = "Sub-Eventos", description = "Gestión de sub-eventos")
@RestController
@RequestMapping("/api/subevents")
@CrossOrigin(origins = "*")
public class SubEventController {

    @Autowired
    private SubEventService subEventService;

    @Autowired
    private InscriptionService inscriptionService;

    @Operation(summary = "Crear sub-evento", description = "Crea un nuevo sub-evento dentro de un evento principal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sub-evento creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o sin permisos"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @PostMapping
    public ResponseEntity<SubEvent> createSubEvent(Principal principal,
                                                   @Valid @RequestBody SubEventDto dto) {
        try {
            SubEvent subEvent = subEventService.createSubEvent(principal.getName(), dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(subEvent);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al crear sub-evento: " + e.getMessage());
        }
    }

    @Operation(summary = "Listar sub-eventos", description = "Lista sub-eventos de un evento principal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de sub-eventos obtenida correctamente"),
            @ApiResponse(responseCode = "404", description = "Evento principal no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/by-event/{eventoPrincipalId}")
    public ResponseEntity<List<SubEvent>> getSubEventsByMainEvent(@PathVariable String eventoPrincipalId) {
        try {
            List<SubEvent> subEvents = subEventService.getSubEventsByMainEvent(eventoPrincipalId);
            return ResponseEntity.ok(subEvents);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener sub-eventos: " + e.getMessage());
        }
    }

    @Operation(summary = "Obtener sub-evento", description = "Obtiene detalles de un sub-evento específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sub-evento encontrado"),
            @ApiResponse(responseCode = "404", description = "Sub-evento no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/{id}")
    public ResponseEntity<SubEvent> getSubEvent(@PathVariable String id) {
        try {
            SubEvent subEvent = subEventService.getSubEventById(id);
            return ResponseEntity.ok(subEvent);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener sub-evento: " + e.getMessage());
        }
    }

    @Operation(summary = "Actualizar sub-evento", description = "Actualiza un sub-evento (solo creador o subcreador del evento principal)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sub-evento actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o sin permisos"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Sub-evento no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @PutMapping("/{id}")
    public ResponseEntity<SubEvent> updateSubEvent(@PathVariable String id,
                                                   Principal principal,
                                                   @Valid @RequestBody SubEventDto dto) {
        try {
            SubEvent subEvent = subEventService.updateSubEvent(id, principal.getName(), dto);
            return ResponseEntity.ok(subEvent);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al actualizar sub-evento: " + e.getMessage());
        }
    }

    @Operation(summary = "Eliminar sub-evento", description = "Elimina un sub-evento (solo creador o subcreador del evento principal)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Sub-evento eliminado correctamente"),
            @ApiResponse(responseCode = "400", description = "Sin permisos para eliminar"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Sub-evento no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubEvent(@PathVariable String id, Principal principal) {
        try {
            subEventService.deleteSubEvent(id, principal.getName());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al eliminar sub-evento: " + e.getMessage());
        }
    }

    @Operation(summary = "Mis sub-eventos creados", description = "Lista sub-eventos creados por el usuario autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de sub-eventos creados"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @GetMapping("/my-created")
    public ResponseEntity<List<SubEvent>> getMyCreatedSubEvents(Principal principal) {
        try {
            List<SubEvent> subEvents = subEventService.getSubEventsByCreator(principal.getName());
            return ResponseEntity.ok(subEvents);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener mis sub-eventos: " + e.getMessage());
        }
    }

    @Operation(summary = "Inscripciones del sub-evento",
            description = "Lista usuarios inscritos a un sub-evento (solo para organizadores)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de inscripciones del sub-evento"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para ver inscripciones"),
            @ApiResponse(responseCode = "404", description = "Sub-evento no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @GetMapping("/{id}/registrations")
    public ResponseEntity<List<com.eventHub.backend_eventHub.events.entities.Inscription>> getSubEventRegistrations(
            @PathVariable String id, Principal principal) {
        try {
            // Verificar que el usuario tenga permisos para ver las inscripciones
            SubEvent subEvent = subEventService.getSubEventById(id);

            var inscriptions = inscriptionService.getSubEventRegistrations(id);
            return ResponseEntity.ok(inscriptions);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener inscripciones del sub-evento: " + e.getMessage());
        }
    }

    @Operation(summary = "Estadísticas del sub-evento",
            description = "Obtiene estadísticas de inscripciones del sub-evento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estadísticas obtenidas correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Sub-evento no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @GetMapping("/{id}/stats")
    public ResponseEntity<InscriptionService.InscriptionStatsDto> getSubEventStats(
            @PathVariable String id, Principal principal) {
        try {
            var stats = inscriptionService.getSubEventInscriptionStats(id);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener estadísticas del sub-evento: " + e.getMessage());
        }
    }

    @Operation(summary = "Cambiar estado del sub-evento",
            description = "Cambia el estado de un sub-evento (solo para organizadores)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado cambiado correctamente"),
            @ApiResponse(responseCode = "400", description = "Estado inválido o sin permisos"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Sub-evento no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<SubEvent> changeSubEventStatus(@PathVariable String id,
                                                         @RequestParam String newStatus,
                                                         Principal principal) {
        try {
            SubEvent subEvent = subEventService.changeSubEventStatus(id, newStatus, principal.getName());
            return ResponseEntity.ok(subEvent);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al cambiar estado del sub-evento: " + e.getMessage());
        }
    }
}