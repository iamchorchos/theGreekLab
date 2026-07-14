package com.thegreeklab.finance.exception;

/** Indicates a non-finite or otherwise unsupported interest rate. */
public class InvalidRateException extends PricingException {
    /**
     * Creates an invalid-rate exception.
     *
     * @param message detail message explaining the invalid rate
     */
    public InvalidRateException(String message) {
        super(message);
    }
}
