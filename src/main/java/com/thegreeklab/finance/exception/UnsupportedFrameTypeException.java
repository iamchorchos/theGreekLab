package com.thegreeklab.finance.exception;

/** Indicates that a pricing model does not support the supplied market-data frame. */
public class UnsupportedFrameTypeException extends PricingException {
    /**
     * Creates an unsupported-frame-type exception.
     *
     * @param message detail message identifying the unsupported frame
     */
    public UnsupportedFrameTypeException(String message) {
        super(message);
    }
}
