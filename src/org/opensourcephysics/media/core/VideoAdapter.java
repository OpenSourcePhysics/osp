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
package org.opensourcephysics.media.core;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.SwingUtilities;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.display.Interactive;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * This provides basic implementations of all Video methods. Subclasses should
 * provide a raw image for display--see ImageVideo or GifVideo for an example.
 * 
 * All public methods implement Video
 *
 * @author Douglas Brown
 * @version 1.0
 */
public abstract class VideoAdapter extends OSPRuntime.Supported implements Video {
// instance fields
	
	protected Image rawImage; // raw image from video source
	protected Dimension size = new Dimension(); // image pixel dimensions
	protected Dimension displayedSize = new Dimension(); // image pixel dimensions with filters
	protected BufferedImage bufferedImage; // offscreen buffered image copy
	protected BufferedImage filteredImage; // filtered image
	protected String baseDir;
	protected int frameCount = 0;
	protected int frameNumber = 0;
	protected int startFrameNumber;
	protected int endFrameNumber;
	protected double rate = 1;
	protected boolean playing = false;
	protected boolean looping = false;
	protected double minX, maxX, minY, maxY;
	protected boolean mouseEnabled = false;
	protected boolean visible = true;
	protected boolean isMeasured = false;
	protected boolean isValidMeasure = false;
	protected boolean widthDominates = true;
	protected boolean isValidImage = false;
	protected boolean isValidFilteredImage = false;
	protected ImageCoordSystem coords;
	protected DoubleArray aspects;
	protected HashMap<String, Object> properties = new HashMap<String, Object>();
	protected FilterStack filterStack = new FilterStack();
	protected DataBufferInt clearRaster;

	/**
	 * startTimes in MS. Created from MediaInfo.analyzeData(JavaScript) or from
	 * Xuggle(Java); passed through XMLControl "video" in TRK/TRZ to other platforms
	 * 
	 * used in:
	 * 
	 * VideoAdapter.getEndTime(), .setEndTime(), .getFrameDuration()
	 * 
	 * Video.getAverageFrameRate(), .getOutliers(), isValid()
	 * 
	 * MovieVideo.getFrameNumberBefore(), .setFromControl(), .setStartTimes(),
	 * .finalizeLoading()
	 * 
	 * 
	 */
	protected double[] startTimesMS;

	/**
	 * set to false by TrackerIO.getImageBytes()
	 */
	private boolean doNotify = true;

	public void setNotify(boolean b) {
		doNotify = b;
	}


	private final static Point2D.Double corner = new Point2D.Double(0, 0);

	/**
	 * Protected constructor creates an empty Video
	 */
	protected VideoAdapter() {
		initialize();
	}

	/**
	 * Draws the video image on the panel.
	 *
	 * @param panel the drawing panel requesting the drawing
	 * @param g     the graphics context on which to draw
	 */
	@Override
	public void draw(DrawingPanel panel, Graphics g) {
		if (rawImage == null || !visible 
				|| filterStack == null  // BH 2021.09.11 disposed
				) {
			return;
		}
		int xoffset = 0, yoffset = 0;
		Graphics2D g2 = null;
		if (((panel instanceof VideoPanel) && ((VideoPanel) panel).isDrawingInImageSpace()) || isMeasured) {
			g2 = (Graphics2D) g.create();
			AffineTransform at = panel.getPixelTransform();
			// OSPLog.debug("Video.draw " + g2.getClip());
			g2.transform(at); // world to screen
			ImageCoordSystem coords = null;
			if (panel instanceof VideoPanel) {
				VideoPanel vidPanel = (VideoPanel) panel;
				if (!vidPanel.isDrawingInImageSpace()) {
					// use video panel's coords for vid to world transform
					coords = vidPanel.getCoords();
				}
			} else { // not a video panel, so draw in world space
				// use this video's coords for vid to world transform
				coords = this.coords;
			}
			if (coords != null) {
				at = coords.getToWorldTransform(frameNumber);
				g2.transform(at);
			}
			g2.drawImage(filterStack.isEmpty() || !filterStack.isEnabled() ? rawImage : getImage(), xoffset, yoffset, panel);
			g2.dispose();
		} else { // center image in panel if not measured
			double centerX = (panel.getXMax() + panel.getXMin()) / 2;
			double centerY = (panel.getYMax() + panel.getYMin()) / 2;
			xoffset = panel.xToPix(centerX) - size.width / 2;
			yoffset = panel.yToPix(centerY) - size.height / 2;
			g.drawImage(filterStack.isEmpty() || !filterStack.isEnabled() ? rawImage : getImage(), xoffset, yoffset, panel);
		}
	}

