package demoJS;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.display.OSPRuntime;

public class SaveXMLFile {
	
  File currentLocation=null;
	
	SaveXMLFile(XMLControlElement xml) {
		/** @j2sNative   	currentLocation = window.location.pathname.split('/').slice(0, -1).join('/') */
		saveXML(xml);
		System.out.println("XML Saved");
	}
	
  public void saveXML(XMLControl xml){
  	//String ext=".txt";
  	String ext=".xml";
    JFileChooser chooser = OSPRuntime.getChooser();
    //JFileChooser chooser = new JFileChooser();
    String oldTitle = chooser.getDialogTitle();
    chooser.setDialogTitle("Save XML File");
    chooser.setCurrentDirectory(currentLocation);
    int result = -1;
    try {
    	// This works in JavaScript and Java, because both are modal for a save request
    	result = chooser.showSaveDialog(null);
    } catch (Throwable e) {
    	e.printStackTrace();
    }
    chooser.setDialogTitle(oldTitle);
    if(result==JFileChooser.APPROVE_OPTION) {
        File file = chooser.getSelectedFile();              
        // check to see if file already exists
        
        // BH It is not possible to see if a file that is going to be saved exists -- or, more
        // Specifically, not relevant, since the browser will always add "(1)" or "(2)", etc. to 
        // a filename if one exists already. Also, the web page has no way of knowing what directory
        // the file will be saved in, and it certainly cannot search it, even if it did know.
        
        org.opensourcephysics.display.OSPRuntime.chooserDir = chooser.getCurrentDirectory().toString();
        String fileName = file.getAbsolutePath();
        if((fileName==null)||fileName.trim().equals("")) {
           return;
        }
        int i = fileName.toLowerCase().lastIndexOf(ext);
        if(i!=fileName.length()-4) {
           fileName += ext;
           file = new File(fileName);
        }
        
        // BH bypass, since it is not a relevant question in JavaScript
        if(/** @j2sNative false && */file.exists()) {
        	
        	// Again, this would not block in JavaScript. "selected" will be NaN. 
            int selected = JOptionPane.showConfirmDialog(null, "Replace existing "+file.getName()+"?", "Replace File",
               JOptionPane.YES_NO_CANCEL_OPTION);
            if(selected!=JOptionPane.YES_OPTION) {
               return;
            }
         }
        xml.write(fileName);
    }

}

	public static void main(String[] args) {
		XMLControlElement control= new XMLControlElement();
		String myName="myName";
		control.setValue("name", myName);
		double pi=3.14;
		control.setValue("pi", pi);
		double[] array= {10.0,20.0,30.0};
		control.setValue("data", array);
		double[][] array2D= {{1,10.0},{2,20.0},{3,30.0}};
		control.setValue("data2", array2D);
		new SaveXMLFile(control);
	}

}
