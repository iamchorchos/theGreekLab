package com.thegreeklab.finance.contract;

import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.exception.InvalidDateException;
import com.thegreeklab.finance.exception.NonPositivePriceException;

import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Immutable, market-data-agnostic description of a vanilla option contract.
 *
 * <p>The expiration is represented exactly once, by {@code expirationDate}.
 * Pricing models derive any timestamp or year fraction from that value and
 * their valuation context, so contradictory expiration metadata cannot be
 * constructed.</p>
 *
 * @param symbol         ticker or identifier of the underlying instrument
 * @param type           {@link OptionType#CALL} or {@link OptionType#PUT}
 * @param option         exercise style of the option
 * @param strikePrice    strike price; must be strictly positive and finite
 * @param expirationDate expiration date and time
 * @param multiplier     contract multiplier; must be strictly positive
 */
public record OptionContract(
        String symbol,
        OptionType type,
        Option option,
        double strikePrice,
        ZonedDateTime expirationDate,
        int multiplier
) {

    /**
     * Validates contract invariants at construction time.
     *
     * @throws NullPointerException      if {@code symbol}, {@code type}, or
     *                                   {@code option} is {@code null}
     * @throws InvalidDateException      if {@code expirationDate} is {@code null}
     * @throws IllegalArgumentException  if {@code symbol} is blank
     * @throws NonPositivePriceException if {@code strikePrice} is not strictly
     *                                   positive and finite, or if
     *                                   {@code multiplier} is not positive
     */
    public OptionContract {
        Objects.requireNonNull(symbol, "Symbol cannot be null");
        Objects.requireNonNull(option, "Option style cannot be null");
        Objects.requireNonNull(type, "Option type cannot be null");
        if (expirationDate == null) {
            throw new InvalidDateException("Expiration date cannot be null.");
        }
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be blank.");
        }
        if (!(strikePrice > 0.0) || !Double.isFinite(strikePrice)) {
            throw new NonPositivePriceException("Strike price must be strictly positive and finite.");
        }
        if (multiplier <= 0) {
            throw new NonPositivePriceException("Multiplier must be strictly positive.");
        }
    }

    /**
     * Returns a copy with a different strike price.
     *
     * @param newStrike replacement strike; must be strictly positive and finite
     * @return a contract equal to this one except for its strike
     * @throws NonPositivePriceException if {@code newStrike} is invalid
     */
    public OptionContract withStrike(double newStrike) {
        return new OptionContract(symbol, type, option, newStrike, expirationDate, multiplier);
    }
}
