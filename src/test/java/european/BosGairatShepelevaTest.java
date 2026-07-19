package european;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.exception.NonPositivePriceException;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.european.discrete.adjustments.BosGairatShepeleva;
import com.thegreeklab.finance.model.european.discrete.CashDividend;
import com.thegreeklab.finance.model.european.discrete.DividendSchedule;
import com.thegreeklab.finance.time.EpochNanos;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static com.thegreeklab.finance.time.DayCountConvention.ACT_365F;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BosGairatShepelevaTest {

    private static final ZonedDateTime VALUATION = ZonedDateTime.of(
            2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
    );

    @Test
    void matchesPublishedVol3() {
        ZonedDateTime expiry = VALUATION.plusDays(365);
        ZonedDateTime exDividend = VALUATION.plusDays(182).plusHours(12);
        OptionContract contract = contract(expiry);
        EquityFrame frame = new EquityFrame(VALUATION, 100.0, 0.06, 0.0);
        DividendSchedule schedule = schedule(exDividend, 7.0);

        BosGairatShepeleva model = new BosGairatShepeleva(
                contract, frame, schedule, 0.30, ACT_365F
        );
        double adjustedVolatility = model.adjustedVolatility();
        double adjustedSpot = 100.0 - 7.0 * Math.exp(-0.06 * 0.5);

        // Haug, Haug and Lewis (2003), Table 1, Vol3: 11.0781.
        // The source reports four decimals, hence the half-unit last-place tolerance.
        assertAll(
                () -> assertEquals(adjustedSpot, model.adjustedSpot(), 1e-12),
                () -> assertEquals(0.3104262106662113, adjustedVolatility, 1e-12),
                () -> assertEquals(11.0781, model.price(), 5e-5)
        );
    }

    @Test
    void keepsMaturityScaling() {
        ZonedDateTime expiry = VALUATION.plusDays(182).plusHours(12);
        ZonedDateTime exDividend = VALUATION.plusDays(91).plusHours(6);
        BosGairatShepeleva model = new BosGairatShepeleva(
                contract(expiry),
                new EquityFrame(VALUATION, 100.0, 0.05, 0.0),
                schedule(exDividend, 3.0),
                0.20,
                ACT_365F
        );

        // Independent evaluation of the Appendix B formula for T=0.5 and tD=0.25.
        // The tight tolerance protects the /sqrt(T) scaling in the second-order term.
        assertEquals(0.20298635747908178, model.adjustedVolatility(), 1e-12);
    }

    @Test
    void emptyScheduleKeepsVol() {
        BosGairatShepeleva model = new BosGairatShepeleva(
                contract(VALUATION.plusDays(365)),
                new EquityFrame(VALUATION, 100.0, 0.05, 0.0),
                new DividendSchedule(List.of()),
                0.20,
                ACT_365F
        );

        assertAll(
                () -> assertEquals(100.0, model.adjustedSpot(), 0.0),
                () -> assertEquals(0.20, model.adjustedVolatility(), 0.0)
        );
    }

    @Test
    void ignoresOutOfRange() {
        ZonedDateTime expiry = VALUATION.plusDays(365);
        DividendSchedule schedule = new DividendSchedule(List.of(
                dividend(VALUATION.minusDays(1), 1.0),
                dividend(VALUATION, 1.0),
                dividend(expiry, 1.0),
                dividend(expiry.plusDays(1), 1.0)
        ));
        BosGairatShepeleva model = new BosGairatShepeleva(
                contract(expiry),
                new EquityFrame(VALUATION, 100.0, 0.05, 0.0),
                schedule,
                0.20,
                ACT_365F
        );

        assertAll(
                () -> assertEquals(100.0, model.adjustedSpot(), 0.0),
                () -> assertEquals(0.20, model.adjustedVolatility(), 0.0)
        );
    }

    @Test
    void rejectsExcessiveDividends() {
        DividendSchedule schedule = schedule(
                VALUATION.plusDays(182).plusHours(12),
                110.0
        );

        assertThrows(
                NonPositivePriceException.class,
                () -> new BosGairatShepeleva(
                        contract(VALUATION.plusDays(365)),
                        new EquityFrame(VALUATION, 100.0, 0.05, 0.0),
                        schedule,
                        0.20,
                        ACT_365F
                )
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

    private static DividendSchedule schedule(ZonedDateTime exDividend, double amount) {
        return new DividendSchedule(List.of(dividend(exDividend, amount)));
    }

    private static CashDividend dividend(ZonedDateTime exDividend, double amount) {
        return new CashDividend(EpochNanos.from(exDividend), amount);
    }
}
