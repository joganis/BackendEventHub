// UniqueEmailValidator.java
package com.eventHub.backend_eventHub.users.validation;

import com.eventHub.backend_eventHub.users.repository.UserRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    private final UserRepository userRepository;
    private String excludeUserId;

    @Override
    public void initialize(UniqueEmail constraintAnnotation) {
        this.excludeUserId = constraintAnnotation.excludeUserId();
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(email)) {
            return true; // Let @NotBlank handle null/empty validation
        }

        return userRepository.findByEmail(email.trim())
                .map(existingUser -> {
                    // Si hay un usuario excluido y coincide, es válido (actualización)
                    return StringUtils.hasText(excludeUserId) &&
                            excludeUserId.equals(existingUser.getId());
                })
                .orElse(true); // Si no existe, es válido
    }
}