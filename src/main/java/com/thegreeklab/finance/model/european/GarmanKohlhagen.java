package com.thegreeklab.finance.model.european;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.exception.InvalidVolatilityException;
import com.thegreeklab.finance.exception.UnsupportedExerciseStyleException;
import com.thegreeklab.finance.frame.FXFrame;
import com.thegreeklab.finance.time.DayCountConvention;

/**
 * Garman-Kohlhagen pricing model for European FX options.
 *
 * <p>This is a thin, type-safe specialization of {@link BlackScholes} that
 * only accepts an {@link FXFrame} as its market data input. The Garman-Kohlhagen
 * formula is recovered automatically from the generalized engine because
 * {@link FXFrame#costOfCarry()} returns {@code domesticRate - foreignRate},
 * treating the foreign interest rate as a continuous "dividend yield" on the
 * foreign currency.
 *
 * <p>Most pricing and Greek logic is inherited unchanged from {@link BlackScholes}.
 * {@link #rho()} is the domestic-rate sensitivity. {@link #epsilon()} is the
 * foreign-rate sensitivity, since the foreign rate enters the generalized model
 * like a continuous dividend yield.
 */
public final class GarmanKohlhagen extends BlackScholes {

    /**
     * Constructs a Garman-Kohlhagen priced FX option.
     *
     * @param contract   the option contract being priced
     * @param frame      FX market data (spot exchange rate, domestic and foreign rates)
     * @param volatility annualized volatility of the FX rate, as a decimal
     * @param dayCountConvention convention used to derive the year fraction
     * @throws InvalidVolatilityException        if {@code volatility} is below {@code 1e-6}
     *                                           or is not finite
     * @throws UnsupportedExerciseStyleException if {@code contract} is not European
     */
    public GarmanKohlhagen(
            OptionContract contract,
            FXFrame frame,
            double volatility,
            DayCountConvention dayCountConvention
    ) {
        super(contract, frame, volatility, dayCountConvention);
    }

    /**
     * Domestic rho: sensitivity of the FX option price to the domestic
     * continuously compounded risk-free rate.
     *
     * @return domestic-rate sensitivity
     */
    @Override
    public double rho() {
        return super.rho();
    }

    /**
     * Foreign rho: sensitivity of the FX option price to the foreign
     * continuously compounded risk-free rate. The inherited generalized BSM
     * epsilon is exactly this quantity because the foreign rate is modeled like
     * a continuous yield on the foreign currency.
     *
     * @return foreign-rate sensitivity
     */
    @Override
    public double epsilon() {
        return super.epsilon();
    }
}
