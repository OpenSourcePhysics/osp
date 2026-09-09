# Fit report units and uncertainty models

## Documentation index

| Document | Contents |
|---|---|
| [Using fit results](fit-uncertainty-guide.md) | Short lab guide to fitted values, uncertainties, and reporting. |
| [Understanding the statistics](fit-uncertainty-details.md) | Statistical reasoning and a worked example. |
| [Bevington implementation](bevington-fit-uncertainties.md) | Profile-curvature method, code, and numerical limitations. |
| [Report definitions and integration](fit-report-physics.md) **(this page)** | Units, uncertainty models, and Tracker integration. |
| [Tests and documentation](README.md) | Regression test instructions and coverage. |

The coefficient optimizer is unchanged. A positive constant weight multiplies
the least-squares objective by a constant and therefore has the same minimizer;
changing the uncertainty controls recomputes profile errors on a cloned function
without changing the displayed coefficients. The default remains equal-weight
least squares. Manual mode does not acquire automatic-fit inference.

## Definitions

For selected valid observations, residuals are y_i - f(x_i), SSE is their squared
sum, and RMS residual is sqrt(SSE/n). With r the effective independent free
parameter rank, df = n-r and residual standard error is sqrt(SSE/df).

The default uncertainty model uses that residual standard error as sigma_y.
For positive sigma_y and df, chi-square = df and reduced chi-square = 1 by
construction. Q is unavailable because sigma_y came from these same residuals.
For an exact perfect fit with estimated sigma_y = 0, chi-square and reduced
chi-square are undefined and shown as N/A rather than evaluating 0/0.

