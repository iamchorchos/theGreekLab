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

### Expiration and day count

Version 2 represents contract expiration only through
`OptionContract.expirationDate()`. Do not precompute an epoch timestamp or a
year denominator when constructing a contract.

Every contract-based pricing engine and implied-volatility calculation requires
the caller to select `DayCountConvention.ACT_365F` or
`DayCountConvention.ACT_360`. There is no day-count default and the setting is
preserved by immutable bumped copies.

For ACT/365F:

```math
T = \frac{t_{expiry} - t_{valuation}}{365 \cdot 86\,400}
```

The numerator is actual elapsed time in seconds. Consequently, an interval
containing 366 actual days has a year fraction of `366 / 365`. At and after
expiration, time to expiry is floored at zero. ACT/360 uses the same actual
elapsed time with a fixed 360-day denominator.

## Supported Greeks

The `StandardGreeks` interface exposes:

- `price()`
- `delta()`
- `gamma()`
- `vega()`
- `theta()`
- `rho()`
- `greeks()`, returning an immutable `StandardGreekValues` snapshot

`BjerksundStensland` implements this standard surface with numerical
bump-and-revalue estimates. `TrinomialTree` obtains delta, gamma and theta
directly from tree nodes and uses immutable bumped copies for vega and rho.
The full `Greeks` interface extends `StandardGreeks` with:

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
        call, frame, 0.22, DayCountConvention.ACT_365F
);

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
import com.thegreeklab.finance.time.DayCountConvention;

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

GarmanKohlhagen model = new GarmanKohlhagen(
        fxCall, frame, 0.115, DayCountConvention.ACT_365F
);

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
import com.thegreeklab.finance.time.DayCountConvention;

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

Black76 model = new Black76(
        futuresPut, frame, 0.185, DayCountConvention.ACT_365F
);

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
import com.thegreeklab.finance.time.DayCountConvention;

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

CoxRossRubenstein model = new CoxRossRubenstein(
        put, frame, 0.22, 301, DayCountConvention.ACT_365F
);

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
import com.thegreeklab.finance.time.DayCountConvention;

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

LeisenReimer model = new LeisenReimer(
        call, frame, 0.22, 301, DayCountConvention.ACT_365F
);

double price = model.price();
double delta = model.delta();
double gamma = model.gamma();
double vega = model.vega();
double rho = model.rho();
```

## Trinomial Tree

`TrinomialTree` is a recombining tree for European and American vanilla
options. It supports every `MarketData` implementation through the generalized
cost-of-carry parameter. American contracts are checked for early exercise at
every node; European contracts use continuation value only.

```java
import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.american.trinomial.TrinomialTree;
import com.thegreeklab.finance.time.DayCountConvention;

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

TrinomialTree model = new TrinomialTree(
        put, frame, 0.22, 301, DayCountConvention.ACT_365F
);

double price = model.price();
double delta = model.delta();
double gamma = model.gamma();
double theta = model.theta();
double vega = model.vega();
double rho = model.rho();

var values = model.greeks();
```

The model is immutable and thread-safe. Its bump methods create independent
models, leaving the original valuation unchanged:

```java
TrinomialTree spotScenario = model.withSpot(210.0);
TrinomialTree volatilityScenario = model.withVolatility(0.24);
TrinomialTree rateScenario = model.withRiskFreeRate(0.05);
long oneDayNanos = 86_400_000_000_000L;
TrinomialTree tomorrow = model.withTimestamp(frame.timestampNanos() + oneDayNanos);
```

The step count must also satisfy the positivity condition
`steps >= floor(b^2 T / (2 sigma^2)) + 1`, where `b` is cost of carry and `T`
is time to expiry. The implementation supports at most `10_000` steps to bound
memory allocation and the tree's quadratic runtime. A count outside the
supported interval produces `InvalidStepCountException`.
Delta, gamma and theta come from the first tree level. Vega uses a one
percentage-point volatility bump and rho uses a one-basis-point rate bump;
both are returned per unit change in their input. For central differences, the
two bumped valuations use the same tree depth, automatically increased when a
bump would otherwise violate the probability-positivity condition.

## Bjerksund-Stensland 2002 American Option

`BjerksundStensland` is a closed-form approximation for vanilla American
options. It supports every `MarketData` implementation through the generalized
cost-of-carry parameter and returns the option price plus the five standard
Greeks. American puts are handled internally through put-call symmetry.

```java
import com.thegreeklab.finance.contract.OptionContract;
import com.thegreeklab.finance.enums.Option;
import com.thegreeklab.finance.enums.OptionType;
import com.thegreeklab.finance.frame.EquityFrame;
import com.thegreeklab.finance.model.american.approximations.BjerksundStensland;
import com.thegreeklab.finance.time.DayCountConvention;

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

BjerksundStensland model = new BjerksundStensland(
        put, frame, 0.22, DayCountConvention.ACT_365F
);

double price = model.price();
double delta = model.delta();
double gamma = model.gamma();
double vega = model.vega();
double theta = model.theta();
double rho = model.rho();

var values = model.greeks();
double snapshotPrice = values.price();
double snapshotDelta = values.delta();
```

The approximation enforces the European and intrinsic-value lower bounds. If
the analytical exercise boundary becomes non-finite, it returns that
no-arbitrage lower bound instead of propagating `NaN`.

The Greeks are numerical bump-and-revalue estimates rather than analytical
derivatives. Vega and rho are expressed per unit move in volatility and rate,
respectively; theta is annualized. Because the price approximation is
piecewise smooth, the estimates can be sensitive to bump size near an early
exercise boundary, expiry or a numerical fallback boundary.

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
import com.thegreeklab.finance.time.DayCountConvention;
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
        VolatilityCalculator.impliedVolatility(
                contract, frame, marketPrice, DayCountConvention.ACT_365F
        );
```

The implied volatility solver supports European options.