	@Override
	public final void invalidateVideoAndFilter() {
		isValidImage = isValidFilteredImage = false;
	}

	protected final void notifyFrame(int n, boolean isAsync) {
		if (!doNotify)
			return;
		// after subclass setFrameNumber(n) - asynchronous -- ImageVideo only??
		Runnable r = new Runnable() {

			@Override
			public void run() {
				firePropertyChange(Video.PROPERTY_VIDEO_FRAMENUMBER, null, Integer.valueOf(n));
			}
			
		};
		if (isAsync)
			SwingUtilities.invokeLater(r);
		else
			r.run();
	}

	/**
	 * Gets the start frame number.
	 *
	 * @return the start frame number
	 * @see #getEndFrameNumber
	 */
	@Override
	public final int getStartFrameNumber() {
		return startFrameNumber;
	}

	/**
	 * Sets the start frame number.
	 *
	 * @param n the desired start frame number
	 * @see #setEndFrameNumber
	 */
	@Override
	public final void setStartFrameNumber(int n) {
		if (n == startFrameNumber) {
			return;
		}
		n = Math.max(0, n);
		startFrameNumber = Math.min(endFrameNumber, n);
		firePropertyChange(VideoClip.PROPERTY_VIDEOCLIP_STARTFRAME, null, Integer.valueOf(startFrameNumber));
	}

	/**
	 * Gets the end frame number.
	 *
	 * @return the end frame number
	 * @see #getStartFrameNumber
	 */
	@Override
	public final int getEndFrameNumber() {
		return endFrameNumber;
	}

	/**
	 * Sets the end frame number.
	 *
	 * @param n the desired end frame number,
	 * @see #setStartFrameNumber
	 */
	@Override
	public final void setEndFrameNumber(int n) {
		if (n == endFrameNumber) {
			return;
		}
		if (frameCount > 1) {
			n = Math.min(frameCount - 1, n);
		}
		endFrameNumber = Math.max(startFrameNumber, n);
		firePropertyChange(PROPERTY_VIDEO_ENDFRAME, null, Integer.valueOf(endFrameNumber));
	}

	/**
	 * Stops the video and resets it to the start time.
	 */
	@Override
	public final void reset() {
		stop();
		setFrameNumber(startFrameNumber);
	}
	
	/**
	 * Sets the frame number to the start frame.
	 */
	@Override
	public final void goToStart() {
		setFrameNumber(startFrameNumber);
	}

	/**
	 * Sets the frame number to the end frame.
	 */
	@Override
	public final void goToEnd() {
		setFrameNumber(endFrameNumber);
	}


	/**
	 * Not used.
	 * 
	 * Starts and stops the video.
	 *
	 * @param playing <code>true</code> starts the video, and <code>false</code>
	 *                stops it
	 */
	@Deprecated
	@Override
	public final void setPlaying(boolean playing) {
		if (playing) {
			play();
		} else {
			stop();
		}
	}

	/**
	 * Gets the playing state of this video.
	 *
	 * @return <code>true</code> if the video is playing
	 */
	@Override
	public final boolean isPlaying() {
		return playing;
	}

	/**
	 * Sets the looping state of this video. If true, the video restarts when
	 * reaching the end.
	 *
	 * @param loops <code>true</code> if the video loops
	 */
	@Override
	public final void setLooping(boolean loops) {
		if (looping == loops) {
			return;
		}
		looping = loops;
		firePropertyChange(PROPERTY_VIDEO_LOOPING, null, Boolean.valueOf(looping));
	}

	/**
	 * Gets the looping state of the video. If true, the video restarts when
	 * reaching the end.
	 *
	 * @return <code>true</code> if the video loops
	 */
	@Override
	public final boolean isLooping() {
		return looping;
	}

	/**
	 * Gets the relative play rate. Relative play rate is the ratio of a video's
	 * play rate to its preferred ("normal") play rate.
	 *
	 * @return the relative play rate.
	 */
	@Override
	public final double getRate() {
		return rate;
	}

	/**
	 * Shows or hides the video.
	 *
	 * @param visible <code>true</code> to show the video
	 */
	@Override
	public final void setVisible(boolean visible) {
		this.visible = visible;
		firePropertyChange(PROPERTY_VIDEO_VIDEOVISIBLE, null, Boolean.valueOf(visible));
	}

