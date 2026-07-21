package math;

import com.thegreeklab.finance.exception.LackingDataException;
import com.thegreeklab.finance.exception.VolatilityException;
import com.thegreeklab.math.volatility.VolatilityCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParkinsonsVolatilityTest {
    private static final double TOLERANCE = 1e-13;

    @ParameterizedTest
    @CsvFileSource(resources = "/parkinson_crossval.csv", numLinesToSkip = 1, maxCharsPerColumn = 50000)
    void matchesReference(String highsStr, String lowsStr, int tradingDays, double expectedVol) {

        double[] highs = Arrays.stream(highsStr.split(";"))
                .mapToDouble(Double::parseDouble)
                .toArray();
        double[] lows = Arrays.stream(lowsStr.split(";"))
                .mapToDouble(Double::parseDouble)
                .toArray();

        List<VolatilityCalculator.PriceBar> ohlc = IntStream.range(0, highs.length)
                .mapToObj(i -> new VolatilityCalculator.PriceBar(highs[i], lows[i]))
                .collect(Collectors.toList());

        double actualVol = VolatilityCalculator.parkinsonsVolatility(ohlc, tradingDays);

        assertEquals(expectedVol, actualVol, TOLERANCE,
                () -> "Precision error on " + ohlc.size() + " bars.");
    }

    @Test
    void rejectsNullBars() {
        assertThrows(
                LackingDataException.class,
                () -> VolatilityCalculator.parkinsonsVolatility(null, 252)
        );
    }

    @Test
    void rejectsTradingDays() {
        List<VolatilityCalculator.PriceBar> bars = List.of(
                new VolatilityCalculator.PriceBar(101.0, 99.0),
                new VolatilityCalculator.PriceBar(102.0, 98.0)
        );

        assertThrows(
                VolatilityException.class,
                () -> VolatilityCalculator.parkinsonsVolatility(bars, 0)
        );
    }

    @Test
    void rejectsNonFinite() {
        assertThrows(
                VolatilityException.class,
                () -> new VolatilityCalculator.PriceBar(Double.NaN, 99.0)
        );
    }

    @Test
    void rejectsNullEntries() {
        List<VolatilityCalculator.PriceBar> bars = Arrays.asList(
                new VolatilityCalculator.PriceBar(101.0, 99.0),
                null
        );

        assertThrows(
                VolatilityException.class,
                () -> VolatilityCalculator.parkinsonsVolatility(bars, 252)
        );
    }
}
