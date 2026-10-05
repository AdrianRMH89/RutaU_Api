package com.rutau.exception;

import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.rutau.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 - el recurso no existe
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                           HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), req);
    }

    // 400 - se rompió una regla del negocio
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(BusinessRuleException ex,
                                                               HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), req);
    }

    // 409 - conflicto con el estado actual
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex,
                                                           HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), req);
    }

    // 409 - la base de datos rechazó el dato (ej. placa o correo duplicado)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                                HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "Conflict",
                "El registro viola una restricción de datos (posible duplicado)", req);
    }

    // 400 - el JSON enviado está mal escrito
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                             HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
                "Cuerpo de la petición inválido o mal formado", req);
    }

    // 400 - falló una validación (@NotBlank, @Email, @Min, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest req) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> Map.of(
                        "field", err.getField(),
                        "message", err.getDefaultMessage() != null ? err.getDefaultMessage() : "inválido"))
                .toList();
        ApiErrorResponse body = ApiErrorResponse.of(400, "Bad Request",
                "Error de validación", req.getRequestURI(), fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // NUEVO: 401 - correo o contraseña incorrectos (escenario de error de la US01)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex,
                                                                 HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", "Correo o contraseña incorrectos", req);
    }

    // NUEVO: 403 - cuenta deshabilitada (ej. suspendida por moderación, US19)
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabled(DisabledException ex,
                                                           HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", "Tu cuenta está suspendida o deshabilitada", req);
    }

    // NUEVO: 403 - el usuario no tiene permiso (rol o dueño del recurso)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                               HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Forbidden",
                ex.getMessage() != null ? ex.getMessage() : "Acceso denegado", req);
    }

    // 405 - se usó un método HTTP que el endpoint no acepta (ej. GET en vez de POST)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                                   HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed",
                "El método " + ex.getMethod() + " no está permitido para esta ruta", req);
    }

    // 404 - la URL no existe
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException ex,
                                                             HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Not Found", "La ruta solicitada no existe", req);
    }

    // 400 - un parámetro de la URL tiene un tipo inválido (ej. /api/trips/abc en vez de un número)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                               HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
                "El parámetro '" + ex.getName() + "' tiene un valor inválido: " + ex.getValue(), req);
    }

    // 500 - cualquier otro error no previsto
    // 415 - el body no se envió como JSON (ej. Content-Type: text/plain)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex,
                                                            HttpServletRequest req) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type",
                "El cuerpo de la petición debe enviarse como JSON (Content-Type: application/json)", req);
    }

    // 400 - falta un parámetro obligatorio de la URL o de la petición
    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<ApiErrorResponse> handleBinding(ServletRequestBindingException ex,
                                                          HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
                "Falta un parámetro obligatorio en la petición", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        // Se registra el error real en la consola para poder diagnosticarlo
        log.error("Error no controlado en {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Ocurrió un error inesperado", req);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String error, String message,
                                                   HttpServletRequest req) {
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(status.value(), error, message, req.getRequestURI()));
    }
}