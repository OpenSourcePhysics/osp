/*
 * The org.opensourcephysics.media.mov package provides movie video services
 * and JavaScript implementations of Video and VideoType.
 *
 * Copyright (c) 2017  Robert Hanson, Douglas Brown and Wolfgang Christian.
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
import java.io.File;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.media.core.MediaRes;
import org.opensourcephysics.media.core.Video;
import org.opensourcephysics.media.core.VideoFileFilter;
import org.opensourcephysics.media.core.VideoRecorder;

/**
 * This implements the VideoType interface with a JS type.
 *
 * @author hansonr
 */
public class JSMovieVideoType extends MovieVideoType {
  
	/**
	 * No-arg constructor.
	 */
	public JSMovieVideoType() {
		super();
	}

  /**
   * Constructor with a file filter for a specific container type.
   * 
   * @param filter the file filter 
   */
  public JSMovieVideoType(VideoFileFilter filter) {
  	super(filter);
  }

  /**
   * Gets the name and/or description of this type.
   *
   * @return a description
   */
  public String getDescription() {
  	if (singleTypeFilter!=null)
  		return singleTypeFilter.getDescription();
    return MediaRes.getString("JSVideoType.Description"); //$NON-NLS-1$
  }
  
  /**
   * Return true if the specified video is this type.
   *
   * @param video the video
   * @return true if the video is this type
   */
  public boolean isType(Video video) {
  	if (!(video.getClass() == JSMovieVideo.class)) return false;
  	if (singleTypeFilter==null) return true;
  	String name = (String)video.getProperty("name"); //$NON-NLS-1$
  	return singleTypeFilter.accept(new File(name));
  }

  @Override
  public Video getVideo(String name) { 
	  return getVideo(name, null);
  }

	@Override
	public Video getVideo(String name, String basePath) {
		try {
			Video video = new JSMovieVideo(name, basePath);
  		video.setProperty("video_type", this); //$NON-NLS-1$
  		return video;
		} catch (Exception e) {
			if (name != null) {
				OSPLog.fine(getDescription() + ": " + e.getMessage()); //$NON-NLS-1$
			}
			e.printStackTrace();
		} 
    return null;
	}


  @Override
  public VideoRecorder getRecorder() {
		OSPLog.warning("JSMovieVideoType unable to record");
		return null;
  }

	@Override
	public String getTypeName() {
		return MovieFactory.ENGINE_JS;
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
