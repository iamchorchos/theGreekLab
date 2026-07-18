package contract;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.exception.InvalidDateException;
import com.thegreeklab.finance.exception.NonPositivePriceException;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OptionContractTest {

    @Test
    void storesExpirationOnce() {
        ZonedDateTime expiry = ZonedDateTime.of(
                2027, 1, 15, 16, 0, 0, 123_456_789, ZoneOffset.UTC
        );

        OptionContract contract = contract(100.0, expiry, 100);

        assertEquals(expiry, contract.expirationDate());
    }

    @Test
    void rejectsBlankSymbol() {
        ZonedDateTime expiry = ZonedDateTime.now(ZoneOffset.UTC).plusYears(1);

        assertThrows(
                IllegalArgumentException.class,
                () -> new OptionContract(" ", OptionType.CALL, Option.EUROPEAN, 100.0, expiry, 100)
        );
    }

    @Test
    void rejectsNullExpiration() {
        assertThrows(
                InvalidDateException.class,
                () -> contract(100.0, null, 100)
        );
    }

    @Test
    void rejectsInvalidTerms() {
        ZonedDateTime expiry = ZonedDateTime.now(ZoneOffset.UTC).plusYears(1);

        assertAll(
                () -> assertThrows(
                        NonPositivePriceException.class,
                        () -> contract(Double.NaN, expiry, 100)
                ),
                () -> assertThrows(
                        NonPositivePriceException.class,
                        () -> contract(100.0, expiry, 0)
                )
        );
    }

    @Test
    void changesOnlyStrike() {
        ZonedDateTime expiry = ZonedDateTime.now(ZoneOffset.UTC).plusYears(1);
        OptionContract original = contract(100.0, expiry, 100);

        OptionContract bumped = original.withStrike(105.0);

        assertAll(
                () -> assertEquals(105.0, bumped.strikePrice(), 0.0),
                () -> assertEquals(original.symbol(), bumped.symbol()),
                () -> assertEquals(original.type(), bumped.type()),
                () -> assertEquals(original.option(), bumped.option()),
                () -> assertEquals(original.expirationDate(), bumped.expirationDate()),
                () -> assertEquals(original.multiplier(), bumped.multiplier())
        );
    }

    private static OptionContract contract(double strike, ZonedDateTime expiry, int multiplier) {
        return new OptionContract(
                "AAPL",
                OptionType.CALL,
                Option.EUROPEAN,
                strike,
                expiry,
                multiplier
        );
    }
}
