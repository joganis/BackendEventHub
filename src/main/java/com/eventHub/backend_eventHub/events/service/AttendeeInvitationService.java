// ================================
// 4. SERVICIO PARA INVITACIONES DE ASISTENTES
// ================================

package com.eventHub.backend_eventHub.events.service;

import com.eventHub.backend_eventHub.events.dto.AttendeeInvitationDto;
import com.eventHub.backend_eventHub.events.dto.BulkAttendeeInvitationDto;
import com.eventHub.backend_eventHub.events.entities.AttendeeInvitation;
import com.eventHub.backend_eventHub.events.entities.Event;
import com.eventHub.backend_eventHub.events.repository.AttendeeInvitationRepository;
import com.eventHub.backend_eventHub.events.repository.EventRepository;
import com.eventHub.backend_eventHub.events.repository.EventRoleRepository;
import com.eventHub.backend_eventHub.domain.entities.Users;
import com.eventHub.backend_eventHub.users.repository.UserRepository;
import com.eventHub.backend_eventHub.utils.emails.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class AttendeeInvitationService {

    @Autowired
    private AttendeeInvitationRepository attendeeInvitationRepo;

    @Autowired
    private EventRepository eventRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private EventRoleRepository eventRoleRepo;

    @Autowired
    private EmailService emailService; // Asumo que existe

    /**
     * Envía invitación a un asistente para evento privado
     */
    @Transactional
    public AttendeeInvitation inviteAttendee(String organizerUsername, AttendeeInvitationDto dto) {
        // 1. Validar que el usuario es organizador del evento
        Users organizer = userRepo.findByUserName(organizerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Usuario organizador no encontrado"));

        Event event = eventRepo.findById(dto.getEventoId())
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        validateOrganizerPermissions(organizer, event);

        // 2. Validar que el evento es privado
        if (!"private".equals(event.getPrivacy())) {
            throw new IllegalArgumentException("Solo se pueden enviar invitaciones para eventos privados");
        }

        // 3. Verificar que no existe invitación activa
        boolean hasActiveInvitation = attendeeInvitationRepo.existsByEventoIdAndEmailInvitadoAndEstadoIn(
                dto.getEventoId(),
                dto.getEmailInvitado(),
                Arrays.asList("pendiente", "aceptada")
        );

        if (hasActiveInvitation) {
            throw new IllegalArgumentException("Ya existe una invitación activa para este email");
        }

        // 4. Verificar si el usuario ya está registrado
        Users existingUser = userRepo.findByEmail(dto.getEmailInvitado()).orElse(null);

        // 5. Crear invitación
        AttendeeInvitation invitation = AttendeeInvitation.builder()
                .evento(event)
                .invitadoPor(organizer)
                .emailInvitado(dto.getEmailInvitado())
                .usuarioInvitado(existingUser) // null si no existe
                .fechaInvitacion(Instant.now())
                .fechaExpiracion(Instant.now().plus(7, ChronoUnit.DAYS)) // 7 días
                .estado("pendiente")
                .mensaje(dto.getMensaje())
                .token(generateUniqueToken())
                .fechaUltimoEnvio(Instant.now())
                .vecesEnviada(1)
                .build();

        invitation = attendeeInvitationRepo.save(invitation);

        // 6. Enviar email
        try {
            sendInvitationEmail(invitation);
        } catch (Exception e) {
            System.err.println("Error enviando email de invitación: " + e.getMessage());
            // No fallar la invitación por error de email
        }

        return invitation;
    }

    /**
     * Envía invitaciones masivas
     */
    @Transactional
    public List<AttendeeInvitation> inviteMultipleAttendees(String organizerUsername, BulkAttendeeInvitationDto dto) {
        List<AttendeeInvitation> invitations = new ArrayList<>();

        for (String email : dto.getEmails()) {
            try {
                AttendeeInvitationDto singleDto = new AttendeeInvitationDto();
                singleDto.setEventoId(dto.getEventoId());
                singleDto.setEmailInvitado(email);
                singleDto.setMensaje(dto.getMensaje());

                AttendeeInvitation invitation = inviteAttendee(organizerUsername, singleDto);
                invitations.add(invitation);
            } catch (IllegalArgumentException e) {
                // Log el error pero continuar con otros emails
                System.err.println("Error invitando a " + email + ": " + e.getMessage());
            }
        }

        return invitations;
    }

    /**
     * Acepta una invitación usando el token
     */
    @Transactional
    public AttendeeInvitation acceptInvitation(String token, String username) {
        AttendeeInvitation invitation = attendeeInvitationRepo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invitación no encontrada o inválida"));

        // Validar estado de la invitación
        if (!"pendiente".equals(invitation.getEstado())) {
            throw new IllegalArgumentException("Esta invitación ya fue " + invitation.getEstado());
        }

        // Validar expiración
        if (Instant.now().isAfter(invitation.getFechaExpiracion())) {
            invitation.setEstado("expirada");
            attendeeInvitationRepo.save(invitation);
            throw new IllegalArgumentException("Esta invitación ha expirado");
        }

        // Validar que el email coincide con el usuario
        Users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!user.getEmail().equals(invitation.getEmailInvitado())) {
            throw new IllegalArgumentException("Esta invitación no corresponde a tu email");
        }

        // Aceptar invitación
        invitation.setEstado("aceptada");
        invitation.setFechaRespuesta(Instant.now());
        invitation.setUsuarioInvitado(user);

        // Agregar usuario a la lista de invitados del evento
        Event event = invitation.getEvento();
        if (event.getInvitedUsers() == null) {
            event.setInvitedUsers(new ArrayList<>());
        }
        if (!event.getInvitedUsers().contains(username)) {
            event.getInvitedUsers().add(username);
            eventRepo.save(event);
        }

        return attendeeInvitationRepo.save(invitation);
    }

    /**
     * Rechaza una invitación
     */
    @Transactional
    public AttendeeInvitation rejectInvitation(String token, String username) {
        AttendeeInvitation invitation = attendeeInvitationRepo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invitación no encontrada o inválida"));

        if (!"pendiente".equals(invitation.getEstado())) {
            throw new IllegalArgumentException("Esta invitación ya fue " + invitation.getEstado());
        }

        // Validar que el email coincide
        Users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!user.getEmail().equals(invitation.getEmailInvitado())) {
            throw new IllegalArgumentException("Esta invitación no corresponde a tu email");
        }

        invitation.setEstado("rechazada");
        invitation.setFechaRespuesta(Instant.now());
        invitation.setUsuarioInvitado(user);

        return attendeeInvitationRepo.save(invitation);
    }

    /**
     * Lista invitaciones pendientes de un usuario
     */
    @Transactional(readOnly = true)
    public List<AttendeeInvitation> getPendingInvitationsForUser(String username) {
        Users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Buscar por email y por usuario registrado
        List<AttendeeInvitation> byEmail = attendeeInvitationRepo.findByEmailInvitadoAndEstado(user.getEmail(), "pendiente");
        List<AttendeeInvitation> byUser = attendeeInvitationRepo.findByUsuarioInvitadoIdAndEstado(user.getId(), "pendiente");

        // Combinar y eliminar duplicados
        List<AttendeeInvitation> allInvitations = new ArrayList<>(byEmail);
        for (AttendeeInvitation inv : byUser) {
            if (!allInvitations.contains(inv)) {
                allInvitations.add(inv);
            }
        }

        return allInvitations;
    }

    /**
     * Lista invitaciones de un evento (para organizadores)
     */
    @Transactional(readOnly = true)
    public List<AttendeeInvitation> getEventInvitations(String eventoId, String organizerUsername) {
        Users organizer = userRepo.findByUserName(organizerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Event event = eventRepo.findById(eventoId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        validateOrganizerPermissions(organizer, event);

        return attendeeInvitationRepo.findByEventoId(eventoId);
    }

    // ================ MÉTODOS AUXILIARES ================

    private void validateOrganizerPermissions(Users user, Event event) {
        // Verificar si es creador
        if (event.getCreator() != null && event.getCreator().getId().equals(user.getId())) {
            return;
        }

        // Verificar si es subcreador activo
        boolean isSubcreator = eventRoleRepo.findAll().stream()
                .anyMatch(role ->
                        role.getUsuario() != null &&
                                role.getUsuario().getId().equals(user.getId()) &&
                                role.getEvento() != null &&
                                role.getEvento().getId().equals(event.getId()) &&
                                role.isActivo() &&
                                "SUBCREADOR".equals(role.getRol())
                );

        if (!isSubcreator) {
            throw new IllegalArgumentException("No tienes permisos para enviar invitaciones para este evento");
        }
    }

    private String generateUniqueToken() {
        return "inv_" + UUID.randomUUID().toString().replace("-", "");
    }

    private void sendInvitationEmail(AttendeeInvitation invitation) {
        // Implementar envío de email con el token de invitación
        String invitationUrl = "https://your-app.com/accept-invitation?token=" + invitation.getToken();

        String subject = "Invitación a evento privado: " + invitation.getEvento().getTitle();
        String message = String.format(
                "Has sido invitado al evento privado '%s' por %s.\n\n" +
                        "Mensaje del organizador: %s\n\n" +
                        "Para aceptar o rechazar la invitación, haz clic en el siguiente enlace:\n%s\n\n" +
                        "Esta invitación expira el %s",
                invitation.getEvento().getTitle(),
                invitation.getInvitadoPor().getUserName(),
                invitation.getMensaje() != null ? invitation.getMensaje() : "No hay mensaje adicional",
                invitationUrl,
                invitation.getFechaExpiracion()
        );

        // emailService.sendEmail(invitation.getEmailInvitado(), subject, message);
        System.out.println("Email enviado a: " + invitation.getEmailInvitado());
    }
}