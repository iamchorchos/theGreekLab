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
![Models](https://img.shields.io/badge/Models-BSM%20%7C%20Black--76%20%7C%20CRR%20%7C%20LR%20%7C%20BS2002-7C3AED)
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
  - Garman-Kohlhagen for FX options
- American option pricing:
  - Cox-Ross-Rubenstein binomial tree
  - Leisen-Reimer binomial tree
  - Bjerksund-Stensland 2002 closed-form approximation
- Native numerical integration:
  - bivariate normal CDF through the Java Foreign Function and Memory API
  - original Fortran `pbivnorm` routine
- Greeks:
  - price, delta, gamma, vega, theta and rho across supported pricing models
  - immutable `StandardGreekValues` snapshots for retrieving them together
  - numerical standard Greeks for Bjerksund-Stensland 2002
  - vanna, volga, charm, speed, lambda
  - dual delta, dual gamma
  - vera, zomma, color, ultima
  - epsilon, veta and parmicharma for supported European models
- Volatility tools:
  - historical close-to-close volatility
  - Parkinson high-low volatility
  - implied volatility via Brent root finding
- Market data abstractions:
  - equity frame
  - futures frame
  - FX frame
- JUnit test suite with numerical cross-validation data

## Requirements

- Java 22+
- Maven, or the included Maven wrapper
- Native `pbivnorm` library for the current platform when using
  Bjerksund-Stensland 2002

The project includes Maven wrapper scripts, so a global Maven installation is
not required.

A Windows x86-64 `pbivnorm.dll` is bundled with the project. GitHub Actions
builds the Linux x86-64 library from `src/main/fortran/pbivnorm.f` before
running Maven. Other platforms can supply an external library through:

```text
-Dthegreeklab.pbivnorm.path=/absolute/path/to/library
```

or the `THEGREEKLAB_PBIVNORM_PATH` environment variable. Applications using
the native CDF should enable native access for the unnamed module:

```text
--enable-native-access=ALL-UNNAMED
```

## Releases

Published versions are available from
[GitHub Releases](https://github.com/iamchorchos/theGreekLab/releases). New
semantic-version tags such as `v1.0.2` are verified automatically and publish:

- the compiled library JAR,
- source and Javadoc JARs,
- SHA-256 checksums for every artifact.

The project is not currently published to Maven Central. For development from
source, use the Maven wrapper; for a released build, download the versioned JAR
and verify it against `SHA256SUMS`.

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

The local HTML report is written to `target/site/jacoco/index.html`. The build
requires at least 85% line coverage and 65% branch coverage. CI also archives
the complete report and uploads `jacoco.xml` to Codacy.

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
.\mvnw.cmd -Pbenchmarks test-compile exec:exec
```

On Linux/macOS:

```bash
./mvnw -Pbenchmarks test-compile exec:exec
```

The model comparison uses a one-year American put with strike 100, spot 95,
a 5% risk-free rate, 3% dividend yield and 25% volatility. The tree models run
with 251 and 1001 steps. Every measured binomial invocation receives a fresh
model so that the benchmark includes tree construction and backward induction
instead of reporting a cached price.
To run only the American-option benchmark:

```powershell
.\mvnw.cmd -Pbenchmarks test-compile exec:exec "-Dbenchmark.include=.*AmericanOptionBenchmark.*"
```

Results are printed to the console and saved as
`target/jmh-result.json`. Run benchmarks outside a debugger on an otherwise
idle machine; absolute timings depend on the JDK, native library, CPU and
operating system.

## Code Quality

The project is configured with:

- Maven Compiler Plugin with Java 22 release target
- JUnit 5 test suite
- JaCoCo XML and HTML coverage reports
- SpotBugs during `mvn verify`
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

BlackScholesMerton model = new BlackScholesMerton(call, frame, 0.22);

double price = model.price();
double delta = model.delta();
double gamma = model.gamma();
double vega = model.vega();
```

Full examples for every supported model and volatility utility are available in
[docs/USAGE.md](docs/USAGE.md).

## Documentation

- [Usage guide](docs/USAGE.md)
- [Mathematical notes](docs/MATH.md)
- [Contributing guide](CONTRIBUTING.md)
- [Security policy](SECURITY.md)

## Project Structure

```text
src/main/java/com/thegreeklab
  finance/contract/           option contract model
  finance/enums/              option type and exercise style enums
  finance/exception/          domain-specific exceptions
  finance/frame/              market-data frames
  finance/model/american/     American option models
    approximations/           Bjerksund-Stensland 2002
    binomial/                 CRR and Leisen-Reimer trees
  finance/model/european/     European option models
  finance/model/greeks/       Greeks interface
  finance/numerical/          numerical utilities
  math/                       volatility, distributions and numerical helpers

src/main/fortran              native pbivnorm source
src/main/resources/native     bundled platform libraries

src/test/java                 unit and cross-validation tests
src/test/resources            numerical reference datasets
```

## Design Notes

- The library separates contract data from market data.
- European models accept only European contracts.
- American binomial models accept only American contracts.
- Bjerksund-Stensland accepts American contracts and exposes price plus
  numerical delta, gamma, vega, theta and rho.
- Market-data frames encode the model-specific cost of carry:
  - `EquityFrame`: `b = r - q`
  - `FuturesFrame`: `b = 0`
  - `FXFrame`: `b = domesticRate - foreignRate`
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
- all 36 Bjerksund-Stensland 2002 values from Haug table 3-2
- bivariate normal identities and perfect-correlation limits
- Bjerksund-Stensland expiry, no-arbitrage bounds and numerical fallback
- Bjerksund-Stensland Greeks in the European limit and American exercise region
- CRR price cross-validation against generated reference data

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
