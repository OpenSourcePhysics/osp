package org.opensourcephysics.media.xuggle;

import java.io.IOException;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.media.core.Video;
import org.opensourcephysics.media.core.VideoRecorder;

/**
 * A factory class to allow options for implementing Xuggle-like behavior
 * @author hansonr
 *
 */
public class XuggleFactory {

	public static VideoRecorder newXuggleVideoRecorder(XuggleVideoType xuggleVideoType) {
		try {
			return (VideoRecorder) Class.forName("org.opensourcephysics.media.core.XuggleVideoRecorder").newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Video newXuggleVideo(String name, String description) {
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

	public static void startXuggleThumbnailTool() {
		try {
			Class.forName("org.opensourcephysics.media.xuggle.XuggleThumbnailTool");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

}
