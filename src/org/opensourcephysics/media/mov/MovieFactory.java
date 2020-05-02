package org.opensourcephysics.media.mov;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.media.core.MediaRes;
import org.opensourcephysics.media.core.Video;
import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.media.core.VideoRecorder;
import org.opensourcephysics.media.core.VideoType;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * A factory class to manage video engines
 * @author hansonr
 *
 */
public class MovieFactory {

	public static final String ENGINE_NONE = "none"; //$NON-NLS-1$
	public static final String ENGINE_JS = "JS"; //$NON-NLS-1$
	public static final String ENGINE_XUGGLE = "Xuggle"; //$NON-NLS-1$

	private static String xuggleClassPath = "org.opensourcephysics.media.xuggle."; //$NON-NLS-1$
	private static String xugglePropertiesPath = "org/opensourcephysics/resources/xuggle/xuggle.properties"; //$NON-NLS-1$

	private static String videoEngineName;

	public static boolean hadXuggleError = false;
	public static Boolean xuggleIsAvailable = null;
	public static boolean xuggleIsPresent = false;

	
	/**
	 * Initialize video classes to register their video types
	 * 
	 * JSMovieVideo and XuggleVideo self-register via their static initializers. 
	 * We could call anything here, and it would do the job. The action happens
	 * at the point of "JSMovieVideo." not "JSMovieVideo.registered". Likewise,
	 * for Xuggle, we just need to load the class, not actually do anything with it.
	 */
	static {
		try {
			if (OSPRuntime.isJS) {
				if (JSMovieVideo.registered) {
					// mere request does the job
					videoEngineName = ENGINE_JS;
					xuggleIsAvailable = Boolean.FALSE;
					xuggleIsPresent = false;
				}
			} else {
				Class.forName(xuggleClassPath + "XuggleVideo"); //$NON-NLS-1$
				xuggleIsAvailable = Boolean.TRUE;
				xuggleIsPresent = true;
				videoEngineName = ENGINE_XUGGLE;
			}
		} catch (Throwable e) {			
			videoEngineName = ENGINE_NONE;
			if (!OSPRuntime.isJS) {
				OSPLog.config("Xuggle not installed? " + xuggleClassPath + "XuggleVideo failed"); //$NON-NLS-1$ //$NON-NLS-2$
				xuggleIsAvailable = Boolean.FALSE;
				String jarPath = OSPRuntime.getLaunchJarPath();
				xuggleIsPresent = (jarPath!=null && ResourceLoader.getResource(jarPath+"!/"+xugglePropertiesPath)!=null); //$NON-NLS-1$
			}
		}
	}

