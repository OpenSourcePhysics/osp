package test;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipEntry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.tools.JarTool;
import org.opensourcephysics.tools.LibraryBrowser;
import org.opensourcephysics.tools.LibraryComPADRE;
import org.opensourcephysics.tools.LibraryResource;
import org.opensourcephysics.tools.ResourceLoader;

import javajs.async.AsyncFileChooser;
import javajs.async.AsyncSwingWorker;

public class LoaderTest {
	
	LibraryBrowser libraryBrowser = getLibraryBrowser();
	JFrame frame = new JFrame("JFrame Example");
	JPanel panel = new JPanel();
	JPanel topPanel = new JPanel();
	JEditorPane pane = new JEditorPane();
	JScrollPane scrollPane= new JScrollPane(pane);
	
	String tempDir;
	ArrayList<String> xmlFiles = new ArrayList<String>();
	ArrayList<String> otherFiles = new ArrayList<String>();
	AsyncFileChooser fileChooser;
	String zipFilePath;
	String editedFilePath;
	
	int progressInit = 0, progress_done = 100;
	
	public LoaderTest(){
		panel.setLayout(new BorderLayout());
		topPanel.setLayout(new FlowLayout());
		pane.setEditable(true);

		JLabel label = new JLabel("ComPADRE");
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));

		JButton getButton = new JButton();
		getButton.setText("Open Browser");
		JButton saveButton = new JButton();
		saveButton.setText("Save Model");
		
		getButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
	   		libraryBrowser.setVisible(true);
			}
		});
		saveButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
	   		if (!saveNewZip()) {
	   			String msg = "Failed to save file "+XML.getName(zipFilePath);
	   		  JOptionPane.showMessageDialog(frame, msg, "Error", JOptionPane.ERROR_MESSAGE);  
	   		}	   			
			}
		});
		topPanel.add(label);
		topPanel.add(getButton);
		topPanel.add(saveButton);
		panel.add(scrollPane,BorderLayout.CENTER);
		panel.add(topPanel,BorderLayout.NORTH);
		frame.add(panel);
		frame.setSize(450, 300);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
	
	private void setEditorText(String text) {
		pane.setText(text);
	}
	
	
	private void showHelp() {
		String msg="The Lord helps those who help themselves.";
	  JOptionPane.showMessageDialog(null,msg);  
		System.out.println(msg);
	}
	
	/**
	 * Gets the library browser.
	 *
	 * @return the library browser
	 */
	private LibraryBrowser getLibraryBrowser() {
		if (libraryBrowser == null) {
			try {
				LibraryComPADRE.desiredOSPType = "EJS";
				libraryBrowser = LibraryBrowser.getBrowser(null);
				libraryBrowser.addComPADRECollection(
						LibraryComPADRE.TRACKER_SERVER_TREE + LibraryComPADRE.PRIMARY_ONLY);
				libraryBrowser.addComPADRECollection(
						LibraryComPADRE.EJS_SERVER_TREE + LibraryComPADRE.PRIMARY_ONLY);
				libraryBrowser.refreshCollectionsMenu();
				libraryBrowser.addPropertyChangeListener(LibraryBrowser.PROPERTY_LIBRARY_TARGET,
					new PropertyChangeListener() {
						@Override
						public void propertyChange(PropertyChangeEvent e) {
							// if HINT_LOAD_RESOURCE, then e.getNewValue() is LibraryResource to load
							if (LibraryBrowser.HINT_LOAD_RESOURCE == e.getOldValue()) {
								LibraryResource record = (LibraryResource) e.getNewValue();
								libraryBrowser.setComandButtonEnabled(false);
								openLibraryResource(record);
							}
						}
					});
				LibraryBrowser.fireHelpEvent = true;
				libraryBrowser.addPropertyChangeListener("help", new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent e) {
						showHelp();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return libraryBrowser;
	}
	
	private void openLibraryResource(LibraryResource record) {
		boolean loadFailed = false;
		try {
			libraryBrowser.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			String target = record.getAbsoluteTarget();
			if (!ResourceLoader.isHTTP(target)) {
				target = ResourceLoader.getURIPath(XML.getResolvedPath(record.getTarget(), record.getBasePath()));
			}
			// download comPADRE targets to osp cache
			if (target.indexOf("document/ServeFile.cfm?") >= 0) {
				String fileName = record.getProperty("download_filename");
				try {
					target = ResourceLoader.downloadToOSPCache(target, fileName, false).toURI().toString();
				} catch (Exception ex) {
					loadFailed = true;
				}
			}
			if (target == null) {
				loadFailed = true;
			}
			if (loadFailed) {
				String name = record.getName();
				if (name == null || "".equals(name))
					name = "Unknown";
				String s = "No resource could be downloaded for node " + name;
				JOptionPane.showMessageDialog(libraryBrowser, s, "Error",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			if (!isZip(target)) {
	 			String msg = XML.getName(target)+ " is not a zip file.";
	 		  JOptionPane.showMessageDialog(frame, msg, "Wrong File Type", 
	 		  		JOptionPane.WARNING_MESSAGE);  
	 		  return;
			}
			else {
				// check target to make sure it is a readable zip file
				Map<String, ZipEntry> contents = ResourceLoader.getZipContents(target, true);
				if (contents.isEmpty()) {
		 			String msg = XML.getName(target)+ " is empty.";
		 		  JOptionPane.showMessageDialog(frame, msg, "Empty File", 
		 		  		JOptionPane.WARNING_MESSAGE);
		 		  return;
				}
//				loadIntoEditor(target);
				new AsyncLoader(target).executeAsync();
				return;
			}
		} finally {
			libraryBrowser.setCursor(Cursor.getDefaultCursor());
		}
	}
	
	private boolean loadIntoEditor(String path) {
		Map<String, ZipEntry> contents = ResourceLoader.getZipContents(path, true);
		// extract the zip file contents
		xmlFiles.clear();
		otherFiles.clear();
		for (String next : contents.keySet()) {
			String s = ResourceLoader.getURIPath(path + "!/" + next);
			if (next.endsWith(".trk") 
					|| next.endsWith(".xml")
					|| next.endsWith(".ejss")) {
				xmlFiles.add(s);
			} else {
				otherFiles.add(s);					
			}
		}
		contents = null;
		if (xmlFiles.isEmpty()) {
 			String msg = XML.getName(path)+ " contains no editable xml files.";
 		  JOptionPane.showMessageDialog(frame, msg, "No Editable Content", JOptionPane.WARNING_MESSAGE);  
			return false;
		}
		// remove file that will be edited so the original 
		// will not be included in the output zip
		editedFilePath = xmlFiles.remove(0);
		String text = ResourceLoader.getString(editedFilePath);
		setEditorText(text);
		libraryBrowser.setVisible(false);
		return true;
	}
	
	private boolean isZip(String path) {
		return path.toLowerCase().endsWith(".zip")
				|| path.toLowerCase().endsWith(".trz");
	}
	
	private boolean saveNewZip() {
		// choose zip file path and create list to fill
		ArrayList<File> toBeZipped = defineZipFilePath();
		if (toBeZipped == null)
			return false;
		
		// add files, including edited text, to list
		if (!prepareZipFiles(toBeZipped))
			return false;

		// compress the list of files with JarTool
		File target = new File(zipFilePath);
		boolean success = JarTool.compress(toBeZipped, target, null);
		
		// delete temp directory after short delay
		OSPRuntime.trigger(1000, (e) -> {
			ResourceLoader.deleteFile(new File(getTempDirectory()));
		});
		
		return success;
	}

	/**
	 * Writes the editor text string to a file.
	 *
	 * @param target the file
	 * @return the saved file, or null if failed
	 */
	private File saveEditorTextTo(File target) {
		if (target == null)
			return null;
		if (!(target.getParentFile().exists() || target.getParentFile().mkdirs()))
			return null;
		String path = target.getAbsolutePath();
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path));
			out.write(pane.getText());
			out.flush();
			out.close();
			return target;
		} catch (IOException ex) {
			return null;
		}
	}

	/**
	 * Uses a file chooser to define the zip file path.
	 *
	 * @return empty List<File> to fill with files to be zipped
	 */
	private ArrayList<File> defineZipFilePath() {
		// show file chooser to get directory and zip name
		AsyncFileChooser chooser = getFileChooser();
		int result = chooser.showSaveDialog(frame);
		if (result != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		File chooserFile = chooser.getSelectedFile();
		if (chooserFile.exists()) {
			// TODO: warn or cancel
		}
		if (!VideoIO.canWrite(chooserFile)) {
			return null;
		}

		// define zipFilePath and check for reserved characters, including spaces
		zipFilePath = chooserFile.getAbsolutePath();
		String ext = XML.getExtension(chooserFile.getName());

		// check for duplicate file if target extension not used
		if (!"zip".equalsIgnoreCase(ext)) {
			String newFilePath = XML.stripExtension(zipFilePath) + ".zip";
			if (!VideoIO.canWrite(new File(newFilePath)))
				return null;
			zipFilePath = newFilePath;
		}

		// return empty list
		return new ArrayList<File>() {
			@Override
			public boolean add(File f) {
				if (!contains(f))
					super.add(f);
				return true;
			}
		};
	}
	
	private AsyncFileChooser getFileChooser() {
		if (fileChooser == null) {
			File dir = (OSPRuntime.chooserDir == null) ? new File(OSPRuntime.getUserHome())
					: new File(OSPRuntime.chooserDir);
			fileChooser = new AsyncFileChooser(dir);
			FileFilter zipFilter = new FileFilter() {

				@Override
				public boolean accept(File f) {
					return f.isDirectory()
					|| "zip".equalsIgnoreCase(VideoIO.getExtension(f));
				}

				@Override
				public String getDescription() {
					return "ZIP files";
				}				
			};
			fileChooser.setDialogTitle("Save As");
			fileChooser.setAcceptAllFileFilterUsed(false);
			fileChooser.addChoosableFileFilter(zipFilter);
			fileChooser.setFileFilter(zipFilter);
			fileChooser.setMultiSelectionEnabled(false);
		}
		return fileChooser;
	}

	
	/**
	 * Adds files to the zip list
	 * 
	 * @param zipList the list of files to be zipped
	 */
	private boolean prepareZipFiles(ArrayList<File> zipList) {

		String tmpDir = getTempDirectory();
		// save edited file and add to the list
		int n = editedFilePath.indexOf("!");
		if (n == -1)
			return false;
		String subPath = editedFilePath.substring(n+2,  editedFilePath.length());
		File targetFile = new File(tmpDir, subPath);
		targetFile = saveEditorTextTo(targetFile);
		if (targetFile == null || !targetFile.exists())
			return false;
		zipList.add(targetFile);		

		for (String path : xmlFiles) {
			n = path.indexOf("!");
			if (n == -1)
				return false;
			subPath = path.substring(n+2,  path.length());
			targetFile = new File(tmpDir, subPath);
			if (!(targetFile.getParentFile().exists() || targetFile.getParentFile().mkdirs()))
				return false;
			targetFile = ResourceLoader.extract(path, targetFile);
			if (!targetFile.exists())
				return false;
			zipList.add(targetFile);
		}
		
		for (String path : otherFiles) {
			n = path.indexOf("!");
			if (n == -1)
				return false;
			subPath = path.substring(n+2,  path.length());
			targetFile = new File(tmpDir, subPath);
			if (!(targetFile.getParentFile().exists() || targetFile.getParentFile().mkdirs()))
				return false;
			targetFile = ResourceLoader.extract(path, targetFile);
			if (!targetFile.exists())
				return false;
			zipList.add(targetFile);
		}
		return true;
	}

	private String getTempDirectory() {
		if (tempDir == null) {
			tempDir = new File(System.getProperty("java.io.tmpdir"), 
					"ejss" + new Random().nextInt()).toString() + File.separator;
		}
		return tempDir;
	}

	public static void main(String[] args) {
		new LoaderTest();
	}
	
	class AsyncLoader extends AsyncSwingWorker {
		
		String path;
		
		AsyncLoader(String path) {
			super(frame, "Loading " + XML.getName(path), 10,
					progressInit, progress_done);
			this.path = path;
		}

		@Override
		public void initAsync() {
		}

		@Override
		public int doInBackgroundAsync(int progress) {
			Map<String, ZipEntry> contents = ResourceLoader.getZipContents(path, true);
			// extract the zip file contents
			xmlFiles.clear();
			otherFiles.clear();
			for (String next : contents.keySet()) {
				String s = ResourceLoader.getURIPath(path + "!/" + next);
				if (next.endsWith(".trk") 
						|| next.endsWith(".xml")
						|| next.endsWith(".ejss")) {
					xmlFiles.add(s);
				} else {
					otherFiles.add(s);					
				}
			}
			contents = null;
			if (xmlFiles.isEmpty()) {
	 			String msg = XML.getName(path)+ " contains no editable xml files.";
	 		  JOptionPane.showMessageDialog(frame, msg, "No Editable Content", JOptionPane.WARNING_MESSAGE);  
				return progress_done;
			}
			// remove file that will be edited so the original 
			// will not be included in the output zip
			editedFilePath = xmlFiles.remove(0);
			String text = ResourceLoader.getString(editedFilePath);
			setEditorText(text);
			return progress_done;
		}

		@Override
		public void doneAsync() {
			libraryBrowser.setVisible(false);
		}
	}
	
}
