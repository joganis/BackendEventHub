// UserServiceTest.java
package com.eventHub.backend_eventHub.users.service;

import com.eventHub.backend_eventHub.domain.entities.State;
import com.eventHub.backend_eventHub.domain.entities.Users;
import com.eventHub.backend_eventHub.domain.enums.StateList;
import com.eventHub.backend_eventHub.domain.repositories.StateRepository;
import com.eventHub.backend_eventHub.users.dto.ChangeStatusDto;
import com.eventHub.backend_eventHub.users.dto.UpdateUserDto;
import com.eventHub.backend_eventHub.users.exception.UserNotFoundException;
import com.eventHub.backend_eventHub.users.exception.UserServiceException;
import com.eventHub.backend_eventHub.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ActiveProfiles;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private Principal principal;

    @InjectMocks
    private UserService userService;

    private Users testUser;
    private State activeState;
    private UpdateUserDto updateUserDto;
    private ChangeStatusDto changeStatusDto;

    @BeforeEach
    void setUp() {
        // Setup test data
        activeState = new State("1", StateList.Active);

        testUser = new Users();
        testUser.setId("user123");
        testUser.setUserName("testuser");
        testUser.setEmail("test@example.com");
        testUser.setName("Test");
        testUser.setLastName("User");
        testUser.setState(activeState);

        updateUserDto = new UpdateUserDto();
        updateUserDto.setEmail("updated@example.com");
        updateUserDto.setName("Updated");
        updateUserDto.setLastName("User");
        updateUserDto.setPhone("3001234567");
        updateUserDto.setIdentification("12345678");
        updateUserDto.setBirthDate("1990-01-01");
        updateUserDto.setHomeAddress("Test Address 123");

        changeStatusDto = new ChangeStatusDto();
        changeStatusDto.setState("Inactive");
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void shouldGetUserByIdSuccessfully() {
        // Given
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        // When
        Users result = userService.getById("user123");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserName()).isEqualTo("testuser");

        userRepository.findById("user123");
    }

    @Test
    @DisplayName("Should handle database errors gracefully")
    void shouldHandleDatabaseErrorsGracefully() {
        // Given
        when(userRepository.findById("user123"))
                .thenThrow(new DataAccessException("Database error") {});

        // When & Then
        assertThatThrownBy(() -> userService.getById("user123"))
                .isInstanceOf(UserServiceException.class)
                .hasMessageContaining("Error obteniendo usuario");
    }

    @Test
    @DisplayName("Should ensure user has state when state is null")
    void shouldEnsureUserHasStateWhenStateIsNull() {
        // Given
        testUser.setState(null);
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
        when(stateRepository.findByNameState(StateList.Active))
                .thenReturn(Optional.of(activeState));
        when(userRepository.save(any(Users.class))).thenReturn(testUser);

        // When
        Users result = userService.getById("user123");

        // Then
        assertThat(result.getState()).isNotNull();
        assertThat(result.getState().getNameState()).isEqualTo(StateList.Active);

        verify(stateRepository).findByNameState(StateList.Active);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should validate email availability")
    void shouldValidateEmailAvailability() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);

        // When
        boolean result = userService.isEmailAvailable("test@example.com");

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should validate username availability")
    void shouldValidateUsernameAvailability() {
        // Given
        when(userRepository.existsByUserName("newuser")).thenReturn(false);

        // When
        boolean result = userService.isUserNameAvailable("newuser");

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsByUserName("newuser");
    }

    @Test
    @DisplayName("Should throw exception with null principal")
    void shouldThrowExceptionWithNullPrincipal() {
        // When & Then
        assertThatThrownBy(() -> userService.getMe(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Principal no puede ser null");
    }

    @Test
    @DisplayName("Should throw exception with empty user ID")
    void shouldThrowExceptionWithEmptyUserId() {
        // When & Then
        assertThatThrownBy(() -> userService.getById(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID de usuario no puede estar vacío");
    }
}