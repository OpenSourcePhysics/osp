/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

/*
 * The org.opensourcephysics.media.core package defines the Open Source Physics
 * media framework for working with video and other media.
 *
 * Copyright (c) 2024  Douglas Brown and Wolfgang Christian.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * For additional information and documentation on Open Source Physics,
 * please see <http://www.opensourcephysics.org/>.
 */
package org.opensourcephysics.media.core;

import java.awt.Dimension;
import java.beans.PropertyChangeListener;

/**
 * This defines methods to control a video image sequence. Individual images
 * within the sequence are referred to as frames.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public interface Video extends InteractiveImage, Trackable, PropertyChangeListener {
	
	public final static String PROPERTY_VIDEO_COORDS = "coords";//$NON-NLS-1$
	public static final String PROPERTY_VIDEO_FILTERCHANGED = "filterChanged"; //$NON-NLS-1$
	public static final String PROPERTY_VIDEO_IMAGE = "image"; //$NON-NLS-1$
	public static final String PROPERTY_VIDEO_SIZE = "size";//$NON-NLS-1$
	public static final String PROPERTY_VIDEO_VIDEOVISIBLE = "videoVisible";//$NON-NLS-1$

	
    public default void removeListener(PropertyChangeListener c) {
		removePropertyChangeListener(PROPERTY_VIDEO_COORDS, c); 
		removePropertyChangeListener(PROPERTY_VIDEO_FILTERCHANGED, c); 
		removePropertyChangeListener(PROPERTY_VIDEO_IMAGE, c); 
		removePropertyChangeListener(PROPERTY_VIDEO_SIZE, c); 
		removePropertyChangeListener(PROPERTY_VIDEO_VIDEOVISIBLE, c); 
	}

    public default void addListener(PropertyChangeListener c) {
		addPropertyChangeListener(PROPERTY_VIDEO_COORDS, c); 
		addPropertyChangeListener(PROPERTY_VIDEO_FILTERCHANGED, c); 
		addPropertyChangeListener(PROPERTY_VIDEO_IMAGE, c); 
		addPropertyChangeListener(PROPERTY_VIDEO_SIZE, c); 
		addPropertyChangeListener(PROPERTY_VIDEO_VIDEOVISIBLE, c); 
	}

	public final static String PROPERTY_VIDEO_FRAMENUMBER = "framenumber"; //$NON-NLS-1$
	public final static String PROPERTY_VIDEO_NEXTFRAME = "nextframe";//$NON-NLS-1$
	public final static String PROPERTY_VIDEO_ENDFRAME = "endframe";//$NON-NLS-1$


	public final static String PROPERTY_VIDEO_LOOPING = "looping"; //$NON-NLS-1$
	public final static String PROPERTY_VIDEO_PLAYING = "playing"; //$NON-NLS-1$
	public final static String PROPERTY_VIDEO_RATE = "rate"; //$NON-NLS-1$

	
	/**
	 * Gets the image size as a Dimension
	 * @param withFilters true to return size displayed after filters are applied
	 */
	public Dimension getImageSize(boolean withFilters);


	/**
	 * Steps forward in the 
	 */
	public void step();

	/**
	 * Steps backward in the video.
	 */
	public void back();

	/**
	 * Gets the total number of frames.
	 * 
	 * @return the number of frames in the image sequence
	 */
	public int getFrameCount();

	/**
	 * Gets the current frame number.
	 * 
	 * @return the number of the current frame
	 */
	public int getFrameNumber();

	/**
	 * Sets the frame number.
	 * 
	 * @param n a number between getStartFrameNumber() and getEndFrameNumber()
	 * @see #getStartFrameNumber
	 * @see #getEndFrameNumber
	 */
	public void setFrameNumber(int n);

	/**
	 * Gets the start frame number.
	 * 
	 * @return the number of the start frame
	 * @see #getEndFrameNumber
	 */
	public int getStartFrameNumber();

	/**
	 * Sets the start frame number.
	 * 
	 * @param n a number between 0 and getEndFrameNumber()
	 * @see #setEndFrameNumber
	 */
	public void setStartFrameNumber(int n);

	/**
	 * Gets the end frame number.
	 * 
	 * @return the number of the end frame
	 * @see #getStartFrameNumber
	 */
	public int getEndFrameNumber();

	/**
	 * Sets the end frame number.
	 * 
	 * @param n a number between getStartFrameNumber() and getFrameCount()
	 * @see #setStartFrameNumber
	 */
	
	public void setEndFrameNumber(int n);

	/**
	 * Gets the start time of the specified frame in milliseconds.
	 * 
	 * @param n the frame number
	 * @return the start time of the frame in milliseconds
	 */
	public double getFrameTime(int n);

	/**
	 * Gets the duration of the specified frame in milliseconds.
	 * 
	 * @param n the frame number
	 * @return the duration of the frame in milliseconds
	 */
	public double getFrameDuration(int n);

	/**
	 * Sets x position of UL corner of the specified video frame in world units.
	 *
	 * @param n the video frame number
	 * @param x the world x position
	 */
	public void setFrameX(int n, double x);

	/**
	 * Sets y position of UL corner of the specified video frame in world units.
	 *
	 * @param n the video frame number
	 * @param y the world y position
	 */
	public void setFrameY(int n, double y);

	/**
	 * Sets the x and y position of the UL corner of the specified video frame in
	 * world units.
	 *
	 * @param n the video frame number
	 * @param x the world x position
	 * @param y the world y position
	 */
	public void setFrameXY(int n, double x, double y);

	/**
	 * Sets the relative aspect of the specified video frame. The pixel aspect of an
	 * image is the ratio of its pixel width to height. Its world aspect is the
	 * ratio of width to height in world units. For example, a 320 x 240 pixel image
	 * has a pixel aspect of 4/3. If its relative aspect is set to 2, then the world
	 * aspect of the image will be 8/3. This means that if the image width is set to
	 * 16, its height will be 6. Conversely, if its height is set to 10, its width
	 * will be 8/3 x 10 = 26.666.
	 *
	 * @param n              the video frame number
	 * @param relativeAspect the world aspect of the image relative to its pixel
	 *                       aspect.
	 */
	public void setFrameRelativeAspect(int n, double relativeAspect);

	/**
	 * Sets the width of the specified video frame in world units. This method also
	 * sets the height using the relative aspect.
	 *
	 * @param n     the video frame number
	 * @param width the width in world units
	 */
	public void setFrameWidth(int n, double width);

	/**
	 * Sets the height of the specified video frame in world units. This method also
	 * sets the width using the relative aspect.
	 *
	 * @param n      the video frame number
	 * @param height the height in world units
	 */
	public void setFrameHeight(int n, double height);

	/**
	 * Sets the angle in radians of the specified video frame measured ccw from the
	 * world x-axis.
	 *
	 * @param n     the video frame number
	 * @param angle the angle n radians
	 */
	public void setFrameAngle(int n, double angle);

	/**
	 * Disposes of this video.
	 */
	public void dispose();

	/**
	 * Returns the VideoType name of this video.
	 */
	public String getTypeName();

	default public boolean isValid() {
		return getDuration() > 0;
	}
	
	  /**
	   * Adds a PropertyChangeListener to this object.
	   *
	   * @param listener the listener requesting property change notification
	   */
	  void addPropertyChangeListener(PropertyChangeListener listener);

	  /**
	   * Adds a PropertyChangeListener to this object.
	   *
	   * @param property the name of the property of interest to the listener
	   * @param listener the listener requesting property change notification
	   */
	  void addPropertyChangeListener(String property, PropertyChangeListener listener);

	  /**
	   * Removes a PropertyChangeListener from this object.
	   *
	   * @param listener the listener requesting removal
	   */
	  void removePropertyChangeListener(PropertyChangeListener listener);

	  /**
	   * Removes a PropertyChangeListener from this object.
	   *
	   * @param property the name of the property of interest to the listener
	   * @param listener the listener requesting removal
	   */
	  void removePropertyChangeListener(String property, PropertyChangeListener listener);

	  
	  // was Playable
	  
	  /**
	   * Plays the media.
	   */
	  public void play();

	  /**
	   * Stops the media.
	   */
	  public void stop();

	  /**
	   * Resets the media.
	   */
	  public void reset();

