package com.thegreeklab.finance.exception;

/** Indicates that a volatility calculation received too few observations. */
public class LackingDataException extends VolatilityException {
    /**
     * Creates a lacking-data exception.
     *
     * @param message detail message explaining which observations are missing
     */
    public LackingDataException(String message) {
        super(message);
    }
}
