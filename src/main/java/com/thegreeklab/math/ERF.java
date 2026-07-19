package com.thegreeklab.math;

import net.jafama.FastMath;

/**
 * High-precision complementary error function, and the standard normal CDF/PDF
 * built on top of it, used throughout the pricing engine wherever {@code N(x)}
 * or {@code N'(x)} appears in the Black-Scholes-Merton formulas.
 *
 * <p>{@link #erfc} implements W. J. Cody's rational Chebyshev approximation.
 * Its interval structure and published coefficients follow Cody's
 * <a href="https://netlib.org/specfun/erf">CALERF reference routine</a> from
 * Netlib SPECFUN. The calculation is split into three regions
 * ({@code |x| <= 0.46875}, {@code 0.46875 < |x| <= 4},
 * {@code |x| > 4}), each with its own set of rational polynomial coefficients
 * ({@link #A}/{@link #B}, {@link #C}/{@link #D}, {@link #P}/{@link #Q}
 * respectively) chosen to keep relative error within machine precision across
 * the whole real line. This is significantly more accurate than the simple
 * polynomial approximations (e.g. Abramowitz &amp; Stegun 7.1.26) commonly used
 * elsewhere, which matters here because {@code d1}/{@code d2} can be far from
 * zero for deep ITM/OTM options.
 */
public final class ERF {

    private static final double SQRT2 = FastMath.sqrt(2.0);
    private static final double INV_SQRT_PI = 0.56418958354775628695;
    private static final double SIXTEEN = 16.0;
    private static final double THRESH = 0.46875;
    private static final double INV_SQRT_2PI = 0.3989422804014327;

    private static final double[] A = {
            3.16112374387056560e00, 1.13864154151050156e02, 3.77485237685302021e02,
            3.20937758913846947e03, 1.85777706184603153e-1
    };
    private static final double[] B = {
            2.36012909523441209e01, 2.44024637934444173e02, 1.28261652607737228e03,
            2.84423683343917062e03
    };

    private static final double[] C = {
            5.64188496988670089e-1, 8.88314979438837594e0, 6.61191906371416295e01,
            2.98635138197400131e02, 8.81952221241769090e02, 1.71204761263407058e03,
            2.05107837782607147e03, 1.23033935479799725e03, 2.15311535474403846e-8
    };
    private static final double[] D = {
            1.57449261107098347e01, 1.17693950891312499e02, 5.37181101862009858e02,
            1.62138957456669019e03, 3.29079923573345963e03, 4.36261909014324716e03,
            3.43936767414372164e03, 1.23033935480374942e03
    };

    private static final double[] P = {
            3.05326634961232344e-1, 3.60344899949804439e-1, 1.25781726111229246e-1,
            1.60837851487422766e-2, 6.58749161529837803e-4, 1.63153871373020978e-2
    };
    private static final double[] Q = {
            2.56852019228982242e00, 1.87295284992346047e00, 5.27905102951428412e-1,
            6.05183413124413191e-2, 2.33520497626869185e-3
    };

    private ERF() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Computes the complementary error function {@code erfc(x) = 1 - erf(x)},
     * using Cody's three-region rational approximation.
     *
     * @param x the input value; any finite double, {@code +/-Infinity}, or
     *          {@code NaN}
     * @return {@code erfc(x)}, in {@code [0, 2]}; approaches {@code 0} as
     * {@code x -> +Infinity} and {@code 2} as {@code x -> -Infinity};
     * returns {@code NaN} when {@code x} is {@code NaN}
     */
    public static double erfc(double x) {
        if (Double.isNaN(x)) {
            return Double.NaN;
        }

        double y = FastMath.abs(x);
        double result;
        if (y <= THRESH) {
            double ysq = y * y;
            double xnum = A[4] * ysq;
            double xden = ysq;

            for (int i = 0; i < 3; i++) {
                xnum = (xnum + A[i]) * ysq;
                xden = (xden + B[i]) * ysq;
            }
            result = y * (xnum + A[3]) / (xden + B[3]);
            result = 1.0 - result; // erfc = 1 - erf

        } else if (y <= 4.0) {
            double xnum = C[8] * y;
            double xden = y;

            for (int i = 0; i < 7; i++) {
                xnum = (xnum + C[i]) * y;
                xden = (xden + D[i]) * y;
            }
            result = (xnum + C[7]) / (xden + D[7]);

            double ysq = FastMath.floor(y * SIXTEEN) / SIXTEEN;
            double del = (y - ysq) * (y + ysq);
            result = FastMath.exp(-ysq * ysq) * FastMath.exp(-del) * result;

        } else {
            if (y >= 26.543) return (x > 0.0) ? 0.0 : 2.0;

            double ysq = 1.0 / (y * y);
            double xnum = P[5] * ysq;
            double xden = ysq;

            for (int i = 0; i < 4; i++) {
                xnum = (xnum + P[i]) * ysq;
                xden = (xden + Q[i]) * ysq;
            }
            result = ysq * (xnum + P[4]) / (xden + Q[4]);
            result = (INV_SQRT_PI - result) / y;

            double yInt = FastMath.floor(y * SIXTEEN) / SIXTEEN;
            double del = (y - yInt) * (y + yInt);
            result = FastMath.exp(-yInt * yInt) * FastMath.exp(-del) * result;
        }

        return (x < 0.0) ? 2.0 - result : result;
    }

    /**
     * Standard normal cumulative distribution function {@code N(x)}, computed
     * via {@code N(x) = 0.5 * erfc(-x / sqrt(2))} for numerical accuracy across
     * the full range of {@code x}.
     *
     * @param x the input value (e.g. {@code d1} or {@code d2} in the BS formulas)
     * @return {@code N(x)}, in {@code [0, 1]}
     */
    public static double cdf(double x) {
        return 0.5 * erfc(-x / SQRT2);
    }

    /**
     * Standard normal probability density function {@code N'(x) = (1/sqrt(2*pi)) * e^(-x^2/2)}.
     *
     * @param x the input value (e.g. {@code d1} or {@code d2} in the BS formulas)
     * @return {@code N'(x)}, always {@code >= 0}
     */
    public static double pdf(double x) {
        return INV_SQRT_2PI * FastMath.exp(-0.5 * x * x);
    }
}
