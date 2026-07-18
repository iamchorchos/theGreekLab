package com.thegreeklab.finance.model.american.trinomial;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.exception.ExpiredContractException;
import com.thegreeklab.finance.exception.InvalidStepCountException;
import com.thegreeklab.finance.exception.InvalidVolatilityException;
import com.thegreeklab.finance.exception.MathException;
import com.thegreeklab.finance.exception.UnsupportedExerciseStyleException;
import com.thegreeklab.finance.frame.MarketData;
import com.thegreeklab.finance.model.greeks.BumpableOptionModel;
import com.thegreeklab.finance.time.DayCountConvention;
import net.jafama.FastMath;

import java.util.Objects;

import static com.thegreeklab.finance.validation.PricingValidation.*;

/**
 * Recombining trinomial tree for European and American vanilla options.
 *
 * <p>The tree uses three risk-neutral transitions per step and rolls option
 * values backwards with early-exercise checks for {@link Option#AMERICAN}
 * contracts. Price, delta, gamma and theta are obtained in one rollback.
 * Vega and rho use bump-and-revalue estimates with immutable model copies.</p>
 *
 * <p>Instances are immutable and safe for concurrent reads.</p>
 */
public final class TrinomialTree implements BumpableOptionModel {

    /** Maximum supported depth, bounded for safe allocation and practical quadratic runtime. */
    public static final int MAX_STEPS = 10_000;

    private static final double VOLATILITY_BUMP = 0.01;
    private static final double RATE_BUMP = 0.0001;

    private final double u;
    private final double pu;
    private final double pd;
    private final double pm;
    private final double dt;
    private final double vol;
    private final int z;
    private final int steps;
    private final OptionContract contract;
    private final MarketData frame;
    private final DayCountConvention dayCountConvention;
    private final TreeResult result;

    /**
     * Creates and evaluates a trinomial option-pricing tree.
     *
     * @param contract   European or American vanilla option contract
     * @param frame      immutable market-data snapshot
     * @param volatility annualized volatility as a decimal
     * @param steps      number of time steps in {@code [1, MAX_STEPS]}
     * @param dayCountConvention convention used to derive the year fraction
     * @throws NullPointerException              if {@code contract} or {@code frame} is {@code null}
     * @throws InvalidVolatilityException        if {@code volatility} is invalid
     * @throws InvalidStepCountException         if {@code steps} is invalid or too small for positive probabilities
     * @throws UnsupportedExerciseStyleException if the contract uses an exotic exercise style
     * @throws ExpiredContractException          if the contract has expired
     * @throws MathException                     if transition probabilities are invalid
     */
    public TrinomialTree(
            OptionContract contract,
            MarketData frame,
            double volatility,
            int steps,
            DayCountConvention dayCountConvention
    ) {
        Objects.requireNonNull(contract, "Contract cannot be null.");
        Objects.requireNonNull(frame, "Market data frame cannot be null.");
        Objects.requireNonNull(dayCountConvention, "Day-count convention cannot be null.");
        requireValidVolatility(volatility);
        requireValidSteps(steps);
        requireSupportedSteps(steps);
        requireNonExoticStyle(contract);

        double timeToExpiry = dayCountConvention.timeToExpiry(
                frame.timestampNanos(),
                contract.expirationDate()
        );
        if (timeToExpiry <= 0.0) {
            throw new ExpiredContractException("Cannot construct trinomial tree for an expired contract.");
        }
        this.dt = timeToExpiry / steps;

        int minimumSteps = minimumRequiredSteps(frame.costOfCarry(), timeToExpiry, volatility);
        if (minimumSteps > MAX_STEPS) {
            throw new InvalidStepCountException(
                    "Market inputs require " + minimumSteps
                            + " trinomial steps, exceeding the supported maximum of " + MAX_STEPS + "."
            );
        }
        if (steps < minimumSteps) {
            throw new InvalidStepCountException("Steps must be at least " + minimumSteps + " to keep trinomial probabilities positive.");
        }
        this.steps = steps;

        this.u = FastMath.exp(volatility * FastMath.sqrt(2.0 * dt));
        this.contract = contract;
        this.frame = frame;
        this.dayCountConvention = dayCountConvention;
        this.vol = volatility;
        this.pu = calcPUD(DType.U);
        this.pd = calcPUD(DType.D);
        this.pm = 1.0 - this.pu - this.pd;
        requireValidProbabilities();
        this.z = (contract.type() == OptionType.CALL) ? 1 : -1;
        this.result = calculateTree();
    }

