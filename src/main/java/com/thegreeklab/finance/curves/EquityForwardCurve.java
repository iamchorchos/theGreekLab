package com.thegreeklab.finance.curves;

import com.thegreeklab.finance.curves.interfaces.ForwardCurve;

import java.util.Objects;

/**
 * Equity forward curve implied by spot, funding and continuous dividend carry.
 *
 * <p>The forward price is calculated as {@code F(T) = S * Dq(T) / Dr(T)},
 * where {@code S} is spot, {@code Dq(T)} is the dividend discount factor and
 * {@code Dr(T)} is the funding discount factor. With flat rates this reduces
 * to {@code F(T) = S * exp((r - q)T)}.</p>
 *
 * <p>Both input curves must have the same valuation timestamp as this curve.
 * This prevents combining market data observed at different points in time.</p>
 *
 * @param valuationTimestampNanos epoch nanoseconds at which all inputs are observed
 * @param spotPrice strictly positive, finite equity spot price
 * @param fundingCurve funding discount curve {@code Dr}
 * @param dividendYieldCurve continuous-dividend discount curve {@code Dq}
 */
public record EquityForwardCurve(
        long valuationTimestampNanos,
        double spotPrice,
        FundingCurve fundingCurve,
        DividendYieldCurve dividendYieldCurve
) implements ForwardCurve {

    /**
     * Validates market-data inputs and their common valuation timestamp.
     *
     * @throws IllegalArgumentException if spot is invalid or a curve has a
     * different valuation timestamp
     * @throws NullPointerException if either input curve is {@code null}
     */
    public EquityForwardCurve {
        if (!(spotPrice > 0.0) || !Double.isFinite(spotPrice)) {
            throw new IllegalArgumentException(
                    "Spot price must be strictly positive and finite."
            );
        }

        Objects.requireNonNull(fundingCurve, "Funding curve cannot be null.");
        Objects.requireNonNull(dividendYieldCurve, "Dividend-yield curve cannot be null.");

        if (fundingCurve.valuationTimestampNanos()
                != valuationTimestampNanos
                || dividendYieldCurve.valuationTimestampNanos()
                != valuationTimestampNanos) {
            throw new IllegalArgumentException(
                    "All curves must share the forward curve valuation timestamp."
            );
        }
    }

    /**
     * Returns the no-arbitrage equity forward price for the delivery timestamp.
     *
     * @param deliveryTimestampNanos delivery timestamp in epoch nanoseconds
     * @return {@code spotPrice * Dq(T) / Dr(T)}
     * @throws IllegalArgumentException if delivery precedes valuation
     */
    @Override
    public double forwardPrice(long deliveryTimestampNanos) {
        if (deliveryTimestampNanos < valuationTimestampNanos) {
            throw new IllegalArgumentException(
                    "Delivery timestamp cannot precede valuation."
            );
        }

        return spotPrice
                * dividendYieldCurve.discountFactor(deliveryTimestampNanos)
                / fundingCurve.discountFactor(deliveryTimestampNanos);
    }
}