	/**
	 * Gets the visibility of the video.
	 *
	 * @return <code>true</code> if the video is visible
	 */
	@Override
	public final boolean isVisible() {
		return visible;
	}

	/**
	 * Gets the minimum x needed to draw this object.
	 *
	 * @return minimum x
	 */
	@Override
	public final double getXMin() {
		if (!isValidMeasure) {
			findMinMaxValues();
		}
		return minX;
	}

	/**
	 * Gets the maximum x needed to draw this object.
	 *
	 * @return maximum x
	 */
	@Override
	public final double getXMax() {
		if (!isValidMeasure) {
			findMinMaxValues();
		}
		return maxX;
	}

	/**
	 * Gets the minimum y needed to draw this object.
	 *
	 * @return minimum y
	 */
	@Override
	public final double getYMin() {
		if (!isValidMeasure) {
			findMinMaxValues();
		}
		return minY;
	}

	/**
	 * Gets the maximum y needed to draw this object.
	 *
	 * @return maximum y
	 */
	@Override
	public final double getYMax() {
		if (!isValidMeasure) {
			findMinMaxValues();
		}
		return maxY;
	}

	/**
	 * Reports whether information is available to set min/max values.
	 *
	 * @return <code>true</code> if min/max values are valid
	 */
	@Override
	public final boolean isMeasured() {
		return isMeasured;
	}

	static int ntest = 0, ntest1 = 0, ntest2 = 0;

	public final BufferedImage getRawBufferedImage() {
		updateBufferedImage();
		return bufferedImage;
	}

	/**
	 * Returns this video if enabled.
	 *
	 * @param panel the drawing panel
	 * @param xpix  the x coordinate in pixels
	 * @param ypix  the y coordinate in pixels
	 * @return this if enabled, otherwise null
	 */
	@Override
	public final Interactive findInteractive(DrawingPanel panel, int xpix, int ypix) {
		return (mouseEnabled ? this : null);
	}

	/**
	 * Sets whether this responds to mouse hits.
	 *
	 * @param enabled <code>true</code> if this responds to mouse hits.
	 */
	@Override
	public final void setEnabled(boolean enabled) {
		mouseEnabled = enabled;
	}

	/**
	 * Gets whether this responds to mouse hits.
	 *
	 * @return <code>true</code> if this responds to mouse hits.
	 */
	@Override
	public final boolean isEnabled() {
		return mouseEnabled;
	}

	/**
	 * Sets x position of upper left corner of the specified video frame in world
	 * units.
	 *
	 * @param n the video frame number
	 * @param x the world x position
	 */
	@Override
	public final void setFrameX(int n, double x) {
		setFrameXY(n, x, coords.imageToWorldY(n, 0, 0));
	}

	/**
	 * Sets x position of upper left corner of all video frames in world units.
	 *
	 * @param x the world x position
	 */
	@Override
	public final void setX(double x) {
		for (int n = 0; n < frameCount; n++) {
			setFrameX(n, x);
		}
	}

	/**
	 * Sets y position of upper left corner of the specified video frame in world
	 * units.
	 *
	 * @param n the video frame number
	 * @param y the world y position
	 */
	@Override
	public final void setFrameY(int n, double y) {
		setFrameXY(n, coords.imageToWorldX(n, 0, 0), y);
	}

	/**
	 * Sets y position of upper left corner of all video frames in world units.
	 *
	 * @param y the world y position
	 */
	@Override
	public final void setY(double y) {
		for (int n = 0; n < frameCount; n++) {
			setFrameY(n, y);
		}
	}

	/**
	 * Gets x position of upper left corner of the current video frame in world
	 * units.
	 *
	 * @return the world x position
	 */
	@Override
	public final double getX() {
		return coords.imageToWorldX(frameNumber, 0, 0);
	}

	/**
	 * Gets y position of upper left corner of the current video frame in world
	 * units.
	 *
	 * @return the world y position
	 */
	@Override
	public final double getY() {
		return coords.imageToWorldY(frameNumber, 0, 0);
	}

	/**
	 * Sets the x and y position of the UL corner of the specified video frame in
	 * world units.
	 *
	 * @param n the video frame number
	 * @param x the world x position
	 * @param y the world y position
	 */
	@Override
	public final void setFrameXY(int n, double x, double y) {
		double sin = coords.getSine(n);
		double cos = coords.getCosine(n);
		double tx = coords.getScaleX(n) * (y * sin - x * cos);
		double ty = coords.getScaleY(n) * (y * cos + x * sin);
		coords.setOriginXY(n, tx, ty);
	}

