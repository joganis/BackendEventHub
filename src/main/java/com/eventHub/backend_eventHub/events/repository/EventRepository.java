// EventRepository actualizado
package com.eventHub.backend_eventHub.events.repository;

import com.eventHub.backend_eventHub.events.entities.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EventRepository extends MongoRepository<Event, String> {

    // Buscar por estado (estos están bien)
    List<Event> findByStatusNameStateIgnoreCase(String nameState);
    Page<Event> findByStatusNameStateIgnoreCase(String nameState, Pageable pageable);

    // Buscar por creador (CORREGIDO)
    @Query("{'creator.userName': ?0}")
    List<Event> findByCreatorUserName(String userName);

    @Query("{'creator.userName': ?0}")
    Page<Event> findByCreatorUserName(String userName, Pageable pageable);

    // Buscar por categoría (estos están bien)
    List<Event> findByCategoriaId(String categoriaId);
    List<Event> findByCategoriaNombreCategoria(String nombreCategoria);

    // Eventos por fechas (estos están bien)
    List<Event> findByStartGreaterThanEqualAndEndLessThanEqual(Instant startDate, Instant endDate);
    List<Event> findByStartGreaterThanEqual(Instant now);
    List<Event> findByEndLessThan(Instant now);

    // ===== EVENTOS PÚBLICOS (SIN AUTENTICACIÓN) =====
    // Estos métodos YA ESTÁN BIEN - usan @Query explícitas

    @Query("{'bloqueado': false, 'status.nameState': ?0, 'privacy': 'public'}")
    List<Event> findPublicEventsForUsers(String status);

    @Query("{'bloqueado': false, 'status.nameState': ?0, 'privacy': 'public', 'start': {$gte: ?1}}")
    List<Event> findUpcomingPublicEvents(String status, Instant now);

    @Query("{'bloqueado': false, 'status.nameState': ?0, 'privacy': 'public', 'destacado': true}")
    List<Event> findFeaturedPublicEvents(String status);

    @Query("{'bloqueado': false, 'privacy': 'public', 'createdAt': {$exists: true}}")
    List<Event> findRecentPublicEvents(Pageable pageable);

    @Query("{'$and': [" +
            "{'bloqueado': false}, " +
            "{'privacy': 'public'}, " +
            "{'$or': [" +
            "  {'title': {$regex: ?0, $options: 'i'}}, " +
            "  {'description': {$regex: ?0, $options: 'i'}}, " +
            "  {'tags': {$regex: ?0, $options: 'i'}}" +
            "]}" +
            "]}")
    List<Event> searchPublicEventsByText(String searchText);

    // ===== EVENTOS PARA USUARIOS AUTENTICADOS =====
    // Estos métodos YA ESTÁN BIEN

    @Query("{'bloqueado': false, 'status.nameState': ?0}")
    List<Event> findActiveEventsForAuthenticatedUsers(String status);

    @Query("{'bloqueado': false, 'status.nameState': ?0, 'start': {$gte: ?1}}")
    List<Event> findUpcomingEventsForAuthenticatedUsers(String status, Instant now);

    // CORREGIDO: Eventos accesibles para usuario
    @Query("{'$and': [" +
            "{'bloqueado': false}, " +
            "{'status.nameState': ?1}, " +
            "{'$or': [" +
            "  {'privacy': 'public'}, " +
            "  {'$and': [{'privacy': 'private'}, {'creator.userName': ?0}]}, " +
            "  {'$and': [{'privacy': 'private'}, {'invitedUsers': ?0}]}" +
            "]}" +
            "]}")
    List<Event> findAccessibleEventsForUser(String username, String status);

    // ===== MÉTODOS ESPECÍFICOS PARA PRIVACIDAD =====
    List<Event> findByPrivacyAndStatusNameStateIgnoreCase(String privacy, String status);

    // CORREGIDO: Verificar acceso a evento privado
    @Query("{'_id': ?0, '$or': [" +
            "{'privacy': 'public'}, " +
            "{'$and': [{'privacy': 'private'}, {'creator.userName': ?1}]}, " +
            "{'$and': [{'privacy': 'private'}, {'invitedUsers': ?1}]}" +
            "]}")
    Event findAccessibleEventById(String eventId, String username);

    // ===== RESTO DE MÉTODOS ESTÁN BIEN =====
    List<Event> findByDestacadoTrueAndStatusNameStateIgnoreCase(String status);
    List<Event> findByBloqueado(boolean bloqueado);

    @Query("{'maxAttendees': {$gt: '$currentAttendees'}, 'permitirInscripciones': true, 'bloqueado': false}")
    List<Event> findEventsWithAvailability();

    List<Event> findTop10ByCreatedAtOrderByCreatedAtDesc();
    List<Event> findByType(String type);
    List<Event> findByTicketType(String ticketType);

    @Query("{'location.address': {$regex: ?0, $options: 'i'}}")
    List<Event> findByLocationAddressContaining(String address);

    @Query("{'location.type': ?0}")
    List<Event> findByLocationType(String locationType);

    @Query(value = "{'location': {$near: {$geometry: {type: 'Point', coordinates: [?0, ?1]}, $maxDistance: ?2}}}")
    List<Event> findEventsNear(Double longitude, Double latitude, Integer maxDistanceMeters);

    List<Event> findByMaxAttendeesGreaterThan(Integer minAttendees);

    @Query("{'otherData.organizer': {$regex: ?0, $options: 'i'}}")
    List<Event> findByOrganizerContaining(String organizer);

    @Query("{'$or': [{'title': {$regex: ?0, $options: 'i'}}, {'description': {$regex: ?0, $options: 'i'}}]}")
    List<Event> searchByTitleOrDescription(String searchText);

    // ===== CONTADORES =====
    long countByStatusNameStateIgnoreCase(String nameState);
    long countByBloqueado(boolean bloqueado);
    long countByPrivacyAndStatusNameStateIgnoreCase(String privacy, String status);
}