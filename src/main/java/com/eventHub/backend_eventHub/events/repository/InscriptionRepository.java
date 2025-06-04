// Nuevo: InscriptionRepository
package com.eventHub.backend_eventHub.events.repository;

import com.eventHub.backend_eventHub.events.entities.Inscription;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InscriptionRepository extends MongoRepository<Inscription, String> {
    // ========== SOLUCIÓN: Usar @Query explícitas para evitar el problema ==========

    // ========== INSCRIPCIONES POR USUARIO ==========
    @Query("{'usuario.userName': ?0}")
    List<Inscription> findByUsuarioUserName(String userName);

    @Query("{'usuario.$id': ?0}")
    List<Inscription> findByUsuarioId(String usuarioId);

    @Query("{'usuario.userName': ?0, 'estado': ?1}")
    List<Inscription> findByUsuarioUserNameAndEstado(String userName, String estado);

    @Query("{'usuario.$id': ?0, 'estado': ?1}")
    List<Inscription> findByUsuarioIdAndEstado(String usuarioId, String estado);

    // ========== INSCRIPCIONES POR EVENTO ==========

    @Query("{'evento.$id': ?0}")
    List<Inscription> findByEventoId(String eventoId);

    @Query("{'evento.$id': ?0, 'estado': ?1}")
    List<Inscription> findByEventoIdAndEstado(String eventoId, String estado);

    // ========== VERIFICAR INSCRIPCIÓN ESPECÍFICA ==========

    @Query("{'usuario.userName': ?0, 'evento.$id': ?1, 'estado': ?2}")
    Optional<Inscription> findByUsuarioUserNameAndEventoIdAndEstado(String userName, String eventoId, String estado);

    @Query("{'usuario.$id': ?0, 'evento.$id': ?1, 'estado': ?2}")
    Optional<Inscription> findByUsuarioIdAndEventoIdAndEstado(String usuarioId, String eventoId, String estado);

    // ========== INSCRIPCIONES A SUB-EVENTOS ==========

    @Query("{'subeventoId': ?0}")
    List<Inscription> findBySubeventoId(String subeventoId);

    @Query("{'subeventoId': ?0, 'estado': ?1}")
    List<Inscription> findBySubeventoIdAndEstado(String subeventoId, String estado);

    @Query("{'usuario.userName': ?0, 'subeventoId': ?1, 'estado': ?2}")
    Optional<Inscription> findByUsuarioUserNameAndSubeventoIdAndEstado(String userName, String subeventoId, String estado);

    @Query("{'usuario.$id': ?0, 'subeventoId': ?1, 'estado': ?2}")
    Optional<Inscription> findByUsuarioIdAndSubeventoIdAndEstado(String usuarioId, String subeventoId, String estado);

    // ========== CONTADORES ==========

    @Query(value = "{'evento.$id': ?0, 'estado': ?1}", count = true)
    long countByEventoIdAndEstado(String eventoId, String estado);

    @Query(value = "{'subeventoId': ?0, 'estado': ?1}", count = true)
    long countBySubeventoIdAndEstado(String subeventoId, String estado);

    // ========== BÚSQUEDAS POR TIPO DE INSCRIPCIÓN ==========

    @Query("{'usuario.userName': ?0, 'tipoInscripcion': ?1, 'estado': ?2}")
    List<Inscription> findByUsuarioUserNameAndTipoInscripcionAndEstado(String userName, String tipoInscripcion, String estado);

    @Query("{'usuario.$id': ?0, 'tipoInscripcion': ?1, 'estado': ?2}")
    List<Inscription> findByUsuarioIdAndTipoInscripcionAndEstado(String usuarioId, String tipoInscripcion, String estado);

    // ========== TODAS LAS INSCRIPCIONES DE UN USUARIO ==========

    @Query("{'usuario.userName': ?0, 'estado': ?1}")
    List<Inscription> findAllByUsuarioUserNameAndEstado(String userName, String estado);

    @Query("{'usuario.$id': ?0, 'estado': ?1}")
    List<Inscription> findAllByUsuarioIdAndEstado(String usuarioId, String estado);
}