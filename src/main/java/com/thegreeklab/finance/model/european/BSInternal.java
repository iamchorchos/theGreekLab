package com.thegreeklab.finance.model.european;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.exception.InvalidVolatilityException;
import com.thegreeklab.finance.exception.UnsupportedExerciseStyleException;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.time.DayCountConvention;

/**
 * Deprecated compatibility alias for {@link BlackScholesMerton}.
 *
 * @deprecated use {@link BlackScholesMerton}; this name is kept only to avoid
 * breaking existing callers.
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated(since = "1.0")
public final class BSInternal extends BlackScholes {

    /**
     * Constructs a standard/Merton priced equity option.
     *
     * @param contract   European option contract being priced
     * @param frame      equity market data (spot price, risk-free rate, dividend yield)
     * @param volatility annualized volatility of the underlying, as a decimal
     * @param dayCountConvention convention used to derive the year fraction
     * @throws InvalidVolatilityException        if {@code volatility} is below {@code 1e-6}
     *                                           or is not finite
     * @throws UnsupportedExerciseStyleException if {@code contract} is not European
     */
    public BSInternal(
            OptionContract contract,
            EquityFrame frame,
            double volatility,
            DayCountConvention dayCountConvention
    ) {
        super(contract, frame, volatility, dayCountConvention);
    }
}
