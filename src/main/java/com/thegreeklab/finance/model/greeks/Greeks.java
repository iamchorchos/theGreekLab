package com.thegreeklab.finance.model.greeks;

/**
 * Common Greek and price surface exposed by option pricing models.
 *
 * <p>Unless a model documents otherwise, first-order Greeks are returned per
 * unit move in the underlying input: vega per 1.00 volatility, rho per 1.00
 * rate and theta on an annualized basis.
 */
public interface Greeks extends StandardGreeks{

    /**
     * Returns vanna, the spot-volatility cross sensitivity.
     *
     * @return cross derivative of price with respect to underlying price and volatility
     */
    double vanna();

    /**
     * Returns volga, the second-order volatility sensitivity.
     *
     * @return second derivative of price with respect to volatility
     */
    double volga();

    /**
     * Returns charm, the time sensitivity of delta.
     *
     * @return derivative of delta with respect to the passage of time
     */
    double charm();

    /**
     * Returns speed, the spot sensitivity of gamma.
     *
     * @return derivative of gamma with respect to the underlying price
     */
    double speed();

    /**
     * Returns lambda, also known as option elasticity.
     *
     * @return option elasticity, i.e. percentage option-price response to a
     * percentage move in the underlying
     */
    double lambda();

    /**
     * Returns dual delta, the first-order strike sensitivity.
     *
     * @return first derivative of price with respect to strike
     */
    double dualDelta();

    /**
     * Returns vera, the volatility sensitivity of rho.
     *
     * @return derivative of rho with respect to volatility
     */
    double vera();

    /**
     * Returns zomma, the volatility sensitivity of gamma.
     *
     * @return derivative of gamma with respect to volatility
     */
    double zomma();

    /**
     * Returns color, the time sensitivity of gamma.
     *
     * @return derivative of gamma with respect to the passage of time
     */
    double color();

    /**
     * Returns ultima, the volatility sensitivity of volga.
     *
     * @return derivative of volga with respect to volatility
     */
    double ultima();

    /**
     * Returns dual gamma, the second-order strike sensitivity.
     *
     * @return second derivative of price with respect to strike
     */
    double dualGamma();
}
