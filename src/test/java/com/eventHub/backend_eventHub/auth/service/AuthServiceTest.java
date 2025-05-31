package com.eventHub.backend_eventHub.auth.service;



import com.eventHub.backend_eventHub.auth.dto.NewUserDto;
import com.eventHub.backend_eventHub.auth.exceptions.AuthenticationFailedException;

import com.eventHub.backend_eventHub.auth.jwt.JwtUtil;
import com.eventHub.backend_eventHub.domain.entities.Role;
import com.eventHub.backend_eventHub.domain.entities.State;
import com.eventHub.backend_eventHub.domain.entities.Users;
import com.eventHub.backend_eventHub.domain.enums.RoleList;
import com.eventHub.backend_eventHub.domain.enums.StateList;
import com.eventHub.backend_eventHub.domain.repositories.RoleRepository;
import com.eventHub.backend_eventHub.domain.repositories.StateRepository;
import com.eventHub.backend_eventHub.utils.emails.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAuthService userAuthService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private StateRepository stateRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private EmailService emailService;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private Users testUser;
    private Role testRole;
    private State testState;
    private NewUserDto newUserDto;

    @BeforeEach
    void setUp() {
        // Setup test data
        testRole = new Role();
        testRole.setId("role1");
        testRole.setNombreRol(RoleList.ROLE_USUARIO);

        testState = new State("state1", StateList.Active);

        testUser = new Users();
        testUser.setId("user1");
        testUser.setUserName("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setRole(testRole);
        testUser.setState(testState);

        newUserDto = new NewUserDto();
        newUserDto.setUserName("newuser");
        newUserDto.setEmail("new@example.com");
        newUserDto.setPassword("password123");
    }

    @Test
    @DisplayName("Authentication should succeed with valid credentials")
    void testAuthenticate_Success() {
        // Given
        String username = "testuser";
        String password = "password123";
        String expectedToken = "jwt-token";

        when(userAuthService.findByUserName(username)).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(authentication, testRole.getNombreRol().toString()))
                .thenReturn(expectedToken);

        // When
        String result = authService.authenticate(username, password);

        // Then
        assertEquals(expectedToken, result);
        verify(userAuthService).findByUserName(username);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateToken(authentication, testRole.getNombreRol().toString());
    }

    @Test
    @DisplayName("Authentication should fail with invalid credentials")
    void testAuthenticate_InvalidCredentials() {
        // Given
        String username = "testuser";
        String password = "wrongpassword";

        when(userAuthService.findByUserName(username)).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        assertThrows(AuthenticationFailedException.class,
                () -> authService.authenticate(username, password));

        verify(userAuthService).findByUserName(username);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(jwtUtil);
    }
}