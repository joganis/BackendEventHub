package com.eventHub.backend_eventHub.auth.jwt;

import com.eventHub.backend_eventHub.domain.entities.Users;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utilidad mejorada para la generación y validación de tokens JWT.
 * Incluye mejor manejo de errores, logging y validaciones adicionales.
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private int expiration;

    // Constantes para mejor mantenimiento
    private static final String ROLE_CLAIM = "role";
    private static final String TYPE_CLAIM = "type";
    private static final String RECOVERY_TOKEN_TYPE = "recovery";
    private static final long RECOVERY_TOKEN_EXPIRATION = 15 * 60 * 1000; // 15 minutos

    /**
     * Genera un token JWT a partir de la autenticación y rol del usuario.
     *
     * @param authentication Objeto de autenticación.
     * @param role           Rol del usuario.
     * @return Token JWT generado.
     */
    public String generateToken(Authentication authentication, String role) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Authentication no puede ser null");
        }

        UserDetails mainUser = (UserDetails) authentication.getPrincipal();
        if (mainUser.getUsername() == null || mainUser.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username no puede estar vacío");
        }

        try {
            SecretKey key = getSigningKey();
            Map<String, Object> claims = new HashMap<>();
            claims.put(ROLE_CLAIM, role);

            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(mainUser.getUsername())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + (expiration * 1000L)))
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            log.error("Error generando token JWT para usuario: {}", mainUser.getUsername(), e);
            throw new RuntimeException("Error generando token JWT", e);
        }
    }

    /**
     * Genera un token de recuperación de contraseña con expiración corta (15 minutos).
     *
     * @param user Usuario para el que se genera el token.
     * @return Token JWT de recuperación.
     */
    public String generateRecoveryToken(Users user) {
        if (user == null || user.getUserName() == null || user.getUserName().trim().isEmpty()) {
            throw new IllegalArgumentException("Usuario o username no pueden ser null/vacío");
        }

        try {
            SecretKey key = getSigningKey();
            return Jwts.builder()
                    .setSubject(user.getUserName())
                    .claim(TYPE_CLAIM, RECOVERY_TOKEN_TYPE)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + RECOVERY_TOKEN_EXPIRATION))
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            log.error("Error generando token de recuperación para usuario: {}", user.getUserName(), e);
            throw new RuntimeException("Error generando token de recuperación", e);
        }
    }

    /**
     * Verifica si el token es de recuperación comprobando la claim "type".
     *
     * @param token Token JWT.
     * @return true si es un token de recuperación, false en caso contrario.
     */
    public boolean isRecoveryToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return RECOVERY_TOKEN_TYPE.equals(claims.get(TYPE_CLAIM));
        } catch (Exception e) {
            log.warn("Error verificando tipo de token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Valida el token comparando el nombre de usuario y verificando la expiración.
     *
     * @param token       Token JWT a validar.
     * @param userDetails Detalles del usuario.
     * @return true si el token es válido, false en caso contrario.
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        if (token == null || token.trim().isEmpty() || userDetails == null) {
            return false;
        }

        try {
            final String userName = extractUserName(token);
            return userName != null
                    && userName.equals(userDetails.getUsername())
                    && !isTokenExpired(token);
        } catch (JwtException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error validando token JWT", e);
            return false;
        }
    }

    /**
     * Verifica si el token ha expirado.
     *
     * @param token Token JWT.
     * @return true si el token está expirado, false en caso contrario.
     */
    public Boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration != null && expiration.before(new Date());
        } catch (Exception e) {
            log.warn("Error verificando expiración del token: {}", e.getMessage());
            return true; // Considerar expirado si hay error
        }
    }

    /**
     * Extrae la fecha de expiración del token.
     *
     * @param token Token JWT.
     * @return Fecha de expiración.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrae el nombre de usuario (subject) del token.
     *
     * @param token Token JWT.
     * @return Nombre de usuario.
     */
    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae el rol del usuario del token.
     *
     * @param token Token JWT.
     * @return Rol del usuario.
     */
    public String extractUserRole(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return (String) claims.get(ROLE_CLAIM);
        } catch (Exception e) {
            log.warn("Error extrayendo rol del token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extrae una claim específica del token usando un resolver de función.
     *
     * @param token          Token JWT.
     * @param claimsResolver Función para extraer la claim específica.
     * @param <T>            Tipo de la claim.
     * @return Valor de la claim extraída.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrae todas las claims del token.
     *
     * @param token Token JWT.
     * @return Objeto Claims con la información del token.
     */
    public Claims extractAllClaims(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token no puede ser null o vacío");
        }

        try {
            SecretKey key = getSigningKey();
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            log.warn("Token JWT malformado o inválido: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error procesando token JWT", e);
            throw new RuntimeException("Error procesando token JWT", e);
        }
    }

    /**
     * Obtiene la clave de firma para JWT.
     *
     * @return SecretKey para firmar tokens.
     */
    private SecretKey getSigningKey() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret no puede estar vacío");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Verifica si un token es válido sin necesidad de UserDetails.
     * Útil para validaciones rápidas.
     *
     * @param token Token JWT a verificar.
     * @return true si el token es válido estructuralmente y no ha expirado.
     */
    public boolean isValidToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}