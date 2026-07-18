package com.thegreeklab.finance.frame;

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
     * Returns the observation timestamp of this market-data snapshot.
     *
     * @return the timestamp ("as of" time) of this market data snapshot, used
     * together with the option's expiration date to compute time to expiry
     */
    long timestampNanos();

    /**
     * Returns the price used as the underlying input to the pricing model.
     *
     * @return the spot price of the underlying asset (or, for futures, the
     * futures price standing in for spot in the pricing formula)
     */
    double spotPrice();

    /**
     * Returns the continuously compounded risk-free discount rate.
     *
     * @return the risk-free discount rate applicable to this market
     */
    double riskFreeRate();

    /**
     * Returns the generalized Black-Scholes cost-of-carry parameter.
     *
     * @return the cost-of-carry rate {@code b}, as defined by Haug's generalized
     * Black-Scholes-Merton model. The specific meaning of {@code b}
     * depends on the asset class (see class-level documentation).
     */
    double costOfCarry();

    /**
     * Creates a copy with a different risk-free rate.
     *
     * @param newRate the replacement risk-free rate
     * @return a copy of this frame with {@link #riskFreeRate()} replaced by {@code newRate};
     * all other fields (including {@link #costOfCarry()}'s other inputs) are left unchanged
     */
    MarketData withRiskFreeRate(double newRate);

    /**
     * Creates a copy with a different underlying price.
     *
     * @param newSpot the replacement spot price; must be strictly positive
     * @return a copy of this frame with {@link #spotPrice()} replaced by {@code newSpot}
     */
    MarketData withSpotPrice(double newSpot);

    /**
     * Creates a copy with a different observation timestamp.
     *
     * @param newTimestampNanos the replacement "as of" timestamp, in nanoseconds since the UNIX epoch
     * @return a copy of this frame with {@link #timestampNanos()} replaced by {@code newTimestampNanos}
     */
    MarketData withTimestampNanos(long newTimestampNanos);

}
