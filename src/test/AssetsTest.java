package test;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.display.OSPRuntime;

import javajs.async.Assets;

/**
 * Test the zip asset loader API.
 * 
 * @author Wolfgang Christian
 *
 */
@SuppressWarnings("serial")
public class AssetsTest extends Test_ {

	static {
		
		// This declaration ensures OSPRuntime has been run, as
		// it is where the assets are defined.
		if (OSPRuntime.isJS) {
			OSPLog.debug("assets=" + Assets.getInstance().toString());			
		}
		
	}

	AssetsTest() {
		if (!OSPRuntime.isJS) {
			System.out.println("Assets are JavaScript only now.");
			System.exit(0);
		}
		// test that an image that is NOT in the zip file can be loaded directly.
		String imageName = "org/opensourcephysics/resources/cover.gif";
		URL url = Assets.getURLFromPath(imageName, true);
		if (url == null) {
			OSPLog.debug(imageName + " was not found in an asset ZIP file");
			url = Assets.getURLFromPath(imageName, false);
		}
		System.out.println("url=" + url);
		if (url != null) {
			ImageIcon icon = new ImageIcon(url);
			JFrame frame = new JFrame("Asset Loader Example");
			IconPanel imagePanel = new IconPanel(icon);
			imagePanel.setLayout(new BorderLayout());
			frame.add(imagePanel);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(400, 400);
			frame.setVisible(true);
		}

		// Test that we can get the ZipEntry list.
		getFileList("osp-assets.zip");

		// Test that we can get the bytes for cover.gif, which is not in the zip file
		byte[] bytes = Assets.getURLContents(url);

		String magic = new String(new byte[] { bytes[0], bytes[1], bytes[2], bytes[3] });
		OSPLog.debug("\n\n" + magic + " " + bytes.length);

		// Test that we can get the bytes for a file that is NOT in the zip file 	
		bytes = Assets.getAssetBytes(imageName);
		assert(bytes.length == 30189);

		// Test that we can get the bytes for a file that is in the zip file 	
		bytes = Assets.getAssetBytes("org/opensourcephysics/resources/display/drawing_tools.xml");
		assert(bytes.length == 964);
		
		// test with file name space
		url = Assets.getURLFromPath("test/spacetest.zip!/Car in a loop with friction.trk");
		bytes = Assets.getURLContents(url);
		assert(bytes.length == 50356);

//		// test remote access - NO CORS at St. Olaf. 
//		url = Assets.getURLFromPath("https://chemapps.stolaf.edu/swingjs/test/spacetest.zip");//!/Car in a loop with friction.trk");
//		bytes = Assets.getURLContents(url);
//		assert(bytes.length == 655915);
//
//		// test remote access- DOES NOT WORK WITH QUERY in Java.
//		url = Assets.getURLFromPath("https://www.compadre.org/osp/document/ServeFile.cfm?ID=15022&DocID=5059&Attachment=1!/Car in a loop with friction.trk");
//		bytes = Assets.getURLContents(url);
//		assert(bytes.length == 50356);
		
		System.out.println("AssetsTest OK");
	}

	public void getFileList(String zipPath) {
		// The problem here is that assets needs to be a URI path in Java. 
		Map<String, ZipEntry> map = Assets.getZipContents(zipPath);
		if (map == null) {
			System.err.println("Map is null: " + zipPath);
			return;
		}
		List<String> list = new ArrayList<>();
		list.add("");
		for (Map.Entry<String, ZipEntry> entry : map.entrySet()) {
			ZipEntry val = entry.getValue();
			list.add(rightFill(val.getName(), 70) + leftFill(""+ val.getSize(), 8) + " bytes");
		}
		String[] s = list.toArray(new String[list.size()]);
		Arrays.sort(s);
		OSPLog.debug(Arrays.toString(s).replace(",", "\n").replaceAll("[\\[\\]]", ""));

		
	}

	private String leftFill(String name, int n) {
		        name = "                                                                      " + name;
		return name.substring(name.length() - n);
	}
	
	private String rightFill(String name, int n) {
		return (name + "                                                                      ").substring(0, n);
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
