// DataInitializer.java
package com.eventHub.backend_eventHub.config;

import com.eventHub.backend_eventHub.domain.entities.Role;
import com.eventHub.backend_eventHub.domain.entities.State;
import com.eventHub.backend_eventHub.domain.entities.Users;
import com.eventHub.backend_eventHub.domain.enums.RoleList;
import com.eventHub.backend_eventHub.domain.enums.StateList;
import com.eventHub.backend_eventHub.domain.repositories.RoleRepository;
import com.eventHub.backend_eventHub.domain.repositories.StateRepository;
import com.eventHub.backend_eventHub.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Inicializador de datos básicos SOLO para desarrollo y testing.
 *
 * IMPORTANTE: Este componente SOLO se ejecuta en perfiles específicos,
 * NUNCA en producción, para evitar sobrescribir datos reales.
 */
@Component
@Profile({"dev", "test", "integration"}) // ESPECÍFICO para entornos seguros
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final StateRepository stateRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String activeProfile = System.getProperty("spring.profiles.active", "unknown");
        log.info(" DataInitializer ejecutándose en perfil: {}", activeProfile);

        // Verificación de seguridad adicional
        if ("prod".equals(activeProfile) || "production".equals(activeProfile)) {
            log.warn("DataInitializer NO debe ejecutarse en producción. Saltando...");
            return;
        }

        log.info(" Verificando e inicializando datos básicos...");

        // Solo inicializar si no existen datos (idempotente)
        initializeStatesIfEmpty();
        initializeRolesIfEmpty();
        initializeUsersIfEmpty();

        log.info("Verificación de datos completada para perfil: {}", activeProfile);
    }

    /**
     * Inicializa los estados básicos solo si no existen.
     */
    private void initializeStatesIfEmpty() {
        long stateCount = stateRepository.count();
        log.info(" Estados existentes en BD: {}", stateCount);

        if (stateCount == 0) {
            log.info(" Creando estados básicos...");

            for (StateList stateEnum : StateList.values()) {
                State state = new State(null, stateEnum);
                stateRepository.save(state);
                log.debug("Estado creado: {}", stateEnum.name());
            }

            log.info("{} estados inicializados", StateList.values().length);
        } else {
            log.info("Estados ya existen, saltando inicialización");
        }
    }

    /**
     * Inicializa los roles básicos solo si no existen.
     */
    private void initializeRolesIfEmpty() {
        long roleCount = roleRepository.count();
        log.info("Roles existentes en BD: {}", roleCount);

        if (roleCount == 0) {
            log.info("Creando roles básicos...");

            for (RoleList roleEnum : RoleList.values()) {
                Role role = new Role(null, roleEnum, null);
                roleRepository.save(role);
                log.debug("Rol creado: {}", roleEnum.name());
            }

            log.info(" {} roles inicializados", RoleList.values().length);
        } else {
            log.info(" Roles ya existen, saltando inicialización");
        }
    }

    /**
     * Inicializa usuarios básicos solo si no existen.
     */
    private void initializeUsersIfEmpty() {
        long userCount = userRepository.count();
        log.info(" Usuarios existentes en BD: {}", userCount);

        if (userCount == 0) {
            log.info("Creando usuarios básicos...");

            try {
                // Obtener entidades necesarias
                State activeState = stateRepository.findByNameState(StateList.Active)
                        .orElseThrow(() -> new RuntimeException("Estado Active no encontrado"));

                Role adminRole = roleRepository.findByNombreRol(RoleList.ROLE_ADMIN)
                        .orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado"));

                Role userRole = roleRepository.findByNombreRol(RoleList.ROLE_USUARIO)
                        .orElseThrow(() -> new RuntimeException("Rol USUARIO no encontrado"));

                // Usuario administrador
                Users admin = createAdminUser(adminRole, activeState);
                userRepository.save(admin);
                log.info(" Usuario admin creado: {}", admin.getUserName());

                // Usuario de prueba
                Users testUser = createTestUser(userRole, activeState);
                userRepository.save(testUser);
                log.info(" Usuario de prueba creado: {}", testUser.getUserName());

                log.info(" 2 usuarios básicos inicializados (admin, testuser)");

            } catch (Exception e) {
                log.error(" Error inicializando usuarios: {}", e.getMessage(), e);
                throw e;
            }
        } else {
            log.info(" Usuarios ya existen, saltando inicialización");
        }
    }

    /**
     * Crea el usuario administrador básico.
     */
    private Users createAdminUser(Role adminRole, State activeState) {
        Users admin = new Users();
        admin.setUserName("admin");
        admin.setEmail("admin@eventhub.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setName("Administrador");
        admin.setLastName("Sistema");
        admin.setIdentification("12345678");
        admin.setBirthDate("1990-01-01");
        admin.setPhone("3001234567");
        admin.setHomeAddress("Oficina Principal EventHub");
        admin.setCountry("Colombia");
        admin.setCity("Bogotá");
        admin.setRole(adminRole);
        admin.setState(activeState);
        return admin;
    }

    /**
     * Crea el usuario de prueba básico.
     */
    private Users createTestUser(Role userRole, State activeState) {
        Users testUser = new Users();
        testUser.setUserName("testuser");
        testUser.setEmail("test@eventhub.com");
        testUser.setPassword(passwordEncoder.encode("user123"));
        testUser.setName("Usuario");
        testUser.setLastName("Prueba");
        testUser.setIdentification("87654321");
        testUser.setBirthDate("1995-05-15");
        testUser.setPhone("3009876543");
        testUser.setHomeAddress("Dirección de Prueba 123");
        testUser.setCountry("Colombia");
        testUser.setCity("Medellín");
        testUser.setRole(userRole);
        testUser.setState(activeState);
        return testUser;
    }
}