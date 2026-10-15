package com.udea.reservations.platform.common.web;

import com.udea.reservations.platform.common.dto.ApiErrorResponse;
import com.udea.reservations.platform.common.exception.BusinessRuleException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        ApiErrorResponse body = ApiErrorResponse.of(ex.getErrorCode(), ex.getMessage(), List.of());
        log.warn("Business rule rejected: errorCode={}, traceId={}", body.errorCode(), body.traceId());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .toList();
        ApiErrorResponse body = ApiErrorResponse.of(
            "VALIDATION_ERROR", "Hay campos obligatorios o inválidos.", details);
        log.warn("Validation failed: traceId={}", body.traceId());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedBody(HttpMessageNotReadableException ex) {
        ApiErrorResponse body = ApiErrorResponse.of(
            "MALFORMED_REQUEST", "El cuerpo de la solicitud no es un JSON válido.", List.of());
        log.warn("Malformed request body: traceId={}", body.traceId());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        ApiErrorResponse body = ApiErrorResponse.of(
            "METHOD_NOT_ALLOWED", "El método HTTP no está soportado para este recurso.", List.of());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NoResourceFoundException ex) {
        ApiErrorResponse body = ApiErrorResponse.of(
            "RESOURCE_NOT_FOUND", "El recurso solicitado no existe.", List.of());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        ApiErrorResponse body = ApiErrorResponse.of(
            "INTERNAL_ERROR", "Ocurrió un error inesperado. Intenta de nuevo más tarde.", List.of());
        log.error("Unexpected error: traceId={}", body.traceId(), ex);
        return ResponseEntity.internalServerError().body(body);
    }
}