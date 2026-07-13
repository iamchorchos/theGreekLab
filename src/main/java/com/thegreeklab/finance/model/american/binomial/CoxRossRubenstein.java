package com.thegreeklab.finance.model.american.binomial;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.exception.InvalidStepCountException;
import com.thegreeklab.finance.exception.InvalidVolatilityException;
import com.thegreeklab.finance.exception.MathException;
import com.thegreeklab.finance.exception.UnsupportedExerciseStyleException;
import com.thegreeklab.finance.frame.MarketData;
import com.thegreeklab.finance.validation.PricingValidation;
import net.jafama.FastMath;

/**
 * Cox-Ross-Rubenstein binomial tree for American vanilla options.
 *
 * <p>The CRR parameterization uses symmetric multiplicative up/down moves
 * ({@code u * d == 1}) and prices by backward induction with early exercise at
 * every node.
 */
public final class CoxRossRubenstein extends BinomialModel {

    private final double volatility;

    /**
     * Constructs a CRR tree and precomputes its up/down factors and
     * risk-neutral transition probability.
     *
     * @param contract   American option contract being priced
     * @param frame      market data snapshot supplying spot, discount rate and cost of carry
     * @param volatility annualized volatility as a decimal; must be finite and above {@code 1e-6}
     * @param steps      positive number of tree steps
     * @throws NullPointerException              if {@code contract} or {@code frame} is {@code null}
     * @throws UnsupportedExerciseStyleException if {@code contract} is not American
     * @throws InvalidStepCountException         if {@code steps} is not positive
     * @throws InvalidVolatilityException        if {@code volatility} is below {@code 1e-6}
     *                                           or is not finite
     * @throws MathException                     if the risk-neutral probability is not finite
     *                                           or falls outside {@code [0, 1]}
     */
    public CoxRossRubenstein(OptionContract contract, MarketData frame, double volatility, int steps) {
        super(contract, frame, steps);
        PricingValidation.requireValidVolatility(volatility);
        this.u = FastMath.exp(volatility * FastMath.sqrt(this.dt));
        this.d = 1.0 / this.u;
        this.p = (FastMath.exp(this.costOfCarry * this.dt) - this.d) / (this.u - this.d);
        if (!Double.isFinite(this.p) || this.p < 0.0 || this.p > 1.0) {
            throw new MathException("Risk-neutral probability must be finite and between 0 and 1. Received: " + this.p);
        }
        this.volatility = volatility;
    }


    @Override
    protected double calculateSpotNode(int step, int upMoves) {
        return this.s * FastMath.pow(this.u, (2 * upMoves - step));
    }

    @Override
    protected BinomialModel withVolatility(double newVolatility) {
        return new CoxRossRubenstein(this.contract, this.frame, newVolatility, this.steps);
    }

    @Override
    protected BinomialModel withRiskFreeRate(double newRate) {
        return new CoxRossRubenstein(this.contract, this.frame.withRiskFreeRate(newRate), this.volatility, this.steps);
    }

    @Override
    protected BinomialModel withSpot(double newSpot) {
        return new CoxRossRubenstein(this.contract, this.frame.withSpotPrice(newSpot), this.volatility, this.steps);
    }

    @Override
    protected BinomialModel withTimestamp(long newTimestampNanos) {
        return new CoxRossRubenstein(this.contract, this.frame.withTimestampNanos(newTimestampNanos), this.volatility, this.steps);
    }

    @Override
    protected BinomialModel withStrike(double newStrike) {
        return new CoxRossRubenstein(this.contract.withStrike(newStrike), this.frame, this.volatility, this.steps);
    }

    /**
     * Computes theta from cached level-2 tree values. This fast path is valid
     * for CRR because the tree recombines symmetrically ({@code u * d == 1}).
     *
     * @return annualized theta
     */
    @Override
    public double theta() {
        return fastThetaViaNodeCache();
    }

    @Override
    public double getVolatility() {
        return volatility;
    }
}

