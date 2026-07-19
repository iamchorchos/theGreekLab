package european;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.exception.InvalidVolatilityException;
import com.thegreeklab.finance.exception.NonPositivePriceException;
import com.thegreeklab.finance.exception.UnsupportedExerciseStyleException;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.european.BlackScholes;
import com.thegreeklab.finance.model.european.discrete.CashDividend;
import com.thegreeklab.finance.model.european.discrete.DividendSchedule;
import com.thegreeklab.finance.model.european.discrete.adjustments.SimpleVolatilityAdjustment;
import com.thegreeklab.finance.time.EpochNanos;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static com.thegreeklab.finance.time.DayCountConvention.ACT_365F;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimpleVolatilityAdjustmentTest {

    private static final ZonedDateTime VALUATION = ZonedDateTime.of(
            2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
    );
    private static final ZonedDateTime EXPIRY = VALUATION.plusDays(365);
    private static final EquityFrame FRAME = new EquityFrame(
            VALUATION, 100.0, 0.05, 0.0
    );

    @Test
    void adjustsInputsAndPrice() {
        DividendSchedule schedule = new DividendSchedule(List.of(
                dividend(VALUATION.plusDays(91).plusHours(6), 2.0),
                dividend(VALUATION.plusDays(182).plusHours(12), 3.0)
        ));
        SimpleVolatilityAdjustment model = model(contract(Option.EUROPEAN), FRAME, schedule, 0.20);

        double dividendPresentValue = 2.0 * Math.exp(-0.05 * 0.25)
                + 3.0 * Math.exp(-0.05 * 0.5);
        double adjustedSpot = 100.0 - dividendPresentValue;
        double adjustedVolatility = 0.20 * 100.0 / adjustedSpot;
        double expectedPrice = BlackScholes.price(
                contract(Option.EUROPEAN),
                new EquityFrame(VALUATION, adjustedSpot, 0.05, 0.0),
                adjustedVolatility,
                ACT_365F
        );

        assertAll(
                () -> assertEquals(adjustedSpot, model.adjustedSpot(), 1e-12),
                () -> assertEquals(adjustedVolatility, model.adjustedVolatility(), 1e-12),
                () -> assertEquals(expectedPrice, model.price(), 1e-12)
        );
    }

    @Test
    void emptyScheduleKeepsInputs() {
        SimpleVolatilityAdjustment model = model(
                contract(Option.EUROPEAN),
                FRAME,
                new DividendSchedule(List.of()),
                0.20
        );

        assertAll(
                () -> assertEquals(100.0, model.adjustedSpot(), 0.0),
                () -> assertEquals(0.20, model.adjustedVolatility(), 0.0),
                () -> assertEquals(
                        BlackScholes.price(contract(Option.EUROPEAN), FRAME, 0.20, ACT_365F),
                        model.price(),
                        0.0
                )
        );
    }

    @Test
    void ignoresOutOfRange() {
        DividendSchedule schedule = new DividendSchedule(List.of(
                dividend(VALUATION.minusDays(1), 1.0),
                dividend(VALUATION, 1.0),
                dividend(EXPIRY, 1.0),
                dividend(EXPIRY.plusDays(1), 1.0)
        ));
        SimpleVolatilityAdjustment model = model(
                contract(Option.EUROPEAN), FRAME, schedule, 0.20
        );

        assertAll(
                () -> assertEquals(100.0, model.adjustedSpot(), 0.0),
                () -> assertEquals(0.20, model.adjustedVolatility(), 0.0)
        );
    }

    @Test
    void rejectsExcessiveDividends() {
        DividendSchedule schedule = new DividendSchedule(List.of(
                dividend(VALUATION.plusDays(182).plusHours(12), 110.0)
        ));

        assertThrows(
                NonPositivePriceException.class,
                () -> model(contract(Option.EUROPEAN), FRAME, schedule, 0.20)
        );
    }

    @Test
    void rejectsInvalidModelInputs() {
        DividendSchedule emptySchedule = new DividendSchedule(List.of());
        OptionContract europeanContract = contract(Option.EUROPEAN);

        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> model(null, FRAME, emptySchedule, 0.20)
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> model(europeanContract, null, emptySchedule, 0.20)
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> model(europeanContract, FRAME, null, 0.20)
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new SimpleVolatilityAdjustment(
                                europeanContract,
                                FRAME,
                                emptySchedule,
                                0.20,
                                null
                        )
                ),
                () -> assertThrows(
                        InvalidVolatilityException.class,
                        () -> model(europeanContract, FRAME, emptySchedule, Double.NaN)
                ),
                () -> assertThrows(
                        UnsupportedExerciseStyleException.class,
                        () -> model(contract(Option.AMERICAN), FRAME, emptySchedule, 0.20)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> model(
                                europeanContract,
                                new EquityFrame(VALUATION, 100.0, 0.05, 0.01),
                                emptySchedule,
                                0.20
                        )
                )
        );
    }

    private static SimpleVolatilityAdjustment model(
            OptionContract contract,
            EquityFrame frame,
            DividendSchedule schedule,
            double volatility
    ) {
        return new SimpleVolatilityAdjustment(
                contract, frame, schedule, volatility, ACT_365F
        );
    }

    private static OptionContract contract(Option option) {
        return new OptionContract(
                "TEST",
                OptionType.CALL,
                option,
                100.0,
                EXPIRY,
                100
        );
    }

    private static CashDividend dividend(ZonedDateTime exDividend, double amount) {
        return new CashDividend(EpochNanos.from(exDividend), amount);
    }
}
