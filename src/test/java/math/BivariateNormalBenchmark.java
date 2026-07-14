package math;

import com.thegreeklab.math.BivariateNormal;
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

import java.util.concurrent.TimeUnit;

/**
 * Measures an end-to-end call to the native {@code pbivnorm} routine through
 * the public Panama FFM bridge, including validation, arena allocation,
 * downcall and result extraction.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = "--enable-native-access=ALL-UNNAMED")
public class BivariateNormalBenchmark {

    @State(Scope.Thread)
    public static class InputState {
        @Param({"central", "tail", "correlated"})
        public String scenario;

        private double x;
        private double y;
        private double rho;

        @Setup(Level.Trial)
        public void setup() {
            switch (scenario) {
                case "central" -> {
                    x = 0.25;
                    y = -0.50;
                    rho = 0.0;
                }
                case "tail" -> {
                    x = -3.0;
                    y = -2.5;
                    rho = 0.35;
                }
                case "correlated" -> {
                    x = 1.25;
                    y = 0.75;
                    rho = 0.90;
                }
                default -> throw new IllegalStateException("Unknown scenario: " + scenario);
            }

            BivariateNormal.cdf(x, y, rho);
        }
    }

    @Benchmark
    public double bivariateNormalCdf(InputState state) {
        return BivariateNormal.cdf(state.x, state.y, state.rho);
    }
}
