package com.thegreeklab.finance.model.european;

import com.thegreeklab.finance.exception.InvalidVolatilityException;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.exception.UnsupportedExerciseStyleException;
import com.thegreeklab.finance.frame.MarketData;
import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.model.greeks.Greeks;
import com.thegreeklab.finance.validation.PricingValidation;
import com.thegreeklab.math.ERF;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.jafama.FastMath;

import java.util.Objects;

/**
 * Generalized Black-Scholes-Merton pricing engine.
 *
 * <p>This class implements the generalized form of the Black-Scholes-Merton model
 * described by Haug, using the cost-of-carry parameter {@code b} to unify the
 * equity, futures (Black-76) and FX (Garman-Kohlhagen) variants of the model
 * under a single set of formulas:
 * <ul>
 *     <li>{@code b = r}       &rarr; standard Black-Scholes (non-dividend-paying stock)</li>
 *     <li>{@code b = r - q}   &rarr; Merton model (stock with continuous dividend yield {@code q})</li>
 *     <li>{@code b = 0}       &rarr; Black-76 model (options on futures)</li>
 *     <li>{@code b = r - rf}  &rarr; Garman-Kohlhagen model (FX options, {@code rf} = foreign rate)</li>
 * </ul>
 * The specific value of {@code b} is supplied by the {@link MarketData} implementation
 * passed to the constructor (see {@link MarketData#costOfCarry()}), which is why
 * {@link Black76} and {@link GarmanKohlhagen} are thin subclasses that simply
 * delegate to this constructor with a more specific {@link MarketData} type.
 *
 * <p>All Greeks are computed analytically and cached at construction time, since
 * {@code d1}, {@code d2} and the associated CDF/PDF values are shared across
 * multiple Greek calculations. This class is immutable and therefore safe to
 * share across threads once constructed.
 *
 * <p>Sign conventions, naming and formulas for the higher-order Greeks follow
 * Espen Gaarder Haug, <i>The Complete Guide to Option Pricing Formulas</i>,
 * 2nd Edition.
 *
 * @see Black76
 * @see GarmanKohlhagen
 * @see BlackScholesMerton
 */
@SuppressWarnings("deprecation")
public sealed abstract class BlackScholes implements Greeks permits BlackScholesMerton, BSInternal, Black76, GarmanKohlhagen {
    private final double stockPrice;
    private final double strikePrice;
    private final double riskFreeRate;
    private final double volatility;
    private final double b;
    private final double rawTimeToExpiry;
    private final double calculationTimeToExpiry;
    private final boolean expired;
    private final OptionType type;

    private final double sqrtT;   // sqrt(time)
    private final double volSq;   // sigma^2

    private final double d1;
    private final double d2;
    private final double df;
    private final double qf;
    private final double cdfD1;
    private final double cdfD2;
    private final double pdfD1;
    private final double pdfD2;
    private final double invVolSqrtT;

