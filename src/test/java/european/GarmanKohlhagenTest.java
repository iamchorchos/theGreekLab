package european;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.FXFrame;
import com.thegreeklab.finance.model.european.GarmanKohlhagen;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.thegreeklab.finance.time.DayCountConvention.ACT_365F;

class GarmanKohlhagenTest {

    private static final double EXACT_TOLERANCE = 1e-12;
    private static final double BUMP_TOLERANCE = 1e-5;

    @Test
    void putCallParity() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        double tYears = 0.75;
        double spot = 1.085;
        double strike = 1.09;
        double domesticRate = 0.045;
        double foreignRate = 0.032;
        double volatility = 0.14;

        OptionContract callContract = contract("EURUSD-C", OptionType.CALL, strike, now, tYears);
        OptionContract putContract = contract("EURUSD-P", OptionType.PUT, strike, now, tYears);
        FXFrame frame = new FXFrame(now, spot, domesticRate, foreignRate);

        GarmanKohlhagen call = new GarmanKohlhagen(callContract, frame, volatility, ACT_365F);
        GarmanKohlhagen put = new GarmanKohlhagen(putContract, frame, volatility, ACT_365F);

        double expected = spot * Math.exp(-foreignRate * call.timeToExpiry())
                - strike * Math.exp(-domesticRate * call.timeToExpiry());

        assertEquals(expected, call.price() - put.price(), EXACT_TOLERANCE);
    }

    @Test
    void rateGreeksMatchBumps() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        double tYears = 1.5;
        double spot = 1.085;
        double strike = 1.09;
        double domesticRate = 0.045;
        double foreignRate = 0.032;
        double volatility = 0.14;
        double bump = 1e-5;

        OptionContract contract = contract("EURUSD", OptionType.CALL, strike, now, tYears);
        FXFrame frame = new FXFrame(now, spot, domesticRate, foreignRate);
        GarmanKohlhagen option = new GarmanKohlhagen(contract, frame, volatility, ACT_365F);

        double domesticBumped = (
                new GarmanKohlhagen(contract, new FXFrame(now, spot, domesticRate + bump, foreignRate), volatility, ACT_365F).price()
                        - new GarmanKohlhagen(contract, new FXFrame(now, spot, domesticRate - bump, foreignRate), volatility, ACT_365F).price()
        ) / (2.0 * bump);

        double foreignBumped = (
                new GarmanKohlhagen(contract, new FXFrame(now, spot, domesticRate, foreignRate + bump), volatility, ACT_365F).price()
                        - new GarmanKohlhagen(contract, new FXFrame(now, spot, domesticRate, foreignRate - bump), volatility, ACT_365F).price()
        ) / (2.0 * bump);

        assertAll(
                () -> assertEquals(domesticBumped, option.rho(), BUMP_TOLERANCE),
                () -> assertEquals(foreignBumped, option.epsilon(), BUMP_TOLERANCE)
        );
    }

    private static OptionContract contract(String symbol, OptionType type, double strike, ZonedDateTime now, double tYears) {
        long nanosToExpiry = (long) (tYears * 365.0 * 86_400.0 * 1_000_000_000L);
        ZonedDateTime expiry = now.plusNanos(nanosToExpiry);
        return new OptionContract(
                symbol,
                type,
                Option.EUROPEAN,
                strike,
                expiry,
                100_000
        );
    }
}
