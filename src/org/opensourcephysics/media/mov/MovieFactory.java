package org.opensourcephysics.media.mov;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.media.core.MediaRes;
import org.opensourcephysics.media.core.Video;
import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.media.core.VideoRecorder;
import org.opensourcephysics.media.core.VideoType;

/**
 * A factory class to allow options for implementing Xuggle-like behavior
 * @author hansonr
 *
 */
public class MovieFactory {

	private static final String ENGINE_NONE = "none"; //$NON-NLS-1$
	private static final String ENGINE_JS = "JS"; //$NON-NLS-1$
	public static final String ENGINE_XUGGLE = "Xuggle"; //$NON-NLS-1$

	private static String xuggleClassPath = "org.opensourcephysics.media.xuggle.";

	private static String movieVideoName;

	private static ArrayList<VideoType> movieVideoTypes = new ArrayList<VideoType>();

	public static boolean isMovieTypeAvailable;

	
	/**
	 * Add video types MOV, MP4, OGG, AVI, etc., as appropriate.
	 * 
	 * JSMovieVideo and XuggleVideo self-register via their static initializers. 
	 * We could call anything here, and it would do the job. The action happens
	 * at the point of "JSMovieVideo." not "JSMovieVideo.registered". Likewise,
	 * for Xuggle, we just need to load the class, not actually do anything with it.
	 * 
	 * Thus no need for "XuggleIO" or "JSMovieIO". 
	 */
	static {
		//public static boolean registerWithViewoIO()
		try {
			if (OSPRuntime.isJS) {
				if (JSMovieVideo.registered) {
					// mere request does the job
				}
			} else {
				Class.forName(xuggleClassPath + "XuggleVideo");
			}
		} catch (Throwable e) {
			OSPLog.config("Xuggle not installed? " + xuggleClassPath + "XuggleVideo not found"); //$NON-NLS-1$
		}
	}

	private static PropertyChangeListener[] errorListener = new PropertyChangeListener[] {
			new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent e) {
					if (e.getPropertyName().equals("xuggle_error")) { //$NON-NLS-1$
						isMovieTypeAvailable = false;
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
	
	/**
	 * Adds a video engine to the list of available engines
	 *
	 * @param movieVideoType the video engine type
	 */
	public static void addMovieVideoType(VideoType movieVideoType) {
		if (movieVideoType == null)
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
			// TODO Auto-generated method stub
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
		if (OSPRuntime.isJS)
			return;
		if (!isMovieTypeAvailable)
			throw new Error("Video frame extractor unavailable"); //$NON-NLS-1$
		boolean logConsole = OSPLog.isConsoleMessagesLogged();
		try {
			OSPLog.setConsoleMessagesLogged(false);
			Class.forName("com.xuggle.xuggler.IContainer"); //$NON-NLS-1$
			OSPLog.setConsoleMessagesLogged(logConsole);
		} catch (Exception ex) {
			OSPLog.setConsoleMessagesLogged(logConsole);
			throw new Error("Video frame extractor unavailable"); //$NON-NLS-1$
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
		if (movieVideoName == null) {
			for (VideoType next : movieVideoTypes) {
				if (next instanceof MovieVideoType && hasVideoEngine()) {
					return movieVideoName = (OSPRuntime.isJS ? ENGINE_JS : ENGINE_XUGGLE);
				}
			}
			movieVideoName = ENGINE_NONE;
		}
		return movieVideoName;
	}

	public static boolean hasVideoEngine() {
		return (OSPRuntime.isJS ? true
				: movieVideoName != null ? movieVideoName != ENGINE_NONE
				: "Xuggle".equals(getVideoProperty("name"))
				&& ((Double) getVideoProperty("version")).doubleValue() == 3.4);
	}

	/**
	 * Communicate with JSMovieVideo or XuggleVideo through
	 * DrawableImage.getProperty(String). This method is only called in Java, and
	 * only a few times, particularly to get the Xuggle version, to call up an
	 * "about" panel, and to update JAR files.
	 * 
	 * @param name
	 * @return something
	 */
	public static Object getVideoProperty(String name) {
		Video video = newMovieVideo(null, null);
		return (video == null ? null : video.getProperty(name));
	}

	public static Video newMovieVideo(String name, String description) {
		Video video = null;
		try {
			if (OSPRuntime.isJS) {
				video = new JSMovieVideo(name);
			} else {
				video = (Video) Class.forName(xuggleClassPath + "XuggleVideo").newInstance();
				if (name != null)
					((PluginVideoI) video).init(name);
			}
		} catch (IOException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			if (name != null) {
				OSPLog.fine(description + ": " + e.getMessage()); //$NON-NLS-1$
				e.printStackTrace();
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
		try {
			return (VideoRecorder) Class.forName(xuggleClassPath + "XuggleVideoRecorder").newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * called by VideoClip.Loader and TrackerIO only
	 * 
	 * @param video
	 */
	public static void setEngine(Video video) {
		movieVideoName = (video instanceof MovieVideoI ? video.getName()
				: ENGINE_NONE);
	}

	public static void startMovieThumbnailTool() {
		if (OSPRuntime.isJS) {
			return; // ?? 
		}
		if (hasVideoEngine()) {
			try {
				Class.forName(xuggleClassPath + "XuggleThumbnailTool");
			} catch (ClassNotFoundException e) {
			}
		}
	}

}
