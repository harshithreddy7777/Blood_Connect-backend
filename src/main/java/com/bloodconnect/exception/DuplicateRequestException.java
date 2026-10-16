package com.bloodconnect.exception;

public class DuplicateRequestException extends RuntimeException {
    public DuplicateRequestException(String message) {
        super(message);
    }
}
