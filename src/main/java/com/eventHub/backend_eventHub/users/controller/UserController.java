package com.eventHub.backend_eventHub.users.controller;

import com.eventHub.backend_eventHub.users.dto.ChangeStatusDto;
import com.eventHub.backend_eventHub.users.dto.UpdateUserDto;
import com.eventHub.backend_eventHub.users.dto.UserProfileDto;
import com.eventHub.backend_eventHub.users.service.UserService;
import com.eventHub.backend_eventHub.domain.entities.Users;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controlador REST para la gestión de usuarios.
 * Permite listar, buscar, mostrar detalles, actualizar perfil y cambiar estado.
 */
@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Operaciones CRUD y de estado para usuarios")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;



    //─────────────────────────────────────────────────────────
    //   Perfil propio: cualquier usuario autenticado
    //─────────────────────────────────────────────────────────

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mi perfil", description = "Obtiene los datos del usuario autenticado")
    public ResponseEntity<UserProfileDto> getMe(Principal principal) {
        Users u = userService.getMe(principal);
        return ResponseEntity.ok(toDto(u));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Actualizar mi perfil", description = "Modifica datos del usuario autenticado")
    public ResponseEntity<UserProfileDto> updateMe(
            Principal principal,
            @Valid @RequestBody UpdateUserDto dto,
            BindingResult br) {
        if (br.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }
        Users updated = userService.updateMe(principal, dto);
        return ResponseEntity.ok(toDto(updated));
    }


    /**
     * GET /users?state={estado}
     * Lista todos los usuarios, opcionalmente filtrados por estado.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar usuarios", description = "Obtiene todos los usuarios o filtra por estado")
    public ResponseEntity<List<UserProfileDto>> list(@RequestParam Optional<String> state) {
        List<UserProfileDto> dtos = userService.getAll(state).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /users/{id}
     * Obtiene un usuario por su ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') ")
    @Operation(summary = "Obtener usuario", description = "Busca un usuario por su identificador")
    public ResponseEntity<UserProfileDto> getById(@PathVariable String id) {
        Users user = userService.getById(id);
        return ResponseEntity.ok(toDto(user));
    }

    /**
     * GET /users/search?username={userName}
     * Busca usuarios por coincidencia de userName.
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Buscar usuarios", description = "Busca usuarios por parte de su nombre de usuario")
    public ResponseEntity<List<UserProfileDto>> search(@RequestParam String username) {
        List<UserProfileDto> dtos = userService.searchByUserName(username).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * PUT /users/{id}
     * Actualiza la información de perfil de un usuario.
     */
//    @PutMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.username")
//    @Operation(summary = "Actualizar perfil", description = "Permite al usuario o admin actualizar datos de perfil")
//    public ResponseEntity<?> update(
//            @PathVariable String id,
//            @Valid @RequestBody UpdateUserDto dto,
//            BindingResult br) {
//        if (br.hasErrors()) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(br.getAllErrors());
//        }
//        Users updated = userService.updateUser(id, dto);
//        return ResponseEntity.ok(toDto(updated));
//    }

    /**
     * PATCH /users/{id}/status
     * Modifica únicamente el estado del usuario.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar estado", description = "Permite al admin cambiar el estado del usuario")
    public ResponseEntity<?> changeStatus(
            @PathVariable String id,
            @Valid @RequestBody ChangeStatusDto dto,
            BindingResult br) {
        if (br.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(br.getAllErrors());
        }
        Users updated = userService.changeState(id, dto);
        return ResponseEntity.ok(toDto(updated));
    }

    /**
     * Mapea la entidad Users al DTO UserProfileDto.
     * Añadido manejo de nulos para evitar NullPointerException.
     *
     * @param u Entidad Users recuperada de la base de datos
     * @return DTO con sólo los campos que debe exponer el API
     */
    private UserProfileDto toDto(Users u) {
        UserProfileDto dto = new UserProfileDto();
        dto.setId(u.getId());
        dto.setUserName(u.getUserName());
        dto.setEmail(u.getEmail());
        dto.setName(u.getName());
        dto.setLastName(u.getLastName());
        dto.setIdentification(u.getIdentification());
        dto.setBirthDate(u.getBirthDate());
        dto.setPhone(u.getPhone());
        dto.setHomeAddress(u.getHomeAddress());
        dto.setCountry(u.getCountry());
        dto.setCity(u.getCity());
        dto.setPhoto(u.getPhoto());

        // Manejo seguro del estado para evitar NullPointerException
        if (u.getState() != null && u.getState().getNameState() != null) {
            dto.setState(u.getState().getNameState().name());
        } else {
            dto.setState("Unknown"); // Valor por defecto cuando no hay estado
        }

        // Manejo seguro del rol para evitar NullPointerException
        if (u.getRole() != null && u.getRole().getNombreRol() != null) {
            dto.setRole(u.getRole().getNombreRol().name());
        } else {
            dto.setRole("Unknown"); // Valor por defecto cuando no hay rol
        }

        return dto;
    }
}