package com.thegreeklab.finance.time;

import com.thegreeklab.finance.exception.InvalidDateException;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Instant-preserving conversions between zoned date-times and
 * epoch-nanosecond values supported by a signed {@code long}. Converting back
 * produces the equivalent date-time in UTC.
 */
public final class EpochNanos {

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private EpochNanos() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Converts a date-time to nanoseconds since the UNIX epoch.
     *
     * @param timestamp timestamp to convert
     * @return epoch-nanosecond representation
     * @throws InvalidDateException if {@code timestamp} is {@code null}
     * @throws ArithmeticException  if the result does not fit in a {@code long}
     */
    public static long from(ZonedDateTime timestamp) {
        if (timestamp == null) {
            throw new InvalidDateException("Timestamp cannot be null.");
        }
        return Math.addExact(
                Math.multiplyExact(timestamp.toInstant().getEpochSecond(), NANOS_PER_SECOND),
                timestamp.getNano()
        );
    }

    /**
     * Converts epoch nanoseconds to a UTC date-time.
     *
     * @param timestampNanos nanoseconds since the UNIX epoch
     * @return equivalent UTC date-time
     */
    public static ZonedDateTime toUtc(long timestampNanos) {
        long epochSecond = Math.floorDiv(timestampNanos, NANOS_PER_SECOND);
        int nanoAdjustment = (int) Math.floorMod(timestampNanos, NANOS_PER_SECOND);
        return Instant.ofEpochSecond(epochSecond, nanoAdjustment).atZone(ZoneOffset.UTC);
    }
}
