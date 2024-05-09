package org.opensourcephysics.media.mov;

import java.io.IOException;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.media.core.Video;
import org.opensourcephysics.media.core.VideoAdapter;
import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.media.core.VideoType;

/**
 * An abstract class that describes common aspects of VideoAdapters that
 * involve actual digital video sources such as MP4 (JSMovieVideo and XuggleVideo).
 * 
 * (formerly MovieVideoI interface)
 * 
 * @author hansonr
 *
 */
public abstract class MovieVideo extends VideoAdapter {
	
	public final static String PROPERTY_VIDEO_PROGRESS = "progress"; //$NON-NLS-1$ // see TFrame
	public final static String PROPERTY_VIDEO_STALLED  = "stalled"; //$NON-NLS-1$  // see TFrame

	// load,save for video frames would be here
	
	static public abstract class Loader extends VideoAdapter.Loader {

		@Override
		public void saveObject(XMLControl control, Object obj) {
			super.saveObject(control, obj);
		}

		public void setVideo(String path, MovieVideo video, String engine) {
			String ext = XML.getExtension(path);
			VideoType type = VideoIO.getVideoType(engine, ext);
			if (type != null)
				video.setProperty("video_type", type); //$NON-NLS-1$

		}

	}

}
