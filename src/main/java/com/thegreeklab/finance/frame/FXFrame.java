package com.thegreeklab.finance.frame;

import com.thegreeklab.finance.exception.InvalidRateException;
import com.thegreeklab.finance.exception.NonPositivePriceException;

import java.time.ZonedDateTime;

/**
 * Market data snapshot for FX options, used to price options under the
 * Garman-Kohlhagen model.
 *
 * <p>The foreign interest rate is treated as a continuous "dividend yield"
 * earned by holding the foreign currency, giving the cost-of-carry term
 * {@code b = domesticRate - foreignRate}.
 *
 * @param timestampNanos the "as of" time of this snapshot in nanoseconds since the UNIX epoch
 * @param spotPrice      current spot exchange rate (domestic currency per unit of foreign currency)
 * @param domesticRate   the domestic risk-free interest rate, continuously compounded
 * @param foreignRate    the foreign risk-free interest rate, continuously compounded
 */
public record FXFrame(
        long timestampNanos,
        double spotPrice,
        double domesticRate,
        double foreignRate
) implements MarketData {

    /**
     * Validates frame invariants at construction time.
     *
     * @throws NonPositivePriceException if {@code spotPrice} is not strictly positive
     * @throws InvalidRateException      if {@code domesticRate} or {@code foreignRate} is not finite
     */
    public FXFrame {
        if (!(spotPrice > 0)) {
            throw new NonPositivePriceException("Spot price must be strictly positive and a valid number.");
        }
        if (!Double.isFinite(domesticRate) || !Double.isFinite(foreignRate)) {
            throw new InvalidRateException("Domestic and foreign rates must be finite numbers.");
        }
    }

    /**
     * Convenience constructor that derives {@code timestampNanos} from a
     * {@link ZonedDateTime} instead of requiring a pre-converted epoch value.
     *
     * @param timestamp    the "as of" time of this snapshot
     * @param spotPrice    current spot exchange rate (domestic currency per unit of foreign currency)
     * @param domesticRate the domestic risk-free interest rate, continuously compounded
     * @param foreignRate  the foreign risk-free interest rate, continuously compounded
     * @throws NonPositivePriceException if {@code spotPrice} is not strictly positive
     * @throws InvalidRateException      if {@code domesticRate} or {@code foreignRate} is not finite
     */
    public FXFrame(ZonedDateTime timestamp, double spotPrice, double domesticRate, double foreignRate) {
        this(
                MarketData.toEpochNanos(timestamp),
                spotPrice,
                domesticRate,
                foreignRate
        );
    }

    /**
     * @return the domestic interest rate, used as the discounting rate
     * {@code r} in the pricing formula
     */
    @Override
    public double riskFreeRate() {
        return domesticRate;
    }

    /**
     * @return the cost-of-carry rate for FX, {@code b = domesticRate - foreignRate}
     */
    @Override
    public double costOfCarry() {
        return domesticRate - foreignRate;
    }

    /**
     * @return a copy of this frame with the domestic discount rate replaced by
     * {@code newRate}; {@code foreignRate} is left unchanged
     */
    @Override
    public FXFrame withRiskFreeRate(double newRate) {
        return new FXFrame(timestampNanos, spotPrice, newRate, foreignRate);
    }

    @Override
    public FXFrame withSpotPrice(double newSpot) {
        return new FXFrame(timestampNanos, newSpot, domesticRate, foreignRate);
    }

    @Override
    public FXFrame withTimestampNanos(long newTimestampNanos) {
        return new FXFrame(newTimestampNanos, spotPrice, domesticRate, foreignRate);
    }
}
