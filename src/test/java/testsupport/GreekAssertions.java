package testsupport;

import com.thegreeklab.finance.model.greeks.StandardGreeks;
import com.thegreeklab.finance.model.greeks.StandardGreekValues;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Shared assertions for standard-Greek snapshots. */
public final class GreekAssertions {

    private GreekAssertions() {
    }

    /**
     * Verifies that a snapshot matches all individual model accessors.
     *
     * @param model model supplying individual price and Greek values
     * @param snapshot immutable snapshot returned by the model
     * @param tolerance absolute comparison tolerance
     */
    public static void assertGreekSnapshotMatches(
            StandardGreeks model,
            StandardGreekValues snapshot,
            double tolerance
    ) {
        assertAll(
                () -> assertEquals(model.price(), snapshot.price(), tolerance),
                () -> assertEquals(model.delta(), snapshot.delta(), tolerance),
                () -> assertEquals(model.gamma(), snapshot.gamma(), tolerance),
                () -> assertEquals(model.vega(), snapshot.vega(), tolerance),
                () -> assertEquals(model.theta(), snapshot.theta(), tolerance),
                () -> assertEquals(model.rho(), snapshot.rho(), tolerance)
        );
    }
}