    /** Immutable values produced by a single tree rollback. */
    private record TreeResult(double price, double delta, double gamma, double theta) {
    }

    /** Direction of a non-middle transition. */
    private enum DType {
        U, D
    }

    /**
     * Calculates the risk-neutral probability of an up or down transition.
     *
     * @param type transition direction
     * @return transition probability
     */
    private double calcPUD(DType type) {
        double temp = FastMath.exp(vol * FastMath.sqrt(dt / 2.0));
        double tempRev = 1.0 / temp;
        double tempB = FastMath.exp(this.frame.costOfCarry() * this.dt / 2.0);

        return switch (type) {
            case U -> FastMath.pow2((tempB - tempRev) / (temp - tempRev));
            case D -> FastMath.pow2((temp - tempB) / (temp - tempRev));
        };
    }

    /** Validates the probability domain and normalization invariant. */
    private void requireValidProbabilities() {
        if (isInvalidProbability(pu) || isInvalidProbability(pm) || isInvalidProbability(pd)) {
            throw new MathException(
                    "Trinomial probabilities must be finite and between 0 and 1. Received: "
                            + "pu=" + pu + ", pm=" + pm + ", pd=" + pd
            );
        }

        double probabilitySum = pu + pm + pd;
        if (FastMath.abs(probabilitySum - 1.0) > 1e-12) {
            throw new MathException(
                    "Trinomial probabilities must sum to 1. Received: " + probabilitySum
            );
        }
    }

    /** Returns whether a transition probability is non-finite or outside {@code [0, 1]}. */
    private static boolean isInvalidProbability(double probability) {
        return !Double.isFinite(probability) || probability < 0.0 || probability > 1.0;
    }

    /** Rejects depths that would make allocation or quadratic rollback impractical. */
    private static void requireSupportedSteps(int steps) {
        if (steps > MAX_STEPS) {
            throw new InvalidStepCountException(
                    "Steps must not exceed " + MAX_STEPS + ". Received: " + steps
            );
        }
    }

    /**
     * Calculates the minimum tree depth required by the probability-positivity condition.
     *
     * @param costOfCarry generalized cost of carry
     * @param timeToExpiry remaining time in years
     * @param volatility annualized volatility
     * @return minimum supported number of time steps
     * @throws InvalidStepCountException if the required depth exceeds the supported integer range
     */
    private static int minimumRequiredSteps(double costOfCarry, double timeToExpiry, double volatility) {
        double threshold = costOfCarry * costOfCarry * timeToExpiry
                / (2.0 * volatility * volatility);
        if (!Double.isFinite(threshold) || threshold >= Integer.MAX_VALUE) {
            throw new InvalidStepCountException("Required trinomial step count exceeds the supported range.");
        }
        return (int) threshold + 1;
    }

    /**
     * Rolls the tree backwards and derives node-based price sensitivities.
     *
     * @return immutable price, delta, gamma and theta result
     */
    private TreeResult calculateTree() {
        double[] values = new double[2 * steps + 1];
        double discountFactor = FastMath.exp(-frame.riskFreeRate() * dt);

        for (int i = 0; i <= (2 * steps); i++) {
            values[i] = FastMath.max(z * (frame.spotPrice() * FastMath.pow(u, (i - steps)) - contract.strikePrice()), 0.0);
        }

        double stepOneDown = values[0];
        double stepOneMiddle = values[1];
        double stepOneUp = values[2];

        for (int i = steps - 1; i >= 0; i--) {
            for (int j = 0; j <= (2 * i); j++) {
                double temp = discountFactor * ((pu * values[j + 2]) + (pm * values[j + 1]) + (pd * values[j]));
                if (contract.option() == Option.EUROPEAN) {
                    values[j] = temp;
                } else {
                    values[j] = FastMath.max(z * (frame.spotPrice() * FastMath.pow(u, (j - i)) - contract.strikePrice()), temp);
                }
            }

            if (i == 1) {
                stepOneDown = values[0];
                stepOneMiddle = values[1];
                stepOneUp = values[2];
            }
        }

        double spotDown = frame.spotPrice() / u;
        double spotMiddle = frame.spotPrice();
        double spotUp = frame.spotPrice() * u;

        double delta = (stepOneUp - stepOneDown) / (spotUp - spotDown);
        double deltaUp = (stepOneUp - stepOneMiddle) / (spotUp - spotMiddle);
        double deltaDown = (stepOneMiddle - stepOneDown) / (spotMiddle - spotDown);
        double gamma = (deltaUp - deltaDown) / ((spotUp - spotDown) / 2.0);
        double theta = (stepOneMiddle - values[0]) / dt;

        return new TreeResult(values[0], delta, gamma, theta);
    }

