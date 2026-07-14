package com.thegreeklab.finance.exception;

/** Indicates an invalid number of steps for a discrete pricing model. */
public class InvalidStepCountException extends PricingException {
    /**
     * Creates an invalid-step-count exception.
     *
     * @param message detail message explaining the invalid step count
     */
    public InvalidStepCountException(String message) {
        super(message);
    }
}
