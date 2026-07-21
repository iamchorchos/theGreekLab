package com.thegreeklab.finance.exception;

/**
 * Indicates that a numerical model cannot be evaluated at a trial parameter
 * value even though the surrounding calibration problem may still be valid.
 */
public class InvalidModelDomainException extends MathException {

    /**
     * Creates an exception for a point outside a model's numerical domain.
     *
     * @param message detail message describing the invalid point
     */
    public InvalidModelDomainException(String message) {
        super(message);
    }
}
