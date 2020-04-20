/*
 * The org.opensourcephysics.media.xuggle package provides Xuggle
 * services including implementations of the Video and VideoRecorder interfaces.
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
package org.opensourcephysics.media.xugglejs;

import java.io.IOException;

import org.opensourcephysics.media.core.VideoAdapter;
import org.opensourcephysics.media.mov.MovieVideoI;


/**
 * A class to display videos using the Xuggle library. Xuggle in turn
 * uses FFMpeg as its video engine.
 */
public class XuggleJSVideo extends VideoAdapter implements MovieVideoI {

	static {
		XuggleJSThumbnailTool.start();
	}

	@Override
	public void init(String fileName) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isSmoothPlay() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setSmoothPlay(boolean b) {
		// TODO Auto-generated method stub
		
	}
	
}