    /**
     * Constructs a priced option instance, eagerly computing {@code d1}, {@code d2}
     * and their associated standard normal CDF/PDF values for reuse across all
     * Greek calculations.
     *
     * @param contract   the option contract (strike, expiry, type) being priced
     * @param frame      the market data snapshot supplying spot price, risk-free
     *                   rate and cost-of-carry {@code b}
     * @param volatility annualized volatility as a decimal (e.g. {@code 0.20} for 20%);
     *                   must be finite and strictly above {@code 1e-6}
     * @throws NullPointerException              if {@code contract} or {@code frame} is {@code null}
     * @throws InvalidVolatilityException        if {@code volatility} is below {@code 1e-6}
     *                                           or is not finite
     * @throws UnsupportedExerciseStyleException if {@code contract.option()} is not
     *                                           {@link Option#EUROPEAN}
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Fail-fast validation protects immutable model invariants before any instance escapes.")
    public BlackScholes(OptionContract contract, MarketData frame, double volatility) {
        Objects.requireNonNull(contract, "Contract cannot be null.");
        Objects.requireNonNull(frame, "Market data frame cannot be null.");
        PricingValidation.requireValidVolatility(volatility);
        validateEuropeanContract(contract);

        this.b = frame.costOfCarry();
        this.stockPrice = frame.spotPrice();
        this.strikePrice = contract.strikePrice();
        this.riskFreeRate = frame.riskFreeRate();
        this.volatility = volatility;
        this.type = contract.type();

        this.volSq = volatility * volatility;

        this.rawTimeToExpiry = contract.getTimeToExpiry(frame.timestampNanos());
        this.expired = this.rawTimeToExpiry <= 0.0;
        this.calculationTimeToExpiry = FastMath.max(
                this.rawTimeToExpiry,
                1.0 / contract.secondsInExpirationYear()
        );

        if (this.expired) {
            this.sqrtT = FastMath.sqrt(this.calculationTimeToExpiry);
            this.invVolSqrtT = 1.0 / (volatility * this.sqrtT);
            this.d1 = 0.0;
            this.d2 = 0.0;
            this.df = 1.0;
            this.qf = 1.0;

            this.cdfD1 = (stockPrice > strikePrice) ? 1.0 : (stockPrice < strikePrice) ? 0.0 : 0.5;
            this.cdfD2 = this.cdfD1;
            this.pdfD1 = 0.0;
            this.pdfD2 = 0.0;
        } else {
            this.sqrtT = FastMath.sqrt(calculationTimeToExpiry);
            this.invVolSqrtT = 1.0 / (volatility * this.sqrtT);
            this.d1 = computeD1(stockPrice, strikePrice, calculationTimeToExpiry, b, volSq, this.invVolSqrtT);
            this.d2 = this.d1 - volatility * this.sqrtT;

            this.df = FastMath.exp(-riskFreeRate * calculationTimeToExpiry);
            this.qf = FastMath.exp((b - riskFreeRate) * calculationTimeToExpiry);

            this.cdfD1 = ERF.cdf(d1);
            this.cdfD2 = ERF.cdf(d2);
            this.pdfD1 = ERF.pdf(d1);
            this.pdfD2 = ERF.pdf(d2);
        }
    }

    /**
     * Fast pricing path for callers that only need the option value.
     *
     * <p>This method duplicates the core pricing arithmetic from the constructor
     * intentionally: implied-volatility solvers and calibration loops can call it
     * many times, and avoiding short-lived model objects reduces allocation pressure.
     * It still applies the same input validation as the full model constructor.
     *
     * @param contract   the European option contract (strike, expiry, type) being priced
     * @param frame      the market data snapshot supplying spot price, risk-free
     *                   rate and cost-of-carry {@code b}
     * @param volatility annualized volatility as a decimal; must be finite and
     *                   strictly above {@code 1e-6}
     * @return the theoretical fair value of the option, or its intrinsic value if
     * {@code contract} has already expired
     * @throws NullPointerException              if {@code contract} or {@code frame} is {@code null}
     * @throws InvalidVolatilityException        if {@code volatility} is below {@code 1e-6}
     *                                           or is not finite
     * @throws UnsupportedExerciseStyleException if {@code contract.option()} is not
     *                                           {@link Option#EUROPEAN}
     */
    public static double price(OptionContract contract, MarketData frame, double volatility) {
        Objects.requireNonNull(contract, "Contract cannot be null.");
        Objects.requireNonNull(frame, "Market data frame cannot be null.");
        PricingValidation.requireValidVolatility(volatility);
        validateEuropeanContract(contract);

        double t = contract.getTimeToExpiry(frame.timestampNanos());
        return computePrice(
                contract.type(),
                frame.spotPrice(),
                contract.strikePrice(),
                t,
                frame.riskFreeRate(),
                frame.costOfCarry(),
                volatility
        );
    }

