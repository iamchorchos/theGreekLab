package com.thegreeklab.math.volatility;

import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Diagnostic result of an implied-volatility calibration.
 *
 * <p>Successful results contain the converged volatility, reproduced model
 * price, residual pricing error and total number of bracket/root iterations.
 * Failure results retain the last valid candidate where one is available;
 * unavailable numerical values are represented by {@link Double#NaN}.</p>
 *
 * @param status terminal solver status
 * @param volatility converged or last valid trial volatility
 * @param modelPrice model price at {@code volatility}
 * @param priceError {@code modelPrice - marketPrice}
 * @param iterations total bracket-discovery and root-finding iterations
 */
public record ImpliedVolatilityResult(
        Status status,
        double volatility,
        double modelPrice,
        double priceError,
        int iterations
) {

    /** Terminal state of an implied-volatility calibration. */
    public enum Status {
        /** A volatility satisfying the configured tolerances was found. */
        CONVERGED,
        /** The contract is at or past expiration. */
        EXPIRED_CONTRACT,
        /** The observed price violates the applicable model-free bounds. */
        PRICE_OUTSIDE_BOUNDS,
        /** The supplied starting volatility is outside the supported range. */
        INVALID_INITIAL_VOLATILITY,
        /** No sign-changing bracket exists in the supported volatility range. */
        ROOT_NOT_BRACKETED,
        /** The model cannot be evaluated in the required parameter region. */
        INVALID_MODEL_DOMAIN,
        /** The solver exhausted its iteration budget. */
        MAX_ITERATIONS
    }

    /** Validates structural result invariants. */
    public ImpliedVolatilityResult {
        Objects.requireNonNull(status, "Status cannot be null.");
        if (iterations < 0) {
            throw new IllegalArgumentException("Iterations cannot be negative.");
        }
        requireFiniteOrNaN(volatility, "Volatility");
        requireFiniteOrNaN(modelPrice, "Model price");
        requireFiniteOrNaN(priceError, "Price error");

        if (status == Status.CONVERGED) {
            if (!(volatility > 0.0) || !Double.isFinite(volatility)) {
                throw new IllegalArgumentException(
                        "Converged volatility must be strictly positive and finite."
                );
            }
            if (!Double.isFinite(modelPrice) || !Double.isFinite(priceError)) {
                throw new IllegalArgumentException(
                        "Converged price diagnostics must be finite."
                );
            }
        }
    }

    /**
     * Reports whether calibration completed successfully.
     *
     * @return {@code true} only when the solver converged
     */
    public boolean converged() {
        return status == Status.CONVERGED;
    }

    /**
     * Compatibility view matching the legacy solver API.
     *
     * @return converged volatility, or empty for every failure status
     */
    public OptionalDouble asOptionalDouble() {
        return converged()
                ? OptionalDouble.of(volatility)
                : OptionalDouble.empty();
    }

    private static void requireFiniteOrNaN(double value, String description) {
        if (Double.isInfinite(value)) {
            throw new IllegalArgumentException(description + " cannot be infinite.");
        }
    }
}
