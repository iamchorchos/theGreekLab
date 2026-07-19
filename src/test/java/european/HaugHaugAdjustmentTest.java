package european;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.european.discrete.CashDividend;
import com.thegreeklab.finance.model.european.discrete.DividendSchedule;
import com.thegreeklab.finance.model.european.discrete.adjustments.HaugHaugAdjustment;
import com.thegreeklab.finance.time.EpochNanos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.thegreeklab.finance.time.DayCountConvention.ACT_365F;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HaugHaugAdjustmentTest {

    private static final double PUBLISHED_TOLERANCE = 5e-5;
    private static final ZonedDateTime VALUATION = ZonedDateTime.of(
            2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
    );

    @Test
    void matchesPublishedVol2() {
        ZonedDateTime expiry = VALUATION.plusDays(365);
        DividendSchedule schedule = new DividendSchedule(List.of(
                dividend(VALUATION.plusDays(182).plusHours(12), 7.0)
        ));
        HaugHaugAdjustment model = new HaugHaugAdjustment(
                contract(expiry),
                new EquityFrame(VALUATION, 100.0, 0.06, 0.0),
                schedule,
                0.30,
                ACT_365F
        );

        double dividendPresentValue = 7.0 * Math.exp(-0.06 * 0.5);
        double intervalVolatility = 0.30 * 100.0
                / (100.0 - dividendPresentValue);
        double expectedVolatility = Math.sqrt(
                (intervalVolatility * intervalVolatility * 0.5)
                        + (0.30 * 0.30 * 0.5)
        );

        // Haug, Haug and Lewis (2003), Table 1, Vol2, X=100 and tD=0.5: 11.1039.
        // The source reports four decimals, hence the half-unit last-place tolerance.
        assertAll(
                () -> assertEquals(expectedVolatility, model.adjustedVolatility(), 1e-12),
                () -> assertEquals(11.1039, model.price(), PUBLISHED_TOLERANCE)
        );
    }

    @ParameterizedTest(name = "T={0}, published Vol2={1}")
    @CsvSource({
            "1, 10.6585",
            "2, 15.1780",
            "3, 18.5348"
    })
    void matchesPublishedSchedule(int yearsToExpiry, double expectedPrice) {
        ZonedDateTime expiry = VALUATION.plusDays(365L * yearsToExpiry);
        List<CashDividend> dividends = new ArrayList<>();
        for (int year = 0; year < yearsToExpiry; year++) {
            dividends.add(dividend(
                    VALUATION.plusDays(182L + 365L * year).plusHours(12),
                    4.0
            ));
        }

        HaugHaugAdjustment model = new HaugHaugAdjustment(
                contract(expiry),
                new EquityFrame(VALUATION, 100.0, 0.06, 0.0),
                new DividendSchedule(dividends),
                0.25,
                ACT_365F
        );

        // Haug, Haug and Lewis (2003), Table 6, Vol2. Each dividend of 4 is
        // paid halfway through its year. Published values have four decimals.
        assertEquals(expectedPrice, model.price(), PUBLISHED_TOLERANCE);
    }

    @Test
    void emptyScheduleKeepsVol() {
        HaugHaugAdjustment model = new HaugHaugAdjustment(
                contract(VALUATION.plusDays(365)),
                new EquityFrame(VALUATION, 100.0, 0.06, 0.0),
                new DividendSchedule(List.of()),
                0.25,
                ACT_365F
        );

        assertAll(
                () -> assertEquals(100.0, model.adjustedSpot(), 0.0),
                () -> assertEquals(0.25, model.adjustedVolatility(), 0.0)
        );
    }

    private static OptionContract contract(ZonedDateTime expiry) {
        return new OptionContract(
                "TEST",
                OptionType.CALL,
                Option.EUROPEAN,
                100.0,
                expiry,
                100
        );
    }

    private static CashDividend dividend(ZonedDateTime exDividend, double amount) {
        return new CashDividend(EpochNanos.from(exDividend), amount);
    }
}
