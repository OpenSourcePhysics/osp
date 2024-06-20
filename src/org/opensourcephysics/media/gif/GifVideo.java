/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

/*
 * The org.opensourcephysics.media package defines the Open Source Physics
 * media framework for working with video and other media.
 *
 * Copyright (c) 2024  Douglas Brown and Wolfgang Christian.
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
package org.opensourcephysics.media.gif;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.media.core.DoubleArray;
import org.opensourcephysics.media.core.ImageCoordSystem;
import org.opensourcephysics.media.core.Video;
import org.opensourcephysics.media.core.VideoAdapter;
import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.media.core.VideoType;
import org.opensourcephysics.tools.Resource;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * This is a video that wraps an animated gif image.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class GifVideo extends VideoAdapter {
	// instance fields
	protected GifDecoder decoder;
	private javax.swing.Timer timer;
	private HashSet<DrawingPanel> panels = new HashSet<DrawingPanel>();

	/**
	 * Creates a GifVideo and loads a gif image specified by name
	 *
	 * @param gifName the name of the image file
	 * @throws IOException
	 */
	public GifVideo(String gifName) throws IOException {
		load(gifName);
		createTimer();
	}

	/**
	 * Draws the video image on the panel.
	 *
	 * @param panel the drawing panel requesting the drawing
	 * @param g     the graphics context on which to draw
	 */
	@Override
	public void draw(DrawingPanel panel, Graphics g) {
		panels.add(panel);
		super.draw(panel, g);
	}

	/**
	 * Plays the video at the current rate.
	 */
	@Override
	public void play() {
		if (getFrameCount() == 1) {
			return;
		}
		if (!timer.isRunning()) {
			if (getFrameNumber() >= getEndFrameNumber()) {
				setFrameNumber(getStartFrameNumber());
			}
			timer.restart();
			firePropertyChange(Video.PROPERTY_VIDEO_PLAYING, null, Boolean.TRUE); // $NON-NLS-1$
		}
	}

	/**
	 * Stops the video.
	 */
	@Override
	public void stop() {
		if (timer.isRunning()) {
			timer.stop();
			firePropertyChange(Video.PROPERTY_VIDEO_PLAYING, null, Boolean.FALSE); // $NON-NLS-1$
		}
	}

	/**
	 * Overrides ImageVideo setFrameNumber method.
	 *
	 * @param n the desired frame number
	 */
	@Override
	public void setFrameNumber(int n) {
		super.setFrameNumber(n);
		n = getFrameNumber(); // may have been bounded
		int index = Math.min(n, decoder.getFrameCount() - 1);
		rawImage = decoder.getFrame(index);
		invalidateVideoAndFilter();
		notifyFrame(n, false);
		// repaint panels in case they don't listen
		Iterator<DrawingPanel> it = panels.iterator();
		while (it.hasNext()) {
			DrawingPanel panel = it.next();
			panel.repaint();
		}
	}

	/**
	 * Gets the end time in milliseconds.
	 *
	 * @return the end time in milliseconds, or -1 if not known
	 */
	@Override
	public double getEndTime() {
		int n = getEndFrameNumber();
		return getFrameTime(n) + decoder.getDelay(n);
	}

	/**
	 * Gets the duration of the video.
	 *
	 * @return the duration of the video in milliseconds, or -1 if not known
	 */
	@Override
	public double getFrameCountDurationMS() {
		int n = getFrameCount() - 1;
		return getFrameTime(n) + decoder.getDelay(n);
	}

	/**
	 * Loads a gif image specified by name.
	 *
	 * @param gifName the gif image name
	 * @throws IOException
	 */
	protected void load(String gifName) throws IOException {
		decoder = new GifDecoder();
		int status = decoder.read(gifName);
		if (status == GifDecoder.STATUS_OPEN_ERROR) {
			throw new IOException("Gif " + gifName + " not found"); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (status == GifDecoder.STATUS_FORMAT_ERROR) {
			throw new IOException("File format error"); //$NON-NLS-1$
		}
		setProperty("name", gifName); //$NON-NLS-1$
		// set path to be saved in XMLControl
		if (gifName.indexOf(":") == -1) { //$NON-NLS-1$
			// if name is relative, path is name
			setProperty("path", XML.forwardSlash(gifName)); //$NON-NLS-1$
			Resource res = ResourceLoader.getResource(gifName);
			if (res != null)
				setProperty("absolutePath", res.getAbsolutePath()); //$NON-NLS-1$
		} else {
			// else path is relative to user directory
			setProperty("path", XML.getRelativePath(gifName)); //$NON-NLS-1$
			setProperty("absolutePath", gifName); //$NON-NLS-1$
		}
		setFrameCount(decoder.getFrameCount());
		startFrameNumber = 0;
		endFrameNumber = frameCount - 1;
		// create startTimes array
		setStartTimes();
		setImage(decoder.getFrame(0));
	}

	@Override
	protected void setStartTimes() {
		startTimesMS = new double[frameCount];
		startTimesMS[0] = 0;
		for (int i = 1; i < startTimesMS.length; i++) {
			startTimesMS[i] = startTimesMS[i - 1] + decoder.getDelay(i - 1);
		}
	}

	/**
	 * Sets the image.
	 *
	 * @param image the image
	 */
	private void setImage(BufferedImage image) {
		rawImage = image;
		size.width = image.getWidth();
		size.height = image.getHeight();
		refreshBufferedImage();
		// create coordinate system and relativeAspects
		coords = new ImageCoordSystem(frameCount, this);
		aspects = new DoubleArray(frameCount, 1);
	}

	/**
	 * Creates the timer.
	 */
	private void createTimer() {
		int delay = decoder.getDelay(0);
		timer = new javax.swing.Timer(delay, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (getFrameNumber() < getEndFrameNumber()) {
					int delay = decoder.getDelay(getFrameNumber() + 1);
					timer.setDelay((int) (delay / getRate()));
					setFrameNumber(getFrameNumber() + 1);
				} else if (looping) {
					int delay = decoder.getDelay(getStartFrameNumber());
					timer.setDelay((int) (delay / getRate()));
					setFrameNumber(getStartFrameNumber());
				} else {
					stop();
				}
			}

		});
	}

	/**
	 * Returns an XML.ObjectLoader to save and load GifVideo data.
	 *
	 * @return the object loader
	 */
	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

	/**
	 * A class to save and load GifVideo data.
	 */
	static class Loader extends VideoAdapter.Loader {

		@Override
		protected VideoAdapter createVideo(String path) throws IOException {
			GifVideo video = new GifVideo(path);
			VideoType gifType = VideoIO.getVideoType(GifVideoType.TYPE_GIF, null);
			if (gifType != null)
				video.setProperty("video_type", gifType); //$NON-NLS-1$
			return video;
		}

	}

	@Override
	public String getTypeName() {
		return GifVideoType.TYPE_GIF;
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
 * Copyright (c) 2024 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
