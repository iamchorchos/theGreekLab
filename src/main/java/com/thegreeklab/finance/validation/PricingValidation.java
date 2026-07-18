package com.thegreeklab.finance.validation;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.exception.InvalidStepCountException;
import com.thegreeklab.finance.exception.InvalidVolatilityException;
import com.thegreeklab.finance.exception.UnsupportedExerciseStyleException;

/**
 * Shared validation rules for option-pricing model inputs.
 */
public final class PricingValidation {

    /** Lowest volatility accepted by the pricing engines. */
    public static final double MIN_VOLATILITY = 1e-6;

    private PricingValidation() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Ensures that an annualized volatility is finite and within the supported domain.
     *
     * @param volatility annualized volatility as a decimal
     * @throws InvalidVolatilityException if {@code volatility} is not finite or is
     *                                    below {@link #MIN_VOLATILITY}
     */
    public static void requireValidVolatility(double volatility) {
        if (!Double.isFinite(volatility) || volatility < MIN_VOLATILITY) {
            throw new InvalidVolatilityException(
                    "Volatility must be strictly positive and finite. Received: "
                            + volatility
            );
        }
    }

    /**
     * Ensures that a discrete pricing model uses a positive number of steps.
     *
     * @param steps requested tree or lattice depth
     * @throws InvalidStepCountException if {@code steps} is not positive
     */
    public static void requireValidSteps(int steps) {
        if (steps <= 0) {
            throw new InvalidStepCountException("Steps must be positive. Received: " + steps);
        }
    }

    /**
     * Ensures that a contract has American exercise style.
     *
     * @param contract option contract to validate
     * @throws UnsupportedExerciseStyleException if the contract is not American
     */
    public static void requireAmericanStyle(OptionContract contract) {
        if (contract.option() != Option.AMERICAN) {
            throw new UnsupportedExerciseStyleException(
                    "Binomial American models only support AMERICAN option style. Received: " + contract.option()
            );
        }
    }

    /**
     * Ensures that a contract uses a supported vanilla exercise style.
     *
     * @param contract option contract to validate
     * @throws UnsupportedExerciseStyleException if the contract is exotic
     */
    public static void requireNonExoticStyle(OptionContract contract) {
        if (contract.option() == Option.EXOTIC) {
            throw new UnsupportedExerciseStyleException(
                    "Exotic exercise style is not supported."
            );
        }
    }
}
