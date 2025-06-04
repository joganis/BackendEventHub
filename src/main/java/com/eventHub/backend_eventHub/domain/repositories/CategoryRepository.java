// Nuevo: CategoryRepository
package com.eventHub.backend_eventHub.domain.repositories;

import com.eventHub.backend_eventHub.domain.entities.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {

    Optional<Category> findByNombreCategoria(String nombreCategoria);
    List<Category> findByActivaTrue();
    boolean existsByNombreCategoria(String nombreCategoria);
}