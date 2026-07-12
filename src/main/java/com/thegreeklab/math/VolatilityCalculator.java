package com.thegreeklab.math;

import com.thegreeklab.finance.exception.LackingDataException;
import com.thegreeklab.finance.exception.NonPositivePriceException;
import com.thegreeklab.finance.exception.UnsupportedExerciseStyleException;
import com.thegreeklab.finance.exception.VolatilityException;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.frame.MarketData;
import com.thegreeklab.finance.model.european.BlackScholes;
import net.jafama.FastMath;
import org.eclipse.collections.api.list.primitive.DoubleList;

import java.util.List;
import java.util.OptionalDouble;

/**
 * High-performance utility class for calculating historical and implied volatility.
 * Optimized for low-latency systems (Zero-Allocation in optimization loops, primitive collections).
 */
public final class VolatilityCalculator {

    private static final double MIN_VOLATILITY_BOUND = 1e-6;
    private static final double MAX_VOLATILITY_BOUND = 10.0;
    private static final double BRENT_TOLERANCE = 1e-8;
    private static final int BRENT_MAX_ITERATIONS = 200;
    private static final double MACHINE_EPSILON = Math.ulp(1.0);
    private static final double VOLATILITY_EPSILON = 1e-6;
    private static final double MIN_PRICE_BAR_VALUE = 1e-8;

    private VolatilityCalculator() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Calculates the annualized historical volatility using Welford's algorithm for numerical stability.
     *
     * @param prices             A time-ordered primitive list of strictly positive asset prices.
     * @param tradingDaysPerYear Number of trading days in a year for the specific asset class.
     * @return Annualized historical volatility as a decimal (e.g., 0.20 for 20%).
     * @throws LackingDataException      if {@code prices} is {@code null} or has fewer than 3 entries
     * @throws NonPositivePriceException if any price is not strictly positive and finite
     * @throws VolatilityException       if {@code tradingDaysPerYear} is not strictly positive
     */
    public static double historicalVolatility(DoubleList prices, int tradingDaysPerYear) {
        if (prices == null || prices.size() < 3) {
            throw new LackingDataException("Need at least 3 prices (2 returns) to calculate sample variance.");
        }

        if (tradingDaysPerYear <= 0) throw new VolatilityException("Trading days per year must be strictly positive.");

        int size = prices.size();
        double prev = prices.get(0);
        validatePositiveFinitePrice(prev, "Prices");

        double prevLog = FastMath.log(prev);

        int n = 0;
        double mean = 0.0, M2 = 0.0;

        for (int i = 1; i < size; i++) {
            double curr = prices.get(i);

            validatePositiveFinitePrice(curr, "Prices");

            double currLog = FastMath.log(curr);
            double logReturn = currLog - prevLog;

            n++;
            double delta = logReturn - mean;
            mean += delta / n;
            M2 += delta * (logReturn - mean);
            prevLog = currLog;
        }

        return FastMath.sqrt((M2 * tradingDaysPerYear) / (n - 1));
    }

    /**
     * Domain record for OHLC data to guarantee type safety and structural integrity.
     *
     * @param high the high price of the bar; must be strictly positive and {@code >= low}
     * @param low  the low price of the bar; must be strictly positive
     */
    public record PriceBar(double high, double low) {
        /**
         * Validates bar invariants at construction time.
         *
         * @throws VolatilityException if either bound is below safety threshold,
         *                             is not finite, or if {@code high < low}
         */
        public PriceBar {
            if (!Double.isFinite(high) || !Double.isFinite(low)
                    || high <= MIN_PRICE_BAR_VALUE || low <= MIN_PRICE_BAR_VALUE || high < low) {
                throw new VolatilityException(String.format("Invalid bar bounds: high=%.8f, low=%.8f", high, low));
            }
        }
    }

