package com.thegreeklab.finance.exception;

/** Indicates a price, strike or multiplier that must be positive but is not. */
public class NonPositivePriceException extends PricingException {
    /**
     * Creates a non-positive-price exception.
     *
     * @param message detail message identifying the invalid value
     */
    public NonPositivePriceException(String message) {
        super(message);
    }
}
