package com.thegreeklab.finance.model.greeks;

/**
 * Core price and Greek surface shared by option-pricing models.
 *
 * <p>Unless a model documents otherwise, vega is returned per unit of
 * volatility, rho per unit of continuously compounded rate, and theta on an
 * annualized basis with respect to the passage of calendar time.</p>
 *
 * <p>Implementations may use analytical formulas, tree values or numerical
 * bump-and-revalue estimates. Concrete models document their calculation
 * method and any associated numerical limitations.</p>
 */
public interface StandardGreeks {

    /**
     * Returns the theoretical option value.
     *
     * @return theoretical option value under the model
     */
    double price();

    /**
     * Returns delta, the first-order spot sensitivity.
     *
     * @return first derivative of price with respect to the underlying price
     */
    double delta();

    /**
     * Returns gamma, the second-order spot sensitivity.
     *
     * @return second derivative of price with respect to the underlying price
     */
    double gamma();

    /**
     * Returns vega, the first-order volatility sensitivity.
     *
     * @return first derivative of price with respect to volatility
     */
    double vega();

    /**
     * Returns theta, the sensitivity to the passage of time.
     *
     * @return first derivative of price with respect to the passage of time,
     * annualized unless the concrete model documents another convention
     */
    double theta();

    /**
     * Returns rho, the first-order discount-rate sensitivity.
     *
     * @return first derivative of price with respect to the discounting rate
     */
    double rho();

    /**
     * Calculates the price and all five standard Greeks as one immutable
     * snapshot.
     *
     * <p>The default implementation delegates to the individual methods.
     * Models may override it to reuse intermediate calculations while
     * preserving the same values and units.</p>
     *
     * @return price, delta, gamma, vega, theta and rho for this model instance
     */
    default StandardGreekValues greeks() {
        return new StandardGreekValues(
                price(),
                delta(),
                gamma(),
                vega(),
                theta(),
                rho()
        );
    }

}
