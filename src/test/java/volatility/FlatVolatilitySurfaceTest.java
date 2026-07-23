package volatility;

import com.thegreeklab.finance.exception.InvalidVolatilityException;
import com.thegreeklab.finance.volatility.FlatVolatilitySurface;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlatVolatilitySurfaceTest {

    private static final long VALUATION_NANOS = 1_000_000_000_000_000_000L;

    @Test
    void returnsTheSameVolatilityAtAllValidSurfacePoints() {
        FlatVolatilitySurface surface = new FlatVolatilitySurface(
                VALUATION_NANOS,
                0.22
        );

        assertAll(
                () -> assertEquals(0.22,
                        surface.impliedVolatility(VALUATION_NANOS, 0.0), 0.0),
                () -> assertEquals(0.22,
                        surface.impliedVolatility(VALUATION_NANOS + 1, -0.25), 0.0),
                () -> assertEquals(0.22,
                        surface.impliedVolatility(VALUATION_NANOS + 1, 0.50), 0.0)
        );
    }

    @Test
    void rejectsInvalidSurfaceInputs() {
        assertAll(
                () -> assertThrows(InvalidVolatilityException.class,
                        () -> new FlatVolatilitySurface(VALUATION_NANOS, Double.NaN)),
                () -> assertThrows(InvalidVolatilityException.class,
                        () -> new FlatVolatilitySurface(VALUATION_NANOS, 0.0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new FlatVolatilitySurface(VALUATION_NANOS, 0.20)
                                .impliedVolatility(VALUATION_NANOS - 1, 0.0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new FlatVolatilitySurface(VALUATION_NANOS, 0.20)
                                .impliedVolatility(VALUATION_NANOS, Double.POSITIVE_INFINITY))
        );
    }
}
