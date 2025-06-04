package com.eventHub.backend_eventHub.auth.controller;

import com.eventHub.backend_eventHub.auth.dto.LoginUserDto;
import com.eventHub.backend_eventHub.auth.dto.NewUserDto;
import com.eventHub.backend_eventHub.auth.exceptions.AuthenticationFailedException;
import com.eventHub.backend_eventHub.auth.exceptions.UserAlreadyExistsException;
import com.eventHub.backend_eventHub.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias para AuthController.
 *
 * Se prueban todos los endpoints del controlador de autenticación:
 * - Login de usuarios
 * - Registro de usuarios
 * - Verificación de estado de autenticación
 */
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    // Datos de prueba
    private static final String VALID_USERNAME = "testuser";
    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_PASSWORD = "password123";
    private static final String JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciJ9.test";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Debe hacer login exitoso con credenciales válidas")
        void shouldLoginSuccessfully_WhenValidCredentials() throws Exception {
            // Given
            LoginUserDto loginUserDto = new LoginUserDto();
            loginUserDto.setUserName(VALID_USERNAME);
            loginUserDto.setPassword(VALID_PASSWORD);

            when(authService.authenticate(loginUserDto.getUserName(), loginUserDto.getPassword()))
                    .thenReturn(JWT_TOKEN);

            // When & Then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginUserDto)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.token").value(JWT_TOKEN))
                    .andExpect(jsonPath("$.message").value("Login exitoso"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(authService).authenticate(loginUserDto.getUserName(), loginUserDto.getPassword());
        }

        @Test
        @DisplayName("Debe fallar login con credenciales inválidas")
        void shouldFailLogin_WhenInvalidCredentials() throws Exception {
            // Given
            LoginUserDto loginUserDto = new LoginUserDto();
            loginUserDto.setUserName(VALID_USERNAME);
            loginUserDto.setPassword("wrongpassword");

            when(authService.authenticate(loginUserDto.getUserName(), loginUserDto.getPassword()))
                    .thenThrow(new AuthenticationFailedException("Credenciales inválidas"));

            // When & Then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginUserDto)))
                    .andExpect(status().isUnauthorized());

            verify(authService).authenticate(loginUserDto.getUserName(), loginUserDto.getPassword());
        }

        @Test
        @DisplayName("Debe fallar validación cuando username está vacío")
        void shouldFailValidation_WhenUsernameIsEmpty() throws Exception {
            // Given
            LoginUserDto loginUserDto = new LoginUserDto();
            loginUserDto.setUserName("");
            loginUserDto.setPassword(VALID_PASSWORD);

            // When & Then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginUserDto)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).authenticate(anyString(), anyString());
        }

        @Test
        @DisplayName("Debe fallar validación cuando password está vacío")
        void shouldFailValidation_WhenPasswordIsEmpty() throws Exception {
            // Given
            LoginUserDto loginUserDto = new LoginUserDto();
            loginUserDto.setUserName(VALID_USERNAME);
            loginUserDto.setPassword("");

            // When & Then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginUserDto)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).authenticate(anyString(), anyString());
        }

        @Test
        @DisplayName("Debe fallar cuando el cuerpo de la petición está vacío")
        void shouldFailValidation_WhenRequestBodyIsEmpty() throws Exception {
            // When & Then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).authenticate(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Debe registrar usuario exitosamente con datos válidos")
        void shouldRegisterSuccessfully_WhenValidData() throws Exception {
            // Given
            NewUserDto newUserDto = new NewUserDto();
            newUserDto.setUserName(VALID_USERNAME);
            newUserDto.setEmail(VALID_EMAIL);
            newUserDto.setPassword(VALID_PASSWORD);

            when(authService.registerUser(any(NewUserDto.class))).thenReturn(JWT_TOKEN);

            // When & Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUserDto)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.token").value(JWT_TOKEN))
                    .andExpect(jsonPath("$.message").value("Usuario registrado exitosamente"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(authService).registerUser(any(NewUserDto.class));
        }

        @Test
        @DisplayName("Debe fallar registro cuando el usuario ya existe")
        void shouldFailRegister_WhenUserAlreadyExists() throws Exception {
            // Given
            NewUserDto newUserDto = new NewUserDto();
            newUserDto.setUserName(VALID_USERNAME);
            newUserDto.setEmail(VALID_EMAIL);
            newUserDto.setPassword(VALID_PASSWORD);

            when(authService.registerUser(any(NewUserDto.class)))
                    .thenThrow(new UserAlreadyExistsException("El nombre de usuario ya existe"));

            // When & Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUserDto)))
                    .andExpect(status().isConflict());

            verify(authService).registerUser(any(NewUserDto.class));
        }

        @Test
        @DisplayName("Debe fallar validación cuando username está vacío")
        void shouldFailValidation_WhenUsernameIsEmpty() throws Exception {
            // Given
            NewUserDto newUserDto = new NewUserDto();
            newUserDto.setUserName("");
            newUserDto.setEmail(VALID_EMAIL);
            newUserDto.setPassword(VALID_PASSWORD);

            // When & Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUserDto)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).registerUser(any(NewUserDto.class));
        }

        @Test
        @DisplayName("Debe fallar validación cuando email es inválido")
        void shouldFailValidation_WhenEmailIsInvalid() throws Exception {
            // Given
            NewUserDto newUserDto = new NewUserDto();
            newUserDto.setUserName(VALID_USERNAME);
            newUserDto.setEmail("invalid-email");
            newUserDto.setPassword(VALID_PASSWORD);

            // When & Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUserDto)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).registerUser(any(NewUserDto.class));
        }

        @Test
        @DisplayName("Debe fallar validación cuando password está vacío")
        void shouldFailValidation_WhenPasswordIsEmpty() throws Exception {
            // Given
            NewUserDto newUserDto = new NewUserDto();
            newUserDto.setUserName(VALID_USERNAME);
            newUserDto.setEmail(VALID_EMAIL);
            newUserDto.setPassword("");

            // When & Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUserDto)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).registerUser(any(NewUserDto.class));
        }

        @Test
        @DisplayName("Debe fallar validación con múltiples campos inválidos")
        void shouldFailValidation_WhenMultipleFieldsAreInvalid() throws Exception {
            // Given
            NewUserDto newUserDto = new NewUserDto();
            newUserDto.setUserName("");
            newUserDto.setEmail("invalid-email");
            newUserDto.setPassword("");

            // When & Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUserDto)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).registerUser(any(NewUserDto.class));
        }
    }

    @Nested
    @DisplayName("Check Auth Tests")
    class CheckAuthTests {

        @Test
        @DisplayName("Debe retornar usuario autenticado cuando hay autenticación válida")
        void shouldReturnAuthenticatedUser_WhenValidAuthentication() throws Exception {
            // Given
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    VALID_USERNAME,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            // When & Then
            mockMvc.perform(get("/auth/check-auth"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.authenticated").value(true))
                    .andExpect(jsonPath("$.username").value(VALID_USERNAME))
                    .andExpect(jsonPath("$.message").value("Autenticado"));
        }

        @Test
        @DisplayName("Debe retornar no autenticado cuando no hay autenticación")
        void shouldReturnNotAuthenticated_WhenNoAuthentication() throws Exception {
            // Given
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(null);
            SecurityContextHolder.setContext(securityContext);

            // When & Then
            mockMvc.perform(get("/auth/check-auth"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.authenticated").value(false))
                    .andExpect(jsonPath("$.username").doesNotExist())
                    .andExpect(jsonPath("$.message").value("No autenticado"));
        }

        @Test
        @DisplayName("Debe retornar no autenticado cuando el usuario es anónimo")
        void shouldReturnNotAuthenticated_WhenAnonymousUser() throws Exception {
            // Given
            Authentication authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn("anonymousUser");

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            // When & Then
            mockMvc.perform(get("/auth/check-auth"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.authenticated").value(false))
                    .andExpect(jsonPath("$.username").doesNotExist())
                    .andExpect(jsonPath("$.message").value("No autenticado"));
        }

        @Test
        @DisplayName("Debe retornar no autenticado cuando la autenticación no es válida")
        void shouldReturnNotAuthenticated_WhenAuthenticationIsNotValid() throws Exception {
            // Given
            Authentication authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(false);

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            // When & Then
            mockMvc.perform(get("/auth/check-auth"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.authenticated").value(false))
                    .andExpect(jsonPath("$.username").doesNotExist())
                    .andExpect(jsonPath("$.message").value("No autenticado"));
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Debe manejar flujo completo de registro y login")
        void shouldHandleCompleteRegistrationAndLoginFlow() throws Exception {
            // Given - Registro
            NewUserDto newUserDto = new NewUserDto();
            newUserDto.setUserName(VALID_USERNAME);
            newUserDto.setEmail(VALID_EMAIL);
            newUserDto.setPassword(VALID_PASSWORD);

            when(authService.registerUser(any(NewUserDto.class))).thenReturn(JWT_TOKEN);

            // When & Then - Registro
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUserDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value(JWT_TOKEN));

            // Given - Login
            LoginUserDto loginUserDto = new LoginUserDto();
            loginUserDto.setUserName(VALID_USERNAME);
            loginUserDto.setPassword(VALID_PASSWORD);

            when(authService.authenticate(loginUserDto.getUserName(), loginUserDto.getPassword()))
                    .thenReturn(JWT_TOKEN);

            // When & Then - Login
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginUserDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(JWT_TOKEN));

            verify(authService).registerUser(any(NewUserDto.class));
            verify(authService).authenticate(loginUserDto.getUserName(), loginUserDto.getPassword());
        }

        @Test
        @DisplayName("Debe manejar múltiples errores consecutivos")
        void shouldHandleMultipleConsecutiveErrors() throws Exception {
            // Given
            LoginUserDto loginUserDto = new LoginUserDto();
            loginUserDto.setUserName(VALID_USERNAME);
            loginUserDto.setPassword("wrongpassword");

            when(authService.authenticate(anyString(), anyString()))
                    .thenThrow(new AuthenticationFailedException("Credenciales inválidas"));

            // When & Then - Múltiples intentos fallidos
            for (int i = 0; i < 3; i++) {
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginUserDto)))
                        .andExpect(status().isUnauthorized());
            }

            verify(authService, times(3)).authenticate(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Debe manejar JSON malformado")
        void shouldHandleMalformedJson() throws Exception {
            // When & Then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(" "))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).authenticate(anyString(), anyString());
        }

        @Test
        @DisplayName("Debe manejar Content-Type incorrecto")
        void shouldHandleIncorrectContentType() throws Exception {
            // Given
            LoginUserDto loginUserDto = new LoginUserDto();
            loginUserDto.setUserName(VALID_USERNAME);
            loginUserDto.setPassword(VALID_PASSWORD);

            // When & Then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(objectMapper.writeValueAsString(loginUserDto)))
                    .andExpect(status().isUnsupportedMediaType());

            verify(authService, never()).authenticate(anyString(), anyString());
        }

        @Test
        @DisplayName("Debe manejar campos null en lugar de vacíos")
        void shouldHandleNullFields() throws Exception {
            // Given
            String jsonWithNullFields = "{\"userName\":null,\"password\":null}";

            // When & Then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonWithNullFields))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).authenticate(anyString(), anyString());
        }

        @Test
        @DisplayName("Debe manejar campos con solo espacios en blanco")
        void shouldHandleWhitespaceOnlyFields() throws Exception {
            // Given
            LoginUserDto loginUserDto = new LoginUserDto();
            loginUserDto.setUserName("   ");
            loginUserDto.setPassword("   ");

            // When & Then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginUserDto)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).authenticate(anyString(), anyString());
        }
    }
}