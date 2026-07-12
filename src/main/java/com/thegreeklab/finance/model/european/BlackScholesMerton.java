package com.thegreeklab.finance.model.european;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.exception.InvalidVolatilityException;
import com.thegreeklab.finance.exception.UnsupportedExerciseStyleException;
import com.thegreeklab.finance.frame.EquityFrame;

/**
 * Black-Scholes-Merton pricing model for European equity options.
 *
 * <p>This is a thin, type-safe specialization of {@link BlackScholes} that only
 * accepts an {@link EquityFrame} as its market data input. The equity frame
 * supplies {@code costOfCarry() = r - q}, recovering the classic
 * Black-Scholes model when {@code q = 0} and the Merton continuous-dividend
 * model when {@code q > 0}.
 *
 * <p>All pricing and Greeks are inherited unchanged from {@link BlackScholes}.
 */
public final class BlackScholesMerton extends BlackScholes {

    /**
     * Constructs a Black-Scholes-Merton priced equity option.
     *
     * @param contract   the option contract being priced
     * @param frame      equity market data (spot price, risk-free rate, dividend yield)
     * @param volatility annualized volatility of the underlying, as a decimal
     * @throws InvalidVolatilityException        if {@code volatility} is below {@code 1e-6}
     *                                           or is not finite
     * @throws UnsupportedExerciseStyleException if {@code contract} is not European
     */
    public BlackScholesMerton(OptionContract contract, EquityFrame frame, double volatility) {
        super(contract, frame, volatility);
    }
}
