package com.thegreeklab.finance.exception;

/** Base exception for volatility estimation and calibration failures. */
public class VolatilityException extends PricingException {
    /**
     * Creates a volatility-calculation exception.
     *
     * @param message detail message explaining the failure
     */
    public VolatilityException(String message) {
        super(message);
    }
}
