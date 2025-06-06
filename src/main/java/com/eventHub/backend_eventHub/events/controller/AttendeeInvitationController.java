// ================================
// 5. CONTROLADOR PARA INVITACIONES DE ASISTENTES
// ================================

package com.eventHub.backend_eventHub.events.controller;

import com.eventHub.backend_eventHub.events.dto.AttendeeInvitationDto;
import com.eventHub.backend_eventHub.events.dto.BulkAttendeeInvitationDto;
import com.eventHub.backend_eventHub.events.entities.AttendeeInvitation;
import com.eventHub.backend_eventHub.events.service.AttendeeInvitationService;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "Invitaciones de Asistentes", description = "Gestión de invitaciones para asistir a eventos privados")
@RestController
@RequestMapping("/api/attendee-invitations")
@CrossOrigin(origins = "*")
public class AttendeeInvitationController {

    @Autowired
    private AttendeeInvitationService attendeeInvitationService;

    @Operation(summary = "Invitar asistente",
            description = "Envía invitación para asistir a un evento privado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Invitación enviada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o condiciones no cumplidas"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para este evento"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("hasRole('USUARIO')")
    @PostMapping("/invite")
    public ResponseEntity<?> inviteAttendee(Principal principal,
                                            @Valid @RequestBody AttendeeInvitationDto dto) {
        try {
            AttendeeInvitation invitation = attendeeInvitationService.inviteAttendee(principal.getName(), dto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Invitación enviada correctamente");
            response.put("invitation", invitation);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = createErrorResponse(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = createErrorResponse("Error interno del servidor");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Operation(summary = "Invitar múltiples asistentes",
            description = "Envía invitaciones masivas para un evento privado")
    @PreAuthorize("hasRole('USUARIO')")
    @PostMapping("/invite-bulk")
    public ResponseEntity<?> inviteMultipleAttendees(Principal principal,
                                                     @Valid @RequestBody BulkAttendeeInvitationDto dto) {
        try {
            List<AttendeeInvitation> invitations = attendeeInvitationService.inviteMultipleAttendees(principal.getName(), dto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Invitaciones procesadas");
            response.put("invitations", invitations);
            response.put("count", invitations.size());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = createErrorResponse("Error procesando invitaciones masivas");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Operation(summary = "Aceptar invitación",
            description = "Acepta una invitación para asistir a un evento privado")
    @PreAuthorize("hasRole('USUARIO')")
    @PostMapping("/accept/{token}")
    public ResponseEntity<?> acceptInvitation(@PathVariable String token, Principal principal) {
        try {
            AttendeeInvitation invitation = attendeeInvitationService.acceptInvitation(token, principal.getName());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Invitación aceptada correctamente. Ahora puedes inscribirte al evento.");
            response.put("invitation", invitation);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = createErrorResponse(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = createErrorResponse("Error procesando invitación");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Operation(summary = "Rechazar invitación",
            description = "Rechaza una invitación para asistir a un evento privado")
    @PreAuthorize("hasRole('USUARIO')")
    @PostMapping("/reject/{token}")
    public ResponseEntity<?> rejectInvitation(@PathVariable String token, Principal principal) {
        try {
            AttendeeInvitation invitation = attendeeInvitationService.rejectInvitation(token, principal.getName());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Invitación rechazada");
            response.put("invitation", invitation);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = createErrorResponse(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = createErrorResponse("Error procesando invitación");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Operation(summary = "Mis invitaciones pendientes",
            description = "Lista invitaciones pendientes del usuario")
    @PreAuthorize("hasRole('USUARIO')")
    @GetMapping("/my-pending")
    public ResponseEntity<?> getMyPendingInvitations(Principal principal) {
        try {
            List<AttendeeInvitation> invitations = attendeeInvitationService.getPendingInvitationsForUser(principal.getName());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", invitations);
            response.put("count", invitations.size());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = createErrorResponse("Error obteniendo invitaciones");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Operation(summary = "Invitaciones de evento",
            description = "Lista invitaciones de un evento (solo para organizadores)")
    @PreAuthorize("hasRole('USUARIO')")
    @GetMapping("/event/{eventoId}")
    public ResponseEntity<?> getEventInvitations(@PathVariable String eventoId, Principal principal) {
        try {
            List<AttendeeInvitation> invitations = attendeeInvitationService.getEventInvitations(eventoId, principal.getName());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", invitations);
            response.put("count", invitations.size());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = createErrorResponse(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = createErrorResponse("Error obteniendo invitaciones del evento");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", LocalDateTime.now());
        return errorResponse;
    }
}