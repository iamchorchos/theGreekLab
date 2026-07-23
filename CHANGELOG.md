# Changelog

All notable changes to this project are documented in this file.

The project follows [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Curve market-data API: flat and log-linearly interpolated discount and
  forward curves, dated node types, and explicit no-extrapolation semantics.
- Nominal `FundingCurve` and `DividendYieldCurve` wrappers preventing equity
  funding and dividend-yield inputs from being exchanged accidentally.
- Curve-aware `ForwardBlack76` European option pricing from forward and
  funding curves, with an equity-forward convenience constructor.
- `VolatilitySurface` and `FlatVolatilitySurface`, plus surface-aware
  `ForwardBlack76` constructors indexed by expiry and log strike-to-forward.
- Optional `thegreeklab-visualization` JavaFX module with validated regular
  surface sampling and a resizable implied-volatility heatmap.

### Changed

- `BlackScholes` shares its internal $d_1$ calculation with `ForwardBlack76`
  without changing existing model behavior.

## [2.2.0] - 2026-07-22

### Added

- Universal model-driven implied-volatility calibration for European,
  American, lattice and discrete-dividend pricing models.
- Immutable `ImpliedVolatilityResult` diagnostics covering convergence,
  residual error, iteration counts and explicit failure statuses.
- `VolatilityPricer` as the common calibration contract for immutable pricing
  models, including recovery from trial points outside a model's valid domain.
- Roll-Geske-Whaley American call pricing for a single discrete cash dividend,
  with immutable bump scenarios, five standard Greeks and implied volatility.
- Explicit `InvalidModelDomainException` reporting for numerically invalid
  model parameter regions.

### Changed

- Volatility analytics now live in `com.thegreeklab.math.volatility`; the
  original `com.thegreeklab.math.VolatilityCalculator` remains as a deprecated
  compatibility facade for the complete 2.1 API.
- Numerical bump-and-revalue Greeks and reusable test assertions are
  consolidated to remove duplicated model and fixture logic.

### Fixed

- Implied-volatility bracketing now searches valid model-domain edges without
  hanging or failing on invalid intermediate lattice probabilities.

## [2.1.0] - 2026-07-19

### Added

- Immutable deterministic `CashDividend` and chronologically ordered
  `DividendSchedule` domain types.
- Simple, Haug-Haug, Bos-Gairat-Shepeleva and Bos-Vandermark European
  discrete-dividend approximations.
- Model-specific adjusted spot, strike and volatility inputs, immutable bump
  scenarios and five numerical standard Greeks for discrete-dividend models.
- Published numerical reference tests and public usage and mathematical
  documentation for the discrete-dividend API.
- Central source register covering model publications, numerical fixtures,
  third-party code provenance and external libraries.

## [2.0.1] - 2026-07-18

### Fixed

- Resolve the CI-friendly `${revision}` property in the POM deployed to Maven
  Central and validate it against the release tag before publication.

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
