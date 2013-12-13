/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;
import java.text.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import org.opensourcephysics.display.*;
import org.opensourcephysics.numerics.*;

/**
 * A panel that displays and controls functional curve fits to a Dataset.
 *
 * @author Douglas Brown
 * @version 1.0
 */
@SuppressWarnings("serial")
public class DatasetCurveFitter extends JPanel {
	
  // static fields
  /** defaultFits are available in every instance */
  static ArrayList<KnownFunction> defaultFits = new ArrayList<KnownFunction>();
  static JFileChooser chooser;
  static NumberFormat SEFormat = NumberFormat.getInstance();

	// instance fields
  /** allFitsMap maps localized names to all default, added and fitBuilder fits */
  Map<String, KnownFunction> allFitsMap = new TreeMap<String, KnownFunction>();
  /** definedFits contains default and added fits */
  ArrayList<KnownFunction> definedFits = new ArrayList<KnownFunction>();
  /** addedFits contains default and added fits */
  ArrayList<KnownFunction> addedFits = new ArrayList<KnownFunction>();
  PropertyChangeListener fitListener;
  Dataset dataset;               // the data to be fit
  KnownFunction fit;             // the function to fit to the data
  HessianMinimize hessian = new HessianMinimize();
  LevenbergMarquardt levmar = new LevenbergMarquardt();
  FunctionDrawer drawer;
  Color color = Color.MAGENTA;
  JButton colorButton, closeButton;
  JCheckBox autofitCheckBox;
  String[] fitNames;
  JLabel fitLabel, eqnLabel, rmsLabel;
  JToolBar fitBar, eqnBar, rmsBar;
  JComboBox fitDropDown;
  JTextField eqnField;
  NumberField rmsField;
  ParamTableModel paramModel;
  JTable paramTable;
  ParamCellRenderer cellRenderer;
  SpinCellEditor spinCellEditor; // uses number-crawler spinner
  int fitNumber = 1;
  JButton fitBuilderButton;
  boolean refreshing = false, isActive;
  JSplitPane splitPane;
  JDialog colorDialog;
  int fontLevel;
  FitBuilder fitBuilder;
  double correlation = Double.NaN;
  double[] uncertainties = new double[2];
  DataToolTab tab;
  
