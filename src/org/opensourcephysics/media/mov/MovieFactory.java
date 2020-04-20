package org.opensourcephysics.media.mov;

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
import org.opensourcephysics.media.xugglejs.XuggleJSThumbnailTool;

/**
 * A factory class to allow options for implementing Xuggle-like behavior
 * @author hansonr
 *
 */
public class MovieFactory {

	public static VideoRecorder newXtractorVideoRecorder(MovieVideoType xuggleVideoType) {
		try {
			return (VideoRecorder) Class.forName("org.opensourcephysics.media.core.XuggleVideoRecorder").newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Video newVideo(String name, String description) {
		try {
			Video video = (Video) Class.forName("org.opensourcephysics.media.core.XuggleVideo").newInstance();
			video.init(name);
			return video;
		} catch (IOException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
	    	OSPLog.fine(description+": "+e.getMessage()); //$NON-NLS-1$
			e.printStackTrace();
			return null;
		}
	}

	public static void startXtractorThumbnailTool() {
		if (OSPRuntime.isJS) {
			XuggleJSThumbnailTool.start();
		} else {
			try {
				Class.forName("org.opensourcephysics.media.xuggle.XuggleThumbnailTool");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	public static void registerWithViewoIO() {
		MovieIO.registerWithVideoIO();
	}

	/**
	 * Determines if a video engine is installed on the current computer. Note that
	 * accessing an installed engine may require switching Java VM.
	 *
	 * @param engine ENGINE_XUGGLE (or a variant of that), or ENGINE_NONE
	 * @return true if JavaScript or installed
	 */
	public static boolean isEngineInstalled(String engine) {
		return (OSPRuntime.isJS || engine.startsWith(VideoIO.ENGINE_XUGGLE) && DiagnosticsForXuggle.getXuggleJar() != null);
	}

	/**
	 * Gets the name of the default video engine.
	 *
	 * @return ENGINE_XUGGLE, or ENGINE_NONE
	 */
	public static String getDefaultEngine() {
		if (OSPRuntime.isJS) {
			return VideoIO.ENGINE_XUGGLEJS;
		}
		/**
		 * Java only
		 * 
		 * @j2sNative
		 */
		{
			for (VideoType next : MovieFactory.videoEngines) {
				// BH! was contains
				if (next.getClass().getSimpleName().startsWith(VideoIO.ENGINE_XUGGLE)) {
					double xuggleVersion = DiagnosticsForXuggle.guessXuggleVersion();
					if (xuggleVersion == 3.4)
						return VideoIO.ENGINE_XUGGLE;
				}
			}
			return VideoIO.ENGINE_NONE;
		}
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
		// BH 2020.04.20 was "equals(ENGINE_XUGGLE)" but I think that was a bug.
		if (engine != null && (engine.startsWith(VideoIO.ENGINE_XUGGLE) || engine.equals(VideoIO.ENGINE_NONE)))
			VideoIO.videoEngine = engine;
	}

	public static ArrayList<VideoType> videoEngines = new ArrayList<VideoType>();

}
