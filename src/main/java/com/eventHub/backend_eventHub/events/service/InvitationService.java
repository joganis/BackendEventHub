// ================================
// 3. NUEVO SERVICIO - InvitationService
// ================================

package com.eventHub.backend_eventHub.events.service;

import com.eventHub.backend_eventHub.events.dto.InvitationSummaryDto;
import com.eventHub.backend_eventHub.events.entities.EventRole;
import com.eventHub.backend_eventHub.events.repository.EventRoleRepository;
import com.eventHub.backend_eventHub.domain.entities.Users;
import com.eventHub.backend_eventHub.users.repository.UserRepository;
import com.eventHub.backend_eventHub.utils.emails.service.EmailService;
import com.eventHub.backend_eventHub.utils.emails.dto.EmailDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvitationService {

    @Autowired
    private EventRoleRepository eventRoleRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private EmailService emailService;

    /**
     * Obtener invitaciones pendientes del usuario
     */
    @Transactional(readOnly = true)
    public List<InvitationSummaryDto> getPendingInvitations(String username) {
        try {
            Users user = userRepo.findByUserName(username).orElse(null);
            if (user == null) {
                return Collections.emptyList();
            }

            List<EventRole> pendingInvitations = eventRoleRepo
                    .findPendingInvitationsByEmail(user.getEmail());

            return pendingInvitations.stream()
                    .map(this::mapToInvitationSummaryDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error al obtener invitaciones pendientes: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Obtener todas las invitaciones del usuario
     */
    @Transactional(readOnly = true)
    public List<InvitationSummaryDto> getAllInvitations(String username) {
        try {
            Users user = userRepo.findByUserName(username).orElse(null);
            if (user == null) {
                return Collections.emptyList();
            }

            List<EventRole> allInvitations = eventRoleRepo
                    .findAllInvitationsByEmail(user.getEmail());

            return allInvitations.stream()
                    .map(this::mapToInvitationSummaryDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error al obtener invitaciones: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Rechazar invitación
     */
    @Transactional
    public void rejectInvitation(String invitationId, String username) {
        Users user = userRepo.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        EventRole invitation = eventRoleRepo.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitación no encontrada"));

        // Verificar que el email coincida
        if (!user.getEmail().equals(invitation.getEmailInvitacion())) {
            throw new IllegalArgumentException("No tienes permisos para rechazar esta invitación");
        }

        // Verificar que no esté ya aceptada
        if (invitation.getUsuario() != null) {
            throw new IllegalArgumentException("No puedes rechazar una invitación ya aceptada");
        }

        // Desactivar la invitación (esto es rechazarla)
        invitation.setActivo(false);
        eventRoleRepo.save(invitation);

        // OPCIONAL: Enviar email de notificación al creador del evento
        try {
            sendRejectionNotificationEmail(invitation, user);
        } catch (Exception e) {
            System.err.println("Error al enviar email de rechazo: " + e.getMessage());
            // No lanzamos excepción aquí para que el rechazo se complete
        }
    }

    /**
     * Contar invitaciones pendientes
     */
    @Transactional(readOnly = true)
    public int getPendingInvitationsCount(String username) {
        try {
            Users user = userRepo.findByUserName(username).orElse(null);
            if (user == null) {
                return 0;
            }

            List<EventRole> pendingInvitations = eventRoleRepo
                    .findPendingInvitationsByEmail(user.getEmail());

            return pendingInvitations.size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Enviar email de invitación (método público para usar desde EventService)
     */
    public void sendInvitationEmail(EventRole invitation) {
        try {
            if (invitation.getEvento() == null || invitation.getEvento().getCreator() == null) {
                System.err.println("No se puede enviar email: evento o creador es null");
                return;
            }

            EmailDto emailDto = new EmailDto();
            emailDto.setRecipientEmail(invitation.getEmailInvitacion());
            emailDto.setSubject("🎪 Invitación para colaborar en evento: " + invitation.getEvento().getTitle());

            String emailBody = createInvitationEmailBody(invitation);
            emailDto.setBody(emailBody);

            emailService.sendEmail(emailDto);
            System.out.println("✅ Email de invitación enviado a: " + invitation.getEmailInvitacion());

        } catch (Exception e) {
            System.err.println("❌ Error al enviar email de invitación: " + e.getMessage());
        }
    }
    // ================ MÉTODOS PRIVADOS ================

    private InvitationSummaryDto mapToInvitationSummaryDto(EventRole eventRole) {
        InvitationSummaryDto dto = new InvitationSummaryDto();

        dto.setId(eventRole.getId());
        dto.setRol(eventRole.getRol());
        dto.setFechaInvitacion(eventRole.getFechaAsignacion());
        dto.setAceptada(eventRole.getUsuario() != null);
        dto.setActiva(eventRole.isActivo());

        // Determinar estado
        if (eventRole.getUsuario() != null) {
            dto.setEstado("aceptada");
        } else if (eventRole.isActivo()) {
            dto.setEstado("pendiente");
        } else {
            dto.setEstado("rechazada");
        }

        // Información del evento (con validación de null)
        if (eventRole.getEvento() != null) {
            dto.setEventoId(eventRole.getEvento().getId());
            dto.setEventoTitulo(eventRole.getEvento().getTitle());
            dto.setEventoDescripcion(eventRole.getEvento().getDescription());
            dto.setEventoFechaInicio(eventRole.getEvento().getStart() != null ?
                    eventRole.getEvento().getStart().toString() : "");
            dto.setEventoFechaFin(eventRole.getEvento().getEnd() != null ?
                    eventRole.getEvento().getEnd().toString() : "");
            dto.setEventoPrivacidad(eventRole.getEvento().getPrivacy());

            // Información del creador
            if (eventRole.getEvento().getCreator() != null) {
                dto.setEventoCreador(eventRole.getEvento().getCreator().getUserName());
                dto.setEventoCreadorEmail(eventRole.getEvento().getCreator().getEmail());
            }

            // Categoría
            if (eventRole.getEvento().getCategoria() != null) {
                dto.setEventoCategoria(eventRole.getEvento().getCategoria().getNombreCategoria());
            }
        } else {
            dto.setEventoTitulo("Evento eliminado");
            dto.setEventoCreador("Usuario eliminado");
        }

        return dto;
    }

    private String createInvitationEmailBody(EventRole invitation) {
        String eventTitle = invitation.getEvento().getTitle();
        String creatorName = invitation.getEvento().getCreator().getUserName();
        String eventStart = invitation.getEvento().getStart() != null ?
                invitation.getEvento().getStart().toString() : "Fecha por definir";
        String invitationId = invitation.getId();

        return String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h2 style="color: #2563eb;">¡Has sido invitado como subcreador! 🎪</h2>
                
                <p>Hola,</p>
                
                <p><strong>%s</strong> te ha invitado a colaborar como <strong>subcreador</strong> en el siguiente evento:</p>
                
                <div style="background-color: #f3f4f6; padding: 20px; border-radius: 8px; margin: 20px 0;">
                    <h3 style="color: #1f2937; margin-top: 0;">📅 %s</h3>
                    <p><strong>Organizador:</strong> %s</p>
                    <p><strong>Fecha:</strong> %s</p>
                    <p><strong>Rol:</strong> Subcreador</p>
                </div>
                
                <p><strong>Como subcreador podrás:</strong></p>
                <ul>
                    <li>✅ Crear y gestionar sub-eventos</li>
                    <li>✅ Editar información del evento principal</li>
                    <li>✅ Ver lista de inscritos</li>
                    <li>✅ Gestionar contenido multimedia</li>
                </ul>
                
                <div style="text-align: center; margin: 30px 0;">
                    <p>Para aceptar o rechazar esta invitación, inicia sesión en EventHub y revisa tus invitaciones pendientes.</p>
                    <p style="font-size: 14px; color: #6b7280;">ID de invitación: %s</p>
                </div>
                
                <hr style="margin: 30px 0; border: 1px solid #e5e7eb;">
                <p style="font-size: 12px; color: #9ca3af; text-align: center;">
                    Este email fue enviado automáticamente por EventHub.<br>
                    Si no esperabas esta invitación, puedes ignorar este mensaje.
                </p>
            </div>
            """, creatorName, eventTitle, creatorName, eventStart, invitationId);
    }

    private void sendRejectionNotificationEmail(EventRole invitation, Users rejectedUser) {
        try {
            if (invitation.getEvento() == null || invitation.getEvento().getCreator() == null) {
                return;
            }

            EmailDto emailDto = new EmailDto();
            emailDto.setRecipientEmail(invitation.getEvento().getCreator().getEmail());
            emailDto.setSubject("❌ Invitación rechazada - " + invitation.getEvento().getTitle());

            String emailBody = String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2 style="color: #dc2626;">Invitación rechazada</h2>
                    
                    <p>Hola <strong>%s</strong>,</p>
                    
                    <p>Te informamos que <strong>%s</strong> ha rechazado la invitación para ser subcreador del evento:</p>
                    
                    <div style="background-color: #fef2f2; padding: 15px; border-radius: 8px; margin: 20px 0;">
                        <h3 style="color: #991b1b; margin-top: 0;">📅 %s</h3>
                    </div>
                    
                    <p>Puedes invitar a otros usuarios desde el panel de gestión del evento.</p>
                    
                    <p>Saludos,<br>Equipo EventHub</p>
                </div>
                """,
                    invitation.getEvento().getCreator().getUserName(),
                    rejectedUser.getUserName(),
                    invitation.getEvento().getTitle()
            );

            emailDto.setBody(emailBody);
            emailService.sendEmail(emailDto);

        } catch (Exception e) {
            System.err.println("Error al enviar notificación de rechazo: " + e.getMessage());
        }
    }
}