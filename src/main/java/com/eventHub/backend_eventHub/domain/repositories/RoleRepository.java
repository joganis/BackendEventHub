package com.eventHub.backend_eventHub.domain.repositories;




import com.eventHub.backend_eventHub.domain.entities.Role;
import com.eventHub.backend_eventHub.domain.enums.RoleList;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad Role.
 *
 * Gestiona la colección "roles" en MongoDB.
 */
@Repository
public interface RoleRepository extends MongoRepository<Role, String> {
    Optional<Role> findByNombreRol(RoleList nombreRol);
}
