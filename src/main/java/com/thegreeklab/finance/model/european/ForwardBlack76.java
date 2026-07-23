package com.thegreeklab.finance.model.european;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.curves.EquityForwardCurve;
import com.thegreeklab.finance.curves.FundingCurve;
import com.thegreeklab.finance.curves.interfaces.ForwardCurve;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.time.DayCountConvention;
import com.thegreeklab.finance.time.EpochNanos;
import com.thegreeklab.finance.validation.PricingValidation;
import com.thegreeklab.finance.volatility.FlatVolatilitySurface;
import com.thegreeklab.finance.volatility.VolatilitySurface;
import com.thegreeklab.math.ERF;
import net.jafama.FastMath;

import java.util.Objects;

/**
 * European-option pricer implementing the Black (1976) formula from a forward
 * curve and a discount curve.
 *
 * <p>This pricer consumes market observables directly: the forward price
 * {@code F(T)} and discount factor {@code DF(T)}. It therefore does not need a
 * spot price, scalar interest rate or cost-of-carry parameter. The price is:</p>
 *
 * <pre>{@code
 * call = DF(T) * [F(T) * N(d1) - K * N(d2)]
 * put  = DF(T) * [K * N(-d2) - F(T) * N(-d1)]
 * }</pre>
 *
 * <p>Version one deliberately exposes price only. Curve-risk and forward-risk
 * sensitivities require explicitly defined bump and interpolation conventions
 * and are not inferred from the spot Greeks of {@code BlackScholes}.</p>
 */
public final class ForwardBlack76 {

    private static final double EXPIRY_DISCOUNT_FACTOR_TOLERANCE = 1e-12;

    private final OptionContract contract;
    private final ForwardCurve forwardCurve;
    private final FundingCurve fundingCurve;
    private final VolatilitySurface volatilitySurface;
    private final DayCountConvention dayCountConvention;

    /**
     * Creates a curve-aware Black-76 pricer for a European vanilla option.
     *
     * @param contract option contract to value; must be European
     * @param forwardCurve curve supplying {@code F(T)}
     * @param fundingCurve funding curve supplying {@code DF(T)}
     * @param volatilitySurface implied-volatility market data
     * @param dayCountConvention convention used to calculate time to expiry
     * @throws NullPointerException if any reference input is {@code null}
     * @throws IllegalArgumentException if the curves have different valuation
     * timestamps, or an equity forward curve uses a different underlying
     * funding curve
     */
    public ForwardBlack76(
            OptionContract contract,
            ForwardCurve forwardCurve,
            FundingCurve fundingCurve,
            VolatilitySurface volatilitySurface,
            DayCountConvention dayCountConvention
    ) {
        this.contract = Objects.requireNonNull(contract, "Contract cannot be null.");
        this.forwardCurve = Objects.requireNonNull(forwardCurve, "Forward curve cannot be null.");
        this.fundingCurve = Objects.requireNonNull(fundingCurve, "Funding curve cannot be null.");
        this.volatilitySurface = Objects.requireNonNull(
                volatilitySurface,
                "Volatility surface cannot be null."
        );
        this.dayCountConvention = Objects.requireNonNull(
                dayCountConvention,
                "Day-count convention cannot be null."
        );
        PricingValidation.requireEuropeanStyle(contract);

        if (forwardCurve.valuationTimestampNanos()
                != fundingCurve.valuationTimestampNanos()) {
            throw new IllegalArgumentException(
                    "Forward and discount curves must share a valuation timestamp."
            );
        }
        if (forwardCurve.valuationTimestampNanos()
                != volatilitySurface.valuationTimestampNanos()) {
            throw new IllegalArgumentException(
                    "Forward curve and volatility surface must share a valuation timestamp."
            );
        }
        if (forwardCurve instanceof EquityForwardCurve equityForwardCurve
                && !equityForwardCurve.fundingCurve().equals(fundingCurve)) {
            throw new IllegalArgumentException(
                    "Equity forward and pricing must use equivalent funding curves."
            );
        }
    }

    /**
     * Creates a curve-aware Black-76 pricer with a flat volatility surface.
     *
     * @param contract option contract to value; must be European
     * @param forwardCurve curve supplying {@code F(T)}
     * @param fundingCurve funding curve supplying {@code DF(T)}
     * @param volatility annualized flat volatility as a decimal
     * @param dayCountConvention convention used to calculate time to expiry
     */
    public ForwardBlack76(
            OptionContract contract,
            ForwardCurve forwardCurve,
            FundingCurve fundingCurve,
            double volatility,
            DayCountConvention dayCountConvention
    ) {
        this(
                contract,
                forwardCurve,
                fundingCurve,
                new FlatVolatilitySurface(
                        Objects.requireNonNull(forwardCurve, "Forward curve cannot be null.")
                                .valuationTimestampNanos(),
                        volatility
                ),
                dayCountConvention
        );
    }

    /**
     * Creates an equity-option pricer using the funding curve already embedded
     * in the supplied equity forward curve.
     *
     * @param contract European vanilla option contract to value
     * @param forwardCurve equity forward curve supplying both {@code F(T)} and
     * the matching funding curve
     * @param volatilitySurface implied-volatility market data
     * @param dayCountConvention convention used to calculate time to expiry
     */
    public ForwardBlack76(
            OptionContract contract,
            EquityForwardCurve forwardCurve,
            VolatilitySurface volatilitySurface,
            DayCountConvention dayCountConvention
    ) {
        this(
                contract,
                forwardCurve,
                forwardCurve.fundingCurve(),
                volatilitySurface,
                dayCountConvention
        );
    }

