package com.thegreeklab.finance.frame;

import com.thegreeklab.finance.exception.InvalidRateException;
import com.thegreeklab.finance.exception.NonPositivePriceException;
import com.thegreeklab.finance.time.EpochNanos;

import java.time.ZonedDateTime;

/**
 * Market data snapshot for equity (and equity-index) options, supporting a
 * continuous dividend yield.
 *
 * <p>Used to price standard Black-Scholes options ({@code dividendYield == 0})
 * as well as Merton-model options on dividend-paying stocks
 * ({@code dividendYield > 0}), via the cost-of-carry term {@code b = r - q}.
 *
 * @param timestampNanos the "as of" time of this snapshot in nanoseconds since the UNIX epoch
 * @param spotPrice      current spot price of the underlying equity
 * @param riskFreeRate   the risk-free interest rate, continuously compounded
 * @param dividendYield  the continuous dividend yield {@code q} paid by the underlying
 */

public record EquityFrame(
        long timestampNanos,
        double spotPrice,
        double riskFreeRate,
        double dividendYield
) implements MarketData {

    /**
     * Validates frame invariants at construction time.
     *
     * @throws NonPositivePriceException if {@code spotPrice} is not strictly positive
     * @throws InvalidRateException      if {@code riskFreeRate} or {@code dividendYield} is not finite
     */
    public EquityFrame {
        if (!(spotPrice > 0)) {
            throw new NonPositivePriceException("Spot price must be strictly positive and a valid number.");
        }
        if (!Double.isFinite(riskFreeRate) || !Double.isFinite(dividendYield)) {
            throw new InvalidRateException("Risk-free rate and dividend yield must be finite numbers.");
        }
    }

    /**
     * Convenience constructor that derives {@code timestampNanos} from a
     * {@link ZonedDateTime} instead of requiring a pre-converted epoch value.
     *
     * @param timestamp     the "as of" time of this snapshot
     * @param spotPrice     current spot price of the underlying equity
     * @param riskFreeRate  the risk-free interest rate, continuously compounded
     * @param dividendYield the continuous dividend yield {@code q} paid by the underlying
     * @throws NonPositivePriceException if {@code spotPrice} is not strictly positive
     * @throws InvalidRateException      if {@code riskFreeRate} or {@code dividendYield} is not finite
     */
    public EquityFrame(ZonedDateTime timestamp, double spotPrice, double riskFreeRate, double dividendYield) {
        this(
                EpochNanos.from(timestamp),
                spotPrice,
                riskFreeRate,
                dividendYield
        );
    }

    /**
     * @return the cost-of-carry rate for equities, {@code b = r - q}
     */

    @Override
    public double costOfCarry() {
        return riskFreeRate - dividendYield; // b = r - q
    }

    /**
     * @return a copy of this frame with the risk-free discount rate replaced by
     * {@code newRate}; {@code dividendYield} is left unchanged
     */
    @Override
    public EquityFrame withRiskFreeRate(double newRate) {
        return new EquityFrame(timestampNanos, spotPrice, newRate, dividendYield);
    }

    @Override
    public EquityFrame withSpotPrice(double newSpot) {
        return new EquityFrame(timestampNanos, newSpot, riskFreeRate, dividendYield);
    }

    @Override
    public EquityFrame withTimestampNanos(long newTimestampNanos) {
        return new EquityFrame(newTimestampNanos, spotPrice, riskFreeRate, dividendYield);
    }
}
