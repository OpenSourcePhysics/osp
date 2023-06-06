/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.AbstractCellEditor;
import javax.swing.AbstractSpinnerModel;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.opensourcephysics.display.CellBorder;
import org.opensourcephysics.display.ColorIcon;
import org.opensourcephysics.display.Dataset;
import org.opensourcephysics.display.FunctionDrawer;
import org.opensourcephysics.display.MessageDrawable;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.display.TeXParser;
import org.opensourcephysics.display.UncertainFunctionDrawer;
import org.opensourcephysics.numerics.Function;
import org.opensourcephysics.numerics.HessianMinimize;
import org.opensourcephysics.numerics.LUPDecomposition;
import org.opensourcephysics.numerics.LevenbergMarquardt;
import org.opensourcephysics.numerics.MultiVarFunction;

/**
 * A panel that displays and controls functional curve fits to a Dataset.
 *
 * @author Douglas Brown
 * @version 1.0
 */
@SuppressWarnings("serial")
public class DatasetCurveFitter extends JPanel {

	public static final String PROPERTY_DATASETCURVEFITTER_CHANGED = "changed";
	public static final String PROPERTY_DATASETCURVEFITTER_DRAWER = "drawer";
	public static final String PROPERTY_DATASETCURVEFITTER_FIT = "fit";

	// static fields
	public static boolean isSciNotation = true;
	
	/** defaultFits are available in every instance */
	static final String FIT_EXP = "Exponential";
	static final String FIT_LOG = "Log";
	static final String FIT_SIN = "Sinusoid";
	static final String FIT_DAMPED = "DampedSine";
	static final String FIT_GAUSS = "Gaussian";
	static final String FIT_POWER = "Power";
	static final String FIT_TEST = "TestFunction";
	static ArrayList<KnownFunction> defaultFits = new ArrayList<KnownFunction>();
	private final static Border labelBorder = BorderFactory.createEmptyBorder(0, 2, 0, 2);

