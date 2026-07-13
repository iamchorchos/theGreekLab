package com.thegreeklab.finance.validation;

import com.thegreeklab.finance.exception.InvalidVolatilityException;

/**
 * Shared validation rules for option-pricing model inputs.
 */
public final class PricingValidation {

    /** Lowest volatility accepted by the pricing engines. */
    public static final double MIN_VOLATILITY = 1e-6;

    private PricingValidation() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Ensures that an annualized volatility is finite and within the supported domain.
     *
     * @param volatility annualized volatility as a decimal
     * @throws InvalidVolatilityException if {@code volatility} is not finite or is
     *                                    below {@link #MIN_VOLATILITY}
     */
    public static void requireValidVolatility(double volatility) {
        if (!Double.isFinite(volatility) || volatility < MIN_VOLATILITY) {
            throw new InvalidVolatilityException(
                    "Volatility must be strictly positive and finite. Received: "
                            + volatility
            );
        }
    }
}
