package european;

import com.thegreeklab.finance.model.european.discrete.CashDividend;
import com.thegreeklab.finance.model.european.discrete.DividendSchedule;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DividendScheduleTest {

    @Test
    void acceptsEmptySchedule() {
        DividendSchedule schedule = new DividendSchedule(List.of());

        assertEquals(List.of(), schedule.dividends());
    }

    @Test
    void sortsAndCopies() {
        CashDividend later = new CashDividend(20L, 2.0);
        CashDividend earlier = new CashDividend(10L, 1.0);
        List<CashDividend> source = new ArrayList<>(List.of(later, earlier));

        DividendSchedule schedule = new DividendSchedule(source);
        source.clear();

        assertAll(
                () -> assertEquals(List.of(earlier, later), schedule.dividends()),
                () -> assertEquals(2, schedule.dividends().size())
        );
    }

    @Test
    @SuppressWarnings("DataFlowIssue") // The mutation attempt intentionally verifies the unmodifiable view.
    void listIsImmutable() {
        DividendSchedule schedule = new DividendSchedule(
                List.of(new CashDividend(10L, 1.0))
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> schedule.dividends().add(new CashDividend(20L, 2.0))
        );
    }

    @Test
    void rejectsNulls() {
        NullPointerException nullList = assertThrows(
                NullPointerException.class,
                () -> new DividendSchedule(null)
        );
        NullPointerException nullEntry = assertThrows(
                NullPointerException.class,
                () -> new DividendSchedule(java.util.Arrays.asList(
                        new CashDividend(10L, 1.0),
                        null
                ))
        );

        assertAll(
                () -> assertEquals("Dividends cannot be null.", nullList.getMessage()),
                () -> assertEquals("Dividend cannot be null.", nullEntry.getMessage())
        );
    }
}
