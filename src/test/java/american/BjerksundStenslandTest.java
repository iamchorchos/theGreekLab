package american;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.frame.FuturesFrame;
import com.thegreeklab.finance.model.american.approximations.BjerksundStensland;
import com.thegreeklab.finance.model.european.BlackScholes;
import com.thegreeklab.finance.model.european.BlackScholesMerton;
import com.thegreeklab.finance.time.EpochNanos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static com.thegreeklab.finance.time.DayCountConvention.ACT_365F;
import static com.thegreeklab.finance.time.DayCountConvention.ACT_360;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BjerksundStenslandTest {

    private static final double TOLERANCE = 1e-12;
    private static final double HAUG_TABLE_TOLERANCE = 1e-4;
    private static final double SECONDS_PER_YEAR = 365.0 * 86_400.0;

    @ParameterizedTest(name = "call: T={0}, sigma={1}, F={2}")
    @CsvSource({
            "0.1, 0.15,  90.0,  0.0205",
            "0.1, 0.15, 100.0,  1.8757",
            "0.1, 0.15, 110.0, 10.0000",
            "0.1, 0.25,  90.0,  0.3151",
            "0.1, 0.25, 100.0,  3.1256",
            "0.1, 0.25, 110.0, 10.3725",
            "0.1, 0.35,  90.0,  0.9479",
            "0.1, 0.35, 100.0,  4.3746",
            "0.1, 0.35, 110.0, 11.1578",
            "0.5, 0.15,  90.0,  0.8099",
            "0.5, 0.15, 100.0,  4.0628",
            "0.5, 0.15, 110.0, 10.7898",
            "0.5, 0.25,  90.0,  2.7180",
            "0.5, 0.25, 100.0,  6.7661",
            "0.5, 0.25, 110.0, 12.9814",
            "0.5, 0.35,  90.0,  4.9665",
            "0.5, 0.35, 100.0,  9.4608",
            "0.5, 0.35, 110.0, 15.5137"
    })
    void callsMatchHaug(
            double timeToExpiry,
            double volatility,
            double futuresPrice,
            double expectedPrice
    ) {
        assertEquals(
                expectedPrice,
                haugTablePrice(OptionType.CALL, timeToExpiry, volatility, futuresPrice),
                HAUG_TABLE_TOLERANCE
        );
    }

    @ParameterizedTest(name = "put: T={0}, sigma={1}, F={2}")
    @CsvSource({
            "0.1, 0.15,  90.0, 10.0000",
            "0.1, 0.15, 100.0,  1.8757",
            "0.1, 0.15, 110.0,  0.0408",
            "0.1, 0.25,  90.0, 10.2280",
            "0.1, 0.25, 100.0,  3.1256",
            "0.1, 0.25, 110.0,  0.4552",
            "0.1, 0.35,  90.0, 10.8663",
            "0.1, 0.35, 100.0,  4.3746",
            "0.1, 0.35, 110.0,  1.2383",
            "0.5, 0.15,  90.0, 10.5400",
            "0.5, 0.15, 100.0,  4.0628",
            "0.5, 0.15, 110.0,  1.0689",
            "0.5, 0.25,  90.0, 12.4097",
            "0.5, 0.25, 100.0,  6.7661",
            "0.5, 0.25, 110.0,  3.2932",
            "0.5, 0.35,  90.0, 14.6445",
            "0.5, 0.35, 100.0,  9.4608",
            "0.5, 0.35, 110.0,  5.8374"
    })
    void putsMatchHaug(
            double timeToExpiry,
            double volatility,
            double futuresPrice,
            double expectedPrice
    ) {
        assertEquals(
                expectedPrice,
                haugTablePrice(OptionType.PUT, timeToExpiry, volatility, futuresPrice),
                HAUG_TABLE_TOLERANCE
        );
    }

    @Test
    void rejectsNullInputs() {
        ZonedDateTime valuationTime = ZonedDateTime.of(2026, 1, 16, 16, 0, 0, 0, ZoneOffset.UTC);
        OptionContract contract = contract(OptionType.CALL, valuationTime.plusYears(1));
        EquityFrame frame = new EquityFrame(valuationTime, 100.0, 0.05, 0.02);

        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new BjerksundStensland(null, frame, 0.20, ACT_365F)),
                () -> assertThrows(NullPointerException.class, () -> new BjerksundStensland(contract, null, 0.20, ACT_365F)),
                () -> assertThrows(NullPointerException.class, () -> new BjerksundStensland(contract, frame, 0.20, null))
        );
    }

    @Test
    void expiryIntrinsic() {
        ZonedDateTime expiry = ZonedDateTime.of(2026, 1, 16, 16, 0, 0, 0, ZoneOffset.UTC);

        BjerksundStensland call = option(OptionType.CALL, expiry, 110.0);
        BjerksundStensland put = option(OptionType.PUT, expiry, 90.0);
        BjerksundStensland outOfTheMoneyCall = option(OptionType.CALL, expiry, 90.0);
        BjerksundStensland outOfTheMoneyPut = option(OptionType.PUT, expiry, 110.0);

        assertAll(
                () -> assertEquals(10.0, call.price(), TOLERANCE),
                () -> assertEquals(10.0, put.price(), TOLERANCE),
                () -> assertEquals(0.0, outOfTheMoneyCall.price(), TOLERANCE),
                () -> assertEquals(0.0, outOfTheMoneyPut.price(), TOLERANCE)
        );
    }

    @Test
    void nearCarryLimit() {
        ZonedDateTime valuationTime = ZonedDateTime.of(2026, 1, 16, 16, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime expiry = valuationTime.plusYears(1);
        double riskFreeRate = 0.05;
        double dividendYield = 5e-9;
        double volatility = 0.20;

        OptionContract contract = contract(OptionType.CALL, expiry);
        EquityFrame frame = new EquityFrame(valuationTime, 100.0, riskFreeRate, dividendYield);
        double americanPrice = new BjerksundStensland(
                contract, frame, volatility, ACT_365F
        ).price();
        double europeanPrice = BlackScholes.callPrice(
                frame.spotPrice(),
                contract.strikePrice(),
                ACT_365F.timeToExpiry(frame.timestampNanos(), contract.expirationDate()),
                frame.riskFreeRate(),
                frame.costOfCarry(),
                volatility
        );

        assertAll(
                () -> assertTrue(Double.isFinite(americanPrice)),
                () -> assertEquals(europeanPrice, americanPrice, 1e-7)
        );
    }

    @Test
    void negativeRateRespectsIntrinsicValue() {
        double spot = 150.0;
        double strike = 100.0;
        double americanPrice = equityPrice(
                OptionType.CALL,
                spot,
                strike,
                1.0,
                -0.10,
                0.0,
                0.01
        );

        assertEquals(spot - strike, americanPrice, TOLERANCE);
    }

    @Test
    void callBounds() {
        double spot = 52.19756690871083;
        double strike = 52.97486450614688;
        double timeToExpiry = 3.007201545343507;
        double riskFreeRate = 0.03683813432515369;
        double dividendYield = 0.132901399890947;
        double volatility = 0.07590344466953339;

        double americanPrice = equityPrice(
                OptionType.CALL,
                spot,
                strike,
                timeToExpiry,
                riskFreeRate,
                dividendYield,
                volatility
        );
        double europeanPrice = BlackScholes.callPrice(
                spot,
                strike,
                timeToExpiry,
                riskFreeRate,
                riskFreeRate - dividendYield,
                volatility
        );

        assertAll(
                () -> assertTrue(americanPrice >= 0.0),
                () -> assertTrue(americanPrice >= europeanPrice)
        );
    }

    @Test
    void putBound() {
        double spot = 174.3989522097575;
        double strike = 55.85385994590797;
        double timeToExpiry = 3.010849148096547;
        double riskFreeRate = 0.02854631713163641;
        double dividendYield = 0.08660764470622795;
        double volatility = 0.8703779425627893;

        double americanPrice = equityPrice(
                OptionType.PUT,
                spot,
                strike,
                timeToExpiry,
                riskFreeRate,
                dividendYield,
                volatility
        );
        double europeanPrice = BlackScholes.callPrice(
                strike,
                spot,
                timeToExpiry,
                dividendYield,
                dividendYield - riskFreeRate,
                volatility
        );

        assertTrue(americanPrice >= europeanPrice);
    }

    @Test
    void unstableBoundaryFallback() {
        double spot = 141.15688869810333;
        double strike = 137.12686515790796;
        double timeToExpiry = 2.412557113141491;
        double riskFreeRate = 0.14986994141885546;
        double dividendYield = 0.0028780144409920215;
        double volatility = 0.031166084131666563;

        double americanPrice = equityPrice(
                OptionType.CALL,
                spot,
                strike,
                timeToExpiry,
                riskFreeRate,
                dividendYield,
                volatility
        );
        double europeanPrice = BlackScholes.callPrice(
                spot,
                strike,
                timeToExpiry,
                riskFreeRate,
                riskFreeRate - dividendYield,
                volatility
        );
        double expectedLowerBound = Math.max(spot - strike, europeanPrice);

        assertAll(
                () -> assertTrue(Double.isFinite(americanPrice)),
                () -> assertEquals(expectedLowerBound, americanPrice, TOLERANCE)
        );
    }

    @Test
    void greeksMatchEuropeanLimit() {
        ZonedDateTime valuationTime = ZonedDateTime.of(2026, 1, 16, 16, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime expiry = valuationTime.plusYears(1);
        EquityFrame frame = new EquityFrame(valuationTime, 100.0, 0.05, 0.0);
        double volatility = 0.20;

        OptionContract americanContract = contract(OptionType.CALL, expiry);
        OptionContract europeanContract = new OptionContract(
                "TEST",
                OptionType.CALL,
                Option.EUROPEAN,
                100.0,
                expiry,
                100
        );

        BjerksundStensland american = new BjerksundStensland(
                americanContract, frame, volatility, ACT_365F
        );
        BlackScholesMerton european = new BlackScholesMerton(
                europeanContract, frame, volatility, ACT_365F
        );

        assertAll(
                () -> assertEquals(european.delta(), american.delta(), 1e-6),
                () -> assertEquals(european.gamma(), american.gamma(), 1e-6),
                () -> assertEquals(european.vega(), american.vega(), 1e-5),
                () -> assertEquals(european.theta(), american.theta(), 5e-2),
                () -> assertEquals(european.rho(), american.rho(), 1e-5)
        );
    }

    @Test
    void americanGreeksAreFinite() {
        ZonedDateTime valuationTime = ZonedDateTime.of(2026, 1, 16, 16, 0, 0, 0, ZoneOffset.UTC);
        OptionContract contract = contract(OptionType.CALL, valuationTime.plusYears(1));
        EquityFrame frame = new EquityFrame(valuationTime, 100.0, 0.05, 0.08);
        BjerksundStensland model = new BjerksundStensland(
                contract, frame, 0.20, ACT_360
        );

        assertAll(
                () -> assertTrue(Double.isFinite(model.delta())),
                () -> assertTrue(Double.isFinite(model.gamma())),
                () -> assertTrue(Double.isFinite(model.vega())),
                () -> assertTrue(Double.isFinite(model.theta())),
                () -> assertTrue(Double.isFinite(model.rho())),
                () -> assertEquals(ACT_360, model.withSpot(101.0).dayCountConvention())
        );
    }

    @Test
    void snapshotMatchesIndividualGreeks() {
        ZonedDateTime valuationTime = ZonedDateTime.of(2026, 1, 16, 16, 0, 0, 0, ZoneOffset.UTC);
        OptionContract contract = contract(OptionType.PUT, valuationTime.plusYears(1));
        EquityFrame frame = new EquityFrame(valuationTime, 95.0, 0.05, 0.03);
        BjerksundStensland model = new BjerksundStensland(
                contract, frame, 0.25, ACT_365F
        );
        var values = model.greeks();

        assertAll(
                () -> assertEquals(model.price(), values.price(), TOLERANCE),
                () -> assertEquals(model.delta(), values.delta(), TOLERANCE),
                () -> assertEquals(model.gamma(), values.gamma(), TOLERANCE),
                () -> assertEquals(model.vega(), values.vega(), TOLERANCE),
                () -> assertEquals(model.theta(), values.theta(), TOLERANCE),
                () -> assertEquals(model.rho(), values.rho(), TOLERANCE),
                () -> assertEquals(
                        model.withVolatility(0.30).price(),
                        model.priceAtVolatility(0.30),
                        TOLERANCE
                )
        );
    }

    private static BjerksundStensland option(
            OptionType type,
            ZonedDateTime expiry,
            double spot
    ) {
        return new BjerksundStensland(
                contract(type, expiry),
                new EquityFrame(expiry, spot, 0.05, 0.02),
                0.20,
                ACT_365F
        );
    }

    private static double haugTablePrice(
            OptionType type,
            double timeToExpiry,
            double volatility,
            double futuresPrice
    ) {
        ZonedDateTime valuationTime = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        long valuationNanos = EpochNanos.from(valuationTime);
        long expiryNanos = Math.addExact(
                valuationNanos,
                Math.round(timeToExpiry * SECONDS_PER_YEAR * 1_000_000_000.0)
        );
        ZonedDateTime expiry = EpochNanos.toUtc(expiryNanos);
        OptionContract contract = new OptionContract(
                "TEST",
                type,
                Option.AMERICAN,
                100.0,
                expiry,
                100
        );
        FuturesFrame frame = new FuturesFrame(valuationNanos, futuresPrice, 0.1);

        return new BjerksundStensland(contract, frame, volatility, ACT_365F).price();
    }

    private static double equityPrice(
            OptionType type,
            double spot,
            double strike,
            double timeToExpiry,
            double riskFreeRate,
            double dividendYield,
            double volatility
    ) {
        ZonedDateTime valuationTime = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        long valuationNanos = EpochNanos.from(valuationTime);
        long expiryNanos = Math.addExact(
                valuationNanos,
                Math.round(timeToExpiry * SECONDS_PER_YEAR * 1_000_000_000.0)
        );
        ZonedDateTime expiry = EpochNanos.toUtc(expiryNanos);
        OptionContract contract = new OptionContract(
                "TEST",
                type,
                Option.AMERICAN,
                strike,
                expiry,
                100
        );
        EquityFrame frame = new EquityFrame(valuationNanos, spot, riskFreeRate, dividendYield);

        return new BjerksundStensland(contract, frame, volatility, ACT_365F).price();
    }

    private static OptionContract contract(OptionType type, ZonedDateTime expiry) {
        return new OptionContract("TEST", type, Option.AMERICAN, 100.0, expiry, 100);
    }

}