	/**
	 * Sets the x and y position of the UL corner of all video frames in world
	 * units.
	 *
	 * @param x the world x position
	 * @param y the world y position
	 */
	@Override
	public final void setXY(double x, double y) {
		for (int n = 0; n < frameCount; n++) {
			setFrameXY(n, x, y);
		}
	}


	/**
	 * Sets the relative aspect of the specified video frame. Relative aspect is the
	 * ratio of the world aspect to the pixel aspect of the image. The pixel aspect
	 * is the ratio of image width to height in pixels, and world aspect is the
	 * ratio of world width to height in world units. For example, a 320 x 240 pixel
	 * movie has a pixel aspect of 1.33. If relative aspect is 2, then the world
	 * aspect will be 2.67. So if the video's width is 16 wu, its height will be 6
	 * wu. Or if its height is 10 wu, its width will be 26.67 wu.
	 *
	 * @param n              the video frame number
	 * @param relativeAspect the desired relative aspect
	 */
	@Override
	public final void setFrameRelativeAspect(int n, double relativeAspect) {
		if ((relativeAspect < 0.001) || (relativeAspect > 1000)) {
			return;
		}
		aspects.set(n, Math.abs(relativeAspect));
		if (isMeasured) {
			if (widthDominates) {
				setFrameWidth(n, size.width / coords.getScaleX(n));
			} else {
				setFrameHeight(n, size.height / coords.getScaleY(n));
			}
		}
	}

	/**
	 * Sets the relative aspect of all video frames. Relative aspect is the ratio of
	 * the world aspect to the pixel aspect of the image. The pixel aspect is the
	 * ratio of image width to height in pixels, and world aspect is the ratio of
	 * world width to height in world units. For example, a 320 x 240 pixel movie
	 * has a pixel aspect of 1.33. If relative aspect is 2, then the world aspect
	 * will be 2.67. So if the video's width is 16 wu, its height will be 6 wu. Or
	 * if its height is 10 wu, its width will be 26.67 wu.
	 *
	 * @param relativeAspect the desired relative aspect
	 */
	@Override
	public final void setRelativeAspect(double relativeAspect) {
		for (int n = 0; n < frameCount; n++) {
			setFrameRelativeAspect(n, relativeAspect);
		}
	}

	/**
	 * Gets the relative aspect of the current video frame.
	 *
	 * @return the relative aspect of the current image.
	 */
	@Override
	public final double getRelativeAspect() {
		return aspects.get(frameNumber);
	}

	/**
	 * Sets the width of the specified video frame in world units. Also sets the
	 * height using the relative aspect.
	 *
	 * @param n     the video frame number
	 * @param width the width in world units
	 * @see #setRelativeAspect
	 */
	@Override
	public final void setFrameWidth(int n, double width) {
		if (width == 0) {
			return;
		}
		width = Math.abs(width);
		// save x and y since setting width invalidates them
		double x = coords.imageToWorldX(n, 0, 0);
		double y = coords.imageToWorldY(n, 0, 0);
		double scaleX = size.width / width;
		coords.setScaleX(n, scaleX);
		coords.setScaleY(n, scaleX * aspects.get(n));
		widthDominates = true;
		// restore x and y to their correct values
		setFrameXY(n, x, y);
	}

	/**
	 * Sets the width of all video frames in world units. Also sets the heights
	 * using the relative aspect.
	 *
	 * @param width the width in world units
	 * @see #setRelativeAspect
	 */
	@Override
	public final void setWidth(double width) {
		for (int n = 0; n < frameCount; n++) {
			setFrameWidth(n, width);
		}
	}

	/**
	 * Gets the current width of the video frame.
	 *
	 * @return the width of the video image
	 */
	@Override
	public final double getWidth() {
		return size.width / coords.getScaleX(frameNumber);
	}

	/**
	 * Sets the height of the specified video frame in world units. Also sets the
	 * width using the relative aspect.
	 *
	 * @param n      the video frame number
	 * @param height the height in world units
	 * @see #setRelativeAspect
	 */
	@Override
	public final void setFrameHeight(int n, double height) {
		if (height == 0) {
			return;
		}
		height = Math.abs(height);
		// save x and y since setting width invalidates them
		double x = coords.imageToWorldX(n, 0, 0);
		double y = coords.imageToWorldY(n, 0, 0);
		double scaleY = size.height / height;
		coords.setScaleY(n, scaleY);
		coords.setScaleX(n, scaleY / aspects.get(n));
		widthDominates = false;
		// restore x and y to their correct values
		setFrameXY(n, x, y);
	}