    /**
     * Prices a European call directly from generalized Black-Scholes inputs.
     *
     * <p>This overload is intended for pricing algorithms which already operate
     * on transformed model parameters and therefore cannot faithfully reconstruct
     * an asset-specific {@link MarketData} instance. One example is the put-call
     * transformation used by the Bjerksund-Stensland approximation.</p>
     *
     * @param spotPrice    current underlying or futures price
     * @param strikePrice  option strike price
     * @param timeToExpiry time to expiry in years; must not be negative
     * @param riskFreeRate continuously compounded discount rate
     * @param costOfCarry  generalized cost-of-carry parameter {@code b}
     * @param volatility   annualized volatility as a decimal
     * @return generalized European call value, or intrinsic value at expiry
     */
    public static double callPrice(
            double spotPrice,
            double strikePrice,
            double timeToExpiry,
            double riskFreeRate,
            double costOfCarry,
            double volatility
    ) {
        validateFormulaInputs(
                spotPrice,
                strikePrice,
                timeToExpiry,
                riskFreeRate,
                costOfCarry,
                volatility
        );
        return computePrice(
                OptionType.CALL,
                spotPrice,
                strikePrice,
                timeToExpiry,
                riskFreeRate,
                costOfCarry,
                volatility
        );
    }

    private static double computePrice(
            OptionType type,
            double s,
            double k,
            double t,
            double r,
            double b,
            double volatility
    ) {
        Objects.requireNonNull(type, "Option type cannot be null.");

        if (t <= 0.0) {
            return switch (type) {
                case CALL -> FastMath.max(s - k, 0.0);
                case PUT -> FastMath.max(k - s, 0.0);
            };
        }

        double volSq = volatility * volatility;
        double sqrtT = FastMath.sqrt(t);
        double invVolSqrtT = 1.0 / (volatility * sqrtT);

        double d1 = computeD1(s, k, t, b, volSq, invVolSqrtT);
        double d2 = d1 - volatility * sqrtT;

        double df = FastMath.exp(-r * t);
        double qf = FastMath.exp((b - r) * t);
        double cdfD1 = ERF.cdf(d1);
        double cdfD2 = ERF.cdf(d2);

        return switch (type) {
            case CALL -> computeCallPrice(s, k, df, qf, cdfD1, cdfD2);
            case PUT -> computePutPrice(s, k, df, qf, cdfD1, cdfD2);
        };
    }

    private static void validateFormulaInputs(
            double spotPrice,
            double strikePrice,
            double timeToExpiry,
            double riskFreeRate,
            double costOfCarry,
            double volatility
    ) {
        if (!(spotPrice > 0.0) || !Double.isFinite(spotPrice)) {
            throw new IllegalArgumentException("Spot price must be strictly positive and finite.");
        }
        if (!(strikePrice > 0.0) || !Double.isFinite(strikePrice)) {
            throw new IllegalArgumentException("Strike price must be strictly positive and finite.");
        }
        if (timeToExpiry < 0.0 || !Double.isFinite(timeToExpiry)) {
            throw new IllegalArgumentException("Time to expiry must be non-negative and finite.");
        }
        if (!Double.isFinite(riskFreeRate) || !Double.isFinite(costOfCarry)) {
            throw new IllegalArgumentException("Risk-free rate and cost of carry must be finite.");
        }
        PricingValidation.requireValidVolatility(volatility);
    }

    /**
     * @return the actual time to expiry in years; {@code 0.0} if this instance
     * is at or after expiration
     */
    public double timeToExpiry() {
        return rawTimeToExpiry;
    }

    /**
     * Epsilon (a.k.a. "psi" in some texts) — the sensitivity of the option price
     * to a proportional change in the continuous dividend/foreign-rate component
     * embedded in the cost-of-carry term. Computed per unit (not per percentage point).
     *
     * @return the epsilon Greek for this option
     */
    public double epsilon() {
        if (expired) {
            return 0.0;
        }
        return switch (type) {
            case CALL -> -stockPrice * calculationTimeToExpiry * qf * cdfD1;
            case PUT -> stockPrice * calculationTimeToExpiry * qf * (1 - cdfD1);
        };
    }


    private double call() {
        return computeCallPrice(stockPrice, strikePrice, df, qf, cdfD1, cdfD2);
    }

    private double put() {
        return computePutPrice(stockPrice, strikePrice, df, qf, cdfD1, cdfD2);
    }

