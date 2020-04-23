/*
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

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.media.core.VideoFileFilter;
import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * This registers Xuggle with VideoIO so it can be used to open and record videos.
 *
 * @author Wolfgang Christian, Douglas Brown
 * @version 1.0
 */
public class JSMovieIO {
	
	/**
	 * Registers HTML5 video types with VideoIO class for file reading
	 *
	 * see https://en.wikipedia.org/wiki/HTML5_video#Browser_support
	 */
	static public void registerWithVideoIO() { // add Xuggle video types, if available
		try {
			VideoIO.addVideoEngine(new MovieVideoType());

			// add common video types
			for (String ext : VideoIO.JS_VIDEO_EXTENSIONS) { // {"mov", "ogg", "mp4"}
				VideoFileFilter filter = new VideoFileFilter(ext, new String[] { ext });
				MovieVideoType movieType = new MovieVideoType(filter);
				// avi not recordable with xuggle
//          if (ext.equals("avi")) { //$NON-NLS-1$
				movieType.setRecordable(false);
//          }
				VideoIO.addVideoType(movieType);
				ResourceLoader.addExtractExtension(ext);
			}

		} catch (Throwable ex) { 
			OSPLog.config("JSMovieIO exception: " + ex.toString()); //$NON-NLS-1$
		}
	}
  
}
