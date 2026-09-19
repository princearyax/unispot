package com.prince.unispot.core.exception;

// 401: unauthorized 
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}