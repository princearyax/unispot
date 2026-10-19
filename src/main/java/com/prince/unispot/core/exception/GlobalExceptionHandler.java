package com.prince.unispot.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.stream.Collectors;

//adding these @ResponseStatus on @ExceptionHandler will add it to the OpenAPI specification with its ErrorResponse schema.

@RestControllerAdvice
public class GlobalExceptionHandler {

        //simple logging facade for java :logger
        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        // 404 :resource does not exist
        @ExceptionHandler(ResourceNotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
                return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
        }

        // 409: conflict: client known duplicate (e.g. email already registered)
        @ExceptionHandler(DuplicateResourceException.class)
        @ResponseStatus(HttpStatus.CONFLICT)
        public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
                return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
        }

        // 409: conflict DB unique/FK constraint fired, safety net for races the app-level: for optimistic concurrency
        @ExceptionHandler(DataIntegrityViolationException.class)
        @ResponseStatus(HttpStatus.CONFLICT)
        public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
                log.warn("Data integrity violation at {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
                return build(HttpStatus.CONFLICT, "Conflict",
                        "This action conflicts with existing data (duplicate or invalid reference).", request);
        }

        // 409 @Version caught a lost-update race 
        @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
        @ResponseStatus(HttpStatus.CONFLICT)
        public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
                return build(HttpStatus.CONFLICT, "Conflict",
                        "This resource was modified by someone else since you last loaded it. Refresh and try again.", request);
        }

        // 401 -- bad email/password on login  unauthenticate
        @ExceptionHandler(AuthenticationException.class)
        @ResponseStatus(HttpStatus.UNAUTHORIZED)
        public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
                return build(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid email or password.", request);
        }

        // 401 -- missing/expired/invalid refresh token
        @ExceptionHandler(InvalidTokenException.class)
        @ResponseStatus(HttpStatus.UNAUTHORIZED)
        public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex, HttpServletRequest request) {
                return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request);
        }

        // 403 , rbac fail
        @ExceptionHandler(AccessDeniedException.class)
        @ResponseStatus(HttpStatus.FORBIDDEN)
        public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
                return build(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request);
        }

        // 400 , validation failed on a @RequestBody
        @ExceptionHandler(MethodArgumentNotValidException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
                String message = ex.getBindingResult().getFieldErrors().stream()
                        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                        .collect(Collectors.joining(", "));
                return build(HttpStatus.BAD_REQUEST, "Bad Request", message, request);
        }

        // 400, wrong type on a @RequestParam/@PathVariable e.g. ?category=NOT_REAL
        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
                String message = "Invalid value '%s' for parameter '%s'".formatted(ex.getValue(), ex.getName());
                return build(HttpStatus.BAD_REQUEST, "Bad Request", message, request);
        }

        // 400 -- generic bad-business-input
        @ExceptionHandler(IllegalArgumentException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
                return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
        }

        // 400 malformed JSON body, or a body value that can't convert to the target
        //mybe bad enum type, or bad query params
        @ExceptionHandler(HttpMessageNotReadableException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
                return build(HttpStatus.BAD_REQUEST, "Bad Request", "Malformed request body.", request);
        }
        //invalid sort column in pageable
        @ExceptionHandler(org.springframework.data.core.PropertyReferenceException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ResponseEntity<ErrorResponse> handleInvalidSortProperty(
                org.springframework.data.core.PropertyReferenceException ex,
                HttpServletRequest request) {
                return build(HttpStatus.BAD_REQUEST, "Bad Request",
                        "Invalid sort property: " + ex.getPropertyName(), request);
        }


        @ExceptionHandler(org.springframework.web.bind.MissingRequestCookieException.class)
        @ResponseStatus(HttpStatus.UNAUTHORIZED)
        public ResponseEntity<ErrorResponse> handleMissingCookie(
                org.springframework.web.bind.MissingRequestCookieException ex,
                HttpServletRequest request) {
                return build(HttpStatus.UNAUTHORIZED, "Unauthorized",
                        "Required cookie '" + ex.getCookieName() + "' is missing.", request);
        }

        //cloudinary errors!
        // 413 Payload Too Large -- file exceeds spring.servlet.multipart.max-file-size
        @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
        @ResponseStatus(HttpStatus.CONTENT_TOO_LARGE)
        public ResponseEntity<ErrorResponse> handleMaxUploadSize(
                org.springframework.web.multipart.MaxUploadSizeExceededException ex,
                HttpServletRequest request) {
                return build(HttpStatus.CONTENT_TOO_LARGE, "Payload Too Large",
                        "Uploaded file exceeds the maximum allowed size of 5MB.", request);
        }

        // 502 Bad Gateway -- upstream media storage (Cloudinary) failure
        @ExceptionHandler(FileStorageException.class)
        @ResponseStatus(HttpStatus.BAD_GATEWAY)
        public ResponseEntity<ErrorResponse> handleFileStorage(
                FileStorageException ex,
                HttpServletRequest request) {
                log.error("Media storage failure at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
                return build(HttpStatus.BAD_GATEWAY, "Media Storage Error", ex.getMessage(), request);
        }

        // 500 for,  unanticipated. 
        @ExceptionHandler(Exception.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
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