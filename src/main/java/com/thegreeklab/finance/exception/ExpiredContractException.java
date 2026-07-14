package com.thegreeklab.finance.exception;

/** Indicates that a calculation requires a contract with positive time to expiry. */
public class ExpiredContractException extends PricingException {
    /**
     * Creates an expired-contract exception.
     *
     * @param message detail message explaining the invalid expiry state
     */
    public ExpiredContractException(String message) {
        super(message);
    }
}
