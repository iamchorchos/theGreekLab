package com.thegreeklab.finance.curves;

import com.thegreeklab.finance.curves.interfaces.DiscountCurve;

import java.util.Objects;

/**
 * Nominal market-data role for the funding discount curve of an equity option.
 *
 * <p>This wrapper distinguishes funding discount factors from other values that
 * are numerically represented by a {@link DiscountCurve}. It prevents callers
 * from accidentally passing a dividend-yield curve where funding discounting is
 * required.</p>
 *
 * @param delegate underlying funding discount curve
 */
public record FundingCurve(DiscountCurve delegate) implements DiscountCurve {

    /**
     * Validates the wrapped discount curve.
     *
     * @throws NullPointerException if the delegate is {@code null}
     */
    public FundingCurve {
        Objects.requireNonNull(delegate, "Funding discount curve cannot be null.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long valuationTimestampNanos() {
        return delegate.valuationTimestampNanos();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double discountFactor(long timestampNanos) {
        return delegate.discountFactor(timestampNanos);
    }
}