    /** {@inheritDoc} */
    @Override
    public double price() {
        return result.price();
    }

    /** {@inheritDoc} */
    @Override
    public double delta() {
        return result.delta();
    }

    /** {@inheritDoc} */
    @Override
    public double gamma() {
        return result.gamma();
    }

    /** {@inheritDoc} */
    @Override
    public double theta() {
        return result.theta();
    }

    /**
     * Estimates vega by bumping volatility by one percentage point.
     *
     * <p>A central difference is used where the lower bump remains in the
     * supported volatility domain; otherwise a forward difference is used.
     * Central valuations share a tree depth sufficient for both bumped
     * probability distributions.</p>
     *
     * @return price sensitivity per unit change in annualized volatility
     */
    @Override
    public double vega() {
        double volatilityUp = vol + VOLATILITY_BUMP;
        double volatilityDown = vol - VOLATILITY_BUMP;

        if (volatilityDown < MIN_VOLATILITY) {
            double priceUp = withVolatility(volatilityUp).price();
            return (priceUp - price()) / VOLATILITY_BUMP;
        }

        return centralDifference(
                frame, volatilityUp,
                frame, volatilityDown,
                VOLATILITY_BUMP
        );
    }

    /**
     * Estimates rho using a central one-basis-point rate bump.
     * Both valuations share a tree depth sufficient for their bumped
     * cost-of-carry values.
     *
     * @return price sensitivity per unit change in the risk-free rate
     */
    @Override
    public double rho() {
        double rate = frame.riskFreeRate();
        MarketData frameUp = frame.withRiskFreeRate(rate + RATE_BUMP);
        MarketData frameDown = frame.withRiskFreeRate(rate - RATE_BUMP);
        return centralDifference(frameUp, vol, frameDown, vol, RATE_BUMP);
    }

    /**
     * Calculates a central price derivative on a common valid tree depth.
     *
     * @param frameUp market data for the upper bump
     * @param volatilityUp volatility for the upper bump
     * @param frameDown market data for the lower bump
     * @param volatilityDown volatility for the lower bump
     * @param bump absolute distance from the base input to either bump
     * @return central bump-and-revalue derivative
     */
    private double centralDifference(
            MarketData frameUp,
            double volatilityUp,
            MarketData frameDown,
            double volatilityDown,
            double bump
    ) {
        double timeToExpiry = dayCountConvention.timeToExpiry(
                frame.timestampNanos(),
                contract.expirationDate()
        );
        int bumpSteps = Math.max(
                steps,
                Math.max(
                        minimumRequiredSteps(frameUp.costOfCarry(), timeToExpiry, volatilityUp),
                        minimumRequiredSteps(frameDown.costOfCarry(), timeToExpiry, volatilityDown)
                )
        );
        double priceUp = new TrinomialTree(
                contract, frameUp, volatilityUp, bumpSteps, dayCountConvention
        ).price();
        double priceDown = new TrinomialTree(
                contract, frameDown, volatilityDown, bumpSteps, dayCountConvention
        ).price();
        return (priceUp - priceDown) / (2.0 * bump);
    }

    /** {@inheritDoc} */
    @Override
    public TrinomialTree withSpot(double newSpot) {
        return new TrinomialTree(
                contract, frame.withSpotPrice(newSpot), vol, steps, dayCountConvention
        );
    }

    /** {@inheritDoc} */
    @Override
    public TrinomialTree withVolatility(double newVolatility) {
        return new TrinomialTree(contract, frame, newVolatility, steps, dayCountConvention);
    }

    /** {@inheritDoc} */
    @Override
    public TrinomialTree withRiskFreeRate(double newRate) {
        return new TrinomialTree(
                contract, frame.withRiskFreeRate(newRate), vol, steps, dayCountConvention
        );
    }

    /** {@inheritDoc} */
    @Override
    public TrinomialTree withTimestamp(long newTimestampNanos) {
        return new TrinomialTree(
                contract,
                frame.withTimestampNanos(newTimestampNanos),
                vol,
                steps,
                dayCountConvention
        );
    }

    @Override
    public DayCountConvention dayCountConvention() {
        return dayCountConvention;
    }
}
