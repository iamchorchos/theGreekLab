package com.thegreeklab.math;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Cumulative distribution function of a standard bivariate normal distribution.
 *
 * <p>The numerical calculation is delegated to the native {@code pbivnorm}
 * Fortran routine through the Java Foreign Function & Memory API.</p>
 */
public final class BivariateNormal {

    /**
     * System property used to point to an external {@code pbivnorm} library.
     */
    public static final String LIBRARY_PATH_PROPERTY = "thegreeklab.pbivnorm.path";

    /**
     * Environment variable used when {@link #LIBRARY_PATH_PROPERTY} is not set.
     */
    public static final String LIBRARY_PATH_ENVIRONMENT_VARIABLE = "THEGREEKLAB_PBIVNORM_PATH";

    private static final ValueLayout.OfDouble DOUBLE = ValueLayout.JAVA_DOUBLE;
    private static final ValueLayout.OfInt INTEGER = ValueLayout.JAVA_INT;

    private BivariateNormal() {
    }

    /**
     * Returns {@code P(X <= x, Y <= y)} for two standard normal variables with
     * correlation {@code correlation}.
     *
     * @param x           upper limit of the first standard normal variable
     * @param y           upper limit of the second standard normal variable
     * @param correlation correlation coefficient in {@code [-1, 1]}
     * @return probability in {@code [0, 1]}
     * @throws IllegalArgumentException if either limit is {@code NaN}, or if
     *                                  {@code correlation} is not finite or is
     *                                  outside {@code [-1, 1]}
     * @throws ArithmeticException      if the native routine returns a non-finite value
     */
    public static double cdf(double x, double y, double correlation) {
        validateArguments(x, y, correlation);

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment probability = arena.allocate(DOUBLE);
            MemorySegment lower = arena.allocate(DOUBLE, 2);
            MemorySegment upperX = arena.allocate(DOUBLE);
            MemorySegment upperY = arena.allocate(DOUBLE);
            MemorySegment limits = arena.allocate(INTEGER, 2);
            MemorySegment correlations = arena.allocate(DOUBLE);
            MemorySegment length = arena.allocate(INTEGER);

            upperX.set(DOUBLE, 0, x);
            upperY.set(DOUBLE, 0, y);
            limits.setAtIndex(INTEGER, 0, 0);
            limits.setAtIndex(INTEGER, 1, 0);
            correlations.set(DOUBLE, 0, correlation);
            length.set(INTEGER, 0, 1);

            NativeBinding.PBIVNORM.invokeExact(
                    probability,
                    lower,
                    upperX,
                    upperY,
                    limits,
                    correlations,
                    length
            );

            double result = probability.get(DOUBLE, 0);
            if (!Double.isFinite(result)) {
                throw new ArithmeticException("pbivnorm returned a non-finite probability: " + result);
            }
            return Math.clamp(result, 0.0, 1.0);
        } catch (RuntimeException | Error exception) {
            throw exception;
        } catch (Throwable throwable) {
            throw new IllegalStateException("Calling the native pbivnorm routine failed", throwable);
        }
    }

    private static void validateArguments(double x, double y, double correlation) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            throw new IllegalArgumentException("Bivariate normal limits must not be NaN");
        }
        if (!Double.isFinite(correlation) || correlation < -1.0 || correlation > 1.0) {
            throw new IllegalArgumentException("Correlation must be finite and between -1 and 1");
        }
    }

    private static final class NativeBinding {

        private static final MethodHandle PBIVNORM = createHandle();

        private static MethodHandle createHandle() {
            Path library = NativeLibrary.resolve();
            SymbolLookup lookup = SymbolLookup.libraryLookup(library, Arena.global());
            MemorySegment symbol = lookup.find("pbivnorm_")
                    .or(() -> lookup.find("pbivnorm"))
                    .orElseThrow(() -> new UnsatisfiedLinkError(
                            "The native library does not export pbivnorm_ or pbivnorm"
                    ));

            return Linker.nativeLinker().downcallHandle(
                    symbol,
                    FunctionDescriptor.ofVoid(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    )
            );
        }

    }

    private static final class NativeLibrary {

        private static Path resolve() {
            String configuredPath = System.getProperty(LIBRARY_PATH_PROPERTY);
            if (configuredPath == null || configuredPath.isBlank()) {
                configuredPath = System.getenv(LIBRARY_PATH_ENVIRONMENT_VARIABLE);
            }
            if (configuredPath != null && !configuredPath.isBlank()) {
                return requireRegularFile(Path.of(configuredPath).toAbsolutePath().normalize());
            }

            String operatingSystem = operatingSystem();
            String architecture = architecture();
            String fileName = System.mapLibraryName("pbivnorm");
            String resourcePath = "/native/%s-%s/%s".formatted(
                    operatingSystem,
                    architecture,
                    fileName
            );
            try (InputStream resource = BivariateNormal.class.getResourceAsStream(resourcePath)) {
                if (resource == null) {
                    throw new UnsatisfiedLinkError(
                            "No bundled pbivnorm library for " + operatingSystem + "-" + architecture
                                    + ". Set -D" + LIBRARY_PATH_PROPERTY + "=<path> to use an external library."
                    );
                }

                Path extractedLibrary = Files.createTempFile("thegreeklab-pbivnorm-", fileSuffix(fileName));
                Files.copy(resource, extractedLibrary, StandardCopyOption.REPLACE_EXISTING);
                extractedLibrary.toFile().deleteOnExit();
                return extractedLibrary;
            } catch (IOException exception) {
                throw new IllegalStateException("Could not extract the bundled pbivnorm library", exception);
            }
        }

        private static Path requireRegularFile(Path path) {
            if (!Files.isRegularFile(path)) {
                throw new IllegalArgumentException("pbivnorm library does not exist: " + path);
            }
            return path;
        }

        private static String operatingSystem() {
            String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            if (osName.contains("win")) {
                return "windows";
            }
            if (osName.contains("mac") || osName.contains("darwin")) {
                return "macos";
            }
            if (osName.contains("linux")) {
                return "linux";
            }
            throw new UnsupportedOperationException("Unsupported operating system: " + osName);
        }

        private static String architecture() {
            String architecture = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
            return switch (architecture) {
                case "amd64", "x86_64" -> "x86_64";
                case "aarch64", "arm64" -> "aarch64";
                default -> throw new UnsupportedOperationException("Unsupported architecture: " + architecture);
            };
        }

        private static String fileSuffix(String fileName) {
            return fileName.substring(fileName.lastIndexOf('.'));
        }
    }
}
