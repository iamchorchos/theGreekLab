package com.thegreeklab.finance.model.greeks;

import com.thegreeklab.finance.time.DayCountConvention;

/**
 * Immutable option model that can create bumped copies for scenario repricing.
 *
 * <p>Every method returns a new model with exactly one input replaced. The
 * original instance remains unchanged. Implementations may narrow the return
 * type covariantly to their concrete model class.</p>
 */
public interface BumpableOptionModel extends StandardGreeks {

    /**
     * Returns the convention used to derive time to expiry.
     *
     * @return explicitly selected day-count convention
     */
    DayCountConvention dayCountConvention();

    /**
     * Creates an equivalent model with a different underlying price.
     *
     * @param newSpot replacement underlying price
     * @return newly constructed model using {@code newSpot}
     */
    BumpableOptionModel withSpot(double newSpot);

    /**
     * Creates an equivalent model with a different annualized volatility.
     *
     * @param newVolatility replacement volatility as a decimal
     * @return newly constructed model using {@code newVolatility}
     */
    BumpableOptionModel withVolatility(double newVolatility);

    /**
     * Creates an equivalent model with a different risk-free rate.
     *
     * <p>The supplied market-data frame determines how the rate change affects
     * its generalized cost of carry.</p>
     *
     * @param newRate replacement continuously compounded risk-free rate
     * @return newly constructed model using {@code newRate}
     */
    BumpableOptionModel withRiskFreeRate(double newRate);

    /**
     * Creates an equivalent model observed at a different instant.
     *
     * @param newTimestampNanos replacement timestamp in nanoseconds since the UNIX epoch
     * @return newly constructed model using {@code newTimestampNanos}
     */
    BumpableOptionModel withTimestamp(long newTimestampNanos);
}
