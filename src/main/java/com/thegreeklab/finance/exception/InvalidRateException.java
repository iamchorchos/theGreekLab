package com.thegreeklab.finance.exception;

public class InvalidRateException extends PricingException {
    public InvalidRateException(String message) {
        super(message);
    }
}
