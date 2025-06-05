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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            @ApiResponse(responseCode = "400", description = "Datos inválidos o condiciones no cumplidas"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflicto - Ya inscrito o evento lleno"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @PostMapping("/register")
    public ResponseEntity<?> registerToEvent(Principal principal,
                                             @Valid @RequestBody InscriptionDto dto) {
        try {
            Inscription inscription = inscriptionService.registerToEvent(principal.getName(), dto);

            // ✅ RESPUESTA EXITOSA con información adicional
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Te has inscrito exitosamente al evento");
            response.put("inscription", inscription);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            // ✅ MANEJO DETALLADO DE ERRORES DE VALIDACIÓN
            return handleValidationError(e);
        } catch (Exception e) {
            // ✅ ERROR INTERNO CON LOGGING
            System.err.println("Error inesperado en inscripción: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = createErrorResponse(
                    "Error interno del servidor",
                    "Ha ocurrido un error inesperado. Por favor, intenta nuevamente.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Operation(summary = "Inscribirse a sub-evento", description = "Inscribe al usuario a un sub-evento específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Inscripción a sub-evento creada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o condiciones no cumplidas"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Sub-evento no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflicto - Ya inscrito o sub-evento lleno"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @PostMapping("/register-subevent")
    public ResponseEntity<?> registerToSubEvent(Principal principal,
                                                @Valid @RequestBody InscriptionDto dto) {
        try {
            if (dto.getSubeventoId() == null || dto.getSubeventoId().isEmpty()) {
                Map<String, Object> errorResponse = createErrorResponse(
                        "Datos inválidos",
                        "El ID del sub-evento es requerido",
                        HttpStatus.BAD_REQUEST
                );
                return ResponseEntity.badRequest().body(errorResponse);
            }

            dto.setTipoInscripcion("subevento");
            Inscription inscription = inscriptionService.registerToSubEvent(principal.getName(), dto);

            // ✅ RESPUESTA EXITOSA
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Te has inscrito exitosamente al sub-evento");
            response.put("inscription", inscription);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return handleValidationError(e);
        } catch (Exception e) {
            System.err.println("Error inesperado en inscripción a sub-evento: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = createErrorResponse(
                    "Error interno del servidor",
                    "Ha ocurrido un error inesperado. Por favor, intenta nuevamente.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Operation(summary = "Cancelar inscripción", description = "Cancela la inscripción del usuario a un evento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inscripción cancelada correctamente"),
            @ApiResponse(responseCode = "400", description = "No se puede cancelar la inscripción"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Inscripción no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @DeleteMapping("/cancel/{eventoId}")
    public ResponseEntity<?> cancelRegistration(@PathVariable String eventoId, Principal principal) {
        try {
            inscriptionService.cancelRegistration(principal.getName(), eventoId);

            // ✅ RESPUESTA EXITOSA
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tu inscripción ha sido cancelada exitosamente");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return handleValidationError(e);
        } catch (Exception e) {
            System.err.println("Error cancelando inscripción: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = createErrorResponse(
                    "Error interno del servidor",
                    "Ha ocurrido un error inesperado. Por favor, intenta nuevamente.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Operation(summary = "Cancelar inscripción a sub-evento",
            description = "Cancela la inscripción del usuario a un sub-evento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inscripción a sub-evento cancelada correctamente"),
            @ApiResponse(responseCode = "400", description = "No se puede cancelar la inscripción"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Inscripción no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @DeleteMapping("/cancel-subevent/{subeventoId}")
    public ResponseEntity<?> cancelSubEventRegistration(@PathVariable String subeventoId,
                                                        Principal principal) {
        try {
            inscriptionService.cancelSubEventRegistration(principal.getName(), subeventoId);

            // ✅ RESPUESTA EXITOSA
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tu inscripción al sub-evento ha sido cancelada exitosamente");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return handleValidationError(e);
        } catch (Exception e) {
            System.err.println("Error cancelando inscripción a sub-evento: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = createErrorResponse(
                    "Error interno del servidor",
                    "Ha ocurrido un error inesperado. Por favor, intenta nuevamente.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
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
    public ResponseEntity<?> getMyRegistrations(Principal principal) {
        try {
            List<Inscription> inscriptions = inscriptionService.getUserRegistrations(principal.getName());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", inscriptions);
            response.put("count", inscriptions.size());
            response.put("message", "Inscripciones obtenidas correctamente");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error obteniendo inscripciones: " + e.getMessage());

            Map<String, Object> errorResponse = createErrorResponse(
                    "Error interno del servidor",
                    "No se pudieron obtener las inscripciones",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
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
    public ResponseEntity<?> getMySubEventRegistrations(Principal principal) {
        try {
            List<Inscription> inscriptions = inscriptionService.getUserSubEventRegistrations(principal.getName());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", inscriptions);
            response.put("count", inscriptions.size());
            response.put("message", "Inscripciones a sub-eventos obtenidas correctamente");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error obteniendo inscripciones a sub-eventos: " + e.getMessage());

            Map<String, Object> errorResponse = createErrorResponse(
                    "Error interno del servidor",
                    "No se pudieron obtener las inscripciones a sub-eventos",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
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
    public ResponseEntity<?> checkRegistration(@PathVariable String eventoId, Principal principal) {
        try {
            boolean isRegistered = inscriptionService.isUserRegistered(principal.getName(), eventoId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("isRegistered", isRegistered);
            response.put("message", isRegistered ? "Estás inscrito en este evento" : "No estás inscrito en este evento");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error verificando inscripción: " + e.getMessage());

            Map<String, Object> errorResponse = createErrorResponse(
                    "Error interno del servidor",
                    "No se pudo verificar el estado de inscripción",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
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
    public ResponseEntity<?> getEventRegistrations(@PathVariable String eventoId,
                                                   Principal principal) {
        try {
            List<Inscription> inscriptions = inscriptionService.getEventRegistrations(eventoId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", inscriptions);
            response.put("count", inscriptions.size());
            response.put("message", "Inscripciones del evento obtenidas correctamente");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error obteniendo inscripciones del evento: " + e.getMessage());

            Map<String, Object> errorResponse = createErrorResponse(
                    "Error interno del servidor",
                    "No se pudieron obtener las inscripciones del evento",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ================ MÉTODOS AUXILIARES PARA MANEJO DE ERRORES ================

    /**
     * ✅ MANEJA ERRORES DE VALIDACIÓN CON CÓDIGOS HTTP APROPIADOS
     */
    private ResponseEntity<?> handleValidationError(IllegalArgumentException e) {
        String message = e.getMessage();
        HttpStatus status;
        String errorType;

        // ✅ DETERMINAR EL CÓDIGO HTTP APROPIADO SEGÚN EL MENSAJE
        if (message.contains("ya estás inscrito") ||
                message.contains("ya inscrito") ||
                message.contains("capacidad máxima") ||
                message.contains("evento lleno") ||
                message.contains("sub-evento lleno")) {
            status = HttpStatus.CONFLICT; // 409
            errorType = "Conflicto de inscripción";

        } else if (message.contains("no encontrado")) {
            status = HttpStatus.NOT_FOUND; // 404
            errorType = "Recurso no encontrado";

        } else if (message.contains("cerradas") ||
                message.contains("no permite inscripciones") ||
                message.contains("fecha límite") ||
                message.contains("ya comenzó") ||
                message.contains("ya ha comenzado") ||
                message.contains("bloqueado") ||
                message.contains("no activo") ||
                message.contains("propio evento") ||
                message.contains("no puedes cancelar")) {
            status = HttpStatus.BAD_REQUEST; // 400
            errorType = "Condición no válida";

        } else {
            status = HttpStatus.BAD_REQUEST; // 400 por defecto
            errorType = "Datos inválidos";
        }

        Map<String, Object> errorResponse = createErrorResponse(errorType, message, status);
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * ✅ CREA RESPUESTAS DE ERROR CONSISTENTES
     */
    private Map<String, Object> createErrorResponse(String error, String message, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("status", status.value());
        errorResponse.put("timestamp", LocalDateTime.now());
        return errorResponse;
    }
}

