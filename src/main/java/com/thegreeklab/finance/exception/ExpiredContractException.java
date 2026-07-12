package com.thegreeklab.finance.exception;

public class ExpiredContractException extends PricingException {
    public ExpiredContractException(String message) {
        super(message);
    }
}
