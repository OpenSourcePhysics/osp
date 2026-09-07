# Bevington-style fit uncertainties: implementation note

Tracker first fits the coefficients of a function to the measured data. It then
estimates an error for each free coefficient by changing that coefficient,
refitting the others, and measuring the increase in chi-square. This second
calculation is the subject of this note.

The implementation is in
[DatasetCurveFitter.java](../src/org/opensourcephysics/tools/DatasetCurveFitter.java)
as used in OSP PR #9. The [short guide](fit-uncertainty-guide.md) introduces the
ideas through a position-versus-time example. This note gives the equations,
code path, and numerical limitations.

The source identifies its uncertainty formula as Eq. 8.13 of *Data Reduction
and Error Analysis for the Physical Sciences*, associated with Philip R.
Bevington and, in later editions, D. Keith Robinson. The code does not specify
an edition or page. The equation-number attribution is inherited from that
comment; an edition-specific match has not been verified here. This note derives
the formula implemented in the code rather than reproducing the book or claiming
that every numerical choice below is prescribed by it.

## 1. The quantity minimized and the measurement uncertainty

An **objective function** is the quantity the fitting algorithm minimizes.
Here it is the sum of squared residuals, SSE. A residual is the difference
between a measured y value and the function's prediction at the same x.
For the selected valid observations, define

```text
residual_i = y_i - f(x_i; parameters)
SSE = sum(residual_i^2)
chi^2 = SSE / sigma_y^2
```

Two kinds of uncertainty occur below. `sigma_y` is the common standard
uncertainty assigned to the measured y values. `sigma_j` is the standard error
calculated for fitted coefficient j. The measurement uncertainty sigma_y is
an input to the coefficient-error calculation; sigma_j is one of its results.

All current uncertainty modes assign the same sigma_y to every selected point.
Dividing SSE by this positive constant squared changes the size of the objective,
but not the coefficient values at its minimum. The optimizer therefore continues
minimizing SSE when sigma_y is supplied. This implements constant-weight least
squares; it does not provide unequal weights for different observations.

The following notation separates the data count from the parameter count:

| Symbol | Meaning |
|---|---|
| n | Number of selected valid observations |
| p | Number of coefficients the optimizer may change |
| r | Number of independent parameter directions, estimated numerically; see section 6 |
| df | Residual degrees of freedom, n-r |
| SSE_min | SSE evaluated at the fitted coefficients |

The measurement uncertainty is obtained as follows:

| Mode | Scale used | Interpretation of fit minimum |
|---|---|---|
| Estimated from residuals | sigma_y^2=SSE_min/df | chi^2_min=df; reduced chi^2=1 by construction, when defined |
| Supplied common sigma_y | user-supplied sigma_y^2 | chi^2_min=SSE_min/sigma_y^2; no residual rescaling |
| Supplied pixel sigma | (pixel sigma × physical units per pixel)^2 | same as supplied common sigma after conversion |

