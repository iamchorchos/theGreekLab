package com.thegreeklab.finance.frame;

import com.thegreeklab.finance.exception.InvalidDateException;

import java.time.ZonedDateTime;

/**
 * Common market-data contract consumed by the generalized Black-Scholes-Merton
 * pricing engine ({@code BlackScholes}).
 *
 * <p>Each asset class is modeled by its own {@code record} implementation, which
 * differs only in how it computes {@link #costOfCarry()} (the parameter {@code b}
 * in Haug's generalized formulation) and, where relevant, {@link #spotPrice()}
 * and {@link #riskFreeRate()}:
 * <ul>
 *     <li>{@code EquityFrame}  &mdash; {@code b = r - q}</li>
 *     <li>{@code FuturesFrame} &mdash; {@code b = 0}, spot price is the futures price</li>
 *     <li>{@code FXFrame}      &mdash; {@code b = domesticRate - foreignRate}</li>
 * </ul>
 * Being {@code sealed}, the permitted implementations are exhaustively known
 * at compile time, which the pricing engine relies on implicitly via its own
 * {@code sealed}/{@code permits} hierarchy.
 */
public sealed interface MarketData permits FXFrame, EquityFrame, FuturesFrame {

    /**
     * @return the timestamp ("as of" time) of this market data snapshot, used
     * together with the option's expiration date to compute time to expiry
     */
    long timestampNanos();

    /**
     * @return the spot price of the underlying asset (or, for futures, the
     * futures price standing in for spot in the pricing formula)
     */
    double spotPrice();

    /**
     * @return the risk-free discount rate applicable to this market
     */
    double riskFreeRate();

    /**
     * @return the cost-of-carry rate {@code b}, as defined by Haug's generalized
     * Black-Scholes-Merton model. The specific meaning of {@code b}
     * depends on the asset class (see class-level documentation).
     */
    double costOfCarry();

    /**
     * @param newRate the replacement risk-free rate
     * @return a copy of this frame with {@link #riskFreeRate()} replaced by {@code newRate};
     * all other fields (including {@link #costOfCarry()}'s other inputs) are left unchanged
     */
    MarketData withRiskFreeRate(double newRate);

    /**
     * @param newSpot the replacement spot price; must be strictly positive
     * @return a copy of this frame with {@link #spotPrice()} replaced by {@code newSpot}
     */
    MarketData withSpotPrice(double newSpot);

    /**
     * @param newTimestampNanos the replacement "as of" timestamp, in nanoseconds since the UNIX epoch
     * @return a copy of this frame with {@link #timestampNanos()} replaced by {@code newTimestampNanos}
     */
    MarketData withTimestampNanos(long newTimestampNanos);

    /**
     * Converts a {@link ZonedDateTime} to nanoseconds since the UNIX epoch, as used
     * by {@link #timestampNanos()} across all {@code MarketData} implementations.
     *
     * @param timestamp the timestamp to convert; must not be {@code null}
     * @return {@code timestamp}, expressed in nanoseconds since the UNIX epoch
     * @throws InvalidDateException if {@code timestamp} is {@code null}
     * @throws ArithmeticException  if the conversion overflows a {@code long}
     */
    static long toEpochNanos(ZonedDateTime timestamp) {
        if (timestamp == null) {
            throw new InvalidDateException("Timestamp cannot be null.");
        }
        return Math.addExact(
                Math.multiplyExact(timestamp.toInstant().getEpochSecond(), 1_000_000_000L),
                timestamp.getNano()
        );
    }
}