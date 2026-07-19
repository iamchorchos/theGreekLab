/**
 * Black-Scholes-Merton input-adjustment approximations for European options on
 * equities paying deterministic discrete cash dividends.
 *
 * <p>Models in this package require a zero continuous dividend yield and use
 * only schedule entries strictly between valuation and expiration. They expose
 * model-specific adjusted spot, strike and volatility values, immutable
 * bump-and-revalue scenarios, and numerical delta, gamma, vega, theta and
 * rho.</p>
 *
 * <p>The available approximations are Simple, Haug-Haug,
 * Bos-Gairat-Shepeleva and Bos-Vandermark. They are intended for fast European
 * vanilla-option estimates and should be independently validated for large or
 * dense dividend schedules.</p>
 */
package com.thegreeklab.finance.model.european.discrete.adjustments;
