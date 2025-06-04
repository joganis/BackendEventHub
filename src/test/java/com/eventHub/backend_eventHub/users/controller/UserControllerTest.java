package com.eventHub.backend_eventHub.users.controller;

import com.eventHub.backend_eventHub.domain.entities.Role;
import com.eventHub.backend_eventHub.domain.entities.State;
import com.eventHub.backend_eventHub.domain.entities.Users;
import com.eventHub.backend_eventHub.domain.enums.RoleList;
import com.eventHub.backend_eventHub.domain.enums.StateList;
import com.eventHub.backend_eventHub.users.dto.ChangeStatusDto;
import com.eventHub.backend_eventHub.users.dto.UpdateUserDto;
import com.eventHub.backend_eventHub.users.dto.UserProfileDto;
import com.eventHub.backend_eventHub.users.exception.UserNotFoundException;
import com.eventHub.backend_eventHub.users.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Tests")
@ActiveProfiles("test")
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private Principal principal;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper;
    private Users testUser;
    private Role userRole;
    private State activeState;
    private UpdateUserDto updateUserDto;
    private ChangeStatusDto changeStatusDto;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // Setup test entities
        activeState = new State("1", StateList.Active);
        userRole = new Role("1", RoleList.ROLE_USUARIO, null);

        testUser = new Users();
        testUser.setId("user123");
        testUser.setUserName("testuser");
        testUser.setEmail("test@example.com");
        testUser.setName("Test");
        testUser.setLastName("User");
        testUser.setState(activeState);
        testUser.setRole(userRole);
        testUser.setPhone("3001234567");
        testUser.setIdentification("12345678");
        testUser.setBirthDate("1990-01-01");
        testUser.setHomeAddress("Test Address 123");
        testUser.setCountry("Colombia");
        testUser.setCity("Bogotá");

        // Setup DTOs
        updateUserDto = new UpdateUserDto();
        updateUserDto.setEmail("updated@example.com");
        updateUserDto.setName("Updated");
        updateUserDto.setLastName("User");
        updateUserDto.setPhone("3001234567");
        updateUserDto.setIdentification("12345678");
        updateUserDto.setBirthDate("1990-01-01");
        updateUserDto.setHomeAddress("Test Address 123");
        updateUserDto.setCountry("Colombia");
        updateUserDto.setCity("Bogotá");

        changeStatusDto = new ChangeStatusDto();
        changeStatusDto.setState("Inactive");

        // NO configurar principal.getName() aquí - se hace en cada test específico
    }

    // ========================================
    // TESTS PARA PERFIL PROPIO (/users/me)
    // ========================================

    @Test
    @DisplayName("Should get authenticated user profile successfully")
    void shouldGetAuthenticatedUserProfileSuccessfully() {
        // Given
        when(userService.getMe(principal)).thenReturn(testUser);

        // When
        ResponseEntity<UserProfileDto> response = userController.getMe(principal);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo("user123");
        assertThat(response.getBody().getUserName()).isEqualTo("testuser");
        assertThat(response.getBody().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getBody().getState()).isEqualTo("Active");
        assertThat(response.getBody().getRole()).isEqualTo("ROLE_USUARIO");

        verify(userService).getMe(principal);
    }

    @Test
    @DisplayName("Should update authenticated user profile successfully")
    void shouldUpdateAuthenticatedUserProfileSuccessfully() {
        // Given
        testUser.setEmail("updated@example.com");
        testUser.setName("Updated");
        when(userService.updateMe(principal, updateUserDto)).thenReturn(testUser);

        // When
        ResponseEntity<UserProfileDto> response = userController.updateMe(principal, updateUserDto);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("updated@example.com");
        assertThat(response.getBody().getName()).isEqualTo("Updated");

        verify(userService).updateMe(principal, updateUserDto);
    }

    // ========================================
    // TESTS PARA GESTIÓN DE USUARIOS (ADMIN)
    // ========================================

    @Test
    @DisplayName("Should list all users successfully")
    void shouldListAllUsersSuccessfully() {
        // Given
        Users user2 = new Users();
        user2.setId("user456");
        user2.setUserName("testuser2");
        user2.setState(activeState);
        user2.setRole(userRole);

        List<Users> users = Arrays.asList(testUser, user2);
        when(userService.getAll(Optional.empty())).thenReturn(users);

        // When
        ResponseEntity<UserController.StandardListResponse<UserProfileDto>> response =
                userController.list(Optional.empty());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(2);
        assertThat(response.getBody().getTotal()).isEqualTo(2);
        assertThat(response.getBody().isFiltered()).isFalse();

        verify(userService).getAll(Optional.empty());
    }

    @Test
    @DisplayName("Should list users filtered by state")
    void shouldListUsersFilteredByState() {
        // Given
        when(userService.getAll(Optional.of("Active"))).thenReturn(Arrays.asList(testUser));

        // When
        ResponseEntity<UserController.StandardListResponse<UserProfileDto>> response =
                userController.list(Optional.of("Active"));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(1);
        assertThat(response.getBody().getTotal()).isEqualTo(1);
        assertThat(response.getBody().isFiltered()).isTrue();
        assertThat(response.getBody().getFilter()).isEqualTo("Active");

        verify(userService).getAll(Optional.of("Active"));
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void shouldGetUserByIdSuccessfully() {
        // Given
        when(userService.getById("user123")).thenReturn(testUser);

        // When
        ResponseEntity<UserProfileDto> response = userController.getById("user123");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo("user123");
        assertThat(response.getBody().getUserName()).isEqualTo("testuser");

        verify(userService).getById("user123");
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        when(userService.getById("nonexistent"))
                .thenThrow(new UserNotFoundException("Usuario no encontrado"));

        // When & Then
        assertThatThrownBy(() -> userController.getById("nonexistent"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Usuario no encontrado");

        verify(userService).getById("nonexistent");
    }

    @Test
    @DisplayName("Should search users successfully")
    void shouldSearchUsersSuccessfully() {
        // Given
        List<Users> users = Arrays.asList(testUser);
        when(userService.searchByUserName("test")).thenReturn(users);

        // When
        ResponseEntity<UserController.StandardListResponse<UserProfileDto>> response =
                userController.search("test");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(1);
        assertThat(response.getBody().getData().get(0).getUserName()).isEqualTo("testuser");
        assertThat(response.getBody().isFiltered()).isTrue();
        assertThat(response.getBody().getFilter()).isEqualTo("test");

        verify(userService).searchByUserName("test");
    }

    @Test
    @DisplayName("Should handle empty search results")
    void shouldHandleEmptySearchResults() {
        // Given
        when(userService.searchByUserName("nonexistent")).thenReturn(Arrays.asList());

        // When
        ResponseEntity<UserController.StandardListResponse<UserProfileDto>> response =
                userController.search("nonexistent");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEmpty();
        assertThat(response.getBody().getTotal()).isEqualTo(0);

        verify(userService).searchByUserName("nonexistent");
    }

    @Test
    @DisplayName("Should change user status successfully")
    void shouldChangeUserStatusSuccessfully() {
        // Given
        State inactiveState = new State("2", StateList.Inactive);
        testUser.setState(inactiveState);
        when(userService.changeState(eq("user123"), any(ChangeStatusDto.class)))
                .thenReturn(testUser);

        // When
        ResponseEntity<UserController.StandardApiResponse<UserProfileDto>> response =
                userController.changeStatus("user123", changeStatusDto);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("Estado del usuario actualizado exitosamente");
        assertThat(response.getBody().getData().getState()).isEqualTo("Inactive");

        verify(userService).changeState(eq("user123"), any(ChangeStatusDto.class));
    }

    // ========================================
    // TESTS PARA VERIFICACIÓN DE DISPONIBILIDAD
    // ========================================

    @Test
    @DisplayName("Should check username availability when available")
    void shouldCheckUsernameAvailabilityWhenAvailable() {
        // Given
        when(userService.isUserNameAvailable("newuser")).thenReturn(true);

        // When
        ResponseEntity<UserController.AvailabilityResponse> response =
                userController.checkUsernameAvailability("newuser");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isAvailable()).isTrue();
        assertThat(response.getBody().getField()).isEqualTo("username");
        assertThat(response.getBody().getValue()).isEqualTo("newuser");
        assertThat(response.getBody().getMessage()).isEqualTo("Username disponible");

        verify(userService).isUserNameAvailable("newuser");
    }

    @Test
    @DisplayName("Should check username availability when not available")
    void shouldCheckUsernameAvailabilityWhenNotAvailable() {
        // Given
        when(userService.isUserNameAvailable("existinguser")).thenReturn(false);

        // When
        ResponseEntity<UserController.AvailabilityResponse> response =
                userController.checkUsernameAvailability("existinguser");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isAvailable()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Username ya está en uso");

        verify(userService).isUserNameAvailable("existinguser");
    }

    @Test
    @DisplayName("Should check email availability when available")
    void shouldCheckEmailAvailabilityWhenAvailable() {
        // Given
        when(userService.isEmailAvailable("new@example.com")).thenReturn(true);

        // When
        ResponseEntity<UserController.AvailabilityResponse> response =
                userController.checkEmailAvailability("new@example.com");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isAvailable()).isTrue();
        assertThat(response.getBody().getField()).isEqualTo("email");
        assertThat(response.getBody().getValue()).isEqualTo("new@example.com");

        verify(userService).isEmailAvailable("new@example.com");
    }

    @Test
    @DisplayName("Should check email availability when not available")
    void shouldCheckEmailAvailabilityWhenNotAvailable() {
        // Given
        when(userService.isEmailAvailable("existing@example.com")).thenReturn(false);

        // When
        ResponseEntity<UserController.AvailabilityResponse> response =
                userController.checkEmailAvailability("existing@example.com");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isAvailable()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Email ya está en uso");

        verify(userService).isEmailAvailable("existing@example.com");
    }

    // ========================================
    // TESTS DE CASOS EDGE Y VALIDACIONES
    // ========================================

    @Test
    @DisplayName("Should handle null state filter gracefully")
    void shouldHandleNullStateFilterGracefully() {
        // Given
        when(userService.getAll(Optional.empty())).thenReturn(Arrays.asList(testUser));

        // When
        ResponseEntity<UserController.StandardListResponse<UserProfileDto>> response =
                userController.list(Optional.empty());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isFiltered()).isFalse();
        assertThat(response.getBody().getFilter()).isNull();

        verify(userService).getAll(Optional.empty());
    }

    @Test
    @DisplayName("Should handle user with null state in DTO mapping")
    void shouldHandleUserWithNullStateInDtoMapping() {
        // Given
        testUser.setState(null);
        when(userService.getById("user123")).thenReturn(testUser);

        // When
        ResponseEntity<UserProfileDto> response = userController.getById("user123");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getState()).isEqualTo("Unknown");

        verify(userService).getById("user123");
    }

    @Test
    @DisplayName("Should handle user with null role in DTO mapping")
    void shouldHandleUserWithNullRoleInDtoMapping() {
        // Given
        testUser.setRole(null);
        when(userService.getById("user123")).thenReturn(testUser);

        // When
        ResponseEntity<UserProfileDto> response = userController.getById("user123");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRole()).isEqualTo("Unknown");

        verify(userService).getById("user123");
    }

    @Test
    @DisplayName("Should handle partial user data in DTO mapping")
    void shouldHandlePartialUserDataInDtoMapping() {
        // Given
        testUser.setName(null);
        testUser.setLastName(null);
        testUser.setPhone(null);
        when(userService.getById("user123")).thenReturn(testUser);

        // When
        ResponseEntity<UserProfileDto> response = userController.getById("user123");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isNull();
        assertThat(response.getBody().getLastName()).isNull();
        assertThat(response.getBody().getPhone()).isNull();
        // Los campos obligatorios deben seguir presentes
        assertThat(response.getBody().getId()).isEqualTo("user123");
        assertThat(response.getBody().getUserName()).isEqualTo("testuser");

        verify(userService).getById("user123");
    }

    @Test
    @DisplayName("Should verify service method calls with correct parameters")
    void shouldVerifyServiceMethodCallsWithCorrectParameters() {
        // Given
        String searchTerm = "test";
        String userId = "user123";
        Optional<String> stateFilter = Optional.of("Active");

        when(userService.searchByUserName(searchTerm)).thenReturn(Arrays.asList(testUser));
        when(userService.getById(userId)).thenReturn(testUser);
        when(userService.getAll(stateFilter)).thenReturn(Arrays.asList(testUser));

        // When
        userController.search(searchTerm);
        userController.getById(userId);
        userController.list(stateFilter);

        // Then
        verify(userService).searchByUserName(searchTerm);
        verify(userService).getById(userId);
        verify(userService).getAll(stateFilter);
    }

    @Test
    @DisplayName("Should handle service exceptions gracefully")
    void shouldHandleServiceExceptionsGracefully() {
        // Given
        when(userService.getById("error123"))
                .thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        assertThatThrownBy(() -> userController.getById("error123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection error");

        verify(userService).getById("error123");
    }



}
