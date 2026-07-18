# TheGreekLab Mathematical Notes

This document summarizes the formulas implemented by TheGreekLab.

Notation:

- $S$ - spot price
- $F$ - futures price
- $K$ - strike price
- $T$ - time to expiry in years
- $r$ - risk-free rate
- $q$ - continuous dividend yield
- $r_f$ - foreign risk-free rate
- $b$ - cost of carry
- $\sigma$ - annualized volatility
- $N(x)$ - standard normal CDF
- $n(x)$ - standard normal PDF
- $\Delta t = T / N$ - binomial time step

## Time To Expiry

Every model requires an explicit day-count convention. If $t_v$ is the
market-data valuation timestamp, $t_e$ is the contract expiration timestamp
and $D$ is the selected fixed year denominator, then:

```math
T = \max\left(\frac{t_e - t_v}{D \cdot 86\,400}, 0\right)
```

The difference is measured in actual elapsed seconds, including the time of
day. `DayCountConvention.ACT_365F` uses $D=365$ and
`DayCountConvention.ACT_360` uses $D=360$; neither denominator changes for a
leap year. `OptionContract` stores only the zoned expiration date, while the
caller selects the convention for each valuation.

## Cost Of Carry

The generalized Black-Scholes engine uses a cost-of-carry parameter $b$:

| Model | Market data | Cost of carry |
| --- | --- | --- |
| Black-Scholes, no dividends | spot equity | $b = r$ |
| Black-Scholes-Merton | dividend-paying equity | $b = r - q$ |
| Black-76 | futures | $b = 0$ |
| Garman-Kohlhagen | FX | $b = r - r_f$ |

The common generalized parameters are:

```math
d_1 = \frac{\ln(S/K) + T(b + \sigma^2/2)}{\sigma\sqrt{T}}
```

```math
d_2 = d_1 - \sigma\sqrt{T}
```

```math
D_r = e^{-rT}
```

```math
D_b = e^{(b-r)T}
```

## Generalized European Option Price

Call:

```math
C = S D_b N(d_1) - K D_r N(d_2)
```

Put:

```math
P = C - S D_b + K D_r
```

Equivalently:

```math
P = K D_r N(-d_2) - S D_b N(-d_1)
```

At expiry, the model returns intrinsic value:

```math
C_T = \max(S-K, 0)
```

```math
P_T = \max(K-S, 0)
```

## Black-Scholes-Merton

For dividend-paying equity options:

```math
b = r - q
```

Therefore:

```math
D_b = e^{-qT}
```

Call:

```math
C = S e^{-qT} N(d_1) - K e^{-rT} N(d_2)
```

Put:

```math
P = K e^{-rT}N(-d_2) - S e^{-qT}N(-d_1)
```

## Black-76

For futures options, TheGreekLab treats the futures price as the model input:

```math
S = F
```

```math
b = 0
```

Therefore:

```math
D_b = e^{-rT}
```

Call:

```math
C = e^{-rT}\left(FN(d_1)-KN(d_2)\right)
```

Put:

```math
P = e^{-rT}\left(KN(-d_2)-FN(-d_1)\right)
```

Black-76 rho is specialized because $d_1$ and $d_2$ do not depend on $r$ when
$F$ is supplied directly:

```math
\rho = -T \cdot V
```

where $V$ is the option price.

## Garman-Kohlhagen

For FX options:

```math
b = r - r_f
```

Therefore:

```math
D_b = e^{-r_f T}
```

Call:

```math
C = S e^{-r_f T}N(d_1) - K e^{-rT}N(d_2)
```

Put:

```math
P = K e^{-rT}N(-d_2) - S e^{-r_f T}N(-d_1)
```

## European Greeks

The following formulas are implemented for the generalized Black-Scholes engine.

### Delta

Call:

```math
\Delta_C = D_b N(d_1)
```

Put:

```math
\Delta_P = D_b(N(d_1)-1)
```

### Gamma

Same for calls and puts:

```math
\Gamma = \frac{D_b n(d_1)}{S\sigma\sqrt{T}}
```

### Vega

Vega is returned per 1.00 volatility unit:

