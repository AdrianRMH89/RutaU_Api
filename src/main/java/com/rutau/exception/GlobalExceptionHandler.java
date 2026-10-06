package com.rutau.exception;

import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.rutau.dto.response.ApiErrorResponse;
import com.rutau.util.Messages;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    // i18n - los mensajes se traducen según el encabezado Accept-Language (es-419 / en-US)
    private final Messages messages;

    // 404 - el recurso no existe
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                           HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Not Found", messages.get(ex.getMessage(), ex.getArgs()), req);
    }

    // 400 - se rompió una regla del negocio
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(BusinessRuleException ex,
                                                               HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", messages.get(ex.getMessage(), ex.getArgs()), req);
    }

    // 409 - conflicto con el estado actual
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex,
                                                           HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "Conflict", messages.get(ex.getMessage(), ex.getArgs()), req);
    }

    // 409 - la base de datos rechazó el dato (ej. placa o correo duplicado)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                                HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "Conflict",
                messages.get("error.data.integrity"), req);
    }

    // 400 - el JSON enviado está mal escrito
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                             HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
                messages.get("error.body.invalid"), req);
    }

    // 400 - falló una validación (@NotBlank, @Email, @Min, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest req) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> Map.of(
                        "field", err.getField(),
                        "message", err.getDefaultMessage() != null ? err.getDefaultMessage() : messages.get("error.field.invalid")))
                .toList();
        ApiErrorResponse body = ApiErrorResponse.of(400, "Bad Request",
                messages.get("error.validation"), req.getRequestURI(), fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // NUEVO: 401 - correo o contraseña incorrectos (escenario de error de la US01)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex,
                                                                 HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", messages.get("error.bad.credentials"), req);
    }

    // NUEVO: 403 - cuenta deshabilitada (ej. suspendida por moderación, US19)
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabled(DisabledException ex,
                                                           HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", messages.get("error.account.disabled"), req);
    }

    // NUEVO: 403 - el usuario no tiene permiso (rol o dueño del recurso)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                               HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Forbidden",
                messages.get(ex.getMessage() != null ? ex.getMessage() : "error.access.denied"), req);
    }

    // 405 - se usó un método HTTP que el endpoint no acepta (ej. GET en vez de POST)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                                   HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed",
                messages.get("error.method.not.allowed", ex.getMethod()), req);
    }

    // 404 - la URL no existe
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException ex,
                                                             HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Not Found", messages.get("error.route.not.found"), req);
    }

    // 400 - un parámetro de la URL tiene un tipo inválido (ej. /api/trips/abc en vez de un número)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                               HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
                messages.get("error.param.invalid", ex.getName(), String.valueOf(ex.getValue())), req);
    }

    // 500 - cualquier otro error no previsto
    // 415 - el body no se envió como JSON (ej. Content-Type: text/plain)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex,
                                                            HttpServletRequest req) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type",
                messages.get("error.media.type"), req);
    }

    // 400 - falta un parámetro obligatorio de la URL o de la petición
    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<ApiErrorResponse> handleBinding(ServletRequestBindingException ex,
                                                          HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
                messages.get("error.param.missing"), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        // Se registra el error real en la consola para poder diagnosticarlo
        log.error("Error no controlado en {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                messages.get("error.unexpected"), req);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String error, String message,
                                                   HttpServletRequest req) {
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(status.value(), error, message, req.getRequestURI()));
    }
}