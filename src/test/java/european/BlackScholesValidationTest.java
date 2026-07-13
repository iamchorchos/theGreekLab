package european;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.exception.InvalidVolatilityException;
import com.thegreeklab.finance.exception.UnsupportedExerciseStyleException;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.european.BlackScholes;
import com.thegreeklab.finance.model.european.BlackScholesMerton;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlackScholesValidationTest {

    @Test
    void constructorRejectsNonFinite() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = contract(now);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        assertThrows(
                InvalidVolatilityException.class,
                () -> new BlackScholesMerton(contract, frame, Double.NaN)
        );
        assertThrows(
                InvalidVolatilityException.class,
                () -> new BlackScholesMerton(contract, frame, Double.POSITIVE_INFINITY)
        );
    }

    @Test
    void priceRejectsNonFinite() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = contract(now);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        assertThrows(
                InvalidVolatilityException.class,
                () -> BlackScholes.price(contract, frame, Double.NaN)
        );
        assertThrows(
                InvalidVolatilityException.class,
                () -> BlackScholes.price(contract, frame, Double.NEGATIVE_INFINITY)
        );
    }

    @Test
    void priceRejectsAmerican() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = contract(now, Option.AMERICAN);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        assertThrows(
                UnsupportedExerciseStyleException.class,
                () -> BlackScholes.price(contract, frame, 0.2)
        );
    }

    @Test
    void constructorRejectsAmerican() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = contract(now, Option.AMERICAN);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        assertThrows(
                UnsupportedExerciseStyleException.class,
                () -> new BlackScholesMerton(contract, frame, 0.2)
        );
    }

    @Test
    void equityEpsilonDefined() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = contract(now);
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.01);
        BlackScholesMerton option = new BlackScholesMerton(contract, frame, 0.2);

        assertTrue(Double.isFinite(option.epsilon()));
    }

    private static OptionContract contract(ZonedDateTime now) {
        return contract(now, Option.EUROPEAN);
    }

    private static OptionContract contract(ZonedDateTime now, Option option) {
        return new OptionContract(
                "TEST",
                OptionType.CALL,
                option,
                100.0,
                now.plusYears(1),
                100
        );
    }
}
