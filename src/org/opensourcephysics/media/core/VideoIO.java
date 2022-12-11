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

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
//import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.zip.ZipEntry;

import javax.imageio.ImageIO;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;

import org.opensourcephysics.controls.ControlsRes;
import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.desktop.OSPDesktop;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.media.gif.GifVideoType;
import org.opensourcephysics.media.mov.MovieFactory;
import org.opensourcephysics.media.mov.MovieVideoI;
import org.opensourcephysics.media.mov.MovieVideoType;
import org.opensourcephysics.tools.FontSizer;
import org.opensourcephysics.tools.LibraryBrowser;
import org.opensourcephysics.tools.ResourceLoader;

import javajs.async.AsyncDialog;
import javajs.async.AsyncFileChooser;
import javajs.async.AsyncSwingWorker;
import javajs.util.VideoReader;

/**
 * This provides static methods for managing video and text input/output.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class VideoIO {

	public static final int PROGRESS_LOAD_INIT        = 0;
	public static final int PROGRESS_VIDEO_LOADING    = 10;
	public static final int PROGRESS_VIDEO_PROCESSING = 20;
	public static final int PROGRESS_VIDEO_READY      = 80;
	public static final int PROGRESS_COMPLETE         = 100;
	public static final int PROGRESS_VIDEO_CANCELED   = -999;

	// static constants
	public static final String[] JS_VIDEO_EXTENSIONS = { "ogg", "mov", "mp4" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	public static final String DEFAULT_PREFERRED_EXPORT_EXTENSION = "mp4"; //$NON-NLS-1$
	public static final String DEFAULT_VIDEO_EXTENSION = "jpg"; //$NON-NLS-1$
	public static final String[] KNOWN_VIDEO_EXTENSIONS = 
		{"mov", "flv", "mp4", "wmv", "avi", "mts",
			"m2ts", "mpg", "mod", "ogg", "dv" };
	public static final String VIDEO_CONVERSION_HELP_PATH = "https://physlets.org/tracker/converting_videos.html";
	
	/**
	 * delimiters TAB, SPACE, COMMA, SEMICOLON
	 */
	public static final String TAB = "\t", SPACE = " ", //$NON-NLS-1$ //$NON-NLS-2$
			COMMA = ",", SEMICOLON = ";"; //$NON-NLS-1$ //$NON-NLS-2$
	protected static String defaultDelimiter = TAB; // tab delimiter by default
	public static Map<String, String> delimiters = new TreeMap<String, String>();
	protected static String delimiter = defaultDelimiter;
	public static Map<String, String> customDelimiters = new TreeMap<String, String>();


	public static SingleExtFileFilter zipFileFilter, trkFileFilter, trzFileFilter;
	public static SingleExtFileFilter videoAndTrkFileFilter, txtFileFilter, jarFileFilter;
	public static SingleExtFileFilter delimitedTextFileFilter;
	protected static boolean dataCopiedToClipboard;
	private static ArrayList<String> filenamesToReload = new ArrayList<String>();


	/**
	 * VideoClip and VideoRecorder need persistent loaders that must later be asynchronously finalized.
	 * 
	 * @author hansonr
	 *
	 */
	public interface FinalizableLoader extends XML.NonStaticLoader {
		
		public void finalizeLoading();
		
		public boolean isFinalized();

	}

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

	public static class ZipImageVideoType extends ImageVideoType {

		// the image file filter
		private ImageVideoType imageVideoType;
		private static final String ZIP = "zip";
		
		public ZipImageVideoType(ImageVideoType type) {
			super(new VideoFileFilter(ZIP, new String[] { ZIP }) {
				@Override
				public boolean accept(File f, boolean checkIfDir) {
					if (!super.accept(f, checkIfDir))
						return false;
					String[] imagePaths = VideoIO.getZippedImagePaths(f.getAbsolutePath());
					return imagePaths != null;
				}
			});
			imageVideoType = type;
		}

	  @Override
	  public String getDescription() {
	  	String zipped = MediaRes.getString("ZipImageVideoType.Description.Zipped");
			String desc = zipped + " " + imageVideoType.getDescription(); //$NON-NLS-1$
			// substitute ".zip" for image extensions
			desc = desc.substring(0, desc.indexOf("(") + 1) + "." + ZIP + ")";
	  	return desc;
	  }
	  
		@Override
		public Video getVideo(String name, String basePath, XMLControl control) {
			String fullPath = basePath == null? name: basePath + "/" + name;
			if (VideoIO.zipFileFilter.accept(new File(fullPath))) {
				String[] imagePaths = VideoIO.getZippedImagePaths(fullPath); // all absolute
				if (imagePaths == null) {
					return null;
				}
				name = imagePaths[0];
				if (!getImageVideoType().accepts(new File(XML.getName(name)))) {
					return null;
				}
				basePath = null;				
				Video video = super.getVideo(name, basePath, control);
				if (video != null)
					video.setProperty("video_type", this); //$NON-NLS-1$
				return video;
			}
			return null;
		}
		
	  public String getImageExtension() {
	  	return imageVideoType.getDefaultExtension();
	  }
	  
	  public ImageVideoType getImageVideoType() {
	  	return imageVideoType;
	  }
	  
	}

	public static MovieVideoType getMovieType(String extension) {
		if (!MovieFactory.hasVideoEngine())
			return null;
		MovieVideoType mtype = null;
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
	protected static SingleExtFileFilter imageFileFilter, jpgFileFilter;
	protected static ArrayList<VideoType> videoTypes;
	protected static VideoFileFilter videoFileFilter;
	protected static Collection<VideoFileFilter> singleVideoTypeFilters = new TreeSet<VideoFileFilter>();
	protected static String defaultXMLExt = "xml"; //$NON-NLS-1$
	private static boolean canceled;
	protected static String preferredExportExtension = DEFAULT_PREFERRED_EXPORT_EXTENSION;
	public static boolean loadIncrementally;
	public static int incrementToLoad = 10;
	public static AsyncSwingWorker loader;

	static {
		videoTypes = new ArrayList<VideoType>();
		videoFileFilter = new VideoFileFilter();
		 // add movie types by instantiating MovieFactory
		MovieFactory.hasVideoEngine();
		// add other video types
		addVideoType(new GifVideoType());
		
		VideoFileFilter filter = new VideoFileFilter("jpg", //$NON-NLS-1$
				new String[] { "jpg", "jpeg" }); //$NON-NLS-1$ //$NON-NLS-2$	
		ImageVideoType vidType = new ImageVideoType(filter);
		addVideoType(vidType);
		addVideoType(new ZipImageVideoType(vidType));
		
		filter = new VideoFileFilter("png", new String[] { "png" }); //$NON-NLS-1$ //$NON-NLS-2$
		vidType = new ImageVideoType(filter);
		addVideoType(vidType);
		addVideoType(new ZipImageVideoType(vidType));
		
		imageFileFilter = new SingleExtFileFilter(null, MediaRes.getString("VideoIO.ImageFileFilter.Description")) { //$NON-NLS-1$
			@Override
			public boolean accept(File f, boolean checkDir) {
				String ext = VideoIO.getExtension(f); 
				return (checkDir && f.isDirectory()
						|| "jpg".equalsIgnoreCase(ext) //$NON-NLS-1$
						|| "jpeg".equalsIgnoreCase(ext) //$NON-NLS-1$
						|| "png".equalsIgnoreCase(ext) //$NON-NLS-1$
						|| "gif".equalsIgnoreCase(ext));  //$NON-NLS-1$
			}

		};
		jpgFileFilter = new SingleExtFileFilter(null, MediaRes.getString("ImageVideoType.JPGFileFilter.Description")) { //$NON-NLS-1$
			@Override
			public boolean accept(File f, boolean checkDir) {
				String ext = VideoIO.getExtension(f); 
				return (checkDir && f.isDirectory()
						|| "jpeg".equalsIgnoreCase(ext) //$NON-NLS-1$
						|| "jpg".equalsIgnoreCase(ext));  //$NON-NLS-1$
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
	 * Gets an array of image paths inside a zip file.
	 *
	 * @param path the path to the zip file OR to an image inside the zip file
	 * @return String[] with image paths in numerical order
	 */
	public static String[] getZippedImagePaths(String zipPath) {
		if (zipPath == null)
			return null;
		// keep only zip filepath if an image path is included
		int n = zipPath.indexOf("!");
		if (n > 0) {
			zipPath = zipPath.substring(0, n);
			if (zipPath.startsWith("jar:")) {
				zipPath = zipPath.substring(4, zipPath.length());
			}
		}
		if (zipFileFilter != null && !zipFileFilter.accept(new File(zipPath)))
			return null;
		
		// check if name requires reload
		String name = XML.getName(zipPath);
		boolean useCache = !filenamesToReload.remove(name);
		
		// get the map and look for images
		Map<String, ZipEntry>  map = ResourceLoader.getZipContents(zipPath, useCache);
		if (map == null)
			return null;
		String[] imagePaths = null;
		for (String next: map.keySet()) {
			if (next.contains("/") || next.contains("\\") || !imageFileFilter.accept(new File(next)))
				continue;
			String imagePath = zipPath + "!/" + next;
			imagePaths = getImageSequencePaths(imagePath, map.keySet());
			if (imagePaths != null && imagePaths.length > 1)
				break;
		}
		// return image paths only if 2 or more images are found
		boolean ret = imagePaths != null && imagePaths.length > 1;
		return ret? imagePaths: null;
	}
	
	/**
	 * Gets the delimiter for copied or exported data
	 *
	 * @return the delimiter
	 */
	public static String getDelimiter() {
		return delimiter;
	}

	/**
	 * Sets the delimiter for copied or exported data
	 *
	 * @param d the delimiter
	 */
	public static void setDelimiter(String d) {
		if (d != null)
			delimiter = d;
	}

	/**
	 * Gets the delimiters for copied or exported data
	 *
	 * @return the delimiter map
	 */
	public static Map<String, String> getDelimiters() {
		if (VideoIO.delimiters.isEmpty()) {
			VideoIO.delimiters.put(MediaRes.getString("VideoIO.Delimiter.Tab"), TAB); //$NON-NLS-1$
			VideoIO.delimiters.put(MediaRes.getString("VideoIO.Delimiter.Space"), SPACE); //$NON-NLS-1$
			VideoIO.delimiters.put(MediaRes.getString("VideoIO.Delimiter.Comma"), COMMA); //$NON-NLS-1$
			VideoIO.delimiters.put(MediaRes.getString("VideoIO.Delimiter.Semicolon"), SEMICOLON); //$NON-NLS-1$
		}
		return VideoIO.delimiters;
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
		FontSizer.setFonts(chooser);
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
//
//	private ArrayList<VideoType> found = new ArrayList<VideoType>();

	/**
	 * Gets an array of video types that can open files with a given extension.
	 *
	 * @param ext the extension
	 * @return the video types
	 */
	public static ArrayList<VideoType> getVideoTypesForPath(String path) {
		String ext = (path.indexOf(".") >= 0 ? XML.getExtension(path) : path).toLowerCase();
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
	public static ArrayList<VideoType> getVideoTypes(boolean mustBeWritable) {
		ArrayList<VideoType> available = new ArrayList<VideoType>();
		for (VideoType next : videoTypes) {
			if (!mustBeWritable || next.canRecord())
				available.add(next);
		}
		return available;
	}
	
	/**
	 * Adds a zip image file path to those requiring a reload rather than cached bytes.
	 * This is required when a zip file is overwritten, for example
	 *
	 * @param zipPath
	 */
  public static void requiresReload(String zipPath) {
  	filenamesToReload.add(XML.getName(zipPath));	
  }
    
	/**
	 * Cancels the current operation when true.
	 *
	 * @param cancel true to cancel
	 */
	public static void setCanceled(boolean cancel) {
		if (canceled == cancel)
			return;
		canceled = cancel;
		if (loader != null && cancel) {
			loader.cancelAsync();
			loader = null;			
		}
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
	 * Determines if a path ends with a know video extension.
	 *
	 * @param path the path
	 * @return true if in KNOWN_VIDEO_EXTENSIONS
	 */
	public static boolean isKnownVideoExtension(String path) {
		String ext = XML.getExtension(path.toLowerCase());
		if (ext == null)
			return false;
		for (int i = 0; i < KNOWN_VIDEO_EXTENSIONS.length; i++) {
			if (ext.equals(KNOWN_VIDEO_EXTENSIONS[i]))
				return true;
		}
		return false;
	}

	public static String getVideoCodec(String path) {
		String codec = null;
		// can only test local files?
		File localFile = ResourceLoader.download(path, null, false);
		if (localFile != null)
			path = localFile.getAbsolutePath();	
		if (codecMap.containsKey(path)) {
			return codecMap.get(path);
		} 
		try {
			VideoReader vr = new VideoReader(path);
			vr.getContents(false);
			codec = (vr.getCodec() == null? "unknown": vr.getCodec());
		} catch (IOException e) {
		}
		codecMap.put(path, codec);
		return codec;
	}
	
	private static HashSet<String> unsupportedPaths = new HashSet<>();

	public static void handleUnsupportedVideo(String path0, String ext, String codec, VideoPanel vidPanel, String why) {
		String path = (path0.startsWith("jar:file:/") ? path0.substring(9) : path0);
		if (unsupportedPaths.contains(path))
			return;
		unsupportedPaths.add(path);
		OSPLog.warning("VideoIO.handleUnsupportedVideo " + path + " from " + why);
		String message = //why + " " + path + "\n\n" + 
				(codec != null
				? MediaRes.getString("VideoIO.Dialog.ConvertVideo.Message.VideoCodec1") + " " + ext.toUpperCase() + " "
						+ MediaRes.getString("VideoIO.Dialog.ConvertVideo.Message.VideoCodec2") + " \"" + codec + "\"."
				: MediaRes.getString("VideoIO.Dialog.ConvertVideo.Message.VideoType") + " \"" + ext + "\".");
		if (codec == null && MovieFactory.xuggleNeeds32bitVM && vidPanel != null) {
			vidPanel.offerReloadVM(ext, message);
			return;
		}
		String helpLink = MediaRes.getString("VideoIO.Dialog.ConvertVideo.Message.MoreInfo") + "<br>" + "<a href=\""
				+ VIDEO_CONVERSION_HELP_PATH + "\">" + VIDEO_CONVERSION_HELP_PATH + "</a>";
		message += "<br><br>" + MediaRes.getString("VideoIO.Dialog.ConvertVideo.Message.Fix") + ":";
		message += "<ol>";

		if (ResourceLoader.isHTTP(path) || OSPRuntime.isJSTemp(path)) {
			message += "<li>" + MediaRes.getString("VideoIO.Dialog.ConvertVideo.Message.Download") + "</li>";
			message += "<li>" + MediaRes.getString("VideoIO.Dialog.ConvertVideo.Message.ConvertDownload") + "</li>";
			message += "<li>" + MediaRes.getString("VideoIO.Dialog.ConvertVideo.Message.Import") + "</li></ol>";
			message += helpLink;
			message += "<br><br>" + MediaRes.getString("VideoIO.Dialog.ConvertVideo.Message.DownloadNow");
			new AsyncDialog().showConfirmDialog(null, new EditorPaneMessage(message),
					MediaRes.getString("VideoIO.Dialog.UnsupportedVideo.Title"), JOptionPane.YES_NO_OPTION, (ev) -> {
						int sel = ev.getID();
						switch (sel) {
						case JOptionPane.YES_OPTION:
							// choose file and save resource
							String name = XML.getName(path);
							VideoIO.getChooserFilesAsync("save video " + name, //$NON-NLS-1$
									(files) -> {
										if (VideoIO.getChooser().getSelectedOption() == AsyncFileChooser.APPROVE_OPTION
												&& files != null) {
											ResourceLoader.downloadResourceFromDialog(path, files[0]);
										}
										return null;
									});
						}
					});
		} else {
			message += "<li>" + MediaRes.getString("VideoIO.Dialog.ConvertVideo.Message.Convert") + "</li>";
			message += "<li>" + MediaRes.getString("VideoIO.Dialog.ConvertVideo.Message.Import") + "</li></ol>";
			message += helpLink;
			new AsyncDialog().showMessageDialog(null, new EditorPaneMessage(message),
					MediaRes.getString("VideoIO.Dialog.UnsupportedVideo.Title"), 
					JOptionPane.INFORMATION_MESSAGE, (ev) -> {
					});
		}
	}

	/**
	 * Returns a video from a specified path. May return null.
	 *
	 * @param path    the path
	 * @param vidType a requested video type (may be null)
	 * @return the video
	 */
	public static Video getVideo(String path, VideoType vidType) {
		return getVideo(path, null, vidType, null);
	}
	
	public static Video getVideo(String path, String basePath, 
			VideoType vidType, XMLControl control) {
		// BH! 2020.04.20 from TrackerIO, but equally useful here.
		path = fixVideoPath(path);
		String fullPath = XML.getResolvedPath(path, basePath);
		OSPLog.fine("Path: " + fullPath + "    Type: " + (vidType == null? null: vidType.getTypeName())); //$NON-NLS-1$ //$NON-NLS-2$		
		// for Xuggle videos, download web files to cache
		if (vidType != null && MovieFactory.ENGINE_XUGGLE.equals(vidType.getTypeName()) && ResourceLoader.isHTTP(fullPath)) {
			// download to cache if doesn't exist
			File localFile = ResourceLoader.download(fullPath, null, false);		
			if (localFile != null) {
				fullPath = localFile.getAbsolutePath();
				path = XML.getName(fullPath);
				basePath = XML.getDirectoryPath(fullPath);
			}
		}
		Video video = null;
		VideoIO.setCanceled(false);

		// try first with specified VideoType, if any
		if (vidType != null) {
			OSPLog.finest("preferred type " + vidType.getClass().getSimpleName() //$NON-NLS-1$
					+ " " + vidType.getDescription()); //$NON-NLS-1$
			video = vidType.getVideo(path, basePath, control);
			if (video != null)
				return video;
		}
		if (VideoIO.isCanceled())
			return null;

		// try other allowed video types for the file extension
		ArrayList<VideoType> allTypes = getVideoTypesForPath(path);
//		ArrayList<VideoType> allowedTypes = new ArrayList<VideoType>();
//		boolean skipMovies = !MovieFactory.hasVideoEngine();
//		for (int i = 0; i < allTypes.size(); i++) {
//			if (skipMovies && allTypes.get(i) instanceof MovieVideoType)
//				continue;
//			allowedTypes.add(allTypes.get(i));
//		}
		for (VideoType next : allTypes) {
//			OSPLog.finest("trying type " + next.getClass().getSimpleName() //$NON-NLS-1$
//					+ " " + next.getDescription()); //$NON-NLS-1$
			video = next.getVideo(path, basePath, control);
			if (VideoIO.isCanceled())
				return null;
			if (video != null) {
				// for zipped image video make sure the VideoType is ZipImageVideoType
				// since non-zip ImageVideoTypes can also open zip images
				int n = fullPath.indexOf("zip!");
				n = n > 0? n: fullPath.indexOf("trz!");
				if (n > 0) {
					fullPath = fullPath.substring(0, n+3);					
				}
				if (fullPath.endsWith(".zip") || fullPath.endsWith(".trz")) {
					if (!(next instanceof ZipImageVideoType)) {
						ArrayList<VideoType> types = VideoIO.getVideoTypesForPath(fullPath);
						for (int i = 0; i < types.size(); i++) {
							if (types.get(i) instanceof  ZipImageVideoType) {
								ZipImageVideoType zipType = (ZipImageVideoType)types.get(i);
								if (zipType.getImageVideoType().getDefaultExtension().equals(next.getDefaultExtension())) {
									video.setProperty("video_type", types.get(i)); //$NON-NLS-1$
									break;
								}
							}								
						}
					}
				}
				return video;
			}
		}
		return null;
	}

private static String fixVideoPath(String path) {
		return (path.startsWith("file:") ? ResourceLoader.getNonURIPath(path): path);
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
	 * Never called.
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
			return new ImageVideo((ImageVideo) video);
		}
		XMLControl control = new XMLControlElement(video);
		return (Video) new XMLControlElement(control).loadObject(null);
	}

	/**
	 * Determines if a file can be written. If the file exists, the user is prompted
	 * for approval to overwrite.
	 *
	 * @param file the file to check
	 * @return true if the file can be written
	 */
	public static boolean canWrite(File file) {
		if (OSPRuntime.isJS)
			return true;
		if (file.exists() && !file.canWrite()) {
			JOptionPane.showMessageDialog(null, ControlsRes.getString("Dialog.ReadOnly.Message"), //$NON-NLS-1$
					ControlsRes.getString("Dialog.ReadOnly.Title"), //$NON-NLS-1$
					JOptionPane.PLAIN_MESSAGE);
			return false;
		}
		if (file.exists()) {
			int selected = JOptionPane.showConfirmDialog(null, "\"" + file.getName() + "\" " //$NON-NLS-1$ //$NON-NLS-2$
					+ MediaRes.getString("VideoIO.Dialog.FileExists.Message"), //$NON-NLS-1$
					MediaRes.getString("VideoIO.Dialog.FileExists.Title"), //$NON-NLS-1$
					JOptionPane.YES_NO_OPTION);
			if (selected != JOptionPane.YES_OPTION) {
				return false;
			}
		}
		return true;
	}

//	/**
//	 * A Stop-gap method to allow Java-only functionality.
//	 * 
//	 * @param type
//	 * @return
//	 */
//	@Deprecated
//	public static File[] getChooserFiles(String type) {
//		return getChooserFilesAsync(type, null);
//	}

	/**
	 * Displays a file chooser and returns the chosen files. The parameter "type"
	 * can be "open", "save video", "save resource", "save image", "open image".
	 * There are more types defined in TrackerIO.getChooserFilesAsync
	 *
	 * @param type String
	 * @param processFiles asynchronous follower method
	 * @return the files, or null if no files chosen or asynchronous
	 */
	public static File[] getChooserFilesAsync(String type, Function<File[], Void> processFiles) {
		AsyncFileChooser chooser = getChooser();
		chooser.setMultiSelectionEnabled(false);
		chooser.setAcceptAllFileFilterUsed(true);
		chooser.setAccessory(null);

		Runnable resetChooser = () -> {
				chooser.resetChoosableFileFilters();
				if (processFiles != null)
					chooser.setSelectedFile(null); // $NON-NLS-1$
		};

		Runnable okOpen = () -> {
				File file = chooser.getSelectedFile();
				resetChooser.run();
				if (chooser.getSelectedOption() != JFileChooser.APPROVE_OPTION)
					return;
				if (processFiles != null)
					processFiles.apply(new File[] { file });
		};

		Runnable okSave = () -> {
			File file = chooser.getSelectedFile();
			resetChooser.run();
			if (chooser.getSelectedOption() != JFileChooser.APPROVE_OPTION)
				return;
			if (processFiles != null && canWrite(file))
				processFiles.apply(new File[] { file });
		};
		
		String originalFileName = "";
		String saveVideo = "save video";
		if (type.startsWith(saveVideo + " ")) {
			originalFileName = type.substring(saveVideo.length() + 1);
			type = saveVideo;
		}
		String saveResource = "save resource";
		if (type.startsWith(saveResource + " ")) {
			originalFileName = type.substring(saveResource.length() + 1);
			type = saveResource;
		}

		switch (type.toLowerCase()) {
		case "open": // open any file //$NON-NLS-1$
			chooser.addChoosableFileFilter(videoFileFilter);
			chooser.setFileFilter(chooser.getAcceptAllFileFilter());
			chooser.showOpenDialog(null, okOpen, resetChooser);
			break;
		case "open image": // open any file //$NON-NLS-1$
			chooser.setAcceptAllFileFilterUsed(false);
			chooser.resetChoosableFileFilters();
			chooser.addChoosableFileFilter(imageFileFilter);
			chooser.setFileFilter(imageFileFilter);
			chooser.showOpenDialog(null, okOpen, resetChooser);
			break;
		case "save video": // save video file //$NON-NLS-1$
			chooser.resetChoosableFileFilters();
			chooser.setDialogTitle(MediaRes.getString("VideoIO.Dialog.SaveVideoAs.Title")); //$NON-NLS-1$
			chooser.setFileFilter(chooser.getAcceptAllFileFilter());
			chooser.setSelectedFile(new File(originalFileName));
			chooser.showSaveDialog(null, okSave, resetChooser);
			break;
		case "save image": // save image file //$NON-NLS-1$
			chooser.setAcceptAllFileFilterUsed(false);
			chooser.resetChoosableFileFilters();
			chooser.setDialogTitle(MediaRes.getString("VideoIO.Dialog.SaveAs.Title")); //$NON-NLS-1$
			chooser.addChoosableFileFilter(jpgFileFilter);
			chooser.setFileFilter(jpgFileFilter);
			chooser.setSelectedFile(new File(originalFileName));
			chooser.showSaveDialog(null, okSave, resetChooser);
			break;
		case "save resource": // save resource file //$NON-NLS-1$
			chooser.resetChoosableFileFilters();
			chooser.setDialogTitle(MediaRes.getString("VideoIO.Dialog.SaveAs.Title")); //$NON-NLS-1$
			chooser.setFileFilter(chooser.getAcceptAllFileFilter());
			chooser.setSelectedFile(new File(originalFileName));
			chooser.showSaveDialog(null, okSave, resetChooser);
			break;
		default:
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
		String path = file.getAbsolutePath();
		if (ResourceLoader.isJarZipTrz(path, false)) { 
			// find a video
			String p = getEmbeddedMovie(path);
			if (p != null) {
				file = new File(path = p);
			}
		}
		if (videoFileFilter.accept(file, true)) { // load video
			ArrayList<VideoType> types = getVideoTypes(false);
			Video video = null;
			for (int i = 0; i < types.size(); i++) {
				VideoType type = types.get(i);
				if (type.accepts(file)) {
					video = type.getVideo(path, null, null);
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
		} else {
			// load data
			XMLControlElement control = new XMLControlElement();
			control.read(path);
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

	private static String getEmbeddedMovie(String path) {
		Map<String, ZipEntry> map = ResourceLoader.getZipContents(path, true);
		for (String key : map.keySet()) {
			if (videoFileFilter.accept(new File(key), true)) { // load video
				return path + "!/" + key;
		}}
		return null;
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
	
	/**
	 * Gets the paths that make up an image sequence.
	 * 
	 * @param imagePath the path to the first image
	 * @param names a set of names of available numbered images
	 * @return array of full paths to all images in sequence
	 */
	public static String[] getImageSequencePaths(String imagePath, Set<String> names) {
		ArrayList<String> imagePaths = new ArrayList<String>();
		String originalPath = imagePath;
		// look for numbered image names with pattern nameXXX.ext
		String extension = ""; //$NON-NLS-1$
		int i = imagePath.lastIndexOf('.');
		if (i > 0 && i < imagePath.length() - 1) {
			extension = imagePath.substring(i).toLowerCase();
			imagePath = imagePath.substring(0, i); // now free of extension
		}
		int len = imagePath.length();
		int digits = 0;
		while (digits <= 4 && --len >= 0 && Character.isDigit(imagePath.charAt(len))) {
			digits++;
		}
		if (digits == 0)
			return new String[] {originalPath};
		
		int limit = (int) Math.pow(10, digits);
		
		String root = imagePath.substring(0, ++len);
		String name = XML.getName(root);
		root = XML.getDirectoryPath(root) + "/";
		String startNumber = imagePath.substring(len);
		int n = Integer.parseInt(startNumber) - 1;
		try {			
			while (++n < limit) {
				// fill with leading zeros if nec
				String num = "000" + n;
				String imageName = name + (num.substring(num.length() - digits)) + extension;
				if (names.contains(imageName)) {
					imagePaths.add(root + imageName);					
					continue;
				}
				break;
			}
		} catch (NumberFormatException ex) {
			ex.printStackTrace();
		}

		return imagePaths.toArray(new String[imagePaths.size()]);
	}
		
	// class to show message in JEditorPane and respond to hyperlinks
	public static class EditorPaneMessage extends JEditorPane {

		public EditorPaneMessage(String htmlBody) {
			super("text/html", "<html><body style=\"" + getLabelStyle() + "\">" + htmlBody + "</body></html>");
			addHyperlinkListener(new HyperlinkListener() {
				@Override
				public void hyperlinkUpdate(HyperlinkEvent e) {
					if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
						OSPDesktop.displayURL(e.getURL().toString());
					}
				}
			});
			setEditable(false);
			setBorder(null);
		}

		static StringBuffer getLabelStyle() {
			// for copying style
			JLabel label = new JLabel();
			Font font = label.getFont();
			Color color = label.getBackground();

			// create some css from the label's font
			StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
			style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
			style.append("font-size:" + font.getSize() + "pt;");
			style.append(
					"background-color: rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ");");
			return style;
		}
	}

	
	private static Map<String, String> codecMap = new HashMap<>();
	
	/**
	 * JavaScript only.
	 * 
	 * Check for a valid video codec and file format. Handle it if it is an MP4 or
	 * MOV with unsupported codec From JSMovieVideo.load, TFrame.loadVideo,
	 * TrackerIO.AsyncLoader.loadVideo
	 * 
	 * @param path
	 * @param libraryBrowser
	 * @return true if not JS or not an mp4 and not mov, or not is a valid mp4/mov
	 */
	public static boolean checkMP4(String path, LibraryBrowser libraryBrowser, VideoPanel panel) {
		String ext = XML.getExtension(path);
		if (!OSPRuntime.isJS || !"mp4".equals(ext) && !"mov".equals(ext))
			return true;
		String codec = getVideoCodec(path);
		OSPLog.fine("VideoIO: " + ext + " codec = " + codec);
		if (codec != null && codec.contains("avc1"))
			return true;
		VideoIO.handleUnsupportedVideo(path, ext, codec, panel, "VideoIO");
		return false;
	}

	public static int progressForFraction(double iFrame, double nFrames) {
		return (int) Math.min(
				PROGRESS_VIDEO_PROCESSING + (iFrame / nFrames % 1.00001) * (PROGRESS_VIDEO_READY - PROGRESS_VIDEO_PROCESSING),
				PROGRESS_VIDEO_READY - 1);
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
