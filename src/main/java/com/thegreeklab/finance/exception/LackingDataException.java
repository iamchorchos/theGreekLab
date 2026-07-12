package com.thegreeklab.finance.exception;

public class LackingDataException extends VolatilityException {
    public LackingDataException(String message) {
        super(message);
    }
}
