package com.thegreeklab.visualization.volatility;

import com.thegreeklab.finance.volatility.VolatilitySurface;

import java.util.Objects;

/**
 * Factory methods for JavaFX implied-volatility visualizations.
 */
@SuppressWarnings("unused")
public final class VolatilitySurfaceCharts {

    private VolatilitySurfaceCharts() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates a resizable JavaFX heatmap from a regularly sampled volatility
     * surface.
     *
     * <p>The returned chart is a JavaFX {@code Node}; callers add it to their
     * own scene and manage the JavaFX application lifecycle.</p>
     *
     * @param surface market-data surface to visualize
     * @param lastExpiryTimestampNanos final sampled expiry in epoch nanoseconds
     * @param expiryCount number of expiry samples
     * @param minimumLogStrikeToForward smallest {@code ln(K / F(T))} value
     * @param maximumLogStrikeToForward largest {@code ln(K / F(T))} value
     * @param moneynessCount number of moneyness samples
     * @return JavaFX heatmap node
     * @throws NullPointerException if {@code surface} is {@code null}
     * @throws IllegalArgumentException if the sampling domain is invalid
     */
    public static VolatilitySurfaceChart heatmap(
            VolatilitySurface surface,
            long lastExpiryTimestampNanos,
            int expiryCount,
            double minimumLogStrikeToForward,
            double maximumLogStrikeToForward,
            int moneynessCount
    ) {
        Objects.requireNonNull(surface, "Volatility surface cannot be null.");
        return new VolatilitySurfaceChart(VolatilitySurfaceSampler.sample(
                surface,
                lastExpiryTimestampNanos,
                expiryCount,
                minimumLogStrikeToForward,
                maximumLogStrikeToForward,
                moneynessCount
        ));
    }

    /**
     * Creates a resizable JavaFX heatmap from previously sampled data.
     *
     * @param grid surface sample to visualize
     * @return JavaFX heatmap node
     * @throws NullPointerException if {@code grid} is {@code null}
     */
    public static VolatilitySurfaceChart heatmap(VolatilitySurfaceGrid grid) {
        return new VolatilitySurfaceChart(Objects.requireNonNull(grid, "Surface grid cannot be null."));
    }
}