```math
\nu = S D_b \sqrt{T} n(d_1)
```

Per percentage point:

```math
\nu_{1\%} = 0.01 \nu
```

### Rho

Call:

```math
\rho_C = K T e^{-rT} N(d_2)
```

Put:

```math
\rho_P = -K T e^{-rT} N(-d_2)
```

For Black-76:

```math
\rho = -T V
```

### Theta

The implementation returns annualized theta by default.

Call:

```math
\Theta_C =
-\frac{S D_b \sigma n(d_1)}{2\sqrt{T}}
+ (r-b)S D_b N(d_1)
- rK e^{-rT}N(d_2)
```

Put:

```math
\Theta_P =
-\frac{S D_b \sigma n(d_1)}{2\sqrt{T}}
- (r-b)S D_b N(-d_1)
+ rK e^{-rT}N(-d_2)
```

Daily theta under the selected fixed-denominator convention:

```math
\Theta_{\text{daily}} = \frac{\Theta}{D}
```

### Epsilon

Epsilon is the sensitivity to the dividend-yield or foreign-rate component
embedded in cost of carry.

Call:

```math
\epsilon_C = -S T D_b N(d_1)
```

Put:

```math
\epsilon_P = S T D_b N(-d_1)
```

`Black76` does not define epsilon because it has no dividend-yield or
foreign-rate input.

### Lambda

Lambda is option elasticity:

```math
\lambda = \Delta \frac{S}{V}
```

where $V$ is the option price.

### Vanna

```math
\text{Vanna} = -D_b n(d_1)\frac{d_2}{\sigma}
```

### Volga

```math
\text{Volga} = \nu \frac{d_1 d_2}{\sigma}
```

### Charm

Call:

```math
\text{Charm}_C =
(r-b)D_bN(d_1)
-D_bn(d_1)\left(\frac{b}{\sigma\sqrt{T}}-\frac{d_2}{2T}\right)
```

Put:

```math
\text{Charm}_P =
-(r-b)D_bN(-d_1)
-D_bn(d_1)\left(\frac{b}{\sigma\sqrt{T}}-\frac{d_2}{2T}\right)
```

### Dual Delta

Call:

```math
\frac{\partial C}{\partial K} = -e^{-rT}N(d_2)
```

Put:

```math
\frac{\partial P}{\partial K} = e^{-rT}N(-d_2)
```

### Dual Gamma

```math
\frac{\partial^2 V}{\partial K^2}
= \frac{e^{-rT}n(d_2)}{K\sigma\sqrt{T}}
```

### Veta

```math
\text{Veta}
= \nu\left((r-b)+\frac{bd_1}{\sigma\sqrt{T}}-\frac{d_1d_2+1}{2T}\right)
```

### Vera

```math
\text{Vera}
= -KT e^{-rT}n(d_2)\frac{d_1}{\sigma}
```

### Speed

```math
\text{Speed}
= -\frac{\Gamma}{S}\left(\frac{d_1}{\sigma\sqrt{T}}+1\right)
```

### Zomma

```math
\text{Zomma}
= \Gamma\frac{d_1d_2-1}{\sigma}
```

### Color

```math
\text{Color}
= -\frac{D_bn(d_1)}{2S T\sigma\sqrt{T}}
\cdot
\left(2(r-b)T + 1 + d_1\left(\frac{2bT}{\sigma\sqrt{T}}-d_2\right)\right)
```

### Ultima

```math
\text{Ultima}
= -\frac{\nu}{\sigma^2}
\cdot
\left(d_1d_2(1-d_1d_2)+d_1^2+d_2^2\right)
```

### Parmicharma

The call formula follows the implementation:

```math
\tau =
\frac{b-\sigma^2/2}{\sigma\sqrt{T}} - \frac{d_2}{2T}
```

```math
A =
(r-b) - \frac{2bT/(\sigma\sqrt{T}) - d_2}{2T}
```

```math
B =
\frac{
2d_2\sigma^2T - b\sigma\sqrt{T}T - \sigma^2T^2\tau
}{
2T^3\sigma^2
}
```

Call:

```math
\text{Parmicharma}_C = A\cdot\text{Charm}_C - D_bn(d_1)B
```

Put:

```math
\text{Parmicharma}_P = (r-b)^2D_b + \text{Parmicharma}_C
```

## Bjerksund-Stensland 2002

The Bjerksund-Stensland 2002 model is a closed-form approximation for vanilla
American options. The implementation follows the generalized cost-of-carry
notation used by Haug.

The approximation is evaluated directly for calls. Puts use put-call symmetry:

```math
P_{BS2002}(S,K,T,r,b,\sigma)
=
C_{BS2002}(K,S,T,r-b,-b,\sigma)
```

If the transformed call has $b \ge r$, early exercise is not optimal and its
value is the generalized European call value.

### Exercise boundaries

The intermediate time is:

```math
t_1 = \frac{\sqrt{5}-1}{2}T
```

The positive root used by the approximation is:

```math
\beta =
\frac{1}{2}-\frac{b}{\sigma^2}
+
\sqrt{
\left(\frac{b}{\sigma^2}-\frac{1}{2}\right)^2
+\frac{2r}{\sigma^2}
}
```

The asymptotic and lower exercise boundaries are:

```math
B_\infty = \frac{\beta}{\beta-1}K
```

```math
B_0 = \max\left(K,\frac{r}{r-b}K\right)
```

For a time $t$:

```math
h(t) =
-\left(bt+2\sigma\sqrt{t}\right)
\frac{K^2}{(B_\infty-B_0)B_0}
```

```math
I(t) = B_0+(B_\infty-B_0)\left(1-e^{h(t)}\right)
```

The two boundaries and their coefficients are:

```math
I_1=I(t_1), \qquad I_2=I(T)
```

```math
\alpha_j=(I_j-K)I_j^{-\beta}, \qquad j\in\{1,2\}
```

### Phi term

For exponent $\gamma$:

```math
\lambda =
-r+\gamma b+\frac{1}{2}\gamma(\gamma-1)\sigma^2
```

```math
\kappa = \frac{2b}{\sigma^2}+2\gamma-1
```

```math
d =
-\frac{
\ln(S/H)+\left(b+(\gamma-1/2)\sigma^2\right)t
}{\sigma\sqrt{t}}
```

```math
\phi(S,t,\gamma,H,I)
=
e^{\lambda t}S^\gamma
\left[
N(d)
-
\left(\frac{I}{S}\right)^\kappa
N\left(d-\frac{2\ln(I/S)}{\sigma\sqrt{t}}\right)
\right]
```

### Ksi term and the bivariate normal CDF

Let $N_2(x,y;\rho)$ denote the standard bivariate normal CDF with correlation
$\rho$. Define:

```math
\rho=\sqrt{\frac{t_1}{T}}
```

```math
e_1=
\frac{\ln(S/I_1)+\left(b+(\gamma-1/2)\sigma^2\right)t_1}
{\sigma\sqrt{t_1}}
```

```math
e_2=
\frac{\ln(I_2^2/(SI_1))+\left(b+(\gamma-1/2)\sigma^2\right)t_1}
{\sigma\sqrt{t_1}}
```

```math
e_3=
\frac{\ln(S/I_1)-\left(b+(\gamma-1/2)\sigma^2\right)t_1}
{\sigma\sqrt{t_1}}
```

```math
e_4=
\frac{\ln(I_2^2/(SI_1))-\left(b+(\gamma-1/2)\sigma^2\right)t_1}
{\sigma\sqrt{t_1}}
```

For the terminal boundary $H$:

```math
f_1=
\frac{\ln(S/H)+\left(b+(\gamma-1/2)\sigma^2\right)T}
{\sigma\sqrt{T}}
```

```math
f_2=
\frac{\ln(I_2^2/(SH))+\left(b+(\gamma-1/2)\sigma^2\right)T}
{\sigma\sqrt{T}}
```

```math
f_3=
\frac{\ln(I_1^2/(SH))+\left(b+(\gamma-1/2)\sigma^2\right)T}
{\sigma\sqrt{T}}
```

