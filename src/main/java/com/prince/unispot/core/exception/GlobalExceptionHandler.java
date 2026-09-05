package com.prince.unispot.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

//this is part of spring MVC, it sees exception raise dby controller after filters etc, so exception in filters are just bubbled to server(tomcat eg)
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

        //@Valid fails on a @RequestBody -- without this it falls through
        //to the catch-all Exception handler below and comes back as a misleading 500
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(
                MethodArgumentNotValidException ex,
                HttpServletRequest request) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse errorResponse = new ErrorResponse(
                request.getRequestURI(),
                "Bad Request",
                message,
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