	static {
		defaultFits.add(new KnownPolynomial(new double[2]));
		defaultFits.add(new KnownPolynomial(new double[3]));
		defaultFits.add(new KnownPolynomial(new double[4]));

		UserFunction f = new UserFunction(FIT_GAUSS); //$NON-NLS-1$
		f.setParameters(new String[] { "A", "B", "C" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				new double[] { 1, 0, 1 },
				new String[] { ToolsRes.getString("Function.Parameter.PeakHeight.Description"), //$NON-NLS-1$
						ToolsRes.getString("Function.Parameter.PeakPosition.Description"), //$NON-NLS-1$
						ToolsRes.getString("Function.Parameter.GaussianRMSWidth.Description")}); //$NON-NLS-1$
		f.setExpression("A * exp(-(x-B)^2 / (2*C^2))", new String[] { "x" }); //$NON-NLS-1$ //$NON-NLS-2$
		f.setDescription(ToolsRes.getString("Function.Gaussian.Description")); //$NON-NLS-1$
		defaultFits.add(f);

		f = new UserFunction(FIT_EXP); //$NON-NLS-1$
		f.setParameters(new String[] { "A", "B", "C" }, //$NON-NLS-1$ //$NON-NLS-2$
				new double[] { 1, -1, 0 }, new String[] { 
						ToolsRes.getString("Function.Parameter.Magnitude.Description"), //$NON-NLS-1$
						ToolsRes.getString("Function.Parameter.ExponentialMultiplier.Description"), //$NON-NLS-1$
						ToolsRes.getString("Function.Parameter.Offset.Description") }); //$NON-NLS-1$
		f.setExpression("A * exp(B*x) + C", new String[] { "x" }); //$NON-NLS-1$ //$NON-NLS-2$
		f.setDescription(ToolsRes.getString("Function.Exponential.Description")); //$NON-NLS-1$
		defaultFits.add(f);

		f = new UserFunction(FIT_SIN); //$NON-NLS-1$
		f.setParameters(new String[] { "A", "B", "C", "D" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				new double[] { 1, 1, 0, 0 }, new String[] { 
						ToolsRes.getString("Function.Parameter.Amplitude.Description"), //$NON-NLS-1$
						ToolsRes.getString("Function.Parameter.Omega.Description"), //$NON-NLS-1$
						ToolsRes.getString("Function.Parameter.Phase.Description"), //$NON-NLS-1$
						ToolsRes.getString("Function.Parameter.Offset.Description") }); //$NON-NLS-1$
		f.setExpression("A * sin(B*x+C) + D", new String[] { "x" }); //$NON-NLS-1$ //$NON-NLS-2$
		f.setDescription(ToolsRes.getString("Function.Sinusoid.Description")); //$NON-NLS-1$
		defaultFits.add(f);
		
		f = new UserFunction(FIT_DAMPED); //$NON-NLS-1$
		f.setParameters(new String[] { "A", "B", "C", "D", "E" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				new double[] { 1, 1, 0, 0, 0 }, new String[] { 
						ToolsRes.getString("Function.Parameter.Intercept.Description"), //$NON-NLS-1$
						ToolsRes.getString("Function.Parameter.Omega.Description"), //$NON-NLS-1$
						ToolsRes.getString("Function.Parameter.Phase.Description"), //$NON-NLS-1$
						ToolsRes.getString("Function.Parameter.Offset.Description"), //$NON-NLS-1$
						ToolsRes.getString("Function.Parameter.ExponentialMultiplier.Description") }); //$NON-NLS-1$
		f.setExpression("A * exp(E*x) * sin(B*x + C) + D", new String[] { "x" }); //$NON-NLS-1$ //$NON-NLS-2$
		f.setDescription(ToolsRes.getString("Function.DampedSine.Description")); //$NON-NLS-1$
		defaultFits.add(f);

		f = new UserFunction(FIT_POWER); //$NON-NLS-1$
		f.setParameters(new String[] { "A", "B" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				new double[] { 1, 1 }, new String[] { 
						ToolsRes.getString("Function.Parameter.Coeff.Description"), //$NON-NLS-1$
						ToolsRes.getString("Function.Parameter.Power.Description") }); //$NON-NLS-1$
		f.setExpression("A * x ^ B", new String[] { "x" }); //$NON-NLS-1$ //$NON-NLS-2$
		f.setDescription(ToolsRes.getString("Function.Power.Description")); //$NON-NLS-1$
		defaultFits.add(f);

		f = new UserFunction(FIT_LOG); //$NON-NLS-1$
		f.setParameters(new String[] { "A", "B" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				new double[] { 1, 0 }, new String[] { 
						ToolsRes.getString("Function.Parameter.Scale.Description"), //$NON-NLS-1$
						ToolsRes.getString("Function.Parameter.Offset.Description") }); //$NON-NLS-1$
		f.setExpression("A * ln(x) + B", new String[] { "x" }); //$NON-NLS-1$ //$NON-NLS-2$
		f.setDescription(ToolsRes.getString("Function.Log.Description")); //$NON-NLS-1$
		defaultFits.add(f);

	}

	// instance fields

	private FitBuilder fitBuilder;
	private PropertyChangeListener fitListener;

	private DataToolTab tab;

	KnownFunction fit; // the function to fit to the data
	double sigma_y_squared = 1; // an estimate of the SD in the y deviations from the fit
	ArrayList<UserFunction> testFunctions = new ArrayList<UserFunction>();;
	Color color = Color.MAGENTA;
	ParamTableModel paramModel;

	/** localFits contains local copies of all fits */
	private ArrayList<KnownFunction> localFits = new ArrayList<KnownFunction>();
	private Dataset dataset; // the data to be fit
	private HessianMinimize hessian = new HessianMinimize();
	private LevenbergMarquardt levmar = new LevenbergMarquardt();
	private UncertainFunctionDrawer drawer;
	/** fitMap maps localized names to all available fits */
	private Map<String, KnownFunction> fitMap = new TreeMap<String, KnownFunction>();
	private Map<KnownFunction, boolean[]> fixedParams = new HashMap<KnownFunction, boolean[]>();
	private Map<KnownFunction, double[]> initialParams = new HashMap<KnownFunction, double[]>();

	int fitNumber = 1;
	boolean refreshing = false;
	private boolean isActive;

	public void setActiveNoFit(boolean b) {
		isActive = b;
	}

	public boolean isActive() {
		return isActive;
	}

	boolean neverBeenActive = true;
	int fontLevel;
	double correlation = Double.NaN;
	double[] uncertainties = new double[2];
	boolean fitEvaluatedToNaN = false;

	private boolean autofit;

	/**
	 * Sets the autofit flag.
	 *
	 * @param auto true to autofit
	 */
	public void setAutofit(boolean auto) {
		autofit = auto;
		if (auto != autofitCheckBox.isSelected())
			autofitCheckBox.doClick(0);
	}

	boolean isAutoFit() {
		return autofit;
	}

	public void setAutoFit(boolean autofit) {
		this.autofit = autofit;
		autofitCheckBox.setSelected(autofit);
		if (!autofit)
			drawer.setUncertainties(null);
	}

	// GUI

	private JButton colorButton, closeButton;
	private JCheckBox autofitCheckBox;
	private JLabel fitLabel, eqnLabel, rmsLabel;
	private JToolBar fitBar, eqnBar, rmsBar;
	private JComboBox<String> fitDropDown;
	private JTextField eqnField;
	private NumberField rmsField;
	private ParamTable paramTable;
	private ParamCellRenderer cellRenderer;
	private SpinCellEditor spinCellEditor; // uses number-crawler spinner
	private JButton fitBuilderButton;

	protected JSplitPane splitPane;

	public JSplitPane getSplitPane() {
		return splitPane;
	}

	private JDialog colorDialog;

	/**
	 * Constructs a DatasetCurveFitter for the specified Dataset.
	 *
	 * @param data    the dataset
	 * @param builder the FitBuilder used for constructing custom fits
	 */
	public DatasetCurveFitter(Dataset data, FitBuilder builder) {
		dataset = data;
		fitBuilder = builder;
		createGUI();
		fitBuilder.removePropertyChangeListener(fitListener);
		fitBuilder.addPropertyChangeListener(fitListener);
//    fit(fit);
	}

	/**
	 * Gets the function drawer.
	 *
	 * @return the drawer
	 */
	public UncertainFunctionDrawer getDrawer() {
		return drawer;
	}

	/**
	 * Gets the data.
	 *
	 * @return the dataset
	 */
	public Dataset getData() {
		return dataset;
	}

	/**
	 * Sets the dataset.
	 *
	 * @param data the dataset - if null, then just initializing
	 */
	public void setData(Dataset data, boolean doFit) {
		dataset = data;
		if (!isActive)
			return;
		if (doFit) {
			fit(fit);
		}
		if (dataset != null) {
			fitBuilder.setDefaultVariables(new String[] { TeXParser.removeSubscripting(dataset.getXColumnName()) });
			if (!isActive) {
				// if active, regression done in fit method
				double[] x = dataset.getValidXPoints();
				double[] y = dataset.getValidYPoints();
				doLinearRegression(x, y);
				refreshStatusBar();
			}
		}
	}

	/**
	 * Sets the color.
	 *
	 * @param newColor the color
	 */
	public void setColor(Color newColor) {
		color = newColor;
		if (drawer != null) {
			drawer.setColor(newColor);
			updateColorButton();
			firePropertyChange(PROPERTY_DATASETCURVEFITTER_CHANGED, null, null); // $NON-NLS-1$
		}
	}

	private void updateColorButton() {
		LookAndFeel currentLF = UIManager.getLookAndFeel();
		boolean nimbus = currentLF.getClass().getName().indexOf("Nimbus") > -1; //$NON-NLS-1$
		if (nimbus) {
			colorButton.setIcon(new ColorIcon(color, 12, DataTool.buttonHeight - 8));
		} else {
			colorButton.setBackground(color);
		}
	}

	/**
	 * Sets the active flag if not already set and does a fit if setting it to true.
	 *
	 * @param active true
	 */
	public void setActiveAndFit(boolean active) {
		if (isActive == active)
			return;
		isActive = active;
		if (active) {
			if (neverBeenActive) {
				neverBeenActive = false;
				setAutoFit(true);
			}
			fit(fit);
		}
	}
	
	/**
	 * Fits a fit function to the current data.
	 *
	 * @param fit the function to fit
	 * @return the rms deviation
	 */
	public double fit(KnownFunction fit) {
		return fit(fit, false);
	}


	/**
	 * Fits a fit function to the current data.
	 *
	 * @param fit the function to fit
	 * @return the rms deviation
	 */
	public double fit(KnownFunction fit, boolean fromScratch) {
		if (drawer == null) {
			selectFit((String) fitDropDown.getSelectedItem());
		}
		if (fit == null)
			return Double.NaN;
		if (dataset == null) {
			if (fit instanceof UserFunction) {
				eqnField.setText("y = " + //$NON-NLS-1$
						((UserFunction) fit).getFullExpression(new String[] { "x" })); //$NON-NLS-1$
			} else {
				eqnField.setText("y = " + fit.getExpression("x").replace(" ", "")); //$NON-NLS-1$ //$NON-NLS-2$
			}
			setAutoFit(false);
			autofitCheckBox.setEnabled(false);
			spinCellEditor.stopCellEditing();
			paramTable.setEnabled(false);
			rmsField.setText(ToolsRes.getString("DatasetCurveFitter.RMSField.NoData")); //$NON-NLS-1$
			rmsField.setForeground(Color.RED);
			return Double.NaN;
		}
		
		double devSq = 0;
		double[] x = dataset.getValidXPoints();
		double[] y = dataset.getValidYPoints();
		
		
		autofitCheckBox.setEnabled(true);
		paramTable.setEnabled(true);
		
		fromScratch = !fit.getName().equals(FIT_TEST) && (fromScratch || fixedParams.get(fit) == null);
		if (fromScratch) {
			// this is a non-test function being fit from scratch
			if (initialParams.get(fit) == null) {
				double[] p = new double[fit.getParameterCount()];
				for (int i = 0 ; i < fit.getParameterCount(); i++) {
					p[i] = fit.getParameterValue(i);
				}
				initialParams.put(fit, p);
			}
			if (fixedParams.get(fit) == null)
				fixedParams.put(fit, new boolean[fit.getParameterCount()]);
			
			double[] scratchParams = getScratchParams(fit, x, y);
			
			if (scratchParams != null) {
				boolean[] fix = fixedParams.get(fit);
				for (int i = 0; i < scratchParams.length; i++) {
					if (!fix[i])
						fit.setParameterValue(i, scratchParams[i]);
				}
			}
		}
		boolean[] fix = fixedParams.get(fit);
		KnownFunction testFit = getTestFunction(fit, fix);
		
		// see if all parameters are fixed--no fitting possible!
		boolean nothingToTest = (fix != null);
		if (fix != null) {
			for (int i = 0; i < fix.length; i++) {
				nothingToTest = nothingToTest && fix[i];
			}
		}
		
		if (nothingToTest) {
			setUncertainties(null);
			tab.refreshPlot();
			drawer.functionChanged = true;
			paramTable.repaint();		
		}		
		else {
	//		if (fit != testFunction)
	//			scan(fit, x, y);
			
			double[] prevParams = null;
			// get deviation before fitting
			double prevDevSq = getDevSquared(fit, x, y);
			// autofit if checkbox is selected
			if (autofit && !Double.isNaN(prevDevSq)) {
				// if any parameters are fixed
				if (testFit instanceof KnownPolynomial) {
					KnownPolynomial poly = (KnownPolynomial) testFit;
					poly.fitData(x, y);
				} else if (testFit instanceof UserFunction) {
					// use HessianMinimize to autofit user function
					UserFunction f = (UserFunction) testFit;
					double[] params = new double[f.getParameterCount()];
					// can't autofit if no parameters or data length < parameter count
					if (params.length > 0 && params.length <= x.length && params.length <= y.length) {
						MinimizeUserFunction minFunc = new MinimizeUserFunction(f, x, y);
						prevParams = new double[params.length];
						for (int i = 0; i < params.length; i++) {
							params[i] = prevParams[i] = f.getParameterValue(i);
						}
						double tol = 1.0E-6;
						int iterations = 20;
						hessian.minimize(minFunc, params, iterations, tol);
						// get deviation after minimizing
						devSq = getDevSquared(testFit, x, y);
						// restore parameters and try Levenberg-Marquardt if Hessian fit is worse
						boolean success = true;
						if (devSq > prevDevSq) {
							for (int i = 0; i < prevParams.length; i++) {
								f.setParameterValue(i, prevParams[i]);
							}
							success = levmar.minimize(minFunc, params, iterations, tol);
							// get deviation after minimizing
							devSq = getDevSquared(testFit, x, y);
						}

						// restore parameters and deviation if new fit is worse
						if (!success || devSq > prevDevSq) {
							for (int i = 0; i < prevParams.length; i++) {
								f.setParameterValue(i, prevParams[i]);
							}
							devSq = prevDevSq;
						}
					}
				}
				if (!testFunctions.contains(fit)) {
					// fit is not a test function
					if (autofit) {
						double[][] sigmas = getUncertainties(fit, testFit, x, y);
						setUncertainties(sigmas);
					}
					else {
						setUncertainties(null);
					}
					if (tab != null)
						tab.refreshPlot();
				}
	
				drawer.functionChanged = true;
				paramTable.repaint();
			}
			// set the parameter values of original fit to those of testFit
			if (fit != testFit) {
				for (int i = 0; i < fit.getParameterCount(); i++) {
					String next = fit.getParameterName(i);
					for (int j = 0; j < testFit.getParameterCount(); j++) {
						if (testFit.getParameterName(j).equals(next)) {
							fit.setParameterValue(i, testFit.getParameterValue(j));
						}
					}
				}
			}
			
		} // end of fitting
		
		doLinearRegression(x, y);
//		doLinearRegression(x, y, false);
		if (devSq == 0) {
			devSq = getDevSquared(fit, x, y);
		}
		double rmsDev = fit.getParameterCount() > x.length && autofit? 
				Double.NaN: 
				Math.sqrt(devSq / x.length);

		rmsField.setForeground(eqnField.getForeground());
		if (x.length == 0 || y.length == 0 || Double.isNaN(rmsDev)) {
			rmsField.setValue(Double.NaN); //$NON-NLS-1$
			rmsField.setToolTipText(ToolsRes.getString("DatasetCurveFitter.InsufficientData.ToolTip"));
		} else {
			rmsField.applyPattern("0.000E0"); //$NON-NLS-1$
			rmsField.setValue(rmsDev);
			rmsField.setToolTipText(null);
		}
		refreshStatusBar();
		firePropertyChange(PROPERTY_DATASETCURVEFITTER_FIT, null, null);
		if (tab != null && tab.areaVisible && tab.measureFit)
			tab.plot.refreshArea();
		
		return rmsDev;
	}

	/**
	 * Adds a fit function.
	 *
	 * @param f               the fit function to add
	 * @param addToFitBuilder ignored--all fits are added to the fit builder
	 */
	public void addFitFunction(KnownFunction f, boolean addToFitBuilder) {
		// check for duplicates
		KnownFunction existing = fitMap.get(f.getName());
		if (existing != null) {
			if (existing.getExpression("x").equals(f.getExpression("x"))) { //$NON-NLS-1$ //$NON-NLS-2$
				return; // duplicate name and expression, so ignore
			}
			// different expression, so change name to something unique
			f.setName(fitBuilder.getUniqueName(f.getName()));
		}

		String selectedFitName = (fit == null ? getPolyFitNameOfDegree(1) : fit.getName());
		fitBuilder.addFitFunction(f);
		fitDropDown.setSelectedItem(selectedFitName);
	}

	/**
	 * Refreshes the parent tab's status bar
	 */
	public void refreshStatusBar() {
		if (tab != null && tab.statsCheckbox.isSelected())
			tab.refreshStatusBar(tab.getCorrelationString());
	}

	/**
	 * Gets the estimated uncertainty (standard error or other) of a best fit
	 * parameter. Returns Double.NaN if uncertainty is unknown or is not best fit.
	 * 
	 * @param paramIndex the parameter index
	 * @return the estimated uncertainty in the parameter
	 */
	public double getUncertainty(int paramIndex) {
		if (uncertainties != null && paramIndex < uncertainties.length && autofit) {
			return uncertainties[paramIndex];
		}
		return Double.NaN;
	}

	/**
	 * Returns two strings describing a parameter and its uncertainty.
	 * One for display, other with more sig figs for tooltip
	 *
	 * @param value the parameter value
	 * @param sigma the uncertainty (may be null)
	 * @return the format values {decimal places, format} or null if uncert unknown or zero
	 */
	public String[] formatUncertainParameter(double value, double sigma, int extraPlaces, NumberFormat format) {
		if (Double.isNaN(sigma) || sigma <= 0) {
			return null;
		}
		
	  int exp = value == 0? 0: (int) Math.floor(Math.log10(Math.abs(value)));
	  int expSig = sigma == 0? 0: (int) Math.floor(Math.log10(Math.abs(sigma)));
	  if (expSig > exp)
	  	exp = expSig;
	  int shift = exp - expSig;
	  double multiplier = Math.pow(10, -exp);
	  int places = Math.max(0, shift) + extraPlaces;
	  
	  String val = String.format("%." + places + "f", value*multiplier);
	  String sig = String.format("%." + places + "f", sigma*multiplier);
		String formatted = val + " \u00B1 " + sig;
		String separator = String.valueOf(OSPRuntime.getCurrentDecimalSeparator());
		formatted = formatted.replace(".", separator);
		if (exp != 0)
			formatted = "(" + formatted +") " + String.format("E%d", exp);
		
		val = format.format(value);
		sig = format.format(sigma);
		String tooltip = val + " \u00B1 " + sig;
		
		return new String[] {formatted, tooltip};
	}
	
	/**
	 * Gets a fit function by name.
	 * 
	 * @param name the name
	 * @return the fit function, or null if none found
	 */
	public KnownFunction getFitFunction(String name) {
		for (int i = localFits.size(); --i >= 0;) {
			if (localFits.get(i).getName().equals(name))
				return localFits.get(i);
		}
		return null;
	}

	/**
	 * Gets the selected fit parameters.
	 *
	 * @return a map of parameter names to values
	 */
	public Map<String, Double> getSelectedFitParameters() {
		return null;
	}

	@Override
	public Dimension getMinimumSize() {
		Dimension dim = fitBar.getPreferredSize();
		dim.height += eqnBar.getPreferredSize().height;
		dim.height += rmsBar.getPreferredSize().height + 1;
		return dim;
	}

	// _______________________ protected & private methods
	// __________________________

	/**
	 * Creates the GUI.
	 */
	protected void createGUI() {
		setLayout(new BorderLayout());
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setResizeWeight(0.7);
		splitPane.setDividerSize(6);
		// create autofit checkbox
		autofitCheckBox = new JCheckBox("", autofit = true); //$NON-NLS-1$
		autofitCheckBox.setOpaque(false);
		autofitCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				autofit = autofitCheckBox.isSelected();
				spinCellEditor.stopCellEditing();
				paramTable.clearSelection();
				if (autofit) {
					fit(fit, true);
				}
				else
					fit(fit);
				firePropertyChange(PROPERTY_DATASETCURVEFITTER_CHANGED, null, null); // $NON-NLS-1$
				// BH check
				paramTable.repaint();
				tab.repaint();
				if (!autofit)
					drawer.setUncertainties(null);
			}

		});

		// create labels
		fitLabel = new JLabel(ToolsRes.getString("DatasetCurveFitter.Label.FitName")); //$NON-NLS-1$
		fitLabel.setBorder(labelBorder);
		eqnLabel = new JLabel(ToolsRes.getString("DatasetCurveFitter.Label.Equation")); //$NON-NLS-1$
		eqnLabel.setBorder(labelBorder);
		rmsLabel = new JLabel();
		rmsLabel.setBorder(labelBorder);
		fitDropDown = new JComboBox<String>() {

			// override getPreferredSize method so has same height as buttons
			@Override
			public Dimension getPreferredSize() {
				return fixSize(super.getPreferredSize());
			}

			// override addItem method so items are in alphabetical order
			// except line & parabola are first
			@Override
			public void addItem(String obj) {
				if (obj == null)
					return;
				String line = getPolyFitNameOfDegree(1);
				String parabola = getPolyFitNameOfDegree(2);
				String name = FitBuilder.localize((String) obj);
				int count = getItemCount();
				// add in alphabetical order, ignoring case
				boolean added = false;
				for (int i = 0; i < count; i++) {
					String next = FitBuilder.localize((String) getItemAt(i));
					if (next != null && name.compareToIgnoreCase(next) < 0) {
						// item comes after name, so insert name here
						insertItemAt(obj, i);
						added = true;
						break;
					}
				}
				if (!added) {
					// add at end
					super.addItem(obj);
				}
				if (obj.equals(line)) {
					removeItem(obj);
					insertItemAt(obj, 0);
				}
				else if (obj.equals(parabola)) {
					removeItem(obj);
					insertItemAt(obj, 0);
				}

			}
		};

		for (KnownFunction f : defaultFits) {
			localFits.add(f.clone());
		}

		// refresh fitMap and initialize fitDropDown with local fits
		refreshFitMap();
		for (String next : fitMap.keySet()) {
			fitDropDown.addItem(next);
		}
		fitDropDown.setSelectedItem(getPolyFitNameOfDegree(1));

		fitDropDown.addActionListener(new ActionListener() {
			@Override	
			public void actionPerformed(ActionEvent e) {
				DatasetCurveFitter f;
				if (refreshing 
						|| (f = fitBuilder.getSelectedCurveFitter()) == null || f != DatasetCurveFitter.this)
					return;
				String selection = (String) fitDropDown.getSelectedItem();
				if (selection != null && fit != null && !selection.equals(fit.getName())) {
					firePropertyChange(PROPERTY_DATASETCURVEFITTER_CHANGED, null, null); // $NON-NLS-1$
				}
				selectFit(selection);
				fitDropDown.setToolTipText(fit == null ? null : fit.getDescription());

			}

		});
		// BH 2020.02.13 can't extend a Basic renderer -- that's a UI component.
		class FitDropDownRenderer extends JLabel implements ListCellRenderer<String> {

			public FitDropDownRenderer() {

				super();
				setOpaque(true);
				setBorder(new EmptyBorder(1, 1, 1, 1));
			}

			@Override
			public Dimension getPreferredSize() {
				Dimension size;

				if ((this.getText() == null) || (this.getText().equals(""))) {
					setText(" ");
					size = super.getPreferredSize();
					setText("");
				} else {
					size = super.getPreferredSize();
				}

				return size;
			}

			@Override
			public Component getListCellRendererComponent(@SuppressWarnings("rawtypes") JList list, String value,
					int index, boolean isSelected, boolean cellHasFocus) {
				if (isSelected) {
					setBackground(list.getSelectionBackground());
					setForeground(list.getSelectionForeground());
					int length = fitDropDown.getItemCount();
					if (index >= 0 && index < length) {
						KnownFunction func = getFitFunction((String) fitDropDown.getItemAt(index));
						list.setToolTipText(func == null ? null : func.getDescription());
					}
				} else {
					setBackground(list.getBackground());
					setForeground(list.getForeground());
				}
				setFont(list.getFont());
				setText((value == null) ? "" : FitBuilder.localize(value.toString())); //$NON-NLS-1$
				return this;
			}
		}

		fitDropDown.setRenderer(new FitDropDownRenderer());

		// create equation field
		eqnField = new JTextField() {
			@Override
			public Dimension getPreferredSize() {
				return fixSize(super.getPreferredSize());
			}

		};
		eqnField.setEditable(false);
		eqnField.setEnabled(true);
		eqnField.setBackground(Color.white);
		eqnField.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// create clone or open user function if double-clicked
				if (e.getClickCount() == 2) {
					String name = fitDropDown.getSelectedItem().toString();
					if (fitBuilder.getPanelNames().contains(name)) {
						fitBuilder.setSelectedPanel(name);
					} else {
						UserFunction uf = createClone(fit, name);
						UserFunctionEditor editor = new UserFunctionEditor();
						editor.setMainFunctions(new UserFunction[] { uf });
						FitFunctionPanel panel = new FitFunctionPanel(editor);
						fitBuilder.addPanel(uf.getName(), panel);
						fitDropDown.setSelectedItem(uf.getName());
					}
					fitBuilder.setVisible(true);
				}
			}
		});
		// create dataBuilder button
		colorButton = DataTool.createButton("    ", false); //$NON-NLS-1$
		colorButton.setToolTipText(ToolsRes.getString("DatasetCurveFitter.Button.Color.Tooltip")); //$NON-NLS-1$
		colorButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JDialog dialog = getColorDialog();
				closeButton.setText(ToolsRes.getString("Button.OK")); //$NON-NLS-1$
				dialog.setTitle(ToolsRes.getString("DatasetCurveFitter.Dialog.Color.Title")); //$NON-NLS-1$
				dialog.setVisible(true);
			}

		});
		colorButton.setBorder(new EmptyBorder(7,1,5,3));
		// create rms field
		rmsField = new NumberField(6) {
			@Override
			public Dimension getPreferredSize() {
				return fixSize(super.getPreferredSize());
			}

		};
		rmsField.setEditable(false);
		rmsField.setEnabled(true);
		rmsField.setBackground(Color.white);
		// create table
		cellRenderer = new ParamCellRenderer();
		spinCellEditor = new SpinCellEditor();
		paramModel = new ParamTableModel();
		paramTable = new ParamTable(paramModel);
		paramTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				// clear selection if pressed on the name column
				if (paramTable.getSelectedColumn() == 0) {
					paramTable.clearSelection();
				}
			}

		});
		JScrollPane scroller = new JScrollPane(paramTable) {
			@Override
			public Dimension getMinimumSize() {
				Dimension dim = spinCellEditor.spinner.getPreferredSize();
				dim.width += cellRenderer.fieldFont.getSize() * 7;
				return dim;
			}
		};
		scroller.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (OSPRuntime.isPopupTrigger(e))
					paramTable.showPopup(e);
			}
		});
		splitPane.setRightComponent(scroller);
		add(getSplitPane(), BorderLayout.CENTER);
		// create fit builder button
		fitBuilderButton = DataTool.createButton(ToolsRes.getString("DatasetCurveFitter.Button.Define.Text")); //$NON-NLS-1$
		fitBuilderButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
