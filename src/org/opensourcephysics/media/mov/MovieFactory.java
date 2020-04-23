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
import org.opensourcephysics.media.xuggle.XuggleIO;
import org.opensourcephysics.media.xuggle.XuggleVideoI;

/**
 * A factory class to allow options for implementing Xuggle-like behavior
 * @author hansonr
 *
 */
public class MovieFactory {

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
			((XuggleVideoI) video).init(name);
			return video;
		} catch (IOException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
	    	OSPLog.fine(description+": "+e.getMessage()); //$NON-NLS-1$
			e.printStackTrace();
			return null;
		}
	}

	public static void startMovieThumbnailTool() {
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
		if (OSPRuntime.isJS) {
			JSMovieIO.registerWithVideoIO();
			return;
		}
		
		if (System.getenv("XUGGLE_HOME") != null) {//$NON-NLS-1$
			XuggleIO.registerWithVideoIO();
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
				|| VideoIO.isMovieEngine(engine) && DiagnosticsForXuggle.getXuggleJar() != null);
	}

	/**
	 * Gets the name of the default video engine.
	 *
	 * @return ENGINE_XUGGLE, or ENGINE_NONE
	 */
	public static String getDefaultEngine() {
		for (VideoType next : MovieFactory.videoEngines) {
			if (next instanceof MovieVideoType
					&& (OSPRuntime.isJS || DiagnosticsForXuggle.guessXuggleVersion() == 3.4))
				return VideoIO.getMovieEngineBaseName();
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
				|| VideoIO.isMovieEngine(engine)))
			VideoIO.videoEngine = engine;
	}

	/** called by VideoClip.Loader and TrackerIO only
	 * 
	 * @param video
	 */
	public static void setEngine(Video video) {
		setEngine(video instanceof MovieVideoI ? VideoIO.getMovieEngineBaseName() : VideoIO.ENGINE_NONE);
	}

	public static ArrayList<VideoType> videoEngines = new ArrayList<VideoType>();

	public static File createThumbnailFile(Dimension defaultThumbnailDimension, String sourcePath, String thumbPath) {
		// TODO Auto-generated method stub
		if (OSPRuntime.isJS) {
			return JSMovieVideo.createThumbnailFile(defaultThumbnailDimension, sourcePath, thumbPath);
		}
		if (getEngine() != VideoIO.ENGINE_NONE) {
			// video files: use Xuggle thumbnail tool, if available
			String className = "org.opensourcephysics.media.xuggle.XuggleThumbnailTool"; //$NON-NLS-1$
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

	public static String getMovieClass() {
		return (OSPRuntime.isJS ? null  : "com.xuggle.xuggler.IContainer"); //$NON-NLS-1$
		
	}


}
