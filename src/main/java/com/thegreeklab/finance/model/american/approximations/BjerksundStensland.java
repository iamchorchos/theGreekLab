package com.thegreeklab.finance.model.american.approximations;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.exception.InvalidVolatilityException;
import com.thegreeklab.finance.exception.UnsupportedExerciseStyleException;
import com.thegreeklab.finance.frame.MarketData;
import com.thegreeklab.finance.model.european.BlackScholes;
import com.thegreeklab.finance.model.greeks.AbstractBumpAndRevalueModel;
import com.thegreeklab.finance.time.DayCountConvention;
import com.thegreeklab.finance.time.EpochNanos;
import com.thegreeklab.finance.validation.PricingValidation;
import com.thegreeklab.math.BivariateNormal;
import com.thegreeklab.math.ERF;
import net.jafama.FastMath;

import java.util.Objects;

/**
 * Prices vanilla American options with the Bjerksund-Stensland 2002
 * closed-form approximation.
 *
 * <p>The implementation uses the generalized cost-of-carry formulation. The
 * value of {@code b} is supplied by {@link MarketData#costOfCarry()}, allowing
 * the same engine to price equity, futures and FX options. American puts are
 * evaluated through put-call symmetry by transforming them into calls.</p>
 *
 * <p>When {@code b >= r}, early exercise of the transformed call is not
 * optimal and the engine returns the generalized European Black-Scholes value.
 * The final price is bounded from below by both the European value and the
 * intrinsic value. If the analytical exercise-boundary calculation becomes
 * numerically unstable, the same no-arbitrage lower bound is returned as a
 * safe fallback.</p>
 *
 * <p>The {@code ksi} term requires the bivariate normal CDF provided by
 * {@link BivariateNormal}, which delegates to the native {@code pbivnorm}
 * routine through the Java Foreign Function and Memory API.</p>
 *
 * <p>The standard Greeks are numerical bump-and-revalue estimates. Delta,
 * gamma and rho use central differences; vega uses the valid interval around
 * the current volatility and respects
 * {@link PricingValidation#MIN_VOLATILITY}; theta advances the market-data
 * timestamp by at most one calendar day and is returned on an annualized
 * basis.</p>
 *
 * <p>The approximation is piecewise smooth because it selects between
 * intrinsic value, the European lower bound and the analytical approximation.
 * Numerical Greeks may therefore be sensitive to bump size near an exercise
 * boundary, expiry or a numerical fallback boundary.</p>
 *
 * <p>The formula and notation follow Espen Gaarder Haug,
 * <i>The Complete Guide to Option Pricing Formulas</i>, 2nd edition.</p>
 *
 * <p>Instances are immutable and safe to share between threads.</p>
 *
 * @see BlackScholes
 */
public final class BjerksundStensland extends AbstractBumpAndRevalueModel {

    private final double strikePrice;
    private final double spotPrice;
    private final double timeToExpiry;
    private final double volatility;
    private final double volatilitySq;
    private final double riskFreeRate;
    private final double costOfCarry;
    private final OptionType type;
    private final OptionContract contract;
    private final MarketData frame;
    private final DayCountConvention dayCountConvention;

    /**
     * Creates an American-option pricing engine for a contract and a market-data snapshot.
     *
     * @param optionContract American vanilla option contract
     * @param marketData     market-data snapshot supplying spot, rates and cost of carry
     * @param volatility     annualized volatility as a decimal; must be finite and at
     *                       least {@link PricingValidation#MIN_VOLATILITY}
     * @param dayCountConvention convention used to derive the year fraction
     * @throws NullPointerException              if {@code optionContract} or
     *                                           {@code marketData} is {@code null}
     * @throws InvalidVolatilityException        if {@code volatility} is not finite
     *                                           or is below the supported minimum
     * @throws UnsupportedExerciseStyleException if the contract is not American
     */
    public BjerksundStensland(
            OptionContract optionContract,
            MarketData marketData,
            double volatility,
            DayCountConvention dayCountConvention
    ) {
        Objects.requireNonNull(optionContract, "Option contract cannot be null.");
        Objects.requireNonNull(marketData, "Market data cannot be null.");
        Objects.requireNonNull(dayCountConvention, "Day-count convention cannot be null.");

        if (optionContract.option() != Option.AMERICAN) {
            throw new UnsupportedExerciseStyleException("Unsupported exercise style: " + optionContract.option());
        }

        PricingValidation.requireValidVolatility(volatility);

        this.strikePrice = optionContract.strikePrice();
        this.spotPrice = marketData.spotPrice();
        this.timeToExpiry = dayCountConvention.timeToExpiry(
                marketData.timestampNanos(),
                optionContract.expirationDate()
        );
        this.volatility = volatility;
        this.volatilitySq = volatility * volatility;
        this.riskFreeRate = marketData.riskFreeRate();
        this.costOfCarry = marketData.costOfCarry();
        this.type = optionContract.type();
        this.contract = optionContract;
        this.frame = marketData;
        this.dayCountConvention = dayCountConvention;
    }

