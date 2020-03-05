package test;

import javax.swing.JFileChooser;

import org.opensourcephysics.display.OSPRuntime;

import javajs.async.AsyncFileChooser;

public class GetFileWithChooser {
	
	
	GetFileWithChooser() {
		AsyncFileChooser chooser = OSPRuntime.getChooser();
		chooser.showOpenDialog(null, new Runnable() {

			@Override
			public void run() {
				String fileName = chooser.getSelectedFile().getAbsolutePath();
				System.out.println("File name=" + fileName);
			}

		}, null);
	}
 
  public static void main(String[] args) {
  	new GetFileWithChooser();
  }
	
}
