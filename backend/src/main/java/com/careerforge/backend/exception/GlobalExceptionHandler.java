package com.careerforge.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

@RestControllerAdvice 
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(
        UserNotFoundException exception){
            Map<String, String> error = Map.of(
                "message", exception.getMessage()
            );

            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
        }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrorS(
        MethodArgumentNotValidException exception){
            String message = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
            return ResponseEntity
            .badRequest()
            .body(Map.of("message",message));
        }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String,String>> handleDuplEmail(
        DuplicateEmailException exception){
            return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Map.of("message", exception.getMessage()));
        }
    
    @ExceptionHandler(InvalidCredentialsException.class)
public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of   ("message", ex.getMessage()));
    }
@ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "You do not have permission to do this"));
    }
@ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<Map<String, String>> handleForbiddenOperation(ForbiddenOperationException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
    }
@ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "That value conflicts with existing data (for example a duplicate email)"));
    }
@ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Malformed request body, or a value that is not allowed (check the role name)"));
    }
    @ExceptionHandler(TooManyAttemptsException.class)
        public ResponseEntity<Map<String, String>> handleTooMany(TooManyAttemptsException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(ex.getRetryAfterMinutes() * 60))
                    .body(Map.of("message", ex.getMessage()));
        }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("message", ex.getMessage()));
    }
}