    /**
     * Calculates the annualized Parkinson's volatility based on high/low prices.
     *
     * <p>Parkinson's estimator uses the intraday high-low range rather than
     * close-to-close returns, making it a more efficient (lower-variance)
     * estimator of volatility under the assumption of continuous, driftless
     * price movement: {@code sigma = sqrt( (N/(4*ln2*n)) * sum( ln(H_i/L_i)^2 ) )}.
     *
     * @param ohlc               a list of high/low price bars; must contain at least 2 bars
     * @param tradingDaysPerYear number of trading days in a year, used for annualization
     * @return the annualized Parkinson's volatility as a decimal
     * @throws LackingDataException if {@code ohlc} is {@code null} or has fewer than 2 bars
     * @throws VolatilityException  if {@code tradingDaysPerYear} is not strictly positive,
     *                              or if {@code ohlc} contains null entries
     */
    public static double parkinsonsVolatility(List<PriceBar> ohlc, int tradingDaysPerYear) {
        if (ohlc == null || ohlc.size() < 2)
            throw new LackingDataException("Too few data points to calculate Parkinson volatility.");

        if (tradingDaysPerYear <= 0) throw new VolatilityException("Trading days per year must be strictly positive.");

        double sum = 0.0;
        for (PriceBar bar : ohlc) {
            if (bar == null) {
                throw new VolatilityException("OHLC bars cannot contain null entries.");
            }
            double logHL = FastMath.log(bar.high() / bar.low());
            sum += logHL * logHL;
        }

        double factor = tradingDaysPerYear / (4.0 * FastMath.log(2) * ohlc.size());
        return FastMath.sqrt(factor * sum);
    }

    /**
     * Calculates Implied Volatility using Brent's method.
     *
     * <p>Before searching, the market price is checked against the model-free
     * arbitrage bounds for the option's type (the standard European call/put
     * bounds implied by put-call parity and discounting). If the supplied
     * {@code marketPrice} falls outside the no-arbitrage range, the method
     * returns empty rather than attempting to solve, since no volatility
     * could reproduce that price under the model.
     *
     * <p>An initial Brenner-Subrahmanyan-style at-the-money approximation is
     * used as a starting guess to bracket the root before handing off to
     * {@link #brent}.
     * Returns an empty Optional if the market price violates arbitrage boundaries or algorithm fails to converge.
     *
     * @param contract    the option contract whose implied volatility is sought
     * @param frame       the market data snapshot (spot, rate, cost-of-carry) to price against
     * @param marketPrice the observed market price of the option
     * @return the implied volatility as a decimal, or {@link OptionalDouble#empty()}
     * if {@code contract} has already expired, the price is outside arbitrage bounds,
     * or the solver fails to converge
     * @throws NonPositivePriceException if spot price, strike price, or market price
     *                                   is not strictly positive and finite
     * @throws UnsupportedExerciseStyleException if {@code contract} is not European
     */
    public static OptionalDouble impliedVolatility(OptionContract contract, MarketData frame, double marketPrice) {
        if (contract.option() != Option.EUROPEAN) {
            throw new UnsupportedExerciseStyleException("Implied volatility solver supports only EUROPEAN options. Received: " + contract.option());
        }

        double timeToExpiry = contract.getTimeToExpiry(frame.timestampNanos());
        if (timeToExpiry <= 0) return OptionalDouble.empty();

        validatePositiveFinitePrice(frame.spotPrice(), "Spot price");
        validatePositiveFinitePrice(contract.strikePrice(), "Strike price");
        validatePositiveFinitePrice(marketPrice, "Market price");

        double dividendYield = frame.riskFreeRate() - frame.costOfCarry();

        double maxCallPrice = frame.spotPrice() * FastMath.exp(-dividendYield * timeToExpiry);
        double maxPutPrice = contract.strikePrice() * FastMath.exp(-frame.riskFreeRate() * timeToExpiry);
        double maxPrice = (contract.type() == OptionType.CALL) ? maxCallPrice : maxPutPrice;

        if (marketPrice >= maxPrice) return OptionalDouble.empty();

        double minCallPrice = Math.max(0.0, maxCallPrice - contract.strikePrice() * FastMath.exp(-frame.riskFreeRate() * timeToExpiry));
        double minPutPrice = Math.max(0.0, maxPutPrice - frame.spotPrice() * FastMath.exp(-dividendYield * timeToExpiry));
        double minPrice = (contract.type() == OptionType.CALL) ? minCallPrice : minPutPrice;

        if (marketPrice <= minPrice) return OptionalDouble.empty();

        double guess = (marketPrice / frame.spotPrice()) * FastMath.sqrt(2 * Math.PI / timeToExpiry);
        guess = Math.min(Math.max(guess, MIN_VOLATILITY_BOUND + VOLATILITY_EPSILON), MAX_VOLATILITY_BOUND - VOLATILITY_EPSILON);

        double fLo = priceError(contract, frame, MIN_VOLATILITY_BOUND, marketPrice);
        double fGuess = priceError(contract, frame, guess, marketPrice);
        double fHi = priceError(contract, frame, MAX_VOLATILITY_BOUND, marketPrice);

        if (fLo * fHi > 0) return OptionalDouble.empty();

        if (fLo * fGuess <= 0)
            return brent(contract, frame, marketPrice, MIN_VOLATILITY_BOUND, guess, fLo, fGuess, BRENT_TOLERANCE, BRENT_MAX_ITERATIONS);
        else
            return brent(contract, frame, marketPrice, guess, MAX_VOLATILITY_BOUND, fGuess, fHi, BRENT_TOLERANCE, BRENT_MAX_ITERATIONS);
    }

