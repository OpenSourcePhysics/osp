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

import java.util.TreeSet;

import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.media.core.VideoFileFilter;
import org.opensourcephysics.media.core.VideoType;

/**
 * This implements the VideoType interface for "movie" video files. Video
 * engines like Xuggle and JS are subclasses of MovieVideoType.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public abstract class MovieVideoType implements VideoType, MovieVideoI {

	protected static TreeSet<VideoFileFilter> movieFileFilters = new TreeSet<VideoFileFilter>();
	protected boolean recordable = true;

	static {
		MovieFactory.startMovieThumbnailTool();
	}

	protected VideoFileFilter singleTypeFilter; // null for general type

	/**
	 * No-arg constructor (no longer attempts calls the MovieFactory).
	 */
	public MovieVideoType() {
	}

	/**
	 * Constructor with a file filter for a specific container type.
	 * 
	 * @param filter the file filter
	 */
	public MovieVideoType(VideoFileFilter filter) {
		this();
		if (filter != null) {
			singleTypeFilter = filter;
			movieFileFilters.add(filter);
		}
	}

	/**
	 * Reports whether this type can record videos
	 *
	 * @return true by default (set recordable to change)
	 */
	@Override
	public boolean canRecord() {
		return recordable;
	}

	/**
	 * Sets the recordable property - Only Xuggle movie factory can do this.
	 *
	 * @param recordable true if recordable
	 */
	public void setRecordable(boolean recordable) {
		this.recordable = recordable;
	}

	/**
	 * Gets the file filters for this type.
	 *
	 * @return an array of file filters
	 */
	@Override
	public VideoFileFilter[] getFileFilters() {
		if (singleTypeFilter != null)
			return new VideoFileFilter[] { singleTypeFilter };
		return movieFileFilters.toArray(new VideoFileFilter[0]);
	}

	/**
	 * Gets the default file filter for this type. May return null.
	 *
	 * @return the default file filter
	 */
	@Override
	public VideoFileFilter getDefaultFileFilter() {
		if (singleTypeFilter != null)
			return singleTypeFilter;
		return null;
	}

	/**
	 * Gets the name and/or description of this type.
	 *
	 * @return a description
	 */
	@Override
	public String getDescription() {
		return (singleTypeFilter == null ? null : singleTypeFilter.getDescription());
	}

	/**
	 * Gets the default extension for this type.
	 *
	 * @return an extension
	 */
	@Override
	public String getDefaultExtension() {
		if (singleTypeFilter != null) {
			return singleTypeFilter.getDefaultExtension();
		}
		return null;
	}

}

/*
 * Open Source Physics software is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.
 * 
 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be
 * released under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston MA 02111-1307 USA or view the license online at
 * http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017 The Open Source Physics project
 * https://www.compadre.org/osp
 */
