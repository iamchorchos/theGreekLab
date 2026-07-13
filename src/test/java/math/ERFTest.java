package math;

import com.thegreeklab.math.ERF;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ERFTest {

    private static final double TOLERANCE = 1e-15;

    @ParameterizedTest(name = "Validation row {index}: x={0}")
    @CsvFileSource(resources = "/erf_crossval.csv", numLinesToSkip = 1)
    void matchesReference(double x, double expectedErfc, double expectedCdf, double expectedPdf) {

        assertEquals(expectedErfc, ERF.erfc(x), TOLERANCE,
                () -> String.format("ERFC mismatch at x = %s", x));

        assertEquals(expectedCdf, ERF.cdf(x), TOLERANCE,
                () -> String.format("CDF mismatch at x = %s", x));

        assertEquals(expectedPdf, ERF.pdf(x), TOLERANCE,
                () -> String.format("PDF mismatch at x = %s", x));
    }

    @Test
    void specialValues() {
        assertEquals(0.0, ERF.erfc(Double.POSITIVE_INFINITY), 0.0);
        assertEquals(2.0, ERF.erfc(Double.NEGATIVE_INFINITY), 0.0);
        assertTrue(Double.isNaN(ERF.erfc(Double.NaN)));

        assertEquals(1.0, ERF.cdf(Double.POSITIVE_INFINITY), 0.0);
        assertEquals(0.0, ERF.cdf(Double.NEGATIVE_INFINITY), 0.0);
        assertTrue(Double.isNaN(ERF.cdf(Double.NaN)));

        assertEquals(0.0, ERF.pdf(Double.POSITIVE_INFINITY), 0.0);
        assertEquals(0.0, ERF.pdf(Double.NEGATIVE_INFINITY), 0.0);
        assertTrue(Double.isNaN(ERF.pdf(Double.NaN)));
    }

    @Test
    void centralValues() {
        assertEquals(1.0, ERF.erfc(0.0), TOLERANCE);
        assertEquals(1.0, ERF.erfc(-0.0), TOLERANCE);
        assertEquals(0.5, ERF.cdf(0.0), TOLERANCE);
        assertEquals(0.3989422804014327, ERF.pdf(0.0), TOLERANCE);
    }

    @Test
    void regionBoundaries() {
        assertTrue(Double.isFinite(ERF.erfc(0.46875)));
        assertTrue(Double.isFinite(ERF.erfc(-0.46875)));
        assertTrue(Double.isFinite(ERF.erfc(4.0)));
        assertTrue(Double.isFinite(ERF.erfc(-4.0)));
        assertEquals(0.0, ERF.erfc(26.543), 0.0);
        assertEquals(2.0, ERF.erfc(-26.543), 0.0);
    }
}
