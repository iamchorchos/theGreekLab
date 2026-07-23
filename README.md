# TheGreekLab

![TheGreekLab banner](docs/banner.png)

[![CI](https://img.shields.io/github/actions/workflow/status/iamchorchos/thegreeklab/ci.yml?branch=main&label=CI&logo=github)](https://github.com/iamchorchos/thegreeklab/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-22-blue)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36)
![JUnit](https://img.shields.io/badge/Tests-JUnit%205-25A162)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/44b8c5d347b14242968b22e35d706416)](https://app.codacy.com/gh/iamchorchos/theGreekLab/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Codacy Coverage](https://app.codacy.com/project/badge/Coverage/44b8c5d347b14242968b22e35d706416)](https://app.codacy.com/gh/iamchorchos/theGreekLab/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_coverage)
[![Release](https://img.shields.io/github/v/release/iamchorchos/theGreekLab)](https://github.com/iamchorchos/theGreekLab/releases)
[![License](https://img.shields.io/badge/combined%20distribution-GPL--3.0--or--later-blue)](LICENSE)
[![Download ZIP](https://img.shields.io/badge/Download-ZIP-2EA44F)](https://github.com/iamchorchos/thegreeklab/archive/refs/heads/main.zip)

![Project Type](https://img.shields.io/badge/Project-Java%20Library-0F766E)
![Domain](https://img.shields.io/badge/Domain-Quant%20Finance-1D4ED8)
![Models](https://img.shields.io/badge/Models-BSM%20%7C%20Black--76%20%7C%20CRR%20%7C%20LR%20%7C%20Trinomial%20%7C%20BS2002-7C3AED)
![Native](https://img.shields.io/badge/Native-Java%20Panama%20%2B%20Fortran-2563EB)
![Greeks](https://img.shields.io/badge/Greeks-Analytical%20%2B%20Numerical-B45309)
![Maven Wrapper](https://img.shields.io/badge/Maven%20Wrapper-Included-2EA44F)

TheGreekLab is a Java quantitative finance library for option pricing, Greeks,
volatility estimation and model cross-validation.

The project focuses on clean domain modeling, explicit input validation and
numerical tests for vanilla European and American option models.

## Features

- European option pricing:
  - Black-Scholes-Merton for equity options
  - Black-76 for futures options
  - curve-aware Forward Black-76 for European options priced from forward and
    funding curves
  - Garman-Kohlhagen for FX options
  - deterministic discrete cash-dividend schedules
  - Simple, Haug-Haug, Bos-Gairat-Shepeleva and Bos-Vandermark
    discrete-dividend approximations
- American option pricing:
  - Cox-Ross-Rubenstein binomial tree
  - Leisen-Reimer binomial tree
  - recombining trinomial tree for European and American vanilla options
  - Bjerksund-Stensland 2002 closed-form approximation
- Native numerical integration:
  - bivariate normal CDF through the Java Foreign Function and Memory API
  - original Fortran `pbivnorm` routine
- Greeks:
  - price, delta, gamma, vega, theta and rho across supported pricing models
  - immutable `StandardGreekValues` snapshots for retrieving them together
  - numerical standard Greeks for Bjerksund-Stensland 2002
  - bump-and-revalue standard Greeks for all discrete-dividend approximations
  - node-based delta, gamma and theta plus bumped vega and rho for the
    trinomial tree
  - vanna, volga, charm, speed, lambda
  - dual delta, dual gamma
  - vera, zomma, color, ultima
  - epsilon, veta and parmicharma for supported European models
- Volatility tools:
  - historical close-to-close volatility
  - Parkinson high-low volatility
  - implied volatility via Brent root finding
  - flat implied-volatility surface indexed by expiry and log strike-to-forward
  - optional JavaFX heatmap visualization for sampled volatility surfaces
- Market data abstractions:
  - equity frame
  - futures frame
  - FX frame
  - flat and log-linearly interpolated discount and forward curves
  - nominal funding and dividend-yield curve roles for equity forwards
- JUnit test suite with numerical cross-validation data

## Requirements

- Java 22+
- Maven, or the included Maven wrapper
- Native `pbivnorm` library for the current platform when using
  Bjerksund-Stensland 2002
- JavaFX is required only by the optional `thegreeklab-visualization` module

The project includes Maven wrapper scripts, so a global Maven installation is
not required.

Release artifacts bundle `pbivnorm` for Windows x86-64, Linux x86-64, macOS
x86-64 and macOS Apple Silicon. GitHub Actions builds the Linux and macOS
libraries from `src/main/fortran/pbivnorm.f` and smoke-tests each native
runtime. Other platforms can supply an external library through:

```text
-Dthegreeklab.pbivnorm.path=/absolute/path/to/library
```

or the `THEGREEKLAB_PBIVNORM_PATH` environment variable. Applications using
the native CDF should enable native access for the unnamed module:

```text
--enable-native-access=ALL-UNNAMED
```

## Releases

Starting with version 2.0.1, releases are published to Maven Central under
`io.github.iamchorchos:thegreeklab` and remain available from
[GitHub Releases](https://github.com/iamchorchos/theGreekLab/releases).

Add the library to a Maven project with:

```xml
<dependency>
    <groupId>io.github.iamchorchos</groupId>
    <artifactId>thegreeklab</artifactId>
    <version>2.2.0</version>
</dependency>
```

JavaFX volatility-surface charts are distributed separately, so core pricing
users do not receive a GUI dependency:

```xml
<dependency>
    <groupId>io.github.iamchorchos</groupId>
    <artifactId>thegreeklab-visualization</artifactId>
    <version>2.2.0</version>
</dependency>
```

New semantic-version tags such as `v2.0.0` are verified automatically and
publish:

- signed Maven artifacts to Maven Central,
- the compiled library JAR on GitHub Releases,
- source and Javadoc JARs,
- SHA-256 checksums for every artifact.

Maintainer setup and release instructions are documented in
[docs/PUBLISHING.md](docs/PUBLISHING.md).

## Running Tests

Windows PowerShell:

```powershell
.\mvnw.cmd test
```

Linux/macOS:

```bash
./mvnw test
```

The full test suite contains large numerical datasets, so it may take longer
than a small unit-test-only project.

To run all verification checks and generate the JaCoCo coverage report:

```bash
./mvnw verify
```

The core-library HTML report is written to
`thegreeklab-core/target/site/jacoco/index.html`. The build requires at least
85% line coverage and 65% branch coverage. CI also archives the complete report
and uploads `jacoco.xml` to Codacy.

After a release is available from Maven Central, compare the current public and
protected API with that explicit baseline using japicmp:

Windows PowerShell:

```powershell
.\mvnw.cmd -pl thegreeklab-core verify -Papi-compatibility "-Dapi.baseline.version=2.1.0"
```

Linux/macOS:

```bash
./mvnw -pl thegreeklab-core verify -Papi-compatibility -Dapi.baseline.version=2.1.0
```

The compatibility profile fails on binary- or source-incompatible changes and
writes its reports to `thegreeklab-core/target/japicmp`. Keeping the baseline version explicit
makes local and CI results reproducible. Enable this profile in CI after the
first `2.x` artifact has been published; the intentional `1.x` to `2.x` API
migration is the bootstrap boundary.

## Benchmarks

The JMH suite measures:

- the Bjerksund-Stensland price, each standard Greek and the combined
  `greeks()` snapshot,
- complete Bjerksund-Stensland, Cox-Ross-Rubinstein and Leisen-Reimer
  valuations, including their standard Greeks,
- an end-to-end call to native `pbivnorm` through the public Panama FFM
  bridge.

Run the benchmark profile on Windows:

```powershell
.\mvnw.cmd -pl thegreeklab-core -Pbenchmarks test-compile exec:exec
```

On Linux/macOS:

```bash
./mvnw -pl thegreeklab-core -Pbenchmarks test-compile exec:exec
```

The model comparison uses a one-year American put with strike 100, spot 95,
a 5% risk-free rate, 3% dividend yield and 25% volatility. The tree models run
with 251 and 1001 steps. Every measured binomial invocation receives a fresh
model so that the benchmark includes tree construction and backward induction
instead of reporting a cached price.
To run only the American-option benchmark:

```powershell
.\mvnw.cmd -pl thegreeklab-core -Pbenchmarks test-compile exec:exec "-Dbenchmark.include=.*AmericanOptionBenchmark.*"
```

Results are printed to the console and saved as
`thegreeklab-core/target/jmh-result.json`. Run benchmarks outside a debugger
on an otherwise idle machine; absolute timings depend on the JDK, native
library, CPU and operating system.

## Code Quality

The project is configured with:

- Maven Compiler Plugin with Java 22 release target
- JUnit 5 test suite
- JaCoCo XML and HTML coverage reports
- SpotBugs during `mvn verify`
- japicmp binary and source API checks against an explicit released baseline
- Codacy coverage and quality monitoring
- GitHub Actions CI in `.github/workflows/ci.yml`
- reproducible release artifacts from `.github/workflows/release.yml`

The project itself should be treated primarily as a library.

## Quick Start

Black-Scholes-Merton equity option:

```java
import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.european.BlackScholesMerton;
import com.thegreeklab.finance.time.DayCountConvention;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
ZonedDateTime expiry = now.plusMonths(6);

OptionContract call = new OptionContract(
        "AAPL",
        OptionType.CALL,
        Option.EUROPEAN,
        210.0,
        expiry,
        100
);

EquityFrame frame = new EquityFrame(
        now,
        205.35,
        0.045,
        0.005
);

BlackScholesMerton model = new BlackScholesMerton(
        call,
        frame,
        0.22,
        DayCountConvention.ACT_365F
);

double price = model.price();
double delta = model.delta();
double gamma = model.gamma();
double vega = model.vega();
```

### Version 2 time model

`OptionContract` stores expiration exactly once as a `ZonedDateTime`. Every
contract-based model explicitly selects a `DayCountConvention`, currently
`ACT_365F` or `ACT_360`; day count has no process-global or environment-based
default.
The v1 constructor parameters `expirationNanosEpoch` and
`secondsInExpirationYear` have been removed, eliminating contradictory
expiration metadata.

Full examples for every supported model and volatility utility are available in
[docs/USAGE.md](docs/USAGE.md).

## Documentation

- [Usage guide](docs/USAGE.md)
- [Mathematical notes](docs/MATH.md)
- [Sources and references](docs/REFERENCES.md)
- [Publishing guide](docs/PUBLISHING.md)
- [Changelog](CHANGELOG.md)
- [Contributing guide](CONTRIBUTING.md)
- [Security policy](SECURITY.md)

## Project Structure

```text
src/main/java/com/thegreeklab
  finance/contract/           option contract model
  finance/enums/              option type and exercise style enums
  finance/exception/          domain-specific exceptions
  finance/curves/             discount, forward and role-specific curve types
  finance/frame/              market-data frames
  finance/model/american/     American option models
    approximations/           Bjerksund-Stensland 2002
    binomial/                 CRR and Leisen-Reimer trees
    trinomial/                recombining trinomial tree
  finance/model/european/     European option models
    discrete/                 cash dividends, schedules and adjustment models
  finance/model/greeks/       Greeks interface
  finance/volatility/         implied-volatility surface market data
  finance/numerical/          numerical utilities
  math/                       volatility, distributions and numerical helpers

src/main/fortran              native pbivnorm source
src/main/resources/native     bundled platform libraries

src/test/java                 unit and cross-validation tests
src/test/resources            numerical reference datasets

thegreeklab-core/             Maven artifact for the core library
thegreeklab-visualization/    optional JavaFX visualization artifact
```

## Design Notes

- The library separates contract data from market data.
- European models accept only European contracts.
- Discrete-dividend models require an `EquityFrame` with zero continuous
  dividend yield and include only cash dividends strictly between valuation
  and expiration.
- `DividendSchedule` is immutable and sorts entries chronologically.
- Discrete-dividend approximations expose their adjusted spot, strike and
  volatility, plus immutable bump scenarios and five numerical standard Greeks.
- American binomial models accept only American contracts.
- The trinomial tree accepts European and American vanilla contracts and
  supports immutable scenario repricing through `BumpableOptionModel`.
- Bjerksund-Stensland accepts American contracts and exposes price plus
  numerical delta, gamma, vega, theta and rho.
- Market-data frames encode the model-specific cost of carry:
  - `EquityFrame`: `b = r - q`
  - `FuturesFrame`: `b = 0`
  - `FXFrame`: `b = domesticRate - foreignRate`
- `ForwardBlack76` prices European options from a `ForwardCurve` and a
  `FundingCurve`. Its `EquityForwardCurve` overload derives the funding curve
  from the equity-forward input, so dividend and funding discounting cannot be
  exchanged accidentally.
- `FlatDiscountCurve` preserves the existing scalar-rate behavior.
  `InterpolatedDiscountCurve` and `InterpolatedForwardCurve` preserve supplied
  nodes and use log-linear interpolation without extrapolation. They are market
  data containers, not bootstrapping or curve-calibration engines.
- Curve-aware Forward Black-76 currently exposes price only; curve and
  forward-risk sensitivities require explicit bump and interpolation policies.
- `FlatVolatilitySurface` is the compatibility bridge from scalar volatility to
  the expiry and log-strike-to-forward surface API. Interpolated smile/surface
  construction is intentionally not included yet.
- `thegreeklab-visualization` samples a `VolatilitySurface` into an immutable
  grid and renders it as a JavaFX heatmap. It is an optional consumer of market
  data, not part of the pricing or calibration path.
- Invalid inputs fail fast through domain-specific exceptions.
- Binomial Greeks are finite-difference based and can be sensitive to tree
  depth, bump size and near-zero option values.

## Validation

The test suite covers:

- model validation and invalid input handling
- Black-Scholes invariants
- expiry behavior
- historical volatility
- Parkinson volatility
- implied volatility
- ERF and normal CDF accuracy
- Peizer-Pratt inversion
- American binomial model behavior
- trinomial price and standard-Greek convergence against Black-Scholes-Merton
- trinomial early-exercise behavior and immutable bump operations
- published Haug-Haug and Bos-Vandermark discrete-dividend reference values
- discrete-dividend input adjustments, schedule filtering and numerical Greeks
- all 36 Bjerksund-Stensland 2002 values from Haug table 3-2
- bivariate normal identities and perfect-correlation limits
- Bjerksund-Stensland expiry, no-arbitrage bounds and numerical fallback
- Bjerksund-Stensland Greeks in the European limit and American exercise region
- CRR model behavior, identities and numerical Greeks
- flat and interpolated discount/forward curves, including timestamp, role and
  interpolation-domain validation
- curve-aware Forward Black-76 against both Black-Scholes-Merton flat-curve
  equivalence and direct forward quotes

The publications, table references, fixture provenance and numerical-data
limitations are recorded in [Sources and references](docs/REFERENCES.md).

## Licensing

The combined JAR distribution is provided under
[GPL-3.0-or-later](LICENSE). TheGreekLab uses component-specific licensing:

- original Java code, tests and documentation: [MIT](LICENSES/MIT.txt),
- `pbivnorm.f` and native binaries compiled from it:
  [GPL-2.0-or-later](LICENSES/GPL-2.0.txt),
- distributions combining the Java and native components:
  [GPL-3.0-or-later](LICENSES/GPL-3.0.txt).

The MIT license continues to apply independently to the original Java files.
The native provenance and author attribution are recorded in [NOTICE](NOTICE).
The complete component-level explanation is in
[LICENSING.md](LICENSING.md).
License texts and the notice are also included in built JAR files under
`META-INF`.

## Status

This is a library-oriented quantitative-finance project with independently
cross-validated pricing models and an automated verification pipeline.

It is not financial advice and should not be used for live trading or risk
management without independent validation.
