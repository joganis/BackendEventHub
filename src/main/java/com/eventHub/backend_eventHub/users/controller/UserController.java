package com.eventHub.backend_eventHub.users.controller;

import com.eventHub.backend_eventHub.users.dto.ChangeStatusDto;
import com.eventHub.backend_eventHub.users.dto.UpdateUserDto;
import com.eventHub.backend_eventHub.users.dto.UserProfileDto;
import com.eventHub.backend_eventHub.users.service.UserService;
import com.eventHub.backend_eventHub.domain.entities.Users;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

/**
 * Controlador REST mejorado para la gestión de usuarios.
 * Incluye mejor documentación, validaciones y manejo de respuestas.
 */
@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Operaciones CRUD y de estado para usuarios")
@RequiredArgsConstructor
@Validated
@Slf4j
public class UserController {

    private final UserService userService;

    //─────────────────────────────────────────────────────────
    //   Perfil propio: cualquier usuario autenticado
    //─────────────────────────────────────────────────────────

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Obtener mi perfil",
            description = "Obtiene los datos del usuario autenticado actualmente"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil obtenido exitosamente"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<UserProfileDto> getMe(Principal principal) {
        log.debug("Obteniendo perfil para usuario: {}", principal.getName());

        Users user = userService.getMe(principal);
        UserProfileDto dto = mapToDto(user);

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Actualizar mi perfil",
            description = "Modifica los datos del usuario autenticado"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "409", description = "Conflicto - Email ya existe")
    })
    public ResponseEntity<UserProfileDto> updateMe(
            Principal principal,
            @Valid @RequestBody UpdateUserDto dto) {

        log.info("Actualizando perfil para usuario: {}", principal.getName());

        Users updated = userService.updateMe(principal, dto);
        UserProfileDto responseDto = mapToDto(updated);

        return ResponseEntity.ok(responseDto);
    }

    //─────────────────────────────────────────────────────────
    //   Gestión de usuarios: solo administradores
    //─────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Listar usuarios",
            description = "Obtiene todos los usuarios del sistema, opcionalmente filtrados por estado"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuarios obtenidos exitosamente"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - Se requiere rol ADMIN")
    })
    public ResponseEntity<StandardListResponse<UserProfileDto>> list(
            @Parameter(description = "Estado para filtrar usuarios (opcional)")
            @RequestParam Optional<String> state) {

        log.debug("Listando usuarios con filtro de estado: {}", state.orElse("ninguno"));

        List<Users> users = userService.getAll(state);
        List<UserProfileDto> dtos = users.stream()
                .map(this::mapToDto)
                .toList();

        StandardListResponse<UserProfileDto> response = StandardListResponse.<UserProfileDto>builder()
                .data(dtos)
                .total(dtos.size())
                .filtered(state.isPresent())
                .filter(state.orElse(null))
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Obtener usuario por ID",
            description = "Busca un usuario específico por su identificador único"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - Se requiere rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<UserProfileDto> getById(
            @Parameter(description = "ID único del usuario")
            @PathVariable @NotBlank String id) {

        log.debug("Obteniendo usuario por ID: {}", id);

        Users user = userService.getById(id);
        UserProfileDto dto = mapToDto(user);

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Buscar usuarios",
            description = "Busca usuarios por coincidencia parcial en el nombre de usuario"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Búsqueda completada"),
            @ApiResponse(responseCode = "400", description = "Parámetro de búsqueda inválido"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - Se requiere rol ADMIN")
    })
    public ResponseEntity<StandardListResponse<UserProfileDto>> search(
            @Parameter(description = "Término de búsqueda para el nombre de usuario")
            @RequestParam @NotBlank String username) {

        log.debug("Buscando usuarios con término: {}", username);

        List<Users> users = userService.searchByUserName(username);
        List<UserProfileDto> dtos = users.stream()
                .map(this::mapToDto)
                .toList();

        StandardListResponse<UserProfileDto> response = StandardListResponse.<UserProfileDto>builder()
                .data(dtos)
                .total(dtos.size())
                .filtered(true)
                .filter(username)
                .build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Cambiar estado de usuario",
            description = "Permite al administrador cambiar el estado de un usuario específico"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado cambiado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Estado inválido"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - Se requiere rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<StandardApiResponse<UserProfileDto>> changeStatus(
            @Parameter(description = "ID único del usuario")
            @PathVariable @NotBlank String id,
            @Valid @RequestBody ChangeStatusDto dto) {

        log.info("Cambiando estado del usuario {} a: {}", id, dto.getState());

        Users updated = userService.changeState(id, dto);
        UserProfileDto responseDto = mapToDto(updated);

        StandardApiResponse<UserProfileDto> response = StandardApiResponse.<UserProfileDto>builder()
                .success(true)
                .message("Estado del usuario actualizado exitosamente")
                .data(responseDto)
                .build();

        return ResponseEntity.ok(response);
    }

    //─────────────────────────────────────────────────────────
    //   Endpoints de utilidad para validaciones
    //─────────────────────────────────────────────────────────

    @GetMapping("/check/username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Verificar disponibilidad de username",
            description = "Verifica si un nombre de usuario está disponible"
    )
    public ResponseEntity<AvailabilityResponse> checkUsernameAvailability(
            @Parameter(description = "Nombre de usuario a verificar")
            @PathVariable @NotBlank String username) {

        boolean available = userService.isUserNameAvailable(username);

        AvailabilityResponse response = AvailabilityResponse.builder()
                .available(available)
                .field("username")
                .value(username)
                .message(available ? "Username disponible" : "Username ya está en uso")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/check/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Verificar disponibilidad de email",
            description = "Verifica si un email está disponible"
    )
    public ResponseEntity<AvailabilityResponse> checkEmailAvailability(
            @Parameter(description = "Email a verificar")
            @PathVariable @NotBlank String email) {

        boolean available = userService.isEmailAvailable(email);

        AvailabilityResponse response = AvailabilityResponse.builder()
                .available(available)
                .field("email")
                .value(email)
                .message(available ? "Email disponible" : "Email ya está en uso")
                .build();

        return ResponseEntity.ok(response);
    }

    //─────────────────────────────────────────────────────────
    //   Métodos de mapeo y DTOs de respuesta
    //─────────────────────────────────────────────────────────

    /**
     * Mapea la entidad Users al DTO UserProfileDto de forma segura.
     * Incluye manejo robusto de valores nulos.
     *
     * @param user Entidad Users recuperada de la base de datos
     * @return DTO con los campos expuestos por la API
     */
    private UserProfileDto mapToDto(Users user) {
        if (user == null) {
            return null;
        }

        UserProfileDto dto = new UserProfileDto();
        dto.setId(user.getId());
        dto.setUserName(user.getUserName());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setLastName(user.getLastName());
        dto.setIdentification(user.getIdentification());
        dto.setBirthDate(user.getBirthDate());
        dto.setPhone(user.getPhone());
        dto.setHomeAddress(user.getHomeAddress());
        dto.setCountry(user.getCountry());
        dto.setCity(user.getCity());
        dto.setPhoto(user.getPhoto());

        // Manejo seguro del estado
        if (user.getState() != null && user.getState().getNameState() != null) {
            dto.setState(user.getState().getNameState().name());
        } else {
            dto.setState("Unknown");
        }

        // Manejo seguro del rol
        if (user.getRole() != null && user.getRole().getNombreRol() != null) {
            dto.setRole(user.getRole().getNombreRol().name());
        } else {
            dto.setRole("Unknown");
        }

        return dto;
    }

    //─────────────────────────────────────────────────────────
    //   DTOs de respuesta para la API
    //─────────────────────────────────────────────────────────

    /**
     * Respuesta genérica de la API.
     */
    @lombok.Data
    @lombok.Builder
    public static class StandardApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        private String timestamp = java.time.LocalDateTime.now().toString();
    }

    /**
     * Respuesta para listas de elementos.
     */
    @lombok.Data
    @lombok.Builder
    public static class StandardListResponse<T> {
        private List<T> data;
        private int total;
        private boolean filtered;
        private String filter;
        private String timestamp = java.time.LocalDateTime.now().toString();
    }

    /**
     * Respuesta para verificación de disponibilidad.
     */
    @lombok.Data
    @lombok.Builder
    public static class AvailabilityResponse {
        private boolean available;
        private String field;
        private String value;
        private String message;
        private String timestamp = java.time.LocalDateTime.now().toString();
    }
}