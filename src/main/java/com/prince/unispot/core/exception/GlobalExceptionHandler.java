package com.prince.unispot.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

        //simple logging facade for java :logger
        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        // 404 :resource does not exist
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
                return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
        }

        // 409: conflict: client known duplicate (e.g. email already registered)
        @ExceptionHandler(DuplicateResourceException.class)
        public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
                return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
        }

        // 409: conflict DB unique/FK constraint fired, safety net for races the app-level: for optimistic concurrency
        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
                log.warn("Data integrity violation at {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
                return build(HttpStatus.CONFLICT, "Conflict",
                        "This action conflicts with existing data (duplicate or invalid reference).", request);
        }

        // 409 @Version caught a lost-update race 
        @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
        public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
                return build(HttpStatus.CONFLICT, "Conflict",
                        "This resource was modified by someone else since you last loaded it. Refresh and try again.", request);
        }

        // 401 -- bad email/password on login  unauthenticate
        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
                return build(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid email or password.", request);
        }

        // 401 -- missing/expired/invalid refresh token
        @ExceptionHandler(InvalidTokenException.class)
        public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex, HttpServletRequest request) {
                return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request);
        }

        // 403 , rbac fail
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
                return build(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request);
        }

        // 400 , validation failed on a @RequestBody
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
                String message = ex.getBindingResult().getFieldErrors().stream()
                        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                        .collect(Collectors.joining(", "));
                return build(HttpStatus.BAD_REQUEST, "Bad Request", message, request);
        }

        // 400, wrong type on a @RequestParam/@PathVariable e.g. ?category=NOT_REAL
        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
                String message = "Invalid value '%s' for parameter '%s'".formatted(ex.getValue(), ex.getName());
                return build(HttpStatus.BAD_REQUEST, "Bad Request", message, request);
        }

        // 400 -- generic bad-business-input
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
                return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
        }

        // 500 for,  unanticipated. 
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, HttpServletRequest request) {
                log.error("Unhandled exception at {}", request.getRequestURI(), ex);
                return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                        "An unexpected error occurred. Please try again later.", request);
        }

        private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(request.getRequestURI(), error, message, status.value(), Instant.now());
                return new ResponseEntity<>(body, status);
        }
}