//				setAutoFit(false);
				String fitName = fit.getName();
				fitBuilder.refreshDropdown(fitName);
				if (fitName != null && fitBuilder.getPanelNames().contains(fitName)) {
					fitBuilder.setSelectedPanel(fitName);
				} else if (fitBuilder.getSelectedName() != null) {
					fitDropDown.setSelectedItem(fitBuilder.getSelectedName());
				}
				fitBuilder.refreshGUI();
				fitBuilder.setVisible(true);
			}

		});
		// create fit listener
		fitListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				processPropertyChange(e);
			}
		};

		// add local fits to fitBuilder
		Collection<KnownFunction> fits = new ArrayList<KnownFunction>(localFits);
		for (KnownFunction f : fits) {
			if (!fitBuilder.addFitFunction(f)) {
				// fit declined--a modified version of it must have been loaded
				// so remove it from localFits
				localFits.remove(f);
			}
		}
		// add fitBuilder functions to localFits list
		for (String next : fitBuilder.getPanelNames()) {
			FitFunctionPanel panel = (FitFunctionPanel) fitBuilder.getPanel(next);
			KnownFunction f = getFitFunction(panel);
			if (localFits.contains(f))
				continue;
			localFits.add(f);
		}

		// assemble components
		JPanel fitPanel = new JPanel(new BorderLayout());
		splitPane.setLeftComponent(fitPanel);
		fitBar = new JToolBar();
		fitBar.setFloatable(false);
		fitBar.setBorder(BorderFactory.createEtchedBorder());
		fitBar.add(fitLabel);
		fitBar.add(fitDropDown);
		fitBar.addSeparator();
		fitBar.add(fitBuilderButton);
		fitPanel.add(fitBar, BorderLayout.NORTH);
		JPanel eqnPanel = new JPanel(new BorderLayout());
		fitPanel.add(eqnPanel, BorderLayout.CENTER);
		eqnBar = new JToolBar();
		eqnBar.setFloatable(false);
		eqnBar.setBorder(BorderFactory.createEtchedBorder());
		eqnBar.add(eqnLabel);
		eqnBar.add(eqnField);
		eqnBar.add(colorButton);
		// override JToolBar border for JavaScript to paint this
		eqnPanel.add(eqnBar, BorderLayout.NORTH);
		JPanel rmsPanel = new JPanel(new BorderLayout());
		eqnPanel.add(rmsPanel, BorderLayout.CENTER);
		rmsBar = new JToolBar();
		rmsBar.setLayout(new BoxLayout(rmsBar, BoxLayout.X_AXIS) {
		    @Override
				public void layoutContainer(Container target) {
		    	super.layoutContainer(target);
		    }
		});
		rmsBar.setFloatable(false);
		rmsBar.setBorder(BorderFactory.createEtchedBorder());
		rmsBar.add(autofitCheckBox);
		rmsBar.addSeparator();
		rmsBar.add(rmsLabel);
		rmsBar.add(rmsField);
		rmsPanel.add(rmsBar, BorderLayout.NORTH);
		refreshGUI();
