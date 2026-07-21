package com.thegreeklab.finance.model.american.binomial;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.exception.*;
import com.thegreeklab.finance.frame.MarketData;
import com.thegreeklab.finance.model.greeks.BumpableOptionModel;
import com.thegreeklab.finance.model.greeks.Greeks;
import com.thegreeklab.finance.time.DayCountConvention;
import com.thegreeklab.math.volatility.VolatilityPricer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.jafama.FastMath;

import java.util.Objects;

import static com.thegreeklab.finance.validation.PricingValidation.requireAmericanStyle;
import static com.thegreeklab.finance.validation.PricingValidation.requireValidSteps;

/**
 * Shared base class for American-option binomial trees.
 *
 * <p>The model stores the option contract, market-data snapshot and common
 * finite-difference helpers used by concrete tree parameterizations. Pricing
 * rolls the tree backwards with early exercise at each node, so only
 * {@link Option#AMERICAN} contracts are accepted.
 */
public abstract sealed class BinomialModel implements Greeks, BumpableOptionModel, VolatilityPricer permits LeisenReimer, CoxRossRubenstein {

    /** Option payoff direction. */
    protected final OptionType type;
    /** Current underlying price. */
    protected final double s;
    /** Option strike price. */
    protected final double k;
    /** Continuously compounded risk-free rate. */
    protected final double r;
    /** Generalized cost-of-carry parameter. */
    protected final double costOfCarry;
    /** Length of one tree step in years. */
    protected final double dt;
    /** Number of time steps in the tree. */
    protected final int steps;
    /** Immutable option contract being priced. */
    protected final OptionContract contract;
    /** Immutable market-data snapshot used by this tree. */
    protected final MarketData frame;
    /** Convention used to convert elapsed time into a year fraction. */
    protected final DayCountConvention dayCountConvention;
    /** Numerical floor used by finite-difference calculations. */
    protected static final double EPSILON = 1e-6;

    /** Relative one-percent bump used for volatility and price sensitivities. */
    protected static final double ONE_PERCENT_BUMP = 0.01;
    /** One-basis-point rate bump. */
    protected static final double ONE_BP_BUMP = 0.0001;
    /** One day expressed in nanoseconds. */
    protected static final long ONE_DAY_NANOS = 86_400_000_000_000L;
    /** Smallest absolute price or strike bump used by finite differences. */
    protected static final double MIN_ABSOLUTE_BUMP = 1e-4;
    /** Smallest time fraction for which a full-day theta bump is used. */
    protected static final double MIN_TIME_FRACTION = 1.0 / 365.0;
    /** Time to expiry, in years, at the market-data timestamp. */
    protected final double tNow;

    /** Per-step upward price multiplier. */
    protected double u;
    /** Per-step downward price multiplier. */
    protected double d;
    /** Risk-neutral probability of an upward move. */
    protected double p;

    /**
     * Initializes shared tree inputs and validates the contract style.
     *
     * @param contract American option contract being priced
     * @param frame    market data snapshot supplying spot, rate and cost of carry
     * @param steps    positive tree depth; higher values usually improve accuracy
     *                 at the cost of runtime
     * @param dayCountConvention convention used to derive the year fraction
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Fail-fast validation protects immutable tree invariants before any instance escapes.")
    public BinomialModel(
            OptionContract contract,
            MarketData frame,
            int steps,
            DayCountConvention dayCountConvention
    ) {
        Objects.requireNonNull(contract, "Contract cannot be null.");
        Objects.requireNonNull(frame, "Market data frame cannot be null.");
        Objects.requireNonNull(dayCountConvention, "Day-count convention cannot be null.");
        requireValidSteps(steps);
        requireAmericanStyle(contract);

        this.type = contract.type();
        this.steps = steps;
        this.s = frame.spotPrice();
        this.k = contract.strikePrice();
        this.r = frame.riskFreeRate();
        this.costOfCarry = frame.costOfCarry();
        double timeToExpiry = dayCountConvention.timeToExpiry(
                frame.timestampNanos(),
                contract.expirationDate()
        );
        if (timeToExpiry <= 0) {
            throw new ExpiredContractException("Cannot construct tree for expired or non-positive TTE contract.");
        }
        this.dt = timeToExpiry / steps;
        this.contract = contract;
        this.frame = frame;
        this.dayCountConvention = dayCountConvention;
        this.tNow = timeToExpiry;
    }


    private double calculatePayoff(double spotNode) {
        return (type == OptionType.CALL)
                ? Math.max(spotNode - k, 0.0)
                : Math.max(k - spotNode, 0.0);
    }

    private double volatilityBumpDerivative(java.util.function.ToDoubleFunction<BinomialModel> metric) {
        double currentVol = this.getVolatility();
        double volDown = FastMath.max(currentVol - ONE_PERCENT_BUMP, EPSILON);
        double totalBump = ONE_PERCENT_BUMP + (currentVol - volDown);
        double up = metric.applyAsDouble(withVolatility(currentVol + ONE_PERCENT_BUMP));
        double down = metric.applyAsDouble(withVolatility(volDown));
        return (up - down) / totalBump;
    }

    /**
     * Estimates theta from cached values at the root and the middle step-two node.
     *
     * @return annualized theta estimate based on two tree time steps
     * @throws InvalidStepCountException if the tree contains fewer than three steps
     */
    protected final double fastThetaViaNodeCache() {
        requireMinSteps(3, "Theta");
        ensureCalculated();

        double price = cache.price();
        double optValue = cache.nodesStep2()[1];
        return (optValue - price) / (2.0 * dt);
    }

