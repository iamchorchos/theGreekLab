package european;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.european.BlackScholesMerton;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.thegreeklab.finance.time.DayCountConvention.ACT_365F;

/**
 * Invariant-based tests for the generalized Black-Scholes engine.
 *
 * <p>The parameter set is validated against analytical relationships that must
 * hold independently of any third-party implementation: put-call parity,
 * Euler homogeneity and Schwarz cross-derivative symmetry.
 */
class BlackScholesInvariantsTest {

    /**
     * Tolerance for identities that are evaluated using closed-form expressions.
     */
    private static final double TOLERANCE_EXACT = 1e-12;

    /**
     * Tolerance for identities checked through finite-difference bumps.
     */
    private static final double TOLERANCE_APPROX = 2e-6;

    /**
     * Verifies three model invariants over a generated set of market scenarios:
     * put-call parity, Euler homogeneity and Schwarz cross-derivative symmetry.
     *
     * @param args one generated market scenario from the CSV fixture
     */
    @ParameterizedTest(name = "[{index}] S: {0}, K: {1}, T: {2}")
    @CsvFileSource(resources = "/invariants_test_cases.csv", numLinesToSkip = 1)
    void invariants(ArgumentsAccessor args) {

        double S = args.getDouble(0);
        double K = args.getDouble(1);
        double tYears = args.getDouble(2);
        double r = args.getDouble(3);
        double q = args.getDouble(4);
        double sigma = args.getDouble(5);

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        long nanos = (long) (tYears * 365.0 * 86_400.0 * 1_000_000_000L);
        ZonedDateTime expiry = now.plusNanos(nanos);

        OptionContract callContract = new OptionContract("CALL", OptionType.CALL, Option.EUROPEAN, K, expiry, 100);
        OptionContract putContract  = new OptionContract("PUT",  OptionType.PUT,  Option.EUROPEAN, K, expiry, 100);
        EquityFrame frame = new EquityFrame(now, S, r, q);

        BlackScholesMerton callEngine = new BlackScholesMerton(callContract, frame, sigma, ACT_365F);
        BlackScholesMerton putEngine  = new BlackScholesMerton(putContract, frame, sigma, ACT_365F);

        // Put-call parity.
        double callPrice = callEngine.price();
        double putPrice = putEngine.price();
        double leftSide = callPrice - putPrice;
        double rightSide = (S * Math.exp(-q * tYears)) - (K * Math.exp(-r * tYears));

        // Euler homogeneity.
        double reconstructedPrice = (S * callEngine.delta()) + (K * callEngine.dualDelta());

        // Schwarz cross-derivative symmetry.
        double dS = 0.0001;
        double dVol = 0.0001;

        BlackScholesMerton engineSpotUp = new BlackScholesMerton(callContract, new EquityFrame(now, S + dS, r, q), sigma, ACT_365F);
        BlackScholesMerton engineSpotDn = new BlackScholesMerton(callContract, new EquityFrame(now, S - dS, r, q), sigma, ACT_365F);
        BlackScholesMerton engineVolUp = new BlackScholesMerton(callContract, new EquityFrame(now, S, r, q), sigma + dVol, ACT_365F);
        BlackScholesMerton engineVolDn = new BlackScholesMerton(callContract, new EquityFrame(now, S, r, q), sigma - dVol, ACT_365F);

        double dDelta_dVol = (engineVolUp.delta() - engineVolDn.delta()) / (2.0 * dVol);
        double dVega_dSpot = (engineSpotUp.vega() - engineSpotDn.vega()) / (2.0 * dS);

        assertAll(
                () -> assertEquals(rightSide, leftSide, TOLERANCE_EXACT, "Put-Call parity is broken! Check your discount factors."),
                () -> assertEquals(callPrice, reconstructedPrice, TOLERANCE_EXACT, "Euler's theorem failed! Delta and DualDelta don't sum up to the price."),
                () -> assertEquals(dDelta_dVol, dVega_dSpot, TOLERANCE_APPROX, "Schwarz symmetry failed!")
        );
    }
}
