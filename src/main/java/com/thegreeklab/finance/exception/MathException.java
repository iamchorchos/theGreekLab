package com.thegreeklab.finance.exception;

/** Indicates a numerical failure or an unstable model configuration. */
public class MathException extends PricingException {
    /**
     * Creates a numerical-calculation exception.
     *
     * @param message detail message explaining the numerical failure
     */
    public MathException(String message) {
        super(message);
    }
}
