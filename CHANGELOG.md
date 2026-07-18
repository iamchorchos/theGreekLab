# Changelog

All notable changes to this project are documented in this file.

The project follows [Semantic Versioning](https://semver.org/).

## [2.0.0] - 2026-07-18

### Changed

- `OptionContract` now stores expiration only as `expirationDate`.
- Pricing models and the implied-volatility solver require an explicit
  `DayCountConvention`; no global default is applied.
- `ACT_365F` and `ACT_360` fixed-denominator conventions are supported.
- Project version advanced to `2.0.0-SNAPSHOT`.
- Maven coordinates changed to `io.github.iamchorchos:thegreeklab` for Central
  Portal namespace verification and conventional artifact naming.
- Added an opt-in japicmp profile for binary and source compatibility checks
  against an explicit released baseline.
- CI now builds the JAR on Windows, exercises the bundled `pbivnorm.dll`, and
  verifies that the DLL is present in the packaged artifact.
- Semantic-version release tags now sign and publish the main, source, Javadoc
  and POM artifacts through Maven Central before creating the GitHub release.

### Removed

- The eight-argument `OptionContract` constructor.
- The redundant `expirationNanosEpoch` and `secondsInExpirationYear` record
  components.
- `OptionContract.getTimeToExpiry(long)`; pricing time is now calculated by
  `DayCountConvention`.
- `MarketData.toEpochNanos(ZonedDateTime)`; neutral timestamp conversion now
  lives in `EpochNanos.from(ZonedDateTime)`.

### Migration

Replace:

```text
new OptionContract(symbol, type, style, strike, expiry, multiplier,
        expirationNanosEpoch, secondsInExpirationYear);
```

with:

```text
new OptionContract(symbol, type, style, strike, expiry, multiplier);
```

Code that needs an explicit year fraction can use:

```text
DayCountConvention.ACT_365F.timeToExpiry(valuationTimestampNanos, expiry);
```

Pass the same convention explicitly to every model and solver:

```text
new BlackScholesMerton(contract, frame, volatility,
        DayCountConvention.ACT_365F);

VolatilityCalculator.impliedVolatility(contract, frame, marketPrice,
        DayCountConvention.ACT_365F);
```

Replace direct calls to `MarketData.toEpochNanos(timestamp)` with
`EpochNanos.from(timestamp)`.
