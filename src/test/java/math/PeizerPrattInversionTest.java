package math;

import com.thegreeklab.math.PeizerPrattInversion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PeizerPrattInversionTest {

    private static final double TOLERANCE = 1e-11;

    @ParameterizedTest(name = "Row {index}: x={0}, steps={1}")
    @CsvFileSource(resources = "/peizer_pratt_crossval.csv", numLinesToSkip = 1)
    void validateMethod2TransformAgainstHighPrecisionReference(double x, int steps, double expectedProbability) {

        double actualProb = PeizerPrattInversion.inverseFunction(x, steps);

        assertEquals(expectedProbability, actualProb, TOLERANCE,
                () -> String.format("Exceeded the tolerance: x=%s, steps=%d", x, steps));
    }
}