    /**
     * Computes {@code d1} from the generalized Black-Scholes-Merton formula:
     * {@code d1 = [ln(S/K) + T(b + sigma^2/2)] / (sigma * sqrt(T))}.
     *
     * @param s           spot price
     * @param k           strike price
     * @param t           time to expiry in years
     * @param b           cost-of-carry rate
     * @param volSq       variance ({@code sigma^2})
     * @param invVolSqrtT precomputed {@code 1 / (sigma * sqrt(T))} for performance
     * @return the value of {@code d1}
     */
    private static double computeD1(double s, double k, double t, double b, double volSq, double invVolSqrtT) {
        return (FastMath.log(s / k) + t * (b + (volSq * 0.5))) * invVolSqrtT;
    }

    /**
     * Generalized call price: {@code C = S * e^((b-r)T) * N(d1) - K * e^(-rT) * N(d2)}.
     *
     * @param s     spot price
     * @param k     strike price
     * @param df    discount factor {@code e^(-rT)}
     * @param qf    carry factor {@code e^((b-r)T)}
     * @param cdfD1 {@code N(d1)}
     * @param cdfD2 {@code N(d2)}
     * @return the theoretical call price
     */
    private static double computeCallPrice(double s, double k, double df, double qf, double cdfD1, double cdfD2) {
        return s * qf * cdfD1 - k * df * cdfD2;
    }

    /**
     * Generalized put price, derived from put-call parity applied to
     * {@link #computeCallPrice}: {@code P = C - S * e^((b-r)T) + K * e^(-rT)}.
     *
     * @param s     spot price
     * @param k     strike price
     * @param df    discount factor {@code e^(-rT)}
     * @param qf    carry factor {@code e^((b-r)T)}
     * @param cdfD1 {@code N(d1)}
     * @param cdfD2 {@code N(d2)}
     * @return the theoretical put price
     */
    private static double computePutPrice(double s, double k, double df, double qf, double cdfD1, double cdfD2) {
        return computeCallPrice(s, k, df, qf, cdfD1, cdfD2) - s * qf + k * df;
    }


    private static void validateEuropeanContract(OptionContract contract) {
        if (contract.option() != Option.EUROPEAN) {
            throw new UnsupportedExerciseStyleException(
                    "Black-Scholes models only support EUROPEAN option style. Received: " + contract.option()
            );
        }
    }

    /**
     * @return delta: the first derivative of price with respect to the underlying
     * spot price. Dispatches to call or put delta based on contract type.
     */
    public double delta() {
        if (expired) {
            return expiredDelta();
        }
        return switch (type) {
            case CALL -> deltaCall();
            case PUT -> deltaPut();
        };
    }

    private double expiredDelta() {
        return switch (type) {
            case CALL -> stockPrice > strikePrice ? 1.0 : stockPrice < strikePrice ? 0.0 : 0.5;
            case PUT -> stockPrice < strikePrice ? -1.0 : stockPrice > strikePrice ? 0.0 : -0.5;
        };
    }

    /**
     * @return the theoretical fair value of the option, dispatching to the
     * call or put pricing formula based on contract type.
     */
    public double price() {
        return switch (type) {
            case CALL -> call();
            case PUT -> put();
        };
    }

    /**
     * @return rho: the first derivative of price with respect to the risk-free
     * interest rate.
     */
    public double rho() {
        if (expired) {
            return 0.0;
        }
        return (type == OptionType.CALL) ? rhoCall() : rhoPut();
    }

    /**
     * @return theta expressed on an annualized basis. Equivalent to {@code theta(false)}.
     */
    public double theta() {
        return theta(false);
    }

    /**
     * Theta: the first derivative of price with respect to the passage of time
     * (time decay).
     *
     * @param daily if {@code true}, converts the annualized theta to a per-calendar-day
     *              figure by dividing by 365; if {@code false}, returns the annualized value
     * @return theta, annualized or daily depending on {@code daily}
     */
    public double theta(boolean daily) {
        if (expired) {
            return 0.0;
        }
        double yearlyTheta = (type == OptionType.CALL) ? thetaCall() : thetaPut();
        return daily ? yearlyTheta / 365.0 : yearlyTheta;
    }