//    refreshFitDropDown();
	}

	protected Dimension fixSize(Dimension dim) {
		dim.height = DataTool.buttonHeight - 2;
		return dim;
	}

	protected void processPropertyChange(PropertyChangeEvent e) {
		if (refreshing)
			return;
		boolean isSelectedCurveFitter = (fitBuilder.getSelectedCurveFitter() == this);
		FitFunctionPanel panel;
		KnownFunction f;
		String name;
		String prop = e.getPropertyName();
		switch (prop) {
		case FunctionTool.PROPERTY_FUNCTIONTOOL_FUNCTION:
			name = (String) e.getNewValue(); // fit or parameter name
			// determine old and new fit names
			panel = (FitFunctionPanel) fitBuilder.getSelectedPanel();
			String fitName = panel.getName();
			String oldFitName = fitName; // assume name unchanged
			if (name.equals(fitName) && e.getOldValue() != null && e.getOldValue() instanceof String) {
				oldFitName = (String) e.getOldValue();
			}
			f = getFitFunction(panel);
			replaceFit(oldFitName, fitName, f);
			if (!fitName.equals(oldFitName)) {
				fitDropDown.addItem(fitName);
			}
			if (isSelectedCurveFitter && tab != null && tab.dataTool != null && !tab.dataTool.isLoading) {
				fitDropDown.setSelectedItem(fitName);
			}
			break;
		case FunctionTool.PROPERTY_FUNCTIONTOOL_PANEL:
			if (e.getNewValue() != null) { // panel selected or added
				f = getFitFunction((FitFunctionPanel) e.getNewValue());
				name = f.getName();
				if (!fitMap.keySet().contains(name)) {
					// new fit panel added
					localFits.add(f);
					fitMap.put(name, f);
					fitDropDown.addItem(name);
				} else {
					// existing panel selected
				}
				if (fitBuilder.isVisible() && isSelectedCurveFitter && tab != null && tab.dataTool != null
						&& !tab.dataTool.isLoading) {
					fitDropDown.setSelectedItem(name);
				}
			}
			if (e.getOldValue() != null) {
				f = getFitFunction((FitFunctionPanel) e.getOldValue());
				if (!fitBuilder.getPanelNames().contains(f.getName())) {
					// fit panel deleted
					localFits.remove(f);
				}
			}
			break;
		default:
			return;
		}
		firePropertyChange(PROPERTY_DATASETCURVEFITTER_CHANGED, null, null); // $NON-NLS-1$
		refreshGUI();
	}

	/**
	 * Refreshes the GUI.
	 */
	protected void refreshGUI() {
		autofitCheckBox.setText(ToolsRes.getString("Checkbox.Autofit.Label")); //$NON-NLS-1$
		rmsLabel.setText(ToolsRes.getString("DatasetCurveFitter.Label.RMSDeviation")); //$NON-NLS-1$
		fitBuilderButton.setText(ToolsRes.getString("DatasetCurveFitter.Button.Define.Text")); //$NON-NLS-1$
		fitBuilderButton.setToolTipText(ToolsRes.getString("DatasetCurveFitter.Button.Define.Tooltip")); //$NON-NLS-1$
		fitLabel.setText(ToolsRes.getString("DatasetCurveFitter.Label.FitName")); //$NON-NLS-1$
		eqnLabel.setText(ToolsRes.getString("DatasetCurveFitter.Label.Equation")); //$NON-NLS-1$
		updateColorButton();
		refreshFitDropDown();
	}
	
	/**
	 * Refreshes the decimal separators.
	 */
	protected void refreshDecimalSeparators() {
		repaint();
		spinCellEditor.field.setValue(spinCellEditor.field.getValue());
	}

	/**
	 * Refreshes the fitDropDown.
	 */
	protected void refreshFitDropDown() {
		Runnable runner = new Runnable() {
			@Override
			public synchronized void run() {

				refreshFitMap();
				String line = getPolyFitNameOfDegree(1);
				String parabola = getPolyFitNameOfDegree(2);
				fitBuilder.defaultFitName = line;
				String toSelect = fitBuilder.defaultFitName;

				refreshing = true;
				fitDropDown.removeAllItems();
				for (String name : fitMap.keySet()) {
					if (fit != null && name.equals(fit.getName())) {
						toSelect = name;
					}
					if (!name.equals(line) && !name.equals(parabola)) {
						if (toSelect == name) {
							String localized = ToolsRes.getString("Function." + name + ".Name");
							if (!localized.startsWith("!"))
								toSelect = name = localized;
						}
						fitDropDown.addItem(name);
					}
				}
				fitDropDown.addItem(parabola); // placed at top
				fitDropDown.addItem(line); // placed at top, pushes parabola to second
				fitDropDown.setSelectedItem(toSelect);
				refreshing = false;
			}
		};
		// invoke later so UI responds
		SwingUtilities.invokeLater(runner);
	}
	
	/**
	 * Refreshes the fit map with localized names.
	 * 
	 * @return a list of
	 */
	protected void refreshFitMap() {
		fitMap.clear();
		for (KnownFunction f : localFits) {
			fitMap.put(f.getName(), f);
		}
	}

	/**
	 * Gets the name of a fit function for specified polynomial degree
	 * 
	 * @return the name of the function
	 */
	public String getPolyFitNameOfDegree(int degree) {
		for (String key : fitMap.keySet()) {
			KnownFunction f = fitMap.get(key);
			if (f instanceof KnownPolynomial) {
				KnownPolynomial poly = (KnownPolynomial) f;
				if (poly.getParameterCount() == degree + 1)
					return key;
			}
		}
		return null;
	}

	protected void setDataToolTab(DataToolTab tab) {
		this.tab = tab;
	}

	/**
	 * Sets the font level.
	 *
	 * @param level the level
	 */
	protected void setFontLevel(int level) {
		fontLevel = level;
		FontSizer.setFonts(this, fontLevel);
		fitBuilder.setFontLevel(level);
		splitPane.setDividerLocation(splitPane.getMaximumDividerLocation());
	}

	/**
	 * Sets the value of a parameter.
	 *
	 * @param row   the row number
	 * @param value the value
	 */
	protected void setParameterValue(int row, double value) {
		if (row < fit.getParameterCount()) {
			fit.setParameterValue(row, value);
		}
	}

	/**
	 * Selects a named fit.
	 * 
	 * @param name the name of the fit function
	 */
	protected void selectFit(String name) {
		if (refreshing)
			return;
		
		if (name == null)
			name = getPolyFitNameOfDegree(1);
		fit = fitMap.get(name);
		if (fit != null) {
			FunctionDrawer prev = drawer;
			drawer = new UncertainFunctionDrawer(fit);
			drawer.setColor(color);
			paramTable.tableChanged(null);
			// construct equation string
			String depVar = (dataset == null) ? "y" : //$NON-NLS-1$
					TeXParser.removeSubscripting(dataset.getColumnName(1));
			String indepVar = (dataset == null) ? "x" : //$NON-NLS-1$
					TeXParser.removeSubscripting(dataset.getColumnName(0));
			if (fit instanceof UserFunction) {
				eqnField.setText(depVar + " = " + //$NON-NLS-1$
						((UserFunction) fit).getFullExpression(new String[] { indepVar }));
			} else {
				eqnField.setText(depVar + " = " + fit.getExpression(indepVar).replace(" ", "")); //$NON-NLS-1$
			}
			firePropertyChange(PROPERTY_DATASETCURVEFITTER_DRAWER, prev, drawer); // $NON-NLS-1$
			if (isActive)
				fit(fit);
			if (fitBuilder.isVisible()) {
				fitBuilder.setSelectedPanel(fit.getName());
			}
			paramTable.getColumnModel().getColumn(1).setMaxWidth(getMinCheckboxColumnWidth() + 10);
			
			revalidate();
		}
		setActiveAndFit(true);
	}
	
	private int getMinCheckboxColumnWidth() {
		String s = ToolsRes.getString("DatasetCurveFitter.Table.Heading.FixedParam");
		Font font = paramTable.getTableHeader().getFont();
		FontMetrics fm = paramTable.getTableHeader().getFontMetrics(font);
		return fm.stringWidth(s);
	}

	protected UserFunction createClone(KnownFunction f, String name) {
		String var = (dataset == null) ? "x" : //$NON-NLS-1$
				TeXParser.removeSubscripting(dataset.getColumnName(0));
		UserFunction uf = f.newUserFunction(var);
		// add digit to end of name
		int n = 1;
		try {
			int len = name.length() - 1;
			String number = name.substring(len);
			n = Integer.parseInt(number) + 1;
			name = name.substring(0, len);
		} catch (Exception ex) {
		}
		// make a set of existing fit names
		Set<String> names = new HashSet<String>();
		for (int i = 0; i < fitDropDown.getItemCount(); i++) {
			names.add(fitDropDown.getItemAt(i).toString());
		}
		// increment digit at end of name if necessary
		try {
			while (names.contains(name + n)) {
				n++;
			}
		} catch (Exception ex) {
		}
		uf.setName(name + n);
		return uf;
	}

