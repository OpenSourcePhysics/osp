/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

/*
 * The org.opensourcephysics.media.core package defines the Open Source Physics
 * media framework for working with video and other media.
 *
 * Copyright (c) 2017  Douglas Brown and Wolfgang Christian.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * For additional information and documentation on Open Source Physics,
 * please see <http://www.opensourcephysics.org/>.
 */
package org.opensourcephysics.media.core;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.function.Function;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.media.gif.GifVideoType;
import org.opensourcephysics.media.mov.MovieFactory;
import org.opensourcephysics.media.mov.MovieVideoI;
import org.opensourcephysics.media.mov.MovieVideoType;
import org.opensourcephysics.tools.FontSizer;
import org.opensourcephysics.tools.ResourceLoader;

import javajs.async.AsyncFileChooser;

/**
 * This provides static methods for managing video and text input/output.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class VideoIO {

	public static class SingleExtFileFilter extends FileFilter {

		private String ext;
		protected String desc;

		public SingleExtFileFilter(String ext, String desc) {
			this.ext = ext;
			this.desc = desc;
		}

		/**
		 * Accepts directories and files with extensions specified in constructor
		 * 
		 * @param f the file
		 * @return true if accepted
		 */
		@Override
		public boolean accept(File f) {
			return (f != null && accept(f, true));
		}

		/**
		 * BH fix for no need to check directories if a remote path or SwingJS temp file
		 * 
		 * @param f
		 * @param checkIfDir
		 * @return
		 */
		public boolean accept(File f, boolean checkIfDir) {
			return (checkIfDir && f.isDirectory()
				|| (ext != null && ext.equalsIgnoreCase(VideoIO.getExtension(f))));
		}

		@Override
		public String getDescription() {
			return desc;
		}

	}

	// static constants
	public static final String[] JS_VIDEO_EXTENSIONS = { "ogg", "mov", "mp4" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	public static final String DEFAULT_PREFERRED_EXPORT_EXTENSION = "mp4"; //$NON-NLS-1$


	public static MovieVideoType getMovieType(String extension) {
		if (!MovieFactory.hasVideoEngine())
			return null;
		MovieVideoType mtype = null;
		if (videoTypes == null) {
			System.out.println("????"); //$NON-NLS-1$
		}
		synchronized (videoTypes) {
			for (VideoType next : videoTypes) {
				if (next instanceof MovieVideoI) {
					mtype = (MovieVideoType) next;
					break;
				}
			}
			if (extension == null || mtype == null) {
				return mtype;
			}
			String id = mtype.getDefaultExtension();
			if (id != null && id.indexOf(extension) > -1)
				return mtype;
			return (MovieVideoType) checkVideoFilter(mtype, extension);
		}
	}

	private static VideoType checkVideoFilter(VideoType mtype, String extension) {
		VideoFileFilter[] filters = mtype.getFileFilters();
		for (VideoFileFilter filter : filters) {
			if (filter.extensions != null) {
				for (String s : filter.extensions)
					if (s.indexOf(extension) > -1)
						return mtype;
			}
		}
		return null;
	}

	// static fields
	protected static AsyncFileChooser chooser;
	protected static FileFilter imageFileFilter;
	protected static ArrayList<VideoType> videoTypes;
	protected static VideoFileFilter videoFileFilter;
	protected static Collection<VideoFileFilter> singleVideoTypeFilters = new TreeSet<VideoFileFilter>();
	protected static String defaultXMLExt = "xml"; //$NON-NLS-1$
	private static boolean canceled;
	protected static String preferredExportExtension = DEFAULT_PREFERRED_EXPORT_EXTENSION;

	static {
		videoTypes = new ArrayList<VideoType>();
		videoFileFilter = new VideoFileFilter();
		 // add movie types by instantiating MovieFactory
		MovieFactory.hasVideoEngine();
		// add other video types
		addVideoType(new GifVideoType());
		VideoFileFilter filter = new VideoFileFilter("jpg", //$NON-NLS-1$
				new String[] { "jpg", "jpeg" }); //$NON-NLS-1$ //$NON-NLS-2$
		addVideoType(new ImageVideoType(filter));
		filter = new VideoFileFilter("png", new String[] { "png" }); //$NON-NLS-1$ //$NON-NLS-2$
		addVideoType(new ImageVideoType(filter));
		imageFileFilter = new SingleExtFileFilter(null, MediaRes.getString("VideoIO.ImageFileFilter.Description")) { //$NON-NLS-1$
			@Override
			public boolean accept(File f, boolean checkDir) {
				String ext = VideoIO.getExtension(f); 
				return (checkDir && f.isDirectory()
						|| "jpg".equalsIgnoreCase(ext) //$NON-NLS-1$
						|| "gif".equalsIgnoreCase(ext));  //$NON-NLS-1$
			}

		};
	}

	/**
	 * protected constructor to discourage instantiation
	 */
	protected VideoIO() {

		/** empty block */
	}

	/**
	 * Gets the extension of a file.
	 *
	 * @param file the file
	 * @return the extension of the file
	 */
	public static String getExtension(File file) {
		return XML.getExtension(file.getName());
	}

	/**
	 * Gets the file chooser.
	 *
	 * @return the file chooser
	 */
	public static AsyncFileChooser getChooser() {
		if (chooser == null) {
			File dir = (OSPRuntime.chooserDir == null) ? new File(OSPRuntime.getUserHome())
					: new File(OSPRuntime.chooserDir);
			chooser = new AsyncFileChooser(dir);
		}
		FontSizer.setFonts(chooser, FontSizer.getLevel());
		return chooser;
	}

	/**
	 * Sets the default xml extension used when saving data.
	 *
	 * @param ext the default extension
	 */
	public static void setDefaultXMLExtension(String ext) {
		defaultXMLExt = ext;
	}

	/**
	 * Gets the path relative to the user directory.
	 *
	 * @param absolutePath the absolute path
	 * @return the relative path
	 */
	public static String getRelativePath(String absolutePath) {
		if ((absolutePath.indexOf("/") == -1) && (absolutePath.indexOf("\\") == -1)) { //$NON-NLS-1$ //$NON-NLS-2$
			return absolutePath;
		}
		if (ResourceLoader.isHTTP(absolutePath)) {
			return absolutePath;
		}
		String path = absolutePath;
		String relativePath = ""; //$NON-NLS-1$
		boolean validPath = false;
		// relative to user directory
		String base = System.getProperty("user.dir"); //$NON-NLS-1$
		if (base == null) {
			return path;
		}
		for (int j = 0; j < 3; j++) {
			if (j > 0) {
				// move up one level
				int k = base.lastIndexOf("\\"); //$NON-NLS-1$
				if (k == -1) {
					k = base.lastIndexOf("/"); //$NON-NLS-1$
				}
				if (k != -1) {
					base = base.substring(0, k);
					relativePath += "../"; //$NON-NLS-1$
				} else {
					break; // no more levels!
				}
			}
			if (path.startsWith(base)) {
				path = path.substring(base.length() + 1);
				// replace backslashes with forward slashes
				int i = path.indexOf("\\"); //$NON-NLS-1$
				while (i != -1) {
					path = path.substring(0, i) + "/" + path.substring(i + 1); //$NON-NLS-1$
					i = path.indexOf("\\"); //$NON-NLS-1$
				}
				relativePath += path;
				validPath = true;
				break;
			}
		}
		if (validPath) {
			return relativePath;
		}
		return path;
	}

	/**
	 * test executing shell commands
	 */
	public static void testExec() {
//  	System.getProperties().list(System.out);
//    // get java vm extensions folder
//    String extFolder = XML.forwardSlash(System.getProperty("java.ext.dirs")); //$NON-NLS-1$
//    // keep only first folder listed
//    String sep = System.getProperty("path.separator");
//    if (extFolder.indexOf(sep) > -1) {
//    	extFolder = extFolder.substring(0, extFolder.indexOf(sep));
//    }
//    extFolder = extFolder+"/"; //$NON-NLS-1$
//    // get xuggle folder and jar names
//    String xuggleHome = System.getenv("XUGGLE_HOME"); //$NON-NLS-1$
//  	String xuggleFolder = XML.forwardSlash(xuggleHome)+"/share/java/jars/"; //$NON-NLS-1$
//    String[] jarNames = {"xuggle-xuggler.jar","slf4j-api.jar", //$NON-NLS-1$ //$NON-NLS-2$
//    		"logback-core.jar","logback-classic.jar"}; //$NON-NLS-1$ //$NON-NLS-2$
//  	String shellCmd = "#!/bin/bash\nsudo cp "+xuggleFolder+"xuggle-xuggler.jar "+extFolder+"xuggle-copy.jar";        
//  	shellCmd = "#!/bin/bash\ncp "+xuggleFolder+"xuggle-xuggler.jar ~/junk.jar";        
//  	String home = OSPRuntime.getUserHome();
//  	String fileName = home+"/copyXuggle.sh";
//    try {
//      File file = new File(fileName);
//      FileOutputStream stream = new FileOutputStream(file);
//      java.nio.charset.Charset charset = java.nio.charset.Charset.forName("UTF-8"); //$NON-NLS-1$
//      Writer out = new OutputStreamWriter(stream, charset);
//      Writer output = new BufferedWriter(out);
//      output.write(shellCmd);
//      output.flush();
//      output.close();
//      Runtime.getRuntime().exec("chmod +x "+fileName);
//      
//      // open a terminal and write to it
//      String[] cmd = {"gnome-terminal", "cd ~/Tracker\n"};
//      Process process = Runtime.getRuntime().exec(cmd);
//      new Thread(new StreamPiper(process.getErrorStream(), System.err)).start();
//      new Thread(new StreamPiper(process.getInputStream(), System.out)).start();
////      Writer stdin = new OutputStreamWriter(process.getOutputStream());
////      stdin.write(shellCmd);
////      stdin.write("gnome-terminal&\n");
////      stdin.write("xterm&\n");
////      stdin.write("cd ~/Tracker\n");
////      stdin.write("dir\n");
////      stdin.write("ls\n");
////      stdin.close();
//
//      final int exitVal = process.waitFor();
//      System.out.println("Exit value: " + exitVal);
//    } catch(Exception ex) {
//      ex.printStackTrace();
//    }
//      	
	}

	/**
	 */
	public static class StreamPiper implements Runnable {

		private final InputStream input;
		private final OutputStream output;

		/**
		 * @param in
		 * @param out
		 */
		public StreamPiper(InputStream in, OutputStream out) {
			input = in;
			output = out;
		}

		@Override
		public void run() {
			try {
				final byte[] buffer = new byte[1024];
				for (int count = 0; (count = input.read(buffer)) >= 0;) {
					output.write(buffer, 0, count);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Copies a source file to a target file.
	 *
	 * @param inFile  the source
	 * @param outFile the target
	 * @return true if successfully copied
	 */
	public static boolean copyFile(File inFile, File outFile) {
		byte[] buffer = new byte[100000];
		try {
			InputStream in = new FileInputStream(inFile);
			OutputStream out = new FileOutputStream(outFile);
			while (true) {
				synchronized (buffer) {
					int amountRead = in.read(buffer);
					if (amountRead == -1) {
						break;
					}
					out.write(buffer, 0, amountRead);
				}
			}
			in.close();
			out.close();
			outFile.setLastModified(inFile.lastModified());
		} catch (IOException ex) {
			return false;
		}
		return true;
	}

	/**
	 * Returns the currently supported video file extensions
	 *
	 * @return an array of extensions
	 */
	public static String[] getVideoExtensions() {
		return videoFileFilter.getExtensions();
	}

	/**
	 * Gets the preferred file extension for video exports.
	 *
	 * @return the preferred extension
	 */
	public static String getPreferredExportExtension() {
		return preferredExportExtension;
	}

	/**
	 * Gets the preferred file extension for video exports.
	 *
	 * @param extension the preferred extension
	 */
	public static void setPreferredExportExtension(String extension) {
		if (extension != null && extension.length() > 1)
			preferredExportExtension = extension;
	}

	/**
	 * Adds a video type to the list of available types
	 *
	 * @param type the video type
	 */
	public static void addVideoType(VideoType type) {
		if (type != null) {
			boolean hasType = false;
			for (VideoType next : videoTypes) {
				if (next.getDescription().equals(type.getDescription()) && next.getClass() == type.getClass()) {
					hasType = true;
				}
			}
			if (!hasType) {
				videoTypes.add(type);
				VideoFileFilter filter = type.getDefaultFileFilter();
				if (filter != null && filter.extensions != null) {
					singleVideoTypeFilters.add(filter);
				}
			}
		}
	}

	/**
	 * Returns the first registered video type corresponding to a class name and/or
	 * extension. Strings are case-insensitive.
	 *
	 * @param typeName one of the predefined video type names (may be null)
	 * @param extension the extension (may be null)
	 * @return the VideoType, or null if none found
	 */
	public static VideoType getVideoType(String typeName, String extension) {
		if (typeName == null && extension == null)
			return null;
		ArrayList<VideoType> candidates = new ArrayList<VideoType>();
		synchronized (videoTypes) {
			// first pass: check type names
			if (typeName == null) {
				candidates.addAll(videoTypes);
			} else {
				for (VideoType next : videoTypes) {
					if (next.getTypeName()==typeName)
						candidates.add(next);
				}
			}
			if (extension == null) {
				if (candidates.isEmpty())
					return null;
				return candidates.get(0);
			}
			// second pass: compare with default extension
			extension = extension.toLowerCase();
			for (VideoType next : candidates) {
				String id = next.getDefaultExtension();
				if (id != null && id.indexOf(extension) > -1)
					return next;
			}
			// third pass: compare with all extensions
			for (VideoType next : candidates) {
				if (checkVideoFilter(next, extension) != null)
					return next;
			}
		}
		return null;
	}

	/**
	 * Gets an array of video types that can open files with a given extension.
	 *
	 * @param ext the extension
	 * @return the video types
	 */
	private static ArrayList<VideoType> getVideoTypesForExtension(String ext) {
		ext = ext.toLowerCase();
		ArrayList<VideoType> found = new ArrayList<VideoType>();
		// first add types for which ext is the default extension
		ArrayList<VideoType> vidTypes = getVideoTypes(false);
		for (VideoType next : vidTypes) {
			String id = next.getDefaultExtension();
			if (id != null && id.indexOf(ext) > -1)
				found.add(next);
		}
		// then add types for which ext is accepted
		for (VideoType next : vidTypes) {
			VideoFileFilter[] filters = next.getFileFilters();
			for (VideoFileFilter filter : filters) {
				if (filter.extensions != null) {
					for (String s : filter.extensions)
						if (s.indexOf(ext) > -1 && !found.contains(next))
							found.add(next);
				}
			}
		}
		return found;
	}

	/**
	 * Gets an array of available video types
	 *
	 * @param canRecord  true if we need a recorder (TrackerIO and VideoGrabber only)
	 * @return the video types
	 */
	public static ArrayList<VideoType> getVideoTypes(boolean canRecord) {
		ArrayList<VideoType> available = new ArrayList<VideoType>();
		for (VideoType next : videoTypes) {
			if (!canRecord || OSPRuntime.canRecordMovieFiles && next.canRecord())
				available.add(next);
		}
		return available;
	}
	
	/**
	 * Cancels the current operation when true.
	 *
	 * @param cancel true to cancel
	 */
	public static void setCanceled(boolean cancel) {
		canceled = cancel;
	}

	/**
	 * Determines if the current operation is canceled.
	 *
	 * @return true if canceled
	 */
	public static boolean isCanceled() {
		return canceled;
	}


	/**
	 * Returns a video from a specified path. May return null.
	 *
	 * @param path    the path
	 * @param vidType a requested video type (may be null)
	 * @return the video
	 */
	public static Video getVideo(String path, VideoType vidType) {
		return getVideo(path, null, vidType);
	}
	public static Video getVideo(String path, String basePath, VideoType vidType) {
		// BH! 2020.04.20 from TrackerIO, but equally useful here.
		if (path.startsWith("file:")) //$NON-NLS-1$
			path = ResourceLoader.getNonURIPath(path);
		OSPLog.fine("path: " + path + " type: " + vidType); //$NON-NLS-1$ //$NON-NLS-2$
		Video video = null;
		VideoIO.setCanceled(false);

		// try first with specified VideoType, if any
		if (vidType != null) {
			OSPLog.finest("preferred type " + vidType.getClass().getSimpleName() //$NON-NLS-1$
					+ " " + vidType.getDescription()); //$NON-NLS-1$
			video = vidType.getVideo(path, basePath);
			if (video != null)
				return video;
		}
		if (VideoIO.isCanceled())
			return null;

		// try other allowed video types for the file extension
		String extension = XML.getExtension(path);
		ArrayList<VideoType> allTypes = getVideoTypesForExtension(extension);
//		ArrayList<VideoType> allowedTypes = new ArrayList<VideoType>();
//		boolean skipMovies = !MovieFactory.hasVideoEngine();
//		for (int i = 0; i < allTypes.size(); i++) {
//			if (skipMovies && allTypes.get(i) instanceof MovieVideoType)
//				continue;
//			allowedTypes.add(allTypes.get(i));
//		}
		for (VideoType next : allTypes) {
			OSPLog.finest("trying type " + next.getClass().getSimpleName() //$NON-NLS-1$
					+ " " + next.getDescription()); //$NON-NLS-1$
			video = next.getVideo(path, basePath);
			if (VideoIO.isCanceled())
				return null;
			if (video != null)
				return video;
		}
		return null;
	}

//	/**
//	 * Returns a video from a specified path using a video engine chosen by user.
//	 * May return null.
//	 *
//	 * @param path      the path
//	 * @param engines   array of available video types
//	 * @param component a JComponent to display with the text (may be null)
//	 * @param frame     owner of the dialogs (may be null)
//	 * @return the video
//	 */
//	public static Video getVideoFromDialog(String path, ArrayList<VideoType> engines, JComponent component, JFrame frame) 
//	 /** @j2sIgnore*/
//	{
//		// provide immediate way to open with other engines
//		String engine = MovieFactory.getMovieVideoName(true);
//		String message = MediaRes.getString("VideoIO.Dialog.TryDifferentEngine.Message1") + " (" + engine + ")."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		message += "\n" + MediaRes.getString("VideoIO.Dialog.TryDifferentEngine.Message2"); //$NON-NLS-1$ //$NON-NLS-2$
//		message += "\n\n" + MediaRes.getString("VideoIO.Dialog.Label.Path") + ": " + path; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		ArrayList<String> optionList = new ArrayList<String>();
//		for (VideoType next : engines) {
//			if (next instanceof MovieVideoType) {
//				// BH TODO fix this ref to Xuggle
//				optionList.add(MediaRes.getString("XuggleVideoType.Description")); //$NON-NLS-1$
//			}
//		}
//		optionList.add(MediaRes.getString("Dialog.Button.Cancel")); //$NON-NLS-1$
//		Object[] options = optionList.toArray(new String[optionList.size()]);
//		// assemble message panel with text and checkbox
//		JPanel messagePanel = new JPanel(new BorderLayout());
//		JTextArea textArea = new JTextArea();
//		textArea.setText(message);
//		textArea.setOpaque(false);
//		textArea.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 4));
//		messagePanel.add(textArea, BorderLayout.NORTH);
//		if (component != null) {
//			component.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
//			messagePanel.add(component, BorderLayout.SOUTH);
//		}
//		int response = JOptionPane.showOptionDialog(frame, messagePanel,
//				MediaRes.getString("VideoClip.Dialog.BadVideo.Title"), //$NON-NLS-1$
//				JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
//		if (response >= 0 && response < options.length - 1) {
//			VideoType desiredType = engines.get(response);
//			Video video = getVideo(path, desiredType);
//			if (video == null && !VideoIO.isCanceled()) {
//				// failed again
//				JOptionPane.showMessageDialog(frame,
//						MediaRes.getString("VideoIO.Dialog.BadVideo.Message") + "\n\n" + path, //$NON-NLS-1$ //$NON-NLS-2$
//						MediaRes.getString("VideoClip.Dialog.BadVideo.Title"), //$NON-NLS-1$
//						JOptionPane.WARNING_MESSAGE);
//			}
//			return video;
//		}
//		return null;
//	}
//
	/**
	 * Returns a clone of the specified video.
	 *
	 * @param video the video to clone
	 * @return the clone
	 */
	public static Video clone(Video video) {
		if (video == null) {
			return null;
		}
		// ImageVideo is special case since may have pasted images
		if (video instanceof ImageVideo) {
			ImageVideo oldVid = (ImageVideo) video;
			ImageVideo newVid = new ImageVideo(oldVid.getImages());
			newVid.rawImage = newVid.images[0];
			Collection<Filter> filters = video.getFilterStack().getFilters();
			if (filters != null) {
				Iterator<Filter> it = filters.iterator();
				while (it.hasNext()) {
					Filter filter = it.next();
					newVid.getFilterStack().addFilter(filter);
				}
			}
			return newVid;
		}
		XMLControl control = new XMLControlElement(video);
		return (Video) new XMLControlElement(control).loadObject(null);
	}

	/**
	 * A Stop-gap method to allow Java-only functionality.
	 * 
	 * @param type
	 * @return
	 */
	@Deprecated
	public static File[] getChooserFiles(String type) {
		return getChooserFilesAsync(type, null);
	}

	/**
	 * Displays a file chooser and returns the chosen files.
	 *
	 * @param type may be "open", "open video", "save", "insert image"
	 * @param processFiles asynchronous follower method
	 * @return the files, or null if no files chosen or asynchronous
	 */
	public static File[] getChooserFilesAsync(String type, Function<File[], Void> processFiles) {
		AsyncFileChooser chooser = getChooser();
		chooser.setMultiSelectionEnabled(false);
		chooser.setAcceptAllFileFilterUsed(true);

		Runnable resetChooser = new Runnable() {

			@Override
			public void run() {
				chooser.resetChoosableFileFilters();
				if (processFiles != null)
					chooser.setSelectedFile(null); // $NON-NLS-1$
			}

		};

		Runnable okOpen = new Runnable() {

			@Override
			public void run() {
				File file = chooser.getSelectedFile();
				resetChooser.run();
				if (processFiles != null)
					processFiles.apply(new File[] { file });
			}

		};

		Runnable okSave = new Runnable() {

			@Override
			public void run() {
				File file = chooser.getSelectedFile();
				resetChooser.run();
//			if (canWrite(file))
				if (processFiles != null)
					processFiles.apply(new File[] { file });
			}

		};
		if (type.toLowerCase().equals("open")) { // open any file //$NON-NLS-1$
			chooser.addChoosableFileFilter(videoFileFilter);
			chooser.setFileFilter(chooser.getAcceptAllFileFilter());
			chooser.showOpenDialog(null, okOpen, resetChooser);
		} else if (type.toLowerCase().equals("open video")) { // open video //$NON-NLS-1$
			chooser.addChoosableFileFilter(videoFileFilter);
			chooser.showOpenDialog(null, okOpen, resetChooser);
		} else if (type.toLowerCase().equals("save")) { // save any file //$NON-NLS-1$
			// note this sets no file filters but does include acceptAll
			// also sets file name to "untitled"
			String filename = MediaRes.getString("VideoIO.FileName.Untitled"); //$NON-NLS-1$
			chooser.setSelectedFile(new File(filename + "." + defaultXMLExt)); //$NON-NLS-1$
			chooser.showSaveDialog(null, okSave, resetChooser);
		} else if (type.toLowerCase().equals("insert image")) { //$NON-NLS-1$
			chooser.setMultiSelectionEnabled(true);
			chooser.setAcceptAllFileFilterUsed(false);
			chooser.addChoosableFileFilter(imageFileFilter);
			chooser.setSelectedFile(new File("")); //$NON-NLS-1$
			chooser.showSaveDialog(null, okSave, resetChooser);
		} else {
			return null;
		}
		// Async will return null here
		File ret = processChoose(chooser, null, processFiles != null);
		return (ret == null ? null : new File[] { ret });
	}

	/**
	 * Also used by TrackerIO
	 * 
	 * @param chooser
	 * @param ret
	 * @param isAsync
	 * @return
	 */
	protected static File processChoose(AsyncFileChooser chooser, File ret, boolean isAsync) {
		if (isAsync)
			return null;
		if (ret == null && chooser.getSelectedOption() == JFileChooser.APPROVE_OPTION)
			ret = chooser.getSelectedFile();
		chooser.setSelectedFile(null);
		return ret;
	}

	/**
	 * Loads the specified video panel from a file selected with a chooser or a
	 * video from a specified file into a VideoPanel. If file is null, a file
	 * chooser is displayed.
	 *
	 * @param file     the file to be loaded
	 * @param vidPanel the video panel
	 * @return the file opened
	 */
	public static void openVideoPanelFileAsync(File file, VideoPanel vidPanel) {
		if (file != null) {
			openVideoPanelFileSync(file, vidPanel);
			return;
		}
		getChooserFilesAsync("open", new Function<File[], Void>() {//$NON-NLS-1$

			@Override
			public Void apply(File[] files) {
				File file = (files == null ? null : files[0]);
				if (file != null) {
					openVideoPanelFileSync(file, vidPanel);
				}
				return null;
			}

		}); 
	}
		
	private static void openVideoPanelFileSync(File file, VideoPanel vidPanel) {
		// BH Can this be a directory? user selects that?
		if (videoFileFilter.accept(file, true)) { // load video
			ArrayList<VideoType> types = getVideoTypes(false);
			Video video = null;
			String name = file.getAbsolutePath();
			for (int i = 0; i < types.size(); i++) {
				VideoType type = types.get(i);
				if (type.accepts(file)) {
					video = type.getVideo(name, null);
					if (video != null) {
						OSPLog.info(file.getName() + " opened as type " + type.getDescription()); //$NON-NLS-1$
						break;
					}
					OSPLog.info(file.getName() + " failed as type " + type.getDescription()); //$NON-NLS-1$
				}
			}
			if (video == null) {
				JOptionPane.showMessageDialog(vidPanel, MediaRes.getString("VideoIO.Dialog.BadVideo.Message") + //$NON-NLS-1$
						ResourceLoader.getNonURIPath(XML.getAbsolutePath(file)));
				return;
			}
			vidPanel.setVideo(video);
			vidPanel.repaint();
		} else { // load data
			XMLControlElement control = new XMLControlElement();
			control.read(file.getAbsolutePath());
			Class<?> type = control.getObjectClass();
			if (VideoPanel.class.isAssignableFrom(type)) {
				vidPanel.setDataFile(file);
				control.loadObject(vidPanel);
			} else if (control.failedToRead()) {
				JOptionPane.showMessageDialog(vidPanel, MediaRes.getString("VideoIO.Dialog.BadFile.Message") + //$NON-NLS-1$
						ResourceLoader.getNonURIPath(XML.getAbsolutePath(file)));
			} else {
				JOptionPane.showMessageDialog(vidPanel, "\"" + file.getName() + "\" " + //$NON-NLS-1$ //$NON-NLS-2$
						MediaRes.getString("VideoIO.Dialog.XMLMismatch.Message"), //$NON-NLS-1$
						MediaRes.getString("VideoIO.Dialog.XMLMismatch.Title"), JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$
				return;
			}
			vidPanel.changed = false;
		}

	}

	/**
	 * Writes VideoPanel data to the specified file. If the file is null it brings
	 * up a chooser.
	 *
	 * @param file     the file to write to
	 * @param vidPanel the video panel
	 * @return the file written to, or null if not written
	 */
	public static File save(File file, VideoPanel vidPanel) {
		return save(file, vidPanel, MediaRes.getString("VideoIO.Dialog.SaveAs.Title")); //$NON-NLS-1$
	}

	/**
	 * Writes VideoPanel data to the specified file. If the file is null it displays
	 * a filechooser.
	 *
	 * @param file         the file to write to
	 * @param vidPanel     the video panel
	 * @param chooserTitle the title for the filechooser
	 * @return the file written to, or null if not written
	 */
	public static File save(File file, VideoPanel vidPanel, String chooserTitle) {
		if (file == null) {
			Video video = vidPanel.getVideo();
			JFileChooser chooser = getChooser();
			chooser.removeChoosableFileFilter(videoFileFilter);
			chooser.removeChoosableFileFilter(imageFileFilter);
			chooser.setDialogTitle(chooserTitle);
			String filename = MediaRes.getString("VideoIO.FileName.Untitled"); //$NON-NLS-1$
			if (vidPanel.getFilePath() != null) {
				filename = XML.stripExtension(vidPanel.getFilePath());
			} else if ((video != null) && (video.getProperty("name") != null)) { //$NON-NLS-1$
				filename = (String) video.getProperty("name"); //$NON-NLS-1$
				int i = filename.lastIndexOf("."); //$NON-NLS-1$
				if (i > 0) {
					filename = filename.substring(0, i);
				}
			}
			file = new File(filename + "." + defaultXMLExt); //$NON-NLS-1$
			String parent = XML.getDirectoryPath(filename);
			if (!parent.equals("")) { //$NON-NLS-1$
				XML.createFolders(parent);
				chooser.setCurrentDirectory(new File(parent));
			}
			chooser.setSelectedFile(file);
			// BH no problem in JavaScript; it will default to standard JavaScript Confirm
			int result = chooser.showSaveDialog(vidPanel);
			if (result == JFileChooser.APPROVE_OPTION) {
				file = chooser.getSelectedFile();
				if (!defaultXMLExt.equals(getExtension(file))) {
					filename = XML.stripExtension(file.getPath());
					file = new File(filename + "." + defaultXMLExt); //$NON-NLS-1$
				}
				if (file.exists()) {
					int selected = JOptionPane.showConfirmDialog(vidPanel, " \"" + file.getName() + "\" " //$NON-NLS-1$ //$NON-NLS-2$
							+ MediaRes.getString("VideoIO.Dialog.FileExists.Message"), //$NON-NLS-1$
							MediaRes.getString("VideoIO.Dialog.FileExists.Title"), //$NON-NLS-1$
							JOptionPane.OK_CANCEL_OPTION);
					if (selected != JOptionPane.OK_OPTION) {
						return null;
					}
				}
				vidPanel.setDataFile(file);
			} else {
				return null;
			}
		}
		Video video = vidPanel.getVideo();
		if (video != null) {
			video.setProperty("base", XML.getDirectoryPath(XML.getAbsolutePath(file))); //$NON-NLS-1$
			if (video instanceof ImageVideo) {
				((ImageVideo) video).saveInvalidImages();
			}
		}
		XMLControl xmlControl = new XMLControlElement(vidPanel);
		xmlControl.write(file.getAbsolutePath());
		vidPanel.changed = false;
		return file;
	}

	/**
	 * Writes an image to a file.
	 * 
	 * @param image    the image to write
	 * @param filePath the path to write to, including extension (png, jpg, gif)
	 * @return the file written, or null if failed
	 */
	public static File writeImageFile(BufferedImage image, String filePath) {
		if (image == null)
			return null;
		File file = new File(filePath);
		File parent = file.getParentFile();
		if (!parent.exists()) {
			parent.mkdirs();
		}
		String ext = XML.getExtension(filePath);
		try {
			if (ImageIO.write(image, ext, file))
				return file;
		} catch (IOException ex) {
			OSPLog.finer(ex.toString());
		}
		return null;
	}

}

/*
 * Open Source Physics software is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.
 * 
 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be
 * released under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston MA 02111-1307 USA or view the license online at
 * http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
