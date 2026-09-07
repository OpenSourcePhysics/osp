# Bevington-style fit uncertainties: implementation note

This note documents the profile/refit curvature calculation in
[DatasetCurveFitter.java](../src/org/opensourcephysics/tools/DatasetCurveFitter.java)
as used in OSP PR #9. Start with the
[short guide and examples](fit-uncertainty-guide.md) for an introduction.

The source identifies its uncertainty formula as Eq. 8.13 of *Data Reduction
and Error Analysis for the Physical Sciences*, associated with Philip R.
Bevington and, in later editions, D. Keith Robinson. The code does not specify
an edition or page. The equation-number attribution is inherited from that
comment; an edition-specific match has not been verified here. This note derives
the formula implemented in the code rather than reproducing the book or claiming
that every numerical choice below is prescribed by it.

## 1. Objective and uncertainty scale

For the selected valid observations, define

```text
residual_i = y_i - f(x_i; parameters)
SSE = sum(residual_i^2)
chi^2 = SSE / sigma_y^2
```

The current modes all use one common y uncertainty. Multiplying SSE by a
positive constant does not change its minimizing coefficients. The optimizer
therefore continues minimizing SSE, including when sigma_y is supplied. This
is mathematically constant-weight least squares; it does not imply support for
unequal per-observation weights.

Let n be the number of selected valid observations, p the number of editable
free parameters, r their effective independent numerical rank, and df=n-r.
The uncertainty scale is:

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

`calibrateChiSquared` establishes the scale once at the fitted minimum.
`getChiSquared` uses that same scale while parameters are perturbed. Re-estimating
sigma_y after every perturbation would erase the increase being measured.
The field `sigma_y_squared` holds a variance despite its older comment referring
to a standard deviation.

## 2. Profile curvature and the factor of two

For parameter a_j, define its profile objective conceptually as

```text
P_j(a) = minimum chi^2 with a_j held at a,
         refitting all other free parameters
```

Original user-fixed parameters remain fixed throughout. Near an identifiable
minimum a_hat, use a local quadratic approximation:

```text
P_j(a_hat + h) ≈ chi^2_min + (1/2)*P_j''(a_hat)*h^2
              ≈ chi^2_min + h^2/sigma_j^2
```

It follows that `sigma_j^2 ≈ 2/P_j''`. A symmetric finite difference gives

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
near the same observations. Holding B at its old fitted value measures a
conditional slice through the objective. Letting B adjust measures a profile.
These are different questions unless the parameters are uncoupled.

For an exact quadratic objective, write its increment as `d^T H d`, where H is
half the Hessian of chi-square. Partition d into the tested displacement h and
other displacements z. Minimizing over z gives

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

The rank calculation does evaluate a Jacobian. That is for counting independent
free directions, not for replacing the parameter-error method.

## 4. A reproducible analytic example

Take `x=[0,1,2,3]`, `y=[1.1,2.9,4.9,7.1]` and fit `y=A*x+B`.
The exact least-squares coefficients are A=2 and B=1, SSE=0.04, n=4, r=2,
df=2, and estimated sigma_y=sqrt(0.02).

Here `x_mean=1.5` and `Sxx=sum((x-x_mean)^2)=5`. At a perturbed slope `2+h`,
the refitted intercept is `1-1.5*h`. Consequently,

```text
SSE_profile(A) = 0.04 + 5*h^2
chi^2_profile(A) = 2 + 250*h^2
D = 500*delta^2
sigma_A = sqrt(0.02/5) = 0.0632455532033676
sigma_B = sqrt(0.02*(1/4 + 1.5^2/5)) = 0.1183215956619923
```

Holding B fixed instead would use sum(x^2)=14 and give the smaller conditional
slope error `sqrt(0.02/14)=0.0377964473`. That is why the refit matters.

If the independently supplied sigma_y is 0.20 instead, A and B are unchanged,
but `sigma_A=0.0894427191`, `sigma_B=0.1673320053`, chi-square=1,
reduced chi-square=0.5, and Q=exp(-1/2)=0.6065306597 for df=2.
With the residual-estimated scale, chi-square=2, reduced chi-square=1,
and Q is N/A. More generally, scaling a supplied sigma_y by c scales regular
profile errors by |c| and divides chi-square by c^2, up to numerical error.

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
additional drawer refits. A model with one free parameter leaves no nuisance
parameter to optimize in its shifted functions. Runtime otherwise depends on
model evaluation, data size, and convergence of the numerical refits.

Changing only the uncertainty choice calls `refreshUncertaintyModel`, which
clones the displayed function before recalculating its profile errors. The
original best-fit coefficients are preserved. The implementation reuses cached
temporary functions and the normal fit infrastructure; it is not an isolated,
side-effect-free statistical library. Tests cover the user-visible coefficient,
Autofit, fixed-parameter, and selection behavior.

## 6. Rank, fixed parameters, and perfect fits

[CurveFitReport.parameterRank](../src/org/opensourcephysics/tools/CurveFitReport.java)
uses normalized free-parameter derivative columns with pivoted, reorthogonalized
Gram-Schmidt and tolerance 1e-7. Polynomial derivatives are analytic; other
functions use central differences with step `1e-5*max(1,abs(parameter))`.
This is separate from the adaptive profile step above. For nonlinear models,
the rank is local and does not establish global identifiability.

For `y=(A+B)*x`, the two derivative columns are identical. Four observations
have p=2, r=1, and df=3. Refitting B can compensate for a perturbation in A:
individual A and B errors should be unavailable. Rank drives df and the report's
warning; each coefficient error still depends on its own profile-curvature
check. The method does not replace a redundant fit with a uniquely identified
parameterization.

A fixed coefficient reports N/A, not an estimated error of zero: its value is
an input constraint, and uncertainty in that input has not been propagated.
Manual mode likewise does not reuse automatic-fit inference. The current
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
uses Student-t factors for coefficient intervals. For nonlinear, bounded,
weakly identified, or multimodal problems, a symmetric local curvature can be
misleading. Neither symmetric/asymmetric confidence intervals nor parameter
p-values are added by this PR.

Numerical limitations include finite-difference cancellation, parameter scaling
(the additive 1 in the step rule is unit dependent), local minima, incomplete
refits, and strongly nonquadratic profiles. The accepted-D guard is not a
certificate of optimizer convergence. Very small/large uncertainty scales can
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
