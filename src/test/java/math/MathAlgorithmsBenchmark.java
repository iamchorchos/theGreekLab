package math;

import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.math.ERF;
import com.thegreeklab.math.PeizerPrattInversion;
import com.thegreeklab.math.VolatilityCalculator;
import org.eclipse.collections.api.list.primitive.DoubleList;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 20, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class MathAlgorithmsBenchmark {

    @State(Scope.Thread)
    public static class ErfState {
        @Param({"-3.5", "0.0", "0.46875", "4.0"})
        public double x;
    }

    @State(Scope.Thread)
    public static class PeizerState {
        @Param({"-3.5", "0.0", "4.0"})
        public double x;

        @Param({"50", "1000"})
        public int steps;
    }

    @State(Scope.Thread)
    public static class HistoricalVolState {
        public DoubleList prices;
        public int tradingDays = 252;

        @Setup(Level.Trial)
        public void setup() {
            DoubleArrayList list = new DoubleArrayList(252);
            double price = 100.0;
            for (int i = 0; i < 252; i++) {
                list.add(price);
                price *= 1.001;
            }
            this.prices = list;
        }
    }

    @State(Scope.Thread)
    public static class ParkinsonsVolState {
        public List<VolatilityCalculator.PriceBar> bars;
        public int tradingDays = 252;

        @Setup(Level.Trial)
        public void setup() {
            bars = new ArrayList<>(252);
            for (int i = 0; i < 252; i++) {
                bars.add(new VolatilityCalculator.PriceBar(105.0, 95.0));
            }
        }
    }

    @State(Scope.Thread)
    public static class ImpliedVolState {
        public OptionContract contract;
        public EquityFrame frame;
        public double marketPrice = 10.45;

        @Setup(Level.Trial)
        public void setup() {
            ZonedDateTime now = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            ZonedDateTime expiry = now.plusYears(1);

            this.contract = new OptionContract("TEST", OptionType.CALL, Option.EUROPEAN, 100.0, expiry, 1);
            this.frame = new EquityFrame(now, 100.0, 0.05, 0.0);
        }
    }

    @Benchmark
    public void benchmarkErfc(ErfState state, Blackhole bh) {
        bh.consume(ERF.erfc(state.x));
    }

    @Benchmark
    public void benchmarkPdf(ErfState state, Blackhole bh) {
        bh.consume(ERF.pdf(state.x));
    }

    @Benchmark
    public void benchmarkPeizerPratt(PeizerState state, Blackhole bh) {
        bh.consume(PeizerPrattInversion.inverseFunction(state.x, state.steps));
    }

    @Benchmark
    public void benchmarkHistoricalVolatility(HistoricalVolState state, Blackhole bh) {
        bh.consume(VolatilityCalculator.historicalVolatility(state.prices, state.tradingDays));
    }

    @Benchmark
    public void benchmarkParkinsonsVolatility(ParkinsonsVolState state, Blackhole bh) {
        bh.consume(VolatilityCalculator.parkinsonsVolatility(state.bars, state.tradingDays));
    }

    @Benchmark
    public void benchmarkImpliedVolatility(ImpliedVolState state, Blackhole bh) {
        bh.consume(VolatilityCalculator.impliedVolatility(state.contract, state.frame, state.marketPrice));
    }
}