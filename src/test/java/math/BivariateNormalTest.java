package math;

import com.thegreeklab.math.BivariateNormal;
import com.thegreeklab.math.ERF;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BivariateNormalTest {

    private static final double TOLERANCE = 1.0e-12;

    @Test
    void positiveCorrelation() {
        assertEquals(1.0 / 3.0, BivariateNormal.cdf(0.0, 0.0, 0.5), TOLERANCE);
    }

    @Test
    void independence() {
        double x = 1.0;
        double y = -0.5;

        assertEquals(ERF.cdf(x) * ERF.cdf(y), BivariateNormal.cdf(x, y, 0.0), TOLERANCE);
    }

    @Test
    void invalidCorrelation() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> BivariateNormal.cdf(0.0, 0.0, 1.01)),
                () -> assertThrows(IllegalArgumentException.class, () -> BivariateNormal.cdf(0.0, 0.0, -1.01)),
                () -> assertThrows(IllegalArgumentException.class, () -> BivariateNormal.cdf(0.0, 0.0, Double.NaN)),
                () -> assertThrows(IllegalArgumentException.class, () -> BivariateNormal.cdf(0.0, 0.0, Double.POSITIVE_INFINITY))
        );
    }

    @Test
    void invalidLimits() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> BivariateNormal.cdf(Double.NaN, 0.0, 0.0)),
                () -> assertThrows(IllegalArgumentException.class, () -> BivariateNormal.cdf(0.0, Double.NaN, 0.0))
        );
    }

    @Test
    void perfectCorrelation() {
        double x = 0.25;
        double y = -0.5;

        assertAll(
                () -> assertEquals(ERF.cdf(Math.min(x, y)), BivariateNormal.cdf(x, y, 1.0), TOLERANCE),
                () -> assertEquals(
                        Math.max(ERF.cdf(x) + ERF.cdf(y) - 1.0, 0.0),
                        BivariateNormal.cdf(x, y, -1.0),
                        TOLERANCE
                )
        );
    }
}
