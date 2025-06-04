// ================================
// InscriptionController SEPARADO - Para gestión de inscripciones
// ================================

package com.eventHub.backend_eventHub.events.controller;

import com.eventHub.backend_eventHub.events.dto.InscriptionDto;
import com.eventHub.backend_eventHub.events.entities.Inscription;
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

@Tag(name = "Inscripciones", description = "Gestión de inscripciones a eventos")
@RestController
@RequestMapping("/api/inscriptions")
@CrossOrigin(origins = "*")
public class InscriptionController {

    @Autowired
    private InscriptionService inscriptionService;

    @Operation(summary = "Inscribirse a evento", description = "Inscribe al usuario autenticado a un evento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Inscripción creada correctamente"),
            @ApiResponse(responseCode = "400", description = "Error en los datos o evento lleno"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @PostMapping("/register")
    public ResponseEntity<Inscription> registerToEvent(Principal principal,
                                                       @Valid @RequestBody InscriptionDto dto) {
        try {
            // Usar método mejorado del servicio
            Inscription inscription = inscriptionService.registerToEvent(principal.getName(), dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(inscription);
        } catch (IllegalArgumentException e) {
            // Mensajes de error más específicos
            if (e.getMessage().contains("no encontrado")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
            } else if (e.getMessage().contains("lleno") ||
                    e.getMessage().contains("cerradas") ||
                    e.getMessage().contains("ya estás inscrito")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
        } catch (Exception e) {
            // Log del error para debugging
            System.err.println("Error inesperado en inscripción: " + e.getMessage());
            e.printStackTrace();

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al inscribirse al evento: " + e.getMessage());
        }
    }

    @Operation(summary = "Inscribirse a sub-evento", description = "Inscribe al usuario a un sub-evento específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Inscripción a sub-evento creada correctamente"),
            @ApiResponse(responseCode = "400", description = "Error en los datos o sub-evento lleno"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @PostMapping("/register-subevent")
    public ResponseEntity<Inscription> registerToSubEvent(Principal principal,
                                                          @Valid @RequestBody InscriptionDto dto) {
        try {
            if (dto.getSubeventoId() == null || dto.getSubeventoId().isEmpty()) {
                throw new IllegalArgumentException("ID de sub-evento es requerido");
            }
            dto.setTipoInscripcion("subevento");
            Inscription inscription = inscriptionService.registerToSubEvent(principal.getName(), dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(inscription);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al inscribirse al sub-evento: " + e.getMessage());
        }
    }

    @Operation(summary = "Cancelar inscripción", description = "Cancela la inscripción del usuario a un evento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Inscripción cancelada correctamente"),
            @ApiResponse(responseCode = "400", description = "No tiene inscripción activa"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @DeleteMapping("/cancel/{eventoId}")
    public ResponseEntity<Void> cancelRegistration(@PathVariable String eventoId, Principal principal) {
        try {
            inscriptionService.cancelRegistration(principal.getName(), eventoId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al cancelar inscripción: " + e.getMessage());
        }
    }

    @Operation(summary = "Cancelar inscripción a sub-evento",
            description = "Cancela la inscripción del usuario a un sub-evento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Inscripción a sub-evento cancelada correctamente"),
            @ApiResponse(responseCode = "400", description = "No tiene inscripción activa en el sub-evento"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @DeleteMapping("/cancel-subevent/{subeventoId}")
    public ResponseEntity<Void> cancelSubEventRegistration(@PathVariable String subeventoId,
                                                           Principal principal) {
        try {
            inscriptionService.cancelSubEventRegistration(principal.getName(), subeventoId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al cancelar inscripción al sub-evento: " + e.getMessage());
        }
    }

    @Operation(summary = "Mis inscripciones", description = "Lista eventos donde el usuario está inscrito")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de inscripciones obtenida correctamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @GetMapping("/my-registrations")
    public ResponseEntity<List<Inscription>> getMyRegistrations(Principal principal) {
        try {
            List<Inscription> inscriptions = inscriptionService.getUserRegistrations(principal.getName());
            return ResponseEntity.ok(inscriptions);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener inscripciones: " + e.getMessage());
        }
    }

    @Operation(summary = "Mis inscripciones a sub-eventos",
            description = "Lista sub-eventos donde el usuario está inscrito")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de inscripciones a sub-eventos"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @GetMapping("/my-subevent-registrations")
    public ResponseEntity<List<Inscription>> getMySubEventRegistrations(Principal principal) {
        try {
            List<Inscription> inscriptions = inscriptionService.getUserSubEventRegistrations(principal.getName());
            return ResponseEntity.ok(inscriptions);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener inscripciones a sub-eventos: " + e.getMessage());
        }
    }

    @Operation(summary = "Verificar inscripción", description = "Verifica si el usuario está inscrito en un evento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado de inscripción verificado"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @GetMapping("/check/{eventoId}")
    public ResponseEntity<Boolean> checkRegistration(@PathVariable String eventoId, Principal principal) {
        try {
            boolean isRegistered = inscriptionService.isUserRegistered(principal.getName(), eventoId);
            return ResponseEntity.ok(isRegistered);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al verificar inscripción: " + e.getMessage());
        }
    }

    @Operation(summary = "Inscripciones de evento",
            description = "Lista inscripciones de un evento (solo para organizadores)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de inscripciones del evento"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "No es organizador del evento"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @GetMapping("/event/{eventoId}")
    public ResponseEntity<List<Inscription>> getEventRegistrations(@PathVariable String eventoId,
                                                                   Principal principal) {
        try {
            // Aquí podrías agregar validación de que el usuario es organizador
            List<Inscription> inscriptions = inscriptionService.getEventRegistrations(eventoId);
            return ResponseEntity.ok(inscriptions);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener inscripciones del evento: " + e.getMessage());
        }
    }
}