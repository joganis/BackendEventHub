
// SubEventService
package com.eventHub.backend_eventHub.events.service;

import com.eventHub.backend_eventHub.events.dto.SubEventDto;
import com.eventHub.backend_eventHub.events.entities.*;
import com.eventHub.backend_eventHub.events.repository.*;
import com.eventHub.backend_eventHub.domain.entities.*;
import com.eventHub.backend_eventHub.domain.repositories.*;
import com.eventHub.backend_eventHub.users.repository.UserRepository;
import com.eventHub.backend_eventHub.domain.enums.StateList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubEventService {
    @Autowired private SubEventRepository subEventRepo;
    @Autowired private EventRepository eventRepo;
    @Autowired private EventRoleRepository eventRoleRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private StateRepository stateRepo;
    @Autowired private InscriptionRepository inscriptionRepo;

    /**
     * Crea un nuevo sub-evento
     */
    @Transactional
    public SubEvent createSubEvent(String username, SubEventDto dto) {
        // Verificar que el usuario puede crear sub-eventos para este evento principal
        if (!canUserManageEvent(username, dto.getEventoPrincipalId())) {
            throw new IllegalArgumentException("No tienes permisos para crear sub-eventos en este evento");
        }

        Users creator = userRepo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Event eventoPrincipal = eventRepo.findById(dto.getEventoPrincipalId())
                .orElseThrow(() -> new IllegalArgumentException("Evento principal no encontrado"));

        if (eventoPrincipal.isBloqueado()) {
            throw new IllegalArgumentException("No se pueden crear sub-eventos en un evento bloqueado");
        }

        State activeState = stateRepo.findByNameState(StateList.Active)
                .orElseThrow(() -> new IllegalArgumentException("Estado Active no encontrado"));

        // Validar fechas
        if (dto.getStart().isBefore(eventoPrincipal.getStart()) ||
                dto.getEnd().isAfter(eventoPrincipal.getEnd())) {
            throw new IllegalArgumentException("Las fechas del sub-evento deben estar dentro del rango del evento principal");
        }

        SubEvent subEvent = SubEvent.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .eventoPrincipal(eventoPrincipal)
                .location(new Location(
                        dto.getLocation().getAddress(),
                        dto.getLocation().getType(),
                        dto.getLocation().getLatitude(),
                        dto.getLocation().getLongitude()))
                .start(dto.getStart())
                .end(dto.getEnd())
                .type(dto.getType())
                .privacy(dto.getPrivacy())
                .ticketType(dto.getTicketType())
                .price(dto.getPrice() != null ?
                        new Price(dto.getPrice().getAmount(), dto.getPrice().getCurrency()) : null)
                .maxAttendees(dto.getMaxAttendees())
                .currentAttendees(0)
                .mainImages(dto.getMainImages() != null ?
                        mapMediaDtosToMedia(dto.getMainImages()) : new ArrayList<>())
                .otherData(dto.getOtherData() != null ?
                        new OtherData(
                                dto.getOtherData().getOrganizer(),
                                dto.getOtherData().getContact(),
                                dto.getOtherData().getNotes()) :
                        new OtherData(null, null, null))
                .status(activeState)
                .creator(creator)
                .history(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Inicializar historial
        HistoryRecord initialRecord = HistoryRecord.builder()
                .field("creation")
                .oldValue(null)
                .newValue("SubEvent created")
                .changedAt(Instant.now())
                .build();
        subEvent.getHistory().add(initialRecord);

        subEvent = subEventRepo.save(subEvent);

        // Agregar el ID del sub-evento al evento principal
        if (eventoPrincipal.getSubeventIds() == null) {
            eventoPrincipal.setSubeventIds(new ArrayList<>());
        }
        eventoPrincipal.getSubeventIds().add(subEvent.getId());
        eventRepo.save(eventoPrincipal);

        return subEvent;
    }

    /**
     * Lista sub-eventos de un evento principal
     */
    @Transactional(readOnly = true)
    public List<SubEvent> getSubEventsByMainEvent(String eventoPrincipalId) {
        try {
            // TEMPORAL: Obtener todos y filtrar manualmente
            List<SubEvent> allSubEvents = subEventRepo.findAll();

            return allSubEvents.stream()
                    .filter(subEvent ->
                            subEvent.getEventoPrincipal() != null &&
                                    subEvent.getEventoPrincipal().getId().equals(eventoPrincipalId) &&
                                    subEvent.getStatus() != null &&
                                    "Active".equalsIgnoreCase(subEvent.getStatus().getNameState().name())
                    )
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Error en getSubEventsByMainEvent: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene un sub-evento por ID
     */
    @Transactional(readOnly = true)
    public SubEvent getSubEventById(String id) {
        return subEventRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sub-evento no encontrado: " + id));
    }

    /**
     * Actualiza un sub-evento
     */
    @Transactional
    public SubEvent updateSubEvent(String id, String username, SubEventDto dto) {
        SubEvent subEvent = getSubEventById(id);

        if (!canUserManageEvent(username, subEvent.getEventoPrincipal().getId())) {
            throw new IllegalArgumentException("No tienes permisos para editar este sub-evento");
        }

        List<HistoryRecord> changes = new ArrayList<>();

        // Actualizar campos básicos
        if (dto.getTitle() != null && !dto.getTitle().equals(subEvent.getTitle())) {
            addHistoryRecord(changes, "title", subEvent.getTitle(), dto.getTitle());
            subEvent.setTitle(dto.getTitle());
        }

        if (dto.getDescription() != null && !dto.getDescription().equals(subEvent.getDescription())) {
            addHistoryRecord(changes, "description", subEvent.getDescription(), dto.getDescription());
            subEvent.setDescription(dto.getDescription());
        }

        if (dto.getStart() != null && !dto.getStart().equals(subEvent.getStart())) {
            // Validar que sigue dentro del rango del evento principal
            if (dto.getStart().isBefore(subEvent.getEventoPrincipal().getStart()) ||
                    dto.getStart().isAfter(subEvent.getEventoPrincipal().getEnd())) {
                throw new IllegalArgumentException("La fecha de inicio debe estar dentro del rango del evento principal");
            }
            addHistoryRecord(changes, "start", subEvent.getStart().toString(), dto.getStart().toString());
            subEvent.setStart(dto.getStart());
        }

        if (dto.getEnd() != null && !dto.getEnd().equals(subEvent.getEnd())) {
            // Validar que sigue dentro del rango del evento principal
            if (dto.getEnd().isBefore(subEvent.getEventoPrincipal().getStart()) ||
                    dto.getEnd().isAfter(subEvent.getEventoPrincipal().getEnd())) {
                throw new IllegalArgumentException("La fecha de fin debe estar dentro del rango del evento principal");
            }
            addHistoryRecord(changes, "end", subEvent.getEnd().toString(), dto.getEnd().toString());
            subEvent.setEnd(dto.getEnd());
        }

        if (dto.getMaxAttendees() != null && !dto.getMaxAttendees().equals(subEvent.getMaxAttendees())) {
            // Verificar que no sea menor que los asistentes actuales
            if (dto.getMaxAttendees() < subEvent.getCurrentAttendees()) {
                throw new IllegalArgumentException("El máximo de asistentes no puede ser menor a los inscritos actuales");
            }
            addHistoryRecord(changes, "maxAttendees",
                    subEvent.getMaxAttendees().toString(), dto.getMaxAttendees().toString());
            subEvent.setMaxAttendees(dto.getMaxAttendees());
        }

        if (dto.getLocation() != null) {
            Location newLocation = new Location(
                    dto.getLocation().getAddress(),
                    dto.getLocation().getType(),
                    dto.getLocation().getLatitude(),
                    dto.getLocation().getLongitude());
            addHistoryRecord(changes, "location", "Updated", dto.getLocation().getAddress());
            subEvent.setLocation(newLocation);
        }

        if (dto.getPrice() != null) {
            Price newPrice = new Price(dto.getPrice().getAmount(), dto.getPrice().getCurrency());
            String oldValue = subEvent.getPrice() != null ?
                    subEvent.getPrice().getAmount() + " " + subEvent.getPrice().getCurrency() : "none";
            addHistoryRecord(changes, "price", oldValue,
                    dto.getPrice().getAmount() + " " + dto.getPrice().getCurrency());
            subEvent.setPrice(newPrice);
        }

        if (dto.getMainImages() != null) {
            subEvent.setMainImages(mapMediaDtosToMedia(dto.getMainImages()));
            addHistoryRecord(changes, "mainImages", "Updated", "New images: " + dto.getMainImages().size());
        }

        if (dto.getOtherData() != null) {
            OtherData newData = new OtherData(
                    dto.getOtherData().getOrganizer(),
                    dto.getOtherData().getContact(),
                    dto.getOtherData().getNotes());
            addHistoryRecord(changes, "otherData", "Updated", "Data updated");
            subEvent.setOtherData(newData);
        }

        // Agregar cambios al historial
        if (!changes.isEmpty()) {
            if (subEvent.getHistory() == null) {
                subEvent.setHistory(new ArrayList<>());
            }
            subEvent.getHistory().addAll(changes);
        }

        subEvent.setUpdatedAt(Instant.now());
        return subEventRepo.save(subEvent);
    }

    /**
     * Elimina un sub-evento
     */
    @Transactional
    public void deleteSubEvent(String id, String username) {
        SubEvent subEvent = getSubEventById(id);

        if (!canUserManageEvent(username, subEvent.getEventoPrincipal().getId())) {
            throw new IllegalArgumentException("No tienes permisos para eliminar este sub-evento");
        }

        // Cancelar inscripciones activas al sub-evento
        List<Inscription> inscriptions = inscriptionRepo.findBySubeventoIdAndEstado(id, "confirmada");
        inscriptions.forEach(inscription -> {
            inscription.setEstado("cancelada");
            inscriptionRepo.save(inscription);
        });

        // Remover el ID del sub-evento del evento principal
        Event eventoPrincipal = subEvent.getEventoPrincipal();
        if (eventoPrincipal.getSubeventIds() != null) {
            eventoPrincipal.getSubeventIds().remove(id);
            eventRepo.save(eventoPrincipal);
        }

        subEventRepo.delete(subEvent);
    }

    /**
     * Lista sub-eventos creados por un usuario
     */
    @Transactional(readOnly = true)
    public List<SubEvent> getSubEventsByCreator(String username) {
        return subEventRepo.findByCreatorUserName(username);
    }

    /**
     * Cambia el estado de un sub-evento
     */
    @Transactional
    public SubEvent changeSubEventStatus(String id, String newStatus, String username) {
        SubEvent subEvent = getSubEventById(id);

        if (!canUserManageEvent(username, subEvent.getEventoPrincipal().getId())) {
            throw new IllegalArgumentException("No tienes permisos para cambiar el estado de este sub-evento");
        }

        try {
            com.eventHub.backend_eventHub.domain.enums.StateList stateEnum =
                    com.eventHub.backend_eventHub.domain.enums.StateList.valueOf(newStatus);

            State newState = stateRepo.findByNameState(stateEnum)
                    .orElseThrow(() -> new IllegalArgumentException("Estado no encontrado: " + newStatus));

            String oldStatus = subEvent.getStatus() != null ?
                    subEvent.getStatus().getNameState().name() : "null";

            subEvent.setStatus(newState);

            // Agregar al historial
            if (subEvent.getHistory() == null) {
                subEvent.setHistory(new ArrayList<>());
            }

            addHistoryRecord(subEvent.getHistory(), "status", oldStatus, newStatus);
            subEvent.setUpdatedAt(Instant.now());

            return subEventRepo.save(subEvent);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado inválido: " + newStatus +
                    ". Valores permitidos: " + java.util.Arrays.toString(
                    com.eventHub.backend_eventHub.domain.enums.StateList.values()));
        }
    }

    // ================ MÉTODOS DE UTILIDAD ================
    private boolean canUserManageEvent(String username, String eventoId) {
        try {
            System.out.println("🔍 Verificando permisos para: " + username + " en evento: " + eventoId);

            // Buscar directamente todos los roles del usuario
            Users user = userRepo.findByUserName(username).orElse(null);
            if (user == null) {
                System.out.println("Usuario no encontrado");
                return false;
            }

            // Buscar roles usando una consulta que SÍ funciona
            List<EventRole> allRoles = eventRoleRepo.findAll(); // Temporal - obtener todos

            // Filtrar manualmente
            boolean hasPermission = allRoles.stream()
                    .anyMatch(role ->
                            role.getUsuario() != null &&
                                    role.getUsuario().getId().equals(user.getId()) &&
                                    role.getEvento() != null &&
                                    role.getEvento().getId().equals(eventoId) &&
                                    role.isActivo() &&
                                    (role.getRol().equals("CREADOR") || role.getRol().equals("SUBCREADOR"))
                    );

            System.out.println("Permisos encontrados: " + hasPermission);
            return hasPermission;

        } catch (Exception e) {
            System.err.println(" Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }



    private List<Media> mapMediaDtosToMedia(List<com.eventHub.backend_eventHub.events.dto.MediaDto> dtos) {
        if (dtos == null) return new ArrayList<>();
        return dtos.stream()
                .map(m -> new Media(m.getUrl(), m.getDescription(), m.getUploadedAt(), m.getMediaType()))
                .collect(java.util.stream.Collectors.toList());
    }

    private void addHistoryRecord(List<HistoryRecord> history, String field, String oldValue, String newValue) {
        history.add(HistoryRecord.builder()
                .field(field)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedAt(Instant.now())
                .build());
    }
}