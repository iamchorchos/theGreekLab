package com.thegreeklab.finance.exception;

/** Indicates that a calculation does not support the supplied option direction. */
public class UnsupportedOptionTypeException extends PricingException {
    /**
     * Creates an unsupported-option-type exception.
     *
     * @param message detail message identifying the unsupported type
     */
    public UnsupportedOptionTypeException(String message) {
        super(message);
    }
}
