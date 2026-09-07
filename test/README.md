# Curve-fit regression tests

## Fit uncertainty documentation

The fit-uncertainty documentation is arranged in three levels so readers can stop when they have the detail they need:

1. **Using the result in a lab:** [Understanding fit uncertainties](fit-uncertainty-guide.md) explains what the fitted value and `+/-` uncertainty mean, how to choose the uncertainty model, and how to interpret R-squared and reduced chi-square.
2. **Understanding the statistics:** [The statistics behind the result](fit-uncertainty-details.md) develops residual variance, degrees of freedom, profile curvature, refitting correlated coefficients, rank, and the worked `2.000 +/- 0.063 m/s` example.
3. **Reviewing the implementation:** [Bevington-style fit uncertainties: implementation note](bevington-fit-uncertainties.md) documents the equations as implemented, numerical step rules, code path, edge cases, rank calculation, and regression tests.

The short guide begins from the principle: **A number from a fit is not the answer; it is a measurement with assumptions attached.**

These standalone Java tests require a built OSP or Tracker JAR and a JDK.
They use main methods and exit unsuccessfully on assertion failures; no test
framework is required. Run from the repository root, replacing `/path/to/osp.jar`
with the built JAR:

```sh
mkdir -p /tmp/osp-fit-tests
javac -cp /path/to/osp.jar -d /tmp/osp-fit-tests test/org/opensourcephysics/tools/*.java
for test in CurveFitPrecisionTest CurveFitReportTest CurveFitPopupTest CurveFitDataToolLayoutTest CurveFitConstraintTest CurveFitPhysicsTest; do
  java -cp /tmp/osp-fit-tests:/path/to/osp.jar org.opensourcephysics.tools.$test || exit 1
done
```

The Swing tests require a graphical desktop. They create their own windows and
synthetic data. The full Data Tool test covers resizing, font sizes, parameter
visibility, compact statistics, and stable plot height across point selections.
The precision and report tests cover display rounding, full-precision values,
spreadsheet columns, and unavailable statistics. The popup test checks that
context-menu gestures preserve Autofit while left-click editing remains available.

The constraint test checks immediate refitting and report updates after fixed
checkbox edits, manual-mode preservation, one-point constrained fits, and unknown
uncertainties for non-identifiable models (including perfect fits).

Statistical conventions: R Square and Adjusted R Square use centered total
sums of squares, including fits through zero (Excel uses an uncentered total
for those fits). Residual degrees of freedom use the numerical rank of the
free-parameter Jacobian; the displayed free count still counts editable parameters.
Rank uses normalized columns and a tolerance of 1e-7; for nonlinear models this
is a local linear approximation, not proof of global identifiability.
Multiple R and classical regression ANOVA are restricted to full-rank,
unconstrained automatic polynomial fits. Parameter standard errors retain
Tracker's profile-curvature method, which may differ from Jacobian covariance
estimates for nonlinear models; they are not 95% confidence intervals.

The physics test covers raw/formatted exports with tab/comma delimiters, absent,
complete and mixed unit metadata, paste round trips, polynomial parameter units,
and the absence of guessed UserFunction units. It also checks residual-estimated
and supplied common uncertainties, chi-square probabilities, unchanged
coefficients, profile-error scaling, numerical rank, fixed parameters, perfect
fits, fractional pixel inputs, and a live host metadata provider.

See [fit-report-physics.md](fit-report-physics.md) for statistical definitions,
metadata integration, and the limits of pixel conversion.
