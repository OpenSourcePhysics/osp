package org.opensourcephysics.media.mov;

/**
 * A marker interface for identifying anything that handles MOV/MP4 video files
 * @author hansonr
 *
 */
public interface MovieVideoI {

	String PROPERTY_VIDEO_PROGRESS = "progress"; //$NON-NLS-1$ // see TFrame
	String PROPERTY_VIDEO_STALLED  = "stalled"; //$NON-NLS-1$  // see TFrame
	String PROPERTY_VIDEO_READY    = "ready"; //$NON-NLS-1$  // see TFrame

}
