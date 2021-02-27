package org.opensourcephysics.tools;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import org.opensourcephysics.controls.ListChooser;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;

/**
 * A class to refresh search data for the LibraryBrowser when running in JS.
 * 
 * @author Douglas Brown
 */
public class LibraryJSSearchRefresher implements PropertyChangeListener {

	private final int BUFFER_SIZE = 4096;
	private final String host = "physlets.org/library/Search/";
	private final String ftpURLFormat = "ftp://%s@%s;type=i";
	private final String[] libraryPaths = { LibraryBrowser.TRACKER_LIBRARY, LibraryBrowser.SHARED_LIBRARY };

	private LibraryBrowser browser;
	private ArrayList<String> paths = new ArrayList<String>();
	private ArrayList<String> names = new ArrayList<String>();
	private TreeMap<String, String> nameToPathMap = new TreeMap<String, String>();
	private int currentIndex = 0;
	private String usernameAndPW;
	private long t0 = System.currentTimeMillis();
	private long t;

	public static void main(String[] args) {
		new LibraryJSSearchRefresher().refreshSearchData();
	}

	/**
	 * Refreshes the search data.
	 */
	void refreshSearchData() {
		readFTPLoginData();
		openLibraryBrowser();
		chooseCollectionsToRefresh();
		refreshNext();
	}

	/**
	 * Reads the FTP username and password data from the local file ~/ftp_login.txt.
	 */
	void readFTPLoginData() {
		String userhome = System.getProperty("user.home");
		File file = new File(userhome, "ftp_login.txt");
		if (!file.exists()) {
			JOptionPane.showMessageDialog(null, "File " + file.getPath() + " not found.\n"
					+ "The file should contain the single line \"FTPusername:pw\"");
			System.exit(0);
		}
		usernameAndPW = ResourceLoader.getString(file.getPath()).trim();
	}

	/**
	 * Opens the LibraryBrowser.
	 */
	void openLibraryBrowser() {
		browser = LibraryBrowser.getBrowser();
		browser.addMetadataLoaderListener(this);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (dim.width - browser.getBounds().width) / 2;
		int y = (dim.height - browser.getBounds().height) / 2;
		browser.setLocation(x, y);
		browser.setVisible(true);
	}

	/**
	 * Lets the user choose which collections to refresh.
	 */
	void chooseCollectionsToRefresh() {
		String query = LibraryComPADRE.TRACKER_SERVER_TREE + LibraryComPADRE.PRIMARY_ONLY;
		String compadreName = "ComPADRE " + LibraryComPADRE.getCollectionName(query);
		nameToPathMap.put(compadreName, query);
		for (int i = 0; i < libraryPaths.length; i++) {
			XMLControl control = new XMLControlElement(libraryPaths[i]);
			Library lib = new Library();
			control.loadObject(lib);
			HashMap<String, String> map = lib.getNameMap();
			TreeSet<String> paths = lib.getAllPaths();
			for (String path : paths) {
				nameToPathMap.put(map.get(path), path);
			}
		}
		for (String name : nameToPathMap.keySet()) {
			names.add(name);
			paths.add(nameToPathMap.get(name));
		}

		ListChooser dialog = new ListChooser("", "Refresh search data for:", null, null);
		if (!dialog.choose(paths, names, null, null, null, null))
			paths.clear();
	}

	/**
	 * Refreshes search data for the next collection path, if any. Exits when all
	 * paths have been refreshed.
	 */
	void refreshNext() {
		//t = System.currentTimeMillis();
		if (currentIndex < paths.size()) {
			String next = paths.get(currentIndex);
			currentIndex++;
			// System.out.println("start refreshing "+next);
			browser.open(next);
		} else {
			// System.out.println("finished refreshing "+paths.size()+" collections in " +
			// (t-t0)/1000 +" sec");
			System.exit(0);
		}
	}

	/**
	 * Uploads a searchable XML file to the web.
	 */
	void uploadToWeb(String localFilePath) {
		String ftpURL = String.format(ftpURLFormat, usernameAndPW, host + XML.getName(localFilePath));

		try {
			URL url = new URL(ftpURL);
			URLConnection conn = url.openConnection();
			OutputStream outputStream = conn.getOutputStream();
			FileInputStream inputStream = new FileInputStream(localFilePath);

			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = -1;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}

			inputStream.close();
			outputStream.close();
			//System.out.println("successfully uploaded file " + localFilePath + " to web");
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		File file = (File) e.getNewValue();
		long now = System.currentTimeMillis();
		if (file.exists())
			uploadToWeb(file.getAbsolutePath());
		browser.closeTab(0);
		// System.out.println("finished refreshing "+ e.getPropertyName() + " in " +
		// (now-t)/1000 + " sec");
		refreshNext();
	}

}