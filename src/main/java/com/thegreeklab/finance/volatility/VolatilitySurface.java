package com.thegreeklab.finance.volatility;

/**
 * Implied-volatility market data indexed by expiry and log strike-to-forward
 * moneyness.
 *
 * <p>The moneyness coordinate is {@code ln(K / F(T))}, where {@code K} is the
 * option strike and {@code F(T)} is the forward price at the requested expiry.
 * Using a forward-relative coordinate makes the surface independent of the
 * spot/carry decomposition used to obtain that forward.</p>
 */
public interface VolatilitySurface {

    /**
     * Returns the timestamp at which this surface is observed.
     *
     * @return valuation timestamp in epoch nanoseconds
     */
    long valuationTimestampNanos();

    /**
     * Returns the implied volatility at an expiry and log strike-to-forward
     * moneyness.
     *
     * @param expiryTimestampNanos option expiry in epoch nanoseconds
     * @param logStrikeToForward {@code ln(K / F(T))}
     * @return annualized implied volatility as a decimal
     * @throws IllegalArgumentException if the requested point is outside the
     * surface domain
     */
    double impliedVolatility(long expiryTimestampNanos, double logStrikeToForward);
}
