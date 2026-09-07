# Understanding fit uncertainties

Suppose Tracker reports a velocity of **2.000 +/- 0.063 m/s**.

- **2.000 m/s** is the velocity calculated from the fitted line's slope.
- **0.063 m/s** is the estimated uncertainty in that velocity. It estimates the
  typical variation in fitted velocity if you repeated the measurements under
  the same conditions. This estimate depends on the assumptions used in the fit.

The number after `+/-` is an estimate of uncertainty, not a known mistake in the
velocity. Tracker does not know the object's true velocity. A small uncertainty
can accompany an inaccurate result if, for example, the distance calibration is
wrong.

This guide explains where those two numbers come from in OSP PR #9. The
[technical note](bevington-fit-uncertainties.md) describes the calculation in detail.

## How Tracker finds the slope and its uncertainty

For an object moving at constant velocity, position versus time follows a line:

```text
x = A*t + B
```

A is the slope, which gives velocity. B is the position at t=0. Tracker chooses
A and B to make the sum of squared differences between the measured positions
and the line as small as possible. Those differences are called **residuals**.

Finding the uncertainty takes another calculation. Tracker tries a slightly
steeper line and adjusts B to find the best position for that line. It then tries
a slightly shallower line. If even a small slope change makes the residuals much
larger, the slope uncertainty is small. If a wider range of slopes fits nearly
as well, the uncertainty is larger. The calculation also needs an estimate of
how uncertain the position measurements are.

There are therefore two uncertainties to keep separate: uncertainty in **each
measured position**, and uncertainty in the **velocity calculated from the fit**.
They even have different units: metres and metres per second.

Tracker calls a fitted coefficient's uncertainty its **standard error**. It is
not a maximum possible error or a 95% confidence interval. The technical note
explains the method identified in the code with Bevington's Eq. 8.13.

## An example you can reproduce in a spreadsheet

Consider these illustrative position measurements:

| Time (s) | Position (m) |
|---|---|
| 0 | 1.1 |
| 1 | 2.9 |
| 2 | 4.9 |
| 3 | 7.1 |

The fitted line is `x = 2*t + 1`. Its residuals are
`+0.1, -0.1, -0.1, +0.1 m`. Squaring and adding them gives the **sum of squared
residuals**, or SSE: `0.040 m^2`.

With four measurements and two independent fitted coefficients, the estimate
of position scatter is `sqrt(0.040/(4-2)) = 0.141421... m`. This quantity is
called the **residual standard error**. Using it as the uncertainty of each
position measurement gives:

```text
Velocity A          2.000 +/- 0.063 m/s
Position at t=0 B   1.00  +/- 0.12  m
```

The slope uncertainty depends on when the positions were measured as well as
how scattered they are. Spreading measurements over a longer time can improve
the velocity estimate, provided the velocity remains constant.

## Choosing the position uncertainty

**Unweighted - estimate from residuals** is the default. All selected points
receive equal weight. Tracker uses their scatter about the fitted line to
estimate their common measurement uncertainty, as in the example above.
This is useful when you have not estimated that uncertainty independently.
The [NIST least-squares guide](https://itl.nist.gov/div898/handbook/pmd/section4/pmd431.htm)
describes this residual-scatter estimate.

**Known uncertainty (data units)** lets you supply that estimate yourself.
For example, entering `0.20 m` means you are assigning that standard uncertainty
to each position measurement. The example's fitted velocity stays at
`2.000 m/s`, but its standard error increases from about `0.063` to `0.089 m/s`.
The line stays the same because every point still has equal weight.

**Known uncertainty (pixels)** lets you describe marking precision in image
pixels. With a calibration of `0.0025 m/pixel`, choosing `1.0 pixel` assigns a
position uncertainty of `0.0025 m`. Choosing `0.5 pixel` assigns `0.00125 m`.
One pixel is a starting choice; you must judge whether it describes your marking.
This option requires suitable position calibration and does not include
uncertainty in the calibration itself.

## Why a result can agree with expectations despite poor measurements

Careless marking can produce more scatter. In the default mode, more scatter
can produce larger errors on the fitted coefficients. A measured result may
then lie within one reported error of the expected value because that error is
large. Agreement and precision are separate things to examine.

The full report includes two useful statistics:

- **R-squared** describes how much of the observed variation the fit accounts
  for. A value near one does not establish accurate measurement or a correct
  model.
- **Reduced chi-square** compares the residuals with the measurement uncertainty.
  With an independently supplied uncertainty, a value well above one means
  more scatter than expected; a value well below one means less. Several
  causes are possible, so inspect the measurements and model before drawing
  conclusions.

When the measurement uncertainty is estimated from these same residuals,
reduced chi-square is exactly one whenever the calculation is defined. That
value cannot independently confirm the fit. The chi-square probability Q is
therefore shown as N/A in this mode.

## Getting velocity and acceleration from position fits

For a line fit, velocity is A and its standard error is the error reported for A.
For a quadratic `y = A*t^2 + B*t + C`, acceleration is `2*A`; its standard error
is twice the error reported for A.

For example, `A = -4.989 +/- 0.023 m/s^2` gives an acceleration of
`-9.978 +/- 0.046 m/s^2`. B gives velocity at t=0, which need not be the first
selected frame. Tracker includes these results in copied reports when it
identifies the data as position versus time.

This calculation uses the position fit. The pixel setting does not assign
errors to Tracker's separate velocity and acceleration data columns. Those
columns combine measurements from several frames, and their errors can be
related because they reuse the same positions.

## Writing the result in a lab report

Keep full precision during calculations and round when writing the final result.
Tracker displays two significant digits in a positive uncertainty and rounds
the fitted value to the same decimal place. Copied numeric results retain their
full precision.

Record the selected time interval, equation, units, and uncertainty choice.
N/A means that an error estimate is unavailable. This can happen when you enter
parameters manually, hold a coefficient fixed, or provide too little information
to determine a coefficient's uncertainty. Use **... → Copy Full Fit Report**
when you need the additional statistics.