	private static PropertyChangeListener[] errorListener = new PropertyChangeListener[] {
		new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				if (e.getPropertyName().equals("xuggle_error")) { //$NON-NLS-1$
					hadXuggleError = true;
					// so noted; remove errorListener
					OSPLog.getOSPLog().removePropertyChangeListener(errorListener[0]);
					errorListener[0] = null;
				}
			}
	  }
	};

	static {
	  OSPLog.getOSPLog().addPropertyChangeListener(errorListener[0]);
	}
	
	private static ArrayList<VideoType> movieVideoTypes = new ArrayList<VideoType>();

	/**
	 * Adds a video engine to the list of available engines
	 *
	 * @param movieVideoType the video engine type
	 */
	public static void addMovieVideoType(VideoType movieVideoType) {
		if (movieVideoType == null || movieVideoTypes == null)
			return;
		OSPLog.finest(movieVideoType.getClass().getSimpleName() + " " + movieVideoType.getDefaultExtension()); //$NON-NLS-1$
		for (VideoType next : movieVideoTypes) {
			if (next.getClass() == movieVideoType.getClass()) {
				return;
			}
		}
		movieVideoTypes.add(movieVideoType);
		if (VideoIO.videoEnginePanel != null)
			VideoIO.videoEnginePanel.addMovieVideoType((MovieVideoType) movieVideoType);
	}

	public static File createThumbnailFile(Dimension defaultThumbnailDimension, String sourcePath, String thumbPath) {
		if (hasVideoEngine()) {
			if (OSPRuntime.isJS)
				return JSMovieVideo.createThumbnailFile(defaultThumbnailDimension, sourcePath, thumbPath);

			// video files: use Xuggle thumbnail tool, if available
			String className = xuggleClassPath + "XuggleThumbnailTool"; //$NON-NLS-1$
			Class<?>[] types = new Class<?>[] { Dimension.class, String.class, String.class };
			try {
				Class<?> xuggleClass = Class.forName(className);
				Method method = xuggleClass.getMethod("createThumbnailFile", types); //$NON-NLS-1$
				return (File) method.invoke(null, new Object[] { defaultThumbnailDimension, sourcePath, thumbPath });
			} catch (Exception ex) {
				OSPLog.fine("failed to create thumbnail: " + ex.toString()); //$NON-NLS-1$
			} catch (Error err) {
			}
		}
		return null;
	}

	public static void ensureAvailable() throws Error {
		if (OSPRuntime.isJS || xuggleIsAvailable == Boolean.TRUE)
			return;
		if (hadXuggleError || xuggleIsAvailable == Boolean.FALSE)
			throw new Error("Movie videos unavailable"); //$NON-NLS-1$
		boolean logConsole = OSPLog.isConsoleMessagesLogged();
		try {
			OSPLog.setConsoleMessagesLogged(false);
			Class.forName("com.xuggle.xuggler.IContainer"); //$NON-NLS-1$
			xuggleIsAvailable = Boolean.TRUE;
			OSPLog.setConsoleMessagesLogged(logConsole);
		} catch (Exception ex) {
			xuggleIsAvailable = Boolean.FALSE;
			OSPLog.setConsoleMessagesLogged(logConsole);
			throw new Error("Movie videos unavailable"); //$NON-NLS-1$
		}
	}

	/**
	 * Gets the name of the current video engine.
	 * 
	 * @param forDialog
	 *
	 * @param forDialog if true, returns locale version of "none"
	 * @return "Xuggle" or "JS" or "none" or Locale version of "none"
	 */
	public static String getMovieVideoName(boolean forDialog) {
		if (forDialog) {
			String name = getMovieVideoName(false);
			return (name == ENGINE_NONE ? MediaRes.getString("VideoIO.Engine.None") : name); //$NON-NLS-1$
		}
		if (videoEngineName == null) {
			for (VideoType next : movieVideoTypes) {
				if (next instanceof MovieVideoType && hasVideoEngine()) {
					return videoEngineName = (OSPRuntime.isJS ? ENGINE_JS : ENGINE_XUGGLE);
				}
			}
			videoEngineName = ENGINE_NONE;
		}
		return videoEngineName;
	}

	public static boolean hasVideoEngine() {
		return (OSPRuntime.isJS ? true
				: videoEngineName != null ? videoEngineName != ENGINE_NONE
				: false);
	}

	public static Video newMovieVideo(String name, String description) {
		Video video = null;
		try {
			if (OSPRuntime.isJS) {
				video = new JSMovieVideo(name);
			} 
//			else {
//				video = (Video) Class.forName(xuggleClassPath + "XuggleVideo").newInstance();
//				if (name != null)
//					((PluginVideoI) video).init(name);
//			}
		} catch (Exception e) {
			if (name != null) {
				OSPLog.fine(description + ": " + e.getMessage()); //$NON-NLS-1$
				e.printStackTrace();
			}
		} catch (Error er) {
			if (name != null) {
				OSPLog.fine(description + ": " + er.getMessage()); //$NON-NLS-1$
				er.printStackTrace();
			}
		}
		return video;
	}

	public static VideoRecorder newMovieVideoRecorder(MovieVideoType videoType) {
		if (!OSPRuntime.canRecordMovieFiles) {
			OSPLog.warning("MovieFactory videoRecorder not implemented");
			return null;
		}
		if (OSPRuntime.isJS) {
			OSPLog.warning("MovieFactory videoRecorder not implemented");
			return null;
		}
//		try {
//			return (VideoRecorder) Class.forName(xuggleClassPath + "XuggleVideoRecorder").newInstance();
//		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
//			e.printStackTrace();
//			return null;
//		}
		return null;
	}

//	/**
//	 * called by VideoClip.Loader and TrackerIO only
//	 * 
//	 * @param video
//	 */
//	public static void setEngine(Video video) {
//		videoEngineName = (video instanceof MovieVideoI ? video.getName()
//				: ENGINE_NONE);
//	}
//
	public static void startMovieThumbnailTool() {
		if (OSPRuntime.isJS) {
			return; // ?? 
		}
		if (hasVideoEngine()) {
			try {
				Class.forName(xuggleClassPath + "XuggleThumbnailTool"); //$NON-NLS-1$
			} catch (ClassNotFoundException e) {
			}
		}
	}
	
	/**
	 * Updates video engine files if available
	 * 
	 * @return array of video engine names that were updated. May return null.
	 */
	public static String[] getUpdatedVideoEngines() {
		if (!OSPRuntime.isJS) /** @j2sNative */ {	
			if (videoEngineName.equals(ENGINE_XUGGLE)) {
				// copy xuggle files to codebase
				try {
					String codebase = OSPRuntime.getLaunchJarDirectory();
					if (codebase != null) {
						// call DiagnosticsForXuggle.copyXuggleJarsTo by reflection
						Class<?> clas = Class.forName("org.opensourcephysics.media.xuggle.DiagnosticsForXuggle"); //$NON-NLS-1$
						Method method = clas.getMethod("copyXuggleJarsTo", new Class[] {File.class}); //$NON-NLS-1$
						Object result = method.invoke(null, new Object[] {new File(codebase)});
						if ((Boolean)result) return new String[] {ENGINE_XUGGLE};
					}		
				} catch (Exception e) {}
			}
		}
		return null;
	}
	
	/**
	 * Show an About dialog for a video engine
	 * 
	 * @param engineName one of the allowed engine names
	 * @param requester name of a requestor. May be null.
	 */
	public static void showAbout(String engineName, String requester) {
		if (engineName==ENGINE_XUGGLE) {
			// call DiagnosticsForXuggle.aboutXuggle by reflection
			try {
				Class<?> clas = Class.forName("org.opensourcephysics.media.xuggle.DiagnosticsForXuggle"); //$NON-NLS-1$
				Method method = clas.getMethod("aboutXuggle", new Class[] {String.class}); //$NON-NLS-1$
				method.invoke(null, new Object[] {requester});
			} catch (Exception e1) {}
		}
	}

}
