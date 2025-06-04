// BirthDateValidator.java
package com.eventHub.backend_eventHub.users.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class BirthDateValidator implements ConstraintValidator<ValidBirthDate, String> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MIN_AGE = 13;
    private static final int MAX_AGE = 120;

    @Override
    public void initialize(ValidBirthDate constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String birthDate, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(birthDate)) {
            return false;
        }

        try {
            LocalDate date = LocalDate.parse(birthDate.trim(), DATE_FORMATTER);
            LocalDate now = LocalDate.now();

            // Verificar que no sea una fecha futura
            if (date.isAfter(now)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("La fecha de nacimiento no puede ser futura")
                        .addConstraintViolation();
                return false;
            }

            // Calcular edad
            int age = now.getYear() - date.getYear();
            if (now.getDayOfYear() < date.getDayOfYear()) {
                age--;
            }

            // Verificar rango de edad
            if (age < MIN_AGE) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Debe ser mayor de " + MIN_AGE + " años")
                        .addConstraintViolation();
                return false;
            }

            if (age > MAX_AGE) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("La edad no puede exceder " + MAX_AGE + " años")
                        .addConstraintViolation();
                return false;
            }

            return true;

        } catch (DateTimeParseException e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Formato de fecha inválido. Use YYYY-MM-DD")
                    .addConstraintViolation();
            return false;
        }
    }
}