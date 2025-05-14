//package com.eventHub.backend_eventHub.events.controller;
//
//
//import com.eventHub.backend_eventHub.events.dto.*;
//import com.eventHub.backend_eventHub.events.service.EventService;
//import jakarta.validation.Valid;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//
//@RestController
//@RequestMapping("/api/events")
//public class EventController {
//
//    @Autowired private EventService eventService;
//
//    // LISTAR (Todos los usuarios autenticados)
//    @GetMapping
//    public ResponseEntity<List<EventResponseDto>> listUpcoming() {
//        return ResponseEntity.ok(eventService.getAllUpcomingEvents());
//    }
//
//    // DETALLE (Todos autenticados)
//    @GetMapping("/{id}")
//    public ResponseEntity<EventResponseDto> detail(@PathVariable String id) {
//        return ResponseEntity.ok(eventService.getEventById(id));
//    }
//
//    // CREAR (Usuario autenticado)
//    @PostMapping
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<EventResponseDto> create(
//            @Valid @RequestBody CreateEventDto dto,
//            Authentication auth) {
//        String userId = ((UserDetailsImpl)auth.getPrincipal()).getId();
//        return new ResponseEntity<>(
//                eventService.createEvent(dto, userId),
//                HttpStatus.CREATED
//        );
//    }
//
//    // ACTUALIZAR (Creator o Subcreator)
//    @PutMapping("/{id}")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<EventResponseDto> update(
//            @PathVariable String id,
//            @Valid @RequestBody UpdateEventDto dto,
//            Authentication auth) {
//        String userId = ((UserDetailsImpl)auth.getPrincipal()).getId();
//        return ResponseEntity.ok(
//                eventService.updateEvent(id, dto, userId)
//        );
//    }
//
//    // ELIMINAR (Solo Creator)
//    @DeleteMapping("/{id}")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<Void> delete(
//            @PathVariable String id,
//            Authentication auth) {
//        String userId = ((UserDetailsImpl)auth.getPrincipal()).getId();
//        eventService.deleteEvent(id, userId);
//        return ResponseEntity.noContent().build();
//    }
//
//    // CAMBIAR ESTADO (Admin global)
//    @PatchMapping("/{id}/state")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<Void> changeState(
//            @PathVariable String id,
//            @Valid @RequestBody ChangeEventStateDto dto,
//            Authentication auth) {
//        String adminId = ((UserDetailsImpl)auth.getPrincipal()).getId();
//        eventService.changeEventState(id, dto, adminId);
//        return ResponseEntity.ok().build();
//    }
//
//    // INVITAR SUBCREADOR (Creator)
//    @PostMapping("/{id}/invite")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<Void> invite(
//            @PathVariable String id,
//            @Valid @RequestBody InviteSubCreatorDto dto,
//            Authentication auth) {
//        String inviterId = ((UserDetailsImpl)auth.getPrincipal()).getId();
//        eventService.inviteSubCreator(id, dto, inviterId);
//        return ResponseEntity.ok().build();
//    }
//
//    // ENDPOINTS ADMIN (diferenciados)
//    @GetMapping("/admin/all")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<List<EventResponseDto>> adminListAll() {
//        return ResponseEntity.ok(eventService.getAllEvents());
//    }
//
//    @DeleteMapping("/admin/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<Void> adminDelete(@PathVariable String id) {
//        eventService.forceDeleteEvent(id);
//        return ResponseEntity.noContent().build();
//    }
//}
