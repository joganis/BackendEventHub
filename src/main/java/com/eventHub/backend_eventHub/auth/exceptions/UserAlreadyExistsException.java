// UserAlreadyExistsException.java
package com.eventHub.backend_eventHub.auth.exceptions;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}