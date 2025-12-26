package com.springanalyzer.exception;

import lombok.Getter;

@Getter
public class SpringAnalyzerException extends RuntimeException {

    private final String errorCode;
    private final String details;

    public SpringAnalyzerException(String message) {
        super(message);
        this.errorCode = "SPRING_ANALYZER_ERROR";
        this.details = null;
    }

    public SpringAnalyzerException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public SpringAnalyzerException(String message, String errorCode, String details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public SpringAnalyzerException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "SPRING_ANALYZER_ERROR";
        this.details = cause.getMessage();
    }
}