For full-rank regression the first expression reduces to the usual residual
variance estimate with n-p in the denominator; see
[NIST's least-squares treatment](https://itl.nist.gov/div898/handbook/pmd/section4/pmd431.htm).
The rank substitution and the supplied-scale modes are implementation details
of this PR. Invalid supplied values are not replaced by a residual estimate.

`calibrateChiSquared` calculates sigma_y once at the fitted coefficients.
`getChiSquared` keeps it fixed while testing nearby coefficient values.
If sigma_y were recalculated from the new residuals at each test value, the
normalization would hide the worsening fit that the calculation needs to measure.
The field `sigma_y_squared` holds a variance despite its older comment referring
to a standard deviation.

## 2. Profile curvature and the factor of two

Hold one coefficient at a trial value and refit the others. Repeat at other
trial values. The resulting minimum chi-square as a function of that one
coefficient is called its **profile**. Its **curvature** measures how quickly
chi-square rises as the tested coefficient moves away from its fitted value.

For coefficient a_j, write the profile as

```text
P_j(a) = minimum chi^2 with a_j held at a,
         refitting all other free parameters
```

Coefficients the user has fixed remain fixed throughout. Let a_hat be the
fitted value of the coefficient being tested, and h a small change in that
value. If the data determine that coefficient and the profile is approximately
quadratic near its minimum, then:

```text
P_j(a_hat + h) ≈ chi^2_min + (1/2)*P_j''(a_hat)*h^2
              ≈ chi^2_min + h^2/sigma_j^2
```

Here `P_j''` is the second derivative of the profile. Comparing the two
expressions gives `sigma_j^2 ≈ 2/P_j''`. The code estimates that second
derivative from two trial values, equally spaced by delta on either side:

```text
D = P_j(a_hat-delta) + P_j(a_hat+delta) - 2*chi^2_min
P_j'' ≈ D/delta^2
sigma_j ≈ delta * sqrt(2/D)
```

D is the code's `twiceDeltaChiSq`: the sum of the two profile increases, not
one increase. Dropping the factor of two would underestimate the error by
sqrt(2) for a quadratic profile.

Under this approximation, moving one parameter by one standard error raises
its profiled chi-square by about one. The implementation estimates a symmetric
local error; it does **not** search for exact left/right Delta-chi-square=1
crossings or construct an asymmetric confidence interval. For a broader
account of profiling and likelihood-based errors, see the
[Particle Data Group statistics review](https://pdg.lbl.gov/2021/reviews/rpp2021-rev-statistics.pdf).
That review is background, not evidence for this implementation's step sizes.

## 3. Why the other parameters must be refitted

Consider `y=A*x+B`. Changing A generally requires changing B to keep the line
near the same observations. Holding B at its original fitted value asks how
precisely A is determined
*if B is already known*. Letting B adjust asks how precisely A is determined
*when B must also be estimated from these data*. The first calculation is called
a conditional slice; the second is a profile. They generally give different
errors because changes in A and B can partly compensate for each other.

The matrix calculation below shows how profiling relates to covariance.
The worked example in section 4 illustrates the same distinction without matrices.

For an exact quadratic objective, write the increase in chi-square as `d^T H d`.
Here d contains all coefficient changes, and H is half the Hessian—the matrix
of second derivatives of chi-square. Separate d into the tested coefficient's
change h and the other coefficients' changes z. Subscripts j and o below refer
to the tested coefficient and the other coefficients, respectively. Minimizing
over z gives

```text
z_best = -H_oo^(-1)*H_oj*h
Delta chi^2_profile = h^2*(H_jj - H_jo*H_oo^(-1)*H_oj)
sigma_j^2 = 1/(H_jj - H_jo*H_oo^(-1)*H_oj) = (H^(-1))_jj
```

Thus exact profiling and the inverse-curvature diagonal agree in a regular,
full-rank quadratic problem. For linear-in-parameter least squares this also
agrees with the usual covariance formula using the same uncertainty scale.
The PR preserves numerical profiling; it does not replace it with a Jacobian
covariance calculation. For nonlinear models, finite steps, nonquadratic
profiles, and imperfect minimization can produce differences.

The separate rank calculation evaluates a Jacobian: a matrix describing how
the predicted data change when each coefficient changes. It uses that matrix
to count independent parameter directions. It does not use it to calculate
parameter errors.

## 4. A reproducible analytic example

Take `x=[0,1,2,3]`, `y=[1.1,2.9,4.9,7.1]` and fit `y=A*x+B`.
The following steps trace the residuals through to the coefficient uncertainties.

### Start with the fitted line and residual variance

The fitted coefficients are A=2 and B=1. Check the residuals directly:

| x | Measured y | Predicted y=2*x+1 | Residual e=measured-predicted |
|---|---|---|---|
| 0 | 1.1 | 1.0 | +0.1 |
| 1 | 2.9 | 3.0 | -0.1 |
| 2 | 4.9 | 5.0 | -0.1 |
| 3 | 7.1 | 7.0 | +0.1 |

```text
SSE_min = 0.1^2 + (-0.1)^2 + (-0.1)^2 + 0.1^2 = 0.04
n = 4 observations
r = 2 independent fitted coefficients
df = n-r = 4-2 = 2
sigma_y^2 = SSE_min/df = 0.04/2 = 0.02
```

**0.02 is the estimated variance**, not the standard deviation. The standard
deviation is `sigma_y=sqrt(0.02)=0.141421...`. Chi-square divides SSE by the
variance, so the minimum chi-square is `0.04/0.02=2`.

### Change the slope and refit the intercept

Let h be a change in slope, so the trial slope is `A=2+h`. The intercept must
be refitted at each trial slope. A least-squares line with a free intercept
passes through the mean point `(x_mean,y_mean)`:

```text
x_mean = (0+1+2+3)/4 = 1.5
y_mean = (1.1+2.9+4.9+7.1)/4 = 4
B = y_mean - A*x_mean
  = 4 - (2+h)*1.5
  = 1 - 1.5*h
```

The trial line's prediction therefore differs from the original line by
`h*(x-1.5)`. Each new residual is `e-h*(x-1.5)`, where e is the original
residual in the table. Square those new residuals and add them:

```text
SSE_profile(A) = sum([e-h*(x-1.5)]^2)
               = sum(e^2) - 2*h*sum(e*(x-1.5)) + h^2*sum((x-1.5)^2)
```

The middle sum is zero:

```text
sum(e*(x-1.5)) = 0.1*(-1.5) + (-0.1)*(-0.5)
                + (-0.1)*0.5 + 0.1*1.5
              = -0.15 + 0.05 - 0.05 + 0.15 = 0
```

The last sum, called Sxx, gives the **5**:

```text
Sxx = sum((x-x_mean)^2)
    = (-1.5)^2 + (-0.5)^2 + 0.5^2 + 1.5^2
    = 2.25 + 0.25 + 0.25 + 2.25 = 5

SSE_profile(A) = 0.04 + 5*h^2
```

### Divide by the residual variance

Keep the estimated variance at 0.02 while testing the trial slopes. To convert
SSE to chi-square, divide by that variance:

```text
chi^2_profile(A) = SSE_profile(A)/sigma_y^2
                 = (0.04 + 5*h^2)/0.02
                 = 2 + (5*h^2)/0.02
```

The increase above the minimum is therefore `(5*h^2)/0.02`: the increase in
SSE divided by the residual variance.

### Add the increases on both sides

The code tests two slope changes: `h=-delta` and `h=+delta`. Squaring removes
the sign, so both trials have the same increase in this example:

```text
chi^2_minus = 2 + (5*delta^2)/0.02
chi^2_plus  = 2 + (5*delta^2)/0.02
chi^2_min   = 2

D = chi^2_minus + chi^2_plus - 2*chi^2_min
  = [2 + (5*delta^2)/0.02] + [2 + (5*delta^2)/0.02] - 2*2
  = (2*5*delta^2)/0.02
```

The factor of 2 counts the two equal increases. As a numerical check, choose
`delta=0.01`. Each increase is `5*(0.01)^2/0.02=0.025`, so each trial has
chi-square 2.025 and `D=2.025+2.025-4=0.05`.

### Convert that increase into the slope uncertainty

Substitute D into the curvature formula from section 2. The factor of 2
cancels, and since delta is a positive step size, delta cancels too:

```text
sigma_A = delta*sqrt(2/D)
        = delta*sqrt(2/[(2*5*delta^2)/0.02])
        = delta*sqrt(0.02/(5*delta^2))
        = sqrt(0.02/5)
        = 0.0632455532033676
```

The result is the square root of the residual variance divided by Sxx.
Using the numerical trial above gives the same answer:
`0.01*sqrt(2/0.05)=0.0632455532033676`. This cancellation is exact for this
quadratic profile; a numerical nonlinear fit need not behave so simply.

### Calculate the intercept uncertainty

For a straight line, profiling the intercept while refitting the slope gives
the familiar expression below. Its inputs have all been calculated above:
the variance is 0.02, n is 4, x_mean is 1.5, and Sxx is 5.

```text
sigma_B^2 = sigma_y^2*(1/n + x_mean^2/Sxx)
          = 0.02*(1/4 + 1.5^2/5)
          = 0.02*(0.25 + 2.25/5)
          = 0.02*(0.25 + 0.45)
          = 0.014

sigma_B = sqrt(0.014) = 0.1183215956619923
```

The `1/n` term describes uncertainty in the fitted line's height at x_mean.
The `x_mean^2/Sxx` term adds the effect of uncertainty in the slope when finding
the intercept at x=0. They combine to give the intercept variance.

### Compare with holding the intercept fixed

If B were held at 1 rather than refitted, changing A would change the prediction
by `h*x` instead of `h*(x-x_mean)`. The squared-residual increase would then
use `sum(x^2)=0+1+4+9=14` instead of Sxx=5. The resulting slope error would be
`sqrt(0.02/14)=0.0377964473`. This comparison keeps the original variance,
0.02, unchanged to isolate the effect of treating B as known exactly.

Actually fixing B=1 in Tracker's default residual-estimated mode also changes
the variance estimate. The fitted line and SSE remain unchanged in this
example, but the free parameter rank drops from two to one:

```text
df = 4 - 1 = 3
sigma_y^2 = 0.04/3
sigma_A = sqrt((0.04/3)/14) = 0.0308606699924184
```

This second calculation includes both the fixed-intercept assumption and the
re-estimated residual variance. Neither is the original problem of estimating
both A and B.

### Change the supplied measurement uncertainty

If the independently supplied sigma_y is 0.20 instead, its variance is 0.04.
A and B stay unchanged. Substituting this variance gives
`sigma_A=sqrt(0.04/5)=0.0894427191` and
`sigma_B=sqrt(0.04*0.70)=0.1673320053`.

Chi-square is now `SSE_min/sigma_y^2=0.04/0.04=1`, and reduced chi-square is
`1/df=1/2=0.5`. For df=2, the survival probability is
`Q=exp(-chi^2/2)=exp(-1/2)=0.6065306597`.
With the residual-estimated variance of 0.02, chi-square is 2, reduced
chi-square is 1, and Q is N/A. More generally, multiplying a supplied sigma_y
by a positive factor c multiplies regular profile errors by c and divides
chi-square by c^2, up to numerical error.

## 5. Code path and numerical procedure

Relevant methods in `DatasetCurveFitter` are `fit`, `getTestFunction`,
`getUncertainties`, `calibrateChiSquared`, `getChiSquared`,
`refreshUncertaintyModel`, and `setUncertainties`.

1. The existing coefficient fit runs. An unconstrained `KnownPolynomial` uses
   its polynomial fitting routine. `UserFunction` fitting uses the existing
   Hessian minimizer, with Levenberg-Marquardt attempted when that fit worsens
   the objective; unsuccessful/worse results are restored. The current caller
   uses 20 iterations and tolerance 1e-6 for these numerical fits.
2. `getTestFunction(f, fixedParams)` removes user-fixed parameters from the
   free representation. `getUncertainties` requires at least one free parameter
   and positive residual degrees of freedom, then establishes the scale.
3. Parameter names map the free representation back to the original order.
   Missing/fixed parameter errors start as NaN.
4. For each free parameter with value a, start with
   `delta=(abs(a)+1)/100000`. Construct two temporary `UserFunction` objects
   representing that parameter held at a-delta and a+delta. The helper actually
   substitutes the parameter name with a numeric literal in the expression;
   it is not a general symbolic algebra engine.
5. Refit each temporary function's remaining parameters with `fit(test)` and
   calculate D using the fixed uncertainty scale. Test functions are recognized
   so they do not recursively launch their own uncertainty calculation.
6. Aim for `0.001 <= D <= 2`, with at most ten attempts. If the preceding D is
   nonzero and below 0.001, multiply delta by ten; if above 2, divide delta by
   ten. The first D is initialized to zero. Exactly zero D does not enlarge the
   step in the current code, so a flat or cancellation-limited case can repeat
   the same perturbation until the limit. Nonfinite D fails the final check.
7. After the loop, a finite D>=0.001 is accepted and the error is
   `delta*sqrt(2/D)`. The upper target of 2 is not a final rejection condition
   after the attempt limit. These thresholds are numerical heuristics, not
   confidence levels or guaranteed convergence criteria.
8. For an accepted error, perform two more constrained refits at a±sigma_j.
   Their parameter arrays are passed with the errors to the function drawer.
   These are not a stored covariance matrix or a simultaneous confidence band.

Each tested parameter can therefore request up to 20 shifted refits plus two
additional drawer refits. For a model with only one free coefficient, holding that coefficient at a
trial value leaves no other coefficient to refit. Runtime otherwise depends on
model evaluation, data size, and convergence of the numerical refits.

Changing only the uncertainty choice calls `refreshUncertaintyModel`, which
clones the displayed function before recalculating its profile errors. The
original best-fit coefficients are preserved. The implementation reuses cached temporary functions and the normal fitting
code, which also refreshes parts of the display. It is not a separate numerical
routine that only returns an array of errors. Tests cover the user-visible coefficient,
Autofit, fixed-parameter, and selection behavior.

## 6. Rank, fixed parameters, and perfect fits

[CurveFitReport.parameterRank](../src/org/opensourcephysics/tools/CurveFitReport.java)
uses normalized free-parameter derivative columns with pivoted, reorthogonalized
Gram-Schmidt and tolerance 1e-7. Polynomial derivatives are analytic; other
functions use central differences with step `1e-5*max(1,abs(parameter))`.
This is separate from the adaptive profile step above. For nonlinear models,
the rank is local and does not establish global identifiability.

A concrete example is `y=(A+B)*x`: the data can determine A+B, but cannot
separately determine A and B. Increasing either coefficient by the same amount
has the same effect on every predicted value, so their derivative columns are
identical. Four observations
have p=2, r=1, and df=3. Refitting B can compensate for a perturbation in A:
individual A and B errors should be unavailable. The rank determines df and whether the report warns about dependent parameters.
Each coefficient error still depends on its own curvature check. The software
does not rewrite this function as a one-parameter fit to A+B.

A fixed coefficient reports N/A, not an estimated error of zero: its value is
an input constraint, and uncertainty in that input has not been propagated.
In manual mode, the software likewise does not report errors left over from
an earlier automatic fit. The current
implementation withholds parameter-error estimates when df<=0 even if a
supplied noise model could support inference in a different implementation.

For exactly zero residual variance in estimated mode, the code temporarily
uses unscaled SSE to check profile curvature. An identifiable direction can
then receive zero estimated error; an unmeasurable direction remains NaN.
This is a consequence of the zero-scatter model, not proof of exact physical
knowledge. Other guards, including constant-response handling, can also make
errors unavailable. Exported chi-square remains N/A for the zero estimated
scale. A supplied positive sigma can give nonzero parameter errors even for
an exact fit; its chi-square is zero and Q=1 when df>0.

## 7. Interpretation and limits

The basic uncertainty model treats x as known and residual measurement errors
as independent with a common y variance. Gaussian errors and an adequate model
are needed for the usual chi-square probability interpretation. The report's
Q is the chi-square survival probability for the fitted residuals, not a
parameter p-value or the probability that the model is true. In estimated mode
Q is deliberately unavailable because the scale was learned from those residuals.

For small samples, estimated standard errors are not automatically normal-based
confidence intervals; a regular linear model with unknown variance ordinarily
uses Student-t factors for coefficient intervals. A symmetric local error can also be misleading when the function depends
nonlinearly on its coefficients, a coefficient is near a physical boundary,
the data barely determine it, or several different coefficient sets fit well. Neither symmetric/asymmetric confidence intervals nor parameter
p-values are added by this PR.

The numerical calculation has several limitations. Subtracting nearly equal
chi-square values can lose precision. Changing coefficient units affects the
step rule because it adds the number 1 to the coefficient magnitude. A refit
can stop short of its minimum or settle in a different local minimum. A profile
can depart substantially from a parabola. Passing the D threshold does not
establish that these problems were avoided. Very small/large uncertainty scales can
also underflow/overflow when squared. Existing tests exercise selected cases,
not all possible UserFunctions or parameterizations.

Pixel conversion treats the calibration transform as given. It does not
propagate uncertain rulers, frame timing, lens distortion, common calibration
errors, or temporal correlations. The default residual estimate can absorb
marking noise and model inadequacy together. A large fitted error can make a
comparison with an expected value look reassuring while the measurement is
imprecise. Inspecting residuals and measurement choices remains necessary.

Copied motion results use only exact single-coefficient transformations:
line-fit velocity A, quadratic velocity at t=0 B, and acceleration 2A with error
2*sigma_A. Velocity at another time, `2*A*t+B`, would require the A/B covariance
or a suitable reparameterized profile fit; it is not inferred by treating the
coefficient errors as independent.

## 8. Verification and maintenance

- [CurveFitPhysicsTest](org/opensourcephysics/tools/CurveFitPhysicsTest.java)
  checks unchanged coefficients across uncertainty modes, supplied-scale error
  scaling, UserFunction profile behavior, rank/df, perfect-fit cases, pixels,
  and derived motion transformations.
- [CurveFitReportTest](org/opensourcephysics/tools/CurveFitReportTest.java)
  includes the analytic four-observation line and report conventions.
- [CurveFitConstraintTest](org/opensourcephysics/tools/CurveFitConstraintTest.java)
  checks fixed/manual behavior and redundant parameters.
- [CurveFitPrecisionTest](org/opensourcephysics/tools/CurveFitPrecisionTest.java)
  separates full-precision values from display rounding.

See [test instructions](README.md) and
[units, uncertainty modes, and integration](fit-report-physics.md). The same
computational fixtures run on the desktop and through SwingJS. The derivation
and worked numbers above are independently checkable algebra; they are not
claims that every theoretical profile is resolved exactly by the finite-step
implementation. This documentation makes no change to that implementation.
