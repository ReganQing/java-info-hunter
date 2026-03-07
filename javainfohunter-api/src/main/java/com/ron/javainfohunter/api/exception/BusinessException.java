package com.ron.javainfohunter.api.exception;

/**
 * Business Exception
 *
 * Runtime exception for business logic violations and domain-specific errors.
 * Used when a business rule is violated or an expected condition is not met.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
public class BusinessException extends RuntimeException {

    /**
     * Construct with error message
     *
     * @param message Error message describing the business rule violation
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * Construct with error message and cause
     *
     * @param message Error message describing the business rule violation
     * @param cause   Root cause of the exception
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct with cause only
     *
     * @param cause Root cause of the exception
     */
    public BusinessException(Throwable cause) {
        super(cause);
    }
}
