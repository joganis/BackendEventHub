// ValidUserState.java
package com.eventHub.backend_eventHub.users.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UserStateValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUserState {
    String message() default "Estado de usuario no válido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}