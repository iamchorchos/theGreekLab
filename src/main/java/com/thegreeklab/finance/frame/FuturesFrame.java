package com.thegreeklab.finance.frame;

import com.thegreeklab.finance.exception.InvalidRateException;
import com.thegreeklab.finance.exception.NonPositivePriceException;
import com.thegreeklab.finance.time.EpochNanos;

import java.time.ZonedDateTime;

/**
 * Market data snapshot for options on futures contracts, used to price
 * options under the Black-76 model.
 *
 * <p>Futures prices are already risk-neutral expectations of the underlying
 * spot price at delivery, so no cost-of-carry adjustment applies
 * ({@code b = 0}), and the futures price itself stands in for {@code spotPrice()}
 * in the generalized pricing formula.
 *
 * @param timestampNanos the "as of" time of this snapshot in nanoseconds since the UNIX epoch
 * @param futuresPrice   current price of the futures contract
 * @param riskFreeRate   the risk-free interest rate, continuously compounded
 */
public record FuturesFrame(
        long timestampNanos,
        double futuresPrice,
        double riskFreeRate
) implements MarketData {

    /**
     * Validates frame invariants at construction time.
     *
     * @throws NonPositivePriceException if {@code futuresPrice} is not strictly positive
     * @throws InvalidRateException      if {@code riskFreeRate} is not finite
     */
    public FuturesFrame {
        if (!(futuresPrice > 0)) {
            throw new NonPositivePriceException("Futures price must be strictly positive and a valid number.");
        }
        if (!Double.isFinite(riskFreeRate)) {
            throw new InvalidRateException("Risk-free rate must be finite.");
        }
    }

    /**
     * Convenience constructor that derives {@code timestampNanos} from a
     * {@link ZonedDateTime} instead of requiring a pre-converted epoch value.
     *
     * @param timestamp    the "as of" time of this snapshot
     * @param futuresPrice current price of the futures contract
     * @param riskFreeRate the risk-free interest rate, continuously compounded
     * @throws NonPositivePriceException if {@code futuresPrice} is not strictly positive
     * @throws InvalidRateException      if {@code riskFreeRate} is not finite
     */
    public FuturesFrame(ZonedDateTime timestamp, double futuresPrice, double riskFreeRate) {
        this(
                EpochNanos.from(timestamp),
                futuresPrice,
                riskFreeRate
        );
    }

    /**
     * @return the futures price, used as the "spot price" input to the
     * generalized pricing formula
     */
    @Override
    public double spotPrice() {
        return futuresPrice;
    }

    /**
     * @return {@code 0.0}; futures prices require no cost-of-carry adjustment
     * under the Black-76 model
     */
    @Override
    public double costOfCarry() {
        return 0.0;
    }

    /**
     * @return a copy of this frame with the risk-free discount rate replaced by
     * {@code newRate}; the observed futures price is left unchanged
     */
    @Override
    public FuturesFrame withRiskFreeRate(double newRate) {
        return new FuturesFrame(timestampNanos, futuresPrice, newRate);
    }

    @Override
    public FuturesFrame withSpotPrice(double newSpot) {
        return new FuturesFrame(timestampNanos, newSpot, riskFreeRate);
    }

    @Override
    public FuturesFrame withTimestampNanos(long newTimestampNanos) {
        return new FuturesFrame(newTimestampNanos, futuresPrice, riskFreeRate);
    }
}