  static {
    defaultFits.add(new KnownPolynomial(new double[] {0, 0}));
    defaultFits.add(new KnownPolynomial(new double[] {0, 0, 0}));
    defaultFits.add(new KnownPolynomial(new double[] {0, 0, 0, 0}));
    UserFunction f = new UserFunction("Gaussian"); //$NON-NLS-1$
    f.setParameters(new String[] {"A", "B", "C"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    		new double[] {1, 0, 1});
    f.setExpression("A*exp(-(x-B)^2/C)", new String[] {"x"}); //$NON-NLS-1$ //$NON-NLS-2$
    defaultFits.add(f);
    f = new UserFunction("Exponential"); //$NON-NLS-1$
    f.setParameters(new String[] {"A", "B", "C"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    		new double[] {1, 1, 0});
    f.setExpression("A*exp(-B*(x-C))", new String[] {"x"}); //$NON-NLS-1$ //$NON-NLS-2$
    defaultFits.add(f);
    f = new UserFunction("Sinusoid"); //$NON-NLS-1$
    f.setParameters(new String[] {"A", "B", "C"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    		new double[] {1, 1, 0});
    f.setExpression("A*sin(B*x+C)", new String[] {"x"}); //$NON-NLS-1$ //$NON-NLS-2$
    defaultFits.add(f);
  }

  /**
   * Constructs a DatasetCurveFitter for the specified Dataset.
   *
   * @param data the dataset
   * @param builder the FitBuilder used for constructing custom fits
   */
  public DatasetCurveFitter(Dataset data, FitBuilder builder) {
    dataset = data;
    fitBuilder = builder;
    createGUI();
    fit(fit);
  }

  /**
   * Gets the function drawer.
   *
   * @return the drawer
   */
  public FunctionDrawer getDrawer() {
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
   * @param data the dataset
   */
  public void setData(Dataset data) {
    dataset = data;
    if (isActive)
    	fit(fit);
    if (dataset != null) {
	  	String var = dataset.getXColumnName();
	    var = TeXParser.removeSubscripting(var);
	    fitBuilder.setDefaultVariables(new String[] {var});
	    if (!isActive) { // if active, regression done in fit method
	      double[] x = dataset.getValidXPoints();
	      double[] y = dataset.getValidYPoints();
	      doLinearRegression(x, y, false);
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
    if(drawer!=null) {
      drawer.setColor(newColor);
      LookAndFeel currentLF = UIManager.getLookAndFeel();
      boolean nimbus = currentLF.getClass().getName().indexOf("Nimbus")>-1; //$NON-NLS-1$
      if(nimbus) {
        colorButton.setIcon(new ColorIcon(color, 12, DataTool.buttonHeight-8));
      } else {
        colorButton.setBackground(color);
      }
      firePropertyChange("changed", null, null);                            //$NON-NLS-1$
    }
  }

  /**
   * Sets the autofit flag.
   *
   * @param auto true to autofit
   */
  public void setAutofit(boolean auto) {
  	if (auto && !autofitCheckBox.isSelected())
  		autofitCheckBox.doClick(0);
  	else if (!auto && autofitCheckBox.isSelected())
  		autofitCheckBox.doClick(0);
  }

  /**
   * Sets the active flag.
   *
   * @param active true
   */
  public void setActive(boolean active) {
  	isActive = active;
  	if (active) fit(fit);
  }

  /**
   * Fits a fit function to the current data.
   *
   * @param fit the function to fit
   * @return the rms deviation
   */
  public double fit(KnownFunction fit) {
    if(drawer==null) {
      selectFit((String) fitDropDown.getSelectedItem());
    }
    if((fit==null)||(dataset==null)) {
      if(fit instanceof UserFunction) {
        eqnField.setText("y = "+                     //$NON-NLS-1$
          ((UserFunction) fit).getFullExpression(new String[] {"x"})); //$NON-NLS-1$
      }
      else {
        eqnField.setText("y = "+ fit.getExpression("x")); //$NON-NLS-1$ //$NON-NLS-2$
      }
      autofitCheckBox.setEnabled(false);
      spinCellEditor.stopCellEditing();
      paramTable.setEnabled(false);
      rmsField.setText("");  //$NON-NLS-1$
      return Double.NaN;
    }
    autofitCheckBox.setEnabled(true);
    paramTable.setEnabled(true);
    double[] x = dataset.getValidXPoints();
    double[] y = dataset.getValidYPoints();
    double devSq = 0;
    // autofit if checkbox is selected
    double[] prevParams = null;
    // get deviation before fitting
    double prevDevSq = getDevSquared(fit, x, y);
    boolean isLinearFit = false;
    if(autofitCheckBox.isSelected()) {
      if(fit instanceof KnownPolynomial) {
        KnownPolynomial poly = (KnownPolynomial) fit;
        poly.fitData(x, y);
        isLinearFit = poly.degree()==1;
      } 
      else if(fit instanceof UserFunction) {
        // use HessianMinimize to autofit user function 
        UserFunction f = (UserFunction) fit;
        double[] params = new double[f.getParameterCount()];
        // can't autofit if no parameters
        if(params.length>0) {
          MinimizeUserFunction minFunc = new MinimizeUserFunction(f, x, y);
          prevParams = new double[params.length];
          for(int i = 0; i<params.length; i++) {
            params[i] = prevParams[i] = f.getParameterValue(i);
          }
          double tol = 1.0E-6;
          int iterations = 20;
          hessian.minimize(minFunc, params, iterations, tol);
          // get deviation after minimizing
          devSq = getDevSquared(fit, x, y);
          // restore parameters and try Levenberg-Marquardt if Hessian fit is worse
          if(devSq>prevDevSq) {
            for(int i = 0; i<prevParams.length; i++) {
              f.setParameterValue(i, prevParams[i]);
            }
            levmar.minimize(minFunc, params, iterations, tol);
            // get deviation after minimizing
            devSq = getDevSquared(fit, x, y);
          }
          // restore parameters and deviation if new fit is worse
          if(devSq>prevDevSq) {
            for(int i = 0; i<prevParams.length; i++) {
              f.setParameterValue(i, prevParams[i]);
            }
            devSq = prevDevSq;
            autofitCheckBox.setSelected(false);
            Toolkit.getDefaultToolkit().beep();
          }
        }
      }
      drawer.functionChanged = true;
      paramTable.repaint();
    }
  	doLinearRegression(x, y, isLinearFit);
    if(devSq==0) {
      devSq = getDevSquared(fit, x, y);
    }
    double rmsDev = Math.sqrt(devSq/x.length);
    rmsField.setForeground(eqnField.getForeground());
    if (x.length==0 || y.length==0) {
        rmsField.setText(ToolsRes.getString("DatasetCurveFitter.RMSField.NoData")); //$NON-NLS-1$
        rmsField.setForeground(Color.RED);
    }
    else {
    	rmsField.applyPattern("0.000E0"); //$NON-NLS-1$
      rmsField.setValue(rmsDev);
    }
    refreshStatusBar();
    firePropertyChange("fit", null, null); //$NON-NLS-1$
    return rmsDev;
  }

  /**
   * Adds a fit function. UserFunctions can optionally be added to the fit builder.
   *
   * @param f the fit function to add
   * @param addToFitBuilder true to add a UserFunction to the fit builder
   */
  public void addFitFunction(KnownFunction f, boolean addToFitBuilder) {
  	// check for duplicates
  	KnownFunction existing = allFitsMap.get(f.getName());
  	if (existing != null) {
  		if (existing.getExpression("x").equals(f.getExpression("x"))) { //$NON-NLS-1$ //$NON-NLS-2$
  			return; // duplicate name and expression, so ignore
  		}
  		else if (f instanceof UserFunction){ // different expression, so modify name
        ((UserFunction)f).setName(fitBuilder.getUniqueName(f.getName()));		
  		}
  		else { // can't modify name, so ignore
  			return;
  		}
  	}
    if (addToFitBuilder) {
    	UserFunction userFunc = (UserFunction)f;
      UserFunctionEditor editor = new UserFunctionEditor();
      editor.setMainFunctions(new UserFunction[] {userFunc});
      FitFunctionPanel panel = new FitFunctionPanel(editor);
      if (dataset != null) {
      	String var = dataset.getXColumnName();
        String[] vars = new String[] {TeXParser.removeSubscripting(var)};
      	editor.setDefaultVariables(vars);
      }
      fitBuilder.addPanel(userFunc.getName(), panel);    	
    }
    else {
      addedFits.add(f);
      refreshFits();
    }
  }

  /**
   * Gets the selected fit name.
   *
   * @return the name of the fit
   */
  public String getSelectedFitName() {
    for(Iterator<String> it = allFitsMap.keySet().iterator(); it.hasNext(); ) {
      String name = it.next().toString();
      if(allFitsMap.get(name)==fit) {
        return name;
      }
    }
    return null;
  }

  /**
   * Refreshes the parent tab's status bar
   */
  public void refreshStatusBar() {
  	if (tab!=null && tab.statsCheckbox.isSelected())
  		tab.refreshStatusBar();
  }

  /**
   * Gets the estimated uncertainty (standard error or other) of a best fit parameter.
   * Returns Double.NaN if uncertainty is unknown or is not best fit.
   * 
   * @param paramIndex the parameter index
   * @return the estimated uncertainty in the parameter
   */
  public double getUncertainty(int paramIndex) {
  	if (paramIndex<uncertainties.length && autofitCheckBox.isSelected()) {
  		return uncertainties[paramIndex];
  	}
  	return Double.NaN;
  }

  /**
   * Returns a string of the uncertainty with appropriate formatting.
   *
   * @param paramIndex the parameter index
   * @return the uncertainty string
   */
  public String getUncertaintyString(int paramIndex) {
  	double uncertainty = getUncertainty(paramIndex);
  	if (Double.isNaN(uncertainty)) return null;
    if(SEFormat instanceof DecimalFormat) {
    	DecimalFormat format = (DecimalFormat) SEFormat;
    	if (uncertainty<0.1) format.applyPattern("0.0E0"); //$NON-NLS-1$
    	else if (uncertainty<1) format.applyPattern("0.00"); //$NON-NLS-1$
    	else if (uncertainty<10) format.applyPattern("0.0"); //$NON-NLS-1$
    	else if (uncertainty<100) format.applyPattern("0"); //$NON-NLS-1$
    	else format.applyPattern("0.0E0"); //$NON-NLS-1$
    }
  	return "± "+SEFormat.format(uncertainty); //$NON-NLS-1$
  }

  /**
   * Gets the selected fit parameters.
   *
   * @return a map of parameter names to values
   */
  public Map<String, Double> getSelectedFitParameters() {
    return null;
  }

  public Dimension getMinimumSize() {
  	Dimension dim = fitBar.getPreferredSize();
  	dim.height += eqnBar.getPreferredSize().height;
  	dim.height += rmsBar.getPreferredSize().height+1;
  	return dim;
  }

  // _______________________ protected & private methods __________________________

  /**
   * Creates the GUI.
   */
  protected void createGUI() {
    setLayout(new BorderLayout());
    splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitPane.setResizeWeight(1);
    splitPane.setDividerSize(4);
    // create autofit checkbox
    autofitCheckBox = new JCheckBox("", true); //$NON-NLS-1$
    autofitCheckBox.setSelected(true);
    autofitCheckBox.setOpaque(false);
    autofitCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        spinCellEditor.stopCellEditing();
        paramTable.clearSelection();
        fit(fit);
        firePropertyChange("changed", null, null); //$NON-NLS-1$
      }

    });
    // initialize fits
    refreshFits();
    fitBuilder.addForbiddenNames(fitNames);
    // create labels
    fitLabel = new JLabel(ToolsRes.getString("DatasetCurveFitter.Label.FitName")); //$NON-NLS-1$
    fitLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
    eqnLabel = new JLabel(ToolsRes.getString("DatasetCurveFitter.Label.Equation")); //$NON-NLS-1$
    eqnLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
    rmsLabel = new JLabel();
    rmsLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
    fitDropDown = new JComboBox(fitNames) {
      public Dimension getPreferredSize() {
        Dimension dim = super.getPreferredSize();
        dim.height = DataTool.buttonHeight-2;
        return dim;
      }

    };
    fitDropDown.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if(!fitDropDown.getSelectedItem().equals(getFitName())) {
          firePropertyChange("changed", null, null); //$NON-NLS-1$
        }
        selectFit((String) fitDropDown.getSelectedItem());
      }

    });
    // create equation field
    eqnField = new JTextField() {
      public Dimension getPreferredSize() {
        Dimension dim = super.getPreferredSize();
        dim.height = DataTool.buttonHeight-2;
        return dim;
      }

    };
    eqnField.setEditable(false);
    eqnField.setEnabled(true);
    eqnField.setBackground(Color.white);
    eqnField.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        // create clone or open user function if double-clicked
        if(e.getClickCount() == 2) {
          String name = fitDropDown.getSelectedItem().toString();
          if (fitBuilder.getPanelNames().contains(name)) {
          	fitBuilder.setSelectedPanel(name);
          }
          else {
	        	UserFunction uf = createClone(fit, name);
	          UserFunctionEditor editor = new UserFunctionEditor();
	          editor.setMainFunctions(new UserFunction[] {uf});
	          FitFunctionPanel panel = new FitFunctionPanel(editor);
	          fitBuilder.addPanel(uf.getName(), panel);
	      		fitDropDown.setSelectedItem(uf.getName());
          }
          fitBuilder.setVisible(true);
        }
      }
    });
    // create dataBuilder button
    colorButton = DataTool.createButton(""); //$NON-NLS-1$
    colorButton.setToolTipText(ToolsRes.getString("DatasetCurveFitter.Button.Color.Tooltip")); //$NON-NLS-1$
    colorButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JDialog dialog = getColorDialog();
        closeButton.setText(ToolsRes.getString("Button.OK"));                                  //$NON-NLS-1$
        dialog.setTitle(ToolsRes.getString("DatasetCurveFitter.Dialog.Color.Title"));          //$NON-NLS-1$
        dialog.setVisible(true);
      }

    });
    // create rms field
    rmsField = new NumberField(6) {
      public Dimension getPreferredSize() {
        Dimension dim = super.getPreferredSize();
        dim.height = DataTool.buttonHeight-2;
        return dim;
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
      public void mousePressed(MouseEvent e) {
        // clear selection if pressed on the name column
        if(paramTable.getSelectedColumn()==0) {
          paramTable.clearSelection();
        }
      }

    });
    JScrollPane scroller = new JScrollPane(paramTable) {
    	public Dimension getMinimumSize() {
    		Dimension dim = spinCellEditor.spinner.getPreferredSize();
    		dim.width += cellRenderer.fieldFont.getSize()*7;
    		return dim;
    	}
    };
    splitPane.setRightComponent(scroller);
    add(splitPane, BorderLayout.CENTER);
    // create fit builder button
    fitBuilderButton = DataTool.createButton(ToolsRes.getString("DatasetCurveFitter.Button.Define.Text")); //$NON-NLS-1$
    fitBuilderButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {      	
        for (String next: fitBuilder.getPanelNames()) {
          FitFunctionPanel panel = (FitFunctionPanel)fitBuilder.getPanel(next);
        	KnownFunction f = getFitFunction(panel);
          allFitsMap.put(next, f);
          fitDropDown.addItem(next);
      	}   	      	
        autofitCheckBox.setSelected(false);
        String fitName = getFitName();
        if (fit instanceof UserFunction 
        		&& fitName!=null && fitBuilder.getPanelNames().contains(fitName)) {
        	fitBuilder.setSelectedPanel(getFitName());
        } 
        else if (fitBuilder.getSelectedName()!=null) {
          fitDropDown.setSelectedItem(fitBuilder.getSelectedName());
        }
        fitBuilder.setVisible(true);
      }

    });
    // create fit listener
    fitListener = new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        if(refreshing) {
          return;
        }
        String prop = e.getPropertyName();
        if(!prop.equals("function")&&!prop.equals("panel")) { //$NON-NLS-1$ //$NON-NLS-2$
          return;
        }
        if (prop.equals("panel")) { // fit panel selected, added or deleted //$NON-NLS-1$
        	if (e.getNewValue() != null) { // panel selected or added
	        	UserFunction func = getFitFunction((FitFunctionPanel)e.getNewValue());
	        	String name = func.getName();
	        	if (!allFitsMap.keySet().contains(name)) {
		          allFitsMap.put(name, func);
		          fitDropDown.addItem(name);
	        	}
	        	if (fitBuilder.isVisible()) {
	        		fitDropDown.setSelectedItem(name);  
	        	}
        	}
          if (e.getOldValue() != null) { // panel may have been deleted
          	UserFunction func = getFitFunction((FitFunctionPanel)e.getOldValue());
          	String name = func.getName();
          	if (!fitBuilder.getPanelNames().contains(name)) {
	            allFitsMap.remove(name);
	            fitDropDown.removeItem(name);
          	}
          }
        }
        firePropertyChange("changed", null, null);            //$NON-NLS-1$
        String name = fitBuilder.getSelectedName();
        FitFunctionPanel panel = (FitFunctionPanel)fitBuilder.getPanel(name);
        KnownFunction fit = DatasetCurveFitter.this.fit;
        if (panel != null) {
          fit = getFitFunction(panel);
          allFitsMap.put(name, fit);
        }
        if(prop.equals("function")) {                       //$NON-NLS-1$
          String prevName = (String) e.getOldValue();
          if(prevName!=null) {                              // fit name has changed
            allFitsMap.remove(prevName);
            allFitsMap.put(name, fit);
            for(int i = 0; i<fitDropDown.getItemCount(); i++) {
              Object obj = fitDropDown.getItemAt(i);
              if(obj.equals(prevName)) {
                fitDropDown.insertItemAt(name, i);
                fitDropDown.removeItem(prevName);
                break;
              }
            }
          }
          if(panel != null && !fitDropDown.getSelectedItem().equals(name)) {
            fitDropDown.setSelectedItem(name);
          }
        }
        refreshGUI();
      }
    };
    // add existing user functions in fitBuilder, if any
    for (String next: fitBuilder.getPanelNames()) {
      FitFunctionPanel panel = (FitFunctionPanel)fitBuilder.getPanel(next);
    	KnownFunction f = getFitFunction(panel);
      allFitsMap.put(next, f);
      fitDropDown.addItem(next);
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
    eqnPanel.add(eqnBar, BorderLayout.NORTH);
    JPanel rmsPanel = new JPanel(new BorderLayout());
    eqnPanel.add(rmsPanel, BorderLayout.CENTER);
    rmsBar = new JToolBar();
    rmsBar.setFloatable(false);
    rmsBar.setBorder(BorderFactory.createEtchedBorder());
    rmsBar.add(autofitCheckBox);
    rmsBar.addSeparator();
    rmsBar.add(rmsLabel);
    rmsBar.add(rmsField);
    rmsPanel.add(rmsBar, BorderLayout.NORTH);
    refreshGUI();
  }

  /**
   * Refreshes the GUI.
   */
  protected void refreshGUI() {
    autofitCheckBox.setText(ToolsRes.getString("Checkbox.Autofit.Label"));                           //$NON-NLS-1$
    rmsLabel.setText(ToolsRes.getString("DatasetCurveFitter.Label.RMSDeviation")); //$NON-NLS-1$
    fitBuilderButton.setText(ToolsRes.getString("DatasetCurveFitter.Button.Define.Text"));           //$NON-NLS-1$
    fitBuilderButton.setToolTipText(ToolsRes.getString("DatasetCurveFitter.Button.Define.Tooltip")); //$NON-NLS-1$
//    paramTable.tableChanged(null);
    int n = fitDropDown.getItemCount();
	  int k = n-fitNames.length; // last k items are fitBuilder fits
    refreshFits();
    Object[] list = new Object[fitNames.length+k];
    for (int i = 0; i < list.length; i++) {
    	if (i < fitNames.length) list[i] = fitNames[i];
    	else list[i] = fitDropDown.getItemAt(i+n-fitNames.length-k);
    }
    DefaultComboBoxModel model = new DefaultComboBoxModel(list);
    int i = fitDropDown.getSelectedIndex();
    fitDropDown.setModel(model);
    fitDropDown.setSelectedIndex(i);
    LookAndFeel currentLF = UIManager.getLookAndFeel();
    boolean nimbus = currentLF.getClass().getName().indexOf("Nimbus")>-1; //$NON-NLS-1$
    if(nimbus) {
      colorButton.setIcon(new ColorIcon(color, 12, DataTool.buttonHeight-8));
    } else {
      colorButton.setBackground(color);
    }
  }
  
  /**
   * Refreshes the fit names.
   */
  protected void refreshFits() {
    definedFits.clear();
    for (KnownFunction f: defaultFits) definedFits.add(f);
    for (KnownFunction f: addedFits) definedFits.add(f);
  	fitNames = new String[definedFits.size()];
  	for (int i = 0; i < fitNames.length; i++) {
  		fitNames[i] = definedFits.get(i).getName();
  		// substitute localized name, if any
  		String s = ToolsRes.getString("Function."+fitNames[i]+".Name"); //$NON-NLS-1$ //$NON-NLS-2$
  		if (!s.startsWith("!")) fitNames[i] = s; //$NON-NLS-1$  			
  		allFitsMap.put(fitNames[i], definedFits.get(i));
  	}
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
   * @param row the row number
   * @param value the value
   */
  protected void setParameterValue(int row, double value) {
    if(row<fit.getParameterCount()) {
      fit.setParameterValue(row, value);
    }
  }

  /**
   * Selects a named fit.
   * @param name the name of the fit function
   */
  protected void selectFit(String name) {
    fit = allFitsMap.get(name);
    if(fit!=null) {
      FunctionDrawer prev = drawer;
      drawer = new FunctionDrawer(fit);
      drawer.setColor(color);
      paramTable.tableChanged(null);
      // construct equation string
      String depVar = (dataset==null)? "y":                                           //$NON-NLS-1$
    	  TeXParser.removeSubscripting(dataset.getColumnName(1));
      String indepVar = (dataset==null)? "x":                                         //$NON-NLS-1$
    	  TeXParser.removeSubscripting(dataset.getColumnName(0));
      if(fit instanceof UserFunction) {
        eqnField.setText(depVar+" = "+                    //$NON-NLS-1$
          ((UserFunction)fit).getFullExpression(new String[] {indepVar}));
      }
      else {
      	eqnField.setText(depVar+" = "+fit.getExpression(indepVar)); //$NON-NLS-1$
      }
      firePropertyChange("drawer", prev, drawer);                 //$NON-NLS-1$
      if (isActive)
      	fit(fit);
      if(fit instanceof UserFunction && fitBuilder.isVisible()) {
        UserFunction f = (UserFunction) fit;
        fitBuilder.setSelectedPanel(f.getName());
      }
      revalidate();
    }
  }

  protected UserFunction createClone(KnownFunction f, String name) {
    String var = (dataset==null)? "x":                                         //$NON-NLS-1$
  	  TeXParser.removeSubscripting(dataset.getColumnName(0));
		f.getExpression(var);
		UserFunction uf = null;
		if (f instanceof UserFunction)
			uf = ((UserFunction)f).clone();
		else {
			uf = new UserFunction(f.getName());
			String[] params = new String[f.getParameterCount()];
			double[] values = new double[f.getParameterCount()];
			for (int i = 0; i < params.length; i++) {
				params[i] = f.getParameterName(i);
				values[i] = f.getParameterValue(i);
			}
			uf.setParameters(params, values);
			uf.setExpression(f.getExpression(var), new String[] {var});
		}
		// add digit to end of name
		int n = 1;
		try {
			String number = name.substring(name.length()-1);
			n = Integer.parseInt(number)+1;
			name = name.substring(0, name.length()-1);
		} catch (Exception ex) {}
		// make a set of existing fit names
  	Set<String> names = new HashSet<String>();
		for (int i = 0; i < fitDropDown.getItemCount(); i++) {
			names.add(fitDropDown.getItemAt(i).toString());
		}
		// increment digit at end of name if necessary
		try {
			while (names.contains(name+n)) {
				n++;
			}
		} catch (Exception ex) {}
		uf.setName(name+n);
    return uf;    	
  }
  
  /**
   * Gets the total deviation squared between function and data
   */
  private double getDevSquared(Function f, double[] x, double[] y) {
    double total = 0;
    for(int i = 0; i<x.length; i++) {
      double next = f.evaluate(x[i]);
      double dev = (next-y[i]);
      total += dev*dev;
    }
    return total;
  }
  
  /**
   * Determines the Pearson correlation and linear fit parameter SEs.
   *
   * @param xd double[]
   * @param yd double[]
   * @param isLinearFit true if linear fit (sets uncertainties to slope and intercept SE)
   */
  public void doLinearRegression(double[] xd, double[] yd, boolean isLinearFit) {	
  	int n = xd.length;
  	
  	// set Double.NaN defaults
  	correlation = Double.NaN;
  	for (int i=0; i< uncertainties.length; i++)
  		uncertainties[i] = Double.NaN;
  	
    // return if less than 3 data points
    if (n<3)  return;
    
    double mean_x = xd[0];
    double mean_y = yd[0];    
    for(int i=1; i<n; i++){
      mean_x += xd[i];
      mean_y += yd[i];
	  }
    mean_x /= n;
    mean_y /= n;

    double sum_sq_x = 0;
    double sum_sq_y = 0;
    double sum_coproduct = 0;
    for(int i=0; i<n; i++){
		  double delta_x = xd[i]-mean_x;
		  double delta_y = yd[i]-mean_y;
		  sum_sq_x += delta_x*delta_x;
		  sum_sq_y += delta_y*delta_y;
		  sum_coproduct += delta_x*delta_y;
		}
    if (sum_sq_x==0 || sum_sq_y==0) {
    	correlation = Double.NaN;
    	for (int i=0; i< uncertainties.length; i++)
    		uncertainties[i] = Double.NaN;
    	return;
    }
    
    double pop_sd_x = sum_sq_x/n;
    double pop_sd_y = sum_sq_y/n;
    double cov_x_y = sum_coproduct/n;
    correlation = cov_x_y*cov_x_y/(pop_sd_x*pop_sd_y);   
    
    if (isLinearFit) {
      double sumSqErr =  Math.max(0.0, sum_sq_y - sum_coproduct * sum_coproduct / sum_sq_x);
      double meanSqErr = sumSqErr/(n-2);
      uncertainties[0] = Math.sqrt(meanSqErr / sum_sq_x); // slope SE
      uncertainties[1] = Math.sqrt(meanSqErr * ((1.0/n) + (mean_x*mean_x) / sum_sq_x)); // intercept SE
    }
  }
  
  private UserFunction getFitFunction(FitFunctionPanel panel) {
  	return panel.getFitFunction().clone();
  }
  
  /**
   * Gets the name of the current fit
   */
  private String getFitName() {
    for(Iterator<String> it = allFitsMap.keySet().iterator(); it.hasNext(); ) {
      Object key = it.next();
      if(fit==allFitsMap.get(key)) {
        return key.toString();
      }
    }
    return null;
  }

  protected JDialog getColorDialog() {
    if(colorDialog==null) {
      // create color dialog
      final Frame frame = JOptionPane.getFrameForComponent(this);
      final JColorChooser cc = new JColorChooser();
      cc.getSelectionModel().addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          color = cc.getColor();
          setColor(color);
          frame.repaint();
        }

      });
      colorDialog = new JDialog(frame, false);
      closeButton = new JButton();
      closeButton.addActionListener(new ActionListener() {
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
      int x = (dim.width-colorDialog.getWidth())/2;
      Point p = this.getLocationOnScreen();
      int y = Math.max(0, p.y-colorDialog.getHeight());
      colorDialog.setLocation(x, y);
    }
    return colorDialog;
  }

  // _______________________ inner classes __________________________
  
