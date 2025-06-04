package com.eventHub.backend_eventHub.auth.jwt;

import com.eventHub.backend_eventHub.auth.service.UserAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filtro de autenticación JWT mejorado que se ejecuta una vez por cada solicitud HTTP.
 *
 * Extrae el token del encabezado Authorization, lo valida y, si es correcto,
 * establece la autenticación en el contexto de Spring Security.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserAuthService userAuthService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;

    // Rutas que NO deben ser procesadas por el filtro JWT
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/auth/register",
            "/auth/login",
            "/password/",
            "/actuator/",
            "/swagger-ui/",
            "/v3/api-docs/"
    );

    // Rutas públicas de eventos que NO requieren autenticación
    private static final List<String> PUBLIC_EVENT_PATHS = Arrays.asList(
            "/api/events/search",
            "/api/events/featured",
            "/api/events/upcoming",
            "/api/events/recent",
            "/api/categories"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getServletPath();
        String method = request.getMethod();

        log.debug("🔍 Processing JWT filter for: {} {}", method, requestPath);

        try {
            String jwt = extractTokenFromRequest(request);

            // Solo procesar JWT si hay token Y no hay autenticación ya establecida
            if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                processJwtToken(jwt, request);
            } else if (jwt == null && isPublicPath(requestPath, method)) {
                log.debug(" Public path accessed without token: {} {}", method, requestPath);
                // Para rutas públicas sin token, simplemente continuar sin autenticación
            } else if (jwt == null) {
                log.debug(" No JWT token found for: {} {}", method, requestPath);
                // No hay token pero puede ser una ruta que requiere autenticación - Spring Security se encargará
            }
        } catch (Exception e) {
            log.error("Error procesando autenticación JWT para {} {}: {}", method, requestPath, e.getMessage());
            // No interrumpir la cadena de filtros, dejar que Spring Security maneje la falta de autenticación
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Determina si una ruta es pública y no requiere autenticación.
     */
    private boolean isPublicPath(String path, String method) {
        // Rutas públicas específicas
        if (PUBLIC_EVENT_PATHS.stream().anyMatch(path::startsWith)) {
            return true;
        }

        // Endpoints específicos de eventos públicos
        if ("GET".equals(method)) {
            // GET /api/events/{id} - detalle público de evento
            if (path.matches("/api/events/[^/]+$")) {
                return true;
            }
            // GET /api/categories/** - todas las categorías públicas
            if (path.startsWith("/api/categories")) {
                return true;
            }
            // GET /api/subevents/by-event/** - sub-eventos públicos
            if (path.startsWith("/api/subevents/by-event/")) {
                return true;
            }
            // GET /api/subevents/{id} - detalle público de sub-evento
            if (path.matches("/api/subevents/[^/]+$")) {
                return true;
            }
        }

        // POST /api/events/search - búsqueda pública
        if ("POST".equals(method) && "/api/events/search".equals(path)) {
            return true;
        }

        return false;
    }

    /**
     * Extrae el token JWT del header Authorization.
     *
     * @param request Solicitud HTTP.
     * @return Token JWT extraído o null si no existe o es inválido.
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String token = authorizationHeader.substring(BEARER_PREFIX_LENGTH);

            // Validación básica del token
            if (StringUtils.hasText(token)) {
                return token;
            }
        }

        return null;
    }

    /**
     * Procesa el token JWT y establece la autenticación si es válido.
     *
     * @param jwt     Token JWT a procesar.
     * @param request Solicitud HTTP para establecer detalles de autenticación.
     */
    private void processJwtToken(String jwt, HttpServletRequest request) {
        try {
            String userName = jwtUtil.extractUserName(jwt);
            log.debug("🔍 Processing JWT for user: {}", userName);

            if (StringUtils.hasText(userName)) {
                UserDetails userDetails = loadUserDetails(userName);

                if (userDetails != null && jwtUtil.validateToken(jwt, userDetails)) {
                    setAuthenticationContext(userDetails, request);
                    log.debug(" Autenticación JWT exitosa para usuario: {}", userName);
                } else {
                    log.warn(" Token JWT inválido para usuario: {}", userName);
                }
            }
        } catch (UsernameNotFoundException e) {
            log.warn(" Usuario no encontrado en token JWT: {}", e.getMessage());
        } catch (Exception e) {
            log.error(" Error procesando token JWT: {}", e.getMessage());
        }
    }

    /**
     * Carga los detalles del usuario de forma segura.
     *
     * @param userName Nombre de usuario a cargar.
     * @return UserDetails del usuario o null si no se encuentra.
     */
    private UserDetails loadUserDetails(String userName) {
        try {
            return userAuthService.loadUserByUsername(userName);
        } catch (UsernameNotFoundException e) {
            log.warn("Usuario no encontrado: {}", userName);
            return null;
        } catch (Exception e) {
            log.error("Error cargando detalles del usuario: {}", userName, e);
            return null;
        }
    }

    /**
     * Establece el contexto de autenticación en Spring Security.
     *
     * @param userDetails Detalles del usuario autenticado.
     * @param request     Solicitud HTTP para establecer detalles adicionales.
     */
    private void setAuthenticationContext(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    /**
     * Determina si este filtro debe ejecutarse para la solicitud actual.
     * Se puede sobrescribir para excluir ciertas rutas del procesamiento JWT.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // Excluir rutas administrativas y de autenticación del procesamiento JWT
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }
}