package com.thegreeklab.finance.curves;

/**
 * One dated discount-factor market observation.
 *
 * <p>The timestamp is interpreted relative to the valuation timestamp of the
 * curve that consumes this node. A discount factor may exceed {@code 1.0} when
 * interest rates are negative, but it must always be strictly positive and
 * finite.</p>
 *
 * @param timestampNanos cash-flow timestamp in epoch nanoseconds
 * @param discountFactor present value of one currency unit paid at the timestamp
 */
public record DiscountFactorNode(
        long timestampNanos,
        double discountFactor
) {

    /**
     * Validates the discount-factor domain.
     *
     * @throws IllegalArgumentException if the discount factor is not strictly
     * positive and finite
     */
    public DiscountFactorNode {
        if (!(discountFactor > 0.0) || !Double.isFinite(discountFactor)) {
            throw new IllegalArgumentException(
                    "Discount factor must be strictly positive and finite."
            );
        }
    }
}
