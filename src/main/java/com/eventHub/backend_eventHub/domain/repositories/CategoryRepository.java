// Nuevo: CategoryRepository
package com.eventHub.backend_eventHub.domain.repositories;

import com.eventHub.backend_eventHub.domain.entities.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {

    // Categorías activas
    List<Category> findByActivaTrue();

    // Buscar por nombre (activas)
    List<Category> findByNombreCategoriaContainingIgnoreCaseAndActivaTrue(String nombreCategoria);

    // Buscar por nombre exacto (case insensitive)
    Optional<Category> findByNombreCategoriaIgnoreCase(String nombreCategoria);

    // Verificar existencia por nombre
    boolean existsByNombreCategoriaIgnoreCase(String nombreCategoria);

    // Buscar categorías por estado
    List<Category> findByActiva(boolean activa);

    // Buscar por nombre sin filtro de activa
    @Query("{'nombreCategoria': {$regex: ?0, $options: 'i'}}")
    List<Category> findByNombreCategoriaContainingIgnoreCase(String nombreCategoria);
}