package contract;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.exception.InvalidDateException;
import com.thegreeklab.finance.exception.NonPositivePriceException;
import com.thegreeklab.finance.frame.MarketData;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OptionContractTest {

    @Test
    void derivesEpochNanos() {
        ZonedDateTime expiry = ZonedDateTime.of(2027, 1, 15, 16, 0, 0, 123_456_789, ZoneOffset.UTC);

        OptionContract contract = new OptionContract(
                "AAPL",
                OptionType.CALL,
                Option.EUROPEAN,
                100.0,
                expiry,
                100
        );

        assertEquals(MarketData.toEpochNanos(expiry), contract.expirationNanosEpoch());
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
                () -> new OptionContract("AAPL", OptionType.CALL, Option.EUROPEAN, 100.0, null, 100)
        );
    }

    @Test
    void rejectsInvalidTerms() {
        ZonedDateTime expiry = ZonedDateTime.now(ZoneOffset.UTC).plusYears(1);

        assertThrows(
                NonPositivePriceException.class,
                () -> new OptionContract("AAPL", OptionType.CALL, Option.EUROPEAN, Double.NaN, expiry, 100)
        );
        assertThrows(
                NonPositivePriceException.class,
                () -> new OptionContract("AAPL", OptionType.CALL, Option.EUROPEAN, 100.0, expiry, 0)
        );
    }

    @Test
    void rejectsYearSeconds() {
        ZonedDateTime expiry = ZonedDateTime.now(ZoneOffset.UTC).plusYears(1);

        assertThrows(
                InvalidDateException.class,
                () -> new OptionContract(
                        "AAPL",
                        OptionType.CALL,
                        Option.EUROPEAN,
                        100.0,
                        expiry,
                        100,
                        MarketData.toEpochNanos(expiry),
                        0.0
                )
        );
    }

    @Test
    void floorsExpiryAtZero() {
        ZonedDateTime expiry = ZonedDateTime.now(ZoneOffset.UTC);
        long expiryNanos = MarketData.toEpochNanos(expiry);
        OptionContract contract = new OptionContract(
                "AAPL",
                OptionType.CALL,
                Option.EUROPEAN,
                100.0,
                expiry,
                100,
                expiryNanos,
                365.0 * 86_400.0
        );

        assertEquals(0.0, contract.getTimeToExpiry(expiryNanos + 1L), 0.0);
    }

    @Test
    void preservesExpiryMetadata() {
        ZonedDateTime expiry = ZonedDateTime.now(ZoneOffset.UTC).plusYears(1);
        long expiryNanos = MarketData.toEpochNanos(expiry);
        OptionContract contract = new OptionContract(
                "AAPL",
                OptionType.CALL,
                Option.EUROPEAN,
                100.0,
                expiry,
                100,
                expiryNanos,
                365.0 * 86_400.0
        );

        OptionContract bumped = contract.withStrike(105.0);

        assertEquals(105.0, bumped.strikePrice(), 0.0);
        assertEquals(expiryNanos, bumped.expirationNanosEpoch());
        assertEquals(contract.secondsInExpirationYear(), bumped.secondsInExpirationYear(), 0.0);
    }
}
