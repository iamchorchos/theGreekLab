package math;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.exception.NonPositivePriceException;
import com.thegreeklab.finance.exception.UnsupportedExerciseStyleException;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.frame.FuturesFrame;
import com.thegreeklab.finance.frame.FXFrame;
import com.thegreeklab.finance.frame.MarketData;
import com.thegreeklab.math.VolatilityCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImpliedVolatilityTest {
    private static final double TOLERANCE = 1e-8;

    @ParameterizedTest
    @CsvFileSource(resources = "/iv_crossval.csv", numLinesToSkip = 1)
    void recoversVolatility(String assetClass, String flag, double spot, double strike, double t,
                  double rateOrDomestic, double divOrForeign, double marketPrice, double expectedIv) {

        long nowNanos = System.currentTimeMillis() * 1_000_000L;
        double secondsInYear = 365.0 * 86400.0;
        long expirationNanosEpoch = nowNanos + (long) (t * secondsInYear * 1_000_000_000.0);

        ZonedDateTime safeFutureDate = ZonedDateTime.now().plusYears(10);

        OptionType type = "c".equals(flag) ? OptionType.CALL : OptionType.PUT;

        OptionContract contract = new OptionContract(
                "TEST", type, Option.EUROPEAN, strike,
                safeFutureDate, 1,
                expirationNanosEpoch, secondsInYear
        );

        MarketData frame = switch (assetClass) {
            case "EQUITY" -> new EquityFrame(nowNanos, spot, rateOrDomestic, divOrForeign);
            case "FUTURES" -> new FuturesFrame(nowNanos, spot, rateOrDomestic);
            case "FX" -> new FXFrame(nowNanos, spot, rateOrDomestic, divOrForeign);
            default -> throw new IllegalArgumentException("Unknown asset class: " + assetClass);
        };

        double actualT = contract.getTimeToExpiry(frame.timestampNanos());
        assertEquals(t, actualT, 1e-9, "Time-to-expiry construction drifted from nominal t");

        OptionalDouble result = VolatilityCalculator.impliedVolatility(contract, frame, marketPrice);

        assertTrue(result.isPresent(), () -> "Solver returned empty for a known-solvable price (" + assetClass + ")");
        assertEquals(expectedIv, result.getAsDouble(), TOLERANCE,
                () -> "IV mismatch for " + assetClass + ", t=" + t + ", K/S=" + (strike / spot));
    }

    @Test
    void rejectsAmerican() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = new OptionContract(
                "TEST",
                OptionType.CALL,
                Option.AMERICAN,
                100.0,
                now.plusYears(1),
                100
        );
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        assertThrows(
                UnsupportedExerciseStyleException.class,
                () -> VolatilityCalculator.impliedVolatility(contract, frame, 10.0)
        );
    }

    @Test
    void rejectsNonFinitePrice() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = new OptionContract(
                "TEST",
                OptionType.CALL,
                Option.EUROPEAN,
                100.0,
                now.plusYears(1),
                100
        );
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        assertThrows(
                NonPositivePriceException.class,
                () -> VolatilityCalculator.impliedVolatility(contract, frame, Double.NaN)
        );
    }
}
