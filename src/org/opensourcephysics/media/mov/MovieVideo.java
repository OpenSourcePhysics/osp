package org.opensourcephysics.media.mov;

import org.opensourcephysics.media.core.VideoAdapter;

/**
 * An abstract class that describes common aspects of JSMovieVideo and
 * XuggleVideo, which involve actual video sources.
 * 
 * (formerly MovieVideoI interface)
 * 
 * @author hansonr
 *
 */
public abstract class MovieVideo extends VideoAdapter {
	
	public final static String PROPERTY_VIDEO_PROGRESS = "progress"; //$NON-NLS-1$ // see TFrame
	public final static String PROPERTY_VIDEO_STALLED  = "stalled"; //$NON-NLS-1$  // see TFrame

}
