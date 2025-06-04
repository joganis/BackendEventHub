package com.eventHub.backend_eventHub.domain.repositories;

// ========== TESTS PARA REPOSITORIOS ==========



import com.eventHub.backend_eventHub.domain.entities.Users;
import com.eventHub.backend_eventHub.domain.entities.Role;
import com.eventHub.backend_eventHub.domain.entities.State;
import com.eventHub.backend_eventHub.domain.enums.RoleList;
import com.eventHub.backend_eventHub.domain.enums.StateList;
import com.eventHub.backend_eventHub.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
@ActiveProfiles("test")
@DataMongoTest
@SpringJUnitConfig
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StateRepository stateRepository;

    private Users testUser;
    private Role testRole;
    private State testState;

    @BeforeEach
    void setUp() {

//        cd C:\Users\Jobany\dump\EventHub\EventHub
//        Ejecuta:
//        mongorestore --db EventHubTest --drop .
        userRepository.deleteAll();
        roleRepository.deleteAll();
        stateRepository.deleteAll();

        // Crear estado de prueba
        testState = new State("1", StateList.Active);
        stateRepository.save(testState);

        // Crear rol de prueba
        testRole = new Role("1", RoleList.ROLE_USUARIO, null);
        roleRepository.save(testRole);

        // Crear usuario de prueba
        testUser = new Users();
        testUser.setUserName("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
        testUser.setRole(testRole);
        testUser.setState(testState);
        testUser.setName("Test");
        testUser.setLastName("User");
    }

    @Test
    void shouldFindUserByUserName() {
        // Given
        userRepository.save(testUser);

        // When
        Optional<Users> found = userRepository.findByUserName("testuser");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUserName()).isEqualTo("testuser");
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldReturnEmptyWhenUserNameNotFound() {
        // When
        Optional<Users> found = userRepository.findByUserName("nonexistent");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldCheckIfUserNameExists() {
        // Given
        userRepository.save(testUser);

        // When & Then
        assertThat(userRepository.existsByUserName("testuser")).isTrue();
        assertThat(userRepository.existsByUserName("nonexistent")).isFalse();
    }

    @Test
    void shouldFindUserByEmail() {
        // Given
        userRepository.save(testUser);

        // When
        Optional<Users> found = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldCheckIfEmailExists() {
        // Given
        userRepository.save(testUser);

        // When & Then
        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }

    @Test
    void shouldFindUsersByStateIgnoreCase() {
        // Given
        userRepository.save(testUser);

        // When
        var users = userRepository.findByStateNameStateIgnoreCase(String.valueOf(StateList.Active));

        // Then
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUserName()).isEqualTo("testuser");
    }
}

@DataMongoTest
@SpringJUnitConfig
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        roleRepository.deleteAll();
    }

    @Test
    void shouldFindRoleByNombreRol() {
        // Given
        Role role = new Role("1", RoleList.ROLE_ADMIN, null);
        roleRepository.save(role);

        // When
        Optional<Role> found = roleRepository.findByNombreRol(RoleList.ROLE_ADMIN);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getNombreRol()).isEqualTo(RoleList.ROLE_ADMIN);
    }

    @Test
    void shouldReturnEmptyWhenRoleNotFound() {
        // When
        Optional<Role> found = roleRepository.findByNombreRol(RoleList.ROLE_ADMIN);

        // Then
        assertThat(found).isEmpty();
    }
}
@ActiveProfiles("test")
@DataMongoTest
@SpringJUnitConfig
class StateRepositoryTest {

    @Autowired
    private StateRepository stateRepository;

    @BeforeEach
    void setUp() {
        stateRepository.deleteAll();
    }

    @Test
    void shouldFindStateByNameState() {
        // Given
        State state = new State("1", StateList.Active);
        stateRepository.save(state);

        // When
        Optional<State> found = stateRepository.findByNameState(StateList.Active);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getNameState()).isEqualTo(StateList.Active);
    }
}