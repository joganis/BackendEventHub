package com.eventHub.backend_eventHub.users.repository;

import com.eventHub.backend_eventHub.domain.entities.Users;
import com.eventHub.backend_eventHub.domain.enums.StateList;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad User.
 *
 * Gestiona la colección "users" en MongoDB.
 */
@Repository
public interface UserRepository extends MongoRepository<Users, String> {
    // ========== BÚSQUEDAS BÁSICAS (YA FUNCIONAN) ==========

    Optional<Users> findByUserName(String userName);
    boolean existsByUserName(String userName);
    Optional<Users> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("{'userName': {$regex: ?0, $options: 'i'}}")
    List<Users> findByUserNameIsContainingIgnoreCase(String userName);

    // ========== BÚSQUEDAS POR ESTADO ==========

    @Query("{'state.nameState': {$regex: ?0, $options: 'i'}}")
    List<Users> findByStateNameStateIgnoreCase(String nameState);

    @Query("{'state.nameState': ?0}")
    List<Users> findByState_NameState(StateList state);

    // ========== BÚSQUEDAS POR ROL ==========

    @Query("{'role.nombreRol': ?0}")
    List<Users> findByRoleNombreRol(String nombreRol);

    @Query("{'role.nombreRol': {$regex: ?0, $options: 'i'}}")
    List<Users> findByRoleNombreRolIgnoreCase(String nombreRol);
}
