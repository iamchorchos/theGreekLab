package com.thegreeklab.finance.exception;

/** Indicates a non-finite, non-positive or otherwise unsupported volatility. */
public class InvalidVolatilityException extends PricingException {
    /**
     * Creates an invalid-volatility exception.
     *
     * @param message detail message explaining the invalid volatility
     */
    public InvalidVolatilityException(String message) {
        super(message);
    }
}
