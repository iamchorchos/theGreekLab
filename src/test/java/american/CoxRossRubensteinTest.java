package american;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.exception.InvalidVolatilityException;
import com.thegreeklab.finance.exception.MathException;
import com.thegreeklab.finance.exception.UnsupportedExerciseStyleException;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.frame.MarketData;
import com.thegreeklab.finance.model.american.binomial.CoxRossRubenstein;
import com.thegreeklab.finance.model.european.BlackScholesMerton;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoxRossRubensteinTest {

    private static final double SECONDS_IN_YEAR = 365.0 * 86_400.0;

    @Test
    void rejectsLowVolatility() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = contract(now, Option.AMERICAN, OptionType.CALL);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        assertThrows(
                InvalidVolatilityException.class,
                () -> new CoxRossRubenstein(contract, frame, 0.0, 101)
        );
    }

    @Test
    void rejectsNaN() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = contract(now, Option.AMERICAN, OptionType.CALL);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        assertThrows(
                InvalidVolatilityException.class,
                () -> new CoxRossRubenstein(contract, frame, Double.NaN, 101)
        );
    }

    @Test
    void rejectsEuropean() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = contract(now, Option.EUROPEAN, OptionType.CALL);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        assertThrows(
                UnsupportedExerciseStyleException.class,
                () -> new CoxRossRubenstein(contract, frame, 0.2, 101)
        );
    }

    @Test
    void rejectsInvalidProbability() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = contract(now, Option.AMERICAN, OptionType.CALL);
        EquityFrame frame = new EquityFrame(now, 100.0, 5.0, 0.0);

        assertThrows(
                MathException.class,
                () -> new CoxRossRubenstein(contract, frame, 0.01, 1)
        );
    }

    @Test
    void callConverges() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract americanCall = contract(now, Option.AMERICAN, OptionType.CALL);
        OptionContract europeanCall = contract(now, Option.EUROPEAN, OptionType.CALL);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        double crrPrice = new CoxRossRubenstein(americanCall, frame, 0.2, 1001).price();
        double blackScholesPrice = new BlackScholesMerton(europeanCall, frame, 0.2).price();

        assertEquals(blackScholesPrice, crrPrice, 0.02);
    }

    @Test
    void putDominatesEuropean() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract americanPut = contract(now, Option.AMERICAN, OptionType.PUT);
        OptionContract europeanPut = contract(now, Option.EUROPEAN, OptionType.PUT);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        double americanPrice = new CoxRossRubenstein(americanPut, frame, 0.2, 1001).price();
        double europeanPrice = new BlackScholesMerton(europeanPut, frame, 0.2).price();

        assertTrue(americanPrice >= europeanPrice - 1e-10);
    }

    @Test
    void stepsImproveConvergence() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract americanCall = contract(now, Option.AMERICAN, OptionType.CALL);
        OptionContract europeanCall = contract(now, Option.EUROPEAN, OptionType.CALL);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        double blackScholesPrice = new BlackScholesMerton(europeanCall, frame, 0.2).price();
        double coarseError = Math.abs(new CoxRossRubenstein(americanCall, frame, 0.2, 101).price() - blackScholesPrice);
        double fineError = Math.abs(new CoxRossRubenstein(americanCall, frame, 0.2, 1001).price() - blackScholesPrice);

        assertTrue(fineError < coarseError);
    }

    private static OptionContract contract(ZonedDateTime now, Option option, OptionType type) {
        ZonedDateTime expiry = now.plusNanos((long) (SECONDS_IN_YEAR * 1_000_000_000L));
        return new OptionContract(
                "TEST",
                type,
                option,
                100.0,
                expiry,
                100,
                MarketData.toEpochNanos(expiry),
                SECONDS_IN_YEAR
        );
    }
}