```math
f_4=
\frac{\ln(SI_1^2/(HI_2^2))+\left(b+(\gamma-1/2)\sigma^2\right)T}
{\sigma\sqrt{T}}
```

Then:

```math
\begin{aligned}
\xi(S,T,\gamma,H,I_2,I_1,t_1)
=e^{\lambda T}S^\gamma\Bigg[&
N_2(-e_1,-f_1;\rho)\\
&-\left(\frac{I_2}{S}\right)^\kappa N_2(-e_2,-f_2;\rho)\\
&-\left(\frac{I_1}{S}\right)^\kappa N_2(-e_3,-f_3;-\rho)\\
&+\left(\frac{I_1}{I_2}\right)^\kappa N_2(-e_4,-f_4;-\rho)
\Bigg]
\end{aligned}
```

`BivariateNormal.cdf()` evaluates $N_2$ using the native Fortran `pbivnorm`
routine through the Java Foreign Function and Memory API. The returned native
probability is required to be finite and is clamped to $[0,1]$ to remove small
floating-point excursions.

### Call value

If $S\ge I_2$, immediate exercise gives $S-K$. Otherwise the approximation is:

```math
\begin{aligned}
C_{BS2002}={}&
\alpha_2S^\beta
-\alpha_2\phi(S,t_1,\beta,I_2,I_2)
+\phi(S,t_1,1,I_2,I_2)
-\phi(S,t_1,1,I_1,I_2)\\
&-K\phi(S,t_1,0,I_2,I_2)
+K\phi(S,t_1,0,I_1,I_2)
+\alpha_1\phi(S,t_1,\beta,I_1,I_2)\\
&-\alpha_1\xi(S,T,\beta,I_1,I_2,I_1,t_1)
+\xi(S,T,1,I_1,I_2,I_1,t_1)\\
&-\xi(S,T,1,K,I_2,I_1,t_1)
-K\xi(S,T,0,I_1,I_2,I_1,t_1)
+K\xi(S,T,0,K,I_2,I_1,t_1)
\end{aligned}
```

Because this is an approximation, the implementation enforces the
no-arbitrage lower bound:

```math
V_A \ge \max(V_E,V_{\mathrm{intrinsic}})
```

The same lower bound is returned if $\beta$, an exercise boundary, a boundary
coefficient or the final approximation becomes non-finite. At expiry the model
returns intrinsic value directly.

The reference tests reproduce all 36 Bjerksund-Stensland 2002 values from Haug
table 3-2 for calls and puts on futures.

### Numerical Greeks

The Bjerksund-Stensland implementation exposes delta, gamma, vega, theta and
rho through bump-and-revalue differentiation of the complete bounded price
function. For a model value $V$ and bump $h$:

```math
\Delta \approx \frac{V(S+h_S)-V(S-h_S)}{2h_S}
```

```math
\Gamma \approx
\frac{V(S+h_\Gamma)-2V(S)+V(S-h_\Gamma)}{h_\Gamma^2}
```

```math
\text{Vega} \approx
\frac{V(\sigma+h_\sigma)-V(\sigma-h_\sigma)}{2h_\sigma}
```

```math
\rho \approx \frac{V(r+h_r)-V(r-h_r)}{2h_r}
```

The volatility interval is shortened at
`PricingValidation.MIN_VOLATILITY`, and the actual interval width is used in
the denominator. Theta advances the market-data timestamp:

```math
\Theta \approx
\frac{V(t+\Delta t)-V(t)}{\Delta t_{\text{years}}}
```

The default bumps are:

| Greek | Bump |
|---|---|
| delta | $\max(10^{-4}S,10^{-6})$ |
| gamma | $\max(10^{-3}S,10^{-6})$ |
| vega | $\max(10^{-4}\sigma,10^{-6})$ |
| rho | $10^{-4}$ (one basis point) |
| theta | at most one calendar day and at most half the remaining time |

These derivatives include the intrinsic-value and European lower bounds.
Consequently, values close to an exercise boundary, expiry or fallback regime
may depend more strongly on the selected bump because the bounded
approximation is only piecewise smooth.

## Cox-Ross-Rubenstein Tree

CRR uses symmetric multiplicative moves:

```math
u = e^{\sigma\sqrt{\Delta t}}
```

```math
d = \frac{1}{u}
```

Risk-neutral probability:

```math
p = \frac{e^{b\Delta t}-d}{u-d}
```

Node spot:

```math
S_{j,i} = S u^{2i-j}
```

where $j$ is the step and $i$ is the number of up moves.

Terminal payoff:

```math
V_{N,i}^{call} = \max(S_{N,i}-K, 0)
```

```math
V_{N,i}^{put} = \max(K-S_{N,i}, 0)
```

Backward induction with early exercise:

```math
V_{j,i} =
\max\left(
\text{payoff}(S_{j,i}),
e^{-r\Delta t}\left(pV_{j+1,i+1}+(1-p)V_{j+1,i}\right)
\right)
```

## Leisen-Reimer Tree

Leisen-Reimer uses the same backward-induction structure but transforms
Black-Scholes $d_1$ and $d_2$ into tree probabilities using Peizer-Pratt
method 2. The implementation requires an odd number of steps.

The Black-Scholes terms are:

```math
d_1 =
\frac{\ln(S/K)+(b+\sigma^2/2)T}{\sigma\sqrt{T}}
```

```math
d_2 = d_1-\sigma\sqrt{T}
```

The Peizer-Pratt transform is:

```math
h_2(x,N)
= \frac{1}{2}
+ \frac{\mathrm{sign}(x)}{2}
\sqrt{
1-\exp\left(
-\left(\frac{x}{N+1/3+0.1/(N+1)}\right)^2
\cdot
\left(N+1/6\right)
\right)
}
```

The implementation uses:

```math
p = h_2(d_2,N)
```

```math
p' = h_2(d_1,N)
```

Up and down factors:

```math
u = e^{b\Delta t}\frac{p'}{p}
```

```math
d = e^{b\Delta t}\frac{1-p'}{1-p}
```

Node spot:

```math
S_{j,i} = S u^i d^{j-i}
```

Backward induction is the same as CRR:

```math
V_{j,i} =
\max\left(
\text{payoff}(S_{j,i}),
e^{-r\Delta t}\left(pV_{j+1,i+1}+(1-p)V_{j+1,i}\right)
\right)
```

## Trinomial Tree

The recombining trinomial model divides maturity into $N$ equal steps:

```math
\Delta t = \frac{T}{N}
```

The up, middle and down spot multipliers are:

```math
u=e^{\sigma\sqrt{2\Delta t}}, \qquad m=1, \qquad d=\frac{1}{u}
```

Define:

```math
a=e^{\sigma\sqrt{\Delta t/2}}, \qquad c=e^{b\Delta t/2}
```

The risk-neutral transition probabilities are:

```math
p_u=\left(\frac{c-a^{-1}}{a-a^{-1}}\right)^2
```

```math
p_d=\left(\frac{a-c}{a-a^{-1}}\right)^2
```

```math
p_m=1-p_u-p_d
```

To prevent negative transition probabilities, the implementation follows the
step-count condition:

```math
N \geq \left\lfloor\frac{b^2T}{2\sigma^2}\right\rfloor+1
```

It also validates that all three probabilities are finite, belong to
$[0,1]$, and sum to one within numerical tolerance. The implementation limits
$N$ to $10\,000$ so that array sizing cannot overflow and the $O(N^2)$
rollback remains bounded.

At level $i$, node $j\in\{0,\ldots,2i\}$ has spot price:

```math
S_{i,j}=S u^{j-i}
```

For $z=1$ for a call and $z=-1$ for a put, the terminal payoff is:

```math
V_{N,j}=\max\left(z(S_{N,j}-K),0\right)
```

The continuation value is:

```math
C_{i,j}=e^{-r\Delta t}
\left(p_dV_{i+1,j}+p_mV_{i+1,j+1}+p_uV_{i+1,j+2}\right)
```

European contracts use $V_{i,j}=C_{i,j}$. American contracts compare the
continuation value with immediate exercise:

```math
V_{i,j}=\max\left(z(S_{i,j}-K),C_{i,j}\right)
```

## Trinomial Greeks

