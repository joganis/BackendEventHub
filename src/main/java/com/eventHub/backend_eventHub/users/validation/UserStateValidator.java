// UserStateValidator.java
package com.eventHub.backend_eventHub.users.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.Set;

public class UserStateValidator implements ConstraintValidator<ValidUserState, String> {

    private static final Set<String> VALID_STATES = Set.of(
            "Active", "Inactive", "Pending", "Canceled", "Blocked",
            "Activo", "Bloqueado" // Estados en español para compatibilidad
    );

    @Override
    public void initialize(ValidUserState constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String state, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(state)) {
            return false;
        }

        String trimmedState = state.trim();

        if (!VALID_STATES.contains(trimmedState)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Estado no válido. Debe ser uno de: " + String.join(", ", VALID_STATES)
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}
