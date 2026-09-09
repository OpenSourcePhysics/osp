package org.opensourcephysics.tools;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import javax.swing.*;
import org.opensourcephysics.display.Dataset;

/** Exercises constraint edits, identifiability, and constrained short datasets. */
public class CurveFitConstraintTest {
    static int passed;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                runTests();
                System.out.println("Passed: " + passed);
            } finally {
                for (Window window : Window.getWindows()) window.dispose();
            }
        });
    }

    static void runTests() {
        Dataset noisy = data(new double[] {0,1,2,3}, new double[] {1.1,2.9,4.9,7.1});
        DatasetCurveFitter fitter = line(noisy);
        close(fitter.getUncertainty(0), Math.sqrt(.02/5), "unconstrained slope uncertainty");
        close(fitter.getUncertainty(1), Math.sqrt(.014), "unconstrained intercept uncertainty");
        JTable table = table(fitter);
        toggle(table, 0);
        check(fitter.isAutoFit(), "fixing slope preserves Autofit");
        check(Double.isNaN(fitter.getUncertainty(0)), "fixed slope has no uncertainty");
        close(fitter.getUncertainty(1), Math.sqrt(.04/3/4), "free intercept uncertainty recomputed immediately");
        check(report(fitter).contains("Free parameters\t1"), "free count refreshed immediately");
        check(report(fitter).contains("Degrees of freedom (n - rank)\t3"), "degrees of freedom refreshed immediately");
        String report = report(fitter);
        check(report.contains("A\t2.0\tN/A\t\tYes"), "export identifies fixed slope");
        close(CurveFitReportTest.cell(report, "B", 2), Math.sqrt(.04/3/4), "export uses new intercept uncertainty");

        toggle(table, 1);
        check(report(fitter).contains("Free parameters\t0"), "all fixed updates free count");
        check(report(fitter).contains("Degrees of freedom (n - rank)\t4"), "all fixed updates degrees of freedom");
        check(Double.isNaN(fitter.getUncertainty(0)) && Double.isNaN(fitter.getUncertainty(1)), "all fixed clears uncertainties");
        toggle(table, 1);
        close(fitter.getUncertainty(1), Math.sqrt(.04/3/4), "unfixing intercept recomputes uncertainty");

        fitter.setData(data(new double[] {1}, new double[] {2.9}), true);
        close(((Number)table.getValueAt(0,2)).doubleValue(), 2, "fixed slope remains visible with one point");
        close(((Number)table.getValueAt(1,2)).doubleValue(), .9, "fitted intercept visible with one point");
        close(fitter.fit(fitter.fit), 0, "one free parameter and one point has finite RMS");
        check(Double.isNaN(fitter.getUncertainty(1)), "no residual degrees of freedom means unknown uncertainty");
        close(CurveFitReportTest.cell(report(fitter), "B", 1), .9, "screen and exported intercept agree");
        toggle(table, 0);
        check(Double.isNaN(((Number)table.getValueAt(0,2)).doubleValue()), "two free parameters still require two points");
        check(Double.isNaN(fitter.fit(fitter.fit)), "underdetermined fit retains unavailable RMS");

        fitter = line(noisy);
        fitter.setAutoFit(false);
        fitter.fit.setParameterValue(0, 5);
        fitter.fit(fitter.fit);
        toggle(table(fitter), 0);
        check(!fitter.isAutoFit(), "constraint edit preserves manual mode");
        close(fitter.fit.getParameterValue(0), 5, "manual constraint does not alter slope");
        close(fitter.fit.getParameterValue(1), 1, "manual constraint does not optimize intercept");
        check(report(fitter).contains("Manual parameters"), "manual mode retained in report");

        fitter = line(data(new double[] {0,1,2,3}, new double[] {1,3,5,7}));
        close(fitter.getUncertainty(0), 0, "identifiable perfect-fit slope retains zero uncertainty");
        close(fitter.getUncertainty(1), 0, "identifiable perfect-fit intercept retains zero uncertainty");
        check(report(fitter).contains("A\t2.0\t0.0\t\tNo"), "legitimate zero uncertainty exported");
        for (double[] y : new double[][] {{.1,1.9,3.9,6.1}, {0,2,4,6}}) {
            fitter = line(data(new double[] {0,1,2,3}, y));
            UserFunction redundant = new UserFunction("Redundant");
            redundant.setParameters(new String[] {"A","B"}, new double[] {1,1}, null);
            redundant.setExpression("(A+B)*x", new String[] {"x"});
            // Exercise the same optimizer path with a deliberately non-identifiable model.
            fitter.fit = redundant;
            fitter.fit(redundant, true);
            check(Double.isNaN(fitter.getUncertainty(0)), "unidentifiable A uncertainty unavailable");
            check(Double.isNaN(fitter.getUncertainty(1)), "unidentifiable B uncertainty unavailable");
            String[] rows = report(fitter).split("\n");
            for (String row : rows) {
                if (row.startsWith("A\t") || row.startsWith("B\t"))
                    check(CurveFitReportTest.splitTabs(row)[2].equals("N/A"), "unidentifiable uncertainty exported as N/A");
            }
        }
    }

    static DatasetCurveFitter line(Dataset data) {
        DatasetCurveFitter fitter = new DatasetCurveFitter(data, new FitBuilder(null));
        fitter.selectFit(fitter.getPolyFitNameOfDegree(1));
        fitter.setActiveAndFit(true);
        return fitter;
    }
    static Dataset data(double[] x, double[] y) {
        Dataset data = new Dataset(); data.setXYColumnNames("t", "y"); data.append(x,y); return data;
    }
    static JTable table(DatasetCurveFitter fitter) {
        return (JTable)((JScrollPane)fitter.splitPane.getRightComponent()).getViewport().getView();
    }
    static void toggle(JTable table, int row) {
        if (org.opensourcephysics.display.OSPRuntime.isJS) {
            // This numerical fixture has no DOM. Live browser checkbox editing
            // is exercised separately in the attached Data Tool UI test.
            check(table.isCellEditable(row,1), "fixed checkbox editable");
            table.getModel().setValueAt(!(Boolean)table.getValueAt(row,1),row,1);
        } else {
            check(table.editCellAt(row,1), "fixed checkbox editable");
            ((JCheckBox)table.getEditorComponent()).doClick();
        }
    }
    static String report(DatasetCurveFitter fitter) {
        int count=fitter.fit.getParameterCount(); boolean[] fixed=new boolean[count]; double[] sigma=new double[count];
        for(int i=0;i<count;i++){fixed[i]=(Boolean)table(fitter).getValueAt(i,1);sigma[i]=fitter.getUncertainty(i);}
        return CurveFitReport.create(fitter.fit,fitter.getData(),fixed,sigma,fitter.isAutoFit(),true);
    }
    static String labels(Container parent) {
        String text="";
        for(Component child:parent.getComponents()){
            if(child instanceof JLabel) text+=((JLabel)child).getText()+"\n";
            if(child instanceof Container) text+=labels((Container)child);
        }
        return text;
    }
    static void close(double value,double expected,String message){check(Math.abs(value-expected)<1e-8,message+": "+value);}
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);passed++;System.out.println("PASS: "+message);}
}
