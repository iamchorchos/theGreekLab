package math;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.european.BlackScholes;
import com.thegreeklab.math.VolatilityCalculator;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.OptionalDouble;

import static com.thegreeklab.finance.time.DayCountConvention.ACT_365F;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
class LegacyVolatilityCalculatorCompatibilityTest {

    @Test
    void delegatesPublishedVersion21Api() {
        DoubleArrayList prices = new DoubleArrayList(100.0, 101.0, 99.0);
        List<VolatilityCalculator.PriceBar> bars = List.of(
                new VolatilityCalculator.PriceBar(101.0, 99.0),
                new VolatilityCalculator.PriceBar(102.0, 98.0)
        );

        assertEquals(
                com.thegreeklab.math.volatility.VolatilityCalculator.historicalVolatility(
                        prices, 252
                ),
                VolatilityCalculator.historicalVolatility(prices, 252)
        );
        assertEquals(
                com.thegreeklab.math.volatility.VolatilityCalculator.parkinsonsVolatility(
                        bars.stream()
                                .map(bar -> new com.thegreeklab.math.volatility
                                        .VolatilityCalculator.PriceBar(bar.high(), bar.low()))
                                .toList(),
                        252
                ),
                VolatilityCalculator.parkinsonsVolatility(bars, 252)
        );
    }

    @Test
    void delegatesPublishedVersion21ImpliedVolatilityApi() {
        ZonedDateTime valuation = ZonedDateTime.of(
                2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
        );
        OptionContract contract = new OptionContract(
                "TEST", OptionType.CALL, Option.EUROPEAN, 100.0,
                valuation.plusYears(1), 100
        );
        EquityFrame frame = new EquityFrame(valuation, 100.0, 0.05, 0.0);
        double marketPrice = BlackScholes.price(contract, frame, 0.25, ACT_365F);

        OptionalDouble result = VolatilityCalculator.impliedVolatility(
                contract, frame, marketPrice, ACT_365F
        );

        assertTrue(result.isPresent());
        assertEquals(0.25, result.getAsDouble(), 1e-8);
    }
}
