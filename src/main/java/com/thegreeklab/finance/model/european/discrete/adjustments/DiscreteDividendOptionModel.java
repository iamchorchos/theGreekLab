package com.thegreeklab.finance.model.european.discrete.adjustments;

import com.thegreeklab.finance.model.greeks.BumpableOptionModel;

/**
 * Common contract for European option models that approximate deterministic
 * discrete cash dividends through adjusted pricing inputs.
 *
 * <p>Implementations expose their method-specific adjusted spot, strike and
 * volatility, the resulting theoretical option value, immutable bump scenarios
 * and five standard bump-and-revalue Greeks.</p>
 *
 * <p>Delta, gamma and rho use central differences. Vega uses the valid local
 * interval around the original volatility, respecting the library minimum.
 * Theta advances the valuation timestamp by at most one calendar day and is
 * annualized under the configured day-count convention.</p>
 *
 * <p>Because the applicable dividend schedule changes discontinuously at an
 * ex-dividend timestamp, numerical theta can be sensitive when its time bump
 * crosses that boundary. All sensitivities are taken with respect to the
 * original market inputs, so they include the effect of every pricing-input
 * adjustment made by the concrete model.</p>
 */
public interface DiscreteDividendOptionModel extends BumpableOptionModel {

    /**
     * Returns spot adjusted for applicable cash dividends by the concrete model.
     *
     * @return model-specific adjusted spot
     */
    double adjustedSpot();

    /**
     * Returns strike price adjusted for applicable cash dividends.
     *
     * @return adjusted strike price
     */
    double adjustedStrike();

    /**
     * Returns volatility adjusted for applicable cash dividends.
     *
     * @return adjusted annualized volatility as a decimal
     */
    double adjustedVolatility();

}
