package com.resumeai.controller;

import com.resumeai.service.ForbiddenException;
import com.resumeai.service.TooManyRequestsException;
import com.resumeai.service.UnauthorizedException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(TooManyRequestsException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Map<String, Object> handleTooManyRequests(TooManyRequestsException ex) {
        return Map.of("detail", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArg(IllegalArgumentException ex) {
        return Map.of("detail", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleUnauthorized(UnauthorizedException ex) {
        return Map.of("detail", ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> handleForbidden(ForbiddenException ex) {
        return Map.of("detail", ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleJsonParseError(HttpMessageNotReadableException ex) {
        return Map.of("detail", "request body must be valid json");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        return Map.of("detail", firstValidationMessage(ex.getBindingResult()));
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBind(BindException ex) {
        return Map.of("detail", firstValidationMessage(ex.getBindingResult()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return Map.of("detail", "unsupported content type, use application/json or application/x-www-form-urlencoded");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleGeneral(Exception ex) {
        log.error("Unhandled error in API", ex);
        return Map.of("detail", "internal server error");
    }

    private String firstValidationMessage(BindingResult bindingResult) {
        FieldError fieldError = bindingResult.getFieldError();
        if (fieldError == null) {
            return "request validation failed";
        }
        String field = fieldError.getField();
        if ("username".equals(field)) {
            return "username format invalid, use 4-32 letters/numbers/_";
        }
        if ("password".equals(field)) {
            return "password must be at least 8 characters";
        }
        String defaultMessage = fieldError.getDefaultMessage();
        if (defaultMessage == null || defaultMessage.isBlank()) {
            return "request validation failed";
        }
        return defaultMessage;
    }
}