	/**
	 * Sets the height of all video frames in world units. Also sets the widths
	 * using the relative aspect.
	 *
	 * @param height the height in world units
	 * @see #setRelativeAspect
	 */
	@Override
	public final void setHeight(double height) {
		for (int n = 0; n < frameCount; n++) {
			setFrameHeight(n, height);
		}
	}

	/**
	 * Gets the current height of the video frame.
	 *
	 * @return the height of the video image
	 */
	@Override
	public final double getHeight() {
		return size.height / coords.getScaleY(frameNumber);
	}

	@Override
	public final Dimension getImageSize(boolean withFilters) {
		if (withFilters) {
			BufferedImage img = getImage();
			if (img != null) {
				displayedSize.setSize(img.getWidth(), img.getHeight());
				return displayedSize;
			}
		}
		return size;
	}

	/**
	 * Sets the angle in radians of the specified video frame measured ccw from the
	 * world x-axis. This results in a rotation only.
	 *
	 * @param n     the video frame number
	 * @param theta the angle in radians
	 */
	@Override
	public final void setFrameAngle(int n, double theta) {
		// save x and y since setting angle invalidates them
		double x = coords.imageToWorldX(n, 0, 0);
		double y = coords.imageToWorldY(n, 0, 0);
		double cos = Math.cos(theta);
		double sin = Math.sin(theta);
		coords.setCosineSine(n, cos, -sin);
		setFrameXY(n, x, y); // restore x and y to their correct values
	}

	/**
	 * Sets the angle in radians of all video frames measured ccw from the world
	 * x-axis. This results in a rotation only.
	 *
	 * @param theta the angle in radians
	 */
	@Override
	public final void setAngle(double theta) {
		for (int n = 0; n < frameCount; n++) {
			setFrameAngle(n, theta);
		}
	}

	/**
	 * Gets the angle in radians of the curent video frame measured ccw from the
	 * world x-axis.
	 *
	 * @return the angle in radians
	 */
	@Override
	public final double getAngle() {
		return -coords.getAngle(frameNumber);
	}

	/**
	 * Steps the video forward one frame.
	 */
	@Override
	public final void step() {
		stop();
		setFrameNumber(frameNumber + 1);
	}

	/**
	 * Steps the video back one frame.
	 */
	@Override
	public final void back() {
		stop();
		setFrameNumber(frameNumber - 1);
	}

	/**
	 * Gets the total number of video frames.
	 *
	 * @return the number of video frames
	 */
	@Override
	public final int getFrameCount() {
		return frameCount;
	}

	/**
	 * Gets the current video frame number.
	 *
	 * @return the current frame number
	 */
	@Override
	public final int getFrameNumber() {
		return frameNumber;
	}


	@Override
	protected final void finalize() {
		OSPLog.finalized(this);
	}

	/**
	 * Initialize this video.
	 */
	protected final void initialize() {
		filterStack.addPropertyChangeListener(Filter.PROPERTY_FILTER_IMAGE, this);
		filterStack.addPropertyChangeListener(Filter.PROPERTY_FILTER_TAB, this);
	}

	/**
	 * Refreshes the BufferedImage based on current size. Creates a new image if
	 * needed.
	 */
	protected final void refreshBufferedImage() {
		if (bufferedImage != null && bufferedImage.getWidth() == size.width && bufferedImage.getHeight() == size.height)
			return;
//		OSPLog.debug("Video.refreshBufferedImage " + size);
		bufferedImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
		isValidImage = false;

		// BH removed - a new buffer will be set up correctly, in this case as
		// 0x00000000, since it has no alpha
// new Color(0, 0, 0, 0).getRGB() is just 0. 
//			// clearRaster = (DataBufferInt) bufferedgetRaster(image).getDataBuffer();
//			int clear = new Color(0, 0, 0, 0).getRGB();
//			int[] rgb = new int[size.width * size.height];
//			for (int i = 0; i < rgb.length; i++) {
//				rgb[i] = clear;
//			}
//			bufferedImage.setRGB(0, 0, size.width, size.height, rgb, 0, size.width);
//			// clearRaster = bufferedImage.getData();
	}

