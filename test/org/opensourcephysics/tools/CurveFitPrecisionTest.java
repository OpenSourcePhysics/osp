package org.opensourcephysics.tools;

import java.util.Locale;
import org.opensourcephysics.display.OSPRuntime;

/** Standalone regression checks for reporting precision and retained values. */
public class CurveFitPrecisionTest {
    private static int passed;

    public static void main(String[] args) {
        Locale original = Locale.getDefault();
        char separator = OSPRuntime.getCurrentDecimalSeparator();
        try {
            OSPRuntime.setPreferredDecimalSeparator(".");
            display(1.23456789, 0.056789, "1.235 ± 0.057");
            display(1.23456789, 0.05001, "1.235 ± 0.050");
            display(-1.23456789, 0.056789, "-1.235 ± 0.057");
            display(0, 0.056789, "0.000 ± 0.057");
            display(1.23456789, 0.0996, "1.23 ± 0.10");
            display(1.23456789, 0.996, "1.2 ± 1.0");
            display(1.23456789, 9.96, "(0.1 ± 1.0) E1");
            display(12345.6789, 567.89, "(1.235 ± 0.057) E4");
            display(0.00123456789, 0.000056789, "(1.235 ± 0.057) E-3");
            display(0.12, 5.6789, "0.1 ± 5.7");
            double value = 1.2345678901234567;
            double sigma = 0.0567890123456789;
            String[] text = DatasetCurveFitter.formatParameterWithUncertainty(value, sigma, 1);
            String[] tooltip = text[1].split(" ± ");
            check(Double.doubleToLongBits(Double.parseDouble(tooltip[0])) == Double.doubleToLongBits(value), "tooltip preserves parameter double");
            check(Double.doubleToLongBits(Double.parseDouble(tooltip[1])) == Double.doubleToLongBits(sigma), "tooltip preserves uncertainty double");
            DatasetCurveFitter.NumberField field = new DatasetCurveFitter.NumberField(12);
            field.applyPattern("0.###");
            field.setValue(value);
            check(Double.doubleToLongBits(field.getValue()) == Double.doubleToLongBits(value), "unchanged rounded editor retains original double");
            for (double unavailable : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
                check(DatasetCurveFitter.formatParameterWithUncertainty(value, unavailable, 1) == null, "unavailable uncertainty " + unavailable);
            }
            Locale.setDefault(Locale.GERMANY);
            display(1.23456789, 0.056789, "1.235 ± 0.057");
            OSPRuntime.setPreferredDecimalSeparator(",");
            display(1.23456789, 0.056789, "1,235 ± 0,057");
            System.out.println("Passed: " + passed);
        } finally {
            Locale.setDefault(original);
            OSPRuntime.setPreferredDecimalSeparator(String.valueOf(separator));
        }
    }

    private static void display(double value, double sigma, String expected) {
        String actual = DatasetCurveFitter.formatParameterWithUncertainty(value, sigma, 1)[0];
        check(expected.equals(actual), "expected " + expected + "; got " + actual);
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        passed++;
        System.out.println("PASS: " + message);
    }
}
