package american;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.exception.InvalidStepCountException;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.frame.MarketData;
import com.thegreeklab.finance.model.american.binomial.BinomialModel;
import com.thegreeklab.finance.model.american.binomial.CoxRossRubenstein;
import com.thegreeklab.finance.model.american.binomial.LeisenReimer;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BinomialGreeksTest {

    private static final double SECONDS_IN_YEAR = 365.0 * 86_400.0;

    @Test
    void crrSigns() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        assertVanillaGreekSanity(
                new CoxRossRubenstein(contract(now, OptionType.CALL), frame, 0.2, 101),
                new CoxRossRubenstein(contract(now, OptionType.PUT), frame, 0.2, 101)
        );
    }

    @Test
    void leisenReimerSigns() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        assertVanillaGreekSanity(
                new LeisenReimer(contract(now, OptionType.CALL), frame, 0.2, 101),
                new LeisenReimer(contract(now, OptionType.PUT), frame, 0.2, 101)
        );
    }

    @Test
    void crrRejectsFewSteps() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        CoxRossRubenstein oneStep = new CoxRossRubenstein(contract(now, OptionType.CALL), frame, 0.2, 1);
        CoxRossRubenstein twoSteps = new CoxRossRubenstein(contract(now, OptionType.CALL), frame, 0.2, 2);

        assertAll(
                () -> assertThrows(InvalidStepCountException.class, oneStep::delta),
                () -> assertThrows(InvalidStepCountException.class, twoSteps::gamma),
                () -> assertThrows(InvalidStepCountException.class, twoSteps::theta)
        );
    }

    private static void assertVanillaGreekSanity(BinomialModel call, BinomialModel put) {
        assertAll(
                () -> assertTrue(call.price() > 0.0),
                () -> assertTrue(put.price() > 0.0),
                () -> assertTrue(call.delta() > 0.0),
                () -> assertTrue(put.delta() < 0.0),
                () -> assertTrue(call.gamma() >= 0.0),
                () -> assertTrue(put.gamma() >= 0.0),
                () -> assertTrue(call.vega() >= 0.0),
                () -> assertTrue(put.vega() >= 0.0),
                () -> assertTrue(call.rho() > 0.0),
                () -> assertTrue(Double.isFinite(call.theta())),
                () -> assertTrue(Double.isFinite(put.theta())),
                () -> assertTrue(Double.isFinite(call.vanna())),
                () -> assertTrue(Double.isFinite(call.volga())),
                () -> assertTrue(Double.isFinite(call.charm())),
                () -> assertTrue(Double.isFinite(call.speed())),
                () -> assertTrue(Double.isFinite(call.lambda())),
                () -> assertTrue(Double.isFinite(call.dualDelta())),
                () -> assertTrue(Double.isFinite(call.vera())),
                () -> assertTrue(Double.isFinite(call.zomma())),
                () -> assertTrue(Double.isFinite(call.color())),
                () -> assertTrue(Double.isFinite(call.ultima())),
                () -> assertTrue(Double.isFinite(call.dualGamma()))
        );
    }

    private static OptionContract contract(ZonedDateTime now, OptionType type) {
        ZonedDateTime expiry = now.plusNanos((long) (SECONDS_IN_YEAR * 1_000_000_000L));
        return new OptionContract(
                "TEST",
                type,
                Option.AMERICAN,
                100.0,
                expiry,
                100,
                MarketData.toEpochNanos(expiry),
                SECONDS_IN_YEAR
        );
    }
}