	/**
	 * Finds the min and max values of x and y.
	 */
	protected final void findMinMaxValues() {
		VideoClip clip = (VideoClip) getProperty("videoclip"); //$NON-NLS-1$
		// check all four corner positions of every frame in the current clip
		AffineTransform at = coords.getToWorldTransform(clip == null ? 0 : clip.getStartFrameNumber());
		minX = minY = Double.MAX_VALUE;
		maxX = maxY = -Double.MAX_VALUE;
		addMinMax(at, 0, 0);
		int w = size.width;
		int h = size.height;
		for (int i = (clip == null ? frameCount : clip.getStepCount()); --i >= 0;) {
			at = coords.getToWorldTransform(clip == null ? i : clip.stepToFrame(i));
			addMinMax(at, 0, 0);
			addMinMax(at, w, 0);
			addMinMax(at, w, h);
			addMinMax(at, 0, h);
		}
		isValidMeasure = true;
	}

	private final void addMinMax(AffineTransform at, int x, int y) {
		corner.setLocation(x, y);
		at.transform(corner, corner);
		minX = Math.min(corner.x, minX);
		maxX = Math.max(corner.x, maxX);
		minY = Math.min(corner.y, minY);
		maxY = Math.max(corner.y, maxY);
	}

	protected final String getAbsolutePath(String path) {
		if (baseDir == null)
			baseDir = XML.getDirectoryPath((String) getProperty("absolutePath"));		
		String base = baseDir;
		// trim "file:" and "jar:file:" protocols from base
		int n = base.indexOf("file:");
		if (n > -1) {
			base = base.substring(n+5, base.length());
			if (OSPRuntime.isWindows() && base.startsWith("/"))
				base = base.substring(1, base.length());
		}
		if (base != "" && !path.replace('\\', '/').startsWith(base)) {
			path = base + "/" + path;
			if (!ResourceLoader.isHTTP(base))
				path = XML.getAbsolutePath(new File(path));
		}
		return path;
	}

	protected final void notifySize(Dimension newDim) {
		if (!doNotify)
			return;
		if ((newDim.height != size.height) || (newDim.width != size.width)) {
			Dimension oldSize = new Dimension(size);
			size.width = newDim.width;
			size.height = newDim.height;
			refreshBufferedImage();
			firePropertyChange(PROPERTY_VIDEO_SIZE, oldSize, size); // $NON-NLS-1$
		}
	}

	////// not final //////
	
	/**
	 * Sets the video frame number.
	 *
	 * @param n the desired frame number
	 */
	@Override
	public void setFrameNumber(int n) {
		if (n == frameNumber) {
			return;
		}
		frameNumber = Math.min(Math.max(n, startFrameNumber), endFrameNumber);
		// for PerspectiveFilter only
		if (doNotify)
			firePropertyChange(PROPERTY_VIDEO_NEXTFRAME, null, frameNumber);
	}


	  
	/**
	 * Gets the start time of the specified frame in milliseconds.
	 *
	 * @param i the frame number
	 * @return the start time of the frame in milliseconds, or -1 if not known
	 */
	@Override
	public double getFrameTime(int i) {
		if ((i >= startTimesMS.length) || (i < 0)) {
			return -1;
		}
		return startTimesMS[i];
	}

	/**
	 * Gets the duration of the specified frame in milliseconds.
	 *
	 * @param n the frame number
	 * @return the duration of the frame in milliseconds
	 */
	@Override
	public final double getFrameDuration(int n) {
		if (frameCount == 1) {
			return getFrameCountDurationMS();
		}
		if (n == frameCount - 1) {
			return getFrameCountDurationMS() - getFrameTime(n);
		}
		return getFrameTime(n + 1) - getFrameTime(n);
	}

//	/**
//	 * Gets the current video time in milliseconds.
//	 * 
//	 * never called
//	 *
//	 * @return the current time in milliseconds, or -1 if not known
//	 */
//	  @Override
//	  public final double getTime() {
//	    return getFrameTime(getFrameNumber());
//	  }
//
//
//  
//	/**
//	 * never called
//	 * 
//	 * Sets the frame number to (nearly) a desired time in milliseconds.
//	 *
//	 * @param millis the desired time in milliseconds
//	 */
//	@Override
//	public void setTime(double millis) {
//		millis = Math.abs(millis);
//		for (int i = 0; i < startTimes.length; i++) {
//			double t = startTimes[i];
//			if (millis < t) { // find first frame with later start time
//				setFrameNumber(i - 1);
//				break;
//			}
//		}
//	}

