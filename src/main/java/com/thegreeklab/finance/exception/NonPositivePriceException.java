package com.thegreeklab.finance.exception;

public class NonPositivePriceException extends PricingException {
    public NonPositivePriceException(String message) {
        super(message);
    }
}
