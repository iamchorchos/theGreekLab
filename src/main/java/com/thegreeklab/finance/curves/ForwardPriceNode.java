package com.thegreeklab.finance.curves;

/**
 * One dated forward-price market observation.
 *
 * <p>Forward prices are required to be strictly positive because the Black-76
 * formula implemented by this library requires a positive forward input.</p>
 *
 * @param deliveryTimestampNanos delivery timestamp in epoch nanoseconds
 * @param forwardPrice quoted forward price for delivery at the timestamp
 */
public record ForwardPriceNode(
        long deliveryTimestampNanos,
        double forwardPrice
) {

    /**
     * Validates the forward-price domain.
     *
     * @throws IllegalArgumentException if the forward price is not strictly
     * positive and finite
     */
    public ForwardPriceNode {
        if (!(forwardPrice > 0.0) || !Double.isFinite(forwardPrice)) {
            throw new IllegalArgumentException(
                    "Forward price must be strictly positive and finite."
            );
        }
    }
}
