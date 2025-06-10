// ================================
// 3. EventRepository DEFINITIVO
// ================================

package com.eventHub.backend_eventHub.events.repository;

import com.eventHub.backend_eventHub.events.entities.Event;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EventRepository extends MongoRepository<Event, String> {

    // ========== BÚSQUEDAS POR ESTADO ==========

    @Query("{'status.nameState': {$regex: ?0, $options: 'i'}}")
    List<Event> findByStatusNameStateIgnoreCase(String nameState);

    @Query("{'status.nameState': {$regex: ?0, $options: 'i'}}")
    Page<Event> findByStatusNameStateIgnoreCase(String nameState, Pageable pageable);

    @Query("{'bloqueado': false, 'status.$id': ObjectId(?0), 'privacy': 'public', 'start': {$gte: ?1}}")
    List<Event> findUpcomingPublicActiveEvents(String activeStateId, Instant now);

    // ========== BÚSQUEDAS POR CREADOR ==========

    @Query("{'creator.userName': ?0}")
    List<Event> findByCreatorUserName(String userName);  //esta es la original

    @Query("{'creator': {'$ref': 'users', '$id': ?0}}")
    List<Event> findByCreatorReference(String creatorId); //usar para listar todos


//    @Query("{'creator.$id': ?0}")
//    List<Event> findByCreatorId(String creatorId);
    @Query("{'creator.$id': ObjectId(?0)}")
    List<Event> findByCreatorId(String creatorId);

    @Query("{'creator.userName': ?0}")
    Page<Event> findByCreatorUserName(String userName, Pageable pageable);



    // ========== BÚSQUEDAS POR id de estados mas eficientes ==========

    @Query("{'privacy': 'public', 'bloqueado': false, 'destacado': true, 'status.$id': ObjectId(?0)}")
    List<Event> findFeaturedPublicActiveEvents(String activeStateId);

    @Query("{'bloqueado': false, 'privacy': 'public', 'status.$id': ObjectId(?0), 'createdAt': {$exists: true}}")
    List<Event> findRecentPublicActiveEvents(String activeStateId, Pageable pageable);

    @Query("{'$and': [" +
            "{'bloqueado': false}, " +
            "{'status.$id': ObjectId(?1)}, " +
            "{'$or': [" +
            "  {'privacy': 'public'}, " +
            "  {'$and': [{'privacy': 'private'}, {'creator.userName': ?0}]}, " +
            "  {'$and': [{'privacy': 'private'}, {'invitedUsers': ?0}]}" +
            "]}" +
            "]}")
    List<Event> findAccessibleActiveEventsForUser(String username, String activeStateId);

    @Query("{'bloqueado': false, 'status.$id': ObjectId(?0), 'privacy': 'public'}")
    List<Event> findPublicActiveEvents(String activeStateId);

    // ========== BÚSQUEDAS POR CATEGORÍA ==========

//    @Query("{'categoria.$id': ?0}")   //eliminar se muesran lso eventos para ususarios no auteticados filtrados
//    List<Event> findByCategoriaId(String categoriaId);
    // ✅ Mantener este (no tiene referencias problemáticas)
    @Query("{'categoria.$id': ObjectId(?0)}")
    List<Event> findByCategoriaId(String categoriaId);

    @Query("{'categoria.nombreCategoria': ?0}")
    List<Event> findByCategoriaNombreCategoria(String nombreCategoria);

    // ========== EVENTOS POR FECHAS ==========

    @Query("{'start': {$gte: ?0}, 'end': {$lte: ?1}}")
    List<Event> findByStartGreaterThanEqualAndEndLessThanEqual(Instant startDate, Instant endDate);

    @Query("{'start': {$gte: ?0}}")
    List<Event> findByStartGreaterThanEqual(Instant now);

    @Query("{'end': {$lt: ?0}}")
    List<Event> findByEndLessThan(Instant now);

    // ========== EVENTOS PÚBLICOS (SIN AUTENTICACIÓN) ==========

//    @Query("{'bloqueado': false, 'status.nameState': ?0, 'privacy': 'public'}") //paara prueba eliminar si consultas publicas funciona
//    List<Event> findPublicEventsForUsers(String status);

    @Query("{'bloqueado': false, 'status.$id': ObjectId(?0), 'privacy': 'public'}")
    List<Event> findPublicEventsForUsers(String activeStateId);

    @Query("{'bloqueado': false,  'privacy': 'public', 'start': {$gte: ?1}}")
    List<Event> findUpcomingPublicEvents(String status, Instant now);

    @Query(value = "{'privacy': 'public', 'bloqueado': false, 'destacado': true}",
            fields = "{'id': 1, 'title': 1, 'description': 1, 'start': 1, 'end': 1, 'location': 1, 'categoria': 1, 'type': 1, 'ticketType': 1, 'price': 1, 'maxAttendees': 1, 'currentAttendees': 1, 'mainImages': 1, 'destacado': 1, 'status': 1}")
    List<Event> findFeaturedPublicEvents(String status);

    @Query("{'bloqueado': false, 'privacy': 'public', 'createdAt': {$exists: true}}")
    List<Event> findRecentPublicEvents(Pageable pageable);

    // ========== BÚSQUEDA DE TEXTO EN EVENTOS PÚBLICOS ==========

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

    @Query("{'$and': [" +
            "{'bloqueado': false}, " +
            "{'privacy': 'public'}, " +
            "{'status.$id': ObjectId(?1)}, " +
            "{'$or': [" +
            "  {'title': {$regex: ?0, $options: 'i'}}, " +
            "  {'description': {$regex: ?0, $options: 'i'}}, " +
            "  {'tags': {$regex: ?0, $options: 'i'}}" +
            "]}" +
            "]}")
    List<Event> searchPublicEventsByText(String searchText, String activeStateId);
    // ✅ CORREGIDO - Eventos públicos por categoría con estado
    @Query("{'bloqueado': false, 'privacy': 'public', 'status.$id': ObjectId(?1), 'categoria.$id': ObjectId(?0)}")
    List<Event> findPublicEventsByCategory(String categoriaId, String activeStateId);

    // ✅ NUEVO - Eventos públicos por tipo
    @Query("{'bloqueado': false, 'privacy': 'public', 'status.$id': ObjectId(?1), 'type': ?0}")
    List<Event> findPublicEventsByType(String type, String activeStateId);

    // ✅ NUEVO - Eventos públicos por ticket type
    @Query("{'bloqueado': false, 'privacy': 'public', 'status.$id': ObjectId(?1), 'ticketType': ?0}")
    List<Event> findPublicEventsByTicketType(String ticketType, String activeStateId);

    // ✅ NUEVO - Eventos públicos destacados
    @Query("{'bloqueado': false, 'privacy': 'public', 'status.$id': ObjectId(?0), 'destacado': true}")
    List<Event> findPublicFeaturedEvents(String activeStateId);




    // ========== EVENTOS PARA USUARIOS AUTENTICADOS ==========

    @Query("{'bloqueado': false, 'status.nameState': ?0}")
    List<Event> findActiveEventsForAuthenticatedUsers(String status);

    @Query("{'bloqueado': false, 'status.nameState': ?0, 'start': {$gte: ?1}}")
    List<Event> findUpcomingEventsForAuthenticatedUsers(String status, Instant now);

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

    // ========== EVENTOS POR PRIVACIDAD ==========

    @Query("{'privacy': ?0, 'status.nameState': {$regex: ?1, $options: 'i'}}")
    List<Event> findByPrivacyAndStatusNameStateIgnoreCase(String privacy, String status);

    @Query("{'_id': ?0, '$or': [" +
            "{'privacy': 'public'}, " +
            "{'$and': [{'privacy': 'private'}, {'creator.userName': ?1}]}, " +
            "{'$and': [{'privacy': 'private'}, {'invitedUsers': ?1}]}" +
            "]}")
    Event findAccessibleEventById(String eventId, String username);

    // ========== EVENTOS DESTACADOS ==========

    @Query("{'destacado': true, 'status.nameState': {$regex: ?0, $options: 'i'}}")
    List<Event> findByDestacadoTrueAndStatusNameStateIgnoreCase(String status);

    // ========== EVENTOS POR ESTADO DE BLOQUEO ==========

    @Query("{'bloqueado': ?0}")
    List<Event> findByBloqueado(boolean bloqueado);

    // ========== EVENTOS CON DISPONIBILIDAD ==========

    @Query("{'$expr': {'$lt': ['$currentAttendees', '$maxAttendees']}, 'permitirInscripciones': true, 'bloqueado': false}")
    List<Event> findEventsWithAvailability();

    // ========== EVENTOS RECIENTES ==========

    @Query(value = "{}", sort = "{'createdAt': -1}")
    List<Event> findTop10ByCreatedAtOrderByCreatedAtDesc(Pageable pageable);

    // ========== OTROS FILTROS ==========

    @Query("{'type': ?0}")
    List<Event> findByType(String type);

    @Query("{'ticketType': ?0}")
    List<Event> findByTicketType(String ticketType);

    // ========== BÚSQUEDA POR UBICACIÓN ==========

    @Query("{'location.address': {$regex: ?0, $options: 'i'}}")
    List<Event> findByLocationAddressContaining(String address);

    @Query("{'location.type': ?0}")
    List<Event> findByLocationType(String locationType);

    @Query("{'location': {$near: {$geometry: {type: 'Point', coordinates: [?0, ?1]}, $maxDistance: ?2}}}")
    List<Event> findEventsNear(Double longitude, Double latitude, Integer maxDistanceMeters);

    // ========== EVENTOS POR CAPACIDAD ==========

    @Query("{'maxAttendees': {$gt: ?0}}")
    List<Event> findByMaxAttendeesGreaterThan(Integer minAttendees);

    // ========== BÚSQUEDA POR ORGANIZADOR ==========

    @Query("{'otherData.organizer': {$regex: ?0, $options: 'i'}}")
    List<Event> findByOrganizerContaining(String organizer);

    // ========== BÚSQUEDA DE TEXTO GENERAL ==========

    @Query("{'$or': [{'title': {$regex: ?0, $options: 'i'}}, {'description': {$regex: ?0, $options: 'i'}}]}")
    List<Event> searchByTitleOrDescription(String searchText);

    // ========== CONTADORES ==========

//    @Query(value = "{'status.nameState': {$regex: ?0, $options: 'i'}}", count = true)
//    long countByStatusNameStateIgnoreCase(String nameState);

    @Query(value = "{'bloqueado': ?0}", count = true)
    long countByBloqueado(boolean bloqueado);

    @Query(value = "{'privacy': ?0, 'status.nameState': {$regex: ?1, $options: 'i'}}", count = true)
    long countByPrivacyAndStatusNameStateIgnoreCase(String privacy, String status);

    // Contadores por estado con ID
    @Query(value = "{'status.$id': ObjectId(?0)}", count = true)
    long countByStatusId(String statusId);

    @Query(value = "{'status.$id': ObjectId(?0), 'privacy': 'public'}", count = true)
    long countPublicEventsByStatusId(String statusId);

    @Query(value = "{'status.$id': ObjectId(?0), 'privacy': 'private'}", count = true)
    long countPrivateEventsByStatusId(String statusId);

    @Query(value = "{'status.$id': ObjectId(?0), 'destacado': true}", count = true)
    long countFeaturedEventsByStatusId(String statusId);

    @Query(value = "{'status.$id': ObjectId(?0), 'bloqueado': false}", count = true)
    long countActiveEventsByStatusId(String statusId);

    // Contadores adicionales útiles
    @Query(value = "{'status.$id': ObjectId(?0), 'privacy': 'public', 'bloqueado': false}", count = true)
    long countPublicActiveEventsByStatusId(String statusId);

    @Query(value = "{'status.$id': ObjectId(?0), 'type': ?1}", count = true)
    long countEventsByStatusAndType(String statusId, String type);

    @Query(value = "{'status.$id': ObjectId(?0), 'ticketType': ?1}", count = true)
    long countEventsByStatusAndTicketType(String statusId, String ticketType);
}


