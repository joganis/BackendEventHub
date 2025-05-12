package com.eventHub.backend_eventHub.users.repository;

import com.eventHub.backend_eventHub.domain.entities.Users;
import org.springframework.data.mongodb.repository.MongoRepository;
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
    Optional<Users> findByUserName(String userName);
    boolean existsByUserName(String userName);
    Optional<Users> findByEmail(String email);


    List<Users> findByUserNameIsContainingIgnoreCase(String userName);
    List<Users> findByEmailAndAndName (String email, String name);

    List<Users> findByStateNameStateIgnoreCase(String nameState);
}
