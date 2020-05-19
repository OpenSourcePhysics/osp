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
 * please see <http://www.opensourcephysics.org/>.
 */
package org.opensourcephysics.media.core;

import java.io.File;

/**
 * This defines methods common to all video types.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public interface VideoType {
	
	public static final String TYPE_GIF = "Gif"; //$NON-NLS-1$
	public static final String TYPE_IMAGE = "Image"; //$NON-NLS-1$



  /**
   * Opens a new video with the specified name.
   *
   * @param path the path to the video
 * @param basePath 
   * @return the new video
   */
  public Video getVideo(String path);
  
  public Video getVideo(String path, String basePath);

  /**
   * Return true if the specified video is this type.
   *
   * @param video the video
   * @return true if the video is this type
   */
  public boolean isType(Video video);

  /**
   * Gets a video recorder. Returns null if canRecord() is false.
   *
   * @return the video recorder
   */
  public VideoRecorder getRecorder();

  /**
   * Reports whether this type can record videos
   *
   * @return true if this can record videos
   */
  public boolean canRecord();

  /**
   * Gets the name/description of this type.
   *
   * @return a description
   */
  public String getDescription();

  /**
   * Gets the default extension for this type.
   *
   * @return a description
   */
  public String getDefaultExtension();

  /**
   * Gets the file filters for this type.
   *
   * @return array of file filters
   */
  public VideoFileFilter[] getFileFilters();

  /**
   * Gets the default file filter for this type. May return null.
   *
   * @return the default file filter
   */
  public VideoFileFilter getDefaultFileFilter();

  /**
   * Gets the short name of the video type.
   *
   * @return the type name
   */
  public String getTypeName();

  /**
   * Checks to see if this file fits this type based on file extension.
   * 
   * @param file
   * @return
   */
  default public boolean accepts(File file) {
	  VideoFileFilter[] filters = getFileFilters();
	  for (int i = filters.length; --i >= 0;)
		  if (filters[i].accept(file))
			  return true;
	  return false;
  }
  
  default String _toString() {
	  String s = "";
	  for (VideoFileFilter f : getFileFilters()) {
		  s += " " + f;
	  }
		return "[" + getTypeName() + " "  + s + "]";
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
 *                     http://www.opensourcephysics.org
 */
