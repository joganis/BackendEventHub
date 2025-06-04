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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final StateRepository stateRepository;

    // Mapeo de estados del frontend a enum
    private static final Map<String, StateList> STATE_MAPPING = Map.of(
            "Activo", StateList.Active,
            "Bloqueado", StateList.Blocked,
            "Active", StateList.Active,
            "Inactive", StateList.Inactive,
            "Pending", StateList.Pending,
            "Canceled", StateList.Canceled,
            "Blocked", StateList.Blocked
    );

    /**
     * Asigna un estado predeterminado (Active) a usuarios que no tienen estado.
     * Método transaccional para consistencia de datos.
     *
     * @param user Usuario a verificar y asignar estado si es necesario
     * @return Usuario con estado asignado
     * @throws UserServiceException si no se puede asignar el estado
     */
    @Transactional
    public Users ensureUserHasState(Users user) {
        if (user == null) {
            throw new IllegalArgumentException("Usuario no puede ser null");
        }

        if (user.getState() == null) {
            try {
                State activeState = stateRepository.findByNameState(StateList.Active)
                        .orElseThrow(() -> {
                            log.error("Estado 'Active' no encontrado en la base de datos");
                            return new UserServiceException(
                                    "Estado predeterminado no encontrado en el sistema",
                                    HttpStatus.INTERNAL_SERVER_ERROR
                            );
                        });

                user.setState(activeState);
                user = userRepository.save(user);
                log.info("Estado predeterminado 'Active' asignado al usuario: {}", user.getUserName());
            } catch (DataAccessException e) {
                log.error("Error accediendo a la base de datos al asignar estado al usuario: {}",
                        user.getUserName(), e);
                throw new UserServiceException("Error interno del servidor", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return user;
    }

    /**
     * Recupera todos los usuarios o filtra por estado.
     * Incluye caché para mejorar rendimiento.
     *
     * @param stateName Nombre del estado opcional
     * @return Lista de usuarios filtrada
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#stateName.orElse('all')")
    public List<Users> getAll(Optional<String> stateName) {
        try {
            List<Users> users;

            if (stateName.isPresent() && StringUtils.hasText(stateName.get())) {
                String stateValue = stateName.get().trim();
                users = userRepository.findByStateNameStateIgnoreCase(stateValue);
                log.debug("Encontrados {} usuarios con estado: {}", users.size(), stateValue);
            } else {
                users = userRepository.findAll();
                log.debug("Encontrados {} usuarios en total", users.size());
            }

            return users.stream()
                    .map(this::ensureUserHasState)
                    .toList();

        } catch (DataAccessException e) {
            log.error("Error accediendo a la base de datos al obtener usuarios", e);
            throw new UserServiceException("Error obteniendo usuarios", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obtiene un usuario por su ID.
     *
     * @param id Identificador del usuario
     * @return Usuario encontrado con estado válido
     * @throws UserNotFoundException si el usuario no existe
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "user", key = "#id")
    public Users getById(String id) {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("ID de usuario no puede estar vacío");
        }

        try {
            Users user = userRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Usuario no encontrado con id: {}", id);
                        return new UserNotFoundException("Usuario no encontrado con id: " + id);
                    });

            return ensureUserHasState(user);

        } catch (DataAccessException e) {
            log.error("Error accediendo a la base de datos al buscar usuario por ID: {}", id, e);
            throw new UserServiceException("Error obteniendo usuario", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Recupera el usuario autenticado por su userName.
     *
     * @param principal Principal del usuario autenticado
     * @return Usuario autenticado
     * @throws UserNotFoundException si el usuario no existe
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "userByUsername", key = "#principal.name")
    public Users getMe(Principal principal) {
        if (principal == null || !StringUtils.hasText(principal.getName())) {
            throw new IllegalArgumentException("Principal no puede ser null o vacío");
        }

        try {
            return userRepository.findByUserName(principal.getName())
                    .map(this::ensureUserHasState)
                    .orElseThrow(() -> {
                        log.warn("Usuario autenticado no encontrado: {}", principal.getName());
                        return new UserNotFoundException("Usuario autenticado no encontrado");
                    });

        } catch (DataAccessException e) {
            log.error("Error accediendo a la base de datos al buscar usuario autenticado: {}",
                    principal.getName(), e);
            throw new UserServiceException("Error obteniendo perfil de usuario", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Busca usuarios por userName con coincidencia parcial.
     *
     * @param userName Subcadena a buscar en userName
     * @return Lista de usuarios encontrados
     */
    @Transactional(readOnly = true)
    public List<Users> searchByUserName(String userName) {
        if (!StringUtils.hasText(userName)) {
            throw new IllegalArgumentException("userName no puede estar vacío");
        }

        try {
            String searchTerm = userName.trim();
            List<Users> users = userRepository.findByUserNameIsContainingIgnoreCase(searchTerm);

            log.debug("Encontrados {} usuarios que contienen: {}", users.size(), searchTerm);

            return users.stream()
                    .map(this::ensureUserHasState)
                    .toList();

        } catch (DataAccessException e) {
            log.error("Error accediendo a la base de datos al buscar usuarios por userName: {}", userName, e);
            throw new UserServiceException("Error buscando usuarios", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Actualiza los datos de perfil de un usuario.
     * Incluye validaciones adicionales y manejo de cache.
     *
     * @param id  Identificador del usuario a actualizar
     * @param dto Objeto con los nuevos valores
     * @return El usuario actualizado
     * @throws UserNotFoundException si no existe el usuario
     * @throws UserServiceException si hay error en la actualización
     */
    @Transactional
    @CacheEvict(value = {"user", "userByUsername", "users"}, allEntries = true)
    public Users updateUser(String id, UpdateUserDto dto) {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("ID de usuario no puede estar vacío");
        }

        if (dto == null) {
            throw new IllegalArgumentException("DTO de actualización no puede ser null");
        }

        try {
            Users user = getById(id);

            // Verificar que el email no esté en uso por otro usuario
            if (StringUtils.hasText(dto.getEmail()) && !dto.getEmail().equals(user.getEmail())) {
                validateEmailUniqueness(dto.getEmail(), id);
            }

            // Mapear campos del DTO a la entidad
            mapDtoToUser(dto, user);

            Users updatedUser = userRepository.save(user);
            log.info("Usuario actualizado exitosamente: {}", updatedUser.getId());

            return updatedUser;

        } catch (DataAccessException e) {
            log.error("Error accediendo a la base de datos al actualizar usuario: {}", id, e);
            throw new UserServiceException("Error actualizando usuario", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Actualiza el perfil del usuario autenticado.
     *
     * @param principal Principal del usuario autenticado
     * @param dto       DTO con los datos a actualizar
     * @return Usuario actualizado
     */
    @Transactional
    @CacheEvict(value = {"user", "userByUsername", "users"}, allEntries = true)
    public Users updateMe(Principal principal, UpdateUserDto dto) {
        Users me = getMe(principal);
        return updateUser(me.getId(), dto);
    }

    /**
     * Cambia el estado de un usuario.
     * Incluye validaciones mejoradas y mapeo de estados.
     *
     * @param id  Identificador del usuario
     * @param dto DTO con el nuevo estado
     * @return Usuario con el estado modificado
     * @throws UserNotFoundException si no existe el usuario
     * @throws UserServiceException si el estado no es válido
     */
    @Transactional
    @CacheEvict(value = {"user", "userByUsername", "users"}, allEntries = true)
    public Users changeState(String id, ChangeStatusDto dto) {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("ID de usuario no puede estar vacío");
        }

        if (dto == null || !StringUtils.hasText(dto.getState())) {
            throw new IllegalArgumentException("Estado no puede estar vacío");
        }

        try {
            Users existing = getById(id);

            // Mapear el estado del DTO al enum correspondiente
            StateList stateEnum = mapStateFromDto(dto.getState().trim());

            // Buscar el estado en la base de datos
            State newState = stateRepository.findByNameState(stateEnum)
                    .orElseThrow(() -> {
                        log.error("Estado '{}' no encontrado en la base de datos", stateEnum.name());
                        return new UserServiceException(
                                "Estado no encontrado en la base de datos: " + stateEnum.name(),
                                HttpStatus.NOT_FOUND
                        );
                    });

            existing.setState(newState);
            Users updatedUser = userRepository.save(existing);

            log.info("Estado del usuario {} cambiado de {} a {}",
                    existing.getId(),
                    existing.getState() != null ? existing.getState().getNameState() : "null",
                    newState.getNameState());

            return updatedUser;

        } catch (DataAccessException e) {
            log.error("Error accediendo a la base de datos al cambiar estado del usuario: {}", id, e);
            throw new UserServiceException("Error cambiando estado del usuario", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Valida que un email no esté en uso por otro usuario.
     *
     * @param email  Email a validar
     * @param userId ID del usuario actual (para excluirlo de la validación)
     * @throws UserServiceException si el email ya está en uso
     */
    private void validateEmailUniqueness(String email, String userId) {
        userRepository.findByEmail(email)
                .filter(existingUser -> !existingUser.getId().equals(userId))
                .ifPresent(existingUser -> {
                    log.warn("Intento de uso de email duplicado: {} por usuario: {}", email, userId);
                    throw new UserServiceException(
                            "El email ya está en uso por otro usuario",
                            HttpStatus.CONFLICT
                    );
                });
    }

    /**
     * Mapea los campos del DTO a la entidad User.
     *
     * @param dto  DTO con los datos fuente
     * @param user Entidad usuario destino
     */
    private void mapDtoToUser(UpdateUserDto dto, Users user) {
        if (StringUtils.hasText(dto.getEmail())) {
            user.setEmail(dto.getEmail().trim());
        }
        if (StringUtils.hasText(dto.getName())) {
            user.setName(dto.getName().trim());
        }
        if (StringUtils.hasText(dto.getLastName())) {
            user.setLastName(dto.getLastName().trim());
        }
        if (StringUtils.hasText(dto.getIdentification())) {
            user.setIdentification(dto.getIdentification().trim());
        }
        if (StringUtils.hasText(dto.getBirthDate())) {
            user.setBirthDate(dto.getBirthDate().trim());
        }
        if (StringUtils.hasText(dto.getPhone())) {
            user.setPhone(dto.getPhone().trim());
        }
        if (StringUtils.hasText(dto.getHomeAddress())) {
            user.setHomeAddress(dto.getHomeAddress().trim());
        }
        if (StringUtils.hasText(dto.getCountry())) {
            user.setCountry(dto.getCountry().trim());
        }
        if (StringUtils.hasText(dto.getCity())) {
            user.setCity(dto.getCity().trim());
        }
        if (StringUtils.hasText(dto.getPhoto())) {
            user.setPhoto(dto.getPhoto().trim());
        }
    }

    /**
     * Mapea el estado desde el DTO al enum StateList.
     *
     * @param stateFromDto Estado recibido desde el frontend
     * @return StateList correspondiente
     * @throws UserServiceException si el estado no es válido
     */
    private StateList mapStateFromDto(String stateFromDto) {
        StateList stateEnum = STATE_MAPPING.get(stateFromDto);

        if (stateEnum == null) {
            // Intentar parsear directamente por si coincide con un valor del enum
            try {
                stateEnum = StateList.valueOf(stateFromDto);
            } catch (IllegalArgumentException e) {
                log.warn("Estado no válido recibido: {}", stateFromDto);
                throw new UserServiceException(
                        "Estado no válido: " + stateFromDto,
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        return stateEnum;
    }

    /**
     * Verifica si un usuario existe por su ID.
     *
     * @param id ID del usuario
     * @return true si existe, false en caso contrario
     */
    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        if (!StringUtils.hasText(id)) {
            return false;
        }

        try {
            return userRepository.existsById(id);
        } catch (DataAccessException e) {
            log.error("Error verificando existencia del usuario: {}", id, e);
            return false;
        }
    }

    /**
     * Verifica si un username está disponible.
     *
     * @param userName Username a verificar
     * @return true si está disponible, false si ya existe
     */
    @Transactional(readOnly = true)
    public boolean isUserNameAvailable(String userName) {
        if (!StringUtils.hasText(userName)) {
            return false;
        }

        try {
            return !userRepository.existsByUserName(userName.trim());
        } catch (DataAccessException e) {
            log.error("Error verificando disponibilidad del username: {}", userName, e);
            return false;
        }
    }

    /**
     * Verifica si un email está disponible.
     *
     * @param email Email a verificar
     * @return true si está disponible, false si ya existe
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }

        try {
            return !userRepository.existsByEmail(email.trim());
        } catch (DataAccessException e) {
            log.error("Error verificando disponibilidad del email: {}", email, e);
            return false;
        }
    }
}