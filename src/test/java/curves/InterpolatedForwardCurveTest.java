package curves;

import com.thegreeklab.finance.curves.ForwardPriceNode;
import com.thegreeklab.finance.curves.InterpolatedForwardCurve;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InterpolatedForwardCurveTest {

    private static final long VALUATION_NANOS = 1_000_000_000_000_000_000L;
    private static final long ONE_YEAR_NANOS = 365L * 24L * 60L * 60L * 1_000_000_000L;
    private static final long ONE_YEAR = VALUATION_NANOS + ONE_YEAR_NANOS;
    private static final long TWO_YEARS = VALUATION_NANOS + 2 * ONE_YEAR_NANOS;

    @Test
    void returnsExactForwardQuotesAtNodes() {
        InterpolatedForwardCurve curve = standardCurve();

        assertAll(
                () -> assertEquals(100.0, curve.forwardPrice(VALUATION_NANOS), 0.0),
                () -> assertEquals(102.0, curve.forwardPrice(ONE_YEAR), 0.0),
                () -> assertEquals(104.0, curve.forwardPrice(TWO_YEARS), 0.0)
        );
    }

    @Test
    void interpolatesLogForwardPricesBetweenNodes() {
        InterpolatedForwardCurve curve = standardCurve();
        long midpoint = ONE_YEAR + ONE_YEAR_NANOS / 2;

        assertEquals(Math.sqrt(102.0 * 104.0), curve.forwardPrice(midpoint), 1e-13);
    }

    @Test
    void preservesNanosecondOffsetsAtLargeEpochTimestamps() {
        InterpolatedForwardCurve curve = new InterpolatedForwardCurve(
                VALUATION_NANOS,
                List.of(
                        new ForwardPriceNode(VALUATION_NANOS, 100.0),
                        new ForwardPriceNode(VALUATION_NANOS + 100, 102.0),
                        new ForwardPriceNode(VALUATION_NANOS + 200, 104.0)
                )
        );

        assertEquals(
                Math.sqrt(102.0 * 104.0),
                curve.forwardPrice(VALUATION_NANOS + 150),
                1e-13
        );
    }

    @Test
    void rejectsInvalidNodes() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ForwardPriceNode(ONE_YEAR, 0.0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ForwardPriceNode(ONE_YEAR, Double.NaN)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new InterpolatedForwardCurve(VALUATION_NANOS, List.of())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new InterpolatedForwardCurve(
                                VALUATION_NANOS,
                                List.of(new ForwardPriceNode(ONE_YEAR, 102.0))
                        )),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new InterpolatedForwardCurve(
                                VALUATION_NANOS,
                                List.of(
                                        new ForwardPriceNode(VALUATION_NANOS, 100.0),
                                        new ForwardPriceNode(TWO_YEARS, 104.0),
                                        new ForwardPriceNode(ONE_YEAR, 102.0)
                                )
                        ))
        );
    }

    @Test
    void rejectsTimestampsOutsideTheCurveDomain() {
        InterpolatedForwardCurve curve = standardCurve();

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> curve.forwardPrice(VALUATION_NANOS - 1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> curve.forwardPrice(TWO_YEARS + 1))
        );
    }

    private static InterpolatedForwardCurve standardCurve() {
        return new InterpolatedForwardCurve(
                VALUATION_NANOS,
                List.of(
                        new ForwardPriceNode(VALUATION_NANOS, 100.0),
                        new ForwardPriceNode(ONE_YEAR, 102.0),
                        new ForwardPriceNode(TWO_YEARS, 104.0)
                )
        );
    }
}