    /**
     * Returns the Bjerksund-Stensland 2002 approximation of the option value.
     *
     * <p>At or after expiry this method returns intrinsic value. Before expiry,
     * the returned value is never below the corresponding European price or
     * intrinsic value.</p>
     *
     * @return theoretical value of the American option
     */
    @Override
    public double price() {
        if (timeToExpiry <= 0.0) {
            return switch (type) {
                case CALL -> FastMath.max(spotPrice - strikePrice, 0.0);
                case PUT -> FastMath.max(strikePrice - spotPrice, 0.0);
            };
        }

        return switch (type) {
            case CALL ->
                    americanCallApproximation(spotPrice, strikePrice, timeToExpiry, riskFreeRate, costOfCarry, volatility);
            case PUT ->
                    americanCallApproximation(strikePrice, spotPrice, timeToExpiry, riskFreeRate - costOfCarry, -costOfCarry, volatility);
        };
    }

    private double americanCallApproximation(double S, double X, double T, double r, double b, double volatility) {
        double intrinsicValue = FastMath.max(S - X, 0.0);
        double europeanValue = BlackScholes.callPrice(S, X, T, r, b, volatility);
        double noArbitrageLowerBound = FastMath.max(intrinsicValue, europeanValue);

        double t1 = 0.5 * (FastMath.sqrt(5) - 1.0) * T;
        if (b >= r) {
            return noArbitrageLowerBound;
        } else {
            double beta = calcBeta(b, r);
            double bInfinity = (beta / (beta - 1.0)) * X;
            double b0 = FastMath.max(X, (r / (r - b)) * X);

            if (!(beta > 1.0)
                    || !Double.isFinite(beta)
                    || !(bInfinity > 0.0)
                    || !Double.isFinite(bInfinity)
                    || !(b0 > 0.0)
                    || !Double.isFinite(b0)) {
                return noArbitrageLowerBound;
            }

            double ht1 = calcHt(b, X, t1, bInfinity, b0);
            double ht2 = calcHt(b, X, T, bInfinity, b0);

            double I1 = calcI(b0, bInfinity, ht1);
            double I2 = calcI(b0, bInfinity, ht2);

            if (!(I1 > 0.0)
                    || !Double.isFinite(I1)
                    || !(I2 > 0.0)
                    || !Double.isFinite(I2)) {
                return noArbitrageLowerBound;
            }

            double alpha1 = calcAlpha(X, I1, beta);
            double alpha2 = calcAlpha(X, I2, beta);

            if (!Double.isFinite(alpha1) || !Double.isFinite(alpha2)) {
                return noArbitrageLowerBound;
            }

            if (S >= I2) {
                return noArbitrageLowerBound;
            } else {
                double approximation = alpha2 * FastMath.pow(S, beta) - alpha2 * phi(S, t1, beta, I2, I2, r, b) + phi(S, t1, 1, I2, I2, r, b)
                        - phi(S, t1, 1, I1, I2, r, b) - X * phi(S, t1, 0, I2, I2, r, b) + X * phi(S, t1, 0, I1, I2, r, b)
                        + alpha1 * phi(S, t1, beta, I1, I2, r, b) - alpha1 * ksi(S, T, beta, I1, I2, I1, t1, r, b) + ksi(S, T, 1, I1, I2, I1, t1, r, b)
                        - ksi(S, T, 1, X, I2, I1, t1, r, b) - X * ksi(S, T, 0, I1, I2, I1, t1, r, b) + X * ksi(S, T, 0, X, I2, I1, t1, r, b);

                if (!Double.isFinite(approximation)) {
                    return noArbitrageLowerBound;
                }

                return FastMath.max(noArbitrageLowerBound, approximation);
            }

        }
    }

    private double phi(double S, double t, double gamma, double h, double i, double r, double b) {
        double lambda = calcLambda(r, b, gamma);

        double d = -calcD1(S, h, b, gamma, t);
        double kappa = calcKappa(b, gamma);
        double temp = d - 2.0 * FastMath.log(i / S) / (volatility * FastMath.sqrt(t));

        return FastMath.exp(lambda * t) * FastMath.pow(S, gamma) * (ERF.cdf(d) - (FastMath.pow(i / S, kappa) * ERF.cdf(temp)));
    }

