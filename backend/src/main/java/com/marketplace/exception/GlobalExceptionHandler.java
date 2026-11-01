package com.marketplace.exception;

import com.marketplace.dto.ValidationError;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<Object> notFound(ResourceNotFoundException exception, WebRequest request) {
        return respond(exception, HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), List.of(), request);
    }

    @ExceptionHandler(InvalidListingException.class)
    ResponseEntity<Object> invalidListing(InvalidListingException exception, WebRequest request) {
        return respond(exception, HttpStatus.BAD_REQUEST, "INVALID_LISTING", exception.getMessage(),
                List.of(new ValidationError(exception.getField(), exception.getMessage())), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> dataConflict(DataIntegrityViolationException exception, WebRequest request) {
        logger.warn("Database integrity conflict", exception);
        return respond(exception, HttpStatus.CONFLICT, "DATA_CONFLICT",
                "The operation conflicts with existing data", List.of(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception exception, WebRequest request) {
        logger.error("Unexpected request failure", exception);
        return respond(exception, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", List.of(), request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(ValidationError::field).thenComparing(ValidationError::message))
                .toList();
        return handleExceptionInternal(exception, problem(status, "VALIDATION_FAILED", "Request validation failed", errors),
                headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (exception.isForReturnValue()) {
            return unexpected(exception, request);
        }
        var errors = exception.getParameterValidationResults().stream().flatMap(result -> {
            String field = result.getMethodParameter().getParameterName();
            return result.getResolvableErrors().stream().map(error ->
                    new ValidationError(field == null ? "parameter" : field, error.getDefaultMessage()));
        }).toList();
        return handleExceptionInternal(exception, problem(status, "INVALID_PARAMETER", "Request parameter validation failed", errors),
                headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String field = exception instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName() : "parameter";
        return handleExceptionInternal(exception, problem(status, "INVALID_PARAMETER", "Request parameter has an invalid type",
                List.of(new ValidationError(field, "Must have the expected type"))), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(exception, problem(status, "INVALID_JSON",
                "Request body must be valid JSON and match the expected schema", List.of()), headers, status, request);
    }

    // Normalize Spring MVC errors too, including 404, 405 and 415, while preserving headers such as Allow.
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        var detail = body instanceof ProblemDetail existing ? existing
                : ProblemDetail.forStatusAndDetail(status, "The request could not be processed");
        if (detail.getInstance() == null && request instanceof ServletWebRequest servletRequest) {
            detail.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
        }
        if (detail.getProperties() == null || !detail.getProperties().containsKey("code")) {
            detail.setProperty("code", switch (status.value()) {
                case 404 -> "NOT_FOUND";
                case 405 -> "METHOD_NOT_ALLOWED";
                case 406 -> "NOT_ACCEPTABLE";
                case 415 -> "UNSUPPORTED_MEDIA_TYPE";
                default -> "HTTP_ERROR";
            });
        }
        if (!detail.getProperties().containsKey("errors")) {
            detail.setProperty("errors", List.of());
        }
        return super.createResponseEntity(detail, headers, status, request);
    }

    private ResponseEntity<Object> respond(Exception exception, HttpStatus status, String code, String message,
            List<ValidationError> errors, WebRequest request) {
        return handleExceptionInternal(exception, problem(status, code, message, errors), new HttpHeaders(), status, request);
    }

    private ProblemDetail problem(HttpStatusCode status, String code, String message, List<ValidationError> errors) {
        var detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setProperty("code", code);
        detail.setProperty("errors", errors);
        return detail;
    }
}
