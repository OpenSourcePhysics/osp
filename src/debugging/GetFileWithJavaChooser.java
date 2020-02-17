package debugging;

import javax.swing.JFileChooser;

public class GetFileWithJavaChooser {

	GetFileWithJavaChooser() {
		JFileChooser chooser = new JFileChooser();
		int result = chooser.showOpenDialog(null);
		String fileName = chooser.getSelectedFile().getAbsolutePath();
		System.out.println("Result=" + result + "  File name=" + fileName);
	}

	public static void main(String[] args) {
		new GetFileWithJavaChooser();
	}
}
