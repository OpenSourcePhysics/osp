package org.opensourcephysics.media.core;

import java.io.IOException;

public interface IncrementallyLoadable {
	
	/**
	 * Attempt to load n more frames.
	 * 
	 * @param n the number of additional frames to load
	 * @return true if n additional frames were loaded, false if fewer or none loaded
	 */
	public boolean loadMoreFrames(int n) throws IOException;
	
	/**
	 * Get the total number of frames loaded.
	 * 
	 * @return the number of frames loaded
	 */
	public int getLoadedFrameCount();
	
	/**
	 * Get the total number of frames expected.
	 * 
	 * @return
	 */
	public int getLoadableFrameCount();
	
	/**
	 * Set the total number of frames expected.
	 * 
	 * @param n the number of frames expected
	 */
	public void setLoadableFrameCount(int n);
	
	/**
	 * Determines if the video is fully loaded.
	 * 
	 * @return true if fully loaded
	 */
	public boolean isFullyLoaded();

}
