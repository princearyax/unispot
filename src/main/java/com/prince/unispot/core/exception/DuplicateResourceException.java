package com.prince.unispot.core.exception;

//409, conflict: state clashes with existing 
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}