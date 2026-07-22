package com.thegreeklab.finance.model.greeks;

import com.thegreeklab.finance.time.DayCountConvention;
import com.thegreeklab.finance.validation.PricingValidation;
import com.thegreeklab.math.volatility.VolatilityPricer;

/**
 * Shared finite-difference implementation for immutable option models.
 *
 * <p>Concrete models provide their current market inputs and immutable bump
 * factories. The common implementation keeps bump sizes, finite-difference
 * formulas and Greek snapshot construction consistent across model families.
 * Models with a dated discontinuity may override
 * {@link #thetaFromPrice(double)} without duplicating the remaining Greeks.</p>
 */
public abstract class AbstractBumpAndRevalueModel
        implements BumpableOptionModel, VolatilityPricer {

    /** Nanoseconds in one fixed 24-hour day used for theta bumps. */
    protected static final long ONE_DAY_NANOS = 86_400_000_000_000L;

    /** Smallest absolute bump used by spot and volatility differences. */
    protected static final double MINIMUM_ABSOLUTE_BUMP = 1e-6;

    private static final double DELTA_SPOT_BUMP = 1e-4;
    private static final double GAMMA_SPOT_BUMP = 1e-3;
    private static final double VOLATILITY_BUMP = 1e-4;
    private static final double RATE_BUMP = 1e-4;

    /**
     * Creates the shared finite-difference base.
     */
    protected AbstractBumpAndRevalueModel() {
    }

    /**
     * Returns the current unadjusted underlying price.
     *
     * @return current unadjusted underlying price
     */
    protected abstract double spotPrice();

    /**
     * Returns the current annualized volatility.
     *
     * @return current annualized volatility
     */
    protected abstract double volatility();

    /**
     * Returns the current continuously compounded risk-free rate.
     *
     * @return current continuously compounded risk-free rate
     */
    protected abstract double riskFreeRate();

    /**
     * Returns the current valuation timestamp.
     *
     * @return current valuation timestamp in epoch nanoseconds
     */
    protected abstract long valuationTimestampNanos();

    /**
     * Returns the contract expiration timestamp.
     *
     * @return contract expiration timestamp in epoch nanoseconds
     */
    protected abstract long expirationTimestampNanos();

    /**
     * Selects a valid central spot bump. Models with a narrower numerical
     * domain may reduce the returned value.
     *
     * @param relativeBump relative bump requested by the finite difference
     * @return positive bump valid for both repricing points
     */
    protected double spotBump(double relativeBump) {
        double requested = Math.max(
                spotPrice() * relativeBump,
                MINIMUM_ABSOLUTE_BUMP
        );
        return Math.min(requested, spotPrice() * 0.5);
    }

    /**
     * Calculates theta while reusing an already evaluated base price.
     *
     * <p>The default implementation advances valuation by at most one day.
     * Models with dated discontinuities can override this hook.</p>
     *
     * @param currentPrice price at the current valuation timestamp
     * @return annualized theta
     */
    protected double thetaFromPrice(double currentPrice) {
        long valuationTimestamp = valuationTimestampNanos();
        long expirationTimestamp = expirationTimestampNanos();
        if (valuationTimestamp >= expirationTimestamp) {
            return 0.0;
        }

        long remainingNanos = Math.subtractExact(
                expirationTimestamp,
                valuationTimestamp
        );
        long bump = Math.min(
                ONE_DAY_NANOS,
                Math.max(1L, remainingNanos / 2L)
        );
        long bumpedTimestamp = Math.addExact(valuationTimestamp, bump);
        DayCountConvention convention = dayCountConvention();
        double elapsedYears = convention.yearFraction(
                valuationTimestamp,
                bumpedTimestamp
        );
        double bumpedPrice = withTimestamp(bumpedTimestamp).price();
        return (bumpedPrice - currentPrice) / elapsedYears;
    }

    @Override
    public final double delta() {
        double bump = spotBump(DELTA_SPOT_BUMP);
        double up = withSpot(spotPrice() + bump).price();
        double down = withSpot(spotPrice() - bump).price();
        return (up - down) / (2.0 * bump);
    }

    @Override
    public final double gamma() {
        return gammaFromPrice(price());
    }

    private double gammaFromPrice(double centerPrice) {
        double bump = spotBump(GAMMA_SPOT_BUMP);
        double up = withSpot(spotPrice() + bump).price();
        double down = withSpot(spotPrice() - bump).price();
        return (up - 2.0 * centerPrice + down) / (bump * bump);
    }

    @Override
    public final double vega() {
        double bump = Math.max(
                volatility() * VOLATILITY_BUMP,
                MINIMUM_ABSOLUTE_BUMP
        );
        double downVolatility = Math.max(
                volatility() - bump,
                PricingValidation.MIN_VOLATILITY
        );
        double upVolatility = volatility() + bump;
        double up = withVolatility(upVolatility).price();
        double down = withVolatility(downVolatility).price();
        return (up - down) / (upVolatility - downVolatility);
    }

    @Override
    public final double theta() {
        return thetaFromPrice(price());
    }

    @Override
    public final double rho() {
        double up = withRiskFreeRate(riskFreeRate() + RATE_BUMP).price();
        double down = withRiskFreeRate(riskFreeRate() - RATE_BUMP).price();
        return (up - down) / (2.0 * RATE_BUMP);
    }

    @Override
    public final StandardGreekValues greeks() {
        double currentPrice = price();
        return new StandardGreekValues(
                currentPrice,
                delta(),
                gammaFromPrice(currentPrice),
                vega(),
                thetaFromPrice(currentPrice),
                rho()
        );
    }

    @Override
    public final double priceAtVolatility(double newVolatility) {
        return withVolatility(newVolatility).price();
    }
}
