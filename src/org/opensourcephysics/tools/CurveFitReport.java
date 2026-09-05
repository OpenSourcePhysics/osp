package org.opensourcephysics.tools;

import org.opensourcephysics.display.Dataset;
import org.opensourcephysics.display.OSPRuntime;

/** Tab-separated snapshots of curve fits for spreadsheets and reports. */
final class CurveFitReport {
    private CurveFitReport() {}
    static final String[] STATISTIC_KEYS = {"Points", "Free", "DF", "SSE", "RMS", "R2", "SE"};

    static String create(KnownFunction fit, Dataset data, boolean[] fixed,
            double[] uncertainties, boolean autofit, boolean includeStatistics) {
        StringBuilder out = new StringBuilder();
        if (includeStatistics) {
            String xName = data == null ? "x" : data.getXColumnName();
            String yName = data == null ? "y" : data.getYColumnName();
            String dataName = data == null ? "" : data.getName();
            if (dataName == null || dataName.trim().isEmpty()) dataName = yName + " vs " + xName;
            row(out, label("Summary"), dataName);
            row(out, label("Model"), fit.getName(), label("Equation"),
                    yName + " = " + fit.getExpression(xName), label(autofit ? "Auto" : "Manual"));
            row(out);
            double[] stats = statistics(fit, data, fixed, autofit);
            double n = stats[0], p = stats[1], df = stats[2], sse = stats[3], r2 = stats[5], sst = stats[7];
            row(out, label("RegressionStatistics"));
            row(out, label("MultipleR"), number(autofit && fit instanceof KnownPolynomial
                    && p == fit.getParameterCount() && stats[8] == p && r2 >= 0
                    ? Math.sqrt(r2) : Double.NaN));
            row(out, label("RSquare"), number(r2));
            row(out, label("AdjustedRSquare"), number(autofit && df > 0 && n > 1 && sst > 0
                    ? 1 - (sse / df) / (sst / (n - 1)) : Double.NaN));
            row(out, label("StandardError"), number(stats[6]));
            row(out, label("Observations"), Integer.toString((int) n));
            row(out);
            row(out, label("ANOVA"));
            row(out, "", "df", "SS", "MS", "F");
            // Classical regression ANOVA applies to an automatically fitted,
            // full-rank polynomial with a freely fitted intercept and no constraints.
            boolean regression = autofit && fit instanceof KnownPolynomial && p == fit.getParameterCount()
                    && stats[8] == p && df > 0 && p > 1 && sst > 0 && sse >= 0 && sse <= sst
                    && hasDistinctAbscissas(data, (int) p);
            double regressionDF = regression ? p - 1 : Double.NaN;
            double regressionSS = regression ? sst - sse : Double.NaN;
            double regressionMS = regressionSS / regressionDF;
            double residualMS = df > 0 ? sse / df : Double.NaN;
            row(out, label("Regression"), integer(regressionDF), number(regressionSS), number(regressionMS),
                    number(regression && residualMS > 0 ? regressionMS / residualMS : Double.NaN));
            row(out, label("Residual"), integer(df), number(sse), number(residualMS));
            row(out, label("Total"), n > 0 ? Integer.toString((int) n - 1) : label("NA"), number(sst));
            row(out);
        }
        // Exactly one numeric coefficient and uncertainty per parameter. No
        // duplicate rounded column: rounding belongs to the on-screen display.
        row(out, label("Parameter"), label("Coefficients"), label("StandardError"), label("Fixed"));
        for (int i = 0; i < fit.getParameterCount(); i++) {
            boolean isFixed = fixed != null && i < fixed.length && fixed[i];
            double sigma = autofit && !isFixed && uncertainties != null && i < uncertainties.length
                    ? uncertainties[i] : Double.NaN;
            if (sigma < 0) sigma = Double.NaN;
            row(out, fit.getParameterName(i), number(fit.getParameterValue(i)), number(sigma), label(isFixed ? "Yes" : "No"));
        }
        return out.toString();
    }

    private static boolean hasDistinctAbscissas(Dataset data, int required) {
        java.util.HashSet<Double> distinct = new java.util.HashSet<Double>();
        for (double x : data.getValidXPoints()) {
            if (!isFinite(x)) return false;
            distinct.add(x == 0 ? 0.0 : x);
            if (distinct.size() >= required) return true;
        }
        return false;
    }

    // SwingJS does not expose Java 8's Double.isFinite overload.
    static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private static String integer(double value) {
        return isFinite(value) ? Integer.toString((int) value) : label("NA");
    }

