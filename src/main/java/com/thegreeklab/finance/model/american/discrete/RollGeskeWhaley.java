package com.thegreeklab.finance.model.american.discrete;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.exception.ExpiredContractException;
import com.thegreeklab.finance.exception.InvalidDateException;
import com.thegreeklab.finance.exception.MathException;
import com.thegreeklab.finance.exception.NonPositivePriceException;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.european.BlackScholes;
import com.thegreeklab.finance.model.european.discrete.CashDividend;
import com.thegreeklab.finance.model.greeks.BumpableOptionModel;
import com.thegreeklab.finance.model.greeks.StandardGreekValues;
import com.thegreeklab.finance.time.DayCountConvention;
import com.thegreeklab.finance.time.EpochNanos;
import com.thegreeklab.finance.validation.PricingValidation;
import com.thegreeklab.math.BivariateNormal;
import com.thegreeklab.math.ERF;
import com.thegreeklab.math.volatility.VolatilityPricer;
import net.jafama.FastMath;

import java.util.Objects;

import static com.thegreeklab.finance.validation.PricingValidation.requireAmericanStyle;
import static com.thegreeklab.finance.validation.PricingValidation.requireCall;
import static com.thegreeklab.finance.validation.PricingValidation.requireNoContinuousDividendYield;
import static com.thegreeklab.finance.validation.PricingValidation.requireValidVolatility;

/**
 * Roll-Geske-Whaley valuation model for an American equity call with one known
 * discrete cash dividend.
 *
 * <p>The model first removes the present value of the dividend from spot. If
 * early exercise immediately before the ex-dividend date cannot be optimal,
 * the result reduces to a European Black-Scholes call on that adjusted spot.
 * Otherwise, the critical ex-dividend stock price is found by bisection and
 * used in the compound-option formula.</p>
 *
 * <p>The implementation supports American calls only, exactly one strictly
 * positive cash dividend between valuation and expiration, and no simultaneous
 * continuous dividend yield. Instances are immutable and thread-safe.</p>
 *
 * <p>The five standard Greeks are numerical bump-and-revalue estimates.
 * Delta, gamma and rho use central differences. Vega uses the valid local
 * volatility interval, and theta normally advances valuation by one day. If
 * the ex-dividend date is at most one day away, theta uses a backward
 * difference so that repricing does not cross the discrete dividend event.
 * Numerical Greeks can be non-smooth near the early-exercise boundary.</p>
 *
 * @see <a href="https://doi.org/10.1016/0304-405X(77)90021-6">Roll (1977)</a>
 * @see <a href="https://doi.org/10.1016/0304-405X(79)90017-5">Geske (1979)</a>
 * @see <a href="https://doi.org/10.1016/0304-405X(81)90011-5">Whaley (1981)</a>
 */
public final class RollGeskeWhaley implements BumpableOptionModel, VolatilityPricer {

    private static final double CRITICAL_PRICE_TOLERANCE = 1e-10;
    private static final double DELTA_SPOT_BUMP = 1e-4;
    private static final double GAMMA_SPOT_BUMP = 1e-3;
    private static final double VOLATILITY_BUMP = 1e-4;
    private static final double RATE_BUMP = 1e-4;
    private static final int MAX_BRACKET_ITERATIONS = 100;
    private static final int MAX_BISECTION_ITERATIONS = 200;
    private static final long ONE_DAY_NANOS = 86_400_000_000_000L;

    private final OptionContract contract;
    private final EquityFrame frame;
    private final CashDividend dividend;
    private final DayCountConvention dayCountConvention;
    private final double spotPrice;
    private final double adjustedSpot;
    private final double strikePrice;
    private final double riskFreeRate;
    private final double volatility;
    private final double dividendAmount;
    private final double timeToExpiry;
    private final double timeToDividend;

