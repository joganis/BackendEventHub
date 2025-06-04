package com.eventHub.backend_eventHub.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador mejorado de errores de autenticación.
 *
 * Se activa cuando un usuario no está autorizado para acceder a un recurso y envía un error 401
 * con información detallada del error en formato JSON.
 */
@Component
@Slf4j
public class JwtEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        String requestPath = request.getServletPath();
        String method = request.getMethod();

        log.warn("🚫 Acceso no autorizado a: {} {} - {}", method, requestPath, authException.getMessage());

        // Configurar respuesta HTTP
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // Crear respuesta JSON detallada
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", determineErrorMessage(requestPath, authException));
        errorResponse.put("path", requestPath);

        // Escribir respuesta JSON
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
    }

    /**
     * Determina el mensaje de error apropiado basado en la ruta y el tipo de excepción.
     */
    private String determineErrorMessage(String path, AuthenticationException authException) {
        // Para rutas administrativas
        if (path.startsWith("/api/admin/")) {
            return "Acceso denegado: Se requieren privilegios de administrador";
        }

        // Para rutas de categorías administrativas
        if (path.startsWith("/api/categories/admin/") ||
                (path.startsWith("/api/categories/") && !path.equals("/api/categories") && !path.contains("/search"))) {
            return "Acceso denegado: Se requieren privilegios de administrador para gestionar categorías";
        }

        // Para rutas de eventos que requieren autenticación
        if (path.startsWith("/api/events/") &&
                (path.contains("/my-") || path.contains("/authenticated") || path.contains("/invite-") || path.contains("/accept-"))) {
            return "Acceso denegado: Debe iniciar sesión para acceder a sus eventos";
        }

        // Para rutas de inscripciones
        if (path.startsWith("/api/inscriptions/")) {
            return "Acceso denegado: Debe iniciar sesión para gestionar inscripciones";
        }

        // Para rutas de invitaciones
        if (path.startsWith("/api/invitations/")) {
            return "Acceso denegado: Debe iniciar sesión para gestionar invitaciones";
        }

        // Para rutas de sub-eventos autenticadas
        if (path.startsWith("/api/subevents/") &&
                (path.contains("/my-") || path.contains("/registrations") || path.contains("/stats"))) {
            return "Acceso denegado: Debe iniciar sesión para gestionar sub-eventos";
        }

        // Mensaje genérico
        return "Acceso denegado: Token de autenticación requerido o inválido";
    }
}