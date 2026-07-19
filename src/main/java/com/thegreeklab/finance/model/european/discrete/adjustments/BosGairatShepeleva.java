package com.thegreeklab.finance.model.european.discrete.adjustments;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.exception.NonPositivePriceException;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.european.discrete.CashDividend;
import com.thegreeklab.finance.model.european.discrete.DividendSchedule;
import com.thegreeklab.finance.time.DayCountConvention;
import com.thegreeklab.math.ERF;
import net.jafama.FastMath;

/**
 * Bos-Gairat-Shepeleva volatility adjustment for European equity options with
 * deterministic discrete cash dividends.
 *
 * <p>The returned volatility is intended to be used together with the
 * escrowed-dividend spot adjustment, in which the present value of dividends
 * paid before expiration is subtracted from the current spot price.
 * {@link #price()} performs a Black-Scholes-Merton valuation using both the
 * escrowed spot and the Bos-Gairat-Shepeleva adjusted volatility.</p>
 *
 * <p>Only dividends with ex-dividend timestamps strictly between the valuation
 * timestamp and option expiration are included. The approximation is intended
 * primarily for small to moderate dividends. Accuracy can deteriorate for
 * large dividends, long maturities, or schedules containing many payments.</p>
 *
 * <p>The implementation follows the analytical form of the adjustment
 * introduced by Bos, Gairat and Shepeleva and reproduced in Appendix B by
 * Haug, Haug and Lewis.</p>
 *
 * @see <a href="https://www.risk.net/derivatives/1530310/dealing-discrete-dividends">
 * Bos, Gairat and Shepeleva, "Dealing with Discrete Dividends", Risk 16(1),
 * 2003, pp. 109-112</a>
 * @see <a href="https://doi.org/10.1002/wilm.42820030514">
 * Haug, Haug and Lewis, "Back to Basics: A New Approach to the Discrete
 * Dividend Problem", Wilmott, 2003</a>
 */
public final class BosGairatShepeleva extends AbstractDiscreteDividendModel {

    /**
     * Creates a Bos-Gairat-Shepeleva volatility adjustment.
     *
     * @param contract European option contract whose strike and expiration are used
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
    public BosGairatShepeleva(
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
    protected BosGairatShepeleva newModel(
            EquityFrame newFrame,
            double newVolatility
    ) {
        return new BosGairatShepeleva(
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
     * <p>If the option is at or past expiration, the original input volatility
     * is returned. When there are no applicable dividends, the formula also
     * reduces to the original volatility.</p>
     *
     * @return Bos-Gairat-Shepeleva adjusted annualized volatility as a decimal
     */
    @Override
    public double adjustedVolatility() {
        double T = dayCountConvention().timeToExpiry(
                frame().timestampNanos(),
                contract().expirationDate()
        );
        if (T <= 0.0) {
            return volatility();
        }
        double sqrtT = FastMath.sqrt(T);
        double r = frame().riskFreeRate();
        int n = dividendCount();
        double S = FastMath.log(frame().spotPrice());
        double X = FastMath.log(
                (contract().strikePrice() + presentValueOfDividends())
                        * FastMath.exp(-r * T)
        );

        double factor = (S - X) / (volatility() * sqrtT);

        double z1 = factor + volatility() * sqrtT / 2.0;
        double z2 = factor + volatility() * sqrtT;

        double sum1 = 0.0;
        double sum2 = 0.0;

        for (int i = 0; i < n; i++) {
            CashDividend dividend = dividendAt(i);
            double ti = dividendTimeAt(i);
            sum1 = sum1 + dividend.amount() * FastMath.exp(-r * ti) * (ERF.cdf(z1) - ERF.cdf(z1 - volatility() * ti / sqrtT));
            for (int j = 0; j < n; j++) {
                double tj = dividendTimeAt(j);
                sum2 = sum2 + dividend.amount() * dividendAt(j).amount() * FastMath.exp(-r * (ti + tj)) * (ERF.cdf(z2) - ERF.cdf(z2 - 2 * volatility() * FastMath.min(ti, tj) / sqrtT));
            }
        }

        double temp1 = volatility() * FastMath.sqrt(FastMath.PI / (2.0 * T));
        double temp2 = 4.0 * FastMath.exp(z1 * z1 / 2 - S) * sum1;
        double temp3 = FastMath.exp(z2 * z2 / 2 - 2 * S) * sum2;
        return FastMath.sqrt(
                volatility() * volatility() + temp1 * (temp2 + temp3)
        );
    }
}
