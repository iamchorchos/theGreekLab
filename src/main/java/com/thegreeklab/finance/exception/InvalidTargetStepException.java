package com.thegreeklab.finance.exception;

/** Indicates a requested tree step outside the valid pricing range. */
public class InvalidTargetStepException extends PricingException {
    /**
     * Creates an invalid-target-step exception.
     *
     * @param message detail message explaining the invalid target step
     */
    public InvalidTargetStepException(String message) {
        super(message);
    }
}