	/**
	 * Gets the start frame time in milliseconds.
	 *
	 * @return the start time in milliseconds, or -1 if not known
	 */
	@Override
	public double getStartTime() {
		return getFrameTime(getStartFrameNumber());
	}

	abstract protected void setStartTimes();

	/**
	 * never called
	 * 
	 * Sets the start time in milliseconds. NOTE: the actual start time is normally
	 * set to the beginning of a frame.
	 *
	 * @param millis the desired start time in milliseconds
	 */
	@Override
	@Deprecated
	public void setStartTime(double millis) {
		millis = Math.abs(millis);
		for (int i = 0; i < startTimesMS.length; i++) {
			double t = startTimesMS[i];
			if (millis < t) { // find first frame with later start time
				setStartFrameNumber(i - 1);
				break;
			}
		}
	}

	/**
	 * Gets the end frame time in milliseconds.
	 *
	 * @return the end time in milliseconds, or -1 if not known
	 */
	@Override
	public double getEndTime() {
		int n = getEndFrameNumber();
		if (n < getFrameCount() - 1)
			return getFrameTime(n + 1);
		return getFrameCountDurationMS();
	}

	/**
	 * Sets the end frame to (nearly) a desired time in milliseconds.
	 *
	 * @param millis the desired end time in milliseconds
	 */
	@Override
	public void setEndTime(double millis) {
		millis = Math.abs(millis);
		millis = Math.min(getFrameCountDurationMS(), millis);
		for (int i = 0; i < startTimesMS.length; i++) {
			double t = startTimesMS[i];
			if (millis < t) { // find first frame with later start time
				setEndFrameNumber(i - 1);
				break;
			}
		}
	}

	@Override
	public void setRate(double rate) {
		rate = Math.abs(rate);
		if ((rate == this.rate) || (rate == 0)) {
			return;
		}
		this.rate = rate;
	}

	/**
	 * Gets the current video image after applying enabled filters.
	 *
	 * @return the current video image with filters applied
	 */
	@Override
	public BufferedImage getImage() {
		updateBufferedImage();
		if (filterStack.isEmpty() || !filterStack.isEnabled()) {
			return bufferedImage;
		} else if (!isValidFilteredImage) { // filteredImage needs refreshing
			isValidFilteredImage = true;
			filteredImage = filterStack.getFilteredImage(bufferedImage);
		}
		return filteredImage;
	}

	protected void updateBufferedImage() {
		refreshBufferedImage();
		if (!isValidImage) { // bufferedImage needs refreshing
			isValidImage = true;
			Graphics g = bufferedImage.createGraphics();
			// bufferedImage.setData(clearRaster);
			g.drawImage(rawImage, 0, 0, null);
			g.dispose();
		}
	}

	protected void setFrameCount(int n) {
		frameCount = n;
	}

	/**
	 * Sets the image coordinate system used to convert from imagespace to
	 * worldspace.
	 *
	 * @param newCoords the image coordinate system
	 */
	@Override
	public void setCoords(ImageCoordSystem newCoords) {
		if (newCoords == coords) {
			return;
		}
		if (coords != null) {
			coords.removePropertyChangeListener(ImageCoordSystem.PROPERTY_COORDS_TRANSFORM, this);
		}
		coords = newCoords;
		coords.addPropertyChangeListener(ImageCoordSystem.PROPERTY_COORDS_TRANSFORM, this);
		isMeasured = true;
		isValidMeasure = false;
		firePropertyChange(PROPERTY_VIDEO_COORDS, null, newCoords);
	}

	/**
	 * Gets the image coordinate system.
	 *
	 * @return the image coordinate system
	 */
	@Override
	public ImageCoordSystem getCoords() {
		return coords;
	}

	/**
	 * Sets the filter stack.
	 *
	 * @param stack the new filter stack
	 */
	@Override
	public void setFilterStack(FilterStack stack) {
		if (stack == filterStack)
			return;
		if (filterStack != null) {
			filterStack.removePropertyChangeListener(Filter.PROPERTY_FILTER_IMAGE, this);
			filterStack.removePropertyChangeListener(Filter.PROPERTY_FILTER_TAB, this);
		}
		filterStack = stack;
		filterStack.addPropertyChangeListener(Filter.PROPERTY_FILTER_IMAGE, this);
		filterStack.addPropertyChangeListener(Filter.PROPERTY_FILTER_TAB, this);
	}

	/**
	 * Gets the filter stack.
	 *
	 * @return the filter stack
	 */
	@Override
	public FilterStack getFilterStack() {
		return filterStack;
	}

