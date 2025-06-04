// PhoneNumberValidator.java
package com.eventHub.backend_eventHub.users.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    // Patrón para números de teléfono colombianos (móviles y fijos)
    private static final Pattern COLOMBIA_PHONE_PATTERN = Pattern.compile(
            "^(?:\\+57)?\\s?(?:3[0-2]\\d|[1-8]\\d?)\\d{7}$"
    );

    // Patrón más flexible para números internacionales
    private static final Pattern INTERNATIONAL_PHONE_PATTERN = Pattern.compile(
            "^[+]?[1-9]\\d{1,14}$"
    );

    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }

        String cleanPhone = phone.trim().replaceAll("\\s+", "");

        // Verificar longitud básica
        if (cleanPhone.length() < 7 || cleanPhone.length() > 15) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("El teléfono debe tener entre 7 y 15 dígitos")
                    .addConstraintViolation();
            return false;
        }

        // Intentar validar como número colombiano primero
        if (COLOMBIA_PHONE_PATTERN.matcher(cleanPhone).matches()) {
            return true;
        }

        // Si no es colombiano, validar como internacional
        if (INTERNATIONAL_PHONE_PATTERN.matcher(cleanPhone).matches()) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("Formato de teléfono no válido")
                .addConstraintViolation();
        return false;
    }
}