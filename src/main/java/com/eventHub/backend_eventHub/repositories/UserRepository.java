package com.eventHub.backend_eventHub.repositories;



import com.eventHub.backend_eventHub.entities.Users;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad User.
 *
 * Gestiona la colección "users" en MongoDB.
 */
@Repository
public interface UserRepository extends MongoRepository<Users, String> {
    Optional<Users> findByUserName(String userName);
    boolean existsByUserName(String userName);
    Optional<Users> findByEmail(String email);

}
