package com.omniguard.adapter.in.rest.exception;

import com.omniguard.adapter.in.rest.dto.common.ErrorResponse;
import com.omniguard.domain.exception.AuthenticationFailedException;
import com.omniguard.domain.exception.AuthorizationDeniedException;
import com.omniguard.domain.exception.BusinessRuleViolationException;
import com.omniguard.domain.exception.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        log.warn("Entidad no encontrada: {} - {}", ex.getEntityType(), ex.getIdentifier());
        return buildError(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolation(BusinessRuleViolationException ex, HttpServletRequest request) {
        log.warn("Violación de regla de negocio: {} - {}", ex.getRuleCode(), ex.getMessage());
        return buildError(HttpStatus.CONFLICT, "Business Rule Violation", ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailed(AuthenticationFailedException ex, HttpServletRequest request) {
        log.warn("Autenticación fallida: {}", ex.getMessage());
        return buildError(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(AuthorizationDeniedException ex, HttpServletRequest request) {
        log.warn("Acceso denegado: {} - {}", ex.getRequiredRole(), ex.getCurrentRole());
        return buildError(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acceso denegado (Spring Security): {}", ex.getMessage());
        return buildError(HttpStatus.FORBIDDEN, "Forbidden", "Acceso denegado", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (existing, replacement) -> existing
                ));
        log.warn("Errores de validación: {}", errors);
        return buildError(HttpStatus.BAD_REQUEST, "Validation Error", "Datos de entrada inválidos", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        jakarta.validation.ConstraintViolation::getMessage,
                        (existing, replacement) -> existing
                ));
        log.warn("Violación de constraints: {}", errors);
        return buildError(HttpStatus.BAD_REQUEST, "Validation Error", "Parámetros inválidos", request, errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Error interno no manejado", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Error interno del servidor", request);
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String error, String message, HttpServletRequest request) {
        return buildError(status, error, message, request, null);
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String error, String message, HttpServletRequest request, Map<String, String> validationErrors) {
        String requestId = request.getHeader("X-Request-ID");
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .validationErrors(validationErrors)
                .requestId(requestId)
                .build();
        return ResponseEntity.status(status).body(response);
    }
}