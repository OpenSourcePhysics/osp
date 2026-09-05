package org.opensourcephysics.tools;

import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.table.TableCellEditor;
import org.opensourcephysics.display.Dataset;

/** Context-menu gestures must never initialize a parameter editor. */
public class CurveFitPopupTest {
    static int passed;
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Dataset data = new Dataset();
                data.append(new double[] {0,1,2,3}, new double[] {1,3,5,7});
                DatasetCurveFitter fitter = new DatasetCurveFitter(data, new FitBuilder(null));
                fitter.fit = new KnownPolynomial(new double[] {1,2});
                fitter.setAutoFit(true);
                fitter.uncertainties = new double[] {.05, .01};
                String labels = labels(fitter);
                check(labels.contains("Points: 4"), "point count visible");
                check(labels.contains("Degrees of freedom: 2"), "degrees of freedom visible");
                check(labels.contains("R-squared: 1.0000"), "R-squared visible");
                check(labels.contains("SSE: 0.0000"), "SSE visible");
                check(labels.contains("Residual SE: 0.0000"), "residual standard error visible");
                boolean[] allowEditor = {false};
                DatasetCurveFitter.ParamTable table = fitter.new ParamTable(fitter.new ParamTableModel()) {
                    @Override public TableCellEditor getCellEditor(int row, int column) {
                        if (!allowEditor[0]) throw new AssertionError("Context click initialized editor");
                        DefaultCellEditor editor = new DefaultCellEditor(new JTextField());
                        editor.setClickCountToStart(1);
                        return editor;
                    }
                };
                for (int column : new int[] {1,2}) {
                    reject(table, column, MouseEvent.BUTTON3, 0, true, "right-click popup");
                    reject(table, column, MouseEvent.BUTTON3, 0, false, "right-click before popup release");
                    reject(table, column, MouseEvent.BUTTON1, InputEvent.CTRL_DOWN_MASK, true, "Control-click popup");
                    if (org.opensourcephysics.display.OSPRuntime.isMac()) {
                        reject(table, column, MouseEvent.BUTTON1, InputEvent.CTRL_DOWN_MASK, false, "macOS Control-click");
                    } else {
                        allowEditor[0] = true;
                        MouseEvent controlClick = new MouseEvent(table, MouseEvent.MOUSE_PRESSED, 0,
                                InputEvent.CTRL_DOWN_MASK, 5, 5, 1, false, MouseEvent.BUTTON1);
                        check(table.editCellAt(0, column, controlClick), "non-popup Control-click permits editing");
                        table.getCellEditor().cancelCellEditing();
                        allowEditor[0] = false;
                    }
                }
                check(fitter.isAutoFit(), "Autofit preserved");
                check(fitter.getUncertainty(0) == .05, "uncertainty preserved");
                check(fitter.fit.getParameterValue(0) == 2, "parameter value preserved");
                allowEditor[0] = true;
                MouseEvent left = new MouseEvent(table, MouseEvent.MOUSE_PRESSED, 0, 0, 5, 5, 1, false, MouseEvent.BUTTON1);
                check(table.editCellAt(0, 2, left), "left-click editing still available");
                table.getCellEditor().cancelCellEditing();
                if (args.length > 0) {
                    JFrame frame = new JFrame();
                    frame.setContentPane(fitter);
                    frame.setSize(1000, 250);
                    frame.addNotify();
                    frame.validate();
                    java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(1000, 250, java.awt.image.BufferedImage.TYPE_INT_RGB);
                    java.awt.Graphics2D graphics = image.createGraphics();
                    frame.getContentPane().printAll(graphics);
                    graphics.dispose();
                    try { javax.imageio.ImageIO.write(image, "png", new java.io.File(args[0])); }
                    catch (java.io.IOException ex) { throw new RuntimeException(ex); }
                }
                System.out.println("Passed: " + passed);
            } finally {
                for (Window window : Window.getWindows()) window.dispose();
            }
        });
        System.exit(0);
    }
    static String labels(java.awt.Container parent) {
        StringBuilder text = new StringBuilder();
        for (java.awt.Component child : parent.getComponents()) {
            if (child instanceof JLabel) text.append(((JLabel)child).getText()).append("\n");
            if (child instanceof java.awt.Container) text.append(labels((java.awt.Container)child));
        }
        return text.toString();
    }
    static void reject(JTable table, int column, int button, int modifiers, boolean popup, String label) {
        MouseEvent event = new MouseEvent(table, MouseEvent.MOUSE_PRESSED, 0, modifiers, 5, 5, 1, popup, button);
        check(!table.editCellAt(0, column, event) && !table.isEditing(), label + " column " + column);
    }
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        passed++;
        System.out.println("PASS: " + message);
    }
}
