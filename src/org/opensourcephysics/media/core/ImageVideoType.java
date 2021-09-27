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
import java.io.IOException;
import java.util.ArrayList;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;

/**
 * This implements the VideoType interface with a buffered image type.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class ImageVideoType implements VideoType {

	private VideoFileFilter[] fileFilters;
	private boolean invalid;

	/**
	 * Default constructor uses all available file types.
	 */
	public ImageVideoType() {
	}

	/**
	 * Constructor with a file filter for a specific image type.
	 * 
	 * @param filter the file filter
	 */
	public ImageVideoType(VideoFileFilter filter) {
		this();
		if (filter != null) {
			fileFilters = new VideoFileFilter[] { filter };
		}
	}

	/**
	 * Opens a video file as an ImageVideo.
	 *
	 * @param file the video file
	 * @return a new image video
	 */
	public Video getVideo(File file) {
		try {
			Video video = new ImageVideo(file.getAbsolutePath(), null, true);
			video.setProperty("video_type", this); //$NON-NLS-1$
			return video;
		} catch (IOException ex) {
			return null;
		}
	}

	/**
	 * Opens a named image as an ImageVideo.
	 *
	 * @param name the name of the image
	 * @return a new image video
	 */
	@Override
	public Video getVideo(String name) {
		return getVideo(name, null);
	}

	@Override
	public Video getVideo(String name, String basePath) {
		
		if (getDefaultFileFilter() == VideoIO.zippedImageFileFilter &&
				VideoIO.zippedImageFileFilter.accept(new File(basePath, name))) {
			name = VideoIO.zippedImageFileFilter.getImagePaths()[0];
			basePath = null;
		}

		Video video;
		// if an XML file with the image name is found, load it in order to get frame
		// duration
		String xmlName = XML.stripExtension(basePath == null ? name : basePath + File.separator + name) + ".xml"; //$NON-NLS-1$
		XMLControl control = new XMLControlElement(new File(xmlName));
		control.setBasepath(basePath == null? XML.getDirectoryPath(name): basePath);
		if (!control.failedToRead() && control.getObjectClass() == ImageVideo.class) {
			video = (Video) control.loadObject(null);
		} else {
			// else load image(s) directly
			try {
				video = new ImageVideo(name, basePath, true);
				video.setProperty("video_type", this); //$NON-NLS-1$
			} catch (IOException ex) {
				video = null;
			}
		}
		invalid = (video == null);
		return video;
	}

	/**
	 * Gets a video recorder.
	 *
	 * @return the video recorder
	 */
	@Override
	public VideoRecorder getRecorder() {
		return new ImageVideoRecorder(this);
	}

	/**
	 * Reports whether this type can record videos
	 *
	 * @return true if this can record videos
	 */
	@Override
	public boolean canRecord() {
		return true;
	}

	/**
	 * Gets the name and/or description of this type.
	 *
	 * @return a description
	 */
	@Override
	public String getDescription() {
			return getFileFilters()[0].getDescription();
	}

	/**
	 * Gets the default extension for this type.
	 *
	 * @return a description
	 */
	@Override
	public String getDefaultExtension() {
			return getFileFilters()[0].getDefaultExtension();
	}

	/**
	 * Gets the file filters for this type.
	 *
	 * @return a file filter
	 */
	@Override
	public VideoFileFilter[] getFileFilters() {
		if (fileFilters == null) {
			ArrayList<VideoType> types = VideoIO.getVideoTypes(true);
			ArrayList<VideoFileFilter> filters = new ArrayList<VideoFileFilter>();
			for (VideoType next: types) {
				if (next instanceof ImageVideoType) {
					ImageVideoType imageType = (ImageVideoType) next;
					filters.add(imageType.getDefaultFileFilter());
				}
			}
			fileFilters = filters.toArray(new VideoFileFilter[filters.size()]);
		}
		return fileFilters;
	}

	/**
	 * Gets the default file filter for this type. May return null.
	 *
	 * @return the default file filter
	 */
	@Override
	public VideoFileFilter getDefaultFileFilter() {
		return getFileFilters()[0];
	}

	/**
	 * Return true if the specified video is this type.
	 *
	 * @param video the video
	 * @return true if the video is this type
	 */
	@Override
	public boolean isType(Video video) {
		return video instanceof ImageVideo;
	}

	@Override
	public String getTypeName() {
		return TYPE_IMAGE;
	}

	@Override
	public String toString() {
		return _toString();
	}

	@Override
	public boolean isValid() {
		return !invalid;
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
 * http://www.opensourcephysics.org
 */
