package com.thegreeklab.finance.enums;

/**
 * The exercise direction of a vanilla option contract.
 */
public enum OptionType {
    /** Right to buy the underlying at the strike price. */
    CALL,
    /** Right to sell the underlying at the strike price. */
    PUT
}
