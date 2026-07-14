# TheGreekLab Usage Guide

This guide shows the main public API paths for TheGreekLab: contract creation,
market data frames, European models, American models, Greeks, native numerical
integration and volatility utilities.

## Core Concepts

The library separates option contracts from market data.

`OptionContract` describes the instrument:

- symbol
- call or put type
- European or American exercise style
- strike
- expiration
- multiplier

`MarketData` implementations describe the pricing context:

- `EquityFrame` for stock or index options
- `FuturesFrame` for futures options
- `FXFrame` for currency options

## Supported Greeks

The common `Greeks` interface exposes:

- `price()`
- `delta()`
- `gamma()`
- `vega()`
- `theta()`
- `rho()`
- `vanna()`
- `volga()`
- `charm()`
- `speed()`
- `lambda()`
- `dualDelta()`
- `dualGamma()`
- `vera()`
- `zomma()`
- `color()`
- `ultima()`

European Black-Scholes-style models also expose:

- `epsilon()`
- `veta()`
- `parmicharma()`

`Black76` intentionally does not support `epsilon()`, because the model takes
the futures price directly and has no dividend-yield or foreign-rate input.

## Black-Scholes-Merton Equity Option

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
double theta = model.theta();
double rho = model.rho();
double epsilon = model.epsilon();
double veta = model.veta();
double parmicharma = model.parmicharma();
```

## Garman-Kohlhagen FX Option

```java
import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.FXFrame;
import com.thegreeklab.finance.model.european.GarmanKohlhagen;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
ZonedDateTime expiry = now.plusMonths(6);

OptionContract fxCall = new OptionContract(
        "EURUSD",
        OptionType.CALL,
        Option.EUROPEAN,
        1.09,
        expiry,
        100_000
);

FXFrame frame = new FXFrame(
        now,
        1.0850,
        0.045,
        0.032
);

GarmanKohlhagen model = new GarmanKohlhagen(fxCall, frame, 0.115);

double price = model.price();
double delta = model.delta();
double rho = model.rho();
double epsilon = model.epsilon();
```

## Black-76 Futures Option

```java
import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.FuturesFrame;
import com.thegreeklab.finance.model.european.Black76;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
ZonedDateTime expiry = now.plusMonths(6);

OptionContract futuresPut = new OptionContract(
        "ES",
        OptionType.PUT,
        Option.EUROPEAN,
        5150.0,
        expiry,
        50
);

FuturesFrame frame = new FuturesFrame(
        now,
        5125.0,
        0.042
);

Black76 model = new Black76(futuresPut, frame, 0.185);

double price = model.price();
double delta = model.delta();
double theta = model.theta();
double rho = model.rho();
```

## Cox-Ross-Rubenstein American Option

```java
import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.american.binomial.CoxRossRubenstein;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
ZonedDateTime expiry = now.plusMonths(6);

OptionContract put = new OptionContract(
        "AAPL",
        OptionType.PUT,
        Option.AMERICAN,
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

CoxRossRubenstein model = new CoxRossRubenstein(put, frame, 0.22, 301);

double price = model.price();
double delta = model.delta();
double gamma = model.gamma();
double vega = model.vega();
double rho = model.rho();
```

## Leisen-Reimer American Option

```java
import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.american.binomial.LeisenReimer;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
ZonedDateTime expiry = now.plusMonths(6);

OptionContract call = new OptionContract(
        "AAPL",
        OptionType.CALL,
        Option.AMERICAN,
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

LeisenReimer model = new LeisenReimer(call, frame, 0.22, 301);

double price = model.price();
double delta = model.delta();
double gamma = model.gamma();
double vega = model.vega();
double rho = model.rho();
```

## Bjerksund-Stensland 2002 American Option

`BjerksundStensland` is a closed-form approximation for vanilla American
options. It supports every `MarketData` implementation through the generalized
cost-of-carry parameter and returns the option price. American puts are handled
internally through put-call symmetry.

```java
import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.american.approximations.BjerksundStensland;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
ZonedDateTime expiry = now.plusMonths(6);

OptionContract put = new OptionContract(
        "AAPL",
        OptionType.PUT,
        Option.AMERICAN,
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

BjerksundStensland model = new BjerksundStensland(put, frame, 0.22);

double price = model.price();
```

The approximation enforces the European and intrinsic-value lower bounds. If
the analytical exercise boundary becomes non-finite, it returns that
no-arbitrage lower bound instead of propagating `NaN`.

## Native pbivnorm Configuration

Bjerksund-Stensland 2002 uses `BivariateNormal.cdf()`. The Java implementation
calls the Fortran `pbivnorm` routine through the Java Foreign Function and
Memory API.

The CDF can also be used directly:

```java
import com.thegreeklab.math.BivariateNormal;

double probability = BivariateNormal.cdf(0.25, -0.50, 0.60);
```

Windows x86-64 uses the bundled library:

```text
src/main/resources/native/windows-x86_64/pbivnorm.dll
```

On Linux x86-64, build the library before running the application or tests:

```bash
mkdir -p src/main/resources/native/linux-x86_64
gfortran -shared -fPIC -O2 -std=legacy -ffixed-line-length-none \
  src/main/fortran/pbivnorm.f \
  -o src/main/resources/native/linux-x86_64/libpbivnorm.so
```

An external library can be selected with a JVM property:

```bash
java --enable-native-access=ALL-UNNAMED \
  -Dthegreeklab.pbivnorm.path=/absolute/path/to/libpbivnorm.so \
  -jar application.jar
```

or with an environment variable:

```text
THEGREEKLAB_PBIVNORM_PATH=/absolute/path/to/library
```

The external file must export either `pbivnorm_` (the usual GNU Fortran name)
or `pbivnorm`. macOS and ARM64 currently require an external build because no
matching binary is bundled.

### Native component license

The Java component is licensed under MIT. The Fortran source and native
libraries are licensed under GPL-2.0-or-later. A distribution combining both
components is provided under GPL-3.0-or-later. See `LICENSE` for the combined
distribution, `LICENSING.md` for the component-level explanation, and
`NOTICE` plus the `LICENSES` directory for complete terms and attribution.

## Historical Volatility

```java
import com.thegreeklab.math.VolatilityCalculator;
import org.eclipse.collections.api.list.primitive.DoubleList;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;

DoubleList prices = new DoubleArrayList(new double[]{
        198.40, 199.25, 201.10, 200.35, 202.80, 204.15
});

double volatility = VolatilityCalculator.historicalVolatility(prices, 252);
```

## Parkinson Volatility

```java
import com.thegreeklab.math.VolatilityCalculator;

import java.util.List;

List<VolatilityCalculator.PriceBar> bars = List.of(
        new VolatilityCalculator.PriceBar(200.20, 197.80),
        new VolatilityCalculator.PriceBar(201.00, 198.90),
        new VolatilityCalculator.PriceBar(202.40, 199.70)
);

double volatility = VolatilityCalculator.parkinsonsVolatility(bars, 252);
```

## Implied Volatility

```java
import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.math.VolatilityCalculator;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.OptionalDouble;

ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
ZonedDateTime expiry = now.plusMonths(6);

OptionContract contract = new OptionContract(
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

double marketPrice = 8.50;

var impliedVolatility =
        VolatilityCalculator.impliedVolatility(contract, frame, marketPrice);
```

The implied volatility solver supports European options.
