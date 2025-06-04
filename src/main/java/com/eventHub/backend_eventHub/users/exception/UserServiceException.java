// UserServiceException.java
package com.eventHub.backend_eventHub.users.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción general para errores del servicio de usuarios.
 */
public class UserServiceException extends RuntimeException {

    private final HttpStatus httpStatus;

    public UserServiceException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public UserServiceException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}