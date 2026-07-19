package com.thegreeklab.finance.model.european.discrete.adjustments;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.exception.NonPositivePriceException;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.european.BlackScholesMerton;
import com.thegreeklab.finance.model.european.discrete.CashDividend;
import com.thegreeklab.finance.model.european.discrete.DividendSchedule;
import com.thegreeklab.finance.model.greeks.StandardGreekValues;
import com.thegreeklab.finance.time.DayCountConvention;
import com.thegreeklab.finance.time.EpochNanos;
import com.thegreeklab.finance.validation.PricingValidation;
import net.jafama.FastMath;

import java.util.List;
import java.util.Objects;

import static com.thegreeklab.finance.validation.PricingValidation.requireEuropeanStyle;
import static com.thegreeklab.finance.validation.PricingValidation.requireNoContinuousDividendYield;

/**
 * Shared preparation and pricing workflow for discrete-dividend volatility
 * adjustment models.
 */
abstract class AbstractDiscreteDividendModel implements DiscreteDividendOptionModel {

    private static final double DELTA_SPOT_BUMP = 1e-4;
    private static final double GAMMA_SPOT_BUMP = 1e-3;
    private static final double VOLATILITY_BUMP = 1e-4;
    private static final double RATE_BUMP = 1e-4;
    private static final long ONE_DAY_NANOS = 86_400_000_000_000L;

    private final OptionContract contract;
    private final EquityFrame frame;
    private final DividendSchedule dividendSchedule;
    private final List<CashDividend> cashDividends;
    private final double[] dividendTimes;
    private final double volatility;
    private final DayCountConvention dayCountConvention;
    private final double presentValueOfDividends;
    private final double adjustedSpot;

    AbstractDiscreteDividendModel(
            OptionContract contract,
            EquityFrame frame,
            DividendSchedule dividends,
            double volatility,
            DayCountConvention dayCountConvention
    ) {
        this.contract = Objects.requireNonNull(contract, "Contract cannot be null.");
        this.frame = Objects.requireNonNull(frame, "Equity frame cannot be null.");
        this.dividendSchedule = Objects.requireNonNull(
                dividends,
                "Dividend schedule cannot be null."
        );
        this.dayCountConvention = Objects.requireNonNull(
                dayCountConvention,
                "Day-count convention cannot be null."
        );

        PricingValidation.requireValidVolatility(volatility);
        requireEuropeanStyle(contract);
        requireNoContinuousDividendYield(frame);

        this.volatility = volatility;
        long valuationTimestamp = frame.timestampNanos();
        long expiryTimestamp = EpochNanos.from(contract.expirationDate());
        this.cashDividends = dividendSchedule.dividends().stream()
                .filter(dividend -> dividend.exTimestampNanos() > valuationTimestamp)
                .filter(dividend -> dividend.exTimestampNanos() < expiryTimestamp)
                .toList();

        double riskFreeRate = frame.riskFreeRate();
        this.dividendTimes = new double[cashDividends.size()];
        double dividendPresentValue = 0.0;
        for (int i = 0; i < cashDividends.size(); i++) {
            CashDividend dividend = cashDividends.get(i);
            double timeToDividend = dayCountConvention.yearFraction(
                    valuationTimestamp,
                    dividend.exTimestampNanos()
            );
            dividendTimes[i] = timeToDividend;
            dividendPresentValue += dividend.amount()
                    * FastMath.exp(-riskFreeRate * timeToDividend);
        }

        this.presentValueOfDividends = dividendPresentValue;
        this.adjustedSpot = frame.spotPrice() - dividendPresentValue;
    }

    /**
     * Returns the validated option contract.
     *
     * @return validated option contract
     */
    protected final OptionContract contract() {
        return contract;
    }

    /**
     * Returns the validated equity market data.
     *
     * @return validated equity market data
     */
    protected final EquityFrame frame() {
        return frame;
    }

    /**
     * Returns the complete immutable schedule supplied to the model.
     *
     * @return original dividend schedule, including currently inapplicable entries
     */
    protected final DividendSchedule dividendSchedule() {
        return dividendSchedule;
    }

