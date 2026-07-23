package com.thegreeklab.visualization.volatility;

import java.util.List;
import java.util.Objects;

/**
 * Immutable rectangular sample of an implied-volatility surface.
 *
 * <p>Rows correspond to increasing expiry timestamps and columns correspond
 * to increasing {@code ln(K / F(T))} values. This type contains presentation
 * data only; it does not interpolate, calibrate, or otherwise alter the
 * supplied market-data surface.</p>
 */
public final class VolatilitySurfaceGrid {

    private final List<Long> expiryTimestampsNanos;
    private final List<Double> logStrikeToForwards;
    private final double[][] impliedVolatilities;

    /**
     * Creates an immutable surface sample.
     *
     * @param expiryTimestampsNanos increasing expiry timestamps
     * @param logStrikeToForwards increasing log strike-to-forward coordinates
     * @param impliedVolatilities volatility values indexed by expiry then
     * moneyness
     * @throws NullPointerException if an argument or element is {@code null}
     * @throws IllegalArgumentException if the axes are empty, unordered, or do
     * not match the matrix dimensions
     */
    public VolatilitySurfaceGrid(
            List<Long> expiryTimestampsNanos,
            List<Double> logStrikeToForwards,
            double[][] impliedVolatilities
    ) {
        this.expiryTimestampsNanos = List.copyOf(
                Objects.requireNonNull(expiryTimestampsNanos, "Expiry timestamps cannot be null.")
        );
        this.logStrikeToForwards = List.copyOf(
                Objects.requireNonNull(logStrikeToForwards, "Moneyness coordinates cannot be null.")
        );
        validateAxes();
        this.impliedVolatilities = copyAndValidate(
                Objects.requireNonNull(impliedVolatilities, "Volatility matrix cannot be null.")
        );
    }

    /**
     * Returns the sampled expiry timestamps in ascending order.
     *
     * @return immutable expiry axis in epoch nanoseconds
     */
    public List<Long> expiryTimestampsNanos() {
        return expiryTimestampsNanos;
    }

    /**
     * Returns sampled {@code ln(K / F(T))} coordinates in ascending order.
     *
     * @return immutable moneyness axis
     */
    public List<Double> logStrikeToForwards() {
        return logStrikeToForwards;
    }

    /**
     * Returns one sampled implied volatility.
     *
     * @param expiryIndex row index on the expiry axis
     * @param moneynessIndex column index on the moneyness axis
     * @return annualized implied volatility as a decimal
     * @throws IndexOutOfBoundsException if either index is outside the grid
     */
    public double impliedVolatility(int expiryIndex, int moneynessIndex) {
        return impliedVolatilities[expiryIndex][moneynessIndex];
    }

    /**
     * Returns the number of sampled expiries.
     *
     * @return grid row count
     */
    public int expiryCount() {
        return expiryTimestampsNanos.size();
    }

    /**
     * Returns the number of sampled moneyness coordinates.
     *
     * @return grid column count
     */
    public int moneynessCount() {
        return logStrikeToForwards.size();
    }

    /**
     * Returns the smallest sampled volatility.
     *
     * @return minimum annualized implied volatility
     */
    public double minimumVolatility() {
        double minimum = Double.POSITIVE_INFINITY;
        for (double[] row : impliedVolatilities) {
            for (double volatility : row) {
                minimum = Math.min(minimum, volatility);
            }
        }
        return minimum;
    }

    /**
     * Returns the largest sampled volatility.
     *
     * @return maximum annualized implied volatility
     */
    public double maximumVolatility() {
        double maximum = Double.NEGATIVE_INFINITY;
        for (double[] row : impliedVolatilities) {
            for (double volatility : row) {
                maximum = Math.max(maximum, volatility);
            }
        }
        return maximum;
    }

    private void validateAxes() {
        if (expiryTimestampsNanos.isEmpty() || logStrikeToForwards.isEmpty()) {
            throw new IllegalArgumentException("Surface grid axes must not be empty.");
        }
        for (int index = 0; index < expiryTimestampsNanos.size(); index++) {
            Objects.requireNonNull(expiryTimestampsNanos.get(index), "Expiry timestamp cannot be null.");
            if (index > 0 && expiryTimestampsNanos.get(index) <= expiryTimestampsNanos.get(index - 1)) {
                throw new IllegalArgumentException("Expiry timestamps must be strictly increasing.");
            }
        }
        for (int index = 0; index < logStrikeToForwards.size(); index++) {
            Double coordinate = Objects.requireNonNull(
                    logStrikeToForwards.get(index), "Moneyness coordinate cannot be null."
            );
            if (!Double.isFinite(coordinate)) {
                throw new IllegalArgumentException("Moneyness coordinates must be finite.");
            }
            if (index > 0 && coordinate <= logStrikeToForwards.get(index - 1)) {
                throw new IllegalArgumentException("Moneyness coordinates must be strictly increasing.");
            }
        }
    }

    private double[][] copyAndValidate(double[][] values) {
        if (values.length != expiryCount()) {
            throw new IllegalArgumentException("Volatility matrix row count must match the expiry axis.");
        }
        double[][] copy = new double[values.length][];
        for (int row = 0; row < values.length; row++) {
            if (values[row] == null || values[row].length != moneynessCount()) {
                throw new IllegalArgumentException(
                        "Every volatility matrix row must match the moneyness axis."
                );
            }
            copy[row] = values[row].clone();
            for (double volatility : copy[row]) {
                if (!Double.isFinite(volatility) || volatility <= 0.0) {
                    throw new IllegalArgumentException(
                            "Sampled implied volatilities must be finite and positive."
                    );
                }
            }
        }
        return copy;
    }
}
