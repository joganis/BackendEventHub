package com.eventHub.backend_eventHub.admin.controller;


import com.eventHub.backend_eventHub.admin.dto.NewAdminDto;
import com.eventHub.backend_eventHub.admin.exception.CustomException;
import com.eventHub.backend_eventHub.admin.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;



/**
 * Controlador REST para la gestión de usuarios con roles elevados (administrador y subadministrador).
 *
 * Los endpoints de este controlador están protegidos y solo son accesibles para usuarios con rol ADMIN.
 */
@RestController
@RequestMapping("/admin")
public class AdminControllerRegister {

    private final AdminService adminService;

    @Autowired
    public AdminControllerRegister(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerAdmin(@Valid @RequestBody NewAdminDto newAdminDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body("Error en los datos enviados: " + bindingResult.getFieldError().getDefaultMessage());
        }

        try {
            adminService.registerAdmin(newAdminDto);
            return ResponseEntity.status(HttpStatus.CREATED).body("Administrador registrado correctamente");
        } catch (CustomException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error inesperado: " + e.getMessage());
        }
    }
}
