package com.eventHub.backend_eventHub.auth.jwt;



import com.eventHub.backend_eventHub.domain.entities.Users;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilidad para la generación y validación de tokens JWT.
 *
 * Permite generar tokens basados en la autenticación y extraer información (subject, rol, expiración) del token.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private int expiration;

    /**
     * Genera un token JWT a partir de la autenticación y rol del usuario.
     *
     * @param authentication Objeto de autenticación.
     * @param role           Rol del usuario.
     * @return Token JWT generado.
     */
    public String generateToken(Authentication authentication, String role) {
        UserDetails mainUser = (UserDetails) authentication.getPrincipal();
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(mainUser.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + expiration * 1000L))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }


    /**
     * Genera un token de recuperación de contraseña con expiración corta (15 minutos).
     *
     * @param user Usuario para el que se genera el token.
     * @return Token JWT de recuperación.
     */
    public String generateRecoveryToken(Users user) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        long recoveryExpiration = 15 * 60 * 1000; // 15 minutos
        return Jwts.builder()
                .setSubject(user.getUserName())
                .claim("type", "recovery")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + recoveryExpiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Verifica si el token es de recuperación comprobando la claim "type".
     *
     * @param token Token JWT.
     * @return true si es un token de recuperación, false en caso contrario.
     */
    public boolean isRecoveryToken(String token) {
        Claims claims = extractAllClaims(token);
        return "recovery".equals(claims.get("type"));
    }

    /**
     * Valida el token comparando el nombre de usuario y verificando la expiración.
     *
     * @param token       Token JWT a validar.
     * @param userDetails Detalles del usuario.
     * @return true si el token es válido, false en caso contrario.
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String userName = extractUserName(token);
            return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
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
        return extractAllClaims(token).getExpiration().before(new Date()); /*aqui cambio */
    }



    /**
     * Extrae la fecha de expiración del token.
     *
     * @param token Token JWT.
     * @return Fecha de expiración.
     */
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    /**
     * Extrae todas las claims del token.
     *
     * @param token Token JWT.
     * @return Objeto Claims con la información del token.
     */
    public Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extrae el nombre de usuario (subject) del token.
     *
     * @param token Token JWT.
     * @return Nombre de usuario.
     */
    public String extractUserName(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extrae el rol del usuario del token.
     *
     * @param token Token JWT.
     * @return Rol del usuario.
     */
    public String extractUserRole(String token) {
        return (String) extractAllClaims(token).get("role");
    }




}