    /**
     * Creates a Roll-Geske-Whaley model.
     *
     * @param contract American call contract being valued
     * @param frame equity market-data snapshot; its continuous dividend yield
     *              must be zero
     * @param volatility annualized volatility as a decimal
     * @param dividend single known cash dividend
     * @param convention convention used for both model year fractions
     * @throws NullPointerException if any reference argument is {@code null}
     * @throws ExpiredContractException if valuation is at or after expiration
     * @throws InvalidDateException if the ex-dividend timestamp is not strictly
     *                              between valuation and expiration
     * @throws NonPositivePriceException if subtracting the discounted dividend
     *                                   makes adjusted spot non-positive
     */
    public RollGeskeWhaley(
            OptionContract contract,
            EquityFrame frame,
            double volatility,
            CashDividend dividend,
            DayCountConvention convention
    ) {
        Objects.requireNonNull(contract, "Contract cannot be null.");
        Objects.requireNonNull(frame, "Frame cannot be null.");
        Objects.requireNonNull(dividend, "Dividend cannot be null.");
        Objects.requireNonNull(convention, "Convention cannot be null.");

        requireAmericanStyle(contract);
        requireCall(contract);
        requireValidVolatility(volatility);
        requireNoContinuousDividendYield(frame);

        this.contract = contract;
        this.frame = frame;
        this.dividend = dividend;
        this.dayCountConvention = convention;

        long valuationTimestamp = frame.timestampNanos();
        long expirationTimestamp = EpochNanos.from(contract.expirationDate());
        if (valuationTimestamp >= expirationTimestamp) {
            throw new ExpiredContractException(
                    "Roll-Geske-Whaley requires positive time to expiry."
            );
        }
        if (dividend.exTimestampNanos() <= valuationTimestamp
                || dividend.exTimestampNanos() >= expirationTimestamp) {
            throw new InvalidDateException(
                    "Dividend ex-date must be strictly after valuation and before expiration."
            );
        }

        this.spotPrice = frame.spotPrice();
        this.strikePrice = contract.strikePrice();
        this.riskFreeRate = frame.riskFreeRate();
        this.volatility = volatility;
        this.dividendAmount = dividend.amount();
        this.timeToExpiry = convention.yearFraction(
                valuationTimestamp,
                expirationTimestamp
        );
        this.timeToDividend = convention.yearFraction(
                valuationTimestamp,
                dividend.exTimestampNanos()
        );
        this.adjustedSpot = spotPrice
                - dividendAmount * FastMath.exp(-riskFreeRate * timeToDividend);

        if (!(adjustedSpot > 0.0) || !Double.isFinite(adjustedSpot)) {
            throw new NonPositivePriceException(
                    "Spot less the present value of the dividend must be positive and finite."
            );
        }
    }

    /**
     * Returns the Roll-Geske-Whaley value of the American call.
     *
     * @return theoretical option value per unit of the underlying
     * @throws MathException if a finite critical stock price cannot be bracketed
     *                       or the bisection solver fails to converge
     */
    @Override
    public double price() {
        double remainingTimeAfterDividend = timeToExpiry - timeToDividend;
        double earlyExerciseThreshold = strikePrice * (
                1.0 - FastMath.exp(-riskFreeRate * remainingTimeAfterDividend)
        );

        if (dividendAmount <= earlyExerciseThreshold) {
            return europeanPrice();
        }

        // In the limiting case D >= K, the critical ex-dividend stock price is zero.
        if (dividendAmount >= strikePrice) {
            return applyAmericanLowerBound(
                    spotPrice
                            - strikePrice * FastMath.exp(
                                    -riskFreeRate * timeToDividend
                            )
            );
        }

        double criticalPrice = findCriticalPrice(remainingTimeAfterDividend);
        double a1 = normalArgument(strikePrice, timeToExpiry);
        double b1 = normalArgument(criticalPrice, timeToDividend);
        double a2 = a1 - volatility * FastMath.sqrt(timeToExpiry);
        double b2 = b1 - volatility * FastMath.sqrt(timeToDividend);
        double correlation = -FastMath.sqrt(timeToDividend / timeToExpiry);

        double result = adjustedSpot * ERF.cdf(b1)
                + adjustedSpot * BivariateNormal.cdf(a1, -b1, correlation)
                - strikePrice * FastMath.exp(-riskFreeRate * timeToExpiry)
                * BivariateNormal.cdf(a2, -b2, correlation)
                - (strikePrice - dividendAmount)
                * FastMath.exp(-riskFreeRate * timeToDividend) * ERF.cdf(b2);

        if (!Double.isFinite(result)) {
            throw new MathException("Roll-Geske-Whaley produced a non-finite price.");
        }
        return applyAmericanLowerBound(result);
    }

