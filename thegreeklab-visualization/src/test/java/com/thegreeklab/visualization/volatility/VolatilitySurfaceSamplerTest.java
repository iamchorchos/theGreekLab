package com.thegreeklab.visualization.volatility;

import com.thegreeklab.finance.volatility.FlatVolatilitySurface;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VolatilitySurfaceSamplerTest {

    private static final long VALUATION_NANOS = 1_000_000_000L;
    private static final long LAST_EXPIRY_NANOS = 11_000_000_000L;

    @Test
    void samplesAFlatSurfaceOnRegularAxes() {
        VolatilitySurfaceGrid grid = VolatilitySurfaceSampler.sample(
                new FlatVolatilitySurface(VALUATION_NANOS, 0.24),
                LAST_EXPIRY_NANOS,
                3,
                -0.20,
                0.20,
                3
        );

        assertEquals(
                java.util.List.of(VALUATION_NANOS, 6_000_000_000L, LAST_EXPIRY_NANOS),
                grid.expiryTimestampsNanos()
        );
        assertEquals(java.util.List.of(-0.20, 0.0, 0.20), grid.logStrikeToForwards());
        assertEquals(0.24, grid.impliedVolatility(0, 0));
        assertEquals(0.24, grid.impliedVolatility(2, 2));
        assertEquals(0.24, grid.minimumVolatility());
        assertEquals(0.24, grid.maximumVolatility());
    }

    @Test
    void rejectsInvalidSamplingDomains() {
        FlatVolatilitySurface surface = new FlatVolatilitySurface(VALUATION_NANOS, 0.24);

        assertThrows(IllegalArgumentException.class, () -> VolatilitySurfaceSampler.sample(
                surface, VALUATION_NANOS, 3, -0.2, 0.2, 3
        ));
        assertThrows(IllegalArgumentException.class, () -> VolatilitySurfaceSampler.sample(
                surface, LAST_EXPIRY_NANOS, 1, -0.2, 0.2, 3
        ));
        assertThrows(IllegalArgumentException.class, () -> VolatilitySurfaceSampler.sample(
                surface, LAST_EXPIRY_NANOS, 3, 0.2, -0.2, 3
        ));
    }
}
