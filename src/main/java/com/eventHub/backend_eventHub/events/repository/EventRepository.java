package com.eventHub.backend_eventHub.events.repository;


import com.eventHub.backend_eventHub.events.entities.Event;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.mongodb.repository.Query;


import java.time.Instant;
@Repository
public interface EventRepository extends MongoRepository<Event, String> {

    /**
     * Busca eventos por estado
     */
    List<Event> findByStatusNameStateIgnoreCase(String nameState);

    /**
     * Busca eventos paginados por estado
     */
    Page<Event> findByStatusNameStateIgnoreCase(String nameState, Pageable pageable);

    /**
     * Busca eventos por creador
     */
    List<Event> findByCreatorUserName(String userName);

    /**
     * Busca eventos por categoría
     */
    List<Event> findByCategoriesContaining(String category);

    /**
     * Busca eventos que ocurren entre dos fechas
     */
    List<Event> findByStartGreaterThanEqualAndEndLessThanEqual(Instant startDate, Instant endDate);

    /**
     * Busca eventos futuros (a partir de ahora)
     */
    List<Event> findByStartGreaterThanEqual(Instant now);

    /**
     * Busca eventos pasados (antes de ahora)
     */
    List<Event> findByEndLessThan(Instant now);

    /**
     * Busca eventos por tipo
     */
    List<Event> findByType(String type);

    /**
     * Busca eventos por privacidad
     */
    List<Event> findByPrivacy(String privacy);

    /**
     * Busca eventos por tipo de ticket
     */
    List<Event> findByTicketType(String ticketType);

    /**
     * Busca eventos por ubicación (dirección contiene)
     */
    @Query("{'location.address': {$regex: ?0, $options: 'i'}}")
    List<Event> findByLocationAddressContaining(String address);

    /**
     * Busca eventos por tipo de ubicación
     */
    @Query("{'location.type': ?0}")
    List<Event> findByLocationType(String locationType);

    /**
     * Busca eventos cercanos a coordenadas
     * Usa la función de agregación geoNear para encontrar eventos cercanos a un punto
     */
    @Query(value = "{'location': {$near: {$geometry: {type: 'Point', coordinates: [?0, ?1]}, $maxDistance: ?2}}}")
    List<Event> findEventsNear(Double longitude, Double latitude, Integer maxDistanceMeters);

    /**
     * Busca eventos con disponibilidad (maxAttendees > 0)
     */
    List<Event> findByMaxAttendeesGreaterThan(Integer minAttendees);

    /**
     * Busca eventos por organizador
     */
    @Query("{'otherData.organizer': {$regex: ?0, $options: 'i'}}")
    List<Event> findByOrganizerContaining(String organizer);

    /**
     * Busca eventos por búsqueda de texto (título o descripción)
     */
    @Query("{'$or': [{'title': {$regex: ?0, $options: 'i'}}, {'description': {$regex: ?0, $options: 'i'}}]}")
    List<Event> searchByTitleOrDescription(String searchText);

    /**
     * Cuenta eventos por estado
     */
    long countByStatusNameStateIgnoreCase(String nameState);
}