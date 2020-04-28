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
import org.opensourcephysics.media.core.MediaRes;
import org.opensourcephysics.media.core.VideoFileFilter;

/**
 * This implements the VideoType interface with a Xuggle or JS type.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class JSMovieVideoType extends MovieVideoType {
  
	/**
	 * Constructor attempts to load a movie class the first time used. This will
	 * throw an error if movies are not available.
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
	  String type = super.getDefaultExtension();
	  return (type == null ? MediaRes.getString("JSVideoType.Description") : type);  //$NON-NLS-1$
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
