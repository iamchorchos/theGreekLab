package com.thegreeklab.finance.model.american.binomial;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.exception.InvalidStepCountException;
import com.thegreeklab.finance.exception.InvalidVolatilityException;
import com.thegreeklab.finance.exception.MathException;
import com.thegreeklab.finance.exception.UnsupportedExerciseStyleException;
import com.thegreeklab.finance.exception.UnsupportedFrameTypeException;
import com.thegreeklab.finance.frame.FuturesFrame;
import com.thegreeklab.finance.frame.MarketData;
import com.thegreeklab.math.PeizerPrattInversion;
import net.jafama.FastMath;

/**
 * Leisen-Reimer binomial tree for American vanilla options.
 *
 * <p>The tree uses the Peizer-Pratt method-2 transform and therefore requires
 * an odd number of time steps. Futures frames are rejected because this
 * implementation is parameterized for spot-style market data.
 */
public final class LeisenReimer extends BinomialModel {

    private final double volatility;
    private final double d1;
    private final double d2;
    private final double pPrime;
    private final double t;


    /**
     * Constructs a Leisen-Reimer tree and precomputes the probability transform
     * and up/down factors.
     *
     * @param contract   American option contract being priced
     * @param frame      market data snapshot supplying spot, discount rate and cost of carry
     * @param volatility annualized volatility as a decimal; must be finite and above {@code 1e-6}
     * @param steps      positive odd number of tree steps
     * @throws NullPointerException              if {@code contract} or {@code frame} is {@code null}
     * @throws UnsupportedExerciseStyleException if {@code contract} is not American
     * @throws UnsupportedFrameTypeException     if {@code frame} is a futures frame
     * @throws InvalidVolatilityException        if {@code volatility} is below {@code 1e-6}
     *                                           or is not finite
     * @throws InvalidStepCountException         if {@code steps} is not odd
     * @throws MathException                     if transformed tree probabilities or
     *                                           up/down factors are not usable
     */
    public LeisenReimer(OptionContract contract, MarketData frame, double volatility, int steps) {
        super(contract, frame, steps);
        if (frame instanceof FuturesFrame) throw new UnsupportedFrameTypeException("FuturesFrame is not supported for Leisen-Reimer.");
        if (!Double.isFinite(volatility) || volatility < EPSILON) {
            throw new InvalidVolatilityException("Volatility must be strictly positive and finite. Received: " + volatility);
        }
        if ((steps & 1) == 0) throw new InvalidStepCountException("Steps must be odd.");

        this.volatility = volatility;
        this.t = this.dt * this.steps;
        this.d1 = (FastMath.log(this.s / this.k) + (this.costOfCarry + 0.5 * this.volatility * this.volatility) * this.t) / (this.volatility * FastMath.sqrt(this.t));
        this.d2 = this.d1 - this.volatility * FastMath.sqrt(this.t);
        this.p = PeizerPrattInversion.inverseFunction(d2, steps);
        this.pPrime = PeizerPrattInversion.inverseFunction(d1, steps);
        this.u = FastMath.exp(this.costOfCarry * this.dt) * (pPrime / p);
        this.d = FastMath.exp(this.costOfCarry * this.dt) * ((1 - pPrime) / (1 - p));
        validateTreeParameters();
    }

    private void validateTreeParameters() {
        if (!isStrictProbability(this.p)) {
            throw new MathException("Leisen-Reimer probability p must be finite and between 0 and 1. Received: " + this.p);
        }
        if (!isStrictProbability(this.pPrime)) {
            throw new MathException("Leisen-Reimer probability pPrime must be finite and between 0 and 1. Received: " + this.pPrime);
        }
        if (!(this.u > 0.0) || !Double.isFinite(this.u)) {
            throw new MathException("Leisen-Reimer up factor must be strictly positive and finite. Received: " + this.u);
        }
        if (!(this.d > 0.0) || !Double.isFinite(this.d)) {
            throw new MathException("Leisen-Reimer down factor must be strictly positive and finite. Received: " + this.d);
        }
    }

    private static boolean isStrictProbability(double value) {
        return value > 0.0 && value < 1.0 && Double.isFinite(value);
    }

    @Override
    protected double calculateSpotNode(int step, int upMoves) {
        return this.s * FastMath.pow(this.u, upMoves) * FastMath.pow(this.d, step - upMoves);
    }

    @Override
    protected BinomialModel withVolatility(double newVolatility) {
        return new LeisenReimer(this.contract, this.frame, newVolatility, this.steps);
    }

    @Override
    protected BinomialModel withRiskFreeRate(double newRate) {
        return new LeisenReimer(this.contract, this.frame.withRiskFreeRate(newRate), this.volatility, this.steps);
    }

    @Override
    protected BinomialModel withSpot(double newSpot) {
        return new LeisenReimer(this.contract, this.frame.withSpotPrice(newSpot), this.volatility, this.steps);
    }

    @Override
    protected BinomialModel withTimestamp(long newTimestampNanos) {
        return new LeisenReimer(this.contract, this.frame.withTimestampNanos(newTimestampNanos), this.volatility, this.steps);
    }

    @Override
    protected BinomialModel withStrike(double newStrike) {
        return new LeisenReimer(this.contract.withStrike(newStrike), this.frame, this.volatility, this.steps);
    }

    @Override
    public double getVolatility() {
        return volatility;
    }

}
