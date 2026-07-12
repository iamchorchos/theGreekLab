package frame;

import com.thegreeklab.finance.exception.InvalidDateException;
import com.thegreeklab.finance.exception.InvalidRateException;
import com.thegreeklab.finance.exception.NonPositivePriceException;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.frame.FXFrame;
import com.thegreeklab.finance.frame.FuturesFrame;
import com.thegreeklab.finance.frame.MarketData;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarketDataFrameTest {

    @Test
    void equityFrameComputesCostOfCarryAndBumpsFields() {
        EquityFrame frame = new EquityFrame(123L, 100.0, 0.05, 0.02);

        EquityFrame rateBumped = frame.withRiskFreeRate(0.06);
        EquityFrame spotBumped = frame.withSpotPrice(101.0);
        EquityFrame timeBumped = frame.withTimestampNanos(456L);

        assertAll(
                () -> assertEquals(0.03, frame.costOfCarry(), 1e-15),
                () -> assertEquals(0.04, rateBumped.costOfCarry(), 1e-15),
                () -> assertEquals(0.02, rateBumped.dividendYield(), 1e-15),
                () -> assertEquals(101.0, spotBumped.spotPrice(), 1e-15),
                () -> assertEquals(456L, timeBumped.timestampNanos())
        );
    }

    @Test
    void fxFrameComputesCostOfCarryAndUsesDomesticRateAsRiskFreeRate() {
        FXFrame frame = new FXFrame(123L, 1.10, 0.05, 0.03);

        FXFrame rateBumped = frame.withRiskFreeRate(0.06);

        assertAll(
                () -> assertEquals(0.05, frame.riskFreeRate(), 1e-15),
                () -> assertEquals(0.02, frame.costOfCarry(), 1e-15),
                () -> assertEquals(0.03, rateBumped.costOfCarry(), 1e-15),
                () -> assertEquals(0.03, rateBumped.foreignRate(), 1e-15)
        );
    }

    @Test
    void futuresFrameUsesFuturesPriceAsSpotAndHasZeroCostOfCarry() {
        FuturesFrame frame = new FuturesFrame(123L, 2500.0, 0.04);

        FuturesFrame rateBumped = frame.withRiskFreeRate(0.05);
        FuturesFrame spotBumped = frame.withSpotPrice(2510.0);

        assertAll(
                () -> assertEquals(2500.0, frame.spotPrice(), 1e-15),
                () -> assertEquals(0.0, frame.costOfCarry(), 0.0),
                () -> assertEquals(0.0, rateBumped.costOfCarry(), 0.0),
                () -> assertEquals(2510.0, spotBumped.futuresPrice(), 1e-15)
        );
    }

    @Test
    void framesRejectInvalidPricesAndRates() {
        assertAll(
                () -> assertThrows(NonPositivePriceException.class, () -> new EquityFrame(1L, 0.0, 0.05, 0.01)),
                () -> assertThrows(NonPositivePriceException.class, () -> new FXFrame(1L, Double.NaN, 0.05, 0.01)),
                () -> assertThrows(NonPositivePriceException.class, () -> new FuturesFrame(1L, -1.0, 0.05)),
                () -> assertThrows(InvalidRateException.class, () -> new EquityFrame(1L, 100.0, Double.POSITIVE_INFINITY, 0.01)),
                () -> assertThrows(InvalidRateException.class, () -> new FXFrame(1L, 1.10, 0.05, Double.NaN)),
                () -> assertThrows(InvalidRateException.class, () -> new FuturesFrame(1L, 2500.0, Double.NEGATIVE_INFINITY))
        );
    }

    @Test
    void convertsZonedDateTimeToEpochNanos() {
        ZonedDateTime timestamp = ZonedDateTime.of(2026, 7, 11, 12, 30, 15, 123_456_789, ZoneOffset.UTC);

        long expected = timestamp.toInstant().getEpochSecond() * 1_000_000_000L + timestamp.getNano();

        assertEquals(expected, MarketData.toEpochNanos(timestamp));
    }

    @Test
    void rejectsNullTimestampConversion() {
        assertThrows(InvalidDateException.class, () -> MarketData.toEpochNanos(null));
    }
}
