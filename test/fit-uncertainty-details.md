# Understanding fit uncertainties: the statistics behind the result

## Documentation index

| Document | Contents |
|---|---|
| [Using fit results](fit-uncertainty-guide.md) | Short lab guide to fitted values, uncertainties, and reporting. |
| [Understanding the statistics](fit-uncertainty-details.md) **(this page)** | Statistical reasoning and a worked example. |
| [Bevington implementation](bevington-fit-uncertainties.md) | Profile-curvature method, code, and numerical limitations. |
| [Report definitions and integration](fit-report-physics.md) | Units, uncertainty models, and Tracker integration. |
| [Tests and documentation](README.md) | Regression test instructions and coverage. |

> **A number from a fit is not the answer; it is a measurement with assumptions attached.**

This is the middle layer of the Tracker fit-uncertainty documentation.

- **Just using Tracker for a lab?** Start with the [short guide](fit-uncertainty-guide.md).
- **Want to know where the uncertainty comes from?** Read this document.
- **Reviewing or maintaining the implementation?** See the [Bevington-style implementation note](bevington-fit-uncertainties.md).

This note assumes that you already know what a residual is and have seen least-squares fitting before. It develops the statistical ideas behind Tracker's reported coefficient standard errors without following the Java code line by line.

## 1. From residuals to a measurement scale

Suppose Tracker fits a function `f(x)` to measured values `y_i`. The residual for point `i` is

```text
residual_i = y_i - f(x_i)
```

and the sum of squared residuals is

```text
SSE = sum(residual_i^2).
```

Tracker currently treats all selected observations as having the same y uncertainty. There are two main ways to set that common uncertainty.

### Estimate it from the residuals

If no independent measurement uncertainty is supplied, Tracker estimates a common variance from the scatter about the fitted curve:

```text
sigma_y^2 = SSE_min / df
```

where `df` is the residual degrees of freedom. For an ordinary full-rank fit with `n` observations and `p` independently determined fitted coefficients,

```text
df = n - p.
```

More generally Tracker uses the numerical rank `r` of the free parameter directions, so

```text
df = n - r.
```

This distinction matters when two adjustable coefficients cannot actually be determined independently from the data.

Because this mode defines `sigma_y` from the fitted residuals themselves,

```text
chi^2_min = SSE_min / sigma_y^2 = df
```

and therefore reduced chi-square is exactly one whenever the calculation is defined. It is one **by construction**, so it is not an independent goodness-of-fit test in this mode.

### Supply the measurement uncertainty

If an experiment provides an independent common uncertainty `sigma_y`, Tracker instead uses

```text
chi^2 = SSE / sigma_y^2.
```

The best-fit coefficients do not change when the same positive uncertainty is assigned to every point: multiplying the objective by a constant does not move its minimum. The uncertainty scale and chi-square statistics do change.

The pixel option is another way of supplying the same kind of common uncertainty. Tracker converts the selected pixel uncertainty to physical position units using the available calibration scale. It does not, in this calculation, propagate uncertainty in the ruler or calibration itself.

## 2. Why moving a coefficient tells us its uncertainty

Consider a straight-line fit

```text
y = A*x + B.
```

The best-fit values of `A` and `B` occur at the minimum of the fit objective. To estimate the uncertainty in `A`, Tracker asks a practical question:

> How far can I move `A` before the fit becomes noticeably worse, while still allowing the other fitted coefficients to adjust?

Tracker therefore holds `A` at a nearby trial value, refits `B`, and calculates the resulting chi-square. Repeating this on both sides of the best-fit value traces the local **profile** of chi-square for `A`.

Near a well-behaved minimum that profile is approximately quadratic:

```text
chi^2_profile(A_hat + h) ~= chi^2_min + h^2 / sigma_A^2.
```

So a displacement of about one standard error corresponds locally to an increase

```text
Delta chi^2 ~= 1.
```

Tracker estimates the local curvature using equal trial steps `delta` on either side:

```text
D = chi^2_minus + chi^2_plus - 2*chi^2_min
sigma_A ~= delta * sqrt(2/D).
```

The factor of two appears because `D` contains the increases from **both** sides of the minimum.

This is a symmetric local standard-error estimate. Tracker does not search independently for left and right confidence limits.

## 3. Why the other coefficients are refitted

Suppose the fitted model is still

```text
y = A*x + B.
```

If the trial slope `A` becomes slightly larger, the intercept `B` can often shift to compensate. Holding `B` fixed would answer a different question:

> How uncertain is `A` if `B` is already known exactly?

Refitting `B` asks the experimentally relevant question:

> How uncertain is `A` when both `A` and `B` must be inferred from these same data?

This is why correlated fitted coefficients matter even when Tracker reports one standard error beside each coefficient.

For a regular linear least-squares problem, the local profile-curvature result agrees with the usual covariance-matrix standard error when both calculations use the same measurement-uncertainty scale. The implementation intentionally retains the numerical profile/refit calculation.

## 4. A reproducible straight-line example

Take

```text
x = [0, 1, 2, 3]
y = [1.1, 2.9, 4.9, 7.1]
```

and fit

```text
y = A*x + B.
```

The fitted line is

```text
A = 2
B = 1.
```

The residuals are

```text
+0.1, -0.1, -0.1, +0.1
```

so

```text
SSE_min = 0.04.
```

There are four observations and two independent fitted coefficients, giving

```text
df = 4 - 2 = 2
sigma_y^2 = 0.04 / 2 = 0.02
sigma_y = sqrt(0.02) = 0.141421...
```

The value `0.02` is the **variance**; `0.141421...` is the standard deviation.

### Profile the slope

Let the trial slope be

```text
A = 2 + h.
```

