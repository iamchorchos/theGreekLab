package math;

import com.thegreeklab.finance.exception.LackingDataException;
import com.thegreeklab.finance.exception.NonPositivePriceException;
import com.thegreeklab.finance.exception.VolatilityException;
import com.thegreeklab.math.VolatilityCalculator;
import org.eclipse.collections.api.list.primitive.DoubleList;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HistoricalVolatilityTest {
    private static final double TOLERANCE = 1e-13;

    @ParameterizedTest
    @CsvFileSource(resources = "/volatility_crossval.csv", numLinesToSkip = 1, maxCharsPerColumn = 20000)
    void validate(String pricesStr, int tradingDays, double expectedVol) {

        double[] pricesArray = Arrays.stream(pricesStr.split(";"))
                .mapToDouble(Double::parseDouble)
                .toArray();
        DoubleList prices = new DoubleArrayList(pricesArray);

        double actualVol = VolatilityCalculator.historicalVolatility(prices, tradingDays);

        assertEquals(expectedVol, actualVol, TOLERANCE,
                () -> "Precision error on  " + prices.size() + " days.");
    }

    @Test
    void rejectsNullPriceListWithDomainException() {
        assertThrows(
                LackingDataException.class,
                () -> VolatilityCalculator.historicalVolatility(null, 252)
        );
    }

    @Test
    void rejectsInvalidTradingDays() {
        DoubleList prices = new DoubleArrayList(100.0, 101.0, 102.0);

        assertThrows(
                VolatilityException.class,
                () -> VolatilityCalculator.historicalVolatility(prices, 0)
        );
    }

    @Test
    void rejectsNonFinitePrices() {
        DoubleList prices = new DoubleArrayList(100.0, Double.NaN, 102.0);

        assertThrows(
                NonPositivePriceException.class,
                () -> VolatilityCalculator.historicalVolatility(prices, 252)
        );
    }
}
