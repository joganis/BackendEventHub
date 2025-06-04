// ================================
// 3. CategoryService NUEVO
// ================================

package com.eventHub.backend_eventHub.domain.service;

import com.eventHub.backend_eventHub.domain.dto.CategoryDto;
import com.eventHub.backend_eventHub.domain.entities.Category;
import com.eventHub.backend_eventHub.domain.repositories.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Obtiene todas las categorías activas
     */
    @Transactional(readOnly = true)
    public List<Category> getAllActiveCategories() {
        return categoryRepository.findByActivaTrue();
    }

    /**
     * Obtiene todas las categorías (activas e inactivas) - Solo para admin
     */
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Obtiene una categoría por ID
     */
    @Transactional(readOnly = true)
    public Category getCategoryById(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada con ID: " + id));
    }

    /**
     * Busca categorías por nombre
     */
    @Transactional(readOnly = true)
    public List<Category> searchCategoriesByName(String name) {
        return categoryRepository.findByNombreCategoriaContainingIgnoreCaseAndActivaTrue(name);
    }

    /**
     * Crea una nueva categoría
     */
    @Transactional
    public Category createCategory(CategoryDto categoryDto) {
        // Verificar que no existe una categoría con el mismo nombre
        if (categoryRepository.existsByNombreCategoriaIgnoreCase(categoryDto.getNombreCategoria())) {
            throw new IllegalArgumentException("Ya existe una categoría con el nombre: " + categoryDto.getNombreCategoria());
        }

        Category category = Category.builder()
                .nombreCategoria(categoryDto.getNombreCategoria())
                .descripcion(categoryDto.getDescripcion())
                .activa(categoryDto.isActiva())
                .build();

        return categoryRepository.save(category);
    }

    /**
     * Actualiza una categoría existente
     */
    @Transactional
    public Category updateCategory(String id, CategoryDto categoryDto) {
        Category category = getCategoryById(id);

        // Verificar que no existe otra categoría con el mismo nombre
        if (!category.getNombreCategoria().equalsIgnoreCase(categoryDto.getNombreCategoria()) &&
                categoryRepository.existsByNombreCategoriaIgnoreCase(categoryDto.getNombreCategoria())) {
            throw new IllegalArgumentException("Ya existe una categoría con el nombre: " + categoryDto.getNombreCategoria());
        }

        category.setNombreCategoria(categoryDto.getNombreCategoria());
        category.setDescripcion(categoryDto.getDescripcion());
        category.setActiva(categoryDto.isActiva());

        return categoryRepository.save(category);
    }

    /**
     * Activa/Desactiva una categoría
     */
    @Transactional
    public Category toggleCategoryStatus(String id) {
        Category category = getCategoryById(id);
        category.setActiva(!category.isActiva());
        return categoryRepository.save(category);
    }
}
