package com.prince.unispot.core.exception;

// resource (Place, Review, ...) does not exist.
//  for  404 NOT_FOUND.
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}