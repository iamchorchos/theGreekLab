package com.thegreeklab.finance.model.european.discrete.adjustments;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.exception.NonPositivePriceException;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.european.discrete.DividendSchedule;
import com.thegreeklab.finance.time.DayCountConvention;

/**
 * Simple volatility adjustment for European equity options with deterministic
 * discrete cash dividends.
 *
 * <p>The approximation first computes the escrowed spot by subtracting the
 * present value of applicable dividends from the current spot and then scales
 * volatility according to
 * {@code adjustedVolatility = volatility * spot / adjustedSpot}. The returned
 * volatility is intended to be used together with the same escrowed spot in a
 * Black-Scholes-Merton valuation. {@link #price()} performs that valuation
 * using both adjusted inputs.</p>
 *
 * <p>Only dividends with ex-dividend timestamps strictly between the valuation
 * timestamp and option expiration are included. The method is inexpensive but
 * does not account explicitly for dividend timing in its volatility scaling.
 * It can therefore overstate volatility when a dividend is paid early in the
 * option's lifetime and should be treated as an approximation.</p>
 *
 * <p>This approach is commonly known as the simple or practitioner volatility
 * adjustment and is denoted {@code Vol1} by Haug, Haug and Lewis.</p>
 *
 * @see <a href="https://doi.org/10.1002/wilm.42820030514">
 * Haug, Haug and Lewis, "Back to Basics: A New Approach to the Discrete
 * Dividend Problem", Wilmott, 2003</a>
 */
public final class SimpleVolatilityAdjustment extends AbstractDiscreteDividendModel {

    /**
     * Creates a simple volatility adjustment.
     *
     * @param contract European option contract whose expiration limits the
     *                 applicable dividend schedule
     * @param frame equity market data containing valuation time, spot and risk-free rate;
     *              its continuous dividend yield must be zero
     * @param dividends schedule of deterministic discrete cash dividends
     * @param volatility annualized volatility of the underlying, expressed as a decimal
     * @param dayCountConvention convention used to derive dividend year fractions
     * @throws NullPointerException if {@code contract}, {@code frame},
     *                              {@code dividends}, or {@code dayCountConvention} is null
     * @throws com.thegreeklab.finance.exception.InvalidVolatilityException
     *         if {@code volatility} is invalid
     * @throws com.thegreeklab.finance.exception.UnsupportedExerciseStyleException
     *         if the contract is not European
     * @throws IllegalArgumentException if the frame contains a non-zero continuous
     *                                  dividend yield
     * @throws NonPositivePriceException if spot minus the present value of
     *                                   dividends is not positive and finite
     */
    public SimpleVolatilityAdjustment(
            OptionContract contract,
            EquityFrame frame,
            DividendSchedule dividends,
            double volatility,
            DayCountConvention dayCountConvention
    ) {
        super(contract, frame, dividends, volatility, dayCountConvention);
        requirePositiveAdjustedSpot(adjustedSpot());
    }

    /** {@inheritDoc} */
    @Override
    protected SimpleVolatilityAdjustment newModel(
            EquityFrame newFrame,
            double newVolatility
    ) {
        return new SimpleVolatilityAdjustment(
                contract(),
                newFrame,
                dividendSchedule(),
                newVolatility,
                dayCountConvention()
        );
    }

    /**
     * Calculates the annualized volatility adjusted for applicable discrete
     * cash dividends.
     *
     * <p>When there are no applicable dividends, the adjusted spot equals the
     * current spot and this method returns the original input volatility.</p>
     *
     * @return simple adjusted annualized volatility as a decimal
     */
    @Override
    public double adjustedVolatility() {
        return volatility() * frame().spotPrice() / adjustedSpot();
    }
}
