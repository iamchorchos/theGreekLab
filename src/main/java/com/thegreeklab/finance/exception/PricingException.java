package com.thegreeklab.finance.exception;

/**
 * Base unchecked exception for option-pricing and market-data validation errors.
 */
public class PricingException extends RuntimeException {
    /**
     * Creates an exception with a descriptive message.
     *
     * @param message detail message explaining the failure
     */
    public PricingException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a descriptive message and underlying cause.
     *
     * @param message detail message explaining the failure
     * @param cause   underlying cause
     */
    public PricingException(String message, Throwable cause) {
        super(message, cause);
    }
}
