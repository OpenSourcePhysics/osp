package test;

import java.io.File;
import javax.swing.JFrame;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.controls.XMLTreePanel;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.js.JSUtil;

import javajs.async.AsyncFileChooser;

public class ReadXMLFile {

	File currentLocation = null;
	JFrame frame = new JFrame("Read XML Example");

	ReadXMLFile() {
		/** @j2sNative currentLocation = window.location.pathname.split('/').slice(0, -1).join('/') */
		frame.setSize(500, 500);
		frame.setVisible(true);
		readXML();
		System.out.println("XML Read");
	}

  public void readXML() {
    //AsyncFileChooser chooser = OSPRuntime.getChooser();
    AsyncFileChooser chooser = new AsyncFileChooser();
    String oldTitle = chooser.getDialogTitle();
    chooser.setDialogTitle("Load XML Data");
    chooser.showOpenDialog(null, new Runnable() {
   	 // OK
		@Override
		public void run() {
			if (!JSUtil.isJS) {
				// no directory information in HTML5
		     org.opensourcephysics.display.OSPRuntime.chooserDir = chooser.getCurrentDirectory().toString();
			}
		     // It is critical to pass the actual file along, as it has the bytes already.
		     File file=chooser.getSelectedFile();
		     OSPLog.fine("reading file="+file);
		     XMLControlElement xml = new XMLControlElement(file);
		     chooser.setDialogTitle(oldTitle);
		     displayXML(xml);
		}
   	 
    }, new Runnable() {
   	 // cancel
		@Override
		public void run() {
		     chooser.setDialogTitle(oldTitle);
		}
   	 
    });
  }
  
	public void displayXML(XMLControlElement xml) {
		XMLTreePanel treePanel = new XMLTreePanel(xml);
		frame.setContentPane(treePanel);
		frame.revalidate();
	}

	public static void main(String[] args) {
		new ReadXMLFile();
	}

}
