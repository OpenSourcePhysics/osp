# Understanding fit uncertainties

A fit gives a best estimate and an uncertainty. They answer different questions:
**what value fits these measurements best, and how precisely do the measurements
constrain it?** A small uncertainty does not establish that the model or the
measurements are correct.

This guide describes the Data Tool behavior in OSP PR #9. The
[technical note](bevington-fit-uncertainties.md) explains the implementation.

## What the error beside a coefficient means

Suppose a line describes position versus time:

```text
x = A*t + B
```

The slope A is velocity; B is position at t=0. To investigate uncertainty in A,
Tracker moves the slope slightly away from its best value, allows B to adjust,
and checks how much worse the fit becomes. It repeats this on the other side.
If the fit becomes worse quickly, the data constrain A tightly. If many nearby
slopes fit almost as well, its uncertainty is larger. This is the idea behind
the profile/refit curvature method identified in the code with Bevington's
Eq. 8.13.

The reported error is a **standard-error estimate**, not a maximum possible
error and not a 95% confidence interval. Its interpretation depends on the
measurement-error model and the adequacy of the fitted function.

## A small example you can reproduce in a spreadsheet

These are illustrative measurements, not a Tracker video:

| Time (s) | Position (m) |
|---|---|
| 0 | 1.1 |
| 1 | 2.9 |
| 2 | 4.9 |
| 3 | 7.1 |

The best line is `x = 2*t + 1`. The measured positions differ from it by
`+0.1, -0.1, -0.1, +0.1 m`, so the sum of squared residuals (SSE) is
`0.040 m^2`.

With four observations and two independent fitted parameters, the residual
standard error is `sqrt(0.040/2) = 0.141421... m`. Using that estimated scatter,
the coefficient results are approximately:

```text
Velocity A       2.000 +/- 0.063 m/s
Position B       1.00  +/- 0.12  m
```

The velocity uncertainty is not the same as the scatter of individual position
measurements. It depends on the measurement times as well as the position
scatter. A longer time span can constrain a slope more tightly, provided the
same model still applies.

## Where the uncertainty scale comes from

**Unweighted - estimate from residuals** is the default. All selected points
receive equal weight, and their scatter about the fit estimates the common
position uncertainty. This is a useful starting point when no independent
measurement uncertainty has been supplied. The usual residual-scatter estimate
is described in the [NIST least-squares guide](https://itl.nist.gov/div898/handbook/pmd/section4/pmd431.htm).

**Known uncertainty (data units)** uses the positive uncertainty you supply.
For the same example, specifying `0.20 m` leaves the best line unchanged, but
makes the velocity uncertainty approximately `0.089 m/s`. The chosen uncertainty
scale has increased; the measured points have not changed.

**Known uncertainty (pixels)** supplies the same kind of model through Tracker's
position calibration. If the calibration is `0.0025 m/pixel`, a choice of
`1.0 pixel` means `0.0025 m`; `0.5 pixel` means `0.00125 m`. One pixel is a
convenient starting choice, not an assertion about your marking accuracy.
The current conversion does not include uncertainty in the calibration itself.
Pixel mode is available only for eligible directly calibrated position data.

## What agreement does—and does not—tell you

Careless marking can increase the residual scatter and therefore increase
residual-estimated parameter errors. A result can then agree with an expected
value within its uncertainty because it is imprecise. A small discrepancy in
units of the reported error (often called a Z-value) does not, by itself,
establish good measurement technique.

The full report offers two different checks:

- **R-squared** describes how much of the observed variation the fitted model
  accounts for. It does not establish that residuals are small relative to your
  measurement uncertainty.
- **Reduced chi-square** compares residuals with the supplied uncertainty scale.
  When Tracker estimates that scale from the same residuals, reduced chi-square
  is 1 by construction whenever defined. It is then not an independent test,
  and the chi-square probability Q is N/A.

With a genuinely supplied common uncertainty, a reduced chi-square much above
one can indicate underestimated uncertainty, a poor model, or other violated
assumptions. A value well below one can indicate an overestimate or correlations.
These are reasons to inspect the experiment, not automatic diagnoses.

## Position fits can give motion results directly

For a line fit to position versus time, the slope and its error are velocity
and its error. For a quadratic `y = A*t^2 + B*t + C`, acceleration is `2*A` and
its standard error is `2*error(A)`.

For example, if `A = -4.989 +/- 0.023 m/s^2`, the acceleration is
`-9.978 +/- 0.046 m/s^2`. The actual software uses unrounded values before
formatting the result. B gives velocity at the data's t=0, which need not be the
first selected frame. These derived quantities appear in copied reports when
Tracker identifies the columns as position versus time.

The pixel setting does not propagate uncertainties into the separately
calculated velocity/acceleration columns. Those calculations reuse positions,
so their errors can be correlated. For this purpose, fitting the measured
position data avoids a second regression on differentiated data.

## Reporting a result

Keep the full precision for subsequent calculations; round at the reporting
stage. Tracker displays two significant digits in positive uncertainty values
and matches the coefficient's displayed decimal place to its uncertainty.
Copied numeric results retain full precision.

Record the fitted interval, equation, units, and uncertainty choice with the
result. If an error is N/A, it is unavailable—not zero. Manual parameters,
fixed coefficients, insufficient information, and unidentifiable parameters
require particular care. The full report is available from **... → Copy Full
Fit Report** when you need the details.
