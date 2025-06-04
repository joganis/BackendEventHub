// Nuevo: EventRoleRepository
package com.eventHub.backend_eventHub.events.repository;

import com.eventHub.backend_eventHub.events.entities.EventRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRoleRepository extends MongoRepository<EventRole, String> {

    // ========== SOLUCIÓN 1: Usar @Query explícitas ==========

    // Roles de un usuario (CORREGIDO con @Query)
    @Query("{'usuario.$id': ?0, 'activo': true}")
    List<EventRole> findByUsuarioIdAndActivoTrue(String usuarioId);

    // ALTERNATIVA: Usar el username con query explícita
    @Query("{'usuario.userName': ?0, 'activo': true}")
    List<EventRole> findByUsuarioUserNameAndActivoTrue(String userName);



    // Roles en un evento
    @Query("{'evento.$id': ?0, 'activo': true}")
    List<EventRole> findByEventoIdAndActivoTrue(String eventoId);

    // Roles de un usuario en un evento específico (CORREGIDO)
    @Query("{'usuario.userName': ?0, 'evento.$id': ?1, 'activo': true}")
    List<EventRole> findByUsuarioUserNameAndEventoIdAndActivoTrue(String userName, String eventoId);

    // Verificar rol específico (CORREGIDO)
    @Query("{'usuario.userName': ?0, 'evento.$id': ?1, 'rol': ?2, 'activo': true}")
    Optional<EventRole> findByUsuarioUserNameAndEventoIdAndRolAndActivoTrue(String userName, String eventoId, String rol);

    // Eventos donde el usuario es subcreador (CORREGIDO)
    @Query("{'usuario.userName': ?0, 'rol': ?1, 'activo': true}")
    List<EventRole> findByUsuarioUserNameAndRolAndActivoTrue(String userName, String rol);

    // Invitaciones por email (este está bien)
    List<EventRole> findByEmailInvitacionAndActivoTrue(String email);

    // Método adicional para verificar si un usuario tiene algún rol en un evento (CORREGIDO)
    @Query(value = "{'usuario.userName': ?0, 'evento.$id': ?1, 'activo': true}", exists = true)
    boolean existsByUsuarioUserNameAndEventoIdAndActivoTrue(String userName, String eventoId);

    // Método para obtener roles de un usuario en eventos específicos (CORREGIDO)
    @Query("{'usuario.userName': ?0, 'evento.$id': {$in: ?1}, 'activo': true}")
    List<EventRole> findByUsuarioUserNameAndEventoIdInAndActivoTrue(String userName, List<String> eventoIds);

    @Query("{'emailInvitacion': ?0, 'usuario': null, 'activo': true}")
    List<EventRole> findPendingInvitationsByEmail(String email);

    @Query("{'emailInvitacion': ?0}")
    List<EventRole> findAllInvitationsByEmail(String email);
}