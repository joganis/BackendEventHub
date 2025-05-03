package com.eventHub.backend_eventHub.auth.service;



import com.eventHub.backend_eventHub.utils.emails.dto.EmailDto;
import com.eventHub.backend_eventHub.auth.dto.RecoveryRequestDto;
import com.eventHub.backend_eventHub.auth.dto.ResetPasswordDto;
import com.eventHub.backend_eventHub.domain.entities.Users;
import com.eventHub.backend_eventHub.users.repository.UserRepository;
import com.eventHub.backend_eventHub.auth.jwt.JwtUtil;
import com.eventHub.backend_eventHub.utils.emails.service.EmailService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordService {

    @Autowired
    private UserRepository userRepository;  // Para buscar y guardar usuarios

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserAuthService userAuthService;  // Servicio que implementa UserDetailsService

    /**
     * Procesa la solicitud de recuperación de contraseña.
     *
     * - Busca al usuario por email.
     * - Genera un token de recuperación.
     * - Envía un correo con el enlace para restablecer la contraseña.
     *
     * @param recoveryRequestDto DTO con el email del usuario.
     */
    public void recoverPassword(RecoveryRequestDto recoveryRequestDto) {
        Users user = userRepository.findByEmail(recoveryRequestDto.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con el email proporcionado"));

        String recoveryToken = jwtUtil.generateRecoveryToken(user);

        EmailDto emailDto = new EmailDto();
        emailDto.setRecipientEmail(user.getEmail());
        emailDto.setSubject("Recuperación de Contraseña");
        emailDto.setBody("Para restablecer su contraseña, haga clic en el siguiente enlace: " +
                "http://localhost:4200/reset-password?token=" + recoveryToken);
        emailService.sendEmail(emailDto);
    }

    /**
     * Procesa la solicitud para restablecer la contraseña.
     *
     * - Extrae las claims del token.
     * - Verifica que sea un token de recuperación y que no haya expirado.
     * - Busca al usuario por username (subject del token).
     * - Actualiza la contraseña del usuario.
     *
     * @param resetPasswordDto DTO con el token y la nueva contraseña.
     */
    @Transactional
    public void resetPassword(ResetPasswordDto resetPasswordDto) {
        Claims claims;
        try {
            claims = jwtUtil.extractAllClaims(resetPasswordDto.getToken());
        } catch (Exception e) {
            throw new RuntimeException("Token inválido o expirado");
        }
        if (!"recovery".equals(claims.get("type")) || jwtUtil.isTokenExpired(resetPasswordDto.getToken())) {
            throw new RuntimeException("Token inválido o expirado");
        }
        String username = claims.getSubject();
        Users user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setPassword(passwordEncoder.encode(resetPasswordDto.getNewPassword()));
        userRepository.save(user);
    }
}