//  /**
//   * Shifts data values by a fixed offset
//   * 
//   * @param values an array of values
//   * @param offset the shift
//   * @return an array with shifted values
//   */
//  private double[] shiftValues(double[] values, double offset) {
//  	if (offset==0) return values;
//  	for (int i=0; i<values.length; i++) {
//  		values[i] += offset;
//  	}
//  	return values;
//  }
//  
	/**
	 * Gets the total deviation squared between function and data
	 * @param f a Function
	 * @param x the x data
	 * @param y the y data 
	 * @return sum of squares of deviations
	 */
	private double getDevSquared(Function f, double[] x, double[] y) {
		fitEvaluatedToNaN = false;
		double total = 0;
		for (int i = 0; i < x.length; i++) {
			double next = f.evaluate(x[i]);
			if (f instanceof UserFunction && tab != null) {
				fitEvaluatedToNaN = fitEvaluatedToNaN || ((UserFunction) f).evaluatedToNaN();
			}
			double dev = (next - y[i]);
			total += dev * dev;
		}
		if (tab != null) {
			tab.plot.setMessage(fitEvaluatedToNaN ? ToolsRes.getString("DatasetCurveFitter.Warning.FunctionError") : "", MessageDrawable.TOP_RIGHT);
		}
		return fitEvaluatedToNaN ? Double.NaN : total;
	}

	/**
	 * Calibrates chi squared to (x.length - paramCount) for a function & data.
	 * @param f a KnownFunction
	 * @param x the x data
	 * @param y the y data
	 * 
	 * @return calibrated chi squared = x.length - paramCount
	 */
  private double calibrateChiSquared(KnownFunction f, double[] x, double[] y) {
  	int paramCount = f.getParameterCount();
 	// set sigma so that chi squared is x.length - paramCount
    sigma_y_squared = getDevSquared(f, x, y) / (x.length - paramCount); 
    return x.length - paramCount;
  }

	/**
	 * Gets chi squared between function and data based on existing calibration
	 * @param f a Function
	 * @param x the x data
	 * @param y the y data
	 * @return chi squared
	 */
	private double getChiSquared(Function f, double[] x, double[] y) {
    return getDevSquared(f, x, y)/sigma_y_squared;    
	}
	
	/**
	 * Gets uncertainties in the parameters of a function.
	 * The fitted function's parameters must have best fit values.
	 * @param original the original fit function with all parameters
	 * @param fitted the function to determine uncertainties
	 * @param x the x data
	 * @param y the y data
	 * 
	 * @return double[][] {{param uncertainties}, {fit params 1}, {fit params 2}, ...}
	 */
	private double[][] getUncertainties(KnownFunction original, KnownFunction fitted, double[] x, double[] y) {
		// calibrate (set sigma_y_squared) and get min chi squared (= x.length-f.paramCount)
		int fitCount = fitted.getParameterCount();
		if (fitCount == 0 || x.length - fitCount <= 0)
			return null;
		
		double minChiSquared = calibrateChiSquared(fitted, x, y);
				
		int paramCount = original.getParameterCount();
		double[] params = new double[paramCount];
		String[] paramNames = new String[paramCount];
		for (int i = 0; i < paramCount; i++) {
			params[i] = original.getParameterValue(i);
			paramNames[i] = original.getParameterName(i);
		}
		
		int[] fittedParamIndex = new int[paramCount];
		for (int i = 0; i < paramCount; i++) {
			fittedParamIndex[i] = -1; // unknown
			String name = original.getParameterName(i);
			for (int k = 0; k < fitCount; k++) {
				if (fitted.getParameterName(k).equals(name)) {
					fittedParamIndex[i] = k;
				}
			}
		}
		
		ArrayList<double[]> results = new ArrayList<double[]>();
		
		// for each parameter in fitted function, measure curvature of chi squared to get sigma
		double[] sigmas = new double[paramCount];    
    for (int i = 0; i < paramCount; i++) {
    	
    	if (fittedParamIndex[i] < 0) {
    		sigmas[i] = Double.NaN;
    		continue;
    	}
    	
    	String paramName = fitted.getParameterName(fittedParamIndex[i]);
    	double val = fitted.getParameterValue(fittedParamIndex[i]);
    	double delta = (Math.abs(val)+1.0)/1e5; //step sizes for the finite differences
    	double chiSq = 0; // sum of both shifted-parameter chi squared values
    	double twiceDeltaChiSq = 0;
			double[][] testParams = new double[2][];
			int tries = 0;
    	while ((twiceDeltaChiSq < 0.001 || twiceDeltaChiSq > 2) && tries < 10) {
    		// adjust delta if needed
    		if (twiceDeltaChiSq < 0.001 && twiceDeltaChiSq != 0)
    			delta *= 10;
    		else if (twiceDeltaChiSq > 2)
    			delta /= 10;
    		chiSq = 0;
	    	for (int j = 0; j < 2; j++) {
		     	double paramVal = j == 0? val - delta: val + delta;
					// get test function with a fixed parameter
					UserFunction test = getTestFunction(fitted, paramName, paramVal);
					if (test == null)
						break;
					// fit the test function to minimize its chi squared
					fit(test);
					// add chi squared of test function
					chiSq += getChiSquared(test, x, y);
	    	}
				twiceDeltaChiSq = chiSq - 2 * minChiSquared;
				tries ++;
    	}
    	if (twiceDeltaChiSq > 0) { // success: positive curvature so can determine sigma
				// use eqn 8.13 in Data Reduction and Error Analysis For the Physical Sciences
    		sigmas[i] = delta * Math.sqrt(2 / twiceDeltaChiSq);
    		
    		// offset fixed parameter by +/- sigma and save test parameters for drawer
	    	for (int j = 0; j < 2; j++) {
		     	double paramVal = j == 0? val - sigmas[i]: val + sigmas[i];
					// get test function from fitted function with fixed parameter
					UserFunction test = getTestFunction(fitted, paramName, paramVal);
					// fit the test function to minimize its chi squared
					fit(test);
					// save array of test fit parameters for drawer
					testParams[j] = new double[paramCount];
					for (int k = 0; k < paramCount; k++) {
						String next = original.getParameterName(k);
			    	if (next.equals(paramName)) {
			    		testParams[j][k] = paramVal;
			    		continue;
			    	}
			    	
			    	for (int m = 0; m < test.getParameterCount(); m++) {
			    		String testName = test.getParameterName(m);
			    		if (next.equals(testName)) {			    		
				    		testParams[j][k] = test.getParameterValue(m);
				    		continue;
				    	}			    			
		    		}
			    	testParams[j][k] = original.getParameterValue(k);
					}
					results.add(testParams[j]);
	    	}
    	}
    }
		results.add(0, sigmas);
		return results.toArray(new double[results.size()][]);
	}
	
	/**
	 * Gets parameter estimates from scratch based on the fit function and data.
	 * May return null.
	 * 
	 * @param f the function
	 * @param x
	 * @param y
	 * @return array of parameter values, or null if unable to estimate
	 */
	private double[] getScratchParams(KnownFunction f, double[] x, double[] y) {
		if (f instanceof KnownPolynomial)
			return null;
		double[] params = new double[f.getParameterCount()];
		if (params.length == 0 || x == null || x.length < params.length)
			return null;
		// use initial parameters as fallback
		double [] initParams = initialParams.get(f);
		if (initParams != null)
			params = initParams;
		
		int dataLen = x.length;
		
		// make arrays of sorted data and find min/max
		double ymax = -Double.MAX_VALUE;
		double ymin = Double.MAX_VALUE;		
		double[] sortedX = new double[dataLen];
		double[] sortedY = new double[dataLen];
		TreeMap<Double, Double> sorted = new TreeMap<Double, Double>();
		for (int i = 0; i < dataLen; i++) {
			sorted.put(x[i], y[i]);
		}
		int index = 0;
		for (double d: sorted.keySet()) {
			sortedX[index] = d;
			index++;
		}		
		index = 0;
		for (double d: sorted.values()) {
			sortedY[index] = d;
			ymax = Math.max(d, ymax);
			ymin = Math.min(d, ymin);
			index++;
		}
		double xmax = sortedX[dataLen - 1], xmin = sortedX[0];

		// line for making linear fits
		KnownPolynomial line = (KnownPolynomial)getFitFunction(getPolyFitNameOfDegree(1));
		
		String exp = f.getExpression("x");
		String name = f.getName();
		boolean isDefault = defaultFits.contains(f);
		switch(name) {
			case FIT_SIN:
				// A * sin(B*x + C) + D
			case FIT_DAMPED:
				// A * exp(E*x) * sin(B*x + C) + D
				// set vertical offset D and amplitude A from data range
				if (exp.contains("+D"))  // distinguish from damped sine
					params[3] = (ymin + ymax) /2; // offset D midway between max and min
				params[0] = (ymax - ymin) / 2; // amplitude A half of range
				int crossings = 0;
				double firstCrossing = Double.MAX_VALUE, lastCrossing = Double.MAX_VALUE;
				double prevX = Double.MAX_VALUE, prevY = Double.MAX_VALUE;
				// count zero crossings and measure first crossing
				boolean posSlope = true;
				for (int i = 0; i < sortedX.length; i++) {
					double yshifted = sortedY[i] - (params.length>3? params[3]: 0);
					if (i==0) {
						prevX = sortedX[i];
						prevY = yshifted;
					}
					boolean crossed = prevY > 0? yshifted <= 0: yshifted > 0;
					if (crossed) {
						crossings++;
						// save crossing position
						lastCrossing = sortedX[i] - (yshifted / (yshifted - prevY)) * (sortedX[i] - prevX);
						if (crossings ==1) {
							firstCrossing = lastCrossing;
							posSlope = yshifted > 0;
						}
					}
					prevX = sortedX[i];
					prevY = yshifted;
				}
				boolean success = firstCrossing != Double.MAX_VALUE;
				
				if (crossings > 1)
					params[1] = Math.PI * (crossings-1) / (lastCrossing - firstCrossing);
				else {
					params[1] = Math.PI * Math.max(1,crossings) / (xmax - xmin);
				}
				
				double phaseToFirstCrossing = success? params[1] * firstCrossing: 0;
				params[2] = posSlope? - phaseToFirstCrossing: Math.PI - phaseToFirstCrossing;
				return params;
			case FIT_EXP:
				// y = A * exp(B*x) + C				
				// find three x-points equally spaced across range to find vertical offset C
				double range = (sortedX[dataLen - 1] - sortedX[0]);
				int mid = (dataLen - 1) / 2;
				while (sortedX[mid] - sortedX[0] < range/2 && mid < dataLen-1)
					mid++;
				while (sortedX[mid] - sortedX[0] > range/2 && mid > 0)
					mid--;
				int tail = Math.min(dataLen-1, 2*mid);
				while (sortedX[tail] - sortedX[0] < 2 * (sortedX[mid] - sortedX[0]) && tail < dataLen-1)
					tail++;
				while (sortedX[tail] - sortedX[0] > 2 * (sortedX[mid] - sortedX[0]) && tail > 0)
					tail--;
				
				if (exp.contains("+C")) {
					// set offset
					params[2] = (sortedY[mid] * sortedY[mid] - sortedY[0] * sortedY[tail])
							/ (2 * sortedY[mid] - sortedY[0] - sortedY[tail]);
					// be sure offset doesn't exceed ymin so y - offset > 0 for logs
					params[2] = Math.min(ymin - (0.001 * range), params[2]);
				}
				double offset = params.length > 2? params[2]: 0;
				params[1] = Math.log((sortedY[tail] - offset) / (sortedY[0] - offset)) / (sortedX[tail] - sortedX[0]);
				params[0] = (sortedY[mid] - offset) / Math.exp(params[1] * sortedX[mid]);
				
//				// make array of ln(y-C), fit line to x - ln(y) data, use line parameters
//				for (int i = 0; i < y.length; i++) {
//					ln[i] = Math.log(y[i] - params[2]);
//				}
//				line.fitData(x, ln);
//				params[0] = Math.exp(line.getParameterValue(0));
//				params[1] = line.getParameterValue(1);						
				
				return params;
			case FIT_GAUSS:
				// A * exp(-(x-B)^2 / (2*C^2))
				
				// this method described on page 7 in Régressions Et Équations Intégrales
				// posted online by Jean Jacquelin
				// https://www.scribd.com/doc/14674814/Regressions-et-equations-integrales
				double[] S = new double[dataLen];
				double[] T = new double[dataLen];
				double sumSSq = 0, sumTSq = 0, sumST = 0, sumSy = 0, sumTy = 0;
				double sumy = sortedY[0];
				S[0] = T[0] = 0;
				for (int i = 1; i < dataLen; i++) {
					S[i] = S[i-1] + 0.5 * (sortedY[i] + sortedY[i-1]) * (sortedX[i] - sortedX[i-1]);
					T[i] = T[i-1] + 0.5 * (sortedX[i]*sortedY[i] + sortedX[i-1]*sortedY[i-1]) * (sortedX[i] - sortedX[i-1]);
					sumSSq += S[i] * S[i];
					sumST += S[i] * T[i];
					sumTSq += T[i] * T[i];
					sumSy += S[i] * (sortedY[i] - sortedY[0]);
					sumTy += T[i] * (sortedY[i] - sortedY[0]);
					sumy += sortedY[i];
				}
				double[][] matrix = new double[][] {{sumSSq, sumST}, {sumST, sumTSq}};
		    LUPDecomposition lupSystem = new LUPDecomposition(matrix);
		    double[][] inverse = lupSystem.inverseMatrixComponents();
		    if (inverse == null)
		    	return null;
		    double[] constants = new double[] {sumSy, sumTy};
		    double[] results = lupSystem.solve(constants);
		    double a = -results[0] / results[1];
		    double b = -2 / results[1];
		    double sumExp = 0;
		    for (int i = 1; i < dataLen; i++) {
		    	sumExp += Math.exp(-((sortedX[i] - a) * (sortedX[i] - a)) / b);
		    }
				params[0] = sumy / sumExp; // A
				params[1] = a;
				params[2] = Math.sqrt(b / 2);
				return params;
			case FIT_LOG:
				// A * ln(x) + B
				if (xmin <= 0)
					return null;
				// make array of ln(x), fit linear fit to ln(x) & y data, use fit A and B
				double[] ln = new double[dataLen];
				for (int i = 0; i < dataLen; i++) {
					ln[i] = Math.log(sortedX[i]);
				}
				line.fitData(ln, sortedY);
				params[0] = line.getParameterValue(0);
				params[1] = line.getParameterValue(1);						
				return params;
			case FIT_POWER:
				// A * x ^ B
				if (xmin <= 0 || ymin <= 0)
					return null;
								
				// make arrays of ln x and ln y, fit line, use fit A and B
				double[] lnx = new double[dataLen];
				double[] lny = new double[dataLen];
				for (int i = 0; i < dataLen; i++) {
					lnx[i] = Math.log(sortedX[i]);
					lny[i] = Math.log(sortedY[i]);
				}
				line.fitData(lnx, lny);
				params[0] = Math.exp(line.getParameterValue(1));
				params[1] = line.getParameterValue(0);						
				return params;
		}
		return null;
	}
	
	private UserFunction getTestFunction(int level) {
		while (testFunctions.size() <= level) {
			UserFunction test = new UserFunction(FIT_TEST);
			testFunctions.add(test);
		}
		return testFunctions.get(level);		
	}
	
	/**
	 * Gets a test function that mimics an input function but fixes one of the parameters.
	 * @param f a KnownFunction
	 * @param paramName name of the fixed parameter
	 * @param paramVal value of the fixed parameter
	 * @return the test function
	 */
	private UserFunction getTestFunction(KnownFunction f, String paramName, double paramVal) {
		
		int level = 0;
		for (int i = 0; i < testFunctions.size(); i++) {
			if (testFunctions.get(i) == f) {
				level = i + 1;
				break;
			}
		}
		UserFunction testFunction = getTestFunction(level);
		
    // set up parameters
		int len = f.getParameterCount();
		if (len < 1) 
			return null;
    String[] paramNames = new String[len - 1];
    double[] paramValues = new double[len - 1];
    String[] desc = new String[len - 1];
    int j = 0;
    for (int i = 0; i < len; i++) {
    	if (f.getParameterName(i).equals(paramName))
    		continue;
     	paramNames[j] = f.getParameterName(i);
    	paramValues[j] = f.getParameterValue(i);
    	desc[j] = f.getParameterDescription(i);
    	j++;
    }
  	
  	// set up expression
  	String expression = f.getExpression("x");
    expression = expression.replace(paramName, "("+String.valueOf(paramVal)+")");
    
  	testFunction.setParameters(paramNames, paramValues, desc);
    testFunction.setExpression(expression, new String[] {"x"}); //$NON-NLS-1$ //$NON-NLS-2$
    return testFunction;
	}

	/**
	 * Gets a test function that mimics an input function but fixes some parameters.
	 * @param f a KnownFunction
	 * @param fixedParams boolean[] of fixed indices, may be null
	 * @return the test function
	 */
	private KnownFunction getTestFunction(KnownFunction f, boolean[] fixedParams) {
		if (fixedParams == null)
			return f;
		KnownFunction test = f;
		for (int i = 0; i < fixedParams.length; i++) {
			if (!fixedParams[i])
				continue;
			String paramName = f.getParameterName(i);
			double paramVal = f.getParameterValue(i);
			test = getTestFunction(test, paramName, paramVal);
		}
		return test;
	}

	/**
	 * Determines the Pearson correlation and linear fit parameter SEs.
	 *
	 * @param xd          double[]
	 * @param yd          double[]
	 * @param isLinearFit true if linear fit (sets uncertainties to slope and
	 *                    intercept SE)
	 */
	public void doLinearRegression(double[] xd, double[] yd) {
		int n = xd.length;
		// set Double.NaN defaults
		correlation = Double.NaN;
//		for (int i = 0; i < uncertainties.length; i++)
//			uncertainties[i] = Double.NaN;

		// return if less than 3 data points
		if (n < 3)
			return;

		double mean_x = xd[0];
		double mean_y = yd[0];
		for (int i = 1; i < n; i++) {
			mean_x += xd[i];
			mean_y += yd[i];
		}
		mean_x /= n;
		mean_y /= n;

		double sum_sq_x = 0;
		double sum_sq_y = 0;
		double sum_coproduct = 0;
		for (int i = 0; i < n; i++) {
			double delta_x = xd[i] - mean_x;
			double delta_y = yd[i] - mean_y;
			sum_sq_x += delta_x * delta_x;
			sum_sq_y += delta_y * delta_y;
			sum_coproduct += delta_x * delta_y;
		}
		if (sum_sq_x == 0 || sum_sq_y == 0) {
			correlation = Double.NaN;
			uncertainties = null;
			return;
		}

		double pop_sd_x = sum_sq_x / n;
		double pop_sd_y = sum_sq_y / n;
		double cov_x_y = sum_coproduct / n;
		correlation = cov_x_y * cov_x_y / (pop_sd_x * pop_sd_y);

//		if (isLinearFit) {
//			double sumSqErr = Math.max(0.0, sum_sq_y - sum_coproduct * sum_coproduct / sum_sq_x);
//			double meanSqErr = sumSqErr / (n - 2);
//			double[] sigmas = new double[2];
//			sigmas[0] = Math.sqrt(meanSqErr / sum_sq_x); // slope
//			sigmas[1] = Math.sqrt(meanSqErr * ((1.0 / n) + (mean_x * mean_x) / sum_sq_x)); // intercept
//			setUncertainties(sigmas);
			
			
//			uncertainties[0] = Math.sqrt(meanSqErr / sum_sq_x); // slope SE
//			uncertainties[1] = Math.sqrt(meanSqErr * ((1.0 / n) + (mean_x * mean_x) / sum_sq_x)); // intercept SE
//			drawer.setUncertainty(0, uncertainties[0]);
//			drawer.setUncertainty(1, uncertainties[1]);
//		}
//		else {
//			drawer.clearUncertainties();
//		}
		
	}
	
	/**
	 * Sets the uncertainties and uncertainParameters.
	 * 
	 * @param sigmas double[][] see getUncertainties(), may be null
	 */
	private void setUncertainties(double[][] sigmas) {		
		uncertainties = sigmas == null? null: sigmas[0];
		drawer.setUncertainties(sigmas);
	}

	private KnownFunction getFitFunction(FitFunctionPanel panel) {
		UserFunction f = panel.getFitFunction();
		if (f.polynomial != null) {
			f.updatePolynomial();
			return f.polynomial.clone();
		}
		return f.clone();
	}

	/**
	 * Replaces an existing fit function with a new one.
	 * 
	 * @param oldName the (localized) name of the existing fit function
	 * @param newName the (localized) new name of the function
	 * @param newFit  the new fit function
	 */
	protected void replaceFit(String oldName, String newName, KnownFunction newFit) {
		KnownFunction oldFit = fitMap.get(oldName);
		if (oldFit != null) {
			if (localFits.contains(oldFit)) {
				localFits.remove(oldFit);
				localFits.add(newFit);
			}
			refreshFitDropDown();
		}
		refreshFitMap();
	}

	/**
	 * Gets a color dialog for the plotted curve fit drawer
	 */
	protected JDialog getColorDialog() {
		if (colorDialog == null) {
			// create color dialog
			final Frame frame = JOptionPane.getFrameForComponent(this);
			final JColorChooser cc = new JColorChooser();
			cc.getSelectionModel().addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					color = cc.getColor();
					setColor(color);
					frame.repaint();
				}

			});
			colorDialog = new JDialog(frame, false);
			closeButton = new JButton();
			closeButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					colorDialog.setVisible(false);
				}

			});
			JPanel contentPane = new JPanel(new BorderLayout());
			JPanel buttonPanel = new JPanel();
			buttonPanel.add(closeButton);
			JPanel chooser = cc.getChooserPanels()[0];
			chooser.setBorder(BorderFactory.createEmptyBorder(2, 2, 12, 2));
			contentPane.add(chooser, BorderLayout.CENTER);
			contentPane.add(buttonPanel, BorderLayout.SOUTH);
			colorDialog.setContentPane(contentPane);
			colorDialog.pack();
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			int x = (dim.width - colorDialog.getWidth()) / 2;
			Point p = this.getLocationOnScreen();
			int y = Math.max(0, p.y - colorDialog.getHeight());
			colorDialog.setLocation(x, y);
		}
		return colorDialog;
	}