// ================================
// EventRepository CORREGIDO - Sin referencias anidadas problemáticas
// ================================
//
//package com.eventHub.backend_eventHub.events.repository;
//
//import com.eventHub.backend_eventHub.events.entities.Event;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.mongodb.repository.MongoRepository;
//import org.springframework.data.mongodb.repository.Query;
//import org.springframework.stereotype.Repository;
//
//import java.time.Instant;
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface EventRepository extends MongoRepository<Event, String> {
//
//    // ✅ CORREGIDO - Sin status.nameState, usar solo campos directos
//    @Query(value = "{'privacy': 'public', 'bloqueado': false}",
//            fields = "{'id': 1, 'title': 1, 'description': 1, 'start': 1, 'end': 1, 'location': 1, 'categoria': 1, 'type': 1, 'ticketType': 1, 'price': 1, 'maxAttendees': 1, 'currentAttendees': 1, 'mainImages': 1, 'destacado': 1, 'permitirInscripciones': 1, 'status': 1, 'privacy': 1}")
//    List<Event> findPublicEventsForUsers(String status);
//
//    // ✅ CORREGIDO - Eventos destacados sin referencia anidada
//    @Query(value = "{'privacy': 'public', 'bloqueado': false, 'destacado': true}",
//            fields = "{'id': 1, 'title': 1, 'description': 1, 'start': 1, 'end': 1, 'location': 1, 'categoria': 1, 'type': 1, 'ticketType': 1, 'price': 1, 'maxAttendees': 1, 'currentAttendees': 1, 'mainImages': 1, 'destacado': 1, 'status': 1}")
//    List<Event> findFeaturedPublicEvents(String status);
//
//    // ✅ CORREGIDO - Próximos eventos sin referencia anidada
//    @Query(value = "{'privacy': 'public', 'bloqueado': false, 'start': {$gte: ?1}}",
//            fields = "{'id': 1, 'title': 1, 'description': 1, 'start': 1, 'end': 1, 'location': 1, 'categoria': 1, 'type': 1, 'ticketType': 1, 'price': 1, 'maxAttendees': 1, 'currentAttendees': 1, 'mainImages': 1, 'destacado': 1, 'status': 1}")
//    List<Event> findUpcomingPublicEvents(String status, Instant fromDate);
//
//    // ✅ CORREGIDO - Eventos recientes sin referencia anidada
//    @Query(value = "{'privacy': 'public', 'bloqueado': false}",
//            fields = "{'id': 1, 'title': 1, 'description': 1, 'start': 1, 'end': 1, 'location': 1, 'categoria': 1, 'type': 1, 'ticketType': 1, 'price': 1, 'maxAttendees': 1, 'currentAttendees': 1, 'mainImages': 1, 'destacado': 1, 'status': 1}",
//            sort = "{'createdAt': -1}")
//    List<Event> findRecentPublicEvents(Pageable pageable);
//
//    // ✅ CORREGIDO - Búsqueda en eventos públicos
//    @Query(value = "{'privacy': 'public', 'bloqueado': false, '$or': [{'title': {$regex: ?0, $options: 'i'}}, {'description': {$regex: ?0, $options: 'i'}}]}",
//            fields = "{'id': 1, 'title': 1, 'description': 1, 'start': 1, 'end': 1, 'location': 1, 'categoria': 1, 'type': 1, 'ticketType': 1, 'price': 1, 'maxAttendees': 1, 'currentAttendees': 1, 'mainImages': 1, 'destacado': 1, 'status': 1}")
//    List<Event> searchPublicEventsByText(String searchText);
//
//    // ✅ CORREGIDO - Eventos por categoría (usa categoria.$id en lugar de referencias anidadas)
//    @Query(value = "{'categoria.$id': ?0}",
//            fields = "{'id': 1, 'title': 1, 'description': 1, 'start': 1, 'end': 1, 'location': 1, 'categoria': 1, 'privacy': 1, 'bloqueado': 1, 'status': 1}")
//    List<Event> findByCategoriaId(String categoriaId);
//
//    // ✅ CORREGIDO - Eventos creados por usuario (usa creator.$id)
//    @Query(value = "{'creator.userName': ?0}",
//            fields = "{'id': 1, 'title': 1, 'description': 1, 'start': 1, 'end': 1, 'location': 1, 'categoria': 1, 'privacy': 1, 'bloqueado': 1, 'status': 1, 'creator': 1, 'maxAttendees': 1, 'currentAttendees': 1, 'permitirInscripciones': 1}")
//    List<Event> findByCreatorUserName(String username);
//
//    // ✅ CORREGIDO - Evento accesible por ID (sin referencia anidada)
//    @Query(value = "{'_id': ?0, '$or': [{'privacy': 'public'}, {'creator.userName': ?1}, {'invitedUsers': ?1}]}",
//            fields = "{'creator': 1, 'categoria': 1, 'status': 1, 'id': 1, 'title': 1, 'description': 1, 'start': 1, 'end': 1, 'location': 1, 'privacy': 1, 'bloqueado': 1, 'invitedUsers': 1, 'maxAttendees': 1, 'currentAttendees': 1, 'permitirInscripciones': 1, 'fechaLimiteInscripcion': 1}")
//    Optional<Event> findAccessibleEventById(String eventId, String username);
//
//    // ✅ CORREGIDO - Eventos accesibles para usuario (sin referencia anidada)
//    @Query(value = "{'$or': [{'privacy': 'public', 'bloqueado': false}, {'creator.userName': ?0}, {'invitedUsers': ?0}]}",
//            fields = "{'id': 1, 'title': 1, 'description': 1, 'start': 1, 'end': 1, 'location': 1, 'categoria': 1, 'type': 1, 'ticketType': 1, 'price': 1, 'maxAttendees': 1, 'currentAttendees': 1, 'mainImages': 1, 'destacado': 1, 'privacy': 1, 'status': 1}")
//    List<Event> findAccessibleEventsForUser(String username, String status);
//
//    // ✅ NUEVO - Solo información básica del evento para validaciones
//    @Query(value = "{'_id': ?0}",
//            fields = "{'id': 1, 'privacy': 1, 'bloqueado': 1, 'status': 1, 'creator': 1, 'invitedUsers': 1, 'permitirInscripciones': 1, 'maxAttendees': 1, 'currentAttendees': 1, 'start': 1, 'fechaLimiteInscripcion': 1}")
//    Optional<Event> findEventForValidation(String eventId);
//}