    /**
     * Returns the original volatility supplied to the model.
     *
     * @return validated original annualized volatility
     */
    protected final double volatility() {
        return volatility;
    }

    /**
     * Returns the convention shared by all model calculations.
     *
     * @return convention used for all model year fractions
     */
    @Override
    public final DayCountConvention dayCountConvention() {
        return dayCountConvention;
    }

    /**
     * Returns the discounted value of the applicable dividend schedule.
     *
     * @return present value of all applicable cash dividends
     */
    protected final double presentValueOfDividends() {
        return presentValueOfDividends;
    }

    /**
     * Returns the size of the applicable dividend schedule.
     *
     * @return number of applicable cash dividends
     */
    protected final int dividendCount() {
        return cashDividends.size();
    }

    /**
     * Returns an applicable dividend by chronological index.
     *
     * @param index zero-based dividend index
     * @return dividend at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is outside the schedule
     */
    protected final CashDividend dividendAt(int index) {
        return cashDividends.get(index);
    }

    /**
     * Returns the precomputed time from valuation to an applicable dividend.
     *
     * @param index zero-based dividend index
     * @return dividend time in years under the configured convention
     * @throws ArrayIndexOutOfBoundsException if {@code index} is outside the schedule
     */
    protected final double dividendTimeAt(int index) {
        return dividendTimes[index];
    }

    /**
     * Validates the model-specific spot supplied to Black-Scholes-Merton.
     *
     * <p>Concrete models call this method after their adjustment state has
     * been initialized. This permits models such as Bos-Vandermark to validate
     * their near-dividend spot instead of the full escrowed-dividend spot.</p>
     *
     * @param modelAdjustedSpot adjusted spot produced by the concrete model
     * @throws NonPositivePriceException if {@code modelAdjustedSpot} is not
     *                                   strictly positive and finite
     */
    protected final void requirePositiveAdjustedSpot(double modelAdjustedSpot) {
        requirePositiveFinite(modelAdjustedSpot, "Adjusted spot");
    }

    /**
     * Validates a model-specific strike supplied to Black-Scholes-Merton.
     *
     * @param modelAdjustedStrike adjusted strike produced by the concrete model
     * @throws NonPositivePriceException if {@code modelAdjustedStrike} is not
     *                                   strictly positive and finite
     */
    protected final void requirePositiveAdjustedStrike(double modelAdjustedStrike) {
        requirePositiveFinite(modelAdjustedStrike, "Adjusted strike");
    }

    private static void requirePositiveFinite(double value, String description) {
        if (!(value > 0.0) || !Double.isFinite(value)) {
            throw new NonPositivePriceException(
                    description + " must be strictly positive and finite."
            );
        }
    }

    /**
     * Creates the concrete model with replacement market data and volatility.
     *
     * @param newFrame replacement equity market data
     * @param newVolatility replacement annualized volatility
     * @return immutable concrete model copy
     */
    protected abstract AbstractDiscreteDividendModel newModel(
            EquityFrame newFrame,
            double newVolatility
    );

    /** {@inheritDoc} */
    @Override
    public final DiscreteDividendOptionModel withSpot(double newSpot) {
        return newModel(frame.withSpotPrice(newSpot), volatility);
    }

    /** {@inheritDoc} */
    @Override
    public final DiscreteDividendOptionModel withVolatility(double newVolatility) {
        return newModel(frame, newVolatility);
    }

    /** {@inheritDoc} */
    @Override
    public final DiscreteDividendOptionModel withRiskFreeRate(double newRate) {
        return newModel(frame.withRiskFreeRate(newRate), volatility);
    }

    /** {@inheritDoc} */
    @Override
    public final DiscreteDividendOptionModel withTimestamp(long newTimestampNanos) {
        return newModel(frame.withTimestampNanos(newTimestampNanos), volatility);
    }

