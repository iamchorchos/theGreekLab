package com.thegreeklab.math;

import net.jafama.FastMath;

/**
 * Peizer-Pratt inversion (method 2), used by the Leisen-Reimer binomial tree to
 * transform {@code d1}/{@code d2} into tree probabilities.
 *
 * <p>Unlike the plain Cox-Ross-Rubinstein tree, Leisen-Reimer chooses its
 * up-probability so that the tree converges smoothly (without the characteristic
 * oscillation) to the Black-Scholes price as the number of steps grows. This
 * uses the Peizer-Pratt {@code h2(x, steps)} transform rather than the plain
 * standard normal CDF. The transform is cheap enough to call once per tree
 * construction. See Leisen &amp; Reimer (1996), "Binomial Models for Option
 * Valuation - Examining and Improving Convergence".
 */
public final class PeizerPrattInversion {

    /** Continuity-correction coefficient (1/3) */
    private static final double ONE_THIRD = 0.3333333333333333;

    /** Continuity-correction coefficient (1/6) */
    private static final double ONE_SIXTH = 0.16666666666666666;

    private PeizerPrattInversion() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Computes {@code h2(x, steps)}: the Peizer-Pratt method-2 transform used
     * to derive Leisen-Reimer tree probabilities from {@code d1} or {@code d2}.
     *
     * @param x     the value being inverted, typically {@code d1} or {@code d2}
     *              from the closed-form Black-Scholes formula
     * @param steps the number of steps in the binomial tree; must be positive
     * @return the Leisen-Reimer probability transform value, in {@code [0, 1]}
     */
    public static double inverseFunction(double x, int steps) {
        double temp = x / (steps + ONE_THIRD + (0.1 / (steps + 1.0)));
        double power = -temp * temp * (steps + ONE_SIXTH);
        double sqRoot = FastMath.sqrt(-FastMath.expm1(power));
        return 0.5 + (Math.copySign(1.0, x) * 0.5) * sqRoot;
    }
}
