package curves;

import com.thegreeklab.finance.curves.DiscountFactorNode;
import com.thegreeklab.finance.curves.InterpolatedDiscountCurve;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InterpolatedDiscountCurveTest {

    private static final long VALUATION_NANOS = 1_000_000_000_000_000_000L;
    private static final long ONE_YEAR_NANOS = 365L * 24L * 60L * 60L * 1_000_000_000L;
    private static final long ONE_YEAR = VALUATION_NANOS + ONE_YEAR_NANOS;
    private static final long TWO_YEARS = VALUATION_NANOS + 2 * ONE_YEAR_NANOS;

    @Test
    void returnsTheValuationAnchorAndExactNodeValues() {
        InterpolatedDiscountCurve curve = standardCurve();

        assertAll(
                () -> assertEquals(1.0, curve.discountFactor(VALUATION_NANOS), 0.0),
                () -> assertEquals(0.95, curve.discountFactor(ONE_YEAR), 0.0),
                () -> assertEquals(0.90, curve.discountFactor(TWO_YEARS), 0.0)
        );
    }

    @Test
    void interpolatesLogDiscountFactorsBetweenNodes() {
        InterpolatedDiscountCurve curve = standardCurve();
        long midpoint = ONE_YEAR + ONE_YEAR_NANOS / 2;

        assertEquals(
                Math.sqrt(0.95 * 0.90),
                curve.discountFactor(midpoint),
                1e-15
        );
    }

    @Test
    void interpolatesBetweenValuationAnchorAndFirstNode() {
        InterpolatedDiscountCurve curve = standardCurve();
        long midpoint = VALUATION_NANOS + ONE_YEAR_NANOS / 2;

        assertEquals(Math.sqrt(0.95), curve.discountFactor(midpoint), 1e-15);
    }

    @Test
    void preservesNanosecondOffsetsAtLargeEpochTimestamps() {
        long firstNode = VALUATION_NANOS + 100;
        long secondNode = VALUATION_NANOS + 200;
        InterpolatedDiscountCurve curve = new InterpolatedDiscountCurve(
                VALUATION_NANOS,
                List.of(
                        new DiscountFactorNode(firstNode, 0.95),
                        new DiscountFactorNode(secondNode, 0.90)
                )
        );

        assertEquals(
                Math.sqrt(0.95 * 0.90),
                curve.discountFactor(VALUATION_NANOS + 150),
                1e-15
        );
    }

    @Test
    void rejectsInvalidNodes() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new DiscountFactorNode(ONE_YEAR, 0.0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new DiscountFactorNode(ONE_YEAR, Double.NaN)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new InterpolatedDiscountCurve(VALUATION_NANOS, List.of())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new InterpolatedDiscountCurve(
                                VALUATION_NANOS,
                                List.of(new DiscountFactorNode(VALUATION_NANOS, 1.0))
                        )),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new InterpolatedDiscountCurve(
                                VALUATION_NANOS,
                                List.of(
                                        new DiscountFactorNode(TWO_YEARS, 0.90),
                                        new DiscountFactorNode(ONE_YEAR, 0.95)
                                )
                        ))
        );
    }

    @Test
    void rejectsTimestampsOutsideTheCurveDomain() {
        InterpolatedDiscountCurve curve = standardCurve();

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> curve.discountFactor(VALUATION_NANOS - 1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> curve.discountFactor(TWO_YEARS + 1))
        );
    }

    private static InterpolatedDiscountCurve standardCurve() {
        return new InterpolatedDiscountCurve(
                VALUATION_NANOS,
                List.of(
                        new DiscountFactorNode(ONE_YEAR, 0.95),
                        new DiscountFactorNode(TWO_YEARS, 0.90)
                )
        );
    }
}
