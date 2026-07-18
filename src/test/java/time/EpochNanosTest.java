package time;

import com.thegreeklab.finance.exception.InvalidDateException;
import com.thegreeklab.finance.time.EpochNanos;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EpochNanosTest {

    @Test
    void convertsTimestampExactly() {
        ZonedDateTime timestamp = ZonedDateTime.of(
                2026, 7, 11, 12, 30, 15, 123_456_789, ZoneOffset.UTC
        );

        long nanos = EpochNanos.from(timestamp);

        assertEquals(timestamp, EpochNanos.toUtc(nanos));
    }

    @Test
    void handlesPreEpochTimestamp() {
        ZonedDateTime timestamp = ZonedDateTime.of(
                1960, 1, 2, 3, 4, 5, 987_654_321, ZoneOffset.UTC
        );

        assertEquals(timestamp, EpochNanos.toUtc(EpochNanos.from(timestamp)));
    }

    @Test
    void rejectsNullTimestamp() {
        assertThrows(InvalidDateException.class, () -> EpochNanos.from(null));
    }
}