    /**
     * @return charm (a.k.a. delta decay): the rate of change of delta with
     * respect to the passage of time.
     */
    public double charm() {
        if (expired) {
            return 0.0;
        }
        return (type == OptionType.CALL) ? charmCall() : charmPut();
    }

    /**
     * @return lambda (a.k.a. omega or elasticity): the percentage change in
     * option value per percentage change in the underlying, i.e.
     * {@code delta * (S / price)}. Returns {@link Double#NaN} if the
     * option price is numerically negligible, to avoid division by
     * a value close to zero.
     */
    public double lambda() {
        return (type == OptionType.CALL) ? lambdaCall() : lambdaPut();
    }

    /**
     * @return dual delta: the first derivative of price with respect to the
     * strike price, used e.g. to back out risk-neutral digital/CDF
     * values from a strike continuum.
     */
    public double dualDelta() {
        if (expired) {
            return 0.0;
        }
        return (type == OptionType.CALL) ? dualDeltaCall() : dualDeltaPut();
    }

    /**
     * @return the "parmicharma" Greek (third-order time/carry cross-sensitivity
     * building on {@link #charm()}); dispatches to the call or put variant
     * depending on contract type.
     */
    public double parmicharma() {
        if (expired) {
            return 0.0;
        }
        return (type == OptionType.CALL) ? parmicharmaCall() : parmicharmaPut();
    }

    private double lambdaCall() {
        double c = call();
        return (c < 1e-10) ? Double.NaN : deltaCall() * (stockPrice / c);
    }

    private double lambdaPut() {
        double p = put();
        return (p < 1e-10) ? Double.NaN : deltaPut() * (stockPrice / p);
    }

    private double deltaCall() {
        return qf * cdfD1;
    }

    private double deltaPut() {
        return qf * (cdfD1 - 1);
    }

    /**
     * @return gamma: the second derivative of price with respect to spot price
     * (the rate of change of delta). Identical formula for calls and puts.
     */
    public double gamma() {
        if (expired) {
            return 0.0;
        }
        return (qf * pdfD1 * invVolSqrtT) / stockPrice;
    }

    /**
     * @return vega expressed per unit of volatility (i.e. per 1.00, not per
     * percentage point). Equivalent to {@code vega(false)}.
     */
    public double vega() {
        return vega(false);
    }

    /**
     * Vega: the first derivative of price with respect to volatility.
     * Identical formula for calls and puts.
     *
     * @param perPercentagePoint if {@code true}, scales the result by {@code 0.01}
     *                           so that it represents the price change for a
     *                           1 percentage-point move in volatility (e.g. 20% &rarr; 21%);
     *                           if {@code false}, returns vega per unit of volatility
     * @return vega, scaled according to {@code perPercentagePoint}
     */
    public double vega(boolean perPercentagePoint) {
        if (expired) {
            return 0.0;
        }
        double v = stockPrice * qf * sqrtT * pdfD1;
        return perPercentagePoint ? v * 0.01 : v;
    }

    private double rhoCall() {
        return strikePrice * calculationTimeToExpiry * df * cdfD2;
    }

    private double rhoPut() {
        return -strikePrice * calculationTimeToExpiry * df * (1 - cdfD2);
    }

    private double thetaCall() {
        return ((-qf * stockPrice * volatility * pdfD1) / (2 * sqrtT)) + ((riskFreeRate - b) * qf * stockPrice * cdfD1) - (riskFreeRate * strikePrice * df * cdfD2);
    }

    private double thetaPut() {
        return ((-qf * stockPrice * volatility * pdfD1) / (2 * sqrtT)) - ((riskFreeRate - b) * qf * stockPrice * (1 - cdfD1)) + (riskFreeRate * strikePrice * df * (1 - cdfD2));
    }

    /**
     * @return vanna: the second-order cross-sensitivity of price to spot price
     * and volatility (equivalently, the sensitivity of delta to volatility).
     */
    public double vanna() {
        if (expired) {
            return 0.0;
        }
        return -qf * pdfD1 * (d2 / volatility);
    }

