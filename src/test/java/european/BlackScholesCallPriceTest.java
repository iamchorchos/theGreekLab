package european;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.frame.FXFrame;
import com.thegreeklab.finance.frame.FuturesFrame;
import com.thegreeklab.finance.frame.MarketData;
import com.thegreeklab.finance.model.european.Black76;
import com.thegreeklab.finance.model.european.BlackScholes;
import com.thegreeklab.finance.model.european.BlackScholesMerton;
import com.thegreeklab.finance.model.european.GarmanKohlhagen;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BlackScholesCallPriceTest {

    private static final double TOLERANCE = 1.0e-12;
    private static final double SPOT = 100.0;
    private static final double STRIKE = 105.0;
    private static final double VOLATILITY = 0.2;
    private static final ZonedDateTime NOW = ZonedDateTime.of(
            2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
    );
    private static final ZonedDateTime EXPIRY = NOW.plusYears(1);

    @Test
    void matchesSpecializedModels() {
        EquityFrame equity = new EquityFrame(NOW, SPOT, 0.05, 0.02);
        FuturesFrame futures = new FuturesFrame(NOW, SPOT, 0.05);
        FXFrame fx = new FXFrame(NOW, SPOT, 0.05, 0.02);
        OptionContract call = contract(OptionType.CALL);

        assertAll(
                () -> assertEquals(
                        new BlackScholesMerton(call, equity, VOLATILITY).price(),
                        directCallPrice(call, equity),
                        TOLERANCE
                ),
                () -> assertEquals(
                        new Black76(call, futures, VOLATILITY).price(),
                        directCallPrice(call, futures),
                        TOLERANCE
                ),
                () -> assertEquals(
                        new GarmanKohlhagen(call, fx, VOLATILITY).price(),
                        directCallPrice(call, fx),
                        TOLERANCE
                )
        );
    }

    @Test
    void supportsPutCallTransform() {
        EquityFrame frame = new EquityFrame(NOW, SPOT, 0.05, 0.02);
        OptionContract put = contract(OptionType.PUT);
        double timeToExpiry = put.getTimeToExpiry(frame.timestampNanos());
        double riskFreeRate = frame.riskFreeRate();
        double costOfCarry = frame.costOfCarry();

        double transformedCall = BlackScholes.callPrice(
                STRIKE,
                SPOT,
                timeToExpiry,
                riskFreeRate - costOfCarry,
                -costOfCarry,
                VOLATILITY
        );

        assertEquals(
                new BlackScholesMerton(put, frame, VOLATILITY).price(),
                transformedCall,
                TOLERANCE
        );
    }

    @Test
    void snapshotMatchesIndividualGreeks() {
        EquityFrame frame = new EquityFrame(NOW, SPOT, 0.05, 0.02);
        BlackScholesMerton model = new BlackScholesMerton(
                contract(OptionType.CALL),
                frame,
                VOLATILITY
        );
        var values = model.greeks();

        assertAll(
                () -> assertEquals(model.price(), values.price(), TOLERANCE),
                () -> assertEquals(model.delta(), values.delta(), TOLERANCE),
                () -> assertEquals(model.gamma(), values.gamma(), TOLERANCE),
                () -> assertEquals(model.vega(), values.vega(), TOLERANCE),
                () -> assertEquals(model.theta(), values.theta(), TOLERANCE),
                () -> assertEquals(model.rho(), values.rho(), TOLERANCE)
        );
    }

    private static double directCallPrice(OptionContract contract, MarketData frame) {
        return BlackScholes.callPrice(
                frame.spotPrice(),
                contract.strikePrice(),
                contract.getTimeToExpiry(frame.timestampNanos()),
                frame.riskFreeRate(),
                frame.costOfCarry(),
                VOLATILITY
        );
    }

    private static OptionContract contract(OptionType type) {
        return new OptionContract("TEST", type, Option.EUROPEAN, STRIKE, EXPIRY, 100);
    }
}
