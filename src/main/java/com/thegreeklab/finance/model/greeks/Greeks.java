package com.thegreeklab.finance.model.greeks;

/**
 * Common Greek and price surface exposed by option pricing models.
 *
 * <p>Unless a model documents otherwise, first-order Greeks are returned per
 * unit move in the underlying input: vega per 1.00 volatility, rho per 1.00
 * rate and theta on an annualized basis.
 */
public interface Greeks {

    /**
     * @return theoretical option value under the model
     */
    double price();

    /**
     * @return first derivative of price with respect to the underlying price
     */
    double delta();

    /**
     * @return second derivative of price with respect to the underlying price
     */
    double gamma();

    /**
     * @return first derivative of price with respect to volatility
     */
    double vega();

    /**
     * @return first derivative of price with respect to the passage of time,
     * annualized unless the concrete model documents another convention
     */
    double theta();

    /**
     * @return first derivative of price with respect to the discounting rate
     */
    double rho();

    /**
     * @return cross derivative of price with respect to underlying price and volatility
     */
    double vanna();

    /**
     * @return second derivative of price with respect to volatility
     */
    double volga();

    /**
     * @return derivative of delta with respect to the passage of time
     */
    double charm();

    /**
     * @return derivative of gamma with respect to the underlying price
     */
    double speed();

    /**
     * @return option elasticity, i.e. percentage option-price response to a
     * percentage move in the underlying
     */
    double lambda();

    /**
     * @return first derivative of price with respect to strike
     */
    double dualDelta();

    /**
     * @return derivative of rho with respect to volatility
     */
    double vera();

    /**
     * @return derivative of gamma with respect to volatility
     */
    double zomma();

    /**
     * @return derivative of gamma with respect to the passage of time
     */
    double color();

    /**
     * @return derivative of volga with respect to volatility
     */
    double ultima();

    /**
     * @return second derivative of price with respect to strike
     */
    double dualGamma();
}