    /**
     * Objective function for the implied-volatility root search: the signed
     * difference between the model price at trial volatility and the
     * observed market price. The root of this function (in {@code vol}) is
     * the implied volatility.
     *
     * @param contract    the option contract being priced
     * @param frame       the market data snapshot to price against
     * @param vol         the trial volatility
     * @param marketPrice the observed market price being matched
     * @return {@code BlackScholes.price(contract, frame, vol) - marketPrice}
     */
    private static double priceError(OptionContract contract, MarketData frame, double vol, double marketPrice) {
        return BlackScholes.price(contract, frame, vol) - marketPrice;
    }

    private static void validatePositiveFinitePrice(double price, String label) {
        if (!(price > 0.0) || !Double.isFinite(price)) {
            throw new NonPositivePriceException(label + " must be strictly positive and finite. Found: " + price);
        }
    }

    /**
     * Finds a root of {@link #priceError} within the bracket {@code [a, b]}
     * using Brent's method (a combination of bisection, secant, and inverse
     * quadratic interpolation steps), per the classic algorithm in Numerical
     * Recipes / Brent (1973).
     *
     * @param contract    the option contract being priced
     * @param frame       the market data snapshot to price against
     * @param marketPrice the observed market price being matched
     * @param a           lower bracket bound (volatility)
     * @param b           upper bracket bound (volatility); the current best estimate
     * @param fa          {@code priceError} evaluated at {@code a}
     * @param fb          {@code priceError} evaluated at {@code b}
     * @param tol         absolute convergence tolerance on the volatility estimate
     * @param maxIter     maximum number of iterations before giving up
     * @return the converged implied volatility as {@link OptionalDouble#of}, or
     * {@link OptionalDouble#empty()} if {@code maxIter} is exceeded without convergence
     */
    private static OptionalDouble brent(OptionContract contract, MarketData frame,
                                        double marketPrice, double a, double b,
                                        double fa, double fb, double tol, int maxIter) {
        double c = a, fc = fa;
        double d = b - a, e = d;

        for (int i = 0; i < maxIter; i++) {
            if (fb * fc > 0) {
                c = a;
                fc = fa;
                d = e = b - a;
            }
            if (Math.abs(fc) < Math.abs(fb)) {
                double tmpX = b;
                b = c;
                c = tmpX;
                double tmpF = fb;
                fb = fc;
                fc = tmpF;
                a = c;
                fa = fc;
            }

            double tol1 = 2 * MACHINE_EPSILON * Math.abs(b) + 0.5 * tol;
            double xm = 0.5 * (c - b);

            if (Math.abs(xm) <= tol1 || Math.abs(fb) <= MACHINE_EPSILON) return OptionalDouble.of(b);

            if (Math.abs(e) >= tol1 && Math.abs(fa) > Math.abs(fb)) {
                double s = fb / fa;
                double p, q;
                if (Math.abs(a - c) <= MACHINE_EPSILON) {
                    p = 2 * xm * s;
                    q = 1 - s;
                } else {
                    double r = fb / fc;
                    q = fa / fc;
                    p = s * (2 * xm * q * (q - r) - (b - a) * (r - 1));
                    q = (q - 1) * (r - 1) * (s - 1);
                }
                if (p > 0) q = -q;
                else p = -p;

                if (2 * p < Math.min(3 * xm * q - Math.abs(tol1 * q), Math.abs(e * q))) {
                    e = d;
                    d = p / q;
                } else {
                    d = xm;
                    e = d;
                }
            } else {
                d = xm;
                e = d;
            }

            a = b;
            fa = fb;
            b += (Math.abs(d) > tol1) ? d : Math.copySign(tol1, xm);
            fb = priceError(contract, frame, b, marketPrice);
        }

        return OptionalDouble.empty();
    }
}