//__________________________ inner classes _____________________________

	/**
	 * A table to display and edit parameters.
	 */
	class ParamTable extends JTable {
		/**
		 * Constructor ParamTable
		 * 
		 * @param model
		 */
		public ParamTable(ParamTableModel model) {
			super(model);
			setGridColor(Color.blue);
			JTableHeader header = getTableHeader();
			header.setForeground(Color.blue);
			MouseAdapter listener = new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					if (OSPRuntime.isPopupTrigger(e))
						showPopup(e);
				}
			};
			this.addMouseListener(listener);
			header.addMouseListener(listener);
		}
		
		public void showPopup(MouseEvent e) {
			JPopupMenu popup = new JPopupMenu();
			JMenuItem item = new JMenuItem(ToolsRes.getString("DatasetCurveFitter.Menuitem.CopyParameters")); //$NON-NLS-1$
			item.addActionListener((ev) -> {
				selectAll();
				ActionEvent event = new ActionEvent(paramTable, ActionEvent.ACTION_PERFORMED, null);
				getActionMap().get("copy").actionPerformed(event);					
			});
			popup.add(item);
			popup.addSeparator();
			JCheckBoxMenuItem sciNotItem = new JCheckBoxMenuItem("Scientific notation"); //$NON-NLS-1$
			sciNotItem.setSelected(isSciNotation);
			spinCellEditor.field.applyDefaultPattern(isSciNotation); //$NON-NLS-1$			
			sciNotItem.addActionListener((ev) -> {
				isSciNotation = sciNotItem.isSelected();	
				spinCellEditor.field.applyDefaultPattern(isSciNotation); //$NON-NLS-1$					
				repaint();
			});
			popup.add(sciNotItem);
			FontSizer.setFonts(popup);				
			popup.show(e.getComponent(), e.getX(), e.getY() - popup.getPreferredSize().height);
		}

		@Override
		public TableCellRenderer getCellRenderer(int row, int column) {
			if (column == 1) {
				return getDefaultRenderer(getColumnClass(column));
			}
			return cellRenderer;
		}

		@Override
		public TableCellEditor getCellEditor(int row, int column) {
			if (column == 1) {
				return getDefaultEditor(getColumnClass(column));
			}
			if (Double.isNaN((Double)getValueAt(row, 2)))
				return null;
			spinCellEditor.rowNumber = row;
			
			Timer timer = new Timer(10, (e) -> {
				spinCellEditor.field.selectAll();					
			});
			timer.setRepeats(false);
			timer.start();
			return spinCellEditor;
		}

		@Override
		public void setFont(Font font) {
			super.setFont(font);
			if (cellRenderer != null) {
				Font aFont = cellRenderer.labelFont;
				aFont = aFont.deriveFont(font.getSize2D());
				cellRenderer.labelFont = aFont;
				spinCellEditor.stepSizeLabel.setFont(aFont);
				aFont = cellRenderer.fieldFont;
				aFont = aFont.deriveFont(font.getSize2D());
				cellRenderer.fieldFont = aFont;
				spinCellEditor.field.setFont(aFont);
			}
			getTableHeader().setFont(font);
			setRowHeight(font.getSize() + 4);
			TableModel model = getModel();
			if (model instanceof DefaultTableModel) {
				DefaultTableModel tm = (DefaultTableModel) model;
				tm.fireTableDataChanged();
			}
		}

	}

	/**
	 * A class to provide model data for the parameters table.
	 * Table columns: name, fixed checkbox, value with uncert 
	 */
	class ParamTableModel extends AbstractTableModel {
		@Override
		public String getColumnName(int col) {
			return (col == 0) ? ToolsRes.getString("Table.Heading.Parameter") : //$NON-NLS-1$
					(col == 1) ? ToolsRes.getString("DatasetCurveFitter.Table.Heading.FixedParam") : //$NON-NLS-1$
					ToolsRes.getString("Table.Heading.Value"); //$NON-NLS-1$
		}

		@Override
		public int getRowCount() {
			return (fit == null) ? 0 : fit.getParameterCount();
		}

		@Override
		public int getColumnCount() {
			return 3;
		}

		@Override
		public Object getValueAt(int row, int col) {
			if (col == 0) {
				return fit.getParameterName(row);
			}
			else if (col == 1) {
				boolean[] fixed = fixedParams.get(fit);
				return fixed == null || fixed.length < row + 1? false: fixed[row];
			}
			
			// if insufficient points to do fit return NaN
			if (dataset == null || 
					(autofit && fit.getParameterCount() > dataset.getValidXPoints().length))
				return Double.NaN;				

			return Double.valueOf(fit.getParameterValue(row));
		}
		
		@Override
		public void setValueAt(Object value, int row, int col) {
			if (col == 1) {
				boolean[] fixed = fixedParams.get(fit);
				if (fixed != null && fixed.length > row) {
					fixed[row] = (Boolean)value;
				}
			}
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return col > 0;
		}

		@Override
		public Class<?> getColumnClass(int c) {
			switch(c) {
				case 0:
					return String.class;
				case 1:
					return Boolean.class;
			}
			return Double.class;
		}

	}

	/**
	 * A cell renderer for the parameter table.
	 */
	class ParamCellRenderer extends JLabel implements TableCellRenderer {
		Color lightBlue = new Color(204, 204, 255);
		Color lightGray = javax.swing.UIManager.getColor("Panel.background"); //$NON-NLS-1$
		Font fieldFont = new JTextField().getFont();
		Font labelFont = getFont();

		// Constructor

		/**
		 * Constructor ParamCellRenderer
		 */
		public ParamCellRenderer() {
			super();
			setOpaque(true); // make background visible
			setBorder(BorderFactory.createEmptyBorder(2, 1, 2, 2));
		}

		private boolean notApplicable;
	
		private boolean isApplicable() {
			return !(autofit && notApplicable);
		}
		public void setNotApplicable(boolean b) {
			notApplicable = b;
		}
		
		// Returns a label for the specified cell.
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int col) {
			setHorizontalAlignment(SwingConstants.LEFT);
			setBorder(new CellBorder(new Color(240, 240, 240)));
			String tooltip = ""; //$NON-NLS-1$
			if (value instanceof String) { // parameter name string
				setFont(labelFont);
				setBackground(isSelected ? Color.LIGHT_GRAY : lightGray);
				setForeground(Color.black);
				setText(value.toString());
				if (col == 0) { // parameter name: tooltip is description
					tooltip = fit.getParameterDescription(row);
				}
			} else if (value instanceof Double) { // Double value
				setFont(fieldFont);
				setBackground(!isApplicable() ? Color.YELLOW : isSelected ? lightBlue : Color.white);
				setForeground(isSelected ? Color.red : table.isEnabled() ? Color.black : Color.gray);
				DecimalFormat format = spinCellEditor.field.format;
				format.setDecimalFormatSymbols(OSPRuntime.getDecimalFormatSymbols());
				double uncertainty = getUncertainty(row);
				String[] uncert = formatUncertainParameter((double)value, uncertainty, 0, format);
				if (Double.isNaN((Double)value)) {
					tooltip = ToolsRes.getString("DatasetCurveFitter.InsufficientData.ToolTip"); //$NON-NLS-1$//$NON-NLS-2$
				}
				else if (!autofit) {
					tooltip = ToolsRes.getString("DatasetCurveFitter.SE.Name") + " " +
							ToolsRes.getString("DatasetCurveFitter.SE.Autofit"); //$NON-NLS-1$//$NON-NLS-2$
				} 
				else {
					if (uncert != null) {
						String desc = fit.getParameterDescription(row);
						String val = fit.getParameterName(row) + " = " + uncert[1];
						tooltip = desc == null? val: desc + " " + val;
					}
					else
						tooltip = ToolsRes.getString("DatasetCurveFitter.SE.Name") + " " +
								ToolsRes.getString("DatasetCurveFitter.SE.Unknown"); //$NON-NLS-1$//$NON-NLS-2$
				}
				setText(isApplicable() ? 
						uncert != null? uncert[0]:  
						format.format(value):  
						"     ---------------");
			}
			setToolTipText(tooltip);
			return this;
		}

	}

	/**
	 * A cell editor that uses a JSpinner with a number crawler model.
	 */
	class SpinCellEditor extends AbstractCellEditor implements TableCellEditor {
		JPanel panel = new JPanel(new BorderLayout());
		SpinnerNumberCrawlerModel crawlerModel = new SpinnerNumberCrawlerModel(1);
		JSpinner spinner;
		NumberField field;
		int rowNumber;
		JLabel stepSizeLabel = new JLabel("10%"); //$NON-NLS-1$

		// Constructor.
		SpinCellEditor() {
			panel.setOpaque(false);
			spinner = new JSpinner(crawlerModel);
			spinner.setToolTipText(ToolsRes.getString("Table.Spinner.ToolTip")); //$NON-NLS-1$
			spinner.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					boolean[] fixed = fixedParams.get(fit);
					if (!fixed[rowNumber])
						setAutoFit(false);
					double val = ((Double) spinner.getValue()).doubleValue();
					field.setValue(val);
					fit.setParameterValue(rowNumber, val);
					if (fit instanceof UserFunction) {
						// get dependent parameter values from fit builder
						UserFunction f = (UserFunction) fit;
						String name = f.getName();
						FitFunctionPanel panel = (FitFunctionPanel) fitBuilder.getPanel(name);
						if (panel != null) {
							name = f.getParameterName(rowNumber);
							Parameter seed = new Parameter(name, field.getText());
							Iterator<?> it = panel.getParamEditor().evaluateDependents(seed).iterator();
							while (it.hasNext()) {
								Parameter p = (Parameter) it.next();
								// find row number, set value in fit
								for (int i = 0; i < f.getParameterCount(); i++) {
									if (f.getParameterName(i).equals(p.getName())) {
										f.setParameterValue(i, p.getValue());
										paramModel.fireTableCellUpdated(i, 1);
										break;
									}
								}
							}
							panel.getFitFunctionEditor().parametersValid = false;
							f.updateReferenceParameters();
						}
					}
					drawer.functionChanged = true;
					fit(fit);
					firePropertyChange(PROPERTY_DATASETCURVEFITTER_CHANGED, null, null); // $NON-NLS-1$
					
				}

			});
			field = new NumberField(10);
			field.applyDefaultPattern(isSciNotation); //$NON-NLS-1$					
			field.setBorder(BorderFactory.createEmptyBorder(1, 1, 0, 0));
			spinner.setBorder(BorderFactory.createEmptyBorder(0, 1, 1, 0));
			spinner.setEditor(field);
			stepSizeLabel.addMouseListener(new MouseInputAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					JPopupMenu popup = new JPopupMenu();
					ActionListener listener = new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							// set the percent delta
							double percent = Double.parseDouble(e.getActionCommand());
							crawlerModel.setPercentDelta(percent);
							crawlerModel.refreshDelta();
							stepSizeLabel.setText(e.getActionCommand() + "%"); //$NON-NLS-1$
						}

					};
					for (int i = 0; i < 3; i++) {
						String val = (i == 0) ? "10" : (i == 1) ? "1.0" : "0.1"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						JMenuItem item = new JMenuItem(val + "%"); //$NON-NLS-1$
						item.setActionCommand(val);
						item.addActionListener(listener);
						popup.add(item);
					}
					// show the popup
					popup.show(stepSizeLabel, 0, stepSizeLabel.getHeight());
				}

			});
			field.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					JComponent comp = (JComponent) e.getSource();
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						spinner.setValue(Double.valueOf(field.getValue()));
						comp.setBackground(Color.white);
						crawlerModel.refreshDelta();
					} else {
						comp.setBackground(Color.yellow);
					}
				}

			});
			panel.add(spinner, BorderLayout.CENTER);
			panel.add(stepSizeLabel, BorderLayout.EAST);
		}

		// Gets the component to be displayed while editing.
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int column) {
			spinner.setValue(value);
			crawlerModel.refreshDelta();
			return panel;
		}

		// Determines when editing starts.
		@Override
		public boolean isCellEditable(EventObject e) {
			return (e instanceof MouseEvent || e instanceof ActionEvent);
		}

		// Called when editing is completed.
		@Override
		public Object getCellEditorValue() {
			if (field.getBackground() == Color.yellow) {
				fit.setParameterValue(rowNumber, field.getValue());
				drawer.functionChanged = true;
				// no such listener?
				firePropertyChange(PROPERTY_DATASETCURVEFITTER_FIT, null, null); // $NON-NLS-1$
				field.setBackground(Color.white);
				firePropertyChange(PROPERTY_DATASETCURVEFITTER_CHANGED, null, null); // $NON-NLS-1$
			}
			return null;
		}

	}

	/**
	 * A number spinner model with a settable delta.
	 */
	class SpinnerNumberCrawlerModel extends AbstractSpinnerModel {
		double val = 0;
		double delta;
		double percentDelta = 10;

		/**
		 * Constructor SpinnerNumberCrawlerModel
		 * 
		 * @param initialDelta
		 */
		public SpinnerNumberCrawlerModel(double initialDelta) {
			delta = initialDelta;
		}

		@Override
		public Object getValue() {
			return Double.valueOf(val);
		}

		@Override
		public Object getNextValue() {
			return Double.valueOf(val + delta);
		}

		@Override
		public Object getPreviousValue() {
			return Double.valueOf(val - delta);
		}

		@Override
		public void setValue(Object value) {
			if (value != null) {
				val = ((Double) value).doubleValue();
				fireStateChanged();
			}
		}

		public void setPercentDelta(double percent) {
			percentDelta = percent;
		}

		public double getPercentDelta() {
			return percentDelta;
		}

		// refresh delta based on current value and percent
		public void refreshDelta() {
			if (val != 0) {
				delta = Math.abs(val * percentDelta / 100);
			}
		}

	}

	/**
	 * A function whose value is the total deviation squared between a multivariable
	 * function and a set of data points. This is minimized by the HessianMinimize
	 * class.
	 */
	public class MinimizeMultiVarFunction implements MultiVarFunction {
		MultiVarFunction f;
		double[] x, y; // the data
		double[] vars = new double[5];

		// Constructor
		MinimizeMultiVarFunction(MultiVarFunction f, double[] x, double[] y) {
			this.f = f;
			this.x = x;
			this.y = y;
		}

		// Evaluates the function
		@Override
		public double evaluate(double[] params) {
			System.arraycopy(params, 0, vars, 1, 4);
			double sum = 0.0;
			for (int i = 0, n = x.length; i < n; i++) {
				vars[0] = x[i];
				// evaluate the function and find deviation
				double dev = y[i] - f.evaluate(vars);
				// sum the squares of the deviations
				sum += dev * dev;
			}
			return sum;
		}

	}

	/**
	 * A function whose value is the total deviation squared between a user function
	 * and a set of data points. This function is minimized by the HessianMinimize
	 * class.
	 */
	public class MinimizeUserFunction implements MultiVarFunction {
		UserFunction f;
		double[] x, y; // the data

		// Constructor
		MinimizeUserFunction(UserFunction f, double[] x, double[] y) {
			this.f = f;
			this.x = x;
			this.y = y;
		}

		// Evaluates this function
		@Override
		public double evaluate(double[] params) {
			// set the parameter values of the user function
			for (int i = 0; i < params.length; i++) {
				f.setParameterValue(i, params[i]);
			}
			double sum = 0.0;
			for (int i = 0; i < x.length; i++) {
				// evaluate the user function and find deviation
				double dev = y[i] - f.evaluate(x[i]);
				// sum the squares of the deviations
				sum += dev * dev;
			}
			return sum;
		}

	}

	/**
	 * A JTextField that accepts only numbers.
	 */
	static class NumberField extends JTextField {
		// instance fields
		protected DecimalFormat format = (DecimalFormat) NumberFormat.getInstance();
		protected double prevValue;
		protected String pattern = "0"; //$NON-NLS-1$
		protected int preferredWidth;

		/**
		 * Constructor NumberField
		 * 
		 * @param columns
		 */
		public NumberField(int columns) {
			super(columns);
			setForeground(Color.black);
		}

		public double getValue() {
			if (getText().equals(format.format(prevValue))) {
				return prevValue;
			}
			double retValue;
			try {
				retValue = format.parse(getText()).doubleValue();
			} catch (ParseException e) {
				Toolkit.getDefaultToolkit().beep();
				setValue(prevValue);
				return prevValue;
			}
			return retValue;
		}

		public void setValue(double value) {
			if (!isVisible()) {
				return;
			}
			format.setDecimalFormatSymbols(OSPRuntime.getDecimalFormatSymbols());
			setText(format.format(value));
			prevValue = value;
		}

		public void applyPattern(String pattern) {
			if (format instanceof DecimalFormat) {
				try {
					// catch occasional exceptions thrown when opening a trk file...
					format.applyPattern(pattern);
					this.pattern = pattern;
				} catch (Exception e) {
				}
			}
		}

		public void applyDefaultPattern(boolean sciNotation) {
			if (sciNotation)
				applyPattern("0.000E0"); //$NON-NLS-1$
			else
				applyPattern("0.###"); //$NON-NLS-1$					
		}

		public String getPattern() {
			return pattern;
		}

		public NumberFormat getFormat() {
			return format;
		}

		protected void refreshPreferredWidth() {
			// determine preferred width of field
			Rectangle2D rect = getFont().getStringBounds(getText(), OSPRuntime.frc);
			preferredWidth = (int) rect.getWidth() + 8;
		}

		@Override
		public Dimension getPreferredSize() {
			Dimension dim = super.getPreferredSize();
			dim.width = Math.max(dim.width, preferredWidth);
			return dim;
		}
		
	}

