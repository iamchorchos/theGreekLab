# Sources and References

This document records the publications, numerical reference data and external
software that influenced TheGreekLab. It also distinguishes a bibliographic
citation from incorporated third-party code.

Unless a component is explicitly identified under
[Third-party code provenance](#third-party-code-provenance), the Java source,
tests and documentation are original project work. Citing a formula or an
algorithm does not mean that source code was copied from the cited work.

## Pricing models and analytical formulas

| Area | Primary source | Use in TheGreekLab |
| --- | --- | --- |
| Black-Scholes | Fischer Black and Myron Scholes, "The Pricing of Options and Corporate Liabilities," *Journal of Political Economy* 81(3), 1973, pp. 637-654. [doi:10.1086/260062](https://doi.org/10.1086/260062) | Foundation of the European equity-option price and Greeks. |
| Merton extension | Robert C. Merton, "Theory of Rational Option Pricing," *The Bell Journal of Economics and Management Science* 4(1), 1973, pp. 141-183. [doi:10.2307/3003143](https://doi.org/10.2307/3003143) | Continuous dividend yield and the generalized cost-of-carry formulation. |
| Black-76 | Fischer Black, "The Pricing of Commodity Contracts," *Journal of Financial Economics* 3(1-2), 1976, pp. 167-179. [doi:10.1016/0304-405X(76)90024-6](https://doi.org/10.1016/0304-405X(76)90024-6) | European options whose model input is a futures price. |
| Garman-Kohlhagen | Mark B. Garman and Steven W. Kohlhagen, "Foreign Currency Option Values," *Journal of International Money and Finance* 2(3), 1983, pp. 231-237. [doi:10.1016/S0261-5606(83)80001-1](https://doi.org/10.1016/S0261-5606(83)80001-1) | European FX options with domestic and foreign rates. |
| Formula notation and Greeks | Espen Gaarder Haug, *The Complete Guide to Option Pricing Formulas*, 2nd ed., McGraw-Hill, 2007, ISBN 978-0-07-138997-6. [Publisher page](https://www.mheducation.com/highered/mhp/product/complete-guide-to-option-pricing-formulas-2e.html) | Generalized cost-of-carry notation, analytical Greeks, tree formulas and published numerical tables. |

## American options and lattice methods

| Area | Primary source | Use in TheGreekLab |
| --- | --- | --- |
| Cox-Ross-Rubinstein | John C. Cox, Stephen A. Ross and Mark Rubinstein, "Option Pricing: A Simplified Approach," *Journal of Financial Economics* 7(3), 1979, pp. 229-263. [doi:10.1016/0304-405X(79)90015-1](https://doi.org/10.1016/0304-405X(79)90015-1) | Recombining binomial tree and early-exercise rollback. |
| Peizer-Pratt inversion | David B. Peizer and John W. Pratt, "A Normal Approximation for Binomial, F, Beta, and Other Common, Related Tail Probabilities, I," *Journal of the American Statistical Association* 63(324), 1968, pp. 1416-1456. [doi:10.1080/01621459.1968.10480938](https://doi.org/10.1080/01621459.1968.10480938) | Probability transform used by the Leisen-Reimer tree. |
| Leisen-Reimer | Dietmar Leisen and Matthias Reimer, "Binomial Models for Option Valuation - Examining and Improving Convergence," *Applied Mathematical Finance* 3(4), 1996, pp. 319-346. [doi:10.1080/13504869600000015](https://doi.org/10.1080/13504869600000015) | Smooth-convergence binomial parameterization. |
| Bjerksund-Stensland 2002 | Petter Bjerksund and Gunnar Stensland, "Closed Form Valuation of American Options," NHH working paper, revised 21 October 2002. [Archived paper](https://citeseerx.ist.psu.edu/document?doi=05e457d6e88609c1e0f5f892b7936ee2e4590fb3&repid=rep1&type=pdf) | Two-period American-option approximation, exercise boundaries and put-call transformation. The implementation notation follows Haug's presentation. |
| Recombining trinomial tree | Haug, *The Complete Guide to Option Pricing Formulas*, 2nd ed., tree and finite-difference chapters. | Three-branch probabilities, backward induction and the minimum-step condition used to prevent negative probabilities. |

### Implementation and educational references

The following VinegarHill FinanceLabs material was consulted as practical
implementation and explanatory inspiration for the binomial and trinomial
trees:

- [Binomial Lattice Framework](https://sites.google.com/view/vinegarhill-financelabs/binomial-lattice-framework)
- [Trinomial Model: the Auld Triangle](https://sites.google.com/view/vinegarhill-financelabs/trinomial-model)

These pages are secondary educational sources. The original model publications
and Haug's presentation remain the primary mathematical references.
TheGreekLab does not distribute source code from VinegarHill FinanceLabs.

The numerical Greeks for the American approximation and the trees are project
implementations based on bump-and-revalue or tree-node differences. Their bump
sizes, boundary behavior and limitations are documented in
[MATH.md](MATH.md).

## Discrete cash dividends

| Method | Primary source | Use in TheGreekLab |
| --- | --- | --- |
| Simple volatility adjustment (`Vol1`) and Haug-Haug adjustment (`Vol2`) | Espen G. Haug, Jorgen Haug and Alan Lewis, "Back to Basics: A New Approach to the Discrete Dividend Problem," *Wilmott* 2003(5), pp. 37-47. [doi:10.1002/wilm.42820030514](https://doi.org/10.1002/wilm.42820030514) | Adjusted spot and the `Vol1`/`Vol2` volatility formulas; Tables 1 and 6 provide published test values. |
| Bos-Vandermark | Michael Bos and Stephen Vandermark, "Finessing Fixed Dividends," *Risk* 15(9), 2002, pp. 157-158. [Article page](https://www.risk.net/derivatives/equity-derivatives/1530307/finessing-fixed-dividends) | Split of dividends into near and far components and the adjusted spot/strike inputs. |
| Bos-Gairat-Shepeleva (`Vol3`) | Michael Bos, Alexander Gairat and Anna Shepeleva, "Dealing with Discrete Dividends," *Risk* 16(1), 2003, pp. 109-112. [Article page](https://www.risk.net/derivatives/1530310/dealing-discrete-dividends) | First- and second-order dividend terms in the volatility adjustment. |
| Consolidated algorithms and examples | Haug, *The Complete Guide to Option Pricing Formulas*, 2nd ed., chapter 9. | Formula notation, implementation cross-checks and the published comparison tables. |

These methods are analytical approximations for deterministic cash dividends;
they are not exact solutions of a discrete-jump process. See
[MATH.md](MATH.md#discrete-cash-dividend-approximations) for their assumptions
and formulas.

## Numerical and statistical algorithms

| Area | Primary source | Use in TheGreekLab |
| --- | --- | --- |
| Error and normal distribution functions | W. J. Cody, "Rational Chebyshev Approximations for the Error Function," *Mathematics of Computation* 23(107), 1969, pp. 631-637. [doi:10.1090/S0025-5718-1969-0247736-4](https://doi.org/10.1090/S0025-5718-1969-0247736-4); Cody's [`CALERF`](https://netlib.org/specfun/erf) reference routine in Netlib SPECFUN. | Three-region rational approximation, interval structure and published coefficients used by `ERF.erfc`; normal CDF and PDF are derived from it. The Java implementation is maintained by this project; the Fortran routine itself is not distributed. |
| Online sample variance | B. P. Welford, "Note on a Method for Calculating Corrected Sums of Squares and Products," *Technometrics* 4(3), 1962, pp. 419-420. [doi:10.1080/00401706.1962.10490022](https://doi.org/10.1080/00401706.1962.10490022) | Numerically stable variance of log returns in historical volatility. |
| High-low volatility | Michael Parkinson, "The Extreme Value Method for Estimating the Variance of the Rate of Return," *The Journal of Business* 53(1), 1980, pp. 61-65. [doi:10.1086/296071](https://doi.org/10.1086/296071) | Parkinson range-based volatility estimator. |
| Root finding | Richard P. Brent, *Algorithms for Minimization Without Derivatives*, Prentice-Hall, 1973, ISBN 978-0-13-022335-7. | Bracketed implied-volatility solver combining bisection, secant and inverse quadratic interpolation steps. |
| Initial implied-volatility estimate | Menachem Brenner and Marti G. Subrahmanyam, "A Simple Formula to Compute the Implied Standard Deviation," *Financial Analysts Journal* 44(5), 1988, pp. 80-83. [doi:10.2469/faj.v44.n5.80](https://doi.org/10.2469/faj.v44.n5.80) | At-the-money-style initial estimate before Brent bracketing. |

## Numerical reference data

Published values are recorded separately from generated fixtures because their
provenance and tolerances have different evidential strength.

### Published tables

| Test area | Reference values | Tolerance rationale |
| --- | --- | --- |
| Bjerksund-Stensland 2002 | All 36 call and put values from Haug, 2nd ed., Table 3-2. | `1e-4`, matching values published to four decimal places. |
| Haug-Haug `Vol2` | Haug, Haug and Lewis (2003), Tables 1 and 6. | `5e-5`, half a unit in the last of four published decimal places. |
| Bos-Gairat-Shepeleva `Vol3` | Haug, Haug and Lewis (2003), Table 1. | `5e-5`, half a unit in the last published decimal place. |
| Bos-Vandermark | Haug, Haug and Lewis (2003), Table 1, `BV` column. | `5e-5`, half a unit in the last published decimal place. |

### Generated CSV fixtures

The following files were introduced in the repository's initial commit on
12 July 2026. Their generation scripts, random seeds and exact external-tool
versions were not committed. Consequently, they remain useful regression data,
but they are **not yet independently reproducible reference datasets**. The
project does not claim a more specific provenance than the repository records
support.

| Fixture | Rows | Current use or recorded intent | Provenance status |
| --- | ---: | --- | --- |
| `erf_crossval.csv` | 100,000 | `erfc`, normal CDF and PDF cross-validation | Original generator and library version not recorded. |
| `peizer_pratt_crossval.csv` | 100,000 | Peizer-Pratt method-2 cross-validation | Original generator and library version not recorded. |
| `iv_crossval.csv` | 30,000 | Implied-volatility recovery | Original generator and library version not recorded. |
| `parkinson_crossval.csv` | 10,000 | Parkinson volatility | Original generator, seed and library version not recorded. |
| `volatility_crossval.csv` | 10,000 | Historical volatility | Original generator, seed and library version not recorded. |
| `invariants_test_cases.csv` | 1,000 | Black-Scholes identities over generated scenarios | Repository-generated scenarios; seed not recorded. |
| `quantlib_test.csv` | 1,000 | Historical Black-Scholes/Greeks comparison fixture | The filename indicates QuantLib, but the version and generating script are not recorded; the file is not consumed by the current test suite. |
| `crr_all_greeks_test_data.csv` | 10,000 | Historical CRR price/Greeks fixture | Original generator and bump conventions not recorded; not consumed by the current test suite. |
| `crr_exact_math_data.csv` | 1,000 | Historical CRR mathematical fixture | Original generator not recorded; not consumed by the current test suite. |
| `bs_test.csv` | 2,500 | Historical European-model fixture | Original generator not recorded; not consumed by the current test suite. |
| `edge_cases.csv` | 5 | Historical boundary fixture | Hand-authored or generator provenance not recorded; not consumed by the current test suite. |

When one of these datasets is regenerated, the same change should add its
script and record the external library and version, random seed, formula or
source publication, units and assertion tolerance. A generated value must not
come from the implementation under test.

## Third-party code provenance

The only incorporated third-party numerical source is
`src/main/fortran/pbivnorm.f`, derived from CRAN `pbivnorm` 0.6.0:

- [CRAN package page](https://CRAN.R-project.org/package=pbivnorm)
- [upstream source repository](https://github.com/brentonk/pbivnorm)
- Alan Genz's original bivariate-normal routines, with the retained Fortran
  comments also crediting J. L. Schonfelder, *Mathematics of Computation*
  32 (1978), pp. 1232-1240, for the error-function routine

The `PBIVNORM` vector wrapper was added by Brenton Kenkel in 2011 and was based
on a routine by Adelchi Azzalini. Full attribution and component licensing are
authoritative in [NOTICE](../NOTICE) and [LICENSING.md](../LICENSING.md). The
Fortran source and binaries compiled from it are GPL-2.0-or-later; the combined
distribution is GPL-3.0-or-later.

## External libraries and test tooling

No source code from these libraries is copied into the Java implementation.
They are normal Maven dependencies or build/test tools; `pom.xml` is the
authoritative record of the versions in a particular release.

| Project | Role |
| --- | --- |
| [Jafama](https://github.com/jeffhain/jafama) | Fast elementary mathematical functions. |
| [Eclipse Collections](https://github.com/eclipse/eclipse-collections) | Primitive collection API used by volatility calculations. |
| [SpotBugs](https://spotbugs.github.io/) | Static-analysis annotations and build-time analysis. |
| [JUnit 5](https://junit.org/junit5/) | Unit and parameterized tests. |
| [JMH](https://github.com/openjdk/jmh) | Microbenchmark harness used in test sources. |
| [QuantLib](https://www.quantlib.org/) | Name associated with one legacy CSV fixture; exact historical version is not recorded. It is not a project dependency. |

## Maintaining this record

Every contribution that introduces a formula, algorithm, external dataset or
third-party source must update this document. For numerical data, record the
source or generator, exact version, seed where applicable, units, transformation
steps and tolerance. For incorporated code or data, also record its license in
`NOTICE` and `LICENSING.md` before merging.
