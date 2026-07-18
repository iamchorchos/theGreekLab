package com.thegreeklab.finance.time;

import com.thegreeklab.finance.exception.InvalidDateException;

import java.time.ZonedDateTime;

/**
 * Conventions for converting elapsed time into a year fraction.
 *
 * <p>Callers explicitly select a convention for each valuation. Keeping this
 * policy outside contract data prevents a contract from carrying
 * valuation-specific time metadata and allows different conventions to be
 * used safely in the same process.</p>
 */
public enum DayCountConvention {

    /** Actual elapsed time divided by a fixed 365-day year. */
    ACT_365F(365.0 * 86_400.0),

    /** Actual elapsed time divided by a fixed 360-day year. */
    ACT_360(360.0 * 86_400.0);

    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final double secondsPerYear;

    DayCountConvention(double secondsPerYear) {
        this.secondsPerYear = secondsPerYear;
    }

    /**
     * Calculates a signed year fraction between two epoch-nanosecond timestamps.
     *
     * @param startTimestampNanos start timestamp in nanoseconds since the UNIX epoch
     * @param endTimestampNanos   end timestamp in nanoseconds since the UNIX epoch
     * @return signed year fraction from {@code startTimestampNanos} to
     * {@code endTimestampNanos}
     * @throws ArithmeticException if the timestamp difference overflows a {@code long}
     */
    public double yearFraction(long startTimestampNanos, long endTimestampNanos) {
        long elapsedNanos = Math.subtractExact(endTimestampNanos, startTimestampNanos);
        return (elapsedNanos / NANOS_PER_SECOND) / secondsPerYear;
    }

    /**
     * Calculates the non-negative time remaining until expiration.
     *
     * @param valuationTimestampNanos valuation timestamp in nanoseconds since the UNIX epoch
     * @param expirationDate          contract expiration date and time
     * @return time to expiry in years, or {@code 0.0} at and after expiration
     * @throws InvalidDateException if {@code expirationDate} is {@code null}
     * @throws ArithmeticException if timestamp conversion or subtraction overflows
     */
    public double timeToExpiry(long valuationTimestampNanos, ZonedDateTime expirationDate) {
        long expirationTimestampNanos = EpochNanos.from(expirationDate);
        if (valuationTimestampNanos >= expirationTimestampNanos) {
            return 0.0;
        }
        return yearFraction(valuationTimestampNanos, expirationTimestampNanos);
    }

    /**
     * Returns the number of seconds in one convention year.
     *
     * @return denominator used by this convention
     */
    public double secondsPerYear() {
        return secondsPerYear;
    }
}