//_______________________________ static methods _________________________________

	/**
	 * Sets the default fit functions. Instances of DatasetCurveFitter instantiated
	 * AFTER this call will make these fits available to the user.
	 * 
	 * @param functions the fit functions
	 */
	public static void setDefaultFitFunctions(ArrayList<KnownFunction> functions) {
		if (functions != null) {
			defaultFits = functions;
		}
	}

	public void getFits(Map<String, KnownFunction> fits, ArrayList<String> fitnames) {
		for (int i = 0; i < fitDropDown.getItemCount(); i++) {
			String name = fitDropDown.getItemAt(i).toString();
			if (!fitnames.contains(name)) {
				fitnames.add(name);
				fits.put(name, fitMap.get(name));
			}
		}
	}

	public String[] getFitNames() {
		int n = fitDropDown.getItemCount();
		String[] names = new String[n];
		for (int i = 0; i < n; i++) {
			names[i] = fitDropDown.getItemAt(i).toString();
		}
		return names;
	}

	public void setSelectedItem(String fitName) {
		fitDropDown.setSelectedItem(fitName);
	}

	public void setText(String text) {
		eqnField.setText(text);
	}

	boolean hasFit(String name) {
		return fitMap.containsKey(name);
	}

	void notifyTabRemoved() {
		fitBuilder.removePropertyChangeListener(fitListener);
	}

	public void setFitVisible(boolean vis) {
		getDrawer().setEnabled(vis);
//		cellRenderer.setNotApplicable(!fittable);
	}
}

/*
 * Open Source Physics software is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.
 * 
 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be
 * released under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston MA 02111-1307 USA or view the license online at
 * http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
