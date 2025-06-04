// ================================
// 4. SubEventRepository DEFINITIVO
// ================================

package com.eventHub.backend_eventHub.events.repository;

import com.eventHub.backend_eventHub.events.entities.SubEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubEventRepository extends MongoRepository<SubEvent, String> {

    // ========== SUB-EVENTOS POR EVENTO PRINCIPAL ==========

    @Query("{'eventoPrincipal.$id': ?0}")
    List<SubEvent> findByEventoPrincipalId(String eventoPrincipalId);

    // ========== SUB-EVENTOS POR ESTADO ==========

    @Query("{'eventoPrincipal.$id': ?0, 'status.nameState': {$regex: ?1, $options: 'i'}}")
    List<SubEvent> findByEventoPrincipalIdAndStatusNameStateIgnoreCase(String eventoPrincipalId, String status);

    // ========== SUB-EVENTOS POR CREADOR ==========

    @Query("{'creator.userName': ?0}")
    List<SubEvent> findByCreatorUserName(String userName);

    @Query("{'creator.$id': ?0}")
    List<SubEvent> findByCreatorId(String creatorId);

    // ========== SUB-EVENTOS POR ESTADO GENERAL ==========

    @Query("{'status.nameState': {$regex: ?0, $options: 'i'}}")
    List<SubEvent> findByStatusNameStateIgnoreCase(String status);

    // ========== SUB-EVENTOS ACTIVOS DE UN EVENTO ==========

    @Query("{'eventoPrincipal.$id': ?0, 'status.nameState': 'Active'}")
    List<SubEvent> findActiveSubEventsByEventoPrincipalId(String eventoPrincipalId);

    // ========== BÚSQUEDA POR TIPO ==========

    @Query("{'eventoPrincipal.$id': ?0, 'type': ?1}")
    List<SubEvent> findByEventoPrincipalIdAndType(String eventoPrincipalId, String type);

    // ========== SUB-EVENTOS CON DISPONIBILIDAD ==========

    @Query("{'eventoPrincipal.$id': ?0, '$expr': {'$lt': ['$currentAttendees', '$maxAttendees']}}")
    List<SubEvent> findAvailableSubEventsByEventoPrincipalId(String eventoPrincipalId);
}