With a supplied finite positive sigma_y, chi-square = SSE/sigma_y^2 and reduced
chi-square = chi-square/df. Q is the chi-square survival probability, the
regularized upper incomplete gamma Q(df/2, chi-square/2). The implementation
uses a convergent lower series or upper continued fraction with a Lanczos
log-gamma evaluation. Tests include published chi-square quantiles and analytic
cases. See [NIST DLMF 8.2](https://dlmf.nist.gov/8.2) and the
[NIST chi-square distribution](https://www.itl.nist.gov/div898/handbook/eda/section3/eda3666.htm).
Q assumes independent Gaussian measurement errors and a suitable model; for
nonlinear fits the rank-based interpretation is local. Invalid supplied input
never silently becomes an estimated uncertainty. Supplied sigma_y is never
rescaled to force reduced chi-square to one.

Parameter errors retain the Bevington Eq. 8.13 profile/refit curvature method.
Only its uncertainty scale changes. This is not a replacement by Jacobian
covariance. Unidentifiable or fixed parameters retain unavailable errors. See the
[short explanation with examples](fit-uncertainty-guide.md) and the
[detailed implementation note](bevington-fit-uncertainties.md).

Centered R Square = 1-SSE/SST, where SST uses deviations from the observed mean.
Adjusted R Square = 1-(SSE/df)/(SST/(n-1)), where defined. R Square is descriptive
variance explained; it is not proof of model adequacy. The report retains the
warning that Excel uses another convention for fits through zero. Valid classical
polynomial ANOVA remains supplemental to the physics sections.

## Units and clipboard

DataTable.getUnits exposes existing renderer unit metadata. DataTool may also
receive a live FitMetadataProvider from its host. Units appear only in headers;
raw/formatted numeric serialization and delimiter selection are unchanged.
ExportText converts supported mathematical display notation to spreadsheet text,
such as m/s^2 and A*t^2. Header construction avoids adding an already present
identical unit suffix. Paste retains literal headers; it does not infer metadata
by parsing units from column names.

KnownPolynomial exposes parameters in descending degree order, so parameter i
has degree parameterCount-1-i. Its unit is y-unit / x-unit^degree, with the
constant's unit equal to y-unit. Missing required units leave the field blank.
UserFunction parameter units remain blank: no symbolic dimensional inference.

## Tracker integration

OSP has no Tracker dependency. FitMetadataProvider supplies units and an optional
physical-y-units-per-pixel conversion. A small companion Tracker adapter resolves
source dataset IDs plus source column IDs, uses existing unit/calibration methods,
and limits pixel mode to directly calibrated PointMass x/y data. Derived
velocities, accelerations, and data functions do not receive pixel conversion.

For isotropic independent pixel-coordinate error, physical sigma_y is pixel
sigma times hypot(a,b), where (a,b) is the relevant row of the image-to-world
linear transform. With isotropic calibration this is pixel sigma / pixels-per-unit.
A common conversion requires fixed scale, and either fixed angle or isotropic
scale. Moving origins do not affect uncertainty. Unsupported varying calibration
returns unavailable; it is not guessed. Presets are 0.5, 1, 1.5, 2, 2.5 and 3
pixels; the editable field accepts a custom positive value.

Metadata and uncertainty controls are session-only, with no project format change.
Dataset drawing error bars exist, but Data Tool has no selection-aligned fitting
uncertainty-column association. This change adds no per-point data-model redesign;
a future column model must explicitly map uncertainties to the selected rows.

## Verification

The desktop suite contains 592 assertions across precision (19), report (118),
popup (17), Data Tool layout (234), constraints (40), and physics (164).
The companion Tracker calibration fixture adds 17 checks. The computational
precision/report/constraint/physics tests also run through SwingJS in Chrome,
Firefox and WebKit (341 checks per browser). Browser interaction checks exercise
selection, resizing, fixed parameters, supplied sigma, and copying the report.
These local runs cover macOS and browser engines; they do not constitute new
native Windows/Linux runtime testing.

### Compact and full copied reports

Copy Fit Report exports the parameter table and the statistics shown on screen, with model, equation, variable units, and full-precision numeric cells. It omits ANOVA and advanced goodness-of-fit analysis. The adjacent `...` menu offers **Copy Full Fit Report**, also available in the parameter context menu. Both formats preserve manual/fixed parameter semantics and uncertainty units. Specified measurement uncertainty and identifiability warnings remain visible in the compact report when applicable.

The uncertainty controls label the default **Unweighted - estimate from residuals**. Known uncertainty entry uses a wider field on a separate row. From the default mode, pixels start at 1; physical units start at one calibrated pixel, or the residual standard error if calibration is unavailable. A zero/undefined starting scatter leaves the entry blank. Switching between supplied physical and pixel modes converts the current value. Pixel presets are offered only in pixel mode. These starting values remain editable measurement-model choices, not claims about instrument accuracy.

The data-uncertainty entry displays two significant digits (for example, `0.0025 m`). Its full stored precision remains in the tooltip and exported reports. Formatting and accepting unchanged display text do not round the uncertainty used in calculations.

### Motion results from position fits

An optional `FitMetadataProvider.getPositionComponent(xColumn, yColumn)` callback identifies position against time using host column identities. Its default is null, preserving existing hosts. For known line fits, velocity is A and its standard error is sigma_A. For known quadratics, velocity at t=0 is B with sigma_B, and constant acceleration is 2A with standard error 2*sigma_A. These are exact coefficient transformations; no new optimizer or covariance method is used. The units come from the corresponding polynomial coefficient units. Manual and fixed-parameter errors remain N/A. Arbitrary UserFunctions, other polynomial degrees, and unidentified column pairs do not receive motion labels.

Derived motion results appear at the end of both copied report formats with full-precision numeric export. The window shows the original parameter table without duplicate velocity and acceleration rows. The reference time is the data's t=0, not necessarily the first selected observation. This feature does not compute velocity uncertainty at other times or propagate pixel errors into numerical derivative columns.

At small window sizes the fit panel scrolls vertically, preserving a usable drawable graph above it. The reserved scrollbar width avoids changing the parameter layout when scrolling becomes necessary. Layout tests now measure the drawable area after axis gutters and verify that scrolling reaches the uncertainty controls.

The on-screen residual summary uses one row: R-squared, SSE, and Residual SE. Points, free parameters, and degrees of freedom remain in copied reports but no longer consume display space. RMS remains beside Autofit.
