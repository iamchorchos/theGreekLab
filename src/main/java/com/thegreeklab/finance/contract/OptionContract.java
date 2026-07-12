package com.thegreeklab.finance.contract;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.exception.InvalidDateException;
import com.thegreeklab.finance.exception.NonPositivePriceException;
import com.thegreeklab.finance.frame.MarketData;

import java.time.Year;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Immutable description of a vanilla option contract: its underlying symbol,
 * type, strike, expiration date and contract multiplier.
 *
 * <p>This record is intentionally market-data-agnostic — it carries no spot
 * price, rate, or volatility — so the same {@code OptionContract} instance can
 * be repriced against different {@code MarketData} snapshots (e.g. for implied
 * volatility solving, or for re-marking a position over time) without
 * reconstruction.
 *
 * @param symbol                  ticker/identifier of the underlying instrument
 * @param type                    {@link OptionType#CALL} or {@link OptionType#PUT}
 * @param option                  the option type (European, American, Exotic)
 * @param strikePrice             the strike price; must be strictly positive
 * @param expirationDate          the expiration date/time of the contract
 * @param multiplier              the contract multiplier (e.g. 100 for standard equity options);
 *                                must be strictly positive
 * @param expirationNanosEpoch    pre-calculated expiration timestamp in nanoseconds since UNIX epoch
 * @param secondsInExpirationYear pre-calculated positive number of seconds in the expiration year
 */
public record OptionContract(
        String symbol,
        OptionType type,
        Option option,
        double strikePrice,
        ZonedDateTime expirationDate,
        int multiplier,
        @JsonIgnore
        long expirationNanosEpoch,
        @JsonIgnore
        double secondsInExpirationYear
) {

    /**
     * Validates contract invariants at construction time.
     *
     * @throws NullPointerException      if {@code symbol}, {@code type}, {@code option},
     *                                   or {@code expirationDate} is {@code null}
     * @throws IllegalArgumentException  if {@code symbol} is blank
     * @throws NonPositivePriceException if {@code strikePrice} is not strictly positive
     *                                   or not finite (e.g. {@code NaN} or {@code Infinity}), or if {@code multiplier}
     *                                   is not strictly positive
     * @throws InvalidDateException      if {@code secondsInExpirationYear} is not strictly positive and finite
     */
    public OptionContract {
        Objects.requireNonNull(symbol, "Symbol cannot be null");
        Objects.requireNonNull(expirationDate, "Expiration date cannot be null");
        Objects.requireNonNull(option, "Option style cannot be null");
        Objects.requireNonNull(type, "Option type cannot be null");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be blank.");
        }
        if (!(strikePrice > 0) || !Double.isFinite(strikePrice)) {
            throw new NonPositivePriceException("Strike price must be strictly positive and finite.");
        }
        if (multiplier <= 0) {
            throw new NonPositivePriceException("Multiplier must be strictly positive.");
        }
        if (!(secondsInExpirationYear > 0.0) || !Double.isFinite(secondsInExpirationYear)) {
            throw new InvalidDateException("Seconds in expiration year must be strictly positive and finite.");
        }
    }

    /**
     * Convenience constructor that derives {@code expirationNanosEpoch} and
     * {@code secondsInExpirationYear} from {@code expirationDate}, instead of
     * requiring the caller to precompute them.
     *
     * @param symbol         ticker/identifier of the underlying instrument
     * @param type           {@link OptionType#CALL} or {@link OptionType#PUT}
     * @param option         the option type (European, American, Exotic)
     * @param strikePrice    the strike price; must be strictly positive
     * @param expirationDate the expiration date/time of the contract
     * @param multiplier     the contract multiplier (e.g. 100 for standard equity options);
     *                       must be strictly positive
     * @throws NullPointerException      if {@code symbol}, {@code type}, or {@code option} is {@code null}
     * @throws IllegalArgumentException  if {@code symbol} is blank
     * @throws NonPositivePriceException if {@code strikePrice} or {@code multiplier}
     *                                   is not strictly positive
     * @throws InvalidDateException      if {@code expirationDate} is {@code null}
     * @throws ArithmeticException       if epoch-nanosecond conversion overflows a {@code long}
     */
    public OptionContract(String symbol, OptionType type, Option option, double strikePrice, ZonedDateTime expirationDate, int multiplier) {
        this(
                symbol, type, option, strikePrice, expirationDate, multiplier,
                MarketData.toEpochNanos(expirationDate),
                Year.isLeap(expirationDate.getYear()) ? 366.0 * 86_400.0 : 365.0 * 86_400.0
        );
    }

    /**
     * Computes the time to expiry in years, measured as actual elapsed seconds
     * divided by the number of seconds in the expiration year (365 or 366 days,
     * depending on whether the expiration year is a leap year). This is an
     * Actual/365 (or /366) Fixed convention keyed off the expiration year only —
     * not a true Actual/Actual convention, which would apportion elapsed days
     * separately across leap and non-leap years for periods spanning both.
     *
     * @param currentTimestampNanos the current ("as of") time to measure from in nanoseconds since UNIX epoch
     * @return the time to expiry in years; {@code 0.0} if {@code currentTimestampNanos}
     * is at or after expiration
     */
    public double getTimeToExpiry(long currentTimestampNanos) {
        long nanosRemaining = this.expirationNanosEpoch - currentTimestampNanos;

        if (nanosRemaining <= 0) {
            return 0.0;
        }

        return (nanosRemaining / 1_000_000_000.0) / this.secondsInExpirationYear;
    }

    /**
     * Returns a copy of this contract with a different strike price, leaving
     * the expiration timestamp fields untouched (they don't depend on strike).
     *
     * @param newStrike the new strike price; must be strictly positive and finite
     * @return a new {@code OptionContract} with {@code newStrike} in place of {@link #strikePrice()}
     * @throws NonPositivePriceException if {@code newStrike} is not strictly positive or not finite
     */
    public OptionContract withStrike(double newStrike) {
        return new OptionContract(symbol, type, option, newStrike, expirationDate, multiplier,
                expirationNanosEpoch, secondsInExpirationYear);
    }
}