For every trial slope the intercept is refitted. A least-squares line with a free intercept passes through the mean point. Here

```text
x_mean = 1.5
y_mean = 4
```

so

```text
B = y_mean - A*x_mean
  = 4 - (2+h)*1.5
  = 1 - 1.5*h.
```

The resulting profile SSE is

```text
SSE_profile(A) = 0.04 + 5*h^2,
```

where

```text
Sxx = sum((x-x_mean)^2) = 5.
```

Dividing by the fixed residual variance gives

```text
chi^2_profile(A)
    = (0.04 + 5*h^2)/0.02
    = 2 + 5*h^2/0.02.
```

Compare this with the profile expression from section 2:

```text
h^2/sigma_A^2 = 5*h^2/0.02
therefore sigma_A^2 = 0.02/5.
```

The slope standard error is therefore

```text
sigma_A = sqrt(0.02/5)
        = 0.063245553...
```

so the fitted slope may be reported approximately as

```text
A = 2.000 +/- 0.063.
```

### Profile the intercept

The corresponding straight-line result for the intercept is

```text
sigma_B^2 = sigma_y^2 * (1/n + x_mean^2/Sxx)
          = 0.02 * (1/4 + 1.5^2/5)
          = 0.014

sigma_B = 0.1183216...
```

so the fitted intercept may be reported approximately as

```text
B = 1.00 +/- 0.12.
```

The two coefficient uncertainties differ because slope and intercept affect the fitted line in different ways.

### What if the intercept were known exactly?

If `B` were held fixed at 1 instead of being refitted, the slope uncertainty would use `sum(x^2)=14` rather than `Sxx=5`:

```text
sigma_A_fixed_B = sqrt(0.02/14)
                = 0.0377964...
```

This comparison keeps the original variance, `0.02`, unchanged so that it
isolates the effect of treating the intercept as known exactly.

If you actually fix `B=1` in Tracker's default residual-estimated mode, Tracker
also recalculates the variance. The line and SSE remain unchanged in this
example, but only one coefficient is now fitted:

```text
df = 4 - 1 = 3
sigma_y^2 = 0.04/3
sigma_A = sqrt((0.04/3)/14) = 0.0308607...
```

The two calculations use different uncertainty scales. Neither smaller number
is a better estimate of the original problem, in which both coefficients had
to be determined from the data.

## 5. Supplying a different measurement uncertainty

Suppose the same four measurements have an independently justified common uncertainty

```text
sigma_y = 0.20.
```

Then

```text
sigma_y^2 = 0.04.
```

The best-fit line remains `A=2`, `B=1`, because every point still has equal weight. The coefficient uncertainties scale with the supplied uncertainty:

```text
sigma_A = sqrt(0.04/5) = 0.0894427...
sigma_B = sqrt(0.04*0.70) = 0.1673320...
```

The fit statistics now have independent content:

```text
chi^2 = SSE_min/sigma_y^2 = 1
reduced chi^2 = 1/2 = 0.5.
```

The usual chi-square probability interpretation assumes independent Gaussian
measurement errors with the stated standard uncertainties and an adequate
model. For `df=2`, the chi-square survival probability is about `Q=0.607`. `Q` is the probability, assuming the model and uncertainty assumptions, of obtaining a chi-square at least this large from repeated data. It is **not** the probability that the model is true.

## 6. When two coefficients cannot be determined separately

Consider

```text
y = (A+B)*x.
```

The data can determine `A+B`, but they cannot determine `A` and `B` separately. Increasing `A` while decreasing `B` by the same amount leaves every prediction unchanged.

So two editable coefficients do not necessarily represent two independently measurable parameter directions. Tracker estimates the local numerical rank `r` of those directions and uses

```text
df = n - r.
```

For four observations of this model,

```text
p = 2 editable coefficients
r = 1 independent direction
df = 3.
```

Tracker reports a warning for dependent parameters and does not pretend that the individual `A` and `B` uncertainties are meaningful.

The exact numerical rank algorithm is described in the implementation note.

## 7. What this standard error does not include

Tracker's current fit uncertainty model assumes that:

- the x values are known well enough to treat as exact;
- residual measurement errors are independent;
- selected y measurements share a common uncertainty;
- the fitted model is an adequate local description of the data;
- the profile is sufficiently regular near its minimum for a symmetric local error to be useful.

It does **not** automatically include uncertainty from:

- ruler or spatial calibration;
- frame timing;
- lens distortion;
- motion outside the calibration plane;
- an inadequate physical model;
- correlations in derived velocity or acceleration columns.

A small reported coefficient uncertainty therefore does not guarantee an accurate physical result. Conversely, a large uncertainty may make an inaccurate-looking result statistically compatible with an expected value simply because the experiment was imprecise.

Residual plots and experimental choices still matter.

## 8. From fitted coefficients to motion quantities

For a line fit

```text
x = A*t + B,
```

velocity is `A`, so its standard error is `sigma_A`.

For a quadratic position fit

```text
y = A*t^2 + B*t + C,
```

acceleration is

```text
a = 2*A
```

with standard error

```text
sigma_a = 2*sigma_A.
```

The coefficient `B` is velocity at `t=0`. Velocity at another time,

```text
v(t) = 2*A*t + B,
```

depends on both `A` and `B`. Its uncertainty generally requires their covariance or an equivalent reparameterized/profile calculation. Tracker does not obtain that uncertainty by pretending the two coefficient errors are independent.

## Where to go next

If this is enough detail, return to the [short guide](fit-uncertainty-guide.md) and use the fit thoughtfully.

If you want the exact numerical step-size rules, Java code path, rank calculation, perfect-fit behavior, and regression tests, continue to the [Bevington-style implementation note](bevington-fit-uncertainties.md).
