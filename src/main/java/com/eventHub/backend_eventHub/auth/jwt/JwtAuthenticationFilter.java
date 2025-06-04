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

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = extractTokenFromRequest(request);

            if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                processJwtToken(jwt, request);
            }
        } catch (Exception e) {
            log.error("Error procesando autenticación JWT: {}", e.getMessage());
            // No interrumpir la cadena de filtros, dejar que Spring Security maneje la falta de autenticación
        }

        filterChain.doFilter(request, response);
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
                    log.info("✅ Autenticación JWT exitosa para usuario: {}", userName);
                } else {
                    log.warn("❌ Token JWT inválido para usuario: {}", userName);
                }
            }
        } catch (UsernameNotFoundException e) {
            log.warn("❌ Usuario no encontrado en token JWT: {}", e.getMessage());
        } catch (Exception e) {
            log.error("❌ Error procesando token JWT: {}", e.getMessage());
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

        // Excluir rutas públicas del procesamiento JWT para mejorar rendimiento
        return path.equals("/auth/register") ||
                path.equals("/auth/login") ||           // ← ESPECÍFICO, NO /auth/check-auth
                path.startsWith("/password/") ||
                path.startsWith("/actuator/") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/");
    }
}