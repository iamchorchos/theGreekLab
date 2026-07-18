package american;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.exception.InvalidStepCountException;
import com.thegreeklab.finance.exception.InvalidVolatilityException;
import com.thegreeklab.finance.exception.UnsupportedExerciseStyleException;
import com.thegreeklab.finance.exception.UnsupportedFrameTypeException;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.frame.FuturesFrame;
import com.thegreeklab.finance.model.american.binomial.LeisenReimer;
import com.thegreeklab.finance.model.european.BlackScholesMerton;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static com.thegreeklab.finance.time.DayCountConvention.ACT_365F;

class LeisenReimerTest {

    private static final double SECONDS_IN_YEAR = 365.0 * 86_400.0;

    @Test
    void rejectsNaN() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = contract(now, Option.AMERICAN);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        assertThrows(
                InvalidVolatilityException.class,
                () -> new LeisenReimer(contract, frame, Double.NaN, 101, ACT_365F)
        );
    }

    @Test
    void rejectsEvenSteps() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = contract(now, Option.AMERICAN);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        assertThrows(
                InvalidStepCountException.class,
                () -> new LeisenReimer(contract, frame, 0.2, 100, ACT_365F)
        );
    }

    @Test
    void rejectsFutures() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = contract(now, Option.AMERICAN);
        FuturesFrame frame = new FuturesFrame(now, 100.0, 0.05);

        assertThrows(
                UnsupportedFrameTypeException.class,
                () -> new LeisenReimer(contract, frame, 0.2, 101, ACT_365F)
        );
    }

    @Test
    void rejectsEuropean() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = contract(now, Option.EUROPEAN);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        assertThrows(
                UnsupportedExerciseStyleException.class,
                () -> new LeisenReimer(contract, frame, 0.2, 101, ACT_365F)
        );
    }

    @Test
    void callConverges() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract americanCall = contract(now, Option.AMERICAN);
        OptionContract europeanCall = contract(now, Option.EUROPEAN);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        double leisenReimerPrice = new LeisenReimer(americanCall, frame, 0.2, 101, ACT_365F).price();
        double blackScholesPrice = new BlackScholesMerton(europeanCall, frame, 0.2, ACT_365F).price();

        assertEquals(blackScholesPrice, leisenReimerPrice, 0.02);
    }

    private static OptionContract contract(ZonedDateTime now, Option option) {
        ZonedDateTime expiry = now.plusNanos((long) (SECONDS_IN_YEAR * 1_000_000_000L));
        return new OptionContract(
                "TEST",
                OptionType.CALL,
                option,
                100.0,
                expiry,
                100
        );
    }
}
