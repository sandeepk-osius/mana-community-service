package com.manacommunity.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler — intercepts every exception thrown by any
 * @RestController and converts it into a consistent ErrorResponse JSON body.
 *
 * Handler priority (top to bottom):
 *   1. ManaCommunityException subtypes — business-specific errors
 *   2. Spring Security exceptions     — authentication / authorisation
 *   3. Spring MVC validation errors   — @Valid / @RequestParam / type mismatch
 *   4. Generic fallback               — any unhandled Throwable → 500
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── 1. Business Exceptions ──────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
            DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidInviteCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInvite(
            InvalidInviteCodeException ex, HttpServletRequest request) {
        log.warn("Invalid invite code: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(RegistrationClosedException.class)
    public ResponseEntity<ErrorResponse> handleRegistrationClosed(
            RegistrationClosedException ex, HttpServletRequest request) {
        log.warn("Registration closed: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(EventFullException.class)
    public ResponseEntity<ErrorResponse> handleEventFull(
            EventFullException ex, HttpServletRequest request) {
        log.warn("Event full: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(AlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyRegistered(
            AlreadyRegisteredException ex, HttpServletRequest request) {
        log.warn("Already registered: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(AgeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleAgeMismatch(
            AgeMismatchException ex, HttpServletRequest request) {
        log.warn("Age mismatch: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(InsufficientBudgetException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBudget(
            InsufficientBudgetException ex, HttpServletRequest request) {
        log.warn("Insufficient budget: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidBidAmountException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBid(
            InvalidBidAmountException ex, HttpServletRequest request) {
        log.warn("Invalid bid: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedActionException ex, HttpServletRequest request) {
        log.warn("Unauthorized action: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(NoPlayersInQueueException.class)
    public ResponseEntity<ErrorResponse> handleNoPlayers(
            NoPlayersInQueueException ex, HttpServletRequest request) {
        log.warn("No players in queue: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    /** Catch-all for any other ManaCommunityException subtype. */
    @ExceptionHandler(ManaCommunityException.class)
    public ResponseEntity<ErrorResponse> handleManaCommunity(
            ManaCommunityException ex, HttpServletRequest request) {
        log.error("Business exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    // ─── 2. Spring Security Exceptions ───────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied to {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "You do not have permission to perform this action.", request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failure at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED",
                "Authentication required. Please log in and try again.", request, null);
    }

    // ─── 3. Validation Exceptions ────────────────────────────────────────────

    /**
     * Handles @Valid failures on @RequestBody DTOs.
     * Returns per-field validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .rejectedValue(fe.getRejectedValue() != null
                                ? fe.getRejectedValue().toString() : null)
                        .message(fe.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        log.warn("Validation failed on {} field(s) at {}", fieldErrors.size(), request.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Request validation failed. Check 'fieldErrors' for details.", request, fieldErrors);
    }

    /**
     * Handles @Validated failures on @RequestParam / @PathVariable.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(cv -> ErrorResponse.FieldError.builder()
                        .field(extractField(cv))
                        .rejectedValue(cv.getInvalidValue() != null
                                ? cv.getInvalidValue().toString() : null)
                        .message(cv.getMessage())
                        .build())
                .collect(Collectors.toList());

        return build(HttpStatus.BAD_REQUEST, "CONSTRAINT_VIOLATION",
                "One or more parameters are invalid.", request, fieldErrors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                "Required parameter '" + ex.getParameterName() + "' is missing.", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        return build(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH",
                "Parameter '" + ex.getName() + "' must be of type " + expected + ".", request, null);
    }

    // ─── 4. Generic Fallback ─────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later or contact support.", request, null);
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String errorCode, String message,
            HttpServletRequest request, List<ErrorResponse.FieldError> fieldErrors) {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(errorCode)
                .message(message)
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(status).body(body);
    }

    private String extractField(ConstraintViolation<?> cv) {
        String path = cv.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }
}
