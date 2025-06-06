
// ================================
// 1. EventRoleRepository DEFINITIVO
// ================================

package com.eventHub.backend_eventHub.events.repository;

import com.eventHub.backend_eventHub.events.entities.EventRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRoleRepository extends MongoRepository<EventRole, String> {

    // ========== BÚSQUEDAS POR USUARIO ==========

    @Query("{'usuario.$id': ObjectId(?0), 'activo': true}")
    List<EventRole> findByUsuarioIdAndActivoTrue(String usuarioId);

    @Query("{'usuario.userName': ObjectId(?0), 'activo': true}")
    List<EventRole> findByUsuarioUserNameAndActivoTrue(String userName);

    // ========== BÚSQUEDAS POR EVENTO ==========


    @Query("{'evento.$id': ?0, 'activo': true}")
    List<EventRole> findByEventoIdAndActivoTrue(String eventoId);

//    @Query("{'evento.$id': ObjectId(?0), 'activo': true}")    //usar este so falla la referencia de arriba
//    List<EventRole> findByEventoIdAndActivoTrue(String eventoId);

    // ========== BÚSQUEDAS COMBINADAS USUARIO + EVENTO ==========

    @Query("{'usuario.$id': ObjectId(?0), 'evento.$id': ?1, 'activo': true}")      //  antas=> usuario.$id': ?0, ahora =>ObjectId(?0)
    List<EventRole> findByUsuarioIdAndEventoIdAndActivoTrue(String usuarioId, String eventoId);

    @Query("{'usuario.userName': ?0, 'evento.$id': ?1, 'activo': true}")
    List<EventRole> findByUsuarioUserNameAndEventoIdAndActivoTrue(String userName, String eventoId);

    // ========== BÚSQUEDAS POR ROL ==========

    @Query("{'usuario.userName': ?0, 'rol': ?1, 'activo': true}")
    List<EventRole> findByUsuarioUserNameAndRolAndActivoTrue(String userName, String rol);

    @Query("{'usuario.$id': ObjectId(?0), 'rol': ?1, 'activo': true}")
    List<EventRole> findByUsuarioIdAndRolAndActivoTrue(String usuarioId, String rol);

    // ========== BÚSQUEDAS ESPECÍFICAS CON ROL ==========

    @Query("{'usuario.userName': ?0, 'evento.$id': ?1, 'rol': ?2, 'activo': true}")
    Optional<EventRole> findByUsuarioUserNameAndEventoIdAndRolAndActivoTrue(String userName, String eventoId, String rol);

//    @Query("{'usuario.$id': ?0, 'evento.$id': ?1, 'rol': ?2, 'activo': true}")
//    Optional<EventRole> findByUsuarioIdAndEventoIdAndRolAndActivoTrue(String usuarioId, String eventoId, String rol);

    @Query("{'usuario.$id': ObjectId(?0), 'evento.$id': ObjectId(?1), 'rol': ?2, 'activo': true}")
    Optional<EventRole> findByUsuarioIdAndEventoIdAndRolAndActivoTrue(String usuarioId, String eventoId, String rol);


    // ========== INVITACIONES POR EMAIL ==========

    @Query("{'emailInvitacion': ?0, 'activo': true}")
    List<EventRole> findByEmailInvitacionAndActivoTrue(String email);

    @Query("{'emailInvitacion': ?0, 'usuario': null, 'activo': true}")
    List<EventRole> findPendingInvitationsByEmail(String email);

    @Query("{'emailInvitacion': ?0}")
    List<EventRole> findAllInvitationsByEmail(String email);

    // ========== VERIFICACIONES DE EXISTENCIA ==========

    @Query(value = "{'usuario.userName': ?0, 'evento.$id': ?1, 'activo': true}", exists = true)
    boolean existsByUsuarioUserNameAndEventoIdAndActivoTrue(String userName, String eventoId);

    @Query(value = "{'usuario.$id': ?0, 'evento.$id': ?1, 'activo': true}", exists = true)
    boolean existsByUsuarioIdAndEventoIdAndActivoTrue(String usuarioId, String eventoId);

    // ========== BÚSQUEDAS EN MÚLTIPLES EVENTOS ==========

    @Query("{'usuario.userName': ?0, 'evento.$id': {$in: ?1}, 'activo': true}")
    List<EventRole> findByUsuarioUserNameAndEventoIdInAndActivoTrue(String userName, List<String> eventoIds);

    @Query("{'usuario.$id': ?0, 'evento.$id': {$in: ?1}, 'activo': true}")
    List<EventRole> findByUsuarioIdAndEventoIdInAndActivoTrue(String usuarioId, List<String> eventoIds);

}