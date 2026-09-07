# Understanding fit uncertainties

> **A number from a fit is not the answer; it is a measurement with assumptions attached.**

Suppose Tracker reports a velocity of **2.000 +/- 0.063 m/s**.

- **2.000 m/s** is the velocity calculated from the fitted line's slope.
- **0.063 m/s** is Tracker's estimate of how precisely that velocity was determined from the selected measurements and uncertainty model.

The number after `+/-` is not a known mistake in the velocity. Tracker does not know the object's true velocity. A small uncertainty can accompany an inaccurate result if, for example, the distance calibration is wrong.

This is the short guide for using fit uncertainties in a lab.

- **Want the statistical reasoning and a worked example?** Continue to [the details](fit-uncertainty-details.md).
- **Reviewing the numerical implementation?** See the [Bevington-style implementation note](bevington-fit-uncertainties.md).

## How Tracker finds the slope and its uncertainty

For an object moving at constant velocity, position versus time follows a line:

```text
x = A*t + B
```

`A` is the slope, which gives velocity. `B` is the position at `t=0`.

Tracker chooses `A` and `B` to make the measured positions lie as close as possible to the fitted line. The vertical differences between the measurements and the line are called **residuals**.

To estimate the uncertainty in `A`, Tracker tries a slightly steeper line and lets `B` readjust to give the best possible fit. It also tries a slightly shallower line. If even a small change in slope makes the fit much worse, the slope is tightly determined. If many nearby slopes fit almost equally well, the slope uncertainty is larger.

There are two uncertainties to keep separate:

- uncertainty in **each measured position**;
- uncertainty in the **velocity calculated from all of those positions**.

They even have different units: metres and metres per second.

Tracker calls a fitted coefficient's uncertainty its **standard error**. It is not a maximum possible error and it is not automatically a 95% confidence interval.

## A small example

Suppose four position measurements are

| Time (s) | Position (m) |
|---|---|
| 0 | 1.1 |
| 1 | 2.9 |
| 2 | 4.9 |
| 3 | 7.1 |

The fitted line is

```text
x = 2*t + 1
```

and Tracker reports approximately

```text
Velocity A          2.000 +/- 0.063 m/s
Position at t=0 B   1.00  +/- 0.12  m
```

The points do not fall exactly on the line. Their scatter helps determine how precisely the slope and intercept are known.

The full calculation, including residuals, degrees of freedom, and why the slope uncertainty is `0.063 m/s`, is worked out in [the statistics guide](fit-uncertainty-details.md).

## Choosing the position uncertainty

Tracker currently gives every selected point the same position uncertainty. You can choose how that common uncertainty is obtained.

### Unweighted - estimate from residuals

This is the default. Tracker gives all selected points equal weight and estimates their common uncertainty from how much they scatter about the fitted curve.

This is useful when you do not have an independent estimate of the measurement uncertainty.

An important consequence is that **reduced chi-square is exactly 1 by construction** whenever this calculation is defined. That value cannot independently confirm that the model is good.

### Known uncertainty (data units)

Use this when you have an independent estimate of the uncertainty of each measured position.

For example, entering `0.20 m` means that you are assigning a standard uncertainty of `0.20 m` to every selected position. Because every point still has the same weight, the best-fit line does not move. The reported coefficient uncertainties and chi-square statistics do change.

### Known uncertainty (pixels)

This describes marking precision in image pixels.

For example, with a calibration of `0.0025 m/pixel`:

```text
1.0 pixel -> 0.0025 m
0.5 pixel -> 0.00125 m
```

One pixel is a reasonable starting point, not a universal truth. You must judge how precisely the object can actually be marked.

This setting describes marking uncertainty only. It does not include uncertainty in the ruler, camera geometry, timing, or calibration itself.

## A precise result can still be wrong

Suppose a class measures gravitational acceleration and obtains

```text
g = 10.10 +/- 0.03 m/s^2.
```

The small uncertainty says the tracked points determine that fitted value very consistently. It does **not** say that the calibration was correct or that the physical model included every important effect.

Calibration, timing, lens distortion, motion outside the calibration plane, or a poor model can produce a precise but inaccurate result.

Likewise, careless marking can increase the scatter and make the reported uncertainty larger. An inaccurate result may then appear to agree with an expected value simply because the experiment was imprecise.

**Precision and accuracy are different questions.**

## What R-squared and reduced chi-square tell you

Tracker's full fit report includes several statistics. Two are especially useful in introductory work.

### R-squared

R-squared describes how much of the observed variation the fitted curve accounts for.

A value close to one means the curve follows the data closely. It does **not** prove that:

- the physical model is correct;
- the calibration is correct;
- the result is accurate;
- the measurement uncertainty is realistic.

A high R-squared is therefore useful, but it is not a certificate of experimental correctness.

### Reduced chi-square

When you supply an independent measurement uncertainty, reduced chi-square compares the observed residual scatter with that stated uncertainty.

As a rough guide:

- near 1: scatter is comparable to what the uncertainty model predicts;
- much greater than 1: more scatter than expected;
- much less than 1: less scatter than expected.

Those outcomes can have several causes. Inspect the residuals, measurements, calibration, and model before drawing conclusions.

In the default residual-estimated mode, reduced chi-square is 1 by construction, so Tracker does not treat it as an independent fit check. The chi-square probability `Q` is shown as N/A in that mode.

The [statistics guide](fit-uncertainty-details.md) explains why.

## Getting velocity and acceleration from position fits

For a line fit

```text
x = A*t + B
```

velocity is `A`, and its standard error is the standard error reported for `A`.

For a quadratic position fit

```text
y = A*t^2 + B*t + C
```

acceleration is

```text
a = 2*A
```

and its standard error is

```text
sigma_a = 2*sigma_A.
```

For example,

```text
A = -4.989 +/- 0.023 m/s^2
```

gives

```text
a = -9.978 +/- 0.046 m/s^2.
```

`B` gives the velocity at `t=0`, which need not be the first selected frame.

Tracker's pixel-uncertainty setting applies to the fitted position measurements. It does not independently assign uncertainties to Tracker's separate velocity and acceleration data columns, because those derived columns reuse measurements from several frames.

## Writing the result in a lab report

Keep full precision during calculations and round when reporting the final result. Tracker displays two significant digits in a positive uncertainty and rounds the fitted value to the same decimal place. Copied numeric results retain full precision.

Record:

- the selected time interval;
- the fitted equation;
- units;
- the uncertainty choice;
- the fitted result and its uncertainty.

Also inspect the graph and residuals rather than reporting fit statistics alone.

`N/A` means that an uncertainty estimate is unavailable. This can occur when parameters are entered manually, a coefficient is held fixed, or the data do not independently determine a coefficient.

Use **... -> Copy Full Fit Report** when you need the additional statistics.

## Want to know why?

The next step is [Understanding fit uncertainties: the statistics behind the result](fit-uncertainty-details.md). It explains residual variance, degrees of freedom, profile curvature, why other coefficients must be refitted, and the `2.000 +/- 0.063 m/s` example in detail.
