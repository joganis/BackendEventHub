package com.eventHub.backend_eventHub.users.service;

import com.azure.core.exception.ResourceNotFoundException;
import com.eventHub.backend_eventHub.domain.entities.State;
import com.eventHub.backend_eventHub.domain.entities.Users;
import com.eventHub.backend_eventHub.domain.enums.StateList;
import com.eventHub.backend_eventHub.domain.repositories.StateRepository;
import com.eventHub.backend_eventHub.users.dto.ChangeStatusDto;
import com.eventHub.backend_eventHub.users.dto.UpdateUserDto;
import com.eventHub.backend_eventHub.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final StateRepository stateRepository;

    /**
     * Asigna un estado predeterminado (Activo) a usuarios nuevos o existentes que no tienen estado.
     * Evita errores NullPointerException al usar el método getState().
     *
     * @param user Usuario a verificar y asignar estado si es necesario
     * @return Usuario con estado asignado (si era necesario)
     */
    private Users ensureUserHasState(Users user) {
        if (user.getState() == null) {
            // Buscar el estado "Active" en la base de datos Aqui REALIZE CAMBION EN IGNORE CASE
            State activeState = stateRepository.findByNameState(StateList.Active.name())
                    .orElseThrow(() -> {
                        log.error("Estado 'Active' no encontrado en la base de datos");
                        return new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Estado predeterminado no encontrado en el sistema"
                        );
                    });

            user.setState(activeState);
            user = userRepository.save(user);
            log.info("Estado predeterminado 'Active' asignado al usuario: {}", user.getUserName());
        }
        return user;
    }

    /**
     * Recupera todos los usuarios o filtra aquellos con el estado indicado.
     * Asegura que todos los usuarios tengan un estado válido.
     *
     * @param stateName Nombre del estado opcional ("Active", "Inactive", etc.)
     * @return Lista de usuarios filtrada o todos los usuarios si no se proporcionó estado
     */
    @Transactional(readOnly = true)
    public List<Users> getAll(Optional<String> stateName) {
        List<Users> users;

        if (stateName.isPresent()) {
            users = userRepository.findByStateNameStateIgnoreCase(stateName.get().trim());
        } else {
            users = userRepository.findAll();
        }

        // Asegurar que todos los usuarios tienen un estado asignado
        return users.stream()
                .map(this::ensureUserHasState)
                .toList();
    }

    /**
     * Obtiene un usuario por su ID o lanza excepción si no existe.
     * Asegura que el usuario tenga un estado válido.
     *
     * @param id Identificador del usuario
     * @return Usuario encontrado con estado válido
     */
    @Transactional(readOnly = true)
    public Users getById(String id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Usuario no encontrado con id: " + id
                        )
                );

        return ensureUserHasState(user);
    }


    /**
     * Recupera el usuario autenticado por su userName.
     */
    @Transactional(readOnly = true)
    public Users getMe(Principal principal) {
        return userRepository.findByUserName(principal.getName())
                .map(this::ensureUserHasState)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Usuario autenticado no encontrado"));
    }



    /**
     * Busca usuarios cuyo userName coincida parcial o exactamente.
     * Asegura que todos los usuarios tengan un estado válido.
     *
     * @param userName Subcadena a buscar en userName
     * @return Lista de usuarios encontrados con estado válido
     */
    @Transactional(readOnly = true)
    public List<Users> searchByUserName(String userName) {
        List<Users> users = userRepository.findByUserNameIsContainingIgnoreCase(userName);

        // Asegurar que todos los usuarios tienen un estado asignado
        return users.stream()
                .map(this::ensureUserHasState)
                .toList();
    }

    /**
     * Actualiza los datos de perfil de un usuario.
     *
     * @param id  Identificador del usuario a actualizar
     * @param dto Objeto con los nuevos valores (email, name, lastName, etc.)
     * @return El usuario ya persistido con los datos actualizados
     * @throws ResourceNotFoundException si no existe un usuario con ese ID
     */
    public Users updateUser(String id, UpdateUserDto dto) {
        // 1. Recuperar la entidad existente o lanzar excepción si no se encuentra
        Users user = getById(id);

        // 2. Mapear cada campo del DTO a la entidad
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());
        user.setLastName(dto.getLastName());
        user.setIdentification(dto.getIdentification());
        user.setBirthDate(dto.getBirthDate());
        user.setPhone(dto.getPhone());
        user.setHomeAddress(dto.getHomeAddress());
        user.setCountry(dto.getCountry());
        user.setCity(dto.getCity());
        user.setPhoto(dto.getPhoto());

        // 3. Guardar y devolver la entidad actualizada
        return userRepository.save(user);
    }


    /**
     * Actualiza el perfil del usuario autenticado.
     */
    public Users updateMe(Principal principal, UpdateUserDto dto) {
        Users me = getMe(principal);
        return updateUser(me.getId(), dto);
    }

    /**
     * Cambia únicamente el estado de un usuario.
     *
     * @param id  Identificador del usuario
     * @param dto DTO con el nuevo estado
     * @return Usuario con el estado modificado
     */
    public Users changeState(String id, ChangeStatusDto dto) {
        // 1) Recupera el usuario o lanza 404 automáticamente
        Users existing = getById(id);

        // 2) Convertir el estado del DTO al valor correspondiente en el enum StateList
        StateList stateEnum;
        try {
            // Intentar mapear el estado del DTO al enum
            String stateFromDto = dto.getState().trim();
            if ("Activo".equalsIgnoreCase(stateFromDto)) {
                stateEnum = StateList.Active;
            } else if ("Bloqueado".equalsIgnoreCase(stateFromDto)) {
                stateEnum = StateList.Blocked;
            } else {
                // Intentar parsear directamente por si coincide con un valor del enum
                stateEnum = StateList.valueOf(stateFromDto);
            }
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estado no válido: " + dto.getState()
            );
        }

        // 3) Busca el State por nombre del enum en la base de datos  CAMBI
        State newState = stateRepository
                .findByNameState(stateEnum.name())  //CAMBIO EN IGNORECASE findByNameStateIgnoreCase(stateEnum.name())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Estado no encontrado en la base de datos: " + stateEnum.name()
                        )
                );

        // 4) Asigna y persiste
        existing.setState(newState);
        return userRepository.save(existing);
    }
}