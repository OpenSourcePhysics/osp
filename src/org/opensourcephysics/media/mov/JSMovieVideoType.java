/*
 * The org.opensourcephysics.media.mov package provides movie video services
 * and JavaScript implementations of Video and VideoType.
 *
 * Copyright (c) 2024  Robert M. Hanson, Douglas Brown and Wolfgang Christian.
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
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.media.core.MediaRes;
import org.opensourcephysics.media.core.Video;
import org.opensourcephysics.media.core.VideoFileFilter;
import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.media.core.VideoRecorder;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * This implements the VideoType interface with a JS type.
 *
 * @author hansonr
 */
public class JSMovieVideoType extends MovieVideoType {
  
	static boolean registered;

	public static void register() {
		// add common video types 
		String[] JS_VIDEO_EXTENSIONS = {
				"mov", "mp4", "ogg" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		for (String ext : JS_VIDEO_EXTENSIONS) {
			VideoFileFilter filter = new VideoFileFilter(ext, new String[] { ext });
			VideoIO.addVideoType(new JSMovieVideoType(filter));
			ResourceLoader.addExtractExtension(ext);
		}
		registered = true;
	}

	/**
	 * Registers HTML5 video types with VideoIO class for file reading
	 *
	 * see https://en.wikipedia.org/wiki/HTML5_video#Browser_support
	 */
	static {
	   register();
	}
  	
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
	setRecordable(false);
  }

  /**
   * Gets the name and/or description of this type.
   *
   * @return a description
   */
  @Override
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
  @Override
public boolean isType(Video video) {
  	if (!(video.getClass() == JSMovieVideo.class)) return false;
  	if (singleTypeFilter==null) return true;
  	String name = (String)video.getProperty("name"); //$NON-NLS-1$
  	return singleTypeFilter.accept(new File(name));
  }

	@Override
	public Video getVideo(String name, String basePath, XMLControl control) {
		Video video = null;
		try {
			video = new JSMovieVideo(name, basePath, control);
			if (video.getFrameNumber() == Integer.MIN_VALUE) {
				video = null;
			} else {
				video.setProperty("video_type", this); //$NON-NLS-1$
			}
		} catch (Exception e) {
			if (name != null) {
				OSPLog.fine(getDescription() + ": " + e.getMessage()); //$NON-NLS-1$
				video = null;
			}
			e.printStackTrace();
		}
		return video;
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
 * Copyright (c) 2024  The Open Source Physics project
 *                     https://www.compadre.org/osp
 */