Price, delta, gamma and theta are obtained during one backward induction. Let
$V_d$, $V_m$ and $V_u$ denote the down, middle and up values at the first tree
level, with corresponding spots $S/u$, $S$ and $Su$.

Delta spans the full first level:

```math
\Delta=\frac{V_u-V_d}{Su-S/u}
```

For gamma, first calculate adjacent deltas:

```math
\Delta_u=\frac{V_u-V_m}{Su-S}, \qquad
\Delta_d=\frac{V_m-V_d}{S-S/u}
```

and then:

```math
\Gamma=\frac{\Delta_u-\Delta_d}{(Su-S/u)/2}
```

Theta uses the middle node one step forward in calendar time:

```math
\Theta=\frac{V_m-V_{0,0}}{\Delta t}
```

Vega is a bump-and-revalue estimate with $h_\sigma=0.01$:

```math
\text{Vega}\approx
\frac{V(\sigma+h_\sigma)-V(\sigma-h_\sigma)}{2h_\sigma}
```

If the lower volatility is outside the supported domain, the implementation
uses the forward difference $(V(\sigma+h_\sigma)-V(\sigma))/h_\sigma$.
Rho uses a central one-basis-point bump, $h_r=10^{-4}$:

```math
\rho\approx\frac{V(r+h_r)-V(r-h_r)}{2h_r}
```

Vega and rho are reported per unit change in volatility and rate. Immutable
model copies exposed through `BumpableOptionModel` provide the same spot,
volatility, rate and timestamp scenario repricing used by these calculations.
For each central difference, both bumped valuations use the same depth. The
implementation raises that common depth when either bumped parameter set would
otherwise violate the trinomial probability-positivity condition.

## Binomial Greeks

Binomial model Greeks are finite-difference based. The implementation uses:

- spot bumps for spot sensitivities
- volatility bumps for volatility sensitivities
- rate bumps for rate sensitivities
- timestamp bumps for time sensitivities
- strike bumps for dual sensitivities

CRR theta uses a cached level-2 tree shortcut:

```math
\Theta \approx \frac{V_{2,1}-V_{0,0}}{2\Delta t}
```

where $V_{2,1}$ is the middle node value at tree level 2.

## Historical Volatility

Given prices $P_0,\dots,P_n$, log returns are:

```math
r_i = \ln(P_i)-\ln(P_{i-1})
```

The sample variance is:

```math
s^2 = \frac{1}{n-1}\sum_{i=1}^{n}(r_i-\bar r)^2
```

Annualized historical volatility:

```math
\sigma_{\text{hist}} = \sqrt{s^2 D}
```

where $D$ is the number of trading days per year.

The implementation computes variance using Welford's online algorithm for
numerical stability.

## Parkinson Volatility

For high-low bars $(H_i,L_i)$:

```math
\sigma_{\text{Parkinson}}
=
\sqrt{
\frac{D}{4n\ln 2}
\sum_{i=1}^{n}
\left(\ln\frac{H_i}{L_i}\right)^2
}
```

where $D$ is the number of trading days per year.

## Implied Volatility

Implied volatility solves:

```math
f(\sigma) = V_{\text{model}}(\sigma) - V_{\text{market}} = 0
```

The solver:

1. validates European exercise style,
2. checks model-free no-arbitrage bounds,
3. builds a volatility bracket,
4. uses Brent's method to solve for $\sigma$.

The volatility search interval is:

```math
\sigma \in [10^{-6}, 10]
```

The initial guess uses a Brenner-Subrahmanyan-style approximation:

```math
\sigma_0 =
\frac{V_{\text{market}}}{S}
\sqrt{\frac{2\pi}{T}}
```

The guess is then clamped into the search interval.

## Normal Distribution Helpers

The standard normal PDF is:

```math
n(x) = \frac{1}{\sqrt{2\pi}}e^{-x^2/2}
```

The standard normal CDF is:

```math
N(x) = \frac{1}{2}\left(1+\mathrm{erf}\left(\frac{x}{\sqrt{2}}\right)\right)
```

The ERF implementation uses numerical approximations and explicit handling for
edge cases such as `NaN` and extreme tails.
