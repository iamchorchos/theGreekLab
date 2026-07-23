package com.thegreeklab.finance.volatility;

import com.thegreeklab.finance.validation.PricingValidation;

/**
 * Volatility surface with one implied volatility at every supported expiry and
 * moneyness.
 *
 * <p>This is a compatibility adapter for pricing flows that previously passed
 * a scalar volatility directly to a model. It validates the query coordinates
 * even though the returned volatility is independent of them.</p>
 *
 * @param valuationTimestampNanos surface valuation timestamp in epoch nanoseconds
 * @param volatility flat annualized implied volatility as a decimal
 */
public record FlatVolatilitySurface(
        long valuationTimestampNanos,
        double volatility
) implements VolatilitySurface {

    /**
     * Validates the flat volatility level.
     */
    public FlatVolatilitySurface {
        PricingValidation.requireValidVolatility(volatility);
    }

    /**
     * Returns the flat volatility after validating the requested surface point.
     *
     * @param expiryTimestampNanos option expiry in epoch nanoseconds
     * @param logStrikeToForward {@code ln(K / F(T))}
     * @return the configured flat volatility
     * @throws IllegalArgumentException if expiry precedes valuation or the
     * moneyness coordinate is not finite
     */
    @Override
    public double impliedVolatility(long expiryTimestampNanos, double logStrikeToForward) {
        if (expiryTimestampNanos < valuationTimestampNanos) {
            throw new IllegalArgumentException(
                    "Volatility surface cannot be queried before valuation."
            );
        }
        if (!Double.isFinite(logStrikeToForward)) {
            throw new IllegalArgumentException(
                    "Log strike-to-forward moneyness must be finite."
            );
        }
        return volatility;
    }
}
