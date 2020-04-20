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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.media.core.DoubleArray;
import org.opensourcephysics.media.core.ImageCoordSystem;
import org.opensourcephysics.media.core.VideoAdapter;
import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.media.core.VideoType;
import org.opensourcephysics.tools.Resource;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * This is a video that uses HTML5/JS to read mp4, mov, and other movie video formats.
 *
 * status: Only fleshed in; not implemented.
 * 
 * @author Douglas Brown
 * @version 1.0
 */
public class JSMovieVideo extends VideoAdapter implements MovieVideoI {
	
		/**
		 * File read status: No errors.
		 */
		public static final int STATUS_OK = 0;

		/**
		 * File read status: Error decoding file (may be partially decoded)
		 */
		public static final int STATUS_FORMAT_ERROR = 1;

		/**
		 * File read status: Unable to open source.
		 */
		public static final int STATUS_OPEN_ERROR = 2;

		public int getFrameCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		public BufferedImage getFrame(int index) {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * 
		 * @param n
		 * @return delay in ms
		 */
		public int getDelay(int n) {
			// TODO Auto-generated method stub
			return 0;
		}

		public int read(String movName) {
			// TODO Auto-generated method stub
			return 0;
		}


	// instance fields
	protected int[] startTimes; // in milliseconds
	private javax.swing.Timer timer;
	private HashSet<DrawingPanel> panels = new HashSet<DrawingPanel>();

	/**
	 * Creates a JSVideo and loads a mov video specified by name
	 *
	 * @param movName the name of the video file
	 * @throws IOException
	 */
	public JSMovieVideo(String movName) throws IOException {
		load(movName);
		createTimer();
	}

	/**
	 * Draws the video image on the panel.
	 *
	 * @param panel the drawing panel requesting the drawing
	 * @param g     the graphics context on which to draw
	 */
	public void draw(DrawingPanel panel, Graphics g) {
		panels.add(panel);
		super.draw(panel, g);
	}

	/**
	 * Plays the video at the current rate.
	 */
	public void play() {
		if (getFrameCount() == 1) {
			return;
		}
		if (!timer.isRunning()) {
			if (getFrameNumber() >= getEndFrameNumber()) {
				setFrameNumber(getStartFrameNumber());
			}
			timer.restart();
			support.firePropertyChange("playing", null, Boolean.TRUE); //$NON-NLS-1$
		}
	}

	/**
	 * Stops the video.
	 */
	public void stop() {
		if (timer.isRunning()) {
			timer.stop();
			support.firePropertyChange("playing", null, Boolean.FALSE); //$NON-NLS-1$
		}
	}

	/**
	 * Overrides VideoAdapter setFrameNumber method.
	 *
	 * @param n the desired frame number
	 */
	@Override
	public void setFrameNumber(int n) {
		super.setFrameNumber(n);
		n = getFrameNumber();
		int index = Math.min(n, getFrameCount() - 1);
		rawImage = getFrame(index);
		isValidImage = false;
		isValidFilteredImage = false;
		support.firePropertyChange("framenumber", null, Integer.valueOf(n)); //$NON-NLS-1$
		// repaint panels in case they don't listen
		Iterator<DrawingPanel> it = panels.iterator();
		while (it.hasNext()) {
			DrawingPanel panel = it.next();
			panel.repaint();
		}
	}

	/**
	 * Gets the start time of the specified frame in milliseconds.
	 *
	 * @param n the frame number
	 * @return the start time of the frame in milliseconds, or -1 if not known
	 */
	public double getFrameTime(int n) {
		if ((n >= startTimes.length) || (n < 0)) {
			return -1;
		}
		return startTimes[n];
	}

	/**
	 * Gets the current video time in milliseconds.
	 *
	 * @return the current time in milliseconds, or -1 if not known
	 */
	public double getTime() {
		return getFrameTime(getFrameNumber());
	}

	/**
	 * Sets the video time in milliseconds.
	 *
	 * @param millis the desired time in milliseconds
	 */
	public void setTime(double millis) {
		millis = Math.abs(millis);
		for (int i = 0; i < startTimes.length; i++) {
			int t = startTimes[i];
			if (millis < t) { // find first frame with later start time
				setFrameNumber(i - 1);
				break;
			}
		}
	}

	/**
	 * Gets the start time in milliseconds.
	 *
	 * @return the start time in milliseconds, or -1 if not known
	 */
	public double getStartTime() {
		return getFrameTime(getStartFrameNumber());
	}

	/**
	 * Sets the start time in milliseconds. NOTE: the actual start time is normally
	 * set to the beginning of a frame.
	 *
	 * @param millis the desired start time in milliseconds
	 */
	public void setStartTime(double millis) {
		millis = Math.abs(millis);
		for (int i = 0; i < startTimes.length; i++) {
			int t = startTimes[i];
			if (millis < t) { // find first frame with later start time
				setStartFrameNumber(i - 1);
				break;
			}
		}
	}

	/**
	 * Gets the end time in milliseconds.
	 *
	 * @return the end time in milliseconds, or -1 if not known
	 */
	public double getEndTime() {
		int n = getEndFrameNumber();
		return getFrameTime(n) + getDelay(n);
	}

	/**
	 * Sets the end time in milliseconds. NOTE: the actual end time is set to the
	 * end of a frame.
	 *
	 * @param millis the desired end time in milliseconds
	 */
	public void setEndTime(double millis) {
		millis = Math.abs(millis);
		millis = Math.min(getDuration(), millis);
		for (int i = 0; i < startTimes.length; i++) {
			int t = startTimes[i];
			if (millis < t) { // find first frame with later start time
				setEndFrameNumber(i - 1);
				break;
			}
		}
	}

	/**
	 * Gets the duration of the video.
	 *
	 * @return the duration of the video in milliseconds, or -1 if not known
	 */
	public double getDuration() {
		int n = getFrameCount() - 1;
		return getFrameTime(n) + getDelay(n);
	}

	/**
	 * Loads a mov video specified by name.
	 *
	 * @param movName the mov video name
	 * @throws IOException
	 */
	protected void load(String movName) throws IOException {
		int status = read(movName);
		if (status == STATUS_OPEN_ERROR) {
			throw new IOException("Gif " + movName + " not found"); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (status == STATUS_FORMAT_ERROR) {
			throw new IOException("File format error"); //$NON-NLS-1$
		}
		setProperty("name", movName); //$NON-NLS-1$
		// set path to be saved in XMLControl
		if (movName.indexOf(":") == -1) { //$NON-NLS-1$
			// if name is relative, path is name
			setProperty("path", XML.forwardSlash(movName)); //$NON-NLS-1$
			Resource res = ResourceLoader.getResource(movName);
			if (res != null)
				setProperty("absolutePath", res.getAbsolutePath()); //$NON-NLS-1$
		} else {
			// else path is relative to user directory
			setProperty("path", XML.getRelativePath(movName)); //$NON-NLS-1$
			setProperty("absolutePath", movName); //$NON-NLS-1$
		}
		frameCount = getFrameCount();
		startFrameNumber = 0;
		endFrameNumber = frameCount - 1;
		// create startTimes array
		startTimes = new int[frameCount];
		startTimes[0] = 0;
		for (int i = 1; i < startTimes.length; i++) {
			startTimes[i] = startTimes[i - 1] + getDelay(i - 1);
		}
		setImage(getFrame(0));
	}

	/**
	 * Sets the image.
	 *
	 * @param image the image
	 */
	private void setImage(BufferedImage image) {
		rawImage = image;
		size = new Dimension(image.getWidth(), image.getHeight());
		refreshBufferedImage();
		// create coordinate system and relativeAspects
		coords = new ImageCoordSystem(frameCount);
		coords.addPropertyChangeListener(this);
		aspects = new DoubleArray(frameCount, 1);
	}

	/**
	 * Creates the timer.
	 */
	private void createTimer() {
		int delay = getDelay(0);
		timer = new javax.swing.Timer(delay, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (getFrameNumber() < getEndFrameNumber()) {
					int delay = getDelay(getFrameNumber() + 1);
					timer.setDelay((int) (delay / getRate()));
					setFrameNumber(getFrameNumber() + 1);
				} else if (looping) {
					int delay = getDelay(getStartFrameNumber());
					timer.setDelay((int) (delay / getRate()));
					setFrameNumber(getStartFrameNumber());
				} else {
					stop();
				}
			}

		});
	}

	public static File createThumbnailFile(Object[] values) {
		//	{ defaultThumbnailDimension, sourcePath, thumbPath };
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Returns an XML.ObjectLoader to save and load JSVideo data.
	 *
	 * @return the object loader
	 */
	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}


	/**
	 * A class to save and load JMovieVideo data.
	 */
	static class Loader extends VideoAdapter.Loader implements XML.ObjectLoader {
		
		protected VideoAdapter createVideo(String path) throws IOException {
			JSMovieVideo video = new JSMovieVideo(path);
			VideoType type = VideoIO.getVideoType("mov", null); //$NON-NLS-1$
			if (type != null)
				video.setProperty("video_type", type); //$NON-NLS-1$
			return video;
		}

	}

	@Override
	public void init(String fileName) throws IOException {
		// n/a
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
