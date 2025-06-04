package com.eventHub.backend_eventHub.auth.jwt;


// ========== TESTS PARA JWT UTIL ==========


import com.eventHub.backend_eventHub.domain.entities.Users;
import com.eventHub.backend_eventHub.domain.entities.Role;
import com.eventHub.backend_eventHub.domain.enums.RoleList;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    private Users testUser;
    private String testSecret = "mySecretKeyForTesting123456789012345678901234567890";
    private int testExpiration = 3600; // 1 hora

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "expiration", testExpiration);

        Role testRole = new Role("1", RoleList.ROLE_USUARIO, null);
        testUser = new Users();
        testUser.setUserName("testuser");
        testUser.setRole(testRole);
    }

    @Test
    void shouldGenerateTokenSuccessfully() {
        // Given
        UserDetails userDetails = new User("testuser", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USUARIO")));

        when(authentication.getPrincipal()).thenReturn(userDetails);

        // When
        String token = jwtUtil.generateToken(authentication, "ROLE_USUARIO");

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT tiene 3 partes separadas por puntos
    }

    @Test
    void shouldExtractUserNameFromToken() {
        // Given
        UserDetails userDetails = new User("testuser", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USUARIO")));

        when(authentication.getPrincipal()).thenReturn(userDetails);
        String token = jwtUtil.generateToken(authentication, "ROLE_USUARIO");

        // When
        String extractedUsername = jwtUtil.extractUserName(token);

        // Then
        assertThat(extractedUsername).isEqualTo("testuser");
    }

    @Test
    void shouldExtractUserRoleFromToken() {
        // Given
        UserDetails userDetails = new User("testuser", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USUARIO")));

        when(authentication.getPrincipal()).thenReturn(userDetails);
        String token = jwtUtil.generateToken(authentication, "ROLE_ADMIN");

        // When
        String extractedRole = jwtUtil.extractUserRole(token);

        // Then
        assertThat(extractedRole).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void shouldExtractExpirationFromToken() {
        // Given
        UserDetails userDetails = new User("testuser", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USUARIO")));

        when(authentication.getPrincipal()).thenReturn(userDetails);
        String token = jwtUtil.generateToken(authentication, "ROLE_USUARIO");

        // When
        Date expiration = jwtUtil.extractExpiration(token);

        // Then
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void shouldValidateTokenSuccessfully() {
        // Given
        UserDetails userDetails = new User("testuser", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USUARIO")));

        when(authentication.getPrincipal()).thenReturn(userDetails);
        String token = jwtUtil.generateToken(authentication, "ROLE_USUARIO");

        when(this.userDetails.getUsername()).thenReturn("testuser");

        // When
        Boolean isValid = jwtUtil.validateToken(token, this.userDetails);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldReturnFalseForInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";
        when(userDetails.getUsername()).thenReturn("testuser");

        // When
        Boolean isValid = jwtUtil.validateToken(invalidToken, userDetails);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldReturnFalseForTokenWithWrongUsername() {
        // Given
        UserDetails tokenUserDetails = new User("testuser", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USUARIO")));

        when(authentication.getPrincipal()).thenReturn(tokenUserDetails);
        String token = jwtUtil.generateToken(authentication, "ROLE_USUARIO");

        when(userDetails.getUsername()).thenReturn("differentuser");

        // When
        Boolean isValid = jwtUtil.validateToken(token, userDetails);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldGenerateRecoveryToken() {
        // When
        String recoveryToken = jwtUtil.generateRecoveryToken(testUser);

        // Then
        assertThat(recoveryToken).isNotNull();
        assertThat(recoveryToken).isNotEmpty();

        // Verificar que es un token de recuperación
        assertThat(jwtUtil.isRecoveryToken(recoveryToken)).isTrue();

        // Verificar que el username es correcto
        assertThat(jwtUtil.extractUserName(recoveryToken)).isEqualTo("testuser");
    }

    @Test
    void shouldIdentifyRecoveryToken() {
        // Given
        String recoveryToken = jwtUtil.generateRecoveryToken(testUser);

        UserDetails userDetails = new User("testuser", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USUARIO")));
        when(authentication.getPrincipal()).thenReturn(userDetails);
        String normalToken = jwtUtil.generateToken(authentication, "ROLE_USUARIO");

        // When & Then
        assertThat(jwtUtil.isRecoveryToken(recoveryToken)).isTrue();
        assertThat(jwtUtil.isRecoveryToken(normalToken)).isFalse();
    }

    @Test
    void shouldDetectExpiredToken() {
        // Given - crear un JwtUtil con expiración muy corta
        JwtUtil shortExpirationJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortExpirationJwtUtil, "secret", testSecret);
        ReflectionTestUtils.setField(shortExpirationJwtUtil, "expiration", -1); // Token ya expirado

        UserDetails userDetails = new User("testuser", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USUARIO")));
        when(authentication.getPrincipal()).thenReturn(userDetails);

        String expiredToken = shortExpirationJwtUtil.generateToken(authentication, "ROLE_USUARIO");

        // When
        Boolean isExpired = shortExpirationJwtUtil.isTokenExpired(expiredToken);

        // Then
        assertThat(isExpired).isTrue();
    }

    @Test
    void shouldExtractAllClaims() {
        // Given
        UserDetails userDetails = new User("testuser", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USUARIO")));

        when(authentication.getPrincipal()).thenReturn(userDetails);
        String token = jwtUtil.generateToken(authentication, "ROLE_ADMIN");

        // When
        Claims claims = jwtUtil.extractAllClaims(token);

        // Then
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("testuser");
        assertThat(claims.get("role")).isEqualTo("ROLE_ADMIN");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }
}