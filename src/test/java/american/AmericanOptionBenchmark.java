package american;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.american.approximations.BjerksundStensland;
import com.thegreeklab.finance.model.american.binomial.CoxRossRubenstein;
import com.thegreeklab.finance.model.american.binomial.LeisenReimer;
import com.thegreeklab.finance.model.greeks.StandardGreekValues;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import static com.thegreeklab.finance.time.DayCountConvention.ACT_365F;

/**
 * Measures Bjerksund-Stensland pricing and Greeks and compares the approximation
 * with fresh Cox-Ross-Rubinstein and Leisen-Reimer trees.
 *
 * <p>The comparison states construct new models for every invocation. This is
 * intentional: binomial models cache their calculated trees, so reusing an
 * instance would measure a cache lookup instead of a complete valuation.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 1, jvmArgsAppend = "--enable-native-access=ALL-UNNAMED")
public class AmericanOptionBenchmark {

    @State(Scope.Thread)
    public static class BjerksundState {
        private BjerksundStensland model;

        @Setup(Level.Trial)
        public void setup() {
            MarketScenario scenario = MarketScenario.standard();
            model = scenario.bjerksund();
        }
    }

    @State(Scope.Thread)
    public static class CrrState {
        @Param({"251", "1001"})
        public int steps;

        private MarketScenario scenario;
        private CoxRossRubenstein crr;

        @Setup(Level.Trial)
        public void setupScenario() {
            scenario = MarketScenario.standard();
        }

        @Setup(Level.Invocation)
        public void setupModel() {
            crr = scenario.crr(steps);
        }
    }

    @State(Scope.Thread)
    public static class LeisenReimerState {
        @Param({"251", "1001"})
        public int steps;

        private MarketScenario scenario;
        private LeisenReimer leisenReimer;

        @Setup(Level.Trial)
        public void setupScenario() {
            scenario = MarketScenario.standard();
        }

        @Setup(Level.Invocation)
        public void setupModel() {
            leisenReimer = scenario.leisenReimer(steps);
        }
    }

    @Benchmark
    public double comparisonBjerksundPrice(BjerksundState state) {
        return state.model.price();
    }

    @Benchmark
    public double bjerksundDelta(BjerksundState state) {
        return state.model.delta();
    }

    @Benchmark
    public double bjerksundGamma(BjerksundState state) {
        return state.model.gamma();
    }

    @Benchmark
    public double bjerksundVega(BjerksundState state) {
        return state.model.vega();
    }

    @Benchmark
    public double bjerksundTheta(BjerksundState state) {
        return state.model.theta();
    }

    @Benchmark
    public double bjerksundRho(BjerksundState state) {
        return state.model.rho();
    }

    @Benchmark
    public StandardGreekValues comparisonBjerksundGreeks(BjerksundState state) {
        return state.model.greeks();
    }

    @Benchmark
    public double comparisonCrrPrice(CrrState state) {
        return state.crr.price();
    }

    @Benchmark
    public double comparisonLeisenReimerPrice(LeisenReimerState state) {
        return state.leisenReimer.price();
    }

    @Benchmark
    public StandardGreekValues comparisonCrrGreeks(CrrState state) {
        return state.crr.greeks();
    }

    @Benchmark
    public StandardGreekValues comparisonLeisenReimerGreeks(LeisenReimerState state) {
        return state.leisenReimer.greeks();
    }

    private record MarketScenario(
            OptionContract contract,
            EquityFrame frame,
            double volatility
    ) {
        private static MarketScenario standard() {
            ZonedDateTime valuation = ZonedDateTime.of(
                    2026, 1, 16, 12, 0, 0, 0, ZoneOffset.UTC
            );
            OptionContract contract = new OptionContract(
                    "BENCH", OptionType.PUT, Option.AMERICAN, 100.0,
                    valuation.plusYears(1), 100
            );
            EquityFrame frame = new EquityFrame(valuation, 95.0, 0.05, 0.03);
            return new MarketScenario(contract, frame, 0.25);
        }

        private BjerksundStensland bjerksund() {
            return new BjerksundStensland(contract, frame, volatility, ACT_365F);
        }

        private CoxRossRubenstein crr(int steps) {
            return new CoxRossRubenstein(contract, frame, volatility, steps, ACT_365F);
        }

        private LeisenReimer leisenReimer(int steps) {
            return new LeisenReimer(contract, frame, volatility, steps, ACT_365F);
        }
    }
}
