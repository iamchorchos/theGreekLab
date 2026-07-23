package com.thegreeklab.finance.curves;

import com.thegreeklab.finance.curves.interfaces.ForwardCurve;

import java.util.List;
import java.util.Objects;

/**
 * Forward curve defined by directly quoted forward-price nodes and log-linear
 * interpolation.
 *
 * <p>The first node must be at the valuation timestamp, making the current
 * forward price {@code F(t0)} explicit. Subsequent nodes must be strictly
 * ordered in time. Between nodes, the curve linearly interpolates
 * {@code ln(F)}, which preserves positivity. The curve does not extrapolate
 * beyond its final node.</p>
 *
 * @param valuationTimestampNanos epoch nanoseconds at which the curve is observed
 * @param nodes forward-price observations, beginning at valuation
 */
public record InterpolatedForwardCurve(
        long valuationTimestampNanos,
        List<ForwardPriceNode> nodes
) implements ForwardCurve {

    /**
     * Copies and validates the forward-price nodes.
     *
     * @throws NullPointerException if the node list or a node is {@code null}
     * @throws IllegalArgumentException if the list is empty, the first node is
     * not at valuation, or timestamps are not strictly increasing
     */
    public InterpolatedForwardCurve {
        Objects.requireNonNull(nodes, "Forward-price nodes cannot be null.");
        nodes = List.copyOf(nodes);
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one forward-price node is required."
            );
        }
        if (nodes.getFirst().deliveryTimestampNanos() != valuationTimestampNanos) {
            throw new IllegalArgumentException(
                    "The first forward-price node must be at the valuation timestamp."
            );
        }

        long previousTimestamp = valuationTimestampNanos;
        for (int index = 0; index < nodes.size(); index++) {
            ForwardPriceNode node = nodes.get(index);
            Objects.requireNonNull(node, "Forward-price nodes cannot contain null.");
            if (index > 0 && node.deliveryTimestampNanos() <= previousTimestamp) {
                throw new IllegalArgumentException(
                        "Forward-price node timestamps must be strictly increasing."
                );
            }
            previousTimestamp = node.deliveryTimestampNanos();
        }
    }

    /**
     * Returns the direct forward quote at a node or an interpolated forward
     * price between nodes.
     *
     * @param deliveryTimestampNanos delivery timestamp in epoch nanoseconds
     * @return forward price for delivery at the requested timestamp
     * @throws IllegalArgumentException if the timestamp precedes valuation or
     * exceeds the final curve node
     */
    @Override
    public double forwardPrice(long deliveryTimestampNanos) {
        if (deliveryTimestampNanos < valuationTimestampNanos) {
            throw new IllegalArgumentException(
                    "Curve cannot return a forward price before valuation."
            );
        }

        ForwardPriceNode lastNode = nodes.getLast();
        if (deliveryTimestampNanos > lastNode.deliveryTimestampNanos()) {
            throw new IllegalArgumentException(
                    "Curve cannot extrapolate beyond its final node."
            );
        }

        ForwardPriceNode leftNode = nodes.getFirst();
        for (ForwardPriceNode rightNode : nodes) {
            if (deliveryTimestampNanos == rightNode.deliveryTimestampNanos()) {
                return rightNode.forwardPrice();
            }
            if (deliveryTimestampNanos < rightNode.deliveryTimestampNanos()) {
                return CurveInterpolation.logLinear(
                        deliveryTimestampNanos,
                        leftNode.deliveryTimestampNanos(),
                        leftNode.forwardPrice(),
                        rightNode.deliveryTimestampNanos(),
                        rightNode.forwardPrice()
                );
            }
            leftNode = rightNode;
        }

        throw new IllegalStateException("Timestamp was not resolved on the curve.");
    }

}
