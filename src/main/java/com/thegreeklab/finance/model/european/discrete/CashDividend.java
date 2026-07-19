package com.thegreeklab.finance.model.european.discrete;

/**
 * Deterministic discrete cash dividend paid per unit of the underlying equity.
 *
 * @param exTimestampNanos ex-dividend timestamp in nanoseconds since the UNIX epoch
 * @param amount cash amount paid per unit of the underlying; strictly positive and finite
 */
public record CashDividend(
        long exTimestampNanos,
        double amount
) {
    /**
     * Validates the dividend amount.
     *
     * @throws IllegalArgumentException if {@code amount} is not strictly
     *                                  positive and finite
     */
    public CashDividend {
        if (amount <= 0.0 || !Double.isFinite(amount)) {
            throw new IllegalArgumentException(
                    "Dividend amount must be positive and finite"
            );
        }
    }
}
