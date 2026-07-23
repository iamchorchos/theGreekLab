package com.thegreeklab.finance.curves;

import net.jafama.FastMath;

/**
 * Shared interpolation routines for positive curve values.
 */
final class CurveInterpolation {

    private CurveInterpolation() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Interpolates strictly positive values linearly in logarithmic space.
     *
     * <p>Timestamp differences are calculated as {@code long} values before
     * conversion to {@code double}. Converting absolute epoch nanoseconds first
     * would lose sub-microsecond placement information for modern timestamps.</p>
     *
     * @param timestampNanos interpolation timestamp
     * @param leftTimestampNanos left node timestamp
     * @param leftValue positive left node value
     * @param rightTimestampNanos right node timestamp
     * @param rightValue positive right node value
     * @return log-linearly interpolated value
     * @throws ArithmeticException if a timestamp difference overflows a long
     */
    static double logLinear(
            long timestampNanos,
            long leftTimestampNanos,
            double leftValue,
            long rightTimestampNanos,
            double rightValue
    ) {
        long elapsedNanos = Math.subtractExact(timestampNanos, leftTimestampNanos);
        long intervalNanos = Math.subtractExact(rightTimestampNanos, leftTimestampNanos);
        double weight = (double) elapsedNanos / intervalNanos;
        double logValue = FastMath.log(leftValue)
                + weight * (FastMath.log(rightValue) - FastMath.log(leftValue));
        return FastMath.exp(logValue);
    }
}
