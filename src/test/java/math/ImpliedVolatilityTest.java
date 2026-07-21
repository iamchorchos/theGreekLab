package math;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.exception.InvalidModelDomainException;
import com.thegreeklab.finance.exception.NonPositivePriceException;
import com.thegreeklab.finance.exception.UnsupportedExerciseStyleException;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.frame.FuturesFrame;
import com.thegreeklab.finance.frame.FXFrame;
import com.thegreeklab.finance.frame.MarketData;
import com.thegreeklab.finance.model.american.binomial.CoxRossRubenstein;
import com.thegreeklab.finance.time.EpochNanos;
import com.thegreeklab.finance.model.european.BlackScholes;
import com.thegreeklab.math.volatility.ImpliedVolatilityResult;
import com.thegreeklab.math.volatility.VolatilityCalculator;
import com.thegreeklab.math.volatility.VolatilityPricer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.OptionalDouble;

import static com.thegreeklab.finance.time.DayCountConvention.ACT_365F;
import static com.thegreeklab.finance.time.DayCountConvention.ACT_360;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImpliedVolatilityTest {
    private static final double TOLERANCE = 1e-8;

    @Test
    void solvesModelIndependentVolatilityPricer() {
        double marketPrice = 9.0;
        VolatilityPricer model = volatility -> 100.0 * volatility * volatility;

        OptionalDouble result = VolatilityCalculator.impliedVolatility(
                model, marketPrice, 0.15
        );

        assertTrue(result.isPresent());
        assertEquals(0.30, result.getAsDouble(), TOLERANCE);
    }

    @Test
    void returnsConvergenceDiagnostics() {
        double marketPrice = 9.0;
        VolatilityPricer model = volatility -> 100.0 * volatility * volatility;

        ImpliedVolatilityResult result = VolatilityCalculator.solveImpliedVolatility(
                model, marketPrice, 0.15
        );

        assertTrue(result.converged());
        assertEquals(ImpliedVolatilityResult.Status.CONVERGED, result.status());
        assertEquals(0.30, result.volatility(), TOLERANCE);
        assertEquals(marketPrice, result.modelPrice(), 1e-10);
        assertEquals(0.0, result.priceError(), 1e-10);
        assertTrue(result.iterations() > 0);
        assertTrue(result.asOptionalDouble().isPresent());
    }

    @Test
    void reportsInvalidInitialVolatility() {
        ImpliedVolatilityResult result = VolatilityCalculator.solveImpliedVolatility(
                volatility -> volatility,
                1.0,
                Double.NaN
        );

        assertEquals(
                ImpliedVolatilityResult.Status.INVALID_INITIAL_VOLATILITY,
                result.status()
        );
        assertFalse(result.converged());
        assertTrue(result.asOptionalDouble().isEmpty());
    }

    @Test
    void reportsUnbracketedRootWithLastValidDiagnostics() {
        ImpliedVolatilityResult result = VolatilityCalculator.solveImpliedVolatility(
                volatility -> volatility,
                1_000.0,
                0.20
        );

        assertEquals(
                ImpliedVolatilityResult.Status.ROOT_NOT_BRACKETED,
                result.status()
        );
        assertEquals(10.0, result.volatility(), 0.0);
        assertEquals(10.0, result.modelPrice(), 0.0);
        assertEquals(-990.0, result.priceError(), 0.0);
        assertTrue(result.iterations() > 0);
    }

    @Test
    void reportsInvalidModelDomain() {
        VolatilityPricer model = volatility -> {
            throw new InvalidModelDomainException(
                    "Unsupported trial volatility: " + volatility
            );
        };

        ImpliedVolatilityResult result = VolatilityCalculator.solveImpliedVolatility(
                model, 10.0, 0.20
        );

        assertEquals(
                ImpliedVolatilityResult.Status.INVALID_MODEL_DOMAIN,
                result.status()
        );
        assertEquals(0.20, result.volatility(), 0.0);
        assertTrue(Double.isNaN(result.modelPrice()));
        assertTrue(Double.isNaN(result.priceError()));
    }

    @Test
    void reportsEuropeanPriceOutsideBounds() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = new OptionContract(
                "TEST", OptionType.CALL, Option.EUROPEAN, 100.0,
                now.plusYears(1), 100
        );
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        ImpliedVolatilityResult result = VolatilityCalculator.solveImpliedVolatility(
                contract, frame, 100.0, ACT_365F
        );

        assertEquals(
                ImpliedVolatilityResult.Status.PRICE_OUTSIDE_BOUNDS,
                result.status()
        );
        assertTrue(result.asOptionalDouble().isEmpty());
    }

    @Test
    void recoversAmericanCrrVolatility() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = new OptionContract(
                "TEST", OptionType.PUT, Option.AMERICAN, 100.0,
                now.plusYears(1), 100
        );
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.02);
        double expectedVolatility = 0.37;
        double marketPrice = new CoxRossRubenstein(
                contract, frame, expectedVolatility, 301, ACT_365F
        ).price();
        VolatilityPricer calibrationModel = new CoxRossRubenstein(
                contract, frame, 0.15, 301, ACT_365F
        );

        OptionalDouble result = VolatilityCalculator.impliedVolatility(
                calibrationModel, marketPrice, 0.15
        );

        assertTrue(result.isPresent());
        assertEquals(expectedVolatility, result.getAsDouble(), 1e-7);
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/iv_crossval.csv", numLinesToSkip = 1)
    void recoversVolatility(String assetClass, String flag, double spot, double strike, double t,
                  double rateOrDomestic, double divOrForeign, double marketPrice, double expectedIv) {

        long nowNanos = System.currentTimeMillis() * 1_000_000L;
        double secondsInYear = 365.0 * 86400.0;
        long expirationNanos = nowNanos + (long) (t * secondsInYear * 1_000_000_000.0);

        ZonedDateTime expirationDate = EpochNanos.toUtc(expirationNanos);

        OptionType type = "c".equals(flag) ? OptionType.CALL : OptionType.PUT;

        OptionContract contract = new OptionContract(
                "TEST", type, Option.EUROPEAN, strike,
                expirationDate, 1
        );

        MarketData frame = switch (assetClass) {
            case "EQUITY" -> new EquityFrame(nowNanos, spot, rateOrDomestic, divOrForeign);
            case "FUTURES" -> new FuturesFrame(nowNanos, spot, rateOrDomestic);
            case "FX" -> new FXFrame(nowNanos, spot, rateOrDomestic, divOrForeign);
            default -> throw new IllegalArgumentException("Unknown asset class: " + assetClass);
        };

        double actualT = ACT_365F.timeToExpiry(frame.timestampNanos(), contract.expirationDate());
        assertEquals(t, actualT, 1e-9, "Time-to-expiry construction drifted from nominal t");

        OptionalDouble result = VolatilityCalculator.impliedVolatility(
                contract, frame, marketPrice, ACT_365F
        );

        assertTrue(result.isPresent(), () -> "Solver returned empty for a known-solvable price (" + assetClass + ")");
        assertEquals(expectedIv, result.getAsDouble(), TOLERANCE,
                () -> "IV mismatch for " + assetClass + ", t=" + t + ", K/S=" + (strike / spot));
    }

    @Test
    void rejectsAmerican() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = new OptionContract(
                "TEST",
                OptionType.CALL,
                Option.AMERICAN,
                100.0,
                now.plusYears(1),
                100
        );
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        assertThrows(
                UnsupportedExerciseStyleException.class,
                () -> VolatilityCalculator.impliedVolatility(contract, frame, 10.0, ACT_365F)
        );
    }

    @Test
    void honorsAct360() {
        ZonedDateTime now = ZonedDateTime.of(
                2026, 1, 2, 12, 0, 0, 0, ZoneOffset.UTC
        );
        OptionContract contract = new OptionContract(
                "TEST",
                OptionType.CALL,
                Option.EUROPEAN,
                100.0,
                now.plusDays(360),
                100
        );
        EquityFrame frame = new EquityFrame(now, 100.0, 0.03, 0.01);
        double expectedVolatility = 0.25;
        double marketPrice = BlackScholes.price(
                contract, frame, expectedVolatility, ACT_360
        );

        OptionalDouble result = VolatilityCalculator.impliedVolatility(
                contract, frame, marketPrice, ACT_360
        );

        assertTrue(result.isPresent());
        assertEquals(expectedVolatility, result.getAsDouble(), TOLERANCE);
    }

    @Test
    void rejectsNonFinitePrice() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        OptionContract contract = new OptionContract(
                "TEST",
                OptionType.CALL,
                Option.EUROPEAN,
                100.0,
                now.plusYears(1),
                100
        );
        EquityFrame frame = new EquityFrame(now, 100.0, 0.05, 0.0);

        assertThrows(
                NonPositivePriceException.class,
                () -> VolatilityCalculator.impliedVolatility(
                        contract, frame, Double.NaN, ACT_365F
                )
        );
    }
}
