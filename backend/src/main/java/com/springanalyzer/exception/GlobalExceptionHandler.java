package com.springanalyzer.exception;

import com.springanalyzer.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), ex.getErrorCode(), ex.getDetails(), 
                request.getRequestURI(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), ex.getErrorCode(), ex.getDetails(), 
                request.getRequestURI(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Validation error: {}", errors);
        return buildErrorResponse("Validation failed", "VALIDATION_ERROR", errors, 
                request.getRequestURI(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("File too large");
        return buildErrorResponse("File size exceeds limit", "FILE_SIZE_EXCEEDED", 
                "Maximum upload size is 500MB", request.getRequestURI(), HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(SpringAnalyzerException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(
            SpringAnalyzerException ex, HttpServletRequest request) {
        log.error("Application error: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), ex.getErrorCode(), ex.getDetails(), 
                request.getRequestURI(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildErrorResponse("Internal server error", "INTERNAL_SERVER_ERROR", 
                "An unexpected error occurred", request.getRequestURI(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(
            String message, String code, String details, String path, HttpStatus status) {
        ApiResponse.ErrorDetails errorDetails = new ApiResponse.ErrorDetails(code, details, path);
        return ResponseEntity.status(status).body(ApiResponse.error(message, errorDetails));
    }
}
