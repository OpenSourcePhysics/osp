package test;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.net.URL;
import java.util.Map;
import java.util.zip.ZipEntry;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * Test the zip asset loader API.
 * 
 * @author Wolfgang Christian
 *
 */
@SuppressWarnings("serial")
public class AssetsTest {

	AssetsTest() {
		/**
		 * @j2sNative console.log('Finding Panel.'); debugger;
		 */
		Object obj = OSPRuntime.jsutil.getAppletInfo("assets");
		System.out.println("obj=" + obj);

		// Pick an image to load from an asset archive.
		// Use asset: 'ospassets/osp_respources.zip'
		//String imageName ="/org/opensourcephysics/resources/controls/images/inspect.gif";

		// Use asset: 'ospassets/physlet_respources.zip'
		//String imageName = "/opticsimages/bear1.gif";
		String imageName = "/cover.gif";

		URL url = ResourceLoader.getImageZipResource(imageName);
		System.out.println("url=" + url);
		ImageIcon icon = new ImageIcon(url);

		JFrame frame = new JFrame("Asset Loader Example");
		IconPanel imagePanel = new IconPanel(icon);
		imagePanel.setLayout(new BorderLayout());
		frame.add(imagePanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(400, 400);
		frame.setVisible(true);
		
		getFileList();

	}
	
	public void getFileList() {
		//String zipFile="/ospassets/physlet_respources.zip"; // fails
		String zipFile="physlet_respources.zip";              // also fails
		Map<String, ZipEntry>  map=ResourceLoader.getZipContents(zipFile);
		if(map==null) {
			System.err.println("Map is null: "+zipFile);
			return;
		}
    for (Map.Entry<String,ZipEntry> entry : map.entrySet()) {  
      System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue()); 
  		// OSPRuntime.jsutil.getZipBytes(entry.getValue());  //to read zip entry
    }
	}

	class IconPanel extends JPanel {

		private ImageIcon icon;

		public IconPanel(ImageIcon icon) {

			this.icon = icon;
			setPreferredSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			icon.paintIcon(this, g, 10, 10);
		}
	}

	public static void main(String[] args) {
		new AssetsTest();
	}

}
