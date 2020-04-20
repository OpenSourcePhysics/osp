package org.opensourcephysics.media.mov;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.media.core.Video;
import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.media.core.VideoRecorder;
import org.opensourcephysics.media.core.VideoType;
import org.opensourcephysics.media.xuggle.DiagnosticsForXuggle;

/**
 * A factory class to allow options for implementing Xuggle-like behavior
 * @author hansonr
 *
 */
public class MovieFactory {

	public static VideoRecorder newMovieVideoRecorder(MovieVideoType videoType) {
		if (OSPRuntime.isJS) {
			OSPLog.warning("MovieFactory videoRecorder not implemented for JavaScript");
			return null;
		}
		try {
			return (VideoRecorder) Class.forName("org.opensourcephysics.media.core.XuggleVideoRecorder").newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Video newVideo(String name, String description) {
		try {
			if (OSPRuntime.isJS) {
				return new JSMovieVideo(name);
			}
			Video video = (Video) Class.forName("org.opensourcephysics.media.core.XuggleVideo").newInstance();
			((MovieVideoI) video).init(name);
			return video;
		} catch (IOException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
	    	OSPLog.fine(description+": "+e.getMessage()); //$NON-NLS-1$
			e.printStackTrace();
			return null;
		}
	}

	public static void startXtractorThumbnailTool() {
		if (OSPRuntime.isJS) {
			return;
		} else {
			try {
				Class.forName("org.opensourcephysics.media.xuggle.XuggleThumbnailTool");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	public static void registerWithViewoIO() {
		if (OSPRuntime.isJS || System.getenv("XUGGLE_HOME") != null) {//$NON-NLS-1$
			MovieIO.registerWithVideoIO();
		} else {
			OSPLog.config("Xuggle not installed? (XUGGLE_HOME not found)"); //$NON-NLS-1$
		}

	}

	/**
	 * Determines if a video engine is installed on the current computer. Note that
	 * accessing an installed engine may require switching Java VM.
	 *
	 * @param engine ENGINE_XUGGLE (or a variant of that), or ENGINE_NONE
	 * @return true if JavaScript or installed
	 */
	public static boolean isEngineInstalled(String engine) {
		return (OSPRuntime.isJS 
				|| VideoIO.isNameLikeMovieEngine(engine) && DiagnosticsForXuggle.getXuggleJar() != null);
	}

	/**
	 * Gets the name of the default video engine.
	 *
	 * @return ENGINE_XUGGLE, or ENGINE_NONE
	 */
	public static String getDefaultEngine() {
		for (VideoType next : MovieFactory.videoEngines) {
			if (VideoIO.isNameLikeMovieEngine(next.getClass().getSimpleName())
					&& (OSPRuntime.isJS || DiagnosticsForXuggle.guessXuggleVersion() == 3.4))
				return VideoIO.getMovieEngineName();
		}
		return VideoIO.ENGINE_NONE;
	}

	/**
	 * Gets the name of the current video engine.
	 *
	 * @return ENGINE_XUGGLE, or ENGINE_NONE
	 */
	public static String getEngine() {
		if (VideoIO.videoEngine == null) {
			VideoIO.videoEngine = getDefaultEngine();
		}
		return VideoIO.videoEngine;
	}

	/**
	 * Sets the current video engine by name.
	 *
	 * @param engine ENGINE_XUGGLE, or ENGINE_NONE
	 */
	public static void setEngine(String engine) {
		if (engine != null && (engine.equals(VideoIO.ENGINE_NONE) 
				|| VideoIO.isNameLikeMovieEngine(engine)))
			VideoIO.videoEngine = engine;
	}

	public static ArrayList<VideoType> videoEngines = new ArrayList<VideoType>();

	public static File createThumbnailFile(Object[] values) {
		if (OSPRuntime.isJS) {
			return JSMovieVideo.createThumbnailFile(values);
		}
		// video files: use Xuggle thumbnail tool, if available
		String className = "org.opensourcephysics.media.xuggle.XuggleThumbnailTool"; //$NON-NLS-1$
		Class<?>[] types = new Class<?>[] { Dimension.class, String.class, String.class };
		try {
			Class<?> xuggleClass = Class.forName(className);
			Method method = xuggleClass.getMethod("createThumbnailFile", types); //$NON-NLS-1$
			return (File) method.invoke(null, values);
		} catch (Exception ex) {
			OSPLog.fine("failed to create thumbnail: " + ex.toString()); //$NON-NLS-1$
		} catch (Error err) {
		}
		return null;
	}

	public static String getMovieClass() {
		return (OSPRuntime.isJS ? null  : "com.xuggle.xuggler.IContainer"); //$NON-NLS-1$
		
	}

}
