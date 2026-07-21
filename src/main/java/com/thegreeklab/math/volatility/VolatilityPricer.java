package com.thegreeklab.math.volatility;

/**
 * Pricing model that can be evaluated at an arbitrary trial volatility.
 * Implementations are used by the model-independent implied-volatility solver.
 */
@FunctionalInterface
public interface VolatilityPricer {

    /**
     * Prices the same instrument and market snapshot at a trial volatility.
     *
     * @param volatility annualized volatility as a decimal
     * @return model price at the supplied volatility
     */
    double priceAtVolatility(double volatility);
}
