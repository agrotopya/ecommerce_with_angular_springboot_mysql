package com.fibiyo.ecommerce.application.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND) // 404 döner
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
         super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
    }
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}