//    /**
//     * Inner fit builder class.
//     */
//  class FitBuilder extends FunctionTool {
//  	
//  	protected FitBuilder() {
//  		super(DatasetCurveFitter.this, new Component[] {loadButton, saveButton,
//  				 new JToolBar.Separator(), newFitButton, cloneFitButton, deleteFitButton});
//  	}
//  	
//  	public void refreshDropdown(String name) {
//	  	deleteFitButton.setEnabled(!getPanelNames().isEmpty());
//	  	if (getPanelNames().isEmpty()) {
//		  	String label = ToolsRes.getString("FitFunctionPanel.Label"); //$NON-NLS-1$
//	      dropdownLabel.setText(label+":"); //$NON-NLS-1$
//	  	}
//	  	super.refreshDropdown(name);
//	  }  			
//  	
//  	protected void refreshGUI() {
//	  	super.refreshGUI();
//	  	setTitle(ToolsRes.getString("DatasetCurveFitter.FitBuilder.Title")); //$NON-NLS-1$
//	  	if (getPanelNames().isEmpty()) {
//		  	String label = ToolsRes.getString("FitFunctionPanel.Label"); //$NON-NLS-1$
//	      dropdownLabel.setText(label+":"); //$NON-NLS-1$
//	  	}
//			saveButton.setEnabled(this.getPanelNames().size()>0);
//			loadButton.setToolTipText(ToolsRes.getString("DatasetCurveFitter.FitBuilder.Button.Load.Tooltip")); //$NON-NLS-1$
//			saveButton.setToolTipText(ToolsRes.getString("DatasetCurveFitter.FitBuilder.Button.Save.Tooltip")); //$NON-NLS-1$
//		} 
//  	
//  	protected boolean chooseFitFunctions(XMLControl control, String description) {
//      ListChooser listChooser = new ListChooser(
//          ToolsRes.getString("DatasetCurveFitter.FitBuilder."+description+".Title"), //$NON-NLS-1$ //$NON-NLS-2$
//          ToolsRes.getString("DatasetCurveFitter.FitBuilder."+description+".Message"), //$NON-NLS-1$ //$NON-NLS-2$
//          fitBuilder);
//      // choose the elements and load the function tool
//      ArrayList<XMLControl> originals = new ArrayList<XMLControl>();
//      ArrayList<XMLControl> choices = new ArrayList<XMLControl>();
//      ArrayList<String> names = new ArrayList<String>();
//      ArrayList<String> expressions = new ArrayList<String>();
//      for (Object next: control.getPropertyContent()) {
//      	if (next instanceof XMLProperty) {
//      		XMLProperty prop = (XMLProperty)next;
//          for (Object obj: prop.getPropertyContent()) {
//          	if (obj instanceof XMLProperty) {
//          		XMLProperty f = (XMLProperty)obj;
//          		XMLControl function = f.getChildControls()[0];
//          		originals.add(function);
//          		choices.add(function);
//          		names.add(function.getString("name")); //$NON-NLS-1$
//          		String desc = function.getString("description"); //$NON-NLS-1$
//          		expressions.add(desc);
//          	}
//          }
//      	}            	
//      }
//      // select all by default
//      boolean[] selected = new boolean[choices.size()];
//      for (int i = 0; i<selected.length; i++) {
//      	selected[i] = true;
//      }
//      if (listChooser.choose(choices, names, expressions, selected)) {
//        // compare choices with originals and remove unwanted object content
//        for (XMLControl next: originals) {
//          if (!choices.contains(next)) {
//            XMLProperty prop = next.getParentProperty();
//            XMLProperty parent = prop.getParentProperty();
//            parent.getPropertyContent().remove(prop);
//          }
//        }
//        return true;
//      }
//      return false;
//  	}
//  	
//  };
//

  /**
   * A table to display and edit parameters.
   */
  class ParamTable extends JTable {
    /**
     * Constructor ParamTable
     * @param model
     */
    public ParamTable(ParamTableModel model) {
      super(model);
//      setPreferredScrollableViewportSize(new Dimension(120, 50));
//      getColumnModel().getColumn(0).setPreferredWidth(150);
//      getColumnModel().getColumn(1).setPreferredWidth(150);
//      getColumnModel().getColumn(2).setWidth(20);
      setGridColor(Color.blue);
      JTableHeader header = getTableHeader();
      header.setForeground(Color.blue);
    }

    public TableCellRenderer getCellRenderer(int row, int column) {
      return cellRenderer;
    }

    public TableCellEditor getCellEditor(int row, int column) {
      spinCellEditor.rowNumber = row;
      return spinCellEditor;
    }

    public void setFont(Font font) {
      super.setFont(font);
      if(cellRenderer!=null) {
        Font aFont = cellRenderer.labelFont;
        aFont = aFont.deriveFont(font.getSize2D());
        cellRenderer.labelFont = aFont;
        spinCellEditor.stepSizeLabel.setFont(aFont);
        aFont = cellRenderer.fieldFont;
        aFont = aFont.deriveFont(font.getSize2D());
        cellRenderer.fieldFont = aFont;
        spinCellEditor.field.setFont(aFont);
      }
      setRowHeight(font.getSize()+4);
    }

  }

  /**
   * A class to provide model data for the parameters table.
   */
  class ParamTableModel extends AbstractTableModel {
    public String getColumnName(int col) {
      return (col==0)? ToolsRes.getString("Table.Heading.Parameter"): //$NON-NLS-1$
      	ToolsRes.getString("Table.Heading.Value"); //$NON-NLS-1$
    }

    public int getRowCount() {
      return (fit==null)? 0: fit.getParameterCount();
    }

    public int getColumnCount() {
      return 2;
    }

    public Object getValueAt(int row, int col) {
      if(col==0) {
        return fit.getParameterName(row);
      }
      return new Double(fit.getParameterValue(row));	
    }

    public boolean isCellEditable(int row, int col) {
      return col==1;
    }

    public Class<?> getColumnClass(int c) {
      return getValueAt(0, c).getClass();
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

    // Returns a label for the specified cell.
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
      int row, int col) {
      setHorizontalAlignment(SwingConstants.LEFT);
      setBorder(new CellBorder(new Color(240, 240, 240)));
      String tooltip = col==1? ToolsRes.getString("DatasetCurveFitter.SE.Description"): null; //$NON-NLS-1$
      if(value instanceof String) { // parameter name string
        setFont(labelFont);
        setBackground(isSelected
                      ? Color.LIGHT_GRAY
                      : lightGray);
        setForeground(Color.black);
        setText(value.toString());
      } else {                      // Double value
        setFont(fieldFont);
        setBackground(isSelected
                      ? lightBlue
                      : Color.white);
        setForeground(isSelected
                      ? Color.red
                      : table.isEnabled()? Color.black
                      : Color.gray);
        Format format = spinCellEditor.field.format;
        setText(format.format(value));
        if (!autofitCheckBox.isSelected()) {
      		tooltip += " "+ToolsRes.getString("DatasetCurveFitter.SE.Autofit");        	  //$NON-NLS-1$//$NON-NLS-2$
        }
        else if (fit instanceof KnownPolynomial) {
          tooltip += " "+ToolsRes.getString("DatasetCurveFitter.SE.Unknown");  //$NON-NLS-1$//$NON-NLS-2$
          KnownPolynomial poly = (KnownPolynomial) fit;
          if (poly.degree()==1) {
          	String uncert = getUncertaintyString(row);
          	if (uncert!=null) 
          		tooltip = uncert+" ("+ToolsRes.getString("DatasetCurveFitter.SE.Name")+")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
          }
        }
        else {
      		tooltip += " "+ToolsRes.getString("DatasetCurveFitter.SE.Unknown");       	  //$NON-NLS-1$//$NON-NLS-2$
        }
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
        public void stateChanged(ChangeEvent e) {
          autofitCheckBox.setSelected(false);
          double val = ((Double) spinner.getValue()).doubleValue();
          field.setValue(val);
          fit.setParameterValue(rowNumber, val);
          if (fit instanceof UserFunction) {
            // get dependent parameter values from fit builder
            UserFunction f = (UserFunction)fit;
            String name = f.getName();
            FitFunctionPanel panel = (FitFunctionPanel) fitBuilder.getPanel(name);
            if(panel!=null) {
              name = f.getParameterName(rowNumber);
              Parameter seed = new Parameter(name, field.getText());
              Iterator<?> it = panel.getParamEditor().evaluateDependents(seed).iterator();
              while(it.hasNext()) {
                Parameter p = (Parameter) it.next();
                // find row number, set value in fit
                for(int i = 0; i<f.getParameterCount(); i++) {
                  if(f.getParameterName(i).equals(p.getName())) {
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
          firePropertyChange("changed", null, null);                       //$NON-NLS-1$
        }

      });
      field = new NumberField(10);
      field.applyPattern("0.000E0"); //$NON-NLS-1$
      field.setBorder(BorderFactory.createEmptyBorder(1, 1, 0, 0));
      spinner.setBorder(BorderFactory.createEmptyBorder(0, 1, 1, 0));
      spinner.setEditor(field);
      stepSizeLabel.addMouseListener(new MouseInputAdapter() {
        public void mousePressed(MouseEvent e) {
          JPopupMenu popup = new JPopupMenu();
          ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              // set the percent delta
              double percent = Double.parseDouble(e.getActionCommand());
              crawlerModel.setPercentDelta(percent);
              crawlerModel.refreshDelta();
              stepSizeLabel.setText(e.getActionCommand()+"%"); //$NON-NLS-1$
            }

          };
          for(int i = 0; i<3; i++) {
            String val = (i==0)? "10": (i==1)? "1.0": "0.1"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            JMenuItem item = new JMenuItem(val+"%"); //$NON-NLS-1$
            item.setActionCommand(val);
            item.addActionListener(listener);
            popup.add(item);
          }
          // show the popup
          popup.show(stepSizeLabel, 0, stepSizeLabel.getHeight());
        }

      });
      field.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          JComponent comp = (JComponent) e.getSource();
          if(e.getKeyCode()==KeyEvent.VK_ENTER) {
            spinner.setValue(new Double(field.getValue()));
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
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      spinner.setValue(value);
      crawlerModel.refreshDelta();
      return panel;
    }

    // Determines when editing starts.
    public boolean isCellEditable(EventObject e) {
      if(e instanceof MouseEvent) {
        return true;
      } else if(e instanceof ActionEvent) {
        return true;
      }
      return false;
    }

    // Called when editing is completed.
    public Object getCellEditorValue() {
      if(field.getBackground()==Color.yellow) {
        fit.setParameterValue(rowNumber, field.getValue());
        drawer.functionChanged = true;
        DatasetCurveFitter.this.firePropertyChange("fit", null, null); //$NON-NLS-1$
        field.setBackground(Color.white);
        firePropertyChange("changed", null, null);                     //$NON-NLS-1$
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
     * @param initialDelta
     */
    public SpinnerNumberCrawlerModel(double initialDelta) {
      delta = initialDelta;
    }

    public Object getValue() {
      return new Double(val);
    }

    public Object getNextValue() {
      return new Double(val+delta);
    }

    public Object getPreviousValue() {
      return new Double(val-delta);
    }

    public void setValue(Object value) {
      if(value!=null) {
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
      if(val!=0) {
        delta = Math.abs(val*percentDelta/100);
      }
    }

  }

  /**
   * A function whose value is the total deviation squared
   * between a multivariable function and a set of data points.
   * This is minimized by the HessianMinimize class.
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
    public double evaluate(double[] params) {
      System.arraycopy(params, 0, vars, 1, 4);
      double sum = 0.0;
      for(int i = 0, n = x.length; i<n; i++) {
        vars[0] = x[i];
        // evaluate the function and find deviation
        double dev = y[i]-f.evaluate(vars);
        // sum the squares of the deviations
        sum += dev*dev;
      }
      return sum;
    }

  }

  /**
   * A function whose value is the total deviation squared
   * between a user function and a set of data points.
   * This function is minimized by the HessianMinimize class.
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
    public double evaluate(double[] params) {
      // set the parameter values of the user function
      for(int i = 0; i<params.length; i++) {
        f.setParameterValue(i, params[i]);
      }
      double sum = 0.0;
      for(int i = 0; i<x.length; i++) {
        // evaluate the user function and find deviation
        double dev = y[i]-f.evaluate(x[i]);
        // sum the squares of the deviations
        sum += dev*dev;
      }
      return sum;
    }

  }

  /**
   * A JTextField that accepts only numbers.
   */
  class NumberField extends JTextField {
    // instance fields
    protected NumberFormat format = NumberFormat.getInstance();
    protected double prevValue;

    /**
     * Constructor NumberField
     * @param columns
     */
    public NumberField(int columns) {
      super(columns);
      setForeground(Color.black);
    }

    public double getValue() {
      if(getText().equals(format.format(prevValue))) {
        return prevValue;
      }
      double retValue;
      try {
        retValue = format.parse(getText()).doubleValue();
      } catch(ParseException e) {
        Toolkit.getDefaultToolkit().beep();
        setValue(prevValue);
        return prevValue;
      }
      return retValue;
    }

    public void setValue(double value) {
      if(!isVisible()) {
        return;
      }
      setText(format.format(value));
      prevValue = value;
    }
    
    public void applyPattern(String pattern) {
      if(format instanceof DecimalFormat) {
        ((DecimalFormat) format).applyPattern(pattern);
      }
    }

  }
  
//  class FitDialog extends JDialog {
//  	FitDialog() {
//  		super(JOptionPane.getFrameForComponent(DatasetCurveFitter.this), false);
//    	setContentPane(fitBuilder.getContentPane());
//    	setTitle(fitBuilder.getTitle());
//  	}
//  	
//    /**
//     * Overrides JDialog setVisible method.
//     *
//     * @param vis true to show this inspector
//     */
//    public void setVisible(boolean vis) {
//    	if (vis) {
//      	setLocation(fitBuilder.getLocation());
//      	setSize(fitBuilder.getSize());
//    	}
//    	else {
//      	fitBuilder.setLocation(getLocation());
//      	fitBuilder.setSize(getSize());
//    	}
//    	super.setVisible(vis);
//    	if (!vis)
//    		dispose();
//    }
//
//  }
//
  /**
   * Sets the default fit functions. Every instance of DatasetCurveFitter will
   * make these fits available.
   * 
   * @param functions the fit functions
   */
  public static void setDefaultFitFunctions(ArrayList<KnownFunction> functions) {
  	if (functions != null) {
  		defaultFits = functions;
  	}
  }

}

/*
 * Open Source Physics software is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.

 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be released
 * under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2007  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
