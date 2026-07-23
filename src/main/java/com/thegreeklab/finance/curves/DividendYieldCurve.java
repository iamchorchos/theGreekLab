package com.thegreeklab.finance.curves;

import com.thegreeklab.finance.curves.interfaces.DiscountCurve;

import java.util.Objects;

/**
 * Nominal market-data role for an equity's continuous-dividend discount curve.
 *
 * <p>The curve supplies {@code Dq(T)} in {@code F(T) = S * Dq(T) / Dr(T)}.
 * This wrapper makes it impossible to exchange it with a {@link FundingCurve}
 * when constructing an {@link EquityForwardCurve}.</p>
 *
 * @param delegate underlying continuous-dividend discount curve
 */
public record DividendYieldCurve(DiscountCurve delegate) implements DiscountCurve {

    /**
     * Validates the wrapped discount curve.
     *
     * @throws NullPointerException if the delegate is {@code null}
     */
    public DividendYieldCurve {
        Objects.requireNonNull(delegate, "Dividend-yield discount curve cannot be null.");
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
