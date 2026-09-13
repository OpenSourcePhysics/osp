package org.opensourcephysics.tools;

import org.opensourcephysics.display.Dataset;
import org.opensourcephysics.display.OSPRuntime;

/** Tab-separated snapshots of curve fits for spreadsheets and reports. */
final class CurveFitReport {
    private CurveFitReport() {}
    static final String[] STATISTIC_KEYS = {"Points", "Free", "DF", "SSE", "RMS", "R2", "SE"};

    static String create(KnownFunction fit, Dataset data, boolean[] fixed,
            double[] uncertainties, boolean autofit, boolean includeStatistics) {
        return create(fit,data,fixed,uncertainties,autofit,includeStatistics,null,null,FitUncertainty.estimated(),Double.NaN);
    }
    static String create(KnownFunction fit, Dataset data, boolean[] fixed,
            double[] uncertainties, boolean autofit, boolean includeStatistics,
            String xUnits, String yUnits, FitUncertainty uncertainty, double unitsPerPixel) {
        StringBuilder out=new StringBuilder();
        if(includeStatistics) {
            String xName=data==null?"x":data.getXColumnName(), yName=data==null?"y":data.getYColumnName();
            row(out,label("Fit"),data==null?"":data.getName());
            row(out,label("Model"),fit.getName(),label(autofit?"Auto":"Manual"));
            row(out,label("XVariable"),org.opensourcephysics.display.ExportText.header(xName,xUnits));
            row(out,label("YVariable"),org.opensourcephysics.display.ExportText.header(yName,yUnits));
            row(out,label("Equation"),org.opensourcephysics.display.ExportText.ascii(yName+" = "+fit.getExpression(xName)));
            double[] stats=statistics(fit,data,fixed,autofit);
            double n=stats[0],p=stats[1],df=stats[2],sse=stats[3],r2=stats[5],sst=stats[7];
            row(out,label("Observations"),integer(n));
            row(out,label("Free"),integer(p));
            row(out,label("Rank"),stats[8]<0?label("NA"):integer(stats[8]));
            row(out,label("DF"),integer(df));
            if(autofit && stats[8]>=0 && stats[8]<p)row(out,label("Identifiability"));
            row(out);row(out,label("Residuals"));
            row(out,label("SSE"),number(sse));
            row(out,label("RMSResidual"),number(stats[4]));
            row(out,label("ResidualStandardError"),number(stats[6]));
            row(out);row(out,label("Goodness"));
            row(out,label("RSquare"),number(r2));
            row(out,label("AdjustedRSquare"),number(autofit && df>0 && n>1 && sst>0?1-(sse/df)/(sst/(n-1)):Double.NaN));
            row(out,label("MultipleR"),number(autofit && fit instanceof KnownPolynomial && p==fit.getParameterCount()
                && stats[8]==p && r2>=0?Math.sqrt(r2):Double.NaN));
            double[] chi=uncertainty.statistics(stats,autofit,unitsPerPixel);
            row(out,label("ChiSquare"),number(chi[1]));
            row(out,label("ReducedChiSquare"),number(chi[2]),uncertainty.mode==FitUncertainty.ESTIMATED && isFinite(chi[2])?label("ByConstruction"):"");
            row(out,label("ChiProbability"),number(chi[3]));
            row(out,label("R2Description"));
            row(out);row(out,label("UncertaintyModel"),label(uncertainty.mode==FitUncertainty.ESTIMATED?"Estimated":"Specified"));
            row(out,label(uncertainty.mode==FitUncertainty.ESTIMATED?"EstimatedSigma":"SpecifiedSigma"),number(chi[0]),org.opensourcephysics.display.ExportText.ascii(yUnits));
            if(uncertainty.mode==FitUncertainty.PIXELS)row(out,label("SigmaPixels"),number(uncertainty.value),"pixels");
            if(uncertainty.mode==FitUncertainty.ESTIMATED)row(out,label("EstimatedCaution"));
            else row(out,label("QCaution"));
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
        if(includeStatistics)row(out,label("Parameters"));
        row(out,label("Parameter"),label("Coefficients"),label("StandardError"),label("Units"),label("Fixed"));
        for(int i=0;i<fit.getParameterCount();i++) {
            boolean isFixed=fixed!=null && i<fixed.length && fixed[i];
            double sigma=autofit && !isFixed && uncertainties!=null && i<uncertainties.length?uncertainties[i]:Double.NaN;
            if(sigma<0)sigma=Double.NaN;
            row(out,fit.getParameterName(i),number(fit.getParameterValue(i)),number(sigma),
                coefficientUnits(fit,i,xUnits,yUnits),label(isFixed?"Yes":"No"));
        }
        return out.toString();
    }
    /** Compact classroom report; numeric cells retain the full stored precision. */
    static String createSummary(KnownFunction fit, Dataset data, boolean[] fixed,
            double[] uncertainties, boolean autofit, String xUnits, String yUnits,
            FitUncertainty uncertainty, double unitsPerPixel) {
        StringBuilder out = new StringBuilder();
        String x = data == null ? "x" : data.getXColumnName();
        String y = data == null ? "y" : data.getYColumnName();
        row(out, label("Title"), data == null ? "" : data.getName());
        row(out, label("Model"), fit.getName(), label(autofit ? "Auto" : "Manual"));
        row(out, label("Equation"), org.opensourcephysics.display.ExportText.ascii(y + " = " + fit.getExpression(x)));
        row(out, label("XVariable"), org.opensourcephysics.display.ExportText.header(x, xUnits));
        row(out, label("YVariable"), org.opensourcephysics.display.ExportText.header(y, yUnits));
        row(out);
        // Reuse the coefficient-only export to preserve units, fixed/manual behavior,
        // and uncertainty values in both report formats.
        out.append(create(fit, data, fixed, uncertainties, autofit, false,
                xUnits, yUnits, uncertainty, unitsPerPixel));
        row(out);
        double[] stats = statistics(fit, data, fixed, autofit);
        for (int i = 0; i < STATISTIC_KEYS.length; i++) {
            String key = STATISTIC_KEYS[i];
            String title = i == 4 ? label("RMSResidual")
                    : ToolsRes.getString("DatasetCurveFitter.Statistics." + key);
            row(out, title, i < 3 ? integer(stats[i]) : number(stats[i]));
        }
        if (autofit && stats[8] >= 0 && stats[8] < stats[1]) row(out, label("Identifiability"));
        if (uncertainty.mode != FitUncertainty.ESTIMATED) {
            row(out, label("SpecifiedSigma"), number(uncertainty.statistics(stats, autofit, unitsPerPixel)[0]),
                    org.opensourcephysics.display.ExportText.ascii(yUnits));
            if (uncertainty.mode == FitUncertainty.PIXELS)
                row(out, label("SigmaPixels"), number(uncertainty.value), "pixels");
        }
        return out.toString();
    }

    /** Exact transformations of polynomial coefficients; no new fit or covariance approximation. */
    static String[][] motionResults(KnownFunction fit, boolean[] fixed, double[] uncertainties,
            boolean autofit, String xUnits, String yUnits, String component) {
        if (component == null || !(fit instanceof KnownPolynomial)
                || fit.getParameterCount() < 2 || fit.getParameterCount() > 3) return new String[0][];
        int degree = fit.getParameterCount() - 1;
        String[][] results = new String[degree][];
        for (int i = 0; i < degree; i++) {
            int parameter = degree == 1 ? 0 : 1 - i;
            double factor = i == 0 ? 1 : 2;
            boolean isFixed = fixed != null && parameter < fixed.length && fixed[parameter];
            double sigma = autofit && !isFixed && uncertainties != null && parameter < uncertainties.length
                    ? factor * uncertainties[parameter] : Double.NaN;
            if (sigma < 0) sigma = Double.NaN;
            results[i] = new String[]{component + " " + label(i == 1 ? "Acceleration"
                    : degree == 1 ? "Velocity" : "VelocityAtZero"),
                    number(factor * fit.getParameterValue(parameter)), number(sigma),
                    coefficientUnits(fit, parameter, xUnits, yUnits), label(isFixed ? "Yes" : "No")};
        }
        return results;
    }

    static String appendMotionResults(String report, String[][] results) {
        if (results.length == 0) return report;
        StringBuilder out = new StringBuilder(report);
        row(out);
        row(out, label("MotionResults"));
        row(out, label("Quantity"), label("Value"), label("StandardError"), label("Units"), label("Fixed"));
        for (String[] result : results) row(out, result);
        return out.toString();
    }

    static String coefficientUnits(KnownFunction fit,int parameter,String xUnits,String yUnits) {
        if(!(fit instanceof KnownPolynomial))return "";
        String x=org.opensourcephysics.display.ExportText.ascii(xUnits).trim();
        String y=org.opensourcephysics.display.ExportText.ascii(yUnits).trim();
        int power=fit.getParameterCount()-1-parameter;
        if(y.length()==0)return "";
        if(power==0)return y;
        if(x.length()==0)return "";
        String denominator=x.matches("[A-Za-z0-9_]+")?x:"("+x+")";
        return y+"/"+denominator+(power==1?"":"^"+power);
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
    static int parameterRank(KnownFunction fit, boolean[] fixed, double[] x) {
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
