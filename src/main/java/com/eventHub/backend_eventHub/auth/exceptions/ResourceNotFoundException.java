// ResourceNotFoundException.java
package com.eventHub.backend_eventHub.auth.exceptions;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}