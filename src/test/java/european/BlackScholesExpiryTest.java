package european;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.european.BlackScholesMerton;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BlackScholesExpiryTest {

    private static final double TOLERANCE = 1e-12;

    @Test
    void itmCallAtExpiry() {
        ZonedDateTime expiry = ZonedDateTime.of(2026, 1, 16, 16, 0, 0, 0, ZoneOffset.UTC);
        OptionContract contract = contract(OptionType.CALL, expiry);
        EquityFrame frame = new EquityFrame(expiry, 110.0, 0.05, 0.0);

        BlackScholesMerton option = new BlackScholesMerton(contract, frame, 0.2);

        assertAll(
                () -> assertEquals(0.0, option.timeToExpiry(), TOLERANCE),
                () -> assertEquals(10.0, option.price(), TOLERANCE),
                () -> assertEquals(1.0, option.delta(), TOLERANCE),
                () -> assertExpiredGreeksAreZero(option)
        );
    }

    @Test
    void otmCallAtExpiry() {
        ZonedDateTime expiry = ZonedDateTime.of(2026, 1, 16, 16, 0, 0, 0, ZoneOffset.UTC);
        OptionContract contract = contract(OptionType.CALL, expiry);
        EquityFrame frame = new EquityFrame(expiry, 90.0, 0.05, 0.0);

        BlackScholesMerton option = new BlackScholesMerton(contract, frame, 0.2);

        assertAll(
                () -> assertEquals(0.0, option.price(), TOLERANCE),
                () -> assertEquals(0.0, option.delta(), TOLERANCE),
                () -> assertExpiredGreeksAreZero(option)
        );
    }

    @Test
    void itmPutAtExpiry() {
        ZonedDateTime expiry = ZonedDateTime.of(2026, 1, 16, 16, 0, 0, 0, ZoneOffset.UTC);
        OptionContract contract = contract(OptionType.PUT, expiry);
        EquityFrame frame = new EquityFrame(expiry, 90.0, 0.05, 0.0);

        BlackScholesMerton option = new BlackScholesMerton(contract, frame, 0.2);

        assertAll(
                () -> assertEquals(10.0, option.price(), TOLERANCE),
                () -> assertEquals(-1.0, option.delta(), TOLERANCE),
                () -> assertExpiredGreeksAreZero(option)
        );
    }

    @Test
    void atmAtExpiry() {
        ZonedDateTime expiry = ZonedDateTime.of(2026, 1, 16, 16, 0, 0, 0, ZoneOffset.UTC);
        EquityFrame frame = new EquityFrame(expiry, 100.0, 0.05, 0.0);

        BlackScholesMerton call = new BlackScholesMerton(contract(OptionType.CALL, expiry), frame, 0.2);
        BlackScholesMerton put = new BlackScholesMerton(contract(OptionType.PUT, expiry), frame, 0.2);

        assertAll(
                () -> assertEquals(0.0, call.price(), TOLERANCE),
                () -> assertEquals(0.5, call.delta(), TOLERANCE),
                () -> assertEquals(0.0, put.price(), TOLERANCE),
                () -> assertEquals(-0.5, put.delta(), TOLERANCE)
        );
    }

    private static void assertExpiredGreeksAreZero(BlackScholesMerton option) {
        assertAll(
                () -> assertEquals(0.0, option.gamma(), TOLERANCE),
                () -> assertEquals(0.0, option.vega(), TOLERANCE),
                () -> assertEquals(0.0, option.theta(), TOLERANCE),
                () -> assertEquals(0.0, option.rho(), TOLERANCE),
                () -> assertEquals(0.0, option.epsilon(), TOLERANCE),
                () -> assertEquals(0.0, option.vanna(), TOLERANCE),
                () -> assertEquals(0.0, option.volga(), TOLERANCE),
                () -> assertEquals(0.0, option.charm(), TOLERANCE),
                () -> assertEquals(0.0, option.dualDelta(), TOLERANCE),
                () -> assertEquals(0.0, option.vera(), TOLERANCE),
                () -> assertEquals(0.0, option.speed(), TOLERANCE),
                () -> assertEquals(0.0, option.zomma(), TOLERANCE),
                () -> assertEquals(0.0, option.color(), TOLERANCE),
                () -> assertEquals(0.0, option.ultima(), TOLERANCE),
                () -> assertEquals(0.0, option.parmicharma(), TOLERANCE),
                () -> assertEquals(0.0, option.dualGamma(), TOLERANCE)
        );
    }

    private static OptionContract contract(OptionType type, ZonedDateTime expiry) {
        return new OptionContract(
                "TEST",
                type,
                Option.EUROPEAN,
                100.0,
                expiry,
                100
        );
    }
}
