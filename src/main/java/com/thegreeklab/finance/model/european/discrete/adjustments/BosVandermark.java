package com.thegreeklab.finance.model.european.discrete.adjustments;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.exception.NonPositivePriceException;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.european.discrete.DividendSchedule;
import com.thegreeklab.finance.time.DayCountConvention;
import net.jafama.FastMath;

/**
 * Bos-Vandermark approximation for European equity options with deterministic
 * discrete cash dividends.
 *
 * <p>For every applicable dividend, the method linearly splits its present
 * value into a near component and a far component according to the dividend's
 * position within the option lifetime. The near component is subtracted from
 * spot, while the far component is carried to expiration and added to strike.
 * The adjusted inputs are then priced with the original volatility using the
 * Black-Scholes-Merton model.</p>
 *
 * <p>Only dividends with ex-dividend timestamps strictly between valuation and
 * expiration are included. As an analytical approximation, the method can lose
 * accuracy for very large dividends.</p>
 *
 * @see <a href="https://www.risk.net/derivatives/equity-derivatives/1530307/finessing-fixed-dividends">
 * Bos and Vandermark, "Finessing Fixed Dividends", Risk 15(9), 2002,
 * pp. 157-158</a>
 */
public final class BosVandermark extends AbstractDiscreteDividendModel {

    private final double timeToExpiry;
    private final double nearDividendPv;
    private final double farDividendPv;

    private record DividendSplit(
            double nearPv,
            double farPv
    ) {
    }

    /**
     * Creates a Bos-Vandermark discrete-dividend approximation.
     *
     * @param contract European option contract whose strike and expiration are used
     * @param frame equity market data containing valuation time, spot and risk-free
     *              rate; its continuous dividend yield must be zero
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
     * @throws NonPositivePriceException if the Bos-Vandermark adjusted spot or
     *                                   strike is not positive and finite
     */
    public BosVandermark(
            OptionContract contract,
            EquityFrame frame,
            DividendSchedule dividends,
            double volatility,
            DayCountConvention dayCountConvention
    ) {
        super(contract, frame, dividends, volatility, dayCountConvention);

        this.timeToExpiry = dayCountConvention().timeToExpiry(
                frame().timestampNanos(),
                contract().expirationDate()
        );

        DividendSplit split = splitDividends();
        this.nearDividendPv = split.nearPv();
        this.farDividendPv = split.farPv();
        requirePositiveAdjustedSpot(adjustedSpot());
        requirePositiveAdjustedStrike(adjustedStrike());
    }

    /** {@inheritDoc} */
    @Override
    protected BosVandermark newModel(
            EquityFrame newFrame,
            double newVolatility
    ) {
        return new BosVandermark(
                contract(),
                newFrame,
                dividendSchedule(),
                newVolatility,
                dayCountConvention()
        );
    }

    private DividendSplit splitDividends() {
        if (timeToExpiry <= 0.0) {
            return new DividendSplit(0.0, 0.0);
        }

        double riskFreeRate = frame().riskFreeRate();
        double nearPv = 0.0;
        double farPv = 0.0;

        for (int i = 0; i < dividendCount(); i++) {
            double dividendTime = dividendTimeAt(i);
            double discountedDividend = dividendAt(i).amount()
                    * FastMath.exp(-riskFreeRate * dividendTime);

            nearPv += (timeToExpiry - dividendTime)
                    / timeToExpiry
                    * discountedDividend;

            farPv += dividendTime
                    / timeToExpiry
                    * discountedDividend;
        }

        return new DividendSplit(nearPv, farPv);
    }

    /**
     * Returns the original volatility, which Bos-Vandermark does not adjust.
     *
     * @return original annualized volatility as a decimal
     */
    @Override
    public double adjustedVolatility() {
        return volatility();
    }

    /**
     * Returns spot reduced by the near-dividend present value.
     *
     * @return Bos-Vandermark adjusted spot
     */
    @Override
    public double adjustedSpot() {
        return frame().spotPrice() - nearDividendPv;
    }

    /**
     * Returns strike increased by the far-dividend value carried to expiration.
     *
     * @return Bos-Vandermark adjusted strike
     */
    @Override
    public double adjustedStrike() {
        return contract().strikePrice()
                + farDividendPv
                * FastMath.exp(frame().riskFreeRate() * timeToExpiry);
    }
}
