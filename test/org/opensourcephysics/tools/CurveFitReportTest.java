package org.opensourcephysics.tools;

import org.opensourcephysics.display.Dataset;
import org.opensourcephysics.display.OSPRuntime;

/** Analytic regression checks and tab-separated spreadsheet layout checks. */
public class CurveFitReportTest {
    static int passed;
    public static void main(String[] args) throws Exception {
        OSPRuntime.setPreferredDecimalSeparator(".");
        KnownPolynomial line = new KnownPolynomial(new double[] {1, 2});
        Dataset data = new Dataset();
        data.setXYColumnNames("time", "position");
        // Residuals are orthogonal to the intercept and x: this is the exact OLS line.
        data.append(new double[] {0, 1, 2, 3}, new double[] {1.1, 2.9, 4.9, 7.1});
        double[] stats = CurveFitReport.statistics(line, data, null, true);
        close(stats[0], 4, "point count");
        close(stats[1], 2, "free parameters");
        close(stats[2], 2, "residual degrees of freedom");
        close(stats[3], .04, "SSE");
        close(stats[4], .1, "RMS uses n");
        close(stats[5], 1 - .04/20.04, "R-squared uses model residuals");
        close(stats[6], Math.sqrt(.02), "standard error uses n-p");
        close(stats[7], 20.04, "total sum of squares");
        String report = CurveFitReport.create(line, data, null, new double[] {.0632455532033676, .1183215956619923}, true, true);
        String[] rows = report.split("\n");
        check(rows[0].startsWith("SUMMARY OUTPUT\tposition vs time\t"), "simple title and data-name fallback");
        check(rows[1].contains("position = ") && rows[1].contains("Automatic fit"), "second header contains model, equation, and mode");
        for (String row : rows) check(splitTabs(row).length == 5, "five aligned spreadsheet columns");
        check(report.indexOf("Regression Statistics") < report.indexOf("ANOVA")
                && report.indexOf("ANOVA") < report.indexOf("Parameter\tCoefficients"), "Excel-style section order");
        close(cell(report, "Multiple R", 1), Math.sqrt(stats[5]), "Multiple R");
        close(cell(report, "Adjusted R Square (centered)", 1), 1 - (.04/2)/(20.04/3), "Adjusted R Square");
        close(cell(report, "Regression", 1), 1, "regression df");
        close(cell(report, "Regression", 2), 20, "regression SS");
        close(cell(report, "Regression", 3), 20, "regression MS");
        close(cell(report, "Regression", 4), 1000, "F statistic");
        close(cell(report, "Residual", 1), 2, "residual df");
        close(cell(report, "Residual", 2), .04, "residual SS");
        close(cell(report, "Residual", 3), .02, "residual MS");
        close(cell(report, "Total", 1), 3, "total df");
        check(report.split("Parameter\\tCoefficients", -1).length == 2, "coefficient header occurs once");
        check(!report.contains("rounded") && !report.contains("±"), "no repeated rounded coefficients");
        check(!report.toLowerCase().contains("weight"), "no irrelevant weights note");
        check(rows[rows.length - 1].startsWith("B\t"), "report ends with coefficients, without a prose footer");
        report = CurveFitReport.create(line, data, new boolean[] {true, false}, new double[] {.05, .01}, true, true);
        close(cell(report, "Residual", 1), 3, "fixed parameter excluded from p");
        check(report.contains("A\t2.0\tN/A\tYes"), "fixed coefficient has no uncertainty");
        check(report.contains("Regression\tN/A\tN/A\tN/A\tN/A"), "no classical regression ANOVA for constrained fits");
        report = CurveFitReport.create(line, data, null, new double[] {.05, .01}, false, true);
        check(report.contains("Manual parameters") && report.contains("A\t2.0\tN/A\tNo"), "manual mode does not reuse uncertainties");
        check(report.contains("Standard Error\tN/A"), "manual mode has no fitted residual standard error");
        KnownPolynomial parabola = new KnownPolynomial(new double[] {0, 0, 1});
        Dataset curved = new Dataset();
        curved.append(new double[] {-2, -1, 0, 1, 2}, new double[] {4, 1, 0, 1, 4});
        close(CurveFitReport.statistics(parabola, curved, null, true)[5], 1, "nonlinear curve has R-squared 1 despite zero linear correlation");
        UserFunction nonlinear = new UserFunction("Exponential");
        nonlinear.setParameters(new String[] {"A"}, new double[] {1}, null);
        nonlinear.setExpression("exp(A*x)", new String[] {"x"});
        report = CurveFitReport.create(nonlinear, data, null, null, true, true);
        check(report.contains("Regression\tN/A\tN/A\tN/A\tN/A"), "no classical regression ANOVA for nonlinear parameter models");
        Dataset constant = new Dataset();
        constant.append(new double[] {0, 1, 2}, new double[] {3, 3, 3});
        check(Double.isNaN(CurveFitReport.statistics(line, constant, null, true)[5]), "constant response has undefined R-squared");
        Dataset shortData = new Dataset(); shortData.append(0, 1);
        check(Double.isNaN(CurveFitReport.statistics(line, shortData, null, true)[6]), "insufficient degrees of freedom");
        check(Double.isNaN(CurveFitReport.statistics(line, null, null, true)[3]), "empty dataset statistics unavailable");
        KnownPolynomial poor = new KnownPolynomial(new double[] {100,100});
        check(CurveFitReport.statistics(poor, data, null, false)[5] < 0, "negative R-squared retained");
        UserFunction invalid = new UserFunction("Invalid"); invalid.setExpression("sqrt(-1)", new String[] {"x"});
        check(Double.isNaN(CurveFitReport.statistics(invalid, data, null, false)[3]), "undefined model statistics unavailable");
        UserFunction redundant = new UserFunction("Redundant");
        redundant.setParameters(new String[] {"A", "B"}, new double[] {1, 1}, null);
        redundant.setExpression("(A+B)*x", new String[] {"x"});
        Dataset redundantData = new Dataset();
        redundantData.append(new double[] {0,1,2,3}, new double[] {.1,1.9,3.9,6.1});
        stats = CurveFitReport.statistics(redundant, redundantData, null, true);
        close(stats[1], 2, "nominal free parameter count retained");
        close(stats[8], 1, "redundant parameters have rank one");
        close(stats[2], 3, "rank determines residual degrees of freedom");
        close(stats[6], Math.sqrt(.04/3), "rank-correct residual standard error");
        close(redundant.getParameterValue(0), 1, "rank calculation preserves original parameters");
        report = CurveFitReport.create(redundant, redundantData, null, null, true, true);
        close(cell(report, "Residual", 1), 3, "export uses rank-correct df");
        close(cell(report, "Adjusted R Square (centered)", 1), stats[5], "adjusted R square uses effective rank");
        check(report.contains("Multiple R\tN/A"), "Multiple R withheld outside classical polynomial fits");
        UserFunction origin = new UserFunction("Origin");
        origin.setParameters(new String[] {"A"}, new double[] {17.0/7}, null);
        origin.setExpression("A*x", new String[] {"x"});
        report = CurveFitReport.create(origin, data, null, null, true, true);
        close(cell(report, "R Square (centered)", 1), .9267179925862561, "through-origin R square explicitly centered");
        stats = CurveFitReport.statistics(line, data, new boolean[] {true,true}, true);
        close(stats[2], 4, "all-fixed model uses n residual degrees of freedom");
        Dataset repeated = new Dataset(); repeated.append(new double[] {1,1,1,1}, new double[] {1,2,3,4});
        close(CurveFitReport.statistics(line, repeated, null, true)[2], 3, "repeated x lowers polynomial rank");
        UserFunction scaled = new UserFunction("Scaled");
        scaled.setParameters(new String[] {"A","B"}, new double[] {1,1}, null);
        scaled.setExpression("A*x+1e-9*B", new String[] {"x"});
        close(CurveFitReport.statistics(scaled, data, null, true)[8], 2, "rank tolerates different parameter units");
        KnownPolynomial large = new KnownPolynomial(new double[] {1e20, 1});
        close(CurveFitReport.statistics(large, data, null, true)[8], 2, "large coefficient does not hide polynomial rank");
        double value = 1.2345678901234567, sigma = .0567890123456789;
        line.setParameterValue(0, value);
        String params = CurveFitReport.create(line, data, null, new double[] {sigma,.01}, true, false);
        String[] first = splitTabs(params.split("\n")[1]);
        check(Double.doubleToLongBits(Double.parseDouble(first[1])) == Double.doubleToLongBits(value), "coefficient round-trips exactly");
        check(Double.doubleToLongBits(Double.parseDouble(first[2])) == Double.doubleToLongBits(sigma), "uncertainty round-trips exactly");
        check(params.split("\n").length == 3, "parameters-only export has one header and two rows");
        check(first[3].equals("No") && first[4].isEmpty(), "coefficient and uncertainty each exported once");
        check(line.getParameterValue(0) == value, "copying leaves model unchanged");
        OSPRuntime.setPreferredDecimalSeparator(",");
        params = CurveFitReport.create(line, data, null, new double[] {sigma,.01}, true, false);
        check(params.contains("1,2345678901234567\t0,0567890123456789"), "locale decimal separator with tab delimiter");
        OSPRuntime.setPreferredDecimalSeparator(".");
        if (args.length > 0) java.nio.file.Files.write(java.nio.file.Paths.get(args[0]),
                CurveFitReport.create(new KnownPolynomial(new double[] {1,2}), data, null,
                        new double[] {.0632455532033676,.1183215956619923}, true, true).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        System.out.println("Passed: " + passed);
    }
    static double cell(String report, String name, int column) {
        for (String row : report.split("\n")) if (row.startsWith(name + "\t")) return Double.parseDouble(splitTabs(row)[column]);
        throw new AssertionError("Missing row: " + name);
    }
    static void close(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1e-10 * Math.max(1,Math.abs(expected)), message + ": " + actual);
    }
    // Preserve trailing empty TSV cells even in SwingJS, whose String.split
    // implementation does not support Java's negative-limit behavior.
    static String[] splitTabs(String row) {
        String[] cells = (row + " ").split("\t");
        int last = cells.length - 1;
        cells[last] = cells[last].substring(0, cells[last].length() - 1);
        return cells;
    }
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        passed++; System.out.println("PASS: " + message);
    }
}
