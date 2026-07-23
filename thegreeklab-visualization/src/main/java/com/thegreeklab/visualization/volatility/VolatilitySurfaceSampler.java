package com.thegreeklab.visualization.volatility;

import com.thegreeklab.finance.volatility.VolatilitySurface;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Samples a {@link VolatilitySurface} on regular expiry and moneyness axes.
 */
public final class VolatilitySurfaceSampler {

    private VolatilitySurfaceSampler() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Samples a surface on evenly spaced expiry and log strike-to-forward axes.
     *
     * @param surface market-data surface to query
     * @param lastExpiryTimestampNanos final sampled expiry in epoch nanoseconds
     * @param expiryCount number of sampled expiries, including valuation and
     * final expiry
     * @param minimumLogStrikeToForward smallest {@code ln(K / F(T))} value
     * @param maximumLogStrikeToForward largest {@code ln(K / F(T))} value
     * @param moneynessCount number of sampled moneyness points
     * @return immutable sampled grid
     * @throws NullPointerException if {@code surface} is {@code null}
     * @throws IllegalArgumentException if the sampling domain is invalid
     * @throws ArithmeticException if timestamp arithmetic overflows
     */
    public static VolatilitySurfaceGrid sample(
            VolatilitySurface surface,
            long lastExpiryTimestampNanos,
            int expiryCount,
            double minimumLogStrikeToForward,
            double maximumLogStrikeToForward,
            int moneynessCount
    ) {
        Objects.requireNonNull(surface, "Volatility surface cannot be null.");
        validateSamplingDomain(
                surface.valuationTimestampNanos(),
                lastExpiryTimestampNanos,
                expiryCount,
                minimumLogStrikeToForward,
                maximumLogStrikeToForward,
                moneynessCount
        );

        List<Long> expiries = evenlySpacedTimestamps(
                surface.valuationTimestampNanos(), lastExpiryTimestampNanos, expiryCount
        );
        List<Double> moneyness = evenlySpacedValues(
                minimumLogStrikeToForward, maximumLogStrikeToForward, moneynessCount
        );
        double[][] values = new double[expiryCount][moneynessCount];
        for (int expiryIndex = 0; expiryIndex < expiryCount; expiryIndex++) {
            for (int moneynessIndex = 0; moneynessIndex < moneynessCount; moneynessIndex++) {
                values[expiryIndex][moneynessIndex] = surface.impliedVolatility(
                        expiries.get(expiryIndex), moneyness.get(moneynessIndex)
                );
            }
        }
        return new VolatilitySurfaceGrid(expiries, moneyness, values);
    }

    private static void validateSamplingDomain(
            long valuationTimestampNanos,
            long lastExpiryTimestampNanos,
            int expiryCount,
            double minimumLogStrikeToForward,
            double maximumLogStrikeToForward,
            int moneynessCount
    ) {
        if (lastExpiryTimestampNanos <= valuationTimestampNanos) {
            throw new IllegalArgumentException("Final expiry must be after the surface valuation timestamp.");
        }
        if (expiryCount < 2 || moneynessCount < 2) {
            throw new IllegalArgumentException("Each surface axis requires at least two sample points.");
        }
        if (!Double.isFinite(minimumLogStrikeToForward)
                || !Double.isFinite(maximumLogStrikeToForward)
                || minimumLogStrikeToForward >= maximumLogStrikeToForward) {
            throw new IllegalArgumentException(
                    "Moneyness bounds must be finite and strictly increasing."
            );
        }
    }

    private static List<Long> evenlySpacedTimestamps(long start, long end, int count) {
        long horizon = Math.subtractExact(end, start);
        long intervals = count - 1L;
        long wholeStep = horizon / intervals;
        long remainder = horizon % intervals;
        List<Long> timestamps = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            long wholePart = Math.multiplyExact(wholeStep, index);
            long remainderPart = Math.multiplyExact(remainder, index) / intervals;
            timestamps.add(Math.addExact(start, Math.addExact(wholePart, remainderPart)));
        }
        return timestamps;
    }

    private static List<Double> evenlySpacedValues(double minimum, double maximum, int count) {
        List<Double> values = new ArrayList<>(count);
        double step = (maximum - minimum) / (count - 1);
        for (int index = 0; index < count; index++) {
            values.add(index == count - 1 ? maximum : minimum + index * step);
        }
        return values;
    }
}
