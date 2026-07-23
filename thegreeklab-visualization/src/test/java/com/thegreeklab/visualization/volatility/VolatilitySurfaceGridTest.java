package com.thegreeklab.visualization.volatility;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VolatilitySurfaceGridTest {

    @Test
    void defensivelyCopiesTheVolatilityMatrix() {
        double[][] values = {{0.20, 0.21}, {0.22, 0.23}};
        VolatilitySurfaceGrid grid = new VolatilitySurfaceGrid(
                List.of(1L, 2L), List.of(-0.1, 0.1), values
        );
        values[0][0] = 0.99;

        assertEquals(0.20, grid.impliedVolatility(0, 0));
    }

    @Test
    void rejectsMalformedGrids() {
        assertThrows(IllegalArgumentException.class, () -> new VolatilitySurfaceGrid(
                List.of(2L, 1L), List.of(-0.1, 0.1), new double[][]{{0.2, 0.2}, {0.2, 0.2}}
        ));
        assertThrows(IllegalArgumentException.class, () -> new VolatilitySurfaceGrid(
                List.of(1L, 2L), List.of(-0.1, 0.1), new double[][]{{0.2}, {0.2}}
        ));
        assertThrows(IllegalArgumentException.class, () -> new VolatilitySurfaceGrid(
                List.of(1L, 2L), List.of(-0.1, 0.1), new double[][]{{0.2, 0.2}, {0.2, 0.0}}
        ));
    }
}
