package com.thegreeklab.finance.model.european;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.exception.InvalidVolatilityException;
import com.thegreeklab.finance.exception.UnsupportedExerciseStyleException;
import com.thegreeklab.finance.frame.FuturesFrame;

/**
 * Black-76 pricing model for European options on futures contracts.
 *
 * <p>This is a thin, type-safe specialization of {@link BlackScholes} that only
 * accepts a {@link FuturesFrame} as its market data input. The Black-76 formula
 * is recovered automatically from the generalized engine because
 * {@link FuturesFrame#costOfCarry()} always returns {@code 0} and
 * {@link FuturesFrame#spotPrice()} returns the futures price itself.
 *
 * <p>Most pricing and Greek logic is inherited from {@link BlackScholes}.
 * {@link #rho()} is specialized because Black-76 takes the observed futures
 * price as an input, so {@code d1}/{@code d2} do not depend on the risk-free
 * rate; the rate sensitivity comes only from the discount factor. {@link #epsilon()}
 * is not defined because there is no dividend or foreign-rate input in
 * {@link FuturesFrame}.
 */
public final class Black76 extends BlackScholes {

    /**
     * Constructs a Black-76 priced futures option.
     *
     * @param contract   the option contract being priced
     * @param frame      futures market data (futures price, risk-free rate)
     * @param volatility annualized volatility of the futures price, as a decimal
     * @throws InvalidVolatilityException        if {@code volatility} is below {@code 1e-6}
     *                                           or is not finite
     * @throws UnsupportedExerciseStyleException if {@code contract} is not European
     */
    public Black76(OptionContract contract, FuturesFrame frame, double volatility) {
        super(contract, frame, volatility);
    }

    /**
     * Black-76 rho with the futures price supplied directly as {@code F}.
     *
     * <p>Since {@code d1} and {@code d2} are independent of {@code r} in this
     * parameterization, differentiating the discount factor gives
     * {@code rho = -T * price}.
     *
     * @return risk-free-rate sensitivity of the discounted futures-option price
     */
    @Override
    public double rho() {
        return -timeToExpiry() * price();
    }

    /**
     * Epsilon is not defined for Black-76 because the model takes the futures
     * price directly and has no dividend-yield or foreign-rate input.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public double epsilon() {
        throw new UnsupportedOperationException("Epsilon is not defined for Black-76 futures options.");
    }
}