    /**
     * @return volga (a.k.a. vomma): the second derivative of price with respect
     * to volatility (the sensitivity of vega to volatility).
     */
    public double volga() {
        if (expired) {
            return 0.0;
        }
        return vega() * ((d1 * d2) / volatility);
    }

    private double charmCall() {
        return (riskFreeRate - b) * qf * cdfD1 - (qf * pdfD1 * (b * invVolSqrtT - (d2 / (2 * calculationTimeToExpiry))));
    }

    private double charmPut() {
        return -(riskFreeRate - b) * qf * (1 - cdfD1) - (qf * pdfD1 * (b * invVolSqrtT - (d2 / (2 * calculationTimeToExpiry))));
    }

    /**
     * @return veta: the rate of change of vega with respect to the passage of time.
     */
    public double veta() {
        if (expired) {
            return 0.0;
        }
        return vega() * ((riskFreeRate - b) + (b * d1 * invVolSqrtT) - ((d1 * d2 + 1) / (2 * calculationTimeToExpiry)));
    }

    /**
     * @return vera (a.k.a. rhova): the rate of change of rho with respect to volatility.
     */
    public double vera() {
        if (expired) {
            return 0.0;
        }
        return -strikePrice * calculationTimeToExpiry * df * pdfD2 * (d1 / volatility);
    }

    /**
     * @return speed: the third derivative of price with respect to spot price
     * (the rate of change of gamma with respect to spot price).
     */
    public double speed() {
        if (expired) {
            return 0.0;
        }
        return -(gamma() / stockPrice) * (d1 * invVolSqrtT + 1);
    }

    /**
     * @return zomma: the rate of change of gamma with respect to volatility.
     */
    public double zomma() {
        if (expired) {
            return 0.0;
        }
        return gamma() * ((d1 * d2 - 1) / volatility);
    }

    /**
     * @return color (a.k.a. gamma decay): the rate of change of gamma with
     * respect to the passage of time.
     */

    public double color() {
        if (expired) {
            return 0.0;
        }
        return (-qf * (pdfD1 * invVolSqrtT) / (2 * stockPrice * calculationTimeToExpiry)) * (2 * (riskFreeRate - b) * calculationTimeToExpiry + 1 + d1 * (2 * b * calculationTimeToExpiry * invVolSqrtT - d2));
    }

    /**
     * @return ultima: the third derivative of price with respect to volatility
     * (the rate of change of volga with respect to volatility).
     */
    public double ultima() {
        if (expired) {
            return 0.0;
        }
        return ((-vega()) / (volSq)) * (d1 * d2 * (1 - d1 * d2) + d1 * d1 + d2 * d2);
    }

    private double parmicharmaCall() {
        double tau_der = ((b - (volSq / 2)) * invVolSqrtT) - (d2 / (2 * calculationTimeToExpiry));
        double first_bracket = (riskFreeRate - b) - (2 * b * calculationTimeToExpiry * invVolSqrtT - d2) / (2 * calculationTimeToExpiry);
        double second_nominator = (2 * d2 * volSq * calculationTimeToExpiry) - (b * volatility * sqrtT * calculationTimeToExpiry) - volSq * calculationTimeToExpiry * calculationTimeToExpiry * tau_der;
        double second_denominator = 2 * calculationTimeToExpiry * calculationTimeToExpiry * calculationTimeToExpiry * volSq;

        return first_bracket * charmCall() - (qf * pdfD1 * (second_nominator / second_denominator));
    }

    private double parmicharmaPut() {
        double rMinusB = riskFreeRate - b;
        return (rMinusB * rMinusB) * qf + parmicharmaCall();
    }

    private double dualDeltaCall() {
        return -df * cdfD2;
    }

    private double dualDeltaPut() {
        return df * (1 - cdfD2);
    }

    /**
     * @return dual gamma: the second derivative of price with respect to the
     * strike price.
     */
    public double dualGamma() {
        if (expired) {
            return 0.0;
        }
        return df * (pdfD2 * invVolSqrtT) / strikePrice;
    }


}
