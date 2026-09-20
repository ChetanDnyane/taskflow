package com.chetan.taskflow.common.exception;

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
