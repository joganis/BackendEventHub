package com.eventHub.backend_eventHub.domain.repositories;

import com.eventHub.backend_eventHub.domain.entities.State;
import com.eventHub.backend_eventHub.domain.enums.StateList;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad State.
 *
 * Gestiona la colección "estados" en MongoDB.
 */
@Repository
public interface StateRepository extends MongoRepository<State, String> {
    /**
     * Busca un estado por su nombre, sin importar mayúsculas/minúsculas.
     *
     * @param nameState nombre del estado a buscar
     * @return Optional con el estado si existe, o vacío si no
     */
    Optional<State> findByNameState(String nameState);

    /**
     * Busca un estado por el valor del enum StateList
     *
     * @param stateEnum valor del enum StateList a buscar
     * @return Optional con el estado si existe, o vacío si no
     */
    Optional<State> findByNameState(StateList stateEnum);
}