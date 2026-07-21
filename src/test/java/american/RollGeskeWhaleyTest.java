package american;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.exception.ExpiredContractException;
import com.thegreeklab.finance.exception.InvalidDateException;
import com.thegreeklab.finance.exception.NonPositivePriceException;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.american.discrete.RollGeskeWhaley;
import com.thegreeklab.finance.model.european.BlackScholesMerton;
import com.thegreeklab.finance.model.european.discrete.CashDividend;
import com.thegreeklab.finance.model.greeks.StandardGreekValues;
import com.thegreeklab.finance.time.EpochNanos;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static com.thegreeklab.finance.time.DayCountConvention.ACT_365F;
import static com.thegreeklab.finance.time.DayCountConvention.ACT_360;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RollGeskeWhaleyTest {

    private static final double SECONDS_PER_YEAR = 365.0 * 86_400.0;

    @Test
    void matchesHaugExample() {
        ZonedDateTime valuation = ZonedDateTime.of(
                2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
        );
        long valuationTimestamp = EpochNanos.from(valuation);
        long dividendTimestamp = timestampAfter(valuationTimestamp, 0.25);
        ZonedDateTime expiration = EpochNanos.toUtc(
                timestampAfter(valuationTimestamp, 1.0 / 3.0)
        );

        RollGeskeWhaley model = new RollGeskeWhaley(
                contract(Option.AMERICAN, 82.0, expiration),
                new EquityFrame(valuationTimestamp, 80.0, 0.06, 0.0),
                0.30,
                new CashDividend(dividendTimestamp, 4.0),
                ACT_365F
        );

        // Haug example: critical price 80.1173 and option value 4.3860.
        assertEquals(4.3860, model.price(), 1e-4);
    }

    @Test
    void matchesMathWorksEuropeanBranch() {
        ZonedDateTime valuation = ZonedDateTime.of(
                2008, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC
        );
        ZonedDateTime dividendDate = ZonedDateTime.of(
                2008, 12, 1, 0, 0, 0, 0, ZoneOffset.UTC
        );
        ZonedDateTime expiration = ZonedDateTime.of(
                2009, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC
        );
        EquityFrame frame = new EquityFrame(valuation, 80.0, 0.06, 0.0);
        CashDividend dividend = new CashDividend(
                EpochNanos.from(dividendDate),
                0.001
        );

        RollGeskeWhaley strike110 = new RollGeskeWhaley(
                contract(Option.AMERICAN, 110.0, expiration),
                frame,
                0.20,
                dividend,
                ACT_365F
        );
        RollGeskeWhaley strike100 = new RollGeskeWhaley(
                contract(Option.AMERICAN, 100.0, expiration),
                frame,
                0.20,
                dividend,
                ACT_365F
        );

        // MathWorks publishes four-decimal values using Actual/Actual. The
        // wider tolerance accounts for this model's explicit ACT/365F input.
        assertAll(
                () -> assertEquals(0.8398, strike110.price(), 5e-4),
                () -> assertEquals(2.0236, strike100.price(), 5e-4)
        );
    }

    @Test
    void rejectsNullInputs() {
        ZonedDateTime valuation = ZonedDateTime.of(
                2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
        );
        ZonedDateTime expiration = valuation.plusYears(1);
        OptionContract contract = contract(
                Option.AMERICAN,
                100.0,
                expiration
        );
        EquityFrame frame = new EquityFrame(valuation, 100.0, 0.05, 0.0);
        CashDividend dividend = new CashDividend(
                EpochNanos.from(valuation.plusMonths(6)),
                2.0
        );

        assertAll(
                () -> assertThrows(NullPointerException.class, () ->
                        new RollGeskeWhaley(null, frame, 0.20, dividend, ACT_365F)),
                () -> assertThrows(NullPointerException.class, () ->
                        new RollGeskeWhaley(contract, null, 0.20, dividend, ACT_365F)),
                () -> assertThrows(NullPointerException.class, () ->
                        new RollGeskeWhaley(contract, frame, 0.20, null, ACT_365F)),
                () -> assertThrows(NullPointerException.class, () ->
                        new RollGeskeWhaley(contract, frame, 0.20, dividend, null))
        );
    }

    @Test
    void rejectsInvalidDates() {
        ZonedDateTime valuation = ZonedDateTime.of(
                2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
        );
        ZonedDateTime expiration = valuation.plusYears(1);
        OptionContract contract = contract(
                Option.AMERICAN,
                100.0,
                expiration
        );
        EquityFrame frame = new EquityFrame(valuation, 100.0, 0.05, 0.0);

        assertAll(
                () -> assertThrows(InvalidDateException.class, () ->
                        new RollGeskeWhaley(
                                contract,
                                frame,
                                0.20,
                                new CashDividend(frame.timestampNanos(), 2.0),
                                ACT_365F
                        )),
                () -> assertThrows(InvalidDateException.class, () ->
                        new RollGeskeWhaley(
                                contract,
                                frame,
                                0.20,
                                new CashDividend(EpochNanos.from(expiration), 2.0),
                                ACT_365F
                        )),
                () -> assertThrows(ExpiredContractException.class, () ->
                        new RollGeskeWhaley(
                                contract,
                                frame.withTimestampNanos(EpochNanos.from(expiration)),
                                0.20,
                                new CashDividend(EpochNanos.from(expiration.plusDays(1)), 2.0),
                                ACT_365F
                        ))
        );
    }

    @Test
    void rejectsNonPositiveAdjustedSpot() {
        ZonedDateTime valuation = ZonedDateTime.of(
                2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
        );
        ZonedDateTime expiration = valuation.plusYears(1);

        assertThrows(NonPositivePriceException.class, () ->
                new RollGeskeWhaley(
                        contract(Option.AMERICAN, 100.0, expiration),
                        new EquityFrame(valuation, 10.0, 0.0, 0.0),
                        0.20,
                        new CashDividend(EpochNanos.from(valuation.plusMonths(6)), 10.0),
                        ACT_365F
                ));
    }

    @Test
    void respectsIntrinsicValueWithNegativeRate() {
        ZonedDateTime valuation = ZonedDateTime.of(
                2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
        );
        ZonedDateTime expiration = valuation.plusYears(1);
        RollGeskeWhaley model = new RollGeskeWhaley(
                contract(Option.AMERICAN, 100.0, expiration),
                new EquityFrame(valuation, 220.0, -0.05, 0.0),
                0.20,
                new CashDividend(EpochNanos.from(valuation.plusMonths(6)), 110.0),
                ACT_365F
        );

        assertEquals(120.0, model.price(), 1e-12);
    }

    @Test
    void bumpsCreateEquivalentCopies() {
        ZonedDateTime valuation = ZonedDateTime.of(
                2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
        );
        ZonedDateTime expiration = valuation.plusYears(1);
        OptionContract contract = contract(
                Option.AMERICAN,
                100.0,
                expiration
        );
        EquityFrame frame = new EquityFrame(valuation, 105.0, 0.04, 0.0);
        CashDividend dividend = new CashDividend(
                EpochNanos.from(valuation.plusMonths(6)),
                3.0
        );
        RollGeskeWhaley model = new RollGeskeWhaley(
                contract,
                frame,
                0.25,
                dividend,
                ACT_360
        );
        long bumpedTimestamp = EpochNanos.from(valuation.plusDays(1));
        double originalPrice = model.price();

        assertAll(
                () -> assertEquals(
                        new RollGeskeWhaley(
                                contract,
                                frame.withSpotPrice(106.0),
                                0.25,
                                dividend,
                                ACT_360
                        ).price(),
                        model.withSpot(106.0).price(),
                        1e-12
                ),
                () -> assertEquals(
                        new RollGeskeWhaley(
                                contract,
                                frame,
                                0.26,
                                dividend,
                                ACT_360
                        ).price(),
                        model.withVolatility(0.26).price(),
                        1e-12
                ),
                () -> assertEquals(
                        new RollGeskeWhaley(
                                contract,
                                frame.withRiskFreeRate(0.05),
                                0.25,
                                dividend,
                                ACT_360
                        ).price(),
                        model.withRiskFreeRate(0.05).price(),
                        1e-12
                ),
                () -> assertEquals(
                        new RollGeskeWhaley(
                                contract,
                                frame.withTimestampNanos(bumpedTimestamp),
                                0.25,
                                dividend,
                                ACT_360
                        ).price(),
                        model.withTimestamp(bumpedTimestamp).price(),
                        1e-12
                ),
                () -> assertEquals(ACT_360, model.dayCountConvention()),
                () -> assertEquals(originalPrice, model.price(), 1e-12)
        );
    }

    @Test
    void greeksMatchEuropeanLimit() {
        ZonedDateTime valuation = ZonedDateTime.of(
                2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
        );
        ZonedDateTime dividendDate = valuation.plusMonths(6);
        ZonedDateTime expiration = valuation.plusYears(1);
        double spot = 100.0;
        double strike = 100.0;
        double rate = 0.05;
        double volatility = 0.20;
        double dividendAmount = 0.01;
        EquityFrame frame = new EquityFrame(valuation, spot, rate, 0.0);
        CashDividend dividend = new CashDividend(
                EpochNanos.from(dividendDate),
                dividendAmount
        );
        RollGeskeWhaley model = new RollGeskeWhaley(
                contract(Option.AMERICAN, strike, expiration),
                frame,
                volatility,
                dividend,
                ACT_365F
        );

        double timeToDividend = ACT_365F.yearFraction(
                frame.timestampNanos(),
                dividend.exTimestampNanos()
        );
        double presentValueOfDividend = dividendAmount
                * Math.exp(-rate * timeToDividend);
        double adjustedSpot = spot - presentValueOfDividend;
        BlackScholesMerton reference = new BlackScholesMerton(
                contract(Option.EUROPEAN, strike, expiration),
                frame.withSpotPrice(adjustedSpot),
                volatility,
                ACT_365F
        );
        double expectedRho = reference.rho()
                + reference.delta() * timeToDividend * presentValueOfDividend;
        double expectedTheta = reference.theta()
                - reference.delta() * rate * presentValueOfDividend;

        assertAll(
                () -> assertEquals(reference.delta(), model.delta(), 1e-8),
                () -> assertEquals(reference.gamma(), model.gamma(), 1e-7),
                () -> assertEquals(reference.vega(), model.vega(), 1e-6),
                () -> assertEquals(expectedTheta, model.theta(), 3e-2),
                () -> assertEquals(expectedRho, model.rho(), 1e-5)
        );
    }

    @Test
    void americanGreeksAreFinite() {
        ZonedDateTime valuation = ZonedDateTime.of(
                2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
        );
        long valuationTimestamp = EpochNanos.from(valuation);
        RollGeskeWhaley model = new RollGeskeWhaley(
                contract(
                        Option.AMERICAN,
                        82.0,
                        EpochNanos.toUtc(timestampAfter(valuationTimestamp, 1.0 / 3.0))
                ),
                new EquityFrame(valuationTimestamp, 80.0, 0.06, 0.0),
                0.30,
                new CashDividend(timestampAfter(valuationTimestamp, 0.25), 4.0),
                ACT_365F
        );
        StandardGreekValues values = model.greeks();

        assertAll(
                () -> assertTrue(Double.isFinite(values.price())),
                () -> assertTrue(Double.isFinite(values.delta())),
                () -> assertTrue(Double.isFinite(values.gamma())),
                () -> assertTrue(Double.isFinite(values.vega())),
                () -> assertTrue(Double.isFinite(values.theta())),
                () -> assertTrue(Double.isFinite(values.rho())),
                () -> assertEquals(model.price(), values.price(), 1e-12),
                () -> assertEquals(model.delta(), values.delta(), 1e-12),
                () -> assertEquals(model.gamma(), values.gamma(), 1e-12),
                () -> assertEquals(model.vega(), values.vega(), 1e-12),
                () -> assertEquals(model.theta(), values.theta(), 1e-12),
                () -> assertEquals(model.rho(), values.rho(), 1e-12)
        );
    }

    @Test
    void thetaStaysBeforeDividend() {
        ZonedDateTime dividendDate = ZonedDateTime.of(
                2026, 7, 1, 0, 0, 0, 0, ZoneOffset.UTC
        );
        ZonedDateTime valuation = dividendDate.minusHours(12);
        RollGeskeWhaley model = new RollGeskeWhaley(
                contract(
                        Option.AMERICAN,
                        100.0,
                        dividendDate.plusMonths(6)
                ),
                new EquityFrame(valuation, 105.0, 0.05, 0.0),
                0.25,
                new CashDividend(EpochNanos.from(dividendDate), 2.0),
                ACT_365F
        );

        assertTrue(Double.isFinite(model.theta()));
    }

    private static OptionContract contract(
            Option style,
            double strike,
            ZonedDateTime expiration
    ) {
        return new OptionContract(
                "TEST", OptionType.CALL, style, strike, expiration, 100
        );
    }

    private static long timestampAfter(long startTimestamp, double years) {
        return Math.addExact(
                startTimestamp,
                Math.round(years * SECONDS_PER_YEAR * 1_000_000_000.0)
        );
    }
}