    /**
     * Creates an equity-option pricer with a flat volatility surface.
     *
     * @param contract European vanilla option contract to value
     * @param forwardCurve equity forward curve supplying both {@code F(T)} and
     * the matching funding curve
     * @param volatility annualized flat volatility as a decimal
     * @param dayCountConvention convention used to calculate time to expiry
     */
    public ForwardBlack76(
            OptionContract contract,
            EquityForwardCurve forwardCurve,
            double volatility,
            DayCountConvention dayCountConvention
    ) {
        this(
                contract,
                forwardCurve,
                new FlatVolatilitySurface(
                        Objects.requireNonNull(forwardCurve, "Forward curve cannot be null.")
                                .valuationTimestampNanos(),
                        volatility
                ),
                dayCountConvention
        );
    }

    /**
     * Values the configured contract using the curve values at its expiry.
     *
     * <p>At or after expiry, the method uses the forward curve at the valuation
     * timestamp and returns intrinsic value.</p>
     *
     * @return theoretical option value per unit of the underlying
     */
    public double price() {
        long valuationNanos = forwardCurve.valuationTimestampNanos();
        double timeToExpiry = dayCountConvention.timeToExpiry(
                valuationNanos,
                contract.expirationDate()
        );

        long pricingTimestamp = timeToExpiry == 0.0
                ? valuationNanos
                : EpochNanos.from(contract.expirationDate());

        double forwardPrice = forwardCurve.forwardPrice(pricingTimestamp);
        double discountFactor = fundingCurve.discountFactor(pricingTimestamp);
        if (timeToExpiry == 0.0) {
            return priceAtExpiry(
                    contract.type(),
                    forwardPrice,
                    contract.strikePrice(),
                    discountFactor
            );
        }
        double volatility = volatilitySurface.impliedVolatility(
                pricingTimestamp,
                FastMath.log(contract.strikePrice() / forwardPrice)
        );

        return price(
                contract.type(),
                forwardPrice,
                contract.strikePrice(),
                discountFactor,
                volatility,
                timeToExpiry
        );
    }

    /**
     * Prices a European option directly from Black-76 inputs.
     *
     * @param type call or put
     * @param forwardPrice forward price {@code F(T)}; must be positive and finite
     * @param strikePrice strike price {@code K}; must be positive and finite
     * @param discountFactor discount factor {@code DF(T)}; must be positive and finite
     * @param volatility annualized volatility as a decimal; required only when
     * {@code timeToExpiry > 0}
     * @param timeToExpiry non-negative time to expiry in years
     * @return theoretical option value per unit of the underlying
     * @throws IllegalArgumentException if time is invalid, a required numeric
     * input is outside its domain, or {@code timeToExpiry == 0.0} and the
     * discount factor is not one
     */
    public static double price(
            OptionType type,
            double forwardPrice,
            double strikePrice,
            double discountFactor,
            double volatility,
            double timeToExpiry
    ) {
        if (timeToExpiry < 0.0 || !Double.isFinite(timeToExpiry)) {
            throw new IllegalArgumentException(
                    "Time to expiry must be non-negative and finite."
            );
        }
        if (timeToExpiry == 0.0) {
            return priceAtExpiry(type, forwardPrice, strikePrice, discountFactor);
        }

        Objects.requireNonNull(type, "Option type cannot be null.");
        requirePositiveFinite(forwardPrice, "Forward price");
        requirePositiveFinite(strikePrice, "Strike price");
        requirePositiveFinite(discountFactor, "Discount factor");
        PricingValidation.requireValidVolatility(volatility);

        double sqrtTime = FastMath.sqrt(timeToExpiry);
        double volSqrtTime = volatility * sqrtTime;
        double d1 = BlackScholes.computeD1(
                forwardPrice,
                strikePrice,
                timeToExpiry,
                0.0,
                volatility * volatility,
                1.0 / volSqrtTime
        );
        double d2 = d1 - volSqrtTime;

        return switch (type) {
            case CALL -> discountFactor * (forwardPrice * ERF.cdf(d1)
                    - strikePrice * ERF.cdf(d2));
            case PUT -> discountFactor * (strikePrice * ERF.cdf(-d2)
                    - forwardPrice * ERF.cdf(-d1));
        };
    }

    private static double intrinsicValue(
            OptionType type,
            double forwardPrice,
            double strikePrice
    ) {
        return switch (type) {
            case CALL -> FastMath.max(forwardPrice - strikePrice, 0.0);
            case PUT -> FastMath.max(strikePrice - forwardPrice, 0.0);
        };
    }

    private static double priceAtExpiry(
            OptionType type,
            double forwardPrice,
            double strikePrice,
            double discountFactor
    ) {
        Objects.requireNonNull(type, "Option type cannot be null.");
        requirePositiveFinite(forwardPrice, "Forward price");
        requirePositiveFinite(strikePrice, "Strike price");
        requirePositiveFinite(discountFactor, "Discount factor");
        if (Math.abs(discountFactor - 1.0) > EXPIRY_DISCOUNT_FACTOR_TOLERANCE) {
            throw new IllegalArgumentException(
                    "Discount factor must equal one at expiry."
            );
        }
        return intrinsicValue(type, forwardPrice, strikePrice);
    }

    private static void requirePositiveFinite(double value, String label) {
        if (!(value > 0.0) || !Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    label + " must be strictly positive and finite."
            );
        }
    }
}
