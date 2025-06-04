// ErrorResponse.java
package com.eventHub.backend_eventHub.users.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Respuesta estándar para errores de la API.
 */
@Data
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}