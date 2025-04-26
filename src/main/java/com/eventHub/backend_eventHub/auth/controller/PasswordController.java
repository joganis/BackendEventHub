package com.eventHub.backend_eventHub.auth.controller;

import com.eventHub.backend_eventHub.auth.dto.RecoveryRequestDto;
import com.eventHub.backend_eventHub.auth.dto.ResetPasswordDto;
import com.eventHub.backend_eventHub.auth.service.PasswordService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para la autenticación que incluye el flujo de recuperación y cambio de contraseña.
 */
@RestController
@RequestMapping("/password")
public class PasswordController  {

    private final PasswordService passwordService;

    @Autowired
    public PasswordController (PasswordService passwordService) {
        this.passwordService = passwordService;
    }


    /**
     * Endpoint para solicitar la recuperación de contraseña.
     * Recibe el email del usuario, genera un token de recuperación y envía un correo.
     *
     * URL: POST /auth/recover-password
     */
    @PostMapping("/recover-password")
    public ResponseEntity<String> recoverPassword(@Valid @RequestBody RecoveryRequestDto recoveryRequestDto,
                                                  BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body("Datos inválidos para recuperación");
        }
        try {
            passwordService.recoverPassword(recoveryRequestDto);
            return ResponseEntity.ok("Correo de recuperación enviado");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Endpoint para restablecer la contraseña.
     * Recibe el token de recuperación y la nueva contraseña.
     *
     * URL: POST /auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordDto resetPasswordDto,
                                                BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body("Datos inválidos para restablecer la contraseña");
        }
        try {
            passwordService.resetPassword(resetPasswordDto);
            return ResponseEntity.ok("Contraseña actualizada exitosamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
