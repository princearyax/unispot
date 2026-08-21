package com.prince.unispot.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    //security, rbac failure(403 Forbidden)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, 
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                request.getRequestURI(),
                "Forbidden",
                ex.getMessage(),
                HttpStatus.FORBIDDEN.value(),
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    // not found, bad busniess (400 Bad Request)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, 
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                request.getRequestURI(),
                "Bad Request",
                ex.getMessage(), // like: "Place not found"
                HttpStatus.BAD_REQUEST.value(),
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // catch - all kida
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, 
            HttpServletRequest request) {
        
        // logger.error("Unexpected error occurred", ex);, for debug etc

        ErrorResponse errorResponse = new ErrorResponse(
                request.getRequestURI(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.", //
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}