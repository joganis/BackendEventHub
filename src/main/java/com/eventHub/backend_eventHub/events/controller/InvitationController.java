// ================================
// 1. NUEVO CONTROLADOR - InvitationController
// ================================

package com.eventHub.backend_eventHub.events.controller;

import com.eventHub.backend_eventHub.events.dto.InvitationSummaryDto;
import com.eventHub.backend_eventHub.events.entities.EventRole;
import com.eventHub.backend_eventHub.events.service.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.security.Principal;
import java.util.List;

@Tag(name = "Invitaciones", description = "Gestión de invitaciones para subcreadores")
@RestController
@RequestMapping("/api/invitations")
@CrossOrigin(origins = "*")
public class InvitationController {

    @Autowired
    private InvitationService invitationService;

    @Operation(summary = "Invitaciones pendientes",
            description = "Obtiene todas las invitaciones pendientes del usuario autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de invitaciones pendientes"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/pending")
    @PreAuthorize("hasRole('USUARIO')")
    public ResponseEntity<List<InvitationSummaryDto>> getPendingInvitations(Principal principal) {
        try {
            List<InvitationSummaryDto> invitations = invitationService.getPendingInvitations(principal.getName());
            return ResponseEntity.ok(invitations);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener invitaciones pendientes: " + e.getMessage());
        }
    }

    @Operation(summary = "Todas las invitaciones",
            description = "Obtiene todas las invitaciones (pendientes, aceptadas y rechazadas)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista completa de invitaciones"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/all")
    @PreAuthorize("hasRole('USUARIO')")
    public ResponseEntity<List<InvitationSummaryDto>> getAllInvitations(Principal principal) {
        try {
            List<InvitationSummaryDto> invitations = invitationService.getAllInvitations(principal.getName());
            return ResponseEntity.ok(invitations);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener invitaciones: " + e.getMessage());
        }
    }

    @Operation(summary = "Rechazar invitación",
            description = "Rechaza una invitación específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitación rechazada correctamente"),
            @ApiResponse(responseCode = "400", description = "Error al rechazar invitación"),
            @ApiResponse(responseCode = "401", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Invitación no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @DeleteMapping("/reject/{invitationId}")
    @PreAuthorize("hasRole('USUARIO')")
    public ResponseEntity<String> rejectInvitation(@PathVariable String invitationId,
                                                   Principal principal) {
        try {
            invitationService.rejectInvitation(invitationId, principal.getName());
            return ResponseEntity.ok("Invitación rechazada correctamente");
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("no encontrada")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al rechazar invitación: " + e.getMessage());
        }
    }

    @Operation(summary = "Contar invitaciones pendientes",
            description = "Obtiene el número de invitaciones pendientes (para notificaciones)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contador de invitaciones"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @GetMapping("/count")
    @PreAuthorize("hasRole('USUARIO')")
    public ResponseEntity<Integer> getPendingInvitationsCount(Principal principal) {
        try {
            int count = invitationService.getPendingInvitationsCount(principal.getName());
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.ok(0); // En caso de error, retornar 0
        }
    }
}