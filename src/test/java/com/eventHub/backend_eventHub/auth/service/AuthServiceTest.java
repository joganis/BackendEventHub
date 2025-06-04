// ========== TESTS PARA SERVICIOS ==========

package com.eventHub.backend_eventHub.auth.service;

import com.eventHub.backend_eventHub.auth.dto.NewUserDto;
import com.eventHub.backend_eventHub.auth.exceptions.AuthenticationFailedException;
import com.eventHub.backend_eventHub.auth.exceptions.ResourceNotFoundException;
import com.eventHub.backend_eventHub.auth.exceptions.UserAlreadyExistsException;
import com.eventHub.backend_eventHub.auth.jwt.JwtUtil;
import com.eventHub.backend_eventHub.domain.entities.Role;
import com.eventHub.backend_eventHub.domain.entities.State;
import com.eventHub.backend_eventHub.domain.entities.Users;
import com.eventHub.backend_eventHub.domain.enums.RoleList;
import com.eventHub.backend_eventHub.domain.enums.StateList;
import com.eventHub.backend_eventHub.domain.repositories.RoleRepository;
import com.eventHub.backend_eventHub.domain.repositories.StateRepository;
import com.eventHub.backend_eventHub.users.repository.UserRepository;
import com.eventHub.backend_eventHub.utils.emails.dto.EmailDto;
import com.eventHub.backend_eventHub.utils.emails.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ActiveProfiles("test")
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
        testState = new State("1", StateList.Active);
        testRole = new Role("1", RoleList.ROLE_USUARIO, null);

        testUser = new Users();
        testUser.setId("1");
        testUser.setUserName("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(testRole);
        testUser.setState(testState);

        newUserDto = new NewUserDto();
        newUserDto.setUserName("newuser");
        newUserDto.setEmail("newuser@example.com");
        newUserDto.setPassword("password123");
    }

    @Test
    void shouldAuthenticateUserSuccessfully() {
        // Given
        String username = "testuser";
        String password = "password123";
        String expectedToken = "jwt.token.here";

        when(userAuthService.findByUserName(username)).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(authentication, "ROLE_USUARIO")).thenReturn(expectedToken);

        // When
        String result = authService.authenticate(username, password);

        // Then
        assertThat(result).isEqualTo(expectedToken);
        verify(userAuthService).findByUserName(username);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateToken(authentication, "ROLE_USUARIO");
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        String username = "nonexistent";
        String password = "password123";

        when(userAuthService.findByUserName(username)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.authenticate(username, password))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Usuario no encontrado");

        verify(userAuthService).findByUserName(username);
        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void shouldThrowExceptionWhenUserIsInactive() {
        // Given
        String username = "testuser";
        String password = "password123";

        State inactiveState = new State("2", StateList.Inactive);
        testUser.setState(inactiveState);

        when(userAuthService.findByUserName(username)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> authService.authenticate(username, password))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Usuario inactivo");

        verify(userAuthService).findByUserName(username);
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void shouldThrowExceptionWhenBadCredentials() {
        // Given
        String username = "testuser";
        String password = "wrongpassword";

        when(userAuthService.findByUserName(username)).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        assertThatThrownBy(() -> authService.authenticate(username, password))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Credenciales inválidas");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        // Given
        String expectedToken = "jwt.token.here";
        String encodedPassword = "encodedPassword123";

        when(userAuthService.existsByUserName(newUserDto.getUserName())).thenReturn(false);
        when(roleRepository.findByNombreRol(RoleList.ROLE_USUARIO)).thenReturn(Optional.of(testRole));
        when(stateRepository.findByNameState(StateList.Active)).thenReturn(Optional.of(testState));
        when(passwordEncoder.encode(newUserDto.getPassword())).thenReturn(encodedPassword);
        when(userAuthService.findByUserName(newUserDto.getUserName())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(authentication, "ROLE_USUARIO")).thenReturn(expectedToken);

        // When
        String result = authService.registerUser(newUserDto);

        // Then
        assertThat(result).isEqualTo(expectedToken);
        verify(userAuthService).existsByUserName(newUserDto.getUserName());
        verify(roleRepository).findByNombreRol(RoleList.ROLE_USUARIO);
        verify(stateRepository).findByNameState(StateList.Active);
        verify(passwordEncoder).encode(newUserDto.getPassword());
        verify(userAuthService).save(any(Users.class));
        verify(emailService).sendEmail(any(EmailDto.class));
    }

    @Test
    void shouldThrowExceptionWhenUserAlreadyExists() {
        // Given
        when(userAuthService.existsByUserName(newUserDto.getUserName())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.registerUser(newUserDto))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("El nombre de usuario ya existe");

        verify(userAuthService).existsByUserName(newUserDto.getUserName());
        verifyNoInteractions(roleRepository);
        verifyNoInteractions(stateRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldThrowExceptionWhenDefaultRoleNotFound() {
        // Given
        when(userAuthService.existsByUserName(newUserDto.getUserName())).thenReturn(false);
        when(roleRepository.findByNombreRol(RoleList.ROLE_USUARIO)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.registerUser(newUserDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Rol por defecto no encontrado");

        verify(roleRepository).findByNombreRol(RoleList.ROLE_USUARIO);
        verifyNoInteractions(stateRepository);
    }

    @Test
    void shouldThrowExceptionWhenDefaultStateNotFound() {
        // Given
        when(userAuthService.existsByUserName(newUserDto.getUserName())).thenReturn(false);
        when(roleRepository.findByNombreRol(RoleList.ROLE_USUARIO)).thenReturn(Optional.of(testRole));
        when(stateRepository.findByNameState(StateList.Active)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.registerUser(newUserDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Estado por defecto no encontrado");

        verify(stateRepository).findByNameState(StateList.Active);
    }

    @Test
    void shouldHandleEmailServiceFailureGracefully() {
        // Given
        String expectedToken = "jwt.token.here";
        String encodedPassword = "encodedPassword123";

        when(userAuthService.existsByUserName(newUserDto.getUserName())).thenReturn(false);
        when(roleRepository.findByNombreRol(RoleList.ROLE_USUARIO)).thenReturn(Optional.of(testRole));
        when(stateRepository.findByNameState(StateList.Active)).thenReturn(Optional.of(testState));
        when(passwordEncoder.encode(newUserDto.getPassword())).thenReturn(encodedPassword);
        when(userAuthService.findByUserName(newUserDto.getUserName())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(authentication, "ROLE_USUARIO")).thenReturn(expectedToken);
        doThrow(new RuntimeException("Email service error")).when(emailService).sendEmail(any(EmailDto.class));

        // When
        String result = authService.registerUser(newUserDto);

        // Then
        assertThat(result).isEqualTo(expectedToken);
        verify(emailService).sendEmail(any(EmailDto.class));
        // El registro debe completarse exitosamente a pesar del error del email
    }
}
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class UserAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAuthService userAuthService;

    private Users testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testRole = new Role("1", RoleList.ROLE_USUARIO, null);

        testUser = new Users();
        testUser.setId("1");
        testUser.setUserName("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
        testUser.setRole(testRole);
    }

    @Test
    void shouldLoadUserByUsernameSuccessfully() {
        // Given
        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(testUser));

        // When
        var userDetails = userAuthService.loadUserByUsername("testuser");

        // Then
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("password123");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USUARIO");

        verify(userRepository).findByUserName("testuser");
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findByUserName("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userAuthService.loadUserByUsername("nonexistent"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findByUserName("nonexistent");
    }

    @Test
    void shouldCheckIfUserExistsByUserName() {
        // Given
        when(userRepository.existsByUserName("testuser")).thenReturn(true);
        when(userRepository.existsByUserName("nonexistent")).thenReturn(false);

        // When & Then
        assertThat(userAuthService.existsByUserName("testuser")).isTrue();
        assertThat(userAuthService.existsByUserName("nonexistent")).isFalse();

        verify(userRepository).existsByUserName("testuser");
        verify(userRepository).existsByUserName("nonexistent");
    }

    @Test
    void shouldSaveUser() {
        // Given
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        userAuthService.save(testUser);

        // Then
        verify(userRepository).save(testUser);
    }

    @Test
    void shouldFindUserByUserName() {
        // Given
        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(testUser));

        // When
        Optional<Users> result = userAuthService.findByUserName("testuser");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
        verify(userRepository).findByUserName("testuser");
    }

    @Test
    void shouldFindUserByEmail() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        Object result = userAuthService.findByEmail("test@example.com");

        // Then
        assertThat(result).isEqualTo(Optional.of(testUser));
        verify(userRepository).findByEmail("test@example.com");
    }
}