    private double rateBumpDerivative(java.util.function.ToDoubleFunction<BinomialModel> metric) {
        double valueUp = metric.applyAsDouble(withRiskFreeRate(this.r + ONE_BP_BUMP));
        double valueDown = metric.applyAsDouble(withRiskFreeRate(this.r - ONE_BP_BUMP));
        return (valueUp - valueDown) / (2.0 * ONE_BP_BUMP);
    }

    private double timeBumpDerivative(java.util.function.ToDoubleFunction<BinomialModel> metric) {
        long bumpNanos = (tNow > MIN_TIME_FRACTION) ? ONE_DAY_NANOS : ONE_DAY_NANOS / 2;
        double tBumped = dayCountConvention.timeToExpiry(
                this.frame.timestampNanos() + bumpNanos,
                this.contract.expirationDate()
        );
        double dtYears = tNow - tBumped;

        if (dtYears <= 0) {
            throw new ExpiredContractException("Cannot compute time-based Greek: contract too close to or at expiry.");
        }

        double valueNow = metric.applyAsDouble(this);
        double valueBumped = metric.applyAsDouble(withTimestamp(this.frame.timestampNanos() + bumpNanos));
        return (valueBumped - valueNow) / dtYears;
    }

    @Override
    public final DayCountConvention dayCountConvention() {
        return dayCountConvention;
    }

    /**
     * Calculates the underlying price at a tree node.
     *
     * @param step    zero-based time step
     * @param upMoves number of upward moves leading to the node
     * @return underlying price at the requested node
     */
    protected abstract double calculateSpotNode(int step, int upMoves);

    private record TreeCache(double price, double[] nodesStep1, double[] nodesStep2) {
    }

    private volatile TreeCache cache;

    /**
     * @return option value at the root node
     */
    @Override
    public double price() {
        ensureCalculated();
        return cache.price();
    }

    /**
     * Rolls the tree backwards from expiration down to a target step.
     *
     * @param targetStep step where backward induction should stop; usually 0
     *                   for price, or 1/2 for finite-difference Greeks
     * @return option values at the requested step
     * @throws InvalidTargetStepException if {@code targetStep} is outside the tree
     */
    public double[] price(int targetStep) {
        if (targetStep < 0 || targetStep >= steps) {
            throw new InvalidTargetStepException("Invalid target step.");
        }

        boolean shouldCache = (targetStep == 0) && (cache == null);
        double[] optionValues = new double[steps + 1];
        double expRdT = FastMath.exp(-r * dt);

        double[] nodesStep1 = null;
        double[] nodesStep2 = null;

        for (int i = 0; i <= steps; i++) {
            double spotNode = calculateSpotNode(steps, i);
            optionValues[i] = calculatePayoff(spotNode);
        }

        for (int j = steps - 1; j >= targetStep; j--) {
            for (int i = 0; i <= j; i++) {
                double spotNode = calculateSpotNode(j, i);
                double cVal = expRdT * (p * optionValues[i + 1] + (1.0 - p) * optionValues[i]);
                double exVal = calculatePayoff(spotNode);
                optionValues[i] = Math.max(exVal, cVal);
            }

            if (shouldCache) {
                if (j == 2) {
                    nodesStep2 = new double[]{optionValues[0], optionValues[1], optionValues[2]};
                } else if (j == 1) {
                    nodesStep1 = new double[]{optionValues[0], optionValues[1]};
                }
            }
        }

        if (shouldCache) {
            this.cache = new TreeCache(optionValues[0], nodesStep1, nodesStep2);
        }

        return optionValues;
    }

    private void ensureCalculated() {
        if (cache == null) {
            price(0);
        }
    }

    private void requireMinSteps(int minSteps, String greekName) {
        if (steps < minSteps) {
            throw new InvalidStepCountException(
                    greekName + " requires at least " + minSteps + " steps, got " + steps + ".");
        }
    }

    @Override
    public double delta() {
        requireMinSteps(2, "Delta");
        ensureCalculated();

        double spotUp = calculateSpotNode(1, 1);
        double spotDown = calculateSpotNode(1, 0);
        return (cache.nodesStep1()[1] - cache.nodesStep1()[0]) / (spotUp - spotDown);
    }

    @Override
    public double gamma() {
        requireMinSteps(3, "Gamma");
        ensureCalculated();

        double[] v = cache.nodesStep2();
        double spotUp = calculateSpotNode(2, 2);
        double spotMid = calculateSpotNode(2, 1);
        double spotDown = calculateSpotNode(2, 0);

        double deltaUp = (v[2] - v[1]) / (spotUp - spotMid);
        double deltaDown = (v[1] - v[0]) / (spotMid - spotDown);
        return (deltaUp - deltaDown) / ((spotUp - spotDown) / 2.0);
    }

