package com.thegreeklab.finance.model.european.discrete.adjustments;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.exception.NonPositivePriceException;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.european.discrete.DividendSchedule;
import com.thegreeklab.finance.time.DayCountConvention;
import net.jafama.FastMath;

/**
 * Haug-Haug volatility adjustment for European equity options with
 * deterministic discrete cash dividends.
 *
 * <p>The approximation divides the option lifetime into intervals ending at
 * successive ex-dividend times. Within each interval it scales volatility by
 * the current spot divided by spot minus the present value of all remaining
 * dividends. The resulting interval variances are time-weighted into one
 * annualized volatility, which {@link #price()} uses together with the
 * escrowed spot in a Black-Scholes-Merton valuation.</p>
 *
 * <p>Only dividends with ex-dividend timestamps strictly between valuation and
 * expiration are included. The method is commonly identified as {@code Vol2}
 * and remains an approximation; accuracy can deteriorate for schedules with
 * multiple or unusually large cash dividends.</p>
 *
 * @see <a href="https://doi.org/10.1002/wilm.42820030514">
 * Haug, Haug and Lewis, "Back to Basics: A New Approach to the Discrete
 * Dividend Problem", Wilmott, 2003, Appendix A</a>
 */
public final class HaugHaugAdjustment extends AbstractDiscreteDividendModel {

    /**
     * Creates a Haug-Haug volatility adjustment.
     *
     * @param contract European option contract whose expiration limits the
     *                 applicable dividend schedule
     * @param frame equity market data containing valuation time, spot and risk-free rate;
     *              its continuous dividend yield must be zero
     * @param dividends schedule of deterministic discrete cash dividends
     * @param volatility annualized volatility of the underlying, expressed as a decimal
     * @param dayCountConvention convention used to derive all year fractions
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
    public HaugHaugAdjustment(
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
    protected HaugHaugAdjustment newModel(
            EquityFrame newFrame,
            double newVolatility
    ) {
        return new HaugHaugAdjustment(
                contract(),
                newFrame,
                dividendSchedule(),
                newVolatility,
                dayCountConvention()
        );
    }

    /**
     * Calculates the time-weighted Haug-Haug volatility for applicable
     * discrete cash dividends.
     *
     * <p>When there are no applicable dividends, the integrated variance
     * reduces to the original input variance. At or past expiration, this
     * method returns the original input volatility.</p>
     *
     * @return Haug-Haug adjusted annualized volatility as a decimal
     */
    @Override
    public double adjustedVolatility() {
        double timeToExpiry = dayCountConvention().timeToExpiry(
                frame().timestampNanos(),
                contract().expirationDate()
        );
        if (timeToExpiry <= 0.0) {
            return volatility();
        }

        double spot = frame().spotPrice();
        double riskFreeRate = frame().riskFreeRate();
        double integratedVariance = 0.0;
        double previousTime = 0.0;
        double remainingDividendPresentValue = presentValueOfDividends();

        for (int j = 0; j < dividendCount(); j++) {
            double intervalVolatility = volatility() * spot
                    / (spot - remainingDividendPresentValue);
            double dividendTime = dividendTimeAt(j);

            integratedVariance += FastMath.pow2(intervalVolatility)
                    * (dividendTime - previousTime);

            remainingDividendPresentValue -= dividendAt(j).amount()
                    * FastMath.exp(-riskFreeRate * dividendTime);
            previousTime = dividendTime;
        }

        integratedVariance += FastMath.pow2(volatility())
                * (timeToExpiry - previousTime);

        return FastMath.sqrt(integratedVariance / timeToExpiry);
    }
}
