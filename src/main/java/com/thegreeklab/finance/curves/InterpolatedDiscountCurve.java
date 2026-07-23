package com.thegreeklab.finance.curves;

import com.thegreeklab.finance.curves.interfaces.DiscountCurve;

import java.util.List;
import java.util.Objects;

/**
 * Discount curve defined by dated discount-factor nodes and log-linear
 * interpolation.
 *
 * <p>The valuation timestamp is an implicit node with {@code DF(t0) = 1.0}.
 * Between nodes, the curve linearly interpolates {@code ln(DF)} in elapsed
 * time. This produces positive discount factors and is the standard simple
 * interpolation choice for a discount curve.</p>
 *
 * <p>Nodes must be strictly ordered after valuation. The curve does not
 * extrapolate beyond its final node.</p>
 *
 * @param valuationTimestampNanos epoch nanoseconds at which the curve is observed
 * @param nodes strictly time-ordered discount-factor observations after valuation
 */
public record InterpolatedDiscountCurve(
        long valuationTimestampNanos,
        List<DiscountFactorNode> nodes
) implements DiscountCurve {

    /**
     * Copies and validates the curve nodes.
     *
     * @throws NullPointerException if the node list or a node is {@code null}
     * @throws IllegalArgumentException if the list is empty, a node is not after
     * valuation, or node timestamps are not strictly increasing
     */
    public InterpolatedDiscountCurve {
        Objects.requireNonNull(nodes, "Discount-factor nodes cannot be null.");
        nodes = List.copyOf(nodes);
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one discount-factor node is required."
            );
        }

        long previousTimestamp = valuationTimestampNanos;
        for (DiscountFactorNode node : nodes) {
            Objects.requireNonNull(node, "Discount-factor nodes cannot contain null.");
            if (node.timestampNanos() <= valuationTimestampNanos) {
                throw new IllegalArgumentException(
                        "Discount-factor nodes must be after the valuation timestamp."
                );
            }
            if (node.timestampNanos() <= previousTimestamp) {
                throw new IllegalArgumentException(
                        "Discount-factor node timestamps must be strictly increasing."
                );
            }
            previousTimestamp = node.timestampNanos();
        }
    }

    /**
     * Returns the discount factor at a timestamp on the curve.
     *
     * @param timestampNanos cash-flow timestamp in epoch nanoseconds
     * @return {@code 1.0} at valuation, a node value at a node timestamp, or a
     * log-linearly interpolated value between nodes
     * @throws IllegalArgumentException if the timestamp precedes valuation or
     * exceeds the final curve node
     */
    @Override
    public double discountFactor(long timestampNanos) {
        if (timestampNanos < valuationTimestampNanos) {
            throw new IllegalArgumentException(
                    "Curve cannot discount to a timestamp before valuation."
            );
        }
        if (timestampNanos == valuationTimestampNanos) {
            return 1.0;
        }

        DiscountFactorNode lastNode = nodes.getLast();
        if (timestampNanos > lastNode.timestampNanos()) {
            throw new IllegalArgumentException(
                    "Curve cannot extrapolate beyond its final node."
            );
        }

        long leftTimestamp = valuationTimestampNanos;
        double leftDiscountFactor = 1.0;
        for (DiscountFactorNode rightNode : nodes) {
            if (timestampNanos == rightNode.timestampNanos()) {
                return rightNode.discountFactor();
            }
            if (timestampNanos < rightNode.timestampNanos()) {
                return CurveInterpolation.logLinear(
                        timestampNanos,
                        leftTimestamp,
                        leftDiscountFactor,
                        rightNode.timestampNanos(),
                        rightNode.discountFactor()
                );
            }
            leftTimestamp = rightNode.timestampNanos();
            leftDiscountFactor = rightNode.discountFactor();
        }

        throw new IllegalStateException("Timestamp was not resolved on the curve.");
    }

}
