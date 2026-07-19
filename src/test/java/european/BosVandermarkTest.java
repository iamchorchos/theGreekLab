package european;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.exception.NonPositivePriceException;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.european.BlackScholesMerton;
import com.thegreeklab.finance.model.european.discrete.CashDividend;
import com.thegreeklab.finance.model.european.discrete.DividendSchedule;
import com.thegreeklab.finance.model.european.discrete.adjustments.BosVandermark;
import com.thegreeklab.finance.time.EpochNanos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static com.thegreeklab.finance.time.DayCountConvention.ACT_365F;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BosVandermarkTest {

    private static final double PUBLISHED_TOLERANCE = 5e-5;
    private static final long NANOS_PER_365_DAY_YEAR = 365L * 86_400_000_000_000L;
    private static final ZonedDateTime VALUATION = ZonedDateTime.of(
            2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
    );
    private static final ZonedDateTime EXPIRY = VALUATION.plusDays(365);
    private static final OptionContract CONTRACT = contract();
    private static final EquityFrame FRAME = new EquityFrame(
            VALUATION, 100.0, 0.06, 0.0
    );

    @ParameterizedTest(name = "tD={0}, published BV={1}")
    @CsvSource({
            "0.0001, 10.5806",
            "0.5000, 11.0979",
            "0.9999, 11.5887"
    })
    void matchesPublishedPrices(double dividendTime, double expectedPrice) {
        BosVandermark model = model(
                new DividendSchedule(List.of(dividend(dividendTime, 7.0)))
        );

        // Haug, Haug and Lewis (2003), Table 1, BV column, X=100.
        // Published values have four decimals, hence the half-unit tolerance.
        assertEquals(expectedPrice, model.price(), PUBLISHED_TOLERANCE);
    }

    @Test
    void splitsDividendBetweenSpotAndStrike() {
        BosVandermark model = model(
                new DividendSchedule(List.of(dividend(0.5, 7.0)))
        );
        double dividendPresentValue = 7.0 * Math.exp(-0.06 * 0.5);
        double nearDividendPv = 0.5 * dividendPresentValue;
        double farDividendPv = 0.5 * dividendPresentValue;

        assertAll(
                () -> assertEquals(
                        100.0 - nearDividendPv,
                        model.adjustedSpot(),
                        1e-12
                ),
                () -> assertEquals(
                        100.0 + farDividendPv * Math.exp(0.06),
                        model.adjustedStrike(),
                        1e-12
                ),
                () -> assertEquals(0.30, model.adjustedVolatility(), 0.0)
        );
    }

    @Test
    void emptyScheduleKeepsInputs() {
        BosVandermark model = model(new DividendSchedule(List.of()));
        double expectedPrice = BlackScholesMerton.price(
                CONTRACT, FRAME, 0.30, ACT_365F
        );

        assertAll(
                () -> assertEquals(100.0, model.adjustedSpot(), 0.0),
                () -> assertEquals(100.0, model.adjustedStrike(), 0.0),
                () -> assertEquals(0.30, model.adjustedVolatility(), 0.0),
                () -> assertEquals(expectedPrice, model.price(), 0.0)
        );
    }

    @Test
    void validatesNearAdjustedSpotInsteadOfFullDividendPv() {
        DividendSchedule lateLargeDividend = new DividendSchedule(List.of(
                dividend(0.99, 110.0)
        ));

        assertDoesNotThrow(() -> model(lateLargeDividend));
    }

    @Test
    void rejectsNonPositiveNearAdjustedSpot() {
        DividendSchedule earlyLargeDividend = new DividendSchedule(List.of(
                dividend(0.01, 110.0)
        ));

        assertThrows(
                NonPositivePriceException.class,
                () -> model(earlyLargeDividend)
        );
    }

    private static BosVandermark model(DividendSchedule schedule) {
        return new BosVandermark(
                CONTRACT, FRAME, schedule, 0.30, ACT_365F
        );
    }

    private static OptionContract contract() {
        return new OptionContract(
                "TEST",
                OptionType.CALL,
                Option.EUROPEAN,
                100.0,
                EXPIRY,
                100
        );
    }

    private static CashDividend dividend(double time, double amount) {
        long exTimestamp = Math.addExact(
                EpochNanos.from(VALUATION),
                Math.round(time * NANOS_PER_365_DAY_YEAR)
        );
        return new CashDividend(exTimestamp, amount);
    }
}
