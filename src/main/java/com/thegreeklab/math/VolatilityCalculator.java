package com.thegreeklab.math;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.frame.MarketData;
import com.thegreeklab.finance.time.DayCountConvention;
import org.eclipse.collections.api.list.primitive.DoubleList;

import java.util.List;
import java.util.OptionalDouble;

/**
 * Compatibility facade for the volatility API published before version 2.2.0.
 *
 * @deprecated Use {@link com.thegreeklab.math.volatility.VolatilityCalculator}
 *             for new code.
 */
@Deprecated(since = "2.2.0", forRemoval = false)
public final class VolatilityCalculator {

    private VolatilityCalculator() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Delegates to the volatility package introduced in version 2.2.0.
     *
     * @param prices time-ordered, strictly positive prices
     * @param tradingDaysPerYear number of trading days used for annualization
     * @return annualized historical volatility
     * @deprecated Use the corresponding method in
     *             {@link com.thegreeklab.math.volatility.VolatilityCalculator}.
     */
    @Deprecated(since = "2.2.0", forRemoval = false)
    public static double historicalVolatility(DoubleList prices, int tradingDaysPerYear) {
        return com.thegreeklab.math.volatility.VolatilityCalculator.historicalVolatility(
                prices, tradingDaysPerYear
        );
    }

    /**
     * Compatibility representation of a high-low price bar.
     *
     * @param high high price
     * @param low low price
     * @deprecated Use
     *             {@link com.thegreeklab.math.volatility.VolatilityCalculator.PriceBar}.
     */
    @Deprecated(since = "2.2.0", forRemoval = false)
    public record PriceBar(double high, double low) {

        /** Validates the bar using the current volatility implementation. */
        public PriceBar {
            new com.thegreeklab.math.volatility.VolatilityCalculator.PriceBar(high, low);
        }

        private com.thegreeklab.math.volatility.VolatilityCalculator.PriceBar current() {
            return new com.thegreeklab.math.volatility.VolatilityCalculator.PriceBar(high, low);
        }
    }

    /**
     * Delegates to the volatility package introduced in version 2.2.0.
     *
     * @param ohlc high-low price bars
     * @param tradingDaysPerYear number of trading days used for annualization
     * @return annualized Parkinson volatility
     * @deprecated Use the corresponding method in
     *             {@link com.thegreeklab.math.volatility.VolatilityCalculator}.
     */
    @Deprecated(since = "2.2.0", forRemoval = false)
    public static double parkinsonsVolatility(List<PriceBar> ohlc, int tradingDaysPerYear) {
        List<com.thegreeklab.math.volatility.VolatilityCalculator.PriceBar> currentBars =
                ohlc == null ? null : ohlc.stream()
                        .map(bar -> bar == null ? null : bar.current())
                        .toList();
        return com.thegreeklab.math.volatility.VolatilityCalculator.parkinsonsVolatility(
                currentBars, tradingDaysPerYear
        );
    }

    /**
     * Delegates to the compatibility view of the universal implied-volatility
     * solver introduced in version 2.2.0.
     *
     * @param contract option contract
     * @param frame market data snapshot
     * @param marketPrice observed option price
     * @param dayCountConvention day-count convention
     * @return converged implied volatility, or empty on solver failure
     * @deprecated Use the corresponding method in
     *             {@link com.thegreeklab.math.volatility.VolatilityCalculator}.
     */
    @Deprecated(since = "2.2.0", forRemoval = false)
    public static OptionalDouble impliedVolatility(
            OptionContract contract,
            MarketData frame,
            double marketPrice,
            DayCountConvention dayCountConvention
    ) {
        return com.thegreeklab.math.volatility.VolatilityCalculator.impliedVolatility(
                contract, frame, marketPrice, dayCountConvention
        );
    }
}
