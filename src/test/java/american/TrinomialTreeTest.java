package american;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.exception.ExpiredContractException;
import com.thegreeklab.finance.exception.InvalidStepCountException;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.american.trinomial.TrinomialTree;
import com.thegreeklab.finance.model.european.BlackScholesMerton;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.thegreeklab.finance.time.DayCountConvention.ACT_365F;
import static com.thegreeklab.finance.time.DayCountConvention.ACT_360;

class TrinomialTreeTest {

    private static final double SECONDS_IN_YEAR = 365.0 * 86_400.0;

    @Test
    void rejectsExpired() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = contract(now, Option.EUROPEAN, OptionType.CALL);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        assertThrows(
                ExpiredContractException.class,
                () -> new TrinomialTree(contract, frame, 0.20, 100, ACT_365F)
        );
    }

    @Test
    void callConvergesToBlackScholes() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime expiry = now.plusNanos((long) (SECONDS_IN_YEAR * 1_000_000_000L));
        OptionContract contract = contract(expiry, Option.EUROPEAN, OptionType.CALL);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        double trinomialPrice = new TrinomialTree(contract, frame, 0.20, 500, ACT_365F).price();
        double blackScholesPrice = new BlackScholesMerton(contract, frame, 0.20, ACT_365F).price();

        assertEquals(blackScholesPrice, trinomialPrice, 0.005);
    }

    @Test
    void greeksConvergeToBlackScholes() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime expiry = now.plusNanos((long) (SECONDS_IN_YEAR * 1_000_000_000L));
        OptionContract contract = contract(expiry, Option.EUROPEAN, OptionType.CALL);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);
        TrinomialTree trinomial = new TrinomialTree(contract, frame, 0.20, 500, ACT_365F);
        BlackScholesMerton blackScholes = new BlackScholesMerton(contract, frame, 0.20, ACT_365F);

        assertEquals(blackScholes.delta(), trinomial.delta(), 0.002);
        assertEquals(blackScholes.gamma(), trinomial.gamma(), 0.0002);
        assertEquals(blackScholes.theta(), trinomial.theta(), 0.05);
        assertEquals(blackScholes.vega(), trinomial.vega(), 0.05);
        assertEquals(blackScholes.rho(), trinomial.rho(), 0.05);
    }

    @Test
    void bumpsPreserveState() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime expiry = now.plusNanos((long) (SECONDS_IN_YEAR * 1_000_000_000L));
        OptionContract contract = contract(expiry, Option.EUROPEAN, OptionType.CALL);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);
        TrinomialTree original = new TrinomialTree(contract, frame, 0.20, 200, ACT_360);
        double originalPrice = original.price();
        long laterTimestamp = frame.timestampNanos() + 86_400_000_000_000L;

        TrinomialTree spotBumped = original.withSpot(101.0);
        TrinomialTree volatilityBumped = original.withVolatility(0.25);
        TrinomialTree rateBumped = original.withRiskFreeRate(0.06);
        TrinomialTree timeBumped = original.withTimestamp(laterTimestamp);

        assertEquals(originalPrice, original.price());
        assertEquals(ACT_360, original.dayCountConvention());
        assertEquals(ACT_360, spotBumped.dayCountConvention());
        assertEquals(ACT_360, volatilityBumped.dayCountConvention());
        assertEquals(ACT_360, rateBumped.dayCountConvention());
        assertEquals(ACT_360, timeBumped.dayCountConvention());
        assertTrue(spotBumped.price() > originalPrice);
        assertEquals(
                new TrinomialTree(contract, frame, 0.25, 200, ACT_360).price(),
                volatilityBumped.price()
        );
        assertEquals(
                volatilityBumped.price(),
                original.priceAtVolatility(0.25)
        );
        assertEquals(
                new TrinomialTree(
                        contract, frame.withRiskFreeRate(0.06), 0.20, 200, ACT_360
                ).price(),
                rateBumped.price()
        );
        assertEquals(
                new TrinomialTree(
                        contract, frame.withTimestampNanos(laterTimestamp), 0.20, 200, ACT_360
                ).price(),
                timeBumped.price()
        );
    }

    @Test
    void americanPutIncludesExercisePremium() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime expiry = now.plusNanos((long) (SECONDS_IN_YEAR * 1_000_000_000L));
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);
        OptionContract europeanPut = contract(expiry, Option.EUROPEAN, OptionType.PUT);
        OptionContract americanPut = contract(expiry, Option.AMERICAN, OptionType.PUT);

        double europeanPrice = new TrinomialTree(europeanPut, frame, 0.20, 200, ACT_365F).price();
        double americanPrice = new TrinomialTree(americanPut, frame, 0.20, 200, ACT_365F).price();

        assertTrue(americanPrice >= europeanPrice);
    }

    @Test
    void zeroCarryIsFinite() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime expiry = now.plusNanos((long) (SECONDS_IN_YEAR * 1_000_000_000L));
        OptionContract contract = contract(expiry, Option.EUROPEAN, OptionType.CALL);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.05);

        double price = new TrinomialTree(contract, frame, 0.20, 100, ACT_365F).price();

        assertTrue(Double.isFinite(price));
    }

    @Test
    void enforcesStepMinimum() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime expiry = now.plusNanos((long) (SECONDS_IN_YEAR * 1_000_000_000L));
        OptionContract contract = contract(expiry, Option.EUROPEAN, OptionType.CALL);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        assertThrows(
                InvalidStepCountException.class,
                () -> new TrinomialTree(contract, frame, 0.02, 3, ACT_365F)
        );
        assertTrue(Double.isFinite(new TrinomialTree(contract, frame, 0.02, 4, ACT_365F).price()));
    }

    @Test
    void vegaHandlesStepBoundary() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime expiry = now.plusNanos((long) (SECONDS_IN_YEAR * 1_000_000_000L));
        OptionContract contract = contract(expiry, Option.EUROPEAN, OptionType.CALL);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);
        TrinomialTree model = new TrinomialTree(contract, frame, 0.02, 4, ACT_365F);

        assertTrue(Double.isFinite(model.vega()));
    }

    @Test
    void rhoHandlesStepBoundary() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime expiry = now.plusNanos((long) (SECONDS_IN_YEAR * 1_000_000_000L));
        OptionContract contract = contract(expiry, Option.EUROPEAN, OptionType.CALL);
        double rate = Math.sqrt(12.999 * 2.0 * 0.20 * 0.20);
        EquityFrame frame = new EquityFrame(now, 100.0, rate, 0.0);
        TrinomialTree model = new TrinomialTree(contract, frame, 0.20, 13, ACT_365F);

        assertTrue(Double.isFinite(model.rho()));
    }

    @Test
    void rejectsTooManySteps() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime expiry = now.plusNanos((long) (SECONDS_IN_YEAR * 1_000_000_000L));
        OptionContract contract = contract(expiry, Option.EUROPEAN, OptionType.CALL);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        assertThrows(
                InvalidStepCountException.class,
                () -> new TrinomialTree(
                        contract, frame, 0.20, TrinomialTree.MAX_STEPS + 1, ACT_365F
                )
        );
    }

    private static OptionContract contract(
            ZonedDateTime expiry,
            Option style,
            OptionType type
    ) {
        return new OptionContract(
                "TEST",
                type,
                style,
                100.0,
                expiry,
                100
        );
    }
}