    /** n, p, df, SSE, RMS, R-squared, residual standard error, SST, local rank; shared by screen and export. */
    static double[] statistics(KnownFunction fit, Dataset data, boolean[] fixed, boolean autofit) {
        int freeParameters = 0;
        for (int i = 0; i < fit.getParameterCount(); i++)
            if (fixed == null || i >= fixed.length || !fixed[i]) freeParameters++;
        double[] x = data == null ? new double[0] : data.getValidXPoints();
        double[] y = data == null ? new double[0] : data.getValidYPoints();
        int n = x.length;
        double sse = 0;
        double mean = 0;
        double sst = 0;
        for (int i = 0; i < n; i++) {
            double predicted = fit.evaluate(x[i]);
            double residual = y[i] - predicted;
            sse += residual * residual;
            double delta = y[i] - mean;
            mean += delta / (i + 1);
            sst += delta * (y[i] - mean);
            if (!isFinite(x[i]) || !isFinite(y[i]) || !isFinite(predicted)
                    || (fit instanceof UserFunction && ((UserFunction) fit).evaluatedToNaN()))
                sse = Double.NaN;
        }
        int rank = autofit && n > 0 ? parameterRank(fit, fixed, x) : -1;
        int df = rank >= 0 ? n - rank : -1;
        boolean valid = n > 0 && isFinite(sse);
        return new double[] {n, freeParameters, autofit && n > 0 && rank >= 0 ? df : Double.NaN,
                valid ? sse : Double.NaN, valid ? Math.sqrt(sse / n) : Double.NaN,
                valid && isFinite(sst) && sst > 0 ? 1 - sse / sst : Double.NaN,
                valid && autofit && df > 0 ? Math.sqrt(sse / df) : Double.NaN,
                n > 0 && isFinite(sst) ? sst : Double.NaN, rank};
    }

    /** Numerical rank of the free-parameter Jacobian, evaluated on a clone.
     * Column normalization makes the tolerance independent of parameter units.
     * For nonlinear models this is a local, linearized degrees-of-freedom estimate.
     */
    private static int parameterRank(KnownFunction fit, boolean[] fixed, double[] x) {
        KnownFunction probe = fit.clone();
        double[][] columns = new double[fit.getParameterCount()][x.length];
        int count = 0;
        for (int j = 0; j < fit.getParameterCount(); j++) {
            if (fixed != null && j < fixed.length && fixed[j]) continue;
            double value = fit.getParameterValue(j);
            double step = 1e-5 * Math.max(1, Math.abs(value));
            double norm = 0;
            for (int i = 0; i < x.length; i++) {
                if (!isFinite(x[i])) return -1;
                double derivative;
                if (fit instanceof KnownPolynomial) {
                    // Polynomial parameters run from highest power to constant.
                    // Exact derivatives avoid cancellation from large coefficients.
                    derivative = Math.pow(x[i], fit.getParameterCount() - 1 - j);
                } else {
                    probe.setParameterValue(j, value + step);
                    double high = probe.evaluate(x[i]);
                    if (probe instanceof UserFunction && ((UserFunction) probe).evaluatedToNaN()) return -1;
                    probe.setParameterValue(j, value - step);
                    double low = probe.evaluate(x[i]);
                    if (probe instanceof UserFunction && ((UserFunction) probe).evaluatedToNaN()) return -1;
                    derivative = (high - low) / (2 * step);
                }
                if (!isFinite(derivative)) return -1;
                columns[count][i] = derivative;
                norm = Math.hypot(norm, derivative);
            }
            probe.setParameterValue(j, value);
            if (norm > 0) {
                for (int i = 0; i < x.length; i++) columns[count][i] /= norm;
                count++;
            }
        }
        // Pivoted, reorthogonalized Gram-Schmidt on normalized columns.
        int rank = 0;
        while (rank < count && rank < x.length) {
            int pivot = rank;
            double largest = 0;
            for (int j = rank; j < count; j++) {
                double norm = 0;
                for (double v : columns[j]) norm = Math.hypot(norm, v);
                if (norm > largest) { largest = norm; pivot = j; }
            }
            if (largest < 1e-7) break;
            double[] swap = columns[rank]; columns[rank] = columns[pivot]; columns[pivot] = swap;
            for (int i = 0; i < x.length; i++) columns[rank][i] /= largest;
            for (int j = rank + 1; j < count; j++) {
                for (int pass = 0; pass < 2; pass++) {
                    double dot = 0;
                    for (int i = 0; i < x.length; i++) dot += columns[rank][i] * columns[j][i];
                    for (int i = 0; i < x.length; i++) columns[j][i] -= dot * columns[rank][i];
                }
            }
            rank++;
        }
        return rank;
    }

    private static String label(String key) {
        return ToolsRes.getString("DatasetCurveFitter.Report." + key);
    }

    private static String number(double value) {
        return isFinite(value)
                ? Double.toString(value).replace('.', OSPRuntime.getCurrentDecimalSeparator()) : label("NA");
    }

    private static void row(StringBuilder out, String... cells) {
        for (int i = 0; i < 5; i++) {
            if (i > 0) out.append('\t');
            out.append(i >= cells.length || cells[i] == null ? "" : cells[i].replace('\t', ' ').replace('\n', ' ').replace('\r', ' '));
        }
        out.append('\n');
    }
}
