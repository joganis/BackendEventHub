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

    // Inscripciones de un usuario (CORREGIDO)
    @Query("{'usuario.userName': ?0}")
    List<Inscription> findByUsuarioUserName(String userName);

    @Query("{'usuario.userName': ?0, 'estado': ?1}")
    List<Inscription> findByUsuarioUserNameAndEstado(String userName, String estado);

    // Inscripciones de un evento (estos están bien - no usan usuario)
    List<Inscription> findByEventoId(String eventoId);
    List<Inscription> findByEventoIdAndEstado(String eventoId, String estado);

    // Verificar si usuario ya está inscrito (CORREGIDO)
    @Query("{'usuario.userName': ?0, 'evento.$id': ?1, 'estado': ?2}")
    Optional<Inscription> findByUsuarioUserNameAndEventoIdAndEstado(String userName, String eventoId, String estado);

    // Inscripciones a subeventos (estos están bien)
    List<Inscription> findBySubeventoId(String subeventoId);
    List<Inscription> findBySubeventoIdAndEstado(String subeventoId, String estado);

    @Query("{'usuario.userName': ?0, 'subeventoId': ?1, 'estado': ?2}")
    Optional<Inscription> findByUsuarioUserNameAndSubeventoIdAndEstado(String userName, String subeventoId, String estado);

    // Contar inscripciones activas (estos están bien)
    long countByEventoIdAndEstado(String eventoId, String estado);
    long countBySubeventoIdAndEstado(String subeventoId, String estado);

    // Métodos adicionales para consultas más complejas (YA ESTABAN BIEN)
    @Query("{'usuario.userName': ?0, 'tipoInscripcion': ?1, 'estado': ?2}")
    List<Inscription> findByUsuarioUserNameAndTipoInscripcionAndEstado(String userName, String tipoInscripcion, String estado);

    // Todas las inscripciones de un usuario (eventos y subeventos) (YA ESTABA BIEN)
    @Query("{'usuario.userName': ?0, 'estado': ?1}")
    List<Inscription> findAllByUsuarioUserNameAndEstado(String userName, String estado);
}