    private double findCriticalPrice(double remainingTimeAfterDividend) {
        double low = 0.0;
        double high = FastMath.max(spotPrice, strikePrice);
        double highResidual = criticalPriceResidual(high, remainingTimeAfterDividend);

        int bracketIterations = 0;
        while (highResidual > 0.0 && bracketIterations < MAX_BRACKET_ITERATIONS) {
            high *= 2.0;
            if (!Double.isFinite(high)) {
                throw new MathException("Critical stock-price bracket overflowed.");
            }
            highResidual = criticalPriceResidual(high, remainingTimeAfterDividend);
            bracketIterations++;
        }
        if (highResidual > 0.0 || !Double.isFinite(highResidual)) {
            throw new MathException("Could not bracket the critical stock price.");
        }

        for (int iteration = 0; iteration < MAX_BISECTION_ITERATIONS; iteration++) {
            double midpoint = 0.5 * (low + high);
            double residual = criticalPriceResidual(
                    midpoint,
                    remainingTimeAfterDividend
            );
            double scaledTolerance = CRITICAL_PRICE_TOLERANCE
                    * FastMath.max(1.0, midpoint);

            if (FastMath.abs(residual) <= scaledTolerance
                    || high - low <= scaledTolerance) {
                return midpoint;
            }
            if (residual > 0.0) {
                low = midpoint;
            } else {
                high = midpoint;
            }
        }

        throw new MathException("Critical stock-price solver did not converge.");
    }

    private double criticalPriceResidual(
            double stockPriceAtDividend,
            double remainingTimeAfterDividend
    ) {
        return BlackScholes.callPrice(
                stockPriceAtDividend,
                strikePrice,
                remainingTimeAfterDividend,
                riskFreeRate,
                riskFreeRate,
                volatility
        ) - stockPriceAtDividend - dividendAmount + strikePrice;
    }

    private double normalArgument(double threshold, double time) {
        return (FastMath.log(adjustedSpot / threshold)
                + (riskFreeRate + 0.5 * volatility * volatility) * time)
                / (volatility * FastMath.sqrt(time));
    }

    private double europeanPrice() {
        return BlackScholes.callPrice(
                adjustedSpot,
                strikePrice,
                timeToExpiry,
                riskFreeRate,
                riskFreeRate,
                volatility
        );
    }

    private double applyAmericanLowerBound(double value) {
        return FastMath.max(
                value,
                FastMath.max(europeanPrice(), spotPrice - strikePrice)
        );
    }

    /**
     * Returns a new model with the underlying spot price replaced.
     *
    * @param newSpot replacement spot price
     * @return independently constructed model using {@code newSpot}
     */
    @Override
    public RollGeskeWhaley withSpot(double newSpot) {
        return new RollGeskeWhaley(
                contract,
                frame.withSpotPrice(newSpot),
                volatility,
                dividend,
                dayCountConvention
        );
    }

    /**
     * Returns a new model with annualized volatility replaced.
     *
     * @param newVolatility replacement volatility as a decimal
     * @return independently constructed model using {@code newVolatility}
     */
    @Override
    public RollGeskeWhaley withVolatility(double newVolatility) {
        return new RollGeskeWhaley(
                contract,
                frame,
                newVolatility,
                dividend,
                dayCountConvention
        );
    }

    /**
     * Returns a new model with the risk-free rate replaced.
     *
     * @param newRate replacement continuously compounded risk-free rate
     * @return independently constructed model using {@code newRate}
     */
    @Override
    public RollGeskeWhaley withRiskFreeRate(double newRate) {
        return new RollGeskeWhaley(
                contract,
                frame.withRiskFreeRate(newRate),
                volatility,
                dividend,
                dayCountConvention
        );
    }

    /**
     * Returns a new model observed at another valuation timestamp.
     *
     * <p>The dividend ex-date and contract expiration remain unchanged. The
     * replacement timestamp must therefore still precede both dates.</p>
     *
     * @param newTimestampNanos replacement timestamp in nanoseconds since the UNIX epoch
     * @return independently constructed model using {@code newTimestampNanos}
     */
    @Override
    public RollGeskeWhaley withTimestamp(long newTimestampNanos) {
        return new RollGeskeWhaley(
                contract,
                frame.withTimestampNanos(newTimestampNanos),
                volatility,
                dividend,
                dayCountConvention
        );
    }

    /**
     * Returns the convention used for all model year fractions.
     *
     * @return explicitly selected day-count convention
     */
    @Override
    public DayCountConvention dayCountConvention() {
        return dayCountConvention;
    }

    /**
     * Estimates delta by centrally bumping the unadjusted equity spot.
     *
     * @return first derivative of price with respect to spot
     */
    @Override
    public double delta() {
        double bump = spotBump(DELTA_SPOT_BUMP);
        double up = withSpot(spotPrice + bump).price();
        double down = withSpot(spotPrice - bump).price();
        return (up - down) / (2.0 * bump);
    }

    /**
     * Estimates gamma by centrally bumping the unadjusted equity spot.
     *
     * @return second derivative of price with respect to spot
     */
    @Override
    public double gamma() {
        return gamma(price());
    }

    private double gamma(double centerPrice) {
        double bump = spotBump(GAMMA_SPOT_BUMP);
        double up = withSpot(spotPrice + bump).price();
        double down = withSpot(spotPrice - bump).price();
        return (up - 2.0 * centerPrice + down) / (bump * bump);
    }

    private double spotBump(double relativeBump) {
        double requestedBump = FastMath.max(spotPrice * relativeBump, 1e-6);
        double validBump = 0.5 * FastMath.min(spotPrice, adjustedSpot);
        return FastMath.min(requestedBump, validBump);
    }

    /**
     * Estimates vega over the valid volatility interval around the current
     * value.
     *
     * @return first derivative of price with respect to volatility, per unit
     *         of volatility
     */
    @Override
    public double vega() {
        double bump = FastMath.max(volatility * VOLATILITY_BUMP, 1e-6);
        double volatilityDown = FastMath.max(
                volatility - bump,
                PricingValidation.MIN_VOLATILITY
        );
        double volatilityUp = volatility + bump;
        double up = withVolatility(volatilityUp).price();
        double down = withVolatility(volatilityDown).price();
        return (up - down) / (volatilityUp - volatilityDown);
    }

    /**
     * Estimates annualized theta without crossing the ex-dividend event.
     *
     * <p>A forward difference of at most one day is used when the dividend is
     * more than one day away. Otherwise, the method uses a one-day backward
     * difference on the same side of the dividend discontinuity.</p>
     *
     * @return first derivative of price with respect to the passage of time
     */
    @Override
    public double theta() {
        return theta(price());
    }

    private double theta(double currentPrice) {
        long valuationTimestamp = frame.timestampNanos();
        long remainingToDividend = Math.subtractExact(
                dividend.exTimestampNanos(),
                valuationTimestamp
        );

        if (remainingToDividend > ONE_DAY_NANOS) {
            long forwardBump = Math.min(
                    ONE_DAY_NANOS,
                    remainingToDividend / 2L
            );
            long bumpedTimestamp = Math.addExact(
                    valuationTimestamp,
                    forwardBump
            );
            double elapsedYears = dayCountConvention.yearFraction(
                    valuationTimestamp,
                    bumpedTimestamp
            );
            return (withTimestamp(bumpedTimestamp).price() - currentPrice)
                    / elapsedYears;
        }

        long previousTimestamp = Math.subtractExact(
                valuationTimestamp,
                ONE_DAY_NANOS
        );
        double elapsedYears = dayCountConvention.yearFraction(
                previousTimestamp,
                valuationTimestamp
        );
        return (currentPrice - withTimestamp(previousTimestamp).price())
                / elapsedYears;
    }

    /**
     * Estimates rho with a central one-basis-point rate bump.
     *
     * @return first derivative of price with respect to the continuously
     *         compounded risk-free rate, per unit of rate
     */
    @Override
    public double rho() {
        double up = withRiskFreeRate(riskFreeRate + RATE_BUMP).price();
        double down = withRiskFreeRate(riskFreeRate - RATE_BUMP).price();
        return (up - down) / (2.0 * RATE_BUMP);
    }

    /**
     * Calculates price and all five numerical Greeks while reusing the base
     * price for gamma and theta.
     *
     * @return immutable price and standard-Greek snapshot
     */
    @Override
    public StandardGreekValues greeks() {
        double currentPrice = price();
        return new StandardGreekValues(
                currentPrice,
                delta(),
                gamma(currentPrice),
                vega(),
                theta(currentPrice),
                rho()
        );
    }

    @Override
    public double priceAtVolatility(double volatility) {
        return withVolatility(volatility).price();
    }
}
