package time;

import com.thegreeklab.finance.exception.InvalidDateException;
import com.thegreeklab.finance.time.DayCountConvention;
import com.thegreeklab.finance.time.EpochNanos;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DayCountConventionTest {

    private static final DayCountConvention ACT_365F = DayCountConvention.ACT_365F;
    private static final double TOLERANCE = 1e-15;

    @Test
    void usesFixed365DayYear() {
        ZonedDateTime start = ZonedDateTime.of(2023, 7, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime end = start.plusYears(1);

        double actual = ACT_365F.timeToExpiry(EpochNanos.from(start), end);

        assertEquals(366.0 / 365.0, actual, TOLERANCE);
    }

    @Test
    void usesFixed360DayYear() {
        ZonedDateTime start = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime end = start.plusDays(360);

        double actual = DayCountConvention.ACT_360.timeToExpiry(EpochNanos.from(start), end);

        assertEquals(1.0, actual, TOLERANCE);
    }

    @Test
    void calculatesSignedFraction() {
        long start = 10L * 86_400L * 1_000_000_000L;
        long end = 9L * 86_400L * 1_000_000_000L;

        assertEquals(-1.0 / 365.0, ACT_365F.yearFraction(start, end), TOLERANCE);
    }

    @Test
    void floorsExpiredContractAtZero() {
        ZonedDateTime expiry = ZonedDateTime.of(2027, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        long afterExpiry = EpochNanos.from(expiry.plusNanos(1));

        assertEquals(0.0, ACT_365F.timeToExpiry(afterExpiry, expiry), 0.0);
    }

    @Test
    void rejectsNullExpiration() {
        assertThrows(InvalidDateException.class, () -> ACT_365F.timeToExpiry(0L, null));
    }
}
