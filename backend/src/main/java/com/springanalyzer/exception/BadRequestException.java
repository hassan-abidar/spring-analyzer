package com.springanalyzer.exception;

public class BadRequestException extends SpringAnalyzerException {

    public BadRequestException(String message) {
        super(message, "BAD_REQUEST");
    }

    public BadRequestException(String message, String details) {
        super(message, "BAD_REQUEST", details);
    }
}
