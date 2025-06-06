
// EventService actualizado
package com.eventHub.backend_eventHub.events.service;

import com.eventHub.backend_eventHub.events.dto.*;
import com.eventHub.backend_eventHub.events.entities.*;
import com.eventHub.backend_eventHub.events.repository.*;
import com.eventHub.backend_eventHub.domain.entities.*;
import com.eventHub.backend_eventHub.domain.repositories.*;
import com.eventHub.backend_eventHub.users.repository.UserRepository;
import com.eventHub.backend_eventHub.domain.enums.StateList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class EventService {
    @Autowired private EventRepository eventRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private StateRepository stateRepo;
    @Autowired private CategoryRepository categoryRepo;
    @Autowired private InscriptionRepository inscriptionRepo;
    @Autowired private EventRoleRepository eventRoleRepo;
    @Autowired private SubEventRepository subEventRepo;
    @Autowired private InvitationService invitationService;
    @Autowired private AttendeeInvitationRepository attendeeInvitationRepo;

    /**
     * Lista eventos públicos y no bloqueados para usuarios NO AUTENTICADOS
     */
    @Transactional(readOnly = true)
    public List<EventSummaryDto> listPublicEvents(EventFilterDto filter) {
        List<Event> events;

        if (filter == null || isFilterEmpty(filter)) {
            events = eventRepo.findPublicEventsForUsers("Active");
        } else {
            events = applyPublicFilters(filter);
        }

        return events.stream()
                .map(this::mapToSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Lista eventos accesibles para usuario AUTENTICADO (públicos + privados donde tiene acceso)
     */
    @Transactional(readOnly = true)
    public List<EventSummaryDto> listAccessibleEvents(String username, EventFilterDto filter) {
        List<Event> events;

        if (filter == null || isFilterEmpty(filter)) {
            events = eventRepo.findAccessibleEventsForUser(username, "Active");
        } else {
            events = applyAuthenticatedFilters(username, filter);
        }

        return events.stream()
                .map(this::mapToSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Lista eventos destacados PÚBLICOS para promoción
     */
    @Transactional(readOnly = true)
    public List<EventSummaryDto> listFeaturedEvents() {
        List<Event> events = eventRepo.findFeaturedPublicEvents("Active");
        return events.stream()
                .filter(event -> isEventActive(event)) // Filtrar por estado activo
                .map(this::mapToSummaryDto)
                .collect(Collectors.toList());
    }

    private boolean isEventActive(Event event) {
        return event.getStatus() != null &&
                "Active".equals(event.getStatus().getNameState().name());
    }

    /**
     * Lista próximos eventos PÚBLICOS
     */
    @Transactional(readOnly = true)
    public List<EventSummaryDto> listUpcomingEvents() {
        List<Event> events = eventRepo.findUpcomingPublicEvents("Active", Instant.now());
        return events.stream()
                .map(this::mapToSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Lista eventos recientes PÚBLICOS
     */
    @Transactional(readOnly = true)
    public List<EventSummaryDto> listRecentEvents() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Event> events = eventRepo.findRecentPublicEvents(pageable);
        return events.stream()
                .map(this::mapToSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene detalle completo de un evento (respeta privacidad)
     */
    @Transactional(readOnly = true)
    public Event getEventDetails(String id) {
        return getEventDetails(id, null); // Para usuarios no autenticados
    }

    /**
     *  MÉTODO ACTUALIZADO - Obtiene detalle completo de un evento para usuario autenticado
     * Ahora incluye validación de acceso a eventos privados
     */
    @Transactional(readOnly = true)
    public Event getEventDetails(String id, String username) {
        Event event = eventRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        if (username != null) {
            //  Usuario autenticado - verificar acceso según privacidad
            if ("public".equals(event.getPrivacy())) {
                // Evento público - verificar que no esté bloqueado
                if (event.isBloqueado()) {
                    throw new IllegalArgumentException("Este evento no está disponible");
                }
            } else {
                // Evento privado - verificar permisos de acceso usando nuevo método
                if (!hasUserAccessToPrivateEvent(username, event)) {
                    throw new IllegalArgumentException("No tienes acceso a este evento privado");
                }
            }
        } else {
            // Usuario no autenticado - solo eventos públicos
            if (!"public".equals(event.getPrivacy())) {
                throw new IllegalArgumentException("Este evento es privado");
            }

            if (event.isBloqueado()) {
                throw new IllegalArgumentException("Este evento no está disponible");
            }
        }

        //  Verificar que el estado sea activo (común para ambos casos)
        if (event.getStatus() == null || !"Active".equals(event.getStatus().getNameState().name())) {
            throw new IllegalArgumentException("Este evento no está activo");
        }

        // Cargar subeventos
        List<SubEvent> subEvents = subEventRepo.findByEventoPrincipalId(id);

        return event;
    }

    /**
     * Crea un nuevo evento
     */
    @Transactional
    public Event createEvent(String username, EventDto dto) {
        Users creator = userRepo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no existe: " + username));

        State activeState = stateRepo.findByNameState(StateList.Active)
                .orElseThrow(() -> new IllegalArgumentException("Estado Active no encontrado"));

        Category category = categoryRepo.findById(dto.getCategoriaId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        Event event = mapDtoToEvent(dto, creator, activeState, category);
        event.setCreatedAt(Instant.now());
        event.setUpdatedAt(Instant.now());

        // Inicializar historial
        event.setHistory(new ArrayList<>());
        addHistoryRecord(event.getHistory(), "creation", null, "Event created");

        event = eventRepo.save(event);

        // Crear rol de CREADOR
        EventRole creatorRole = EventRole.builder()
                .usuario(creator)
                .evento(event)
                .rol("CREADOR")
                .fechaAsignacion(Instant.now())
                .activo(true)
                .build();
        eventRoleRepo.save(creatorRole);

        return event;
    }

    /**
     * Lista eventos creados por un usuario
     */
    @Transactional(readOnly = true)
    public List<Event> listMyCreatedEvents(String username) {
        // Buscar el usuario por username
        Users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));

        // Usar el método que funciona: findByCreatorId
        return eventRepo.findByCreatorId(user.getId());
    }

    /**
     * Lista eventos donde el usuario es subcreador
     */
    @Transactional(readOnly = true)
    public List<Event> listEventsAsSubcreator(String username) {
        List<EventRole> roles = eventRoleRepo.findByUsuarioUserNameAndRolAndActivoTrue(username, "SUBCREADOR");
        return roles.stream()
                .map(role -> role.getEvento())
                .collect(Collectors.toList());
    }

    /**
     * Actualiza un evento (solo creador o subcreador)
     */
    @Transactional
    public Event updateEvent(String eventId, String username, UpdateEventDto dto) {
        Event event = getById(eventId);

        if (!canUserEditEvent(username, eventId)) {
            throw new IllegalArgumentException("No tienes permisos para editar este evento");
        }

        List<HistoryRecord> changes = new ArrayList<>();

        // Aplicar cambios y registrar historial
        updateEventFields(event, dto, changes);

        if (!changes.isEmpty()) {
            if (event.getHistory() == null) {
                event.setHistory(new ArrayList<>());
            }
            event.getHistory().addAll(changes);
        }

        event.setUpdatedAt(Instant.now());
        return eventRepo.save(event);
    }

    /**
     * Elimina un evento (solo creador)
     */
    @Transactional
    public void deleteEvent(String eventId, String username) {
        Event event = getById(eventId);

        if (!isEventCreator(username, eventId)) {
            throw new IllegalArgumentException("Solo el creador puede eliminar este evento");
        }

        // Cancelar inscripciones activas
        List<Inscription> inscriptions = inscriptionRepo.findByEventoIdAndEstado(eventId, "confirmada");
        inscriptions.forEach(inscription -> {
            inscription.setEstado("cancelada");
            inscriptionRepo.save(inscription);
        });

        // Eliminar subeventos
        List<SubEvent> subEvents = subEventRepo.findByEventoPrincipalId(eventId);
        subEventRepo.deleteAll(subEvents);

        // Desactivar roles
        List<EventRole> roles = eventRoleRepo.findByEventoIdAndActivoTrue(eventId);
        roles.forEach(role -> {
            role.setActivo(false);
            eventRoleRepo.save(role);
        });

        eventRepo.delete(event);
    }

    /**
     * Invita a un subcreador
     */
    @Transactional
    public EventRole inviteSubcreator(String eventId, String creatorUsername, EventRoleDto dto) {
        if (!isEventCreator(creatorUsername, eventId)) {
            throw new IllegalArgumentException("Solo el creador puede invitar subcreadores");
        }

        Event event = getById(eventId);

        // Verificar si ya existe una invitación activa
        if (eventRoleRepo.findByEmailInvitacionAndActivoTrue(dto.getEmailInvitacion()).size() > 0) {
            throw new IllegalArgumentException("Ya existe una invitación activa para este email");
        }

        EventRole role = EventRole.builder()
                .evento(event)
                .emailInvitacion(dto.getEmailInvitacion())
                .rol("SUBCREADOR")
                .fechaAsignacion(Instant.now())
                .activo(true)
                .build();
        // GUARDAR PRIMERO
        EventRole savedRole = eventRoleRepo.save(role);

        // DESPUÉS ENVIAR EMAIL (sin afectar el retorno)
        try {
            invitationService.sendInvitationEmail(savedRole);
        } catch (Exception e) {
            System.err.println("Error al enviar email de invitación: " + e.getMessage());
            // No lanzamos excepción para que la invitación se complete exitosamente
        }

        // RETORNAR EL MISMO TIPO QUE ANTES
        return savedRole; // EventRole (no boolean)
    }

    /**
     * Acepta invitación como subcreador
     */
    @Transactional
    public EventRole acceptSubcreatorInvitation(String username, String invitationId) {
        Users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        EventRole role = eventRoleRepo.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitación no encontrada"));

        if (!role.isActivo()) {
            throw new IllegalArgumentException("Invitación ya no está activa");
        }

        if (role.getUsuario() != null) {
            throw new IllegalArgumentException("Invitación ya fue aceptada");
        }

        // Verificar que el email del usuario coincida con la invitación
        if (!user.getEmail().equals(role.getEmailInvitacion())) {
            throw new IllegalArgumentException("El email no coincide con la invitación");
        }

        role.setUsuario(user);
        return eventRoleRepo.save(role);
    }

    // ================ MÉTODOS DE UTILIDAD ================

    private Event getById(String id) {
        return eventRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado: " + id));
    }

    private boolean isEventCreator(String username, String eventId) {
        try {
            // Obtener el usuario primero
            Users user = userRepo.findByUserName(username).orElse(null);
            if (user == null) {
                return false;
            }

            // Buscar todos los roles activos del usuario
            List<EventRole> roles = eventRoleRepo.findAll(); // Temporal - obtener todos

            // Filtrar manualmente por usuario, evento y rol CREADOR
            return roles.stream()
                    .anyMatch(role ->
                            role.getUsuario() != null &&
                                    role.getUsuario().getId().equals(user.getId()) &&
                                    role.getEvento() != null &&
                                    role.getEvento().getId().equals(eventId) &&
                                    role.isActivo() &&
                                    role.getRol().equals("CREADOR")
                    );

        } catch (Exception e) {
            System.err.println("Error en isEventCreator: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si un usuario tiene acceso a un evento privado
     */
    public boolean hasUserAccessToPrivateEvent(String username, Event event) {
        try {
            Users user = userRepo.findByUserName(username).orElse(null);
            if (user == null) {
                return false;
            }

            // 1. Verificar si es el creador del evento
            if (event.getCreator() != null && event.getCreator().getUserName().equals(username)) {
                return true;
            }

            // 2. Verificar si es subcreador activo
            boolean isSubcreator = eventRoleRepo.findAll().stream()
                    .anyMatch(role ->
                            role.getUsuario() != null &&
                                    role.getUsuario().getId().equals(user.getId()) &&
                                    role.getEvento() != null &&
                                    role.getEvento().getId().equals(event.getId()) &&
                                    role.isActivo() &&
                                    ("CREADOR".equals(role.getRol()) || "SUBCREADOR".equals(role.getRol()))
                    );

            if (isSubcreator) {
                return true;
            }

            // 3. NUEVA VALIDACIÓN - Verificar si está en la lista de invitados
            if (event.getInvitedUsers() != null && event.getInvitedUsers().contains(username)) {
                return true;
            }

            // 4.  NUEVA VALIDACIÓN - Verificar si tiene invitación aceptada
            return attendeeInvitationRepo.findAll().stream()
                    .anyMatch(invitation ->
                            invitation.getEvento() != null &&
                                    invitation.getEvento().getId().equals(event.getId()) &&
                                    invitation.getEmailInvitado().equals(user.getEmail()) &&
                                    "aceptada".equals(invitation.getEstado())
                    );

        } catch (Exception e) {
            System.err.println("Error verificando acceso a evento privado: " + e.getMessage());
            return false;
        }
    }

    private boolean canUserEditEvent(String username, String eventId) {
        try {
            Users user = userRepo.findByUserName(username).orElse(null);
            if (user == null) {
                return false;
            }

            // Buscar todos los roles activos del usuario
            List<EventRole> roles = eventRoleRepo.findAll();

            // Filtrar por usuario, evento y roles permitidos
            return roles.stream()
                    .anyMatch(role ->
                            role.getUsuario() != null &&
                                    role.getUsuario().getId().equals(user.getId()) &&
                                    role.getEvento() != null &&
                                    role.getEvento().getId().equals(eventId) &&
                                    role.isActivo() &&
                                    (role.getRol().equals("CREADOR") || role.getRol().equals("SUBCREADOR"))
                    );

        } catch (Exception e) {
            System.err.println("Error en canUserEditEvent: " + e.getMessage());
            return false;
        }
    }

    private boolean isFilterEmpty(EventFilterDto filter) {
        return filter.getStatus() == null && filter.getCategoriaId() == null &&
                filter.getSearchText() == null && filter.getStartDate() == null;
    }

    private List<Event> applyPublicFilters(EventFilterDto filter) {
        // Implementar lógica de filtros para eventos públicos
        if (filter.getSearchText() != null) {
            return eventRepo.searchPublicEventsByText(filter.getSearchText());
        }
        if (filter.getCategoriaId() != null) {
            return eventRepo.findByCategoriaId(filter.getCategoriaId())
                    .stream()
                    .filter(e -> "public".equals(e.getPrivacy()) && !e.isBloqueado())
                    .collect(Collectors.toList());
        }
        return eventRepo.findPublicEventsForUsers("Active");
    }

    private List<Event> applyAuthenticatedFilters(String username, EventFilterDto filter) {
        // Implementar lógica de filtros para usuarios autenticados
        if (filter.getSearchText() != null) {
            // Para usuarios autenticados, incluir búsqueda en eventos privados accesibles
            return eventRepo.findAccessibleEventsForUser(username, "Active")
                    .stream()
                    .filter(event -> event.getTitle().toLowerCase().contains(filter.getSearchText().toLowerCase()) ||
                            (event.getDescription() != null && event.getDescription().toLowerCase().contains(filter.getSearchText().toLowerCase())))
                    .collect(Collectors.toList());
        }
        if (filter.getCategoriaId() != null) {
            return eventRepo.findAccessibleEventsForUser(username, "Active")
                    .stream()
                    .filter(event -> event.getCategoria() != null &&
                            event.getCategoria().getId().equals(filter.getCategoriaId()))
                    .collect(Collectors.toList());
        }
        return eventRepo.findAccessibleEventsForUser(username, "Active");
    }

    private List<Event> applyFilters(EventFilterDto filter) {
        // Método de compatibilidad - delegar a filtros públicos
        return applyPublicFilters(filter);
    }

    private EventSummaryDto mapToSummaryDto(Event event) {
        EventSummaryDto dto = new EventSummaryDto();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setUbicacion(event.getLocation() != null ? event.getLocation().getAddress() : "");
        dto.setFechaInicio(event.getStart().toString());
        dto.setFechaFin(event.getEnd().toString());
        dto.setCategoria(event.getCategoria() != null ? event.getCategoria().getNombreCategoria() : "");
        dto.setTipo(event.getType());
        dto.setEsPago("paid".equals(event.getTicketType()));
        dto.setPrecio(event.getPrice() != null ? event.getPrice().getAmount() : 0.0);
        dto.setMoneda(event.getPrice() != null ? event.getPrice().getCurrency() : "");
        dto.setMaxAttendees(event.getMaxAttendees());
        dto.setCurrentAttendees(event.getCurrentAttendees());
        dto.setDisponible(event.getCurrentAttendees() < event.getMaxAttendees() && event.isPermitirInscripciones());
        dto.setImagenPrincipal(event.getMainImages() != null && !event.getMainImages().isEmpty() ?
                event.getMainImages().get(0).getUrl() : "");
        dto.setDestacado(event.isDestacado());
        return dto;
    }

    private Event mapDtoToEvent(EventDto dto, Users creator, State status, Category category) {
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
                .currentAttendees(0)
                .categoria(category)
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
                .creator(creator)
                .status(status)
                .destacado(dto.isDestacado())
                .permitirInscripciones(dto.isPermitirInscripciones())
                .fechaLimiteInscripcion(dto.getFechaLimiteInscripcion())
                .tags(dto.getTags())
                .bloqueado(false)
                .build();
    }

    private List<Media> mapMediaDtosToMedia(List<MediaDto> dtos) {
        if (dtos == null) return new ArrayList<>();
        return dtos.stream()
                .map(m -> new Media(m.getUrl(), m.getDescription(), m.getUploadedAt(), m.getMediaType()))
                .collect(Collectors.toList());
    }

    private void addHistoryRecord(List<HistoryRecord> history, String field, String oldValue, String newValue) {
        history.add(HistoryRecord.builder()
                .field(field)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedAt(Instant.now())
                .build());
    }

    private void updateEventFields(Event event, UpdateEventDto dto, List<HistoryRecord> changes) {
        if (dto.getTitle() != null && !dto.getTitle().equals(event.getTitle())) {
            addHistoryRecord(changes, "title", event.getTitle(), dto.getTitle());
            event.setTitle(dto.getTitle());
        }

        if (dto.getDescription() != null && !dto.getDescription().equals(event.getDescription())) {
            addHistoryRecord(changes, "description", event.getDescription(), dto.getDescription());
            event.setDescription(dto.getDescription());
        }

        if (dto.getStart() != null && !dto.getStart().equals(event.getStart())) {
            addHistoryRecord(changes, "start", event.getStart().toString(), dto.getStart().toString());
            event.setStart(dto.getStart());
        }

        if (dto.getEnd() != null && !dto.getEnd().equals(event.getEnd())) {
            addHistoryRecord(changes, "end", event.getEnd().toString(), dto.getEnd().toString());
            event.setEnd(dto.getEnd());
        }

        if (dto.getPrice() != null) {
            String oldValue = event.getPrice() != null ?
                    event.getPrice().getAmount() + " " + event.getPrice().getCurrency() : "none";
            event.setPrice(new Price(dto.getPrice().getAmount(), dto.getPrice().getCurrency()));
            addHistoryRecord(changes, "price", oldValue,
                    dto.getPrice().getAmount() + " " + dto.getPrice().getCurrency());
        }

        if (dto.getMaxAttendees() != null && !dto.getMaxAttendees().equals(event.getMaxAttendees())) {
            addHistoryRecord(changes, "maxAttendees",
                    event.getMaxAttendees() != null ? event.getMaxAttendees().toString() : "none",
                    dto.getMaxAttendees().toString());
            event.setMaxAttendees(dto.getMaxAttendees());
        }

        // Actualizar multimedia
        if (dto.getMainImages() != null) {
            event.setMainImages(mapMediaDtosToMedia(dto.getMainImages()));
            addHistoryRecord(changes, "mainImages", "Updated", "New images: " + dto.getMainImages().size());
        }

        if (dto.getGalleryImages() != null) {
            event.setGalleryImages(mapMediaDtosToMedia(dto.getGalleryImages()));
            addHistoryRecord(changes, "galleryImages", "Updated", "New images: " + dto.getGalleryImages().size());
        }

        if (dto.getVideos() != null) {
            event.setVideos(mapMediaDtosToMedia(dto.getVideos()));
            addHistoryRecord(changes, "videos", "Updated", "New videos: " + dto.getVideos().size());
        }

        if (dto.getDocuments() != null) {
            event.setDocuments(mapMediaDtosToMedia(dto.getDocuments()));
            addHistoryRecord(changes, "documents", "Updated", "New documents: " + dto.getDocuments().size());
        }

        if (dto.getOtherData() != null) {
            OtherData oldData = event.getOtherData();
            OtherData newData = new OtherData(
                    dto.getOtherData().getOrganizer(),
                    dto.getOtherData().getContact(),
                    dto.getOtherData().getNotes());

            if (oldData == null || !newData.getOrganizer().equals(oldData.getOrganizer())) {
                addHistoryRecord(changes, "organizer",
                        oldData != null ? oldData.getOrganizer() : "none",
                        newData.getOrganizer());
            }

            event.setOtherData(newData);
        }
    }
}