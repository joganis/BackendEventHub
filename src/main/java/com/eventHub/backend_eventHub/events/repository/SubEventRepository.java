// Nuevo: SubEventRepository
package com.eventHub.backend_eventHub.events.repository;

import com.eventHub.backend_eventHub.events.entities.SubEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubEventRepository extends MongoRepository<SubEvent, String> {


    // Subeventos de un evento principal (CORREGIDO)
    @Query("{'eventoPrincipal.$id': ?0}")
    List<SubEvent> findByEventoPrincipalId(String eventoPrincipalId);

    // Subeventos por estado (CORREGIDO)
    @Query("{'eventoPrincipal.$id': ?0, 'status.nameState': {$regex: ?1, $options: 'i'}}")
    List<SubEvent> findByEventoPrincipalIdAndStatusNameStateIgnoreCase(String eventoPrincipalId, String status);

    // Subeventos por creador (CORREGIDO)
    @Query("{'creator.userName': ?0}")
    List<SubEvent> findByCreatorUserName(String userName);



}