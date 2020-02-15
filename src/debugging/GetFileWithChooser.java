package debugging;

import javax.swing.JFileChooser;

import org.opensourcephysics.display.OSPRuntime;

public class GetFileWithChooser {
	
	
	GetFileWithChooser(){
    JFileChooser chooser = OSPRuntime.getChooser();
    //JFileChooser chooser = new JFileChooser();
    int result = chooser.showOpenDialog(null);
    if(result==JFileChooser.APPROVE_OPTION) {
      String fileName = chooser.getSelectedFile().getAbsolutePath();
      System.out.println("File name="+fileName);
    }
	}

  public static void main(String[] args) {
  	new GetFileWithChooser();
  }
	
}
