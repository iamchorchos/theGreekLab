package european;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.FuturesFrame;
import com.thegreeklab.finance.model.european.Black76;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static com.thegreeklab.finance.time.DayCountConvention.ACT_365F;

class Black76Test {

    private static final double TOLERANCE = 1e-12;

    @Test
    void rhoUsesDiscountOnly() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        double tYears = 1.25;
        long nanosToExpiry = (long) (tYears * 365.0 * 86_400.0 * 1_000_000_000L);
        ZonedDateTime expiry = now.plusNanos(nanosToExpiry);
        OptionContract callContract = new OptionContract(
                "FUT",
                OptionType.CALL,
                Option.EUROPEAN,
                95.0,
                expiry,
                1
        );
        OptionContract putContract = new OptionContract(
                "FUT",
                OptionType.PUT,
                Option.EUROPEAN,
                95.0,
                expiry,
                1
        );

        FuturesFrame frame = new FuturesFrame(now, 100.0, 0.04);
        Black76 call = new Black76(callContract, frame, 0.25, ACT_365F);
        Black76 put = new Black76(putContract, frame, 0.25, ACT_365F);

        assertAll(
                () -> assertEquals(-call.timeToExpiry() * call.price(), call.rho(), TOLERANCE),
                () -> assertEquals(-put.timeToExpiry() * put.price(), put.rho(), TOLERANCE)
        );
    }

    @Test
    void epsilonUndefined() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = new OptionContract(
                "FUT",
                OptionType.CALL,
                Option.EUROPEAN,
                95.0,
                now.plusYears(1),
                1
        );
        FuturesFrame frame = new FuturesFrame(now, 100.0, 0.04);
        Black76 option = new Black76(contract, frame, 0.25, ACT_365F);

        assertThrows(UnsupportedOperationException.class, option::epsilon);
    }
}