	/**
	 * Sets a user property of the video.
	 *
	 * @param name  the name of the property
	 * @param value the value of the property
	 */
	@Override
	public void setProperty(String name, Object value) {
		if (name.equals("measure")) { //$NON-NLS-1$
			isValidMeasure = false;
		} else {
			properties.put(name, value);
		}
	}

	/**
	 * Gets a user property of the video. May return null.
	 *
	 * @param name the name of the property
	 * @return the value of the property
	 */
	@Override
	public Object getProperty(String name) {
		return properties.get(name);
	}

	/**
	 * Gets a collection of user property names for the video.
	 *
	 * @return a collection of property names
	 */
	@Override
	public Collection<String> getPropertyNames() {
		return properties.keySet();
	}

	/**
	 * Disposes of this video.
	 */
	@Override
	public void dispose() {
		if (coords != null)
			coords.removePropertyChangeListener(ImageCoordSystem.PROPERTY_COORDS_TRANSFORM, this);
		coords = null;
		if (filterStack != null) {
			// WAS MEMORY LEAK!
			filterStack.removePropertyChangeListener(Filter.PROPERTY_FILTER_IMAGE, this);
			filterStack.removePropertyChangeListener(Filter.PROPERTY_FILTER_TAB, this);

			filterStack.setInspectorsVisible(false);
			filterStack = null;
		}
		bufferedImage = filteredImage = null;

		super.dispose();
	}

	/**
	 * Responds to property change events.
	 *
	 * @param e the property change event
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		switch (e.getPropertyName()) {
		case ImageCoordSystem.PROPERTY_COORDS_TRANSFORM:
			isMeasured = true;
			isValidMeasure = false;
			break;
		case Filter.PROPERTY_FILTER_IMAGE:
		case Filter.PROPERTY_FILTER_TAB:
			isValidFilteredImage = false;
			firePropertyChange(e); // to StepperClipControl
			break;
		}
	}

	/**
	 * A class to save and load and save Video data.
	 */
	abstract public static class Loader implements XML.ObjectLoader {

		protected Loader() {
			// only created by a Video
		}

		/**
		 * subclassed to GifVideo, ImageVideo, JSMovieVideo, and XuggleVideo
		 * 
		 * @param path
		 * @return
		 * @throws IOException
		 */
		protected abstract Video createVideo(String path) throws IOException;

		/**
		 * Saves video data to an XMLControl. GIFVideo and MovieVideo only
		 *
		 * @param control the control to save to
		 * @param obj     the Video object to save
		 */
		@Override
		public void saveObject(XMLControl control, Object obj) {
			Video video = (Video) obj;
			String base = (String) video.getProperty("base"); //$NON-NLS-1$
			String absPath = (String) video.getProperty("absolutePath"); //$NON-NLS-1$
			if (base != null && absPath != null)
				control.setValue("path", XML.getPathRelativeTo(absPath, base)); //$NON-NLS-1$
			else {
				String path = (String) video.getProperty("path"); //$NON-NLS-1$
				control.setValue("path", path); //$NON-NLS-1$
			}
			if (!video.getFilterStack().isEmpty()) {
				control.setValue("filters", video.getFilterStack().getFilters()); //$NON-NLS-1$
			}
		}

		/**
		 * Creates a new Video.
		 *
		 * @param control the control
		 * @return the new Video
		 */
		@Override
		public Object createObject(XMLControl control) {
			try {
				String fullpath = control.getString("absolutePath");
				if (fullpath != null)
					return createVideo(fullpath);
				String path = control.getString("path"); //$NON-NLS-1$
				if (OSPRuntime.checkTempDirCache)
					path = OSPRuntime.tempDir + path;
				return createVideo(path);
			} catch (IOException ex) {
				OSPLog.fine(ex.getMessage());
				return null;
			}
		}

		/**
		 * This does nothing, but is required by the XML.ObjectLoader interface
		 *
		 * @param control the control
		 * @param obj     the Video object
		 * @return the loaded object
		 */
		@Override
		public Object loadObject(XMLControl control, Object obj) {
			Video video = (Video) obj;
			Collection<?> filters = (Collection<?>) control.getObject("filters"); //$NON-NLS-1$
			if (filters != null) {
				video.getFilterStack().clear();
				Iterator<?> it = filters.iterator();
				while (it.hasNext()) {
					Filter filter = (Filter) it.next();
					video.getFilterStack().addFilter(filter);
				}
			}
			return obj;
		}

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
