# TheGreekLab

![TheGreekLab banner](docs/banner.png)

[![CI](https://img.shields.io/github/actions/workflow/status/iamchorchos/TheGreekLab/ci.yml?branch=main&label=CI&logo=github)](https://github.com/iamchorchos/TheGreekLab/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-blue)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36)
![JUnit](https://img.shields.io/badge/Tests-JUnit%205-25A162)
![Code Quality](https://img.shields.io/badge/Code%20Quality-SpotBugs%20%2B%20Qodana-6B57FF)
![License](https://img.shields.io/badge/License-MIT-green)
[![Download ZIP](https://img.shields.io/badge/Download-ZIP-2EA44F)](https://github.com/iamchorchos/TheGreekLab/archive/refs/heads/main.zip)

![Project Type](https://img.shields.io/badge/Project-Java%20Library-0F766E)
![Domain](https://img.shields.io/badge/Domain-Quant%20Finance-1D4ED8)
![Models](https://img.shields.io/badge/Models-Black--Scholes%20%7C%20Black--76%20%7C%20CRR%20%7C%20LR-7C3AED)
![Greeks](https://img.shields.io/badge/Greeks-Analytical%20%2B%20Binomial-B45309)
![Maven Wrapper](https://img.shields.io/badge/Maven%20Wrapper-Included-2EA44F)
![Status](https://img.shields.io/badge/Status-Portfolio%20Ready-111827)

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
- Greeks:
  - price
  - delta, gamma, vega, theta, rho
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

- Java 21+
- Maven, or the included Maven wrapper

The project includes Maven wrapper scripts, so a global Maven installation is
not required.

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

## Code Quality

The project is configured with:

- Maven Compiler Plugin with Java 21 release target
- JUnit 5 test suite
- SpotBugs during `mvn verify`
- Qodana configuration in `qodana.yaml`
- GitHub Actions CI in `.github/workflows/ci.yml`

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

## Project Structure

```text
src/main/java/com/thegreeklab
  finance/contract/           option contract model
  finance/enums/              option type and exercise style enums
  finance/exception/          domain-specific exceptions
  finance/frame/              market-data frames
  finance/model/american/     American option models
  finance/model/european/     European option models
  finance/model/greeks/       Greeks interface
  finance/numerical/          numerical utilities
  math/                       volatility, ERF, Peizer-Pratt inversion

src/test/java                 unit and cross-validation tests
src/test/resources            numerical reference datasets
```

## Design Notes

- The library separates contract data from market data.
- European models accept only European contracts.
- American binomial models accept only American contracts.
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
- CRR price cross-validation against generated reference data

## Status

This is a library-oriented project suitable for experimentation, learning and
portfolio presentation in quantitative finance software engineering.

It is not financial advice and should not be used for live trading or risk
management without independent validation.
# TheGreekLab

