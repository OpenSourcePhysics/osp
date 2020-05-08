package test;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.ResourceLoader;


public class AssetsTest {

	AssetsTest() {
		/** @j2sNative
		 console.log('Finding Panel.'); 
	   debugger; 
	*/
		Object obj =OSPRuntime.jsutil.getAppletInfo("assets");
		System.out.println("obj="+obj);
		//String imageName = "/org/opensourcephysics/resources/controls/images/inspect.gif"; //$NON-NLS-1$
		String imageName = "cover.gif"; //$NON-NLS-1$
		URL url=ResourceLoader.getImageZipResource(imageName);
		System.out.println("url="+url);
		ImageIcon icon = new ImageIcon(url);
		
		JFrame frame = new JFrame("Zip Resource Loader Example");
		IconPanel imagePanel = new IconPanel(icon);
		imagePanel.setLayout(new BorderLayout());
		frame.add(imagePanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(300, 300);
		frame.setVisible(true);
	
	}
	

class IconPanel extends JPanel {

    private ImageIcon icon;

    public IconPanel(ImageIcon icon) {

    	this.icon=icon;
      setPreferredSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        icon.paintIcon(this, g, 100, 100);
    }
}
	

	public static void main(String[] args) {
		new AssetsTest();
	}

}
