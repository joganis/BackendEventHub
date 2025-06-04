// UniqueEmail.java - Validación personalizada para emails únicos
package com.eventHub.backend_eventHub.users.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UniqueEmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueEmail {
    String message() default "El email ya está en uso";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /**
     * ID del usuario a excluir de la validación (útil para actualizaciones)
     */
    String excludeUserId() default "";
}