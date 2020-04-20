/*
 * The org.opensourcephysics.media.frame package provides video
 * frame services including implementations of the Video and VideoRecorder interfaces
 * using Xuggle (Java) and JS (JavaScript -- our minimal implementation).
 *
 * Copyright (c) 2017  Douglas Brown and Wolfgang Christian.
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
 * please see <https://www.compadre.org/osp/>.
 */
package org.opensourcephysics.media.mov;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.TreeSet;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.media.core.MediaRes;
import org.opensourcephysics.media.core.VideoFileFilter;
import org.opensourcephysics.media.core.Video;
import org.opensourcephysics.media.core.VideoRecorder;
import org.opensourcephysics.media.core.VideoType;

/**
 * This implements the VideoType interface with a Xuggle or JS type.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class MovieVideoType implements VideoType {
	
  protected static TreeSet<VideoFileFilter> movieFileFilters 
  		= new TreeSet<VideoFileFilter>();
  protected static PropertyChangeListener errorListener;
  protected static boolean isMovieTypeAvailable = true;
  protected boolean recordable = OSPRuntime.canRecordMovieFiles;
  
  static {
  	errorListener = new PropertyChangeListener() {
    	public void propertyChange(PropertyChangeEvent e) {
    		if (e.getPropertyName().equals("xuggle_error")) { //$NON-NLS-1$
    			isMovieTypeAvailable = false;
    		}
    	}
  	};
    OSPLog.getOSPLog().addPropertyChangeListener(errorListener);
    MovieFactory.startXtractorThumbnailTool();
  }
  
  private VideoFileFilter singleTypeFilter; // null for general type
    
  /**
   * Constructor attempts to load a movie class the first time used.
   * This will throw an error if movies are not available.
   */
  public MovieVideoType() {
	  if (OSPRuntime.isJS && OSPRuntime.canReadJSMovieFiles)
		  return;
  	if (!isMovieTypeAvailable)
			throw new Error("Video frame extractor unavailable"); //$NON-NLS-1$
  	boolean logConsole = OSPLog.isConsoleMessagesLogged();
  	try {
    	OSPLog.setConsoleMessagesLogged(false);
  		Class.forName(MovieFactory.getMovieClass());
    	OSPLog.setConsoleMessagesLogged(logConsole);
		} catch (Exception ex) {
    	OSPLog.setConsoleMessagesLogged(logConsole);
			throw new Error("Video frame extractor unavailable"); //$NON-NLS-1$
		}
  }

  /**
   * Constructor with a file filter for a specific container type.
   * 
   * @param filter the file filter 
   */
  public MovieVideoType(VideoFileFilter filter) {
  	this();
  	if (filter!=null) {
			singleTypeFilter = filter;
			movieFileFilters.add(filter);
  	}
  }

  /**
   * Opens a named video as a XtractorVideo.
   *
   * @param name the name of the video
   * @return a new Xtractor video
   */
  public Video getVideo(String name) { 
    	Video video = MovieFactory.newVideo(name, getDescription());
    	if (video != null)
    		video.setProperty("video_type", this); //$NON-NLS-1$
      return video;
  }

  /**
   * Reports whether this type can record videos
   *
   * @return true by default (set recordable to change)
   */
  public boolean canRecord() {
    return recordable;
  }

  /**
   * Sets the recordable property
   *
   * @param record true if recordable
   */
  public void setRecordable(boolean record) {
    recordable = record;
  }

  /**
   * Gets a Xuggle video recorder.
   *
   * @return the video recorder
   */
  public VideoRecorder getRecorder() {
  	return MovieFactory.newMovieVideoRecorder(this);  	
  }

  /**
   * Gets the file filters for this type.
   *
   * @return an array of file filters
   */
  public VideoFileFilter[] getFileFilters() {
  	if (singleTypeFilter!=null)
  		return new VideoFileFilter[] {singleTypeFilter};
    return movieFileFilters.toArray(new VideoFileFilter[0]);
  }

  /**
   * Gets the default file filter for this type. May return null.
   *
   * @return the default file filter
   */
  public VideoFileFilter getDefaultFileFilter() {
  	if (singleTypeFilter!=null)
  		return singleTypeFilter;
  	return null;
  }
  
  /**
   * Return true if the specified video is this type.
   *
   * @param video the video
   * @return true if the video is this type
   */
  public boolean isType(Video video) {
  	if (!(video instanceof MovieVideoI)) return false;
  	if (singleTypeFilter==null) return true;
  	String name = (String)video.getProperty("name"); //$NON-NLS-1$
  	return singleTypeFilter.accept(new File(name));
  }

  /**
   * Gets the name and/or description of this type.
   *
   * @return a description
   */
  public String getDescription() {
  	if (singleTypeFilter!=null)
  		return singleTypeFilter.getDescription();
    return MediaRes.getString("XuggleVideoType.Description"); //$NON-NLS-1$
  }

  /**
   * Gets the default extension for this type.
   *
   * @return an extension
   */
  public String getDefaultExtension() {
  	if (singleTypeFilter!=null) {
  		return singleTypeFilter.getDefaultExtension();
  	}
    return null;
  }

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
 * Copyright (c) 2017  The Open Source Physics project
 *                     https://www.compadre.org/osp
 */
