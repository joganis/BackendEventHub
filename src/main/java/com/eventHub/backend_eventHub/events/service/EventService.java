
package com.eventHub.backend_eventHub.events.service;

import com.eventHub.backend_eventHub.events.dto.*;

import com.eventHub.backend_eventHub.events.entities.*;
import com.eventHub.backend_eventHub.events.repository.EventRepository;
import com.eventHub.backend_eventHub.domain.entities.State;
import com.eventHub.backend_eventHub.users.repository.UserRepository;
import com.eventHub.backend_eventHub.domain.entities.Users;
import com.eventHub.backend_eventHub.domain.repositories.StateRepository;
import com.eventHub.backend_eventHub.domain.enums.StateList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EventService {
    @Autowired private EventRepository eventRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private StateRepository stateRepo;

    /**
     * Lista todos los eventos o filtra por estado
     * @param status Estado por el cual filtrar (opcional)
     * @return Lista de eventos
     */
    @Transactional(readOnly = true)
    public List<Event> listAll(String status) {
        return (status == null)
                ? eventRepo.findAll()
                : eventRepo.findByStatusNameStateIgnoreCase(status);
    }

    /**
     * Obtiene un evento por su ID
     * @param id ID del evento
     * @return Evento encontrado
     * @throws IllegalArgumentException si no existe el evento
     */
    @Transactional(readOnly = true)
    public Event getById(String id) {
        return eventRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado: " + id));
    }

    /**
     * Crea un nuevo evento
     * @param username Usuario creador
     * @param dto Datos del evento
     * @return Evento creado
     */
    @Transactional
    public Event create(String username, EventDto dto) {
        // Verificar usuario
        Users creator = userRepo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe: " + username));

        // Obtener estado "Active"
        State activeState = stateRepo.findByNameState(StateList.Active)
                .orElseThrow(() -> new IllegalArgumentException("Estado Active no encontrado"));

        // Construir el evento
        Event ev = mapDtoToEvent(dto, creator, activeState);

        // Inicializar el historial
        ev.setHistory(new ArrayList<>());
        HistoryRecord initialRecord = HistoryRecord.builder()
                .field("creation")
                .oldValue(null)
                .newValue("Event created")
                .changedAt(Instant.now())
                .build();
        ev.getHistory().add(initialRecord);

        // Guardar y devolver
        return eventRepo.save(ev);
    }

    /**
     * Actualiza un evento existente
     * @param id ID del evento
     * @param dto Datos a actualizar
     * @return Evento actualizado
     */
    @Transactional
    public Event update(String id, UpdateEventDto dto) {
        Event ev = getById(id);
        List<HistoryRecord> changes = new ArrayList<>();

        // Actualizar datos básicos
        if (dto.getTitle() != null && !dto.getTitle().equals(ev.getTitle())) {
            String oldValue = ev.getTitle();
            ev.setTitle(dto.getTitle());
            addHistoryRecord(changes, "title", oldValue, dto.getTitle());
        }

        if (dto.getDescription() != null && !dto.getDescription().equals(ev.getDescription())) {
            String oldValue = ev.getDescription();
            ev.setDescription(dto.getDescription());
            addHistoryRecord(changes, "description", oldValue, dto.getDescription());
        }

        if (dto.getStart() != null && !dto.getStart().equals(ev.getStart())) {
            String oldValue = ev.getStart().toString();
            ev.setStart(dto.getStart());
            addHistoryRecord(changes, "start", oldValue, dto.getStart().toString());
        }

        if (dto.getEnd() != null && !dto.getEnd().equals(ev.getEnd())) {
            String oldValue = ev.getEnd().toString();
            ev.setEnd(dto.getEnd());
            addHistoryRecord(changes, "end", oldValue, dto.getEnd().toString());
        }

        if (dto.getPrice() != null) {
            String oldValue = ev.getPrice() != null ?
                    ev.getPrice().getAmount() + " " + ev.getPrice().getCurrency() : "none";
            ev.setPrice(new Price(dto.getPrice().getAmount(), dto.getPrice().getCurrency()));
            addHistoryRecord(changes, "price", oldValue,
                    dto.getPrice().getAmount() + " " + dto.getPrice().getCurrency());
        }

        if (dto.getMaxAttendees() != null && !dto.getMaxAttendees().equals(ev.getMaxAttendees())) {
            String oldValue = ev.getMaxAttendees() != null ? ev.getMaxAttendees().toString() : "none";
            ev.setMaxAttendees(dto.getMaxAttendees());
            addHistoryRecord(changes, "maxAttendees", oldValue, dto.getMaxAttendees().toString());
        }

        if (dto.getCategories() != null) {
            String oldValue = ev.getCategories() != null ? String.join(",", ev.getCategories()) : "none";
            ev.setCategories(dto.getCategories());
            addHistoryRecord(changes, "categories", oldValue, String.join(",", dto.getCategories()));
        }

        // Actualizar medios y contenido multimedia
        updateMediaCollection(ev, dto, changes);

        // Actualizar otros datos
        if (dto.getOtherData() != null) {
            OtherData oldData = ev.getOtherData();
            OtherData newData = new OtherData(
                    dto.getOtherData().getOrganizer(),
                    dto.getOtherData().getContact(),
                    dto.getOtherData().getNotes());

            ev.setOtherData(newData);

            if (!newData.getOrganizer().equals(oldData.getOrganizer())) {
                addHistoryRecord(changes, "organizer", oldData.getOrganizer(), newData.getOrganizer());
            }

            if (!newData.getContact().equals(oldData.getContact())) {
                addHistoryRecord(changes, "contact", oldData.getContact(), newData.getContact());
            }

            if (!newData.getNotes().equals(oldData.getNotes())) {
                addHistoryRecord(changes, "notes", oldData.getNotes(), newData.getNotes());
            }
        }

        // Añadir cambios al historial si hay alguno
        if (!changes.isEmpty()) {
            if (ev.getHistory() == null) {
                ev.setHistory(new ArrayList<>());
            }
            ev.getHistory().addAll(changes);
        }

        return eventRepo.save(ev);
    }

    /**
     * Cambia el estado de un evento
     * @param id ID del evento
     * @param statusName Nuevo estado
     * @return Evento actualizado
     */
    @Transactional
    public Event changeStatus(String id, String statusName) {
        Event ev = getById(id);

        try {
            StateList stateEnum = StateList.valueOf(statusName);
            State newState = stateRepo.findByNameState(stateEnum)
                    .orElseThrow(() -> new IllegalArgumentException("Estado no encontrado en la base de datos: " + statusName));

            // Registrar cambio en historial
            String oldStatus = ev.getStatus() != null ? ev.getStatus().getNameState().name() : "null";

            ev.setStatus(newState);

            // Añadir entrada al historial
            if (ev.getHistory() == null) {
                ev.setHistory(new ArrayList<>());
            }

            HistoryRecord record = HistoryRecord.builder()
                    .field("status")
                    .oldValue(oldStatus)
                    .newValue(statusName)
                    .changedAt(Instant.now())
                    .build();

            ev.getHistory().add(record);

            return eventRepo.save(ev);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado inválido: " + statusName +
                    ". Valores permitidos: " + java.util.Arrays.toString(StateList.values()));
        }
    }

    /**
     * Comprueba si un usuario es el creador o administrador de un evento
     * @param eventId ID del evento
     * @param username Nombre de usuario
     * @return true si es creador o admin
     */
    @Transactional(readOnly = true)
    public boolean isCreatorOrAdmin(String eventId, String username) {
        Event event = getById(eventId);
        return event.getCreator().getUserName().equals(username);
    }

    /**
     * Mapea un DTO a una entidad Event
     */
    private Event mapDtoToEvent(EventDto dto, Users creator, State status) {
        return Event.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
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
                .categories(dto.getCategories())
                .mainImages(mapMediaDtosToMedia(dto.getMainImages()))
                .galleryImages(mapMediaDtosToMedia(dto.getGalleryImages()))
                .videos(mapMediaDtosToMedia(dto.getVideos()))
                .documents(mapMediaDtosToMedia(dto.getDocuments()))
                .otherData(dto.getOtherData() != null ?
                        new OtherData(
                                dto.getOtherData().getOrganizer(),
                                dto.getOtherData().getContact(),
                                dto.getOtherData().getNotes()) :
                        new OtherData(null, null, null))
                .subeventIds(dto.getSubeventIds())
                .creator(creator)
                .status(status)
                .build();
    }

    /**
     * Convierte una lista de MediaDto a Media
     */
    private List<Media> mapMediaDtosToMedia(List<MediaDto> dtos) {
        if (dtos == null) return new ArrayList<>();
        return dtos.stream()
                .map(m -> new Media(m.getUrl(), m.getDescription(), m.getUploadedAt(), m.getMediaType()))
                .collect(Collectors.toList());
    }

    /**
     * Añade un registro al historial
     */
    private void addHistoryRecord(List<HistoryRecord> records, String field, String oldValue, String newValue) {
        records.add(HistoryRecord.builder()
                .field(field)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedAt(Instant.now())
                .build());
    }

    /**
     * Actualiza colecciones de media
     */
    private void updateMediaCollection(Event ev, UpdateEventDto dto, List<HistoryRecord> changes) {
        if (dto.getMainImages() != null) {
            String oldValue = "Main images updated";
            ev.setMainImages(mapMediaDtosToMedia(dto.getMainImages()));
            addHistoryRecord(changes, "mainImages", oldValue, "New images: " + dto.getMainImages().size());
        }

        if (dto.getGalleryImages() != null) {
            String oldValue = "Gallery images updated";
            ev.setGalleryImages(mapMediaDtosToMedia(dto.getGalleryImages()));
            addHistoryRecord(changes, "galleryImages", oldValue, "New images: " + dto.getGalleryImages().size());
        }

        if (dto.getVideos() != null) {
            String oldValue = "Videos updated";
            ev.setVideos(mapMediaDtosToMedia(dto.getVideos()));
            addHistoryRecord(changes, "videos", oldValue, "New videos: " + dto.getVideos().size());
        }

        if (dto.getDocuments() != null) {
            String oldValue = "Documents updated";
            ev.setDocuments(mapMediaDtosToMedia(dto.getDocuments()));
            addHistoryRecord(changes, "documents", oldValue, "New documents: " + dto.getDocuments().size());
        }
    }
}