    /**
     * Estimates delta by centrally bumping the original, unadjusted spot.
     *
     * @return numerical first derivative of price with respect to spot
     */
    @Override
    public final double delta() {
        double bump = spotBump(DELTA_SPOT_BUMP);
        double up = withSpot(frame.spotPrice() + bump).price();
        double down = withSpot(frame.spotPrice() - bump).price();
        return (up - down) / (2.0 * bump);
    }

    /**
     * Estimates gamma by centrally bumping the original, unadjusted spot.
     *
     * @return numerical second derivative of price with respect to spot
     */
    @Override
    public final double gamma() {
        return gamma(price());
    }

    private double gamma(double centerPrice) {
        double bump = spotBump(GAMMA_SPOT_BUMP);
        double up = withSpot(frame.spotPrice() + bump).price();
        double down = withSpot(frame.spotPrice() - bump).price();
        return (up - 2.0 * centerPrice + down) / (bump * bump);
    }

    private double spotBump(double relativeBump) {
        double requestedBump = Math.max(frame.spotPrice() * relativeBump, 1e-6);
        return Math.min(requestedBump, frame.spotPrice() * 0.5);
    }

    /**
     * Estimates vega by bumping the original volatility over its valid local
     * interval.
     *
     * @return numerical first derivative of price per unit of volatility
     */
    @Override
    public final double vega() {
        double bump = Math.max(volatility * VOLATILITY_BUMP, 1e-6);
        double volatilityDown = Math.max(
                volatility - bump,
                PricingValidation.MIN_VOLATILITY
        );
        double upVolatility = volatility + bump;
        double up = withVolatility(upVolatility).price();
        double down = withVolatility(volatilityDown).price();
        return (up - down) / (upVolatility - volatilityDown);
    }

    /**
     * Estimates annualized theta by advancing the valuation timestamp by at
     * most one calendar day. At expiry the method returns {@code 0.0}.
     *
     * @return numerical derivative of price with respect to the passage of time
     */
    @Override
    public final double theta() {
        return theta(price());
    }

    private double theta(double currentPrice) {
        long valuationTimestamp = frame.timestampNanos();
        long expirationTimestamp = EpochNanos.from(contract.expirationDate());
        if (valuationTimestamp >= expirationTimestamp) {
            return 0.0;
        }

        long remainingNanos = Math.subtractExact(
                expirationTimestamp,
                valuationTimestamp
        );
        long bump = Math.min(ONE_DAY_NANOS, Math.max(1L, remainingNanos / 2L));
        long bumpedTimestamp = Math.addExact(valuationTimestamp, bump);
        double elapsedYears = dayCountConvention.yearFraction(
                valuationTimestamp,
                bumpedTimestamp
        );
        double bumpedPrice = withTimestamp(bumpedTimestamp).price();
        return (bumpedPrice - currentPrice) / elapsedYears;
    }

    /**
     * Estimates rho with a central one-basis-point bump of the original
     * continuously compounded risk-free rate.
     *
     * @return numerical first derivative of price per unit of rate
     */
    @Override
    public final double rho() {
        double rate = frame.riskFreeRate();
        double up = withRiskFreeRate(rate + RATE_BUMP).price();
        double down = withRiskFreeRate(rate - RATE_BUMP).price();
        return (up - down) / (2.0 * RATE_BUMP);
    }

    /**
     * Calculates price and all standard numerical Greeks while reusing the
     * base price for gamma and theta.
     *
     * @return immutable price and Greek snapshot
     */
    @Override
    public final StandardGreekValues greeks() {
        double currentPrice = price();
        return new StandardGreekValues(
                currentPrice,
                delta(),
                gamma(currentPrice),
                vega(),
                theta(currentPrice),
                rho()
        );
    }

    @Override
    public double adjustedSpot() {
        return adjustedSpot;
    }

    @Override
    public final double price() {
        OptionContract adjustedContract = contract.withStrike(adjustedStrike());
        EquityFrame adjustedFrame = frame.withSpotPrice(adjustedSpot());

        return BlackScholesMerton.price(
                adjustedContract,
                adjustedFrame,
                adjustedVolatility(),
                dayCountConvention
        );
    }

    @Override
    public double adjustedStrike() {
        return contract.strikePrice();
    }
}