//	  /**
//	   * Gets the current media time in milliseconds.
//	   *
//	   * @return the current time in milliseconds
//	   */
//	  public double getTime();
//
//	  /**
//	   * Sets the media time in milliseconds.
//	   *
//	   * @param millis the desired time in milliseconds
//	   */
//	  public void setTime(double millis);

	  /**
	   * Gets the start time in milliseconds.
	   *
	   * @return the start time in milliseconds
	   */
	  public double getStartTime();

	  /**
	   * Sets the start time in milliseconds.
	   *
	   * @param millis the desired start time in milliseconds
	   */
	  public void setStartTime(double millis);

	  /**
	   * Gets the end time in milliseconds.
	   *
	   * @return the end time in milliseconds
	   */
	  public double getEndTime();

	  /**
	   * Sets the end time in milliseconds.
	   *
	   * @param millis the desired end time in milliseconds
	   */
	  public void setEndTime(double millis);

	  /**
	   * Sets the time to the start time.
	   */
	  public void goToStart();

	  /**
	   * Sets the time to the end time.
	   */
	  public void goToEnd();

	  /**
	   * Gets the duration of the media, including a time for the last frame
	   *
	   * @return the duration of the media in milliseconds or -1
	   */
	  public double getDuration();
	  
	  /**
	   * Gets the rate at which the media plays relative to its normal rate.
	   *
	   * @return the relative play rate. A rate of 1.0 plays at the normal rate.
	   */
	  public double getRate();

	  /**
	   * Sets the rate at which the media plays relative to its normal rate.
	   *
	   * @param rate the relative play rate. A rate of 1.0 plays at the normal rate.
	   */
	  public void setRate(double rate);

	  
	  ////////////// common for all adapters -- in VideoAdapter only
	  
	  /**
	   * Starts and stops the media.
	   *
	   * @param playing <code>true</code> starts the media, and
	   * <code>false</code> stops it
	   */
	  public void setPlaying(boolean playing);

	  /**
	   * Gets whether the media is playing.
	   *
	   * @return <code>true</code> if the media is playing
	   */
	  public boolean isPlaying();

	  /**
	   * Sets the looping behavior of the media.
	   * When true, the media restarts when reaching the end.
	   *
	   * @param looping <code>true</code> if the media is looping
	   */
	  public void setLooping(boolean looping);

	  /**
	   * Gets the looping behavior of the media.
	   * When true, the video restarts when reaching the end.
	   *
	   * @return <code>true</code> if the media is looping
	   */
	  public boolean isLooping();

	/**
	 * Invalidate the video and its filter so that it can be refreshed and so that
	 * data can be rebuilt.
	 * 
	 */
	  public void invalidateVideoAndFilter();

}

/*
 * Open Source Physics software is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.

 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be released
 * under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2024  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
