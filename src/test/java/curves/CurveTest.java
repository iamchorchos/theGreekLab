package curves;

import com.thegreeklab.finance.curves.EquityForwardCurve;
import com.thegreeklab.finance.curves.DividendYieldCurve;
import com.thegreeklab.finance.curves.FlatDiscountCurve;
import com.thegreeklab.finance.curves.FundingCurve;
import com.thegreeklab.finance.time.DayCountConvention;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurveTest {

    private static final long VALUATION_NANOS = 1_000_000_000_000_000_000L;
    private static final long ONE_YEAR_NANOS = 365L * 24L * 60L * 60L * 1_000_000_000L;
    private static final long ONE_YEAR_AFTER_VALUATION = VALUATION_NANOS + ONE_YEAR_NANOS;
    private static final double FUNDING_RATE = 0.05;
    private static final double DIVIDEND_YIELD = 0.02;

    @Test
    void flatDiscountCurveMatchesContinuousDiscounting() {
        FlatDiscountCurve curve = flatCurve(0.05);

        assertAll(
                () -> assertEquals(1.0, curve.discountFactor(VALUATION_NANOS), 0.0),
                () -> assertEquals(
                        Math.exp(-0.05),
                        curve.discountFactor(ONE_YEAR_AFTER_VALUATION),
                        1e-15
                ),
                () -> assertEquals(
                        0.05,
                        curve.zeroRate(ONE_YEAR_AFTER_VALUATION, DayCountConvention.ACT_365F),
                        1e-15
                )
        );
    }

    @Test
    void flatDiscountCurveSupportsZeroAndNegativeRates() {
        FlatDiscountCurve zeroRateCurve = flatCurve(0.0);
        FlatDiscountCurve negativeRateCurve = flatCurve(-0.01);

        assertAll(
                () -> assertEquals(1.0, zeroRateCurve.discountFactor(ONE_YEAR_AFTER_VALUATION), 0.0),
                () -> assertEquals(
                        Math.exp(0.01),
                        negativeRateCurve.discountFactor(ONE_YEAR_AFTER_VALUATION),
                        1e-15
                )
        );
    }

    @Test
    void flatDiscountCurveRejectsInvalidInputs() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new FlatDiscountCurve(VALUATION_NANOS, Double.NaN, DayCountConvention.ACT_365F)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new FlatDiscountCurve(VALUATION_NANOS, Double.POSITIVE_INFINITY, DayCountConvention.ACT_365F)),
                () -> assertThrows(NullPointerException.class,
                        () -> new FlatDiscountCurve(VALUATION_NANOS, 0.05, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> flatCurve(0.05).discountFactor(VALUATION_NANOS - 1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> flatCurve(0.05).zeroRate(VALUATION_NANOS, DayCountConvention.ACT_365F))
        );
    }

    @Test
    void equityForwardCurveMatchesContinuousCarryFormula() {
        EquityForwardCurve curve = new EquityForwardCurve(
                VALUATION_NANOS,
                100.0,
                fundingCurve(),
                dividendYieldCurve()
        );

        assertAll(
                () -> assertEquals(100.0, curve.forwardPrice(VALUATION_NANOS), 1e-15),
                () -> assertEquals(
                        100.0 * Math.exp(0.03),
                        curve.forwardPrice(ONE_YEAR_AFTER_VALUATION),
                        1e-13
                )
        );
    }

    @Test
    void equityForwardCurveRejectsInvalidInputsAndMismatchedTimestamps() {
        FundingCurve matchingFundingCurve = fundingCurve();
        DividendYieldCurve matchingDividendYieldCurve = dividendYieldCurve();
        FlatDiscountCurve laterCurve = new FlatDiscountCurve(
                ONE_YEAR_AFTER_VALUATION,
                DIVIDEND_YIELD,
                DayCountConvention.ACT_365F
        );

        FundingCurve laterFundingCurve = new FundingCurve(laterCurve);
        DividendYieldCurve laterDividendYieldCurve = new DividendYieldCurve(laterCurve);

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new EquityForwardCurve(VALUATION_NANOS, 0.0, matchingFundingCurve, matchingDividendYieldCurve)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new EquityForwardCurve(VALUATION_NANOS, Double.NaN, matchingFundingCurve, matchingDividendYieldCurve)),
                () -> assertThrows(NullPointerException.class,
                        () -> new EquityForwardCurve(VALUATION_NANOS, 100.0, null, matchingDividendYieldCurve)),
                () -> assertThrows(NullPointerException.class,
                        () -> new EquityForwardCurve(VALUATION_NANOS, 100.0, matchingFundingCurve, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new EquityForwardCurve(VALUATION_NANOS, 100.0, laterFundingCurve, matchingDividendYieldCurve)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new EquityForwardCurve(VALUATION_NANOS, 100.0, matchingFundingCurve, laterDividendYieldCurve)),
                () -> assertThrows(NullPointerException.class, () -> new FundingCurve(null)),
                () -> assertThrows(NullPointerException.class, () -> new DividendYieldCurve(null))
        );
    }

    @Test
    void equityForwardCurveRejectsDeliveryBeforeValuation() {
        EquityForwardCurve curve = new EquityForwardCurve(
                VALUATION_NANOS,
                100.0,
                fundingCurve(),
                dividendYieldCurve()
        );

        assertThrows(IllegalArgumentException.class,
                () -> curve.forwardPrice(VALUATION_NANOS - 1));
    }

    private static FlatDiscountCurve flatCurve(double rate) {
        return new FlatDiscountCurve(
                VALUATION_NANOS,
                rate,
                DayCountConvention.ACT_365F
        );
    }

    private static FundingCurve fundingCurve() {
        return new FundingCurve(flatCurve(FUNDING_RATE));
    }

    private static DividendYieldCurve dividendYieldCurve() {
        return new DividendYieldCurve(flatCurve(DIVIDEND_YIELD));
    }
}
