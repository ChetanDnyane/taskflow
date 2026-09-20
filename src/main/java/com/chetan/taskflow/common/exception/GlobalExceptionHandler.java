package com.chetan.taskflow.common.exception;

// <editor-fold defaultstate="collapsed" desc="Translate controller failures into API error responses">
/*
 * RestControllerAdvice applies across controllers. Each ExceptionHandler selects a status and a
 * JSON object with a stable error code plus a human-readable message. Validation also includes
 * fieldErrors. Login authentication failures intentionally share one message for unknown users and
 * wrong passwords. Missing and foreign-owned tasks share TASK_NOT_FOUND so ownership is not exposed.
 * Security filter failures use handlers in SecurityConfig or JwtAuthenticationFilter instead.
 */
// </editor-fold>

import com.chetan.taskflow.task.TaskNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<Map<String, String>>
    handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException exception
    ) {

        Map<String, String> response = Map.of(
                "error", "EMAIL_ALREADY_REGISTERED",
                "message", exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    // <editor-fold defaultstate="collapsed" desc="Collect one validation message per field">
    /*
     * LinkedHashMap preserves encounter order. putIfAbsent keeps the first reported constraint
     * for a field instead of overwriting it with another failure. The envelope combines the
     * VALIDATION_FAILED code, a summary, and per-field messages for client form feedback.
     * The existing println is diagnostic output and is not part of the HTTP response.
     */
    // </editor-fold>
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>>
    handleValidationErrors(
            MethodArgumentNotValidException exception
    ) {

        System.out.println(">>> VALIDATION HANDLER CALLED <<<");

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        fieldErrors.putIfAbsent(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", "VALIDATION_FAILED");
        response.put("message", "Request validation failed");
        response.put("fieldErrors", fieldErrors);

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(
            AuthenticationException exception) {

        Map<String, String> response = Map.of(
                "error", "INVALID_CREDENTIALS",
                "message", "Invalid email or password"
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleTaskNotFound(
            TaskNotFoundException exception) {

        Map<String, String> response = Map.of(
                "error", "TASK_NOT_FOUND",
                "message", exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }
}
