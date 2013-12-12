package org.opensourcephysics.tools;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.filechooser.FileFilter;

import org.opensourcephysics.controls.ListChooser;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.controls.XMLProperty;
import org.opensourcephysics.display.Dataset;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.display.TeXParser;

/**
 * This is a FunctionTool used by DatasetCurveFitter to build, save and load custom fit functions.
 * Some methods are tailored for use with DataTool since that is its main application. 
 * 
 * @author Doug Brown
 *
 */
public class FitBuilder extends FunctionTool {
	
	static JFileChooser chooser;
	
  protected JButton newFitButton, deleteFitButton, cloneFitButton, loadButton, saveButton;
  protected Component parent;

	/**
	 * Constructor
	 * 
	 * @param c a component to determine the dialog owner
	 */
	public FitBuilder(Component c) {
		super(c);
		parent = c;
    newFitButton = new JButton(ToolsRes.getString("DatasetCurveFitter.Button.NewFit.Text"));          //$NON-NLS-1$
    newFitButton.setToolTipText(ToolsRes.getString("DatasetCurveFitter.Button.NewFit.Tooltip"));      //$NON-NLS-1$
    newFitButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        // create new user function and function panel 
        String name = getUniqueName(ToolsRes.getString("DatasetCurveFitter.NewFit.Name")); //$NON-NLS-1$
        UserFunction f = new UserFunction(name);
        Dataset dataset = null;
        DatasetCurveFitter fitter = getSelectedCurveFitter();
        if (fitter!=null) {
        	dataset = fitter.getData();
        }
        String var = (dataset==null)? "x":  //$NON-NLS-1$
      	  TeXParser.removeSubscripting(dataset.getColumnName(0));
        
        f.setExpression("0", new String[] {var});  //$NON-NLS-1$
        addFitFunction(f);
      }
    });
    deleteFitButton = new JButton(ToolsRes.getString("DatasetCurveFitter.Button.DeleteFit.Text")); //$NON-NLS-1$
    deleteFitButton.setToolTipText(ToolsRes.getString("DatasetCurveFitter.Button.DeleteFit.Tooltip")); //$NON-NLS-1$
    deleteFitButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        // remove selected fit and panel 
        String name = getSelectedName();
        removePanel(name);
      }

    });
    cloneFitButton = new JButton(ToolsRes.getString("DatasetCurveFitter.Button.Clone.Text")); //$NON-NLS-1$
    cloneFitButton.setToolTipText(ToolsRes.getString("DatasetCurveFitter.Button.Clone.Tooltip")); //$NON-NLS-1$
    cloneFitButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        // inner popup menu listener class
      	final Map<String, KnownFunction> fits = new HashMap<String, KnownFunction>();
      	final ArrayList<String> fitnames = new ArrayList<String>();
      	for (DatasetCurveFitter fitter: curveFitters) {
      		for (int i = 0; i < fitter.fitDropDown.getItemCount(); i++) {
      			String name = fitter.fitDropDown.getItemAt(i).toString();
      			if (!fitnames.contains(name)) {
      				fitnames.add(name);
      				fits.put(name, fitter.allFitsMap.get(name));
      			}
      		}
      	}
        ActionListener listener = new ActionListener() {
          public void actionPerformed(ActionEvent e) {
          	for (String name: fitnames) {
          		if (name.equals(e.getActionCommand())) {           			
                DatasetCurveFitter fitter = getSelectedCurveFitter();
                if (fitter!=null) {
	          			KnownFunction f = fits.get(name);
	          			UserFunction uf = fitter.createClone(f, name);
	                UserFunctionEditor editor = new UserFunctionEditor();
	                editor.setMainFunctions(new UserFunction[] {uf});
	                FitFunctionPanel panel = new FitFunctionPanel(editor);
	                addPanel(uf.getName(), panel);
                }
          		}
          	}
          }
        };
        // create popup menu and add existing fit function items
        JPopupMenu popup = new JPopupMenu();
        for (String name: fitnames) {
          JMenuItem item = new JMenuItem(name);
          item.setActionCommand(name);
          item.addActionListener(listener);
          popup.add(item);          		
        }
        // show the popup below the button
        popup.show(cloneFitButton, 0, cloneFitButton.getHeight());
      }
    });
    String imageFile = "/org/opensourcephysics/resources/tools/images/open.gif"; //$NON-NLS-1$
    Icon openIcon = ResourceLoader.getIcon(imageFile);
    loadButton = new JButton(openIcon);
    loadButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	loadFits();
      }

    });
    imageFile = "/org/opensourcephysics/resources/tools/images/save.gif"; //$NON-NLS-1$
    Icon saveIcon = ResourceLoader.getIcon(imageFile);
    saveButton = new JButton(saveIcon);
    saveButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	saveFits();
      }
    });
    setToolbarComponents(new Component[] {loadButton, saveButton,
				 new JToolBar.Separator(), newFitButton, cloneFitButton, deleteFitButton});
	}
	
	/**
	 * Gets the DataSetCurveFitter currently selected in the DataTool.
	 * 
	 * @return the selected DataSetCurveFitter
	 */
	public DatasetCurveFitter getSelectedCurveFitter() {
		Window win = this.getOwner();
		if (win!=null && win instanceof DataTool) {
			DataTool dataTool = (DataTool)win;
			DataToolTab tab = dataTool.getSelectedTab();
			if (tab!=null) {
				return tab.curveFitter;
			}
		}
		return null;
	}
	
	/**
	 * Refreshes the dropdown with names of the available fits.
	 * 
	 * @param name the selected fit name
	 */
	public void refreshDropdown(String name) {
  	deleteFitButton.setEnabled(!getPanelNames().isEmpty());
  	if (getPanelNames().isEmpty()) {
	  	String label = ToolsRes.getString("FitFunctionPanel.Label"); //$NON-NLS-1$
      dropdownLabel.setText(label+":"); //$NON-NLS-1$
  	}
  	super.refreshDropdown(name);
  }
	
	/**
	 * Adds a new fit function.
	 * 
	 * @param f the fit function to add
	 */
	public void addFitFunction(UserFunction f) {
    UserFunctionEditor editor = new UserFunctionEditor();
    editor.setMainFunctions(new UserFunction[] {f});
    FitFunctionPanel panel = new FitFunctionPanel(editor);
    addPanel(f.getName(), panel);		
	}
	
	/**
	 * Loads fit functions from an XML file chosen by the user.
	 * 
	 * @return the path to the file opened, or null if none
	 */
	public String loadFits() {
  	if (chooser==null) {
  		chooser = OSPRuntime.getChooser();
      for (FileFilter filter: chooser.getChoosableFileFilters()) {
      	if (filter.getDescription().toLowerCase().indexOf("xml")>-1) { //$NON-NLS-1$
          chooser.setFileFilter(filter);
      		break;
      	}
      }
  	}
    int result = chooser.showOpenDialog(FitBuilder.this);
    if(result==JFileChooser.APPROVE_OPTION) {
      OSPRuntime.chooserDir = chooser.getCurrentDirectory().toString();
      String fileName = chooser.getSelectedFile().getAbsolutePath();
      return loadFits(fileName, false);
    }
    return null;
	}
	
	/**
	 * Loads fit functions from an XML file.
	 * 
	 * @param path the path to the XML file
	 * @param loadAll true to load all fit functions defined in the file, false to let the user choose
	 * @return the path to the file opened, or null if failed
	 */
	public String loadFits(String path, boolean loadAll) {
		if (path==null) return loadFits();
		
    XMLControl control = new XMLControlElement(path);    
    if (control.failedToRead()) {
      JOptionPane.showMessageDialog(FitBuilder.this, 
      		ToolsRes.getString("Dialog.Invalid.Message"), //$NON-NLS-1$
      		ToolsRes.getString("Dialog.Invalid.Title"), //$NON-NLS-1$
      		JOptionPane.ERROR_MESSAGE);
      return null;
    }

    Class<?> type = control.getObjectClass();
    if (FitBuilder.class.isAssignableFrom(type)) {
      // choose fits to load
      if (loadAll || chooseFitFunctions(control, "Load")) { //$NON-NLS-1$
        control.loadObject(this);            	
      }
    }
		else {
      JOptionPane.showMessageDialog(FitBuilder.this, 
          ToolsRes.getString("DatasetCurveFitter.FitBuilder.Dialog.WrongType.Message"), //$NON-NLS-1$
      		ToolsRes.getString("DatasetCurveFitter.FitBuilder.Dialog.WrongType.Title"), //$NON-NLS-1$
      		JOptionPane.ERROR_MESSAGE);
		}
    return path;
	}
	
	/**
	 * Saves fit functions to an XML file chosen by the user.
	 * 
	 * @return the path to the file saved, or null if cancelled or failed
	 */
	public String saveFits() {
  	XMLControl control = new XMLControlElement(this);
    // choose fits to save
    if (chooseFitFunctions(control, "Save")) { //$NON-NLS-1$
    	if (chooser==null) {
    		chooser = OSPRuntime.getChooser();
        for (FileFilter filter: chooser.getChoosableFileFilters()) {
        	if (filter.getDescription().toLowerCase().indexOf("xml")>-1) { //$NON-NLS-1$
	          chooser.setFileFilter(filter);
        		break;
        	}
        }
    	}
      int result = chooser.showSaveDialog(FitBuilder.this);
      if (result==JFileChooser.APPROVE_OPTION) {
        OSPRuntime.chooserDir = chooser.getCurrentDirectory().toString();
        File file = chooser.getSelectedFile();
        // check to see if file already exists
        if(file.exists()) {
          int isSelected = JOptionPane.showConfirmDialog(FitBuilder.this, 
          		ToolsRes.getString("Tool.Dialog.ReplaceFile.Message")+" "+file.getName()+"?", //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$
          		ToolsRes.getString("Tool.Dialog.ReplaceFile.Title"), //$NON-NLS-1$
              JOptionPane.YES_NO_CANCEL_OPTION);
          if(isSelected!=JOptionPane.YES_OPTION) {
            return null;
          }
        }
        return saveFits(file.getAbsolutePath());
      }
    }
    return null;
	}
	
	/**
	 * Saves fit functions to an XML file.
	 * 
	 * @param path the file path
	 * @return the path to the file saved
	 */
	public String saveFits(String path) {
		if (path==null) return saveFits();
    // add .xml extension if none but don't replace other extensions
    if(XML.getExtension(path)==null) {
    	path += ".xml";                                    //$NON-NLS-1$
    }
  	XMLControl control = new XMLControlElement(this);
    control.write(path);
    return path;
	}
	
	/**
	 * Refreshes the GUI.
	 */
	protected void refreshGUI() {
  	super.refreshGUI();
  	setTitle(ToolsRes.getString("DatasetCurveFitter.FitBuilder.Title")); //$NON-NLS-1$
  	if (getPanelNames().isEmpty()) {
	  	String label = ToolsRes.getString("FitFunctionPanel.Label"); //$NON-NLS-1$
      dropdownLabel.setText(label+":"); //$NON-NLS-1$
  	}
  	if (saveButton!=null) {
			saveButton.setEnabled(!getPanelNames().isEmpty());
			loadButton.setToolTipText(ToolsRes.getString("DatasetCurveFitter.FitBuilder.Button.Load.Tooltip")); //$NON-NLS-1$
			saveButton.setToolTipText(ToolsRes.getString("DatasetCurveFitter.FitBuilder.Button.Save.Tooltip")); //$NON-NLS-1$
	    deleteFitButton.setEnabled(!getPanelNames().isEmpty());
	    newFitButton.setText(ToolsRes.getString("DatasetCurveFitter.Button.NewFit.Text"));                 //$NON-NLS-1$
	    newFitButton.setToolTipText(ToolsRes.getString("DatasetCurveFitter.Button.NewFit.Tooltip"));       //$NON-NLS-1$
	    deleteFitButton.setText(ToolsRes.getString("DatasetCurveFitter.Button.DeleteFit.Text"));           //$NON-NLS-1$
	    deleteFitButton.setToolTipText(ToolsRes.getString("DatasetCurveFitter.Button.DeleteFit.Tooltip")); //$NON-NLS-1$
      DatasetCurveFitter fitter = getSelectedCurveFitter();
      cloneFitButton.setEnabled(fitter!=null);
  	}
	} 
	
	/**
	 * Displays a dialog with a list of fit functions to load or save.
	 * 
	 * @param control a FitBuilder XMLControl
	 * @param description a description of the purpose (ie load or save)
	 * @return true if not cancelled by the user
	 */
	protected boolean chooseFitFunctions(XMLControl control, String description) {
    ListChooser listChooser = new ListChooser(
        ToolsRes.getString("DatasetCurveFitter.FitBuilder."+description+".Title"), //$NON-NLS-1$ //$NON-NLS-2$
        ToolsRes.getString("DatasetCurveFitter.FitBuilder."+description+".Message"), //$NON-NLS-1$ //$NON-NLS-2$
        this);
    // choose the elements and load the function tool
    ArrayList<XMLControl> originals = new ArrayList<XMLControl>();
    ArrayList<XMLControl> choices = new ArrayList<XMLControl>();
    ArrayList<String> names = new ArrayList<String>();
    ArrayList<String> expressions = new ArrayList<String>();
    for (Object next: control.getPropertyContent()) {
    	if (next instanceof XMLProperty) {
    		XMLProperty prop = (XMLProperty)next;
        for (Object obj: prop.getPropertyContent()) {
        	if (obj instanceof XMLProperty) {
        		XMLProperty f = (XMLProperty)obj;
        		XMLControl function = f.getChildControls()[0];
        		originals.add(function);
        		choices.add(function);
        		names.add(function.getString("name")); //$NON-NLS-1$
        		String desc = function.getString("description"); //$NON-NLS-1$
        		expressions.add(desc);
        	}
        }
    	}            	
    }
    // select all by default
    boolean[] selected = new boolean[choices.size()];
    for (int i = 0; i<selected.length; i++) {
    	selected[i] = true;
    }
    if (listChooser.choose(choices, names, expressions, selected)) {
      // compare choices with originals and remove unwanted object content
      for (XMLControl next: originals) {
        if (!choices.contains(next)) {
          XMLProperty prop = next.getParentProperty();
          XMLProperty parent = prop.getParentProperty();
          parent.getPropertyContent().remove(prop);
        }
      }
      return true;
    }
    return false;
	}
}