    @Override
    public double theta() {
        requireMinSteps(1, "Theta");
        return timeBumpDerivative(BinomialModel::price);
    }

    @Override
    public double vega() {
        requireMinSteps(1, "Vega");
        return volatilityBumpDerivative(BinomialModel::price);
    }

    @Override
    public double rho() {
        requireMinSteps(1, "Rho");
        return rateBumpDerivative(BinomialModel::price);
    }

    @Override
    public double vanna() {
        requireMinSteps(2, "Vanna");
        return volatilityBumpDerivative(BinomialModel::delta);
    }

    @Override
    public double volga() {
        requireMinSteps(1, "Volga");
        return volatilityBumpDerivative(BinomialModel::vega);
    }


    /**
     * Creates an equivalent tree with a replacement risk-free rate.
     *
     * @param newRate replacement continuously compounded rate
     * @return newly constructed tree with the requested rate
     */
    @Override
    public abstract BinomialModel withRiskFreeRate(double newRate);

    /**
     * Creates an equivalent tree with a replacement volatility.
     *
     * @param newVolatility replacement annualized volatility
     * @return newly constructed tree with the requested volatility
     */
    @Override
    public abstract BinomialModel withVolatility(double newVolatility);

    /**
     * Returns the annualized volatility used by this tree.
     *
     * @return annualized volatility as a decimal
     */
    protected abstract double getVolatility();

    /**
     * Creates an equivalent tree with a replacement underlying price.
     *
     * @param newSpot replacement underlying price
     * @return newly constructed tree with the requested price
     */
    @Override
    public abstract BinomialModel withSpot(double newSpot);

    /**
     * Creates an equivalent tree observed at another timestamp.
     *
     * @param newTimestampNanos replacement timestamp in nanoseconds since the UNIX epoch
     * @return newly constructed tree at the requested timestamp
     */
    @Override
    public abstract BinomialModel withTimestamp(long newTimestampNanos);

    @Override
    public double charm() {
        requireMinSteps(2, "Charm");
        return timeBumpDerivative(BinomialModel::delta);
    }

    @Override
    public double speed() {
        requireMinSteps(3, "Speed");

        double spotBump = FastMath.max(this.s * ONE_PERCENT_BUMP, MIN_ABSOLUTE_BUMP);

        if (this.s - spotBump <= 0) {
            throw new MathException("Spot price too low for stable Speed calculation.");
        }

        BinomialModel bumpedUp = withSpot(this.s + spotBump);
        BinomialModel bumpedDown = withSpot(this.s - spotBump);
        return (bumpedUp.gamma() - bumpedDown.gamma()) / (2.0 * spotBump);
    }

    @Override
    public double vera() {
        requireMinSteps(1, "Vera");
        return rateBumpDerivative(BinomialModel::vega);
    }

    @Override
    public double zomma() {
        requireMinSteps(3, "Zomma");
        return volatilityBumpDerivative(BinomialModel::gamma);
    }

    @Override
    public double ultima() {
        requireMinSteps(1, "Ultima");
        return volatilityBumpDerivative(BinomialModel::volga);
    }

    @Override
    public double color() {
        requireMinSteps(3, "Color");
        return timeBumpDerivative(BinomialModel::gamma);
    }

    @Override
    public double dualGamma() {
        requireMinSteps(1, "DualGamma");
        double strikeBump = FastMath.max(this.k * ONE_PERCENT_BUMP, MIN_ABSOLUTE_BUMP);

        if (this.k - strikeBump <= 0) {
            throw new MathException("Strike too low for stable DualGamma calculation.");
        }

        double priceUp = withStrike(this.k + strikeBump).price();
        double priceMid = this.price();
        double priceDown = withStrike(this.k - strikeBump).price();
        return (priceUp - 2.0 * priceMid + priceDown) / (strikeBump * strikeBump);
    }

    @Override
    public double dualDelta() {
        requireMinSteps(1, "DualDelta");
        double strikeBump = FastMath.max(this.k * ONE_PERCENT_BUMP, MIN_ABSOLUTE_BUMP);

        if (this.k - strikeBump <= 0) {
            throw new MathException("Strike too low for stable DualDelta calculation.");
        }

        BinomialModel bumpedUp = withStrike(this.k + strikeBump);
        BinomialModel bumpedDown = withStrike(this.k - strikeBump);
        return (bumpedUp.price() - bumpedDown.price()) / (2.0 * strikeBump);
    }

    @Override
    public double lambda() {
        requireMinSteps(2, "Lambda");
        double currentPrice = this.price();
        if (currentPrice < EPSILON) {
            throw new MathException("Lambda undefined: option price too close to zero.");
        }
        return this.delta() * this.s / currentPrice;
    }

    /**
     * Creates an equivalent tree with a replacement strike.
     *
     * @param newStrike replacement strike price
     * @return newly constructed tree with the requested strike
     */
    protected abstract BinomialModel withStrike(double newStrike);

    @Override
    public double priceAtVolatility(double volatility) {
        return withVolatility(volatility).price();
    }
}
