package european.forward;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.curves.DiscountFactorNode;
import com.thegreeklab.finance.curves.DividendYieldCurve;
import com.thegreeklab.finance.curves.EquityForwardCurve;
import com.thegreeklab.finance.curves.FlatDiscountCurve;
import com.thegreeklab.finance.curves.ForwardPriceNode;
import com.thegreeklab.finance.curves.FundingCurve;
import com.thegreeklab.finance.curves.InterpolatedDiscountCurve;
import com.thegreeklab.finance.curves.InterpolatedForwardCurve;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.european.BlackScholesMerton;
import com.thegreeklab.finance.model.european.ForwardBlack76;
import com.thegreeklab.finance.time.DayCountConvention;
import com.thegreeklab.finance.time.EpochNanos;
import com.thegreeklab.finance.volatility.FlatVolatilitySurface;
import com.thegreeklab.finance.volatility.VolatilitySurface;
import com.thegreeklab.math.ERF;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ForwardBlack76Test {

    private static final DayCountConvention DAY_COUNT = DayCountConvention.ACT_365F;
    private static final ZonedDateTime VALUATION = ZonedDateTime.of(
            2026, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC
    );
    private static final ZonedDateTime EXPIRY = VALUATION.plusYears(1);
    private static final long VALUATION_NANOS = EpochNanos.from(VALUATION);

    @Test
    void matchesBlackScholesMertonForFlatEquityCurves() {
        assertAll(
                () -> assertEquals(
                        blackScholesMertonPrice(OptionType.CALL),
                        forwardBlackPrice(OptionType.CALL),
                        1e-12
                ),
                () -> assertEquals(
                        blackScholesMertonPrice(OptionType.PUT),
                        forwardBlackPrice(OptionType.PUT),
                        1e-12
                )
        );
    }

    @Test
    void flatVolatilitySurfaceMatchesTheScalarVolatilityConstructor() {
        FlatDiscountCurve rawFundingCurve = flatCurve(VALUATION_NANOS, 0.05);
        EquityForwardCurve forwardCurve = new EquityForwardCurve(
                VALUATION_NANOS,
                100.0,
                new FundingCurve(rawFundingCurve),
                new DividendYieldCurve(flatCurve(VALUATION_NANOS, 0.02))
        );
        OptionContract contract = contract(OptionType.CALL);

        double scalarPrice = new ForwardBlack76(
                contract,
                forwardCurve,
                0.20,
                DAY_COUNT
        ).price();
        double surfacePrice = new ForwardBlack76(
                contract,
                forwardCurve,
                new FlatVolatilitySurface(VALUATION_NANOS, 0.20),
                DAY_COUNT
        ).price();

        assertEquals(scalarPrice, surfacePrice, 0.0);
    }

    @Test
    void rejectsAVolatilitySurfaceWithADifferentValuationTimestamp() {
        FlatDiscountCurve rawFundingCurve = flatCurve(VALUATION_NANOS, 0.05);
        EquityForwardCurve forwardCurve = new EquityForwardCurve(
                VALUATION_NANOS,
                100.0,
                new FundingCurve(rawFundingCurve),
                new DividendYieldCurve(flatCurve(VALUATION_NANOS, 0.02))
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ForwardBlack76(
                        contract(OptionType.CALL),
                        forwardCurve,
                        new FlatVolatilitySurface(VALUATION_NANOS + 1, 0.20),
                        DAY_COUNT
                )
        );
    }

    @Test
    void queriesVolatilitySurfaceUsingExpiryAndLogStrikeToForward() {
        FlatDiscountCurve rawFundingCurve = flatCurve(VALUATION_NANOS, 0.05);
        EquityForwardCurve forwardCurve = new EquityForwardCurve(
                VALUATION_NANOS,
                100.0,
                new FundingCurve(rawFundingCurve),
                new DividendYieldCurve(flatCurve(VALUATION_NANOS, 0.02))
        );
        RecordingVolatilitySurface surface = new RecordingVolatilitySurface(
                VALUATION_NANOS,
                0.20
        );
        OptionContract contract = contract(OptionType.CALL);

        new ForwardBlack76(contract, forwardCurve, surface, DAY_COUNT).price();

        long expiryNanos = EpochNanos.from(EXPIRY);
        assertAll(
                () -> assertEquals(expiryNanos, surface.requestedExpiryNanos),
                () -> assertEquals(
                        Math.log(contract.strikePrice() / forwardCurve.forwardPrice(expiryNanos)),
                        surface.requestedLogStrikeToForward,
                        1e-15
                )
        );
    }

    @Test
    void returnsIntrinsicValueAtExpiry() {
        assertAll(
                () -> assertEquals(
                        5.0,
                        ForwardBlack76.price(OptionType.CALL, 105.0, 100.0, 1.0, 0.20, 0.0),
                        0.0
                ),
                () -> assertEquals(
                        5.0,
                        ForwardBlack76.price(OptionType.PUT, 95.0, 100.0, 1.0, 0.20, 0.0),
                        0.0
                )
        );
    }

    @Test
    void doesNotQueryVolatilitySurfaceAtExpiry() {
        OptionContract expiredContract = new OptionContract(
                "TEST",
                OptionType.CALL,
                Option.EUROPEAN,
                100.0,
                VALUATION,
                100
        );
        InterpolatedForwardCurve forwardCurve = new InterpolatedForwardCurve(
                VALUATION_NANOS,
                List.of(new ForwardPriceNode(VALUATION_NANOS, 105.0))
        );
        VolatilitySurface surfaceRejectingValuationQueries = new VolatilitySurface() {
            @Override
            public long valuationTimestampNanos() {
                return VALUATION_NANOS;
            }

            @Override
            public double impliedVolatility(long expiryTimestampNanos, double logStrikeToForward) {
                throw new AssertionError("An expired option must not query volatility.");
            }
        };

        assertEquals(
                5.0,
                new ForwardBlack76(
                        expiredContract,
                        forwardCurve,
                        new FundingCurve(flatCurve(VALUATION_NANOS, 0.05)),
                        surfaceRejectingValuationQueries,
                        DAY_COUNT
                ).price(),
                0.0
        );
    }

    @Test
    void rejectsAInconsistentDiscountFactorAtExpiry() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ForwardBlack76.price(
                        OptionType.CALL,
                        105.0,
                        100.0,
                        0.85,
                        0.20,
                        0.0
                )
        );
    }

    @Test
    void derivesTimeToExpiryFromTheSameEpochTimestampUsedByCurves() {
        long expiryNanos = EpochNanos.from(EXPIRY);

        assertEquals(
                DAY_COUNT.yearFraction(VALUATION_NANOS, expiryNanos),
                DAY_COUNT.timeToExpiry(VALUATION_NANOS, EXPIRY),
                0.0
        );
    }

    @Test
    void pricesEndToEndFromInterpolatedFundingAndDividendCurves() {
        long oneYear = EpochNanos.from(VALUATION.plusYears(1));
        long twoYears = EpochNanos.from(VALUATION.plusYears(2));
        long expiryNanos = oneYear + (twoYears - oneYear) / 2;
        ZonedDateTime expiry = EpochNanos.toUtc(expiryNanos);

        InterpolatedDiscountCurve fundingCurve = new InterpolatedDiscountCurve(
                VALUATION_NANOS,
                List.of(
                        new DiscountFactorNode(oneYear, 0.95),
                        new DiscountFactorNode(twoYears, 0.90)
                )
        );
        InterpolatedDiscountCurve dividendCurve = new InterpolatedDiscountCurve(
                VALUATION_NANOS,
                List.of(
                        new DiscountFactorNode(oneYear, 0.98),
                        new DiscountFactorNode(twoYears, 0.96)
                )
        );
        EquityForwardCurve forwardCurve = new EquityForwardCurve(
                VALUATION_NANOS,
                100.0,
                new FundingCurve(fundingCurve),
                new DividendYieldCurve(dividendCurve)
        );
        OptionContract contract = new OptionContract(
                "TEST",
                OptionType.CALL,
                Option.EUROPEAN,
                100.0,
                expiry,
                100
        );
        double volatility = 0.20;

        double price = new ForwardBlack76(
                contract,
                forwardCurve,
                volatility,
                DAY_COUNT
        ).price();

        double discountFactor = Math.sqrt(0.95 * 0.90);
        double dividendDiscountFactor = Math.sqrt(0.98 * 0.96);
        double forward = 100.0 * dividendDiscountFactor / discountFactor;
        double timeToExpiry = DAY_COUNT.yearFraction(VALUATION_NANOS, expiryNanos);
        double volSqrtTime = volatility * Math.sqrt(timeToExpiry);
        double d1 = (Math.log(forward / contract.strikePrice())
                + 0.5 * volatility * volatility * timeToExpiry) / volSqrtTime;
        double d2 = d1 - volSqrtTime;
        double expected = discountFactor * (forward * ERF.cdf(d1)
                - contract.strikePrice() * ERF.cdf(d2));

        assertEquals(expected, price, 1e-12);
    }

    @Test
    void pricesFromDirectInterpolatedForwardQuotes() {
        long oneYear = EpochNanos.from(VALUATION.plusYears(1));
        long twoYears = EpochNanos.from(VALUATION.plusYears(2));
        long expiryNanos = oneYear + (twoYears - oneYear) / 2;
        ZonedDateTime expiry = EpochNanos.toUtc(expiryNanos);
        InterpolatedForwardCurve forwardCurve = new InterpolatedForwardCurve(
                VALUATION_NANOS,
                List.of(
                        new ForwardPriceNode(VALUATION_NANOS, 100.0),
                        new ForwardPriceNode(oneYear, 102.0),
                        new ForwardPriceNode(twoYears, 104.0)
                )
        );
        InterpolatedDiscountCurve discountCurve = new InterpolatedDiscountCurve(
                VALUATION_NANOS,
                List.of(
                        new DiscountFactorNode(oneYear, 0.95),
                        new DiscountFactorNode(twoYears, 0.90)
                )
        );
        OptionContract contract = new OptionContract(
                "FUTURE",
                OptionType.CALL,
                Option.EUROPEAN,
                100.0,
                expiry,
                100
        );
        double volatility = 0.20;

        double price = new ForwardBlack76(
                contract,
                forwardCurve,
                new FundingCurve(discountCurve),
                volatility,
                DAY_COUNT
        ).price();
        double timeToExpiry = DAY_COUNT.yearFraction(VALUATION_NANOS, expiryNanos);
        double expected = ForwardBlack76.price(
                OptionType.CALL,
                Math.sqrt(102.0 * 104.0),
                100.0,
                Math.sqrt(0.95 * 0.90),
                volatility,
                timeToExpiry
        );

        assertEquals(expected, price, 1e-12);
    }

    @Test
    void rejectsDifferentFundingCurveForAnEquityForwardCurve() {
        FlatDiscountCurve rawFundingCurve = flatCurve(VALUATION_NANOS, 0.05);
        FundingCurve fundingCurve = new FundingCurve(rawFundingCurve);
        EquityForwardCurve forwardCurve = new EquityForwardCurve(
                VALUATION_NANOS,
                100.0,
                fundingCurve,
                new DividendYieldCurve(flatCurve(VALUATION_NANOS, 0.02))
        );
        FundingCurve differentFundingCurve = new FundingCurve(
                flatCurve(VALUATION_NANOS, 0.04)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ForwardBlack76(
                        contract(OptionType.CALL),
                        forwardCurve,
                        differentFundingCurve,
                        0.20,
                        DAY_COUNT
                )
        );
    }

    @Test
    void acceptsAnEquivalentButSeparatelyConstructedFundingCurve() {
        FundingCurve embeddedFundingCurve = new FundingCurve(
                flatCurve(VALUATION_NANOS, 0.05)
        );
        EquityForwardCurve forwardCurve = new EquityForwardCurve(
                VALUATION_NANOS,
                100.0,
                embeddedFundingCurve,
                new DividendYieldCurve(flatCurve(VALUATION_NANOS, 0.02))
        );
        FundingCurve equivalentFundingCurve = new FundingCurve(
                flatCurve(VALUATION_NANOS, 0.05)
        );

        assertDoesNotThrow(
                () -> new ForwardBlack76(
                        contract(OptionType.CALL),
                        forwardCurve,
                        equivalentFundingCurve,
                        0.20,
                        DAY_COUNT
                )
        );
    }

    @Test
    void rejectsMismatchedForwardAndFundingValuationTimestamps() {
        InterpolatedForwardCurve forwardCurve = new InterpolatedForwardCurve(
                VALUATION_NANOS,
                List.of(new ForwardPriceNode(VALUATION_NANOS, 100.0))
        );
        FundingCurve laterFundingCurve = new FundingCurve(
                flatCurve(VALUATION_NANOS + 1, 0.05)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ForwardBlack76(
                        contract(OptionType.CALL),
                        forwardCurve,
                        laterFundingCurve,
                        0.20,
                        DAY_COUNT
                )
        );
    }

    @Test
    void rejectsInvalidDirectFormulaInputs() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ForwardBlack76.price(OptionType.CALL, 0.0, 100.0, 0.95, 0.20, 1.0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ForwardBlack76.price(OptionType.CALL, 100.0, 100.0, 0.0, 0.20, 1.0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ForwardBlack76.price(OptionType.CALL, 100.0, 100.0, 0.95, 0.20, -1.0))
        );
    }

    private static double forwardBlackPrice(OptionType type) {
        FlatDiscountCurve rawFundingCurve = flatCurve(VALUATION_NANOS, 0.05);
        FundingCurve fundingCurve = new FundingCurve(rawFundingCurve);
        EquityForwardCurve forwardCurve = new EquityForwardCurve(
                VALUATION_NANOS,
                100.0,
                fundingCurve,
                new DividendYieldCurve(flatCurve(VALUATION_NANOS, 0.02))
        );

        return new ForwardBlack76(
                contract(type),
                forwardCurve,
                0.20,
                DAY_COUNT
        ).price();
    }

    private static double blackScholesMertonPrice(OptionType type) {
        return new BlackScholesMerton(
                contract(type),
                new EquityFrame(VALUATION, 100.0, 0.05, 0.02),
                0.20,
                DAY_COUNT
        ).price();
    }

    private static OptionContract contract(OptionType type) {
        return new OptionContract(
                "TEST",
                type,
                Option.EUROPEAN,
                100.0,
                EXPIRY,
                100
        );
    }

    private static FlatDiscountCurve flatCurve(long valuationNanos, double rate) {
        return new FlatDiscountCurve(valuationNanos, rate, DAY_COUNT);
    }

    private static final class RecordingVolatilitySurface implements VolatilitySurface {
        private final long valuationTimestampNanos;
        private final double volatility;
        private long requestedExpiryNanos = Long.MIN_VALUE;
        private double requestedLogStrikeToForward = Double.NaN;

        private RecordingVolatilitySurface(long valuationTimestampNanos, double volatility) {
            this.valuationTimestampNanos = valuationTimestampNanos;
            this.volatility = volatility;
        }

        @Override
        public long valuationTimestampNanos() {
            return valuationTimestampNanos;
        }

        @Override
        public double impliedVolatility(long expiryTimestampNanos, double logStrikeToForward) {
            requestedExpiryNanos = expiryTimestampNanos;
            requestedLogStrikeToForward = logStrikeToForward;
            return volatility;
        }
    }
}
