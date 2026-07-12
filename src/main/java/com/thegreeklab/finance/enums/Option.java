package com.thegreeklab.finance.enums;

/**
 * The exercise style of an option contract.
 */
public enum Option {
    /** Exercisable only at expiration. */
    EUROPEAN,
    /** Exercisable at any time up to and including expiration. */
    AMERICAN,
    /** Non-vanilla payoff structures (e.g. barrier, Asian, lookback). */
    EXOTIC
}
