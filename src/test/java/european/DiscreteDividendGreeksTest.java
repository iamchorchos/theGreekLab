package european;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.european.BlackScholesMerton;
import com.thegreeklab.finance.model.european.discrete.CashDividend;
import com.thegreeklab.finance.model.european.discrete.DividendSchedule;
import com.thegreeklab.finance.model.european.discrete.adjustments.BosGairatShepeleva;
import com.thegreeklab.finance.model.european.discrete.adjustments.BosVandermark;
import com.thegreeklab.finance.model.european.discrete.adjustments.DiscreteDividendOptionModel;
import com.thegreeklab.finance.model.european.discrete.adjustments.HaugHaugAdjustment;
import com.thegreeklab.finance.model.european.discrete.adjustments.SimpleVolatilityAdjustment;
import com.thegreeklab.finance.model.greeks.StandardGreekValues;
import com.thegreeklab.finance.time.EpochNanos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static com.thegreeklab.finance.time.DayCountConvention.ACT_365F;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscreteDividendGreeksTest {

    private static final ZonedDateTime VALUATION = ZonedDateTime.of(
            2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
    );
    private static final ZonedDateTime EXPIRY = VALUATION.plusDays(365);
    private static final OptionContract CONTRACT = contract();
    private static final EquityFrame FRAME = new EquityFrame(
            VALUATION, 100.0, 0.05, 0.0
    );

    @ParameterizedTest(name = "{0} matches BSM without dividends")
    @EnumSource(ModelKind.class)
    void matchesBlackScholesWithoutDividends(ModelKind kind) {
        double volatility = 0.20;
        DiscreteDividendOptionModel model = model(
                kind,
                FRAME,
                new DividendSchedule(List.of()),
                volatility
        );
        BlackScholesMerton reference = new BlackScholesMerton(
                CONTRACT, FRAME, volatility, ACT_365F
        );

        // Spot, volatility and rate use central differences. Theta uses a
        // one-day forward difference and therefore has the wider tolerance.
        assertAll(
                () -> assertEquals(reference.price(), model.price(), 0.0),
                () -> assertEquals(reference.delta(), model.delta(), 1e-7),
                () -> assertEquals(reference.gamma(), model.gamma(), 1e-6),
                () -> assertEquals(reference.vega(), model.vega(), 1e-6),
                () -> assertEquals(reference.theta(), model.theta(), 3e-2),
                () -> assertEquals(reference.rho(), model.rho(), 1e-5)
        );
    }

    @ParameterizedTest(name = "{0} returns finite dividend Greeks")
    @EnumSource(ModelKind.class)
    void returnsFiniteGreeks(ModelKind kind) {
        DividendSchedule schedule = new DividendSchedule(List.of(
                dividend(VALUATION.plusDays(182).plusHours(12), 7.0)
        ));
        DiscreteDividendOptionModel model = model(
                kind,
                new EquityFrame(VALUATION, 100.0, 0.06, 0.0),
                schedule,
                0.30
        );

        StandardGreekValues greeks = model.greeks();

        assertAll(
                () -> assertTrue(Double.isFinite(greeks.price())),
                () -> assertTrue(Double.isFinite(greeks.delta())),
                () -> assertTrue(Double.isFinite(greeks.gamma())),
                () -> assertTrue(Double.isFinite(greeks.vega())),
                () -> assertTrue(Double.isFinite(greeks.theta())),
                () -> assertTrue(Double.isFinite(greeks.rho()))
        );
    }

    @Test
    void rewindRestoresEarlierDividends() {
        ZonedDateTime initialValuation = VALUATION.plusDays(9);
        ZonedDateTime earlierValuation = VALUATION;
        ZonedDateTime firstExDate = VALUATION.plusDays(4);
        ZonedDateTime secondExDate = VALUATION.plusDays(182).plusHours(12);
        DividendSchedule schedule = new DividendSchedule(List.of(
                dividend(firstExDate, 1.0),
                dividend(secondExDate, 2.0)
        ));
        SimpleVolatilityAdjustment model = new SimpleVolatilityAdjustment(
                CONTRACT,
                new EquityFrame(initialValuation, 100.0, 0.05, 0.0),
                schedule,
                0.20,
                ACT_365F
        );

        DiscreteDividendOptionModel rewound = model.withTimestamp(
                EpochNanos.from(earlierValuation)
        );
        double expectedPresentValue = Math.exp(
                -0.05 * ACT_365F.yearFraction(
                        EpochNanos.from(earlierValuation),
                        EpochNanos.from(firstExDate)
                )
        ) + 2.0 * Math.exp(
                -0.05 * ACT_365F.yearFraction(
                        EpochNanos.from(earlierValuation),
                        EpochNanos.from(secondExDate)
                )
        );

        assertEquals(100.0 - expectedPresentValue, rewound.adjustedSpot(), 1e-12);
    }

    private static DiscreteDividendOptionModel model(
            ModelKind kind,
            EquityFrame frame,
            DividendSchedule schedule,
            double volatility
    ) {
        return switch (kind) {
            case SIMPLE -> new SimpleVolatilityAdjustment(
                    CONTRACT, frame, schedule, volatility, ACT_365F
            );
            case HAUG_HAUG -> new HaugHaugAdjustment(
                    CONTRACT, frame, schedule, volatility, ACT_365F
            );
            case BOS_GAIRAT_SHEPELEVA -> new BosGairatShepeleva(
                    CONTRACT, frame, schedule, volatility, ACT_365F
            );
            case BOS_VANDERMARK -> new BosVandermark(
                    CONTRACT, frame, schedule, volatility, ACT_365F
            );
        };
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

    private static CashDividend dividend(ZonedDateTime exDividend, double amount) {
        return new CashDividend(EpochNanos.from(exDividend), amount);
    }

    private enum ModelKind {
        SIMPLE,
        HAUG_HAUG,
        BOS_GAIRAT_SHEPELEVA,
        BOS_VANDERMARK
    }
}
