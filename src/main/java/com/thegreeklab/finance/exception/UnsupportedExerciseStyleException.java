package com.thegreeklab.finance.exception;

/** Indicates that a pricing model does not support the contract's exercise style. */
public class UnsupportedExerciseStyleException extends PricingException {
    /**
     * Creates an unsupported-exercise-style exception.
     *
     * @param message detail message identifying the unsupported style
     */
    public UnsupportedExerciseStyleException(String message) {
        super(message);
    }
}
