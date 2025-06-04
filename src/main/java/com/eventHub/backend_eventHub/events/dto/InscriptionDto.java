// Nuevo: InscriptionDto
package com.eventHub.backend_eventHub.events.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InscriptionDto {
    @NotBlank
    private String eventoId;

    private String subeventoId; // Opcional, para inscripciones a subeventos

    private String tipoInscripcion = "evento_principal"; // "evento_principal" o "subevento"
}