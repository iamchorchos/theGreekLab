package com.thegreeklab.finance.model.greeks;

/**
 * Immutable snapshot of an option value and its five standard Greeks.
 *
 * <p>The component units follow {@link StandardGreeks}: vega is expressed per
 * unit of volatility, rho per unit of continuously compounded rate, and theta
 * on an annualized basis unless the producing model documents otherwise.</p>
 *
 * @param price theoretical option value
 * @param delta first derivative with respect to the underlying price
 * @param gamma second derivative with respect to the underlying price
 * @param vega  first derivative with respect to volatility
 * @param theta first derivative with respect to the passage of time
 * @param rho   first derivative with respect to the discounting rate
 */
public record StandardGreekValues(
        double price,
        double delta,
        double gamma,
        double vega,
        double theta,
        double rho
) {
}
