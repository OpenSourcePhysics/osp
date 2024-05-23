package demoJS;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.display.OSPRuntime;

public class SaveTXTFile {
	
  File currentLocation=null;
	
  SaveTXTFile() {
  	
  	/** @j2sNative   	currentLocation = window.location.pathname.split('/').slice(0, -1).join('/') */
  	
  	String str="Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. "
  			+ "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure "
  			+ "dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat "
  			+ "non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.\n";
		saveTXT(str);
		System.out.println("TXT Saved");
	}
	
  public void saveTXT(String str){
  	String ext=".txt";
    JFileChooser chooser = OSPRuntime.getChooser();
    //JFileChooser chooser = new JFileChooser();
    String oldTitle = chooser.getDialogTitle();
    chooser.setDialogTitle("Save TXT File");
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
        FileOutputStream stream=null;
				try {
					stream = new FileOutputStream(file);
				} catch (FileNotFoundException ex) {
					OSPLog.info(ex.getMessage());
					return;
				}
        java.nio.charset.Charset charset = java.nio.charset.Charset.forName("UTF-8");
        OutputStreamWriter out = new OutputStreamWriter(stream, charset);
        try {
        	BufferedWriter output = new BufferedWriter(out);
          output.write(str);
          output.flush();
          output.close();
        } catch(IOException ex) {
          OSPLog.info(ex.getMessage());
        }
    }

}

	public static void main(String[] args) {
		new SaveTXTFile();
	}

}
