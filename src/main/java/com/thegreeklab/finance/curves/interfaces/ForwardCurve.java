package com.thegreeklab.finance.curves.interfaces;

/**
 * Term structure of forward prices for delivery of an underlying instrument.
 *
 * <p>A forward price is the price agreed today for delivery at a specified
 * future timestamp. Unlike a {@link DiscountCurve}, it represents the value
 * of an underlying instrument rather than the present value of a currency
 * cash flow.</p>
 */
public interface ForwardCurve {

    /**
     * Returns the timestamp at which this curve is observed.
     *
     * @return epoch nanoseconds of the curve's valuation time
     */
    long valuationTimestampNanos();

    /**
     * Returns the forward price for delivery at the supplied timestamp.
     *
     * @param deliveryTimestampNanos delivery timestamp in epoch nanoseconds
     * @return the forward price for delivery at {@code deliveryTimestampNanos}
     * @throws IllegalArgumentException if the timestamp is outside the curve's
     * supported domain
     */
    double forwardPrice(long deliveryTimestampNanos);
}
