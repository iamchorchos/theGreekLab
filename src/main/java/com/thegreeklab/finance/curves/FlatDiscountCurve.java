package com.thegreeklab.finance.curves;

import com.thegreeklab.finance.curves.interfaces.DiscountCurve;
import com.thegreeklab.finance.time.DayCountConvention;
import net.jafama.FastMath;

/**
 * Discount curve with one continuously compounded rate for every maturity.
 *
 * <p>The discount factor is {@code exp(-rT)}, where {@code r} is the supplied
 * continuously compounded rate and {@code T} is measured using the supplied
 * day-count convention. This class is primarily a simple market-data adapter
 * for flat-rate models and a compatibility bridge from scalar rate inputs.</p>
 *
 * @param valuationTimestampNanos epoch nanoseconds at which the curve is observed
 * @param continuouslyCompoundedRate flat continuously compounded annual rate;
 *                                    it may be negative but must be finite
 * @param dayCountConvention convention used to calculate time from valuation
 */
public record FlatDiscountCurve  (
    long valuationTimestampNanos,
    double continuouslyCompoundedRate,
    DayCountConvention dayCountConvention
) implements DiscountCurve {

    /**
     * Validates immutable curve inputs.
     *
     * @throws IllegalArgumentException if the rate is not finite
     * @throws NullPointerException if the day-count convention is {@code null}
     */
    public FlatDiscountCurve {
        if (!Double.isFinite(continuouslyCompoundedRate)) {
            throw new IllegalArgumentException("Rate must be finite.");
        }
        if (dayCountConvention == null) {
            throw new NullPointerException("Day-count convention cannot be null.");
        }
    }

    /**
     * Calculates {@code exp(-rT)} for a timestamp at or after valuation.
     *
     * @param timestampNanos future cash-flow timestamp in epoch nanoseconds
     * @return the corresponding discount factor
     * @throws IllegalArgumentException if {@code timestampNanos} precedes the
     * valuation timestamp
     */
    @Override
    public double discountFactor(long timestampNanos) {
        if (timestampNanos < valuationTimestampNanos) {
            throw new IllegalArgumentException(
                    "Curve cannot discount to a timestamp before valuation."
            );
        }

        double time = dayCountConvention.yearFraction(
                valuationTimestampNanos,
                timestampNanos
        );

        return FastMath.exp(-continuouslyCompoundedRate * time);
    }
}
