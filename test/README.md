# Curve-fit regression tests

These standalone Java tests require a built OSP or Tracker JAR and a JDK.
They use main methods and exit unsuccessfully on assertion failures; no test
framework is required. Run from the repository root, replacing `/path/to/osp.jar`
with the built JAR:

```sh
mkdir -p /tmp/osp-fit-tests
javac -cp /path/to/osp.jar -d /tmp/osp-fit-tests test/org/opensourcephysics/tools/*.java
for test in CurveFitPrecisionTest CurveFitReportTest CurveFitPopupTest CurveFitDataToolLayoutTest CurveFitConstraintTest; do
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