    private double ksi(double S, double T, double gamma, double H, double I2, double I1, double t1, double r, double b) {
        double e1 = calcE(S, I1, b, gamma, t1, t1);
        double e2 = calcE(I2 * I2, S * I1, b, gamma, t1, t1);
        double e3 = calcE(S, I1, b, gamma, -t1, t1);
        double e4 = calcE(I2 * I2, S * I1, b, gamma, -t1, t1);

        double f1 = calcE(S, H, b, gamma, T, T);
        double f2 = calcE(I2 * I2, S * H, b, gamma, T, T);
        double f3 = calcE(I1 * I1, S * H, b, gamma, T, T);
        double f4 = calcE(S * I1 * I1, H * I2 * I2, b, gamma, T, T);

        double rho = FastMath.sqrt(t1 / T);
        double lambda = calcLambda(r, b, gamma);
        double kappa = calcKappa(b, gamma);

        return FastMath.exp(lambda * T) * FastMath.pow(S, gamma) * (
                BivariateNormal.cdf(-e1, -f1, rho)
                        - FastMath.pow(I2 / S, kappa) * BivariateNormal.cdf(-e2, -f2, rho)
                        - FastMath.pow(I1 / S, kappa) * BivariateNormal.cdf(-e3, -f3, -rho)
                        + FastMath.pow(I1 / I2, kappa) * BivariateNormal.cdf(-e4, -f4, -rho)
        );
    }

    private double calcE(double S, double I, double b, double gamma, double t1, double T) {
        double temp = FastMath.log(S / I) + t1 * (b + (gamma - 0.5) * volatilitySq);
        return temp / (volatility * FastMath.sqrt(T));
    }

    private double calcBeta(double b, double r) {
        double x = (b / volatilitySq) - 0.5;
        double temp2 = r / volatilitySq;
        double sqrtTemp = FastMath.sqrt((x * x) + 2.0 * temp2);

        return -x + sqrtTemp;
    }

    private double calcHt(double b, double X, double time, double bInfinity, double b0) {
        double temp1 = -(b * time + (2.0 * volatility * FastMath.sqrt(time)));
        double temp2 = (X * X) / ((bInfinity - b0) * b0);
        return temp1 * temp2;
    }

    private double calcI(double b0, double bInfinity, double h) {
        double temp = (bInfinity - b0) * (1.0 - FastMath.exp(h));
        return b0 + temp;
    }

    private double calcAlpha(double X, double I, double beta) {
        return (I - X) * FastMath.pow(I, -beta);
    }

    private double calcLambda(double r, double b, double gamma) {
        return -r + (gamma * b) + (0.5 * gamma * (gamma - 1.0) * volatilitySq);
    }

    private double calcKappa(double b, double gamma) {
        return 2.0 * (b / volatilitySq) + (2.0 * gamma - 1.0);
    }

    private double calcD1(double S, double h, double b, double gamma, double time) {
        double logTemp = FastMath.log(S / h);

        double temp = (b + (gamma - 0.5) * volatilitySq) * time;
        return (logTemp + temp) / (volatility * FastMath.sqrt(time));
    }

    @Override
    public BjerksundStensland withVolatility(double newVolatility) {
        return new BjerksundStensland(contract, frame, newVolatility, dayCountConvention);
    }

    @Override
    public BjerksundStensland withRiskFreeRate(double newRate) {
        return new BjerksundStensland(
                contract, frame.withRiskFreeRate(newRate), volatility, dayCountConvention
        );
    }

    @Override
    public BjerksundStensland withSpot(double newSpot) {
        return new BjerksundStensland(
                contract, frame.withSpotPrice(newSpot), volatility, dayCountConvention
        );
    }

    @Override
    public BjerksundStensland withTimestamp(long newTimestampNanos) {
        return new BjerksundStensland(
                contract,
                frame.withTimestampNanos(newTimestampNanos),
                volatility,
                dayCountConvention
        );
    }

    @Override
    protected double spotPrice() {
        return spotPrice;
    }

    @Override
    protected double volatility() {
        return volatility;
    }

    @Override
    protected double riskFreeRate() {
        return riskFreeRate;
    }

    @Override
    protected long valuationTimestampNanos() {
        return frame.timestampNanos();
    }

    @Override
    protected long expirationTimestampNanos() {
        return EpochNanos.from(contract.expirationDate());
    }

    @Override
    protected double spotBump(double relativeBump) {
        return Math.max(
                spotPrice * relativeBump,
                MINIMUM_ABSOLUTE_BUMP
        );
    }

    @Override
    public DayCountConvention dayCountConvention() {
        return dayCountConvention;
    }

}
