package com.thegreeklab.finance.curves.interfaces;

import com.thegreeklab.finance.time.DayCountConvention;

/**
 * Term structure of discount factors observed at one valuation timestamp.
 *
 * <p>A discount factor expresses the present value of one unit of currency
 * paid at a future timestamp. Implementations must return {@code 1.0} at
 * {@link #valuationTimestampNanos()} and a strictly positive, finite value for
 * every supported future timestamp.</p>
 *
 * <p>All timestamps are epoch nanoseconds, preserving the intraday valuation
 * convention used by the option-pricing models.</p>
 */
public interface DiscountCurve {

    /**
     * Returns the timestamp at which this curve is observed.
     *
     * @return epoch nanoseconds of the curve's valuation time
     */
    long valuationTimestampNanos();

    /**
     * Returns the present value of one currency unit payable at the supplied
     * timestamp.
     *
     * @param timestampNanos future cash-flow timestamp in epoch nanoseconds
     * @return the strictly positive discount factor for {@code timestampNanos}
     * @throws IllegalArgumentException if the timestamp is outside the curve's
     * supported domain
     */
    double discountFactor(long timestampNanos);

    /**
     * Derives the continuously compounded zero rate between the curve's
     * valuation time and a future timestamp.
     *
     * <p>The rate is calculated as {@code -ln(DF(T)) / T}, where {@code T} is
     * measured by the supplied day-count convention.</p>
     *
     * @param timestampNanos future timestamp in epoch nanoseconds
     * @param convention convention used to convert elapsed time to years
     * @return the continuously compounded zero rate
     * @throws IllegalArgumentException if the timestamp is not after valuation
     */
    default double zeroRate(
            long timestampNanos,
            DayCountConvention convention
    ) {
        double time = convention.yearFraction(
                valuationTimestampNanos(),
                timestampNanos
        );

        if (time <= 0.0) {
            throw new IllegalArgumentException(
                    "Curve timestamp must be after valuation timestamp."
            );
        }

        return -Math.log(discountFactor(timestampNanos)) / time;
    }
}
