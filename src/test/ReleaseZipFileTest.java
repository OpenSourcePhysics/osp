package test;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;
import java.util.zip.ZipEntry;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.opensourcephysics.display.OSPFrame;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.media.core.VideoIO.SingleExtFileFilter;
import org.opensourcephysics.tools.Resource;
import org.opensourcephysics.tools.ResourceLoader;

import javajs.async.AsyncFileChooser;

/**
 * This attempts to access a TRZ file and then delete it while the GUI is still open.
 * The real goal is to be able to replace a TRZ file that is initially open in the Library Browser.
 * This currently fails due to the TRZ file being open, even after attempting to remove
 * it from the browser.  Here the issue is simplified to opening and closing an InputStream
 * at line 81.
 */
public class ReleaseZipFileTest {

	OSPFrame frame = new OSPFrame("TRZ File Catch & Release Test");
	JMenuItem openItem;
	JMenuBar menuBar;

	ReleaseZipFileTest() {
	  menuBar = new JMenuBar();
	  openItem = new JMenuItem("Open TRZ"); //$NON-NLS-1$
		openItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AsyncFileChooser chooser = OSPRuntime.getChooser();
				VideoIO.trzFileFilter = new SingleExtFileFilter("trz", "TRZ files--MAY BE DELETED!!"); //$NON-NLS-1$ //$NON-NLS-2$

				chooser.addChoosableFileFilter(VideoIO.trzFileFilter);
				chooser.setFileFilter(VideoIO.trzFileFilter);
				chooser.setCurrentDirectory(new File((String)OSPRuntime.getPreference("file_chooser_directory")));
				chooser.showOpenDialog(frame, new Runnable() {

					@Override
					public void run() {
						File file = chooser.getSelectedFile();
						String path = file.getAbsolutePath();
						if (!path.endsWith(".trz"))
							return;
						System.out.println("TRZ path "+path);

						OSPRuntime.setPreference("file_chooser_directory", file.getParent());
						OSPRuntime.savePreferences();
						
						Map<String, ZipEntry> map = ResourceLoader.getZipContents(path);

						for (String next: map.keySet()) {
							if (next.contains(".htm")) {
								System.out.println("zip entry: "+next);
								URL url0 = null;
								Resource res = null;
								try {
									url0 = new URL("jar", null, new URL("file", null, path+"!/"+next).toString()); //$NON-NLS-1$ //$NON-NLS-2$
									res = new Resource(url0);
								} catch (MalformedURLException e) {
								}

								if (res != null) try {
									URL url = res.getURL();
									
									// opening and closing a stream here prevents file from being deleted below--why???
									InputStream stream = url.openStream();
									stream.close();
									
								} catch (Exception e) {
									System.err.println(e);
								}
								
								try {
									Files.delete(file.toPath());
								} catch (Exception e) {
									System.err.println(e);
								}								
								System.out.println("file deleted? "+!file.exists());
							}
						}
					}

				}, null);
			}

		});
		
	  JMenu menu = new JMenu("File");
		menuBar.add(menu);
		menu.add(openItem);
		
		frame.setJMenuBar(menuBar);
		frame.setSize(500, 500);
		frame.setLocation(new Point(500, 100));
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		openItem.doClick(100);
	}

	public static void main(String[] args) {
		new ReleaseZipFileTest();

	}

}
