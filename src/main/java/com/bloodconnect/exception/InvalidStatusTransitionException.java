package com.bloodconnect.exception;

/**
 * Thrown when an illegal status transition is attempted on a BloodRequest.
 */
public class InvalidStatusTransitionException extends IllegalStateException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
