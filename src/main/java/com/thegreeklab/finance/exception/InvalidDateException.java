package com.thegreeklab.finance.exception;

/** Indicates an invalid, missing or unrepresentable date or timestamp. */
public class InvalidDateException extends PricingException {
    /**
     * Creates an invalid-date exception.
     *
     * @param message detail message explaining the invalid date
     */
    public InvalidDateException(String message) {
        super(message);
    }
}
