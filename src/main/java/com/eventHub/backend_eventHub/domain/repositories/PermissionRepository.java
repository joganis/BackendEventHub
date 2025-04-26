package com.eventHub.backend_eventHub.domain.repositories;


import com.eventHub.backend_eventHub.domain.entities.Permission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la entidad Permission.
 *
 * Gestiona la colección "permissions" en MongoDB.
 */
@Repository
public interface PermissionRepository extends MongoRepository<Permission, String> {
    // Métodos de consulta adicionales
}
