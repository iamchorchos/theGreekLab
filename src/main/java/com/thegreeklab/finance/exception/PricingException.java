package com.thegreeklab.finance.exception;

/**
 * Base exception for all market data related errors.
 */
public class PricingException extends RuntimeException {
    public PricingException(String message) {
        super(message);
    }

    public PricingException(String message, Throwable cause) {
        super(message, cause);
    }
}