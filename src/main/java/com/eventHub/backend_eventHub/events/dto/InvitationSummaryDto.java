package com.eventHub.backend_eventHub.events.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitationSummaryDto {
    private String id;
    private String eventoId;
    private String eventoTitulo;
    private String eventoDescripcion;
    private String eventoCreador;
    private String eventoCreadorEmail;
    private String rol;
    private Instant fechaInvitacion;
    private String estado; // "pendiente", "aceptada", "rechazada"
    private boolean aceptada;
    private boolean activa;

    // Información adicional del evento
    private String eventoFechaInicio;
    private String eventoFechaFin;
    private String eventoCategoria;
    private String eventoPrivacidad;
}