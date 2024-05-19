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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.SwingUtilities;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.display.Data;
import org.opensourcephysics.display.Drawable;
import org.opensourcephysics.display.InteractivePanel;
import org.opensourcephysics.display.MessageDrawable;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.media.core.VideoIO.FinalizableLoader;

/**
 * This is an interactive drawing panel with a video player. It can draw videos
 * and other Trackable objects in either imagespace or worldspace. When drawing
 * in imagespace, the image reference frame (ie the image itself) is fixed. When
 * drawing in worldspace, the world reference frame is fixed. The image
 * reference frame defines positions in pixel units relative to the upper left
 * corner of a video image--ie, the UL corner of a 320 x 240 video is at (0.0,
 * 0.0) and the LR corner is at (320.0, 240.0). When drawing in imagespace,
 * non-Trackable objects are not drawn.
 *
 * @author Douglas Brown
 * @version 1.0
 */
@SuppressWarnings("serial")
public class VideoPanel extends InteractivePanel implements PropertyChangeListener {
	// static fields
	protected final static int defaultWidth = 640;
	protected final static int defaultHeight = 480;

	public static final String PROPERTY_VIDEOPANEL_DATAFILE = "datafile";
	public static final String PROPERTY_VIDEOPANEL_IMAGESPACE = "imagespace";	

	// instance fields
	/** set to true when this panel has been changed, false when saved */
	public boolean changed = false;

	/** default file name used for initial saveAs */
	public String defaultFileName;
	/** progress and framesLoaded used to keep track of loading process */
	protected int progress;
	public int framesLoaded;
	protected VideoPlayer player;
	protected Video video = null;
	protected boolean playerVisible = true;
	protected boolean drawingInImageSpace = false;
	protected double imageWidth, imageHeight; // image dimensions in image units
	protected double xOffset, yOffset; // imagespace drawing offset in pixel units
	protected double imageBorder; // a fraction >= 0
	protected ImageCoordSystem coords; // image <--> world transforms
	protected Point2D pt = new Point2D.Double();
	protected File dataFile;
	protected String timeUnit = "s"; //$NON-NLS-1$
	/**
	 * map filter names to classes
	 */
	protected Map<String, Class<? extends Filter>> filterClasses = new TreeMap<String, Class<? extends Filter>>(); 

	
	
	protected FinalizableLoader loader; // for asynchronous loading
	private Video videoLoading;

	/**
	 * Constructs a blank VideoPanel with a player.
	 */
	public VideoPanel() {
		this(null);
	}

	/**
	 * Constructs a VideoPanel with a video and player.
	 *
	 * @param video the video to be drawn
	 */
	public VideoPanel(Video video) {
		super();
		setName("VideoPanel");
		squareAspect = true;
		//setSquareAspect(true);
		addVideoPlayer();
		// define mousePanel and messagePanel
		// make new CoordinateStringBuilder
		setCoordinateStringBuilder(new VidCartesianCoordinateStringBuilder());
		// create coords and put origin at center of panel
		coords = new ImageCoordSystem();
		setVideo(video);
		setImageSize(video, true);
		// put origin at center of image
	}

	protected void addVideoPlayer() {
		player = new VideoPlayer(this);
		player.addFrameListener(this);
		add(player, BorderLayout.SOUTH);
		VideoClip clip = player.getVideoClip();
		clip.addListener(this);

		// TODO Auto-generated method stub
		
	}

	private void setImageSize(Video video, boolean isConstructor) {
		Dimension d;
		if (video != null && (d = video.getImageSize(false)).width > 0) {
			setImageWidth(d.width);
			setImageHeight(d.height);
		} else {
			setImageWidth(defaultWidth);
			setImageHeight(defaultHeight);
		}
		if (!isConstructor)
			coords.setAllOriginsXY(0, 0);
		if (isConstructor) {
			int w = (int) getImageWidth();
			int h = (int) getImageHeight();
			setPreferredSize(new Dimension(w, h + player.height));
			coords.setAllOriginsXY(imageWidth / 2, imageHeight / 2);
		} else {
			coords.setAllOriginsXY(imageWidth / 2, imageHeight / 2);
			if (video != null)
				video.setProperty("measure", "invalidate");
		}
	}

	/**
	 * Sets the video.
	 *
	 * @param newVideo     the video
	 * @param playAllSteps true to play all steps
	 */
	public void setVideo(Video newVideo, boolean playAllSteps) {
		if (newVideo == video) {
			return;
		}
		if (videoLoading != null && videoLoading == newVideo)
			videoLoading = null;
		initializePlayer(video, newVideo, playAllSteps);
	}

	/**
	 * Sets the video.
	 *
	 * @param newVideo the video
	 */
	public void setVideo(Video newVideo) {
		setVideo(newVideo, false);
	}

	/**
	 * Gets the video.
	 *
	 * @return the video
	 */
	public Video getVideo() {
		return video;
	}

	private void initializePlayer(Video prev, Video newVideo, boolean playAllSteps) {
		// testing against 1:01 v
		if (prev != null) {
			prev.removePropertyChangeListener(AsyncVideoI.PROPERTY_ASYNCVIDEOI_IMAGEREADY, this);
			prev.removePropertyChangeListener(AsyncVideoI.PROPERTY_ASYNCVIDEOI_READY, this);
		}
		if (newVideo != null) {
			newVideo.removePropertyChangeListener(AsyncVideoI.PROPERTY_ASYNCVIDEOI_IMAGEREADY, this);
			newVideo.addPropertyChangeListener(AsyncVideoI.PROPERTY_ASYNCVIDEOI_IMAGEREADY, this);
			newVideo.removePropertyChangeListener(AsyncVideoI.PROPERTY_ASYNCVIDEOI_READY, this);
			newVideo.addPropertyChangeListener(AsyncVideoI.PROPERTY_ASYNCVIDEOI_READY, this);
		}
		VideoClip prevClip = getPlayer().getVideoClip();
		VideoClip newClip = new VideoClip((Video) newVideo);
		if (newVideo == null && prevClip != null) {
			XMLControl control = new XMLControlElement(prevClip);
			control.setValue("video", null); //$NON-NLS-1$
			control.loadObject(newClip);
		}
		newClip.setPlayAllSteps(playAllSteps);
		getPlayer().setVideoClip(newClip);
		if (prev != null) {
			prev.dispose();
		}
	}

	/**
	 * Gets the image width in image units.
	 *
	 * @return the width
	 */
	public double getImageWidth() {
		return imageWidth;
	}

	/**
	 * Sets the image width in image units.
	 *
	 * @param w the width
	 */
	public void setImageWidth(double w) {
		// don't allow widths smaller than the video image
		// NOTE video image may not be same size as raw video when filters applied
		if (video != null) {
      BufferedImage vidImage = video.getImage();
      if(vidImage!=null) {
        w = Math.max(w, vidImage.getWidth());
      }
		}
		imageWidth = w;
	}

	/**
	 * Gets the image height in image units (1.0 unit/pixel).
	 *
	 * @return the height
	 */
	public double getImageHeight() {
		return imageHeight;
	}

	/**
	 * Sets the image height in image units (1.0 unit/pixel).
	 *
	 * @param h the height
	 */
	public void setImageHeight(double h) {
		// don't allow heights smaller than the video image
		// NOTE video image may not be same size as raw video when filters applied
		if (video != null) {
      BufferedImage vidImage = video.getImage();
      if(vidImage!=null) {
        h = Math.max(h, vidImage.getHeight());
      }
		}
		imageHeight = h;
	}

	/**
	 * Gets the image border.
	 *
	 * @return the border fraction
	 */
	public double getImageBorder() {
		return imageBorder;
	}

	/**
	 * Sets the image border.
	 *
	 * @param borderFraction the border fraction
	 */
	public void setImageBorder(double borderFraction) {
//    imageBorder = Math.max(borderFraction, 0);
		imageBorder = borderFraction;
	}

	/**
	 * Gets the time unit
	 *
	 * @return unit the desired unit
	 */
	public String getTimeUnit() {
		return timeUnit;
	}

	/**
	 * Sets the time unit.
	 *
	 * @param unit the time unit
	 * @return true if unit was changed
	 */
	public boolean setTimeUnit(String unit) {
		if ("".equals(unit)) //$NON-NLS-1$
			unit = null;
		if (unit == null)
			return false;
		unit = unit.trim();
		if (timeUnit.equals(unit))
			return false;
		// prevent numbers being set as units
		try {
			Double.parseDouble(unit);
			return false;
		} catch (Exception e) {
		}
		timeUnit = unit;

		return true;
	}

	/**
	 * Sets the image coordinate system used to convert between image and world
	 * spaces.
	 *
	 * @param newCoords the image coordinate system
	 */
	public void setCoords(ImageCoordSystem newCoords) {
		if (video != null) {
			video.setCoords(newCoords);
		} else {
			coords = newCoords;
		}
	}

	/**
	 * Gets the current image coordinate system used for drawing.
	 *
	 * @return the current image coordinate system
	 */
	public ImageCoordSystem getCoords() {
//		if (video != null) {
//			return video.getCoords();
//		} else {
			return coords;
//		}
	}

	/**
	 * Sets the file in which data is saved.
	 *
	 * @param file the data file
	 */
	public void setDataFile(File file) {
		File prev = dataFile;
		dataFile = file;
		if (file != null) {
			defaultFileName = XML.forwardSlash(file.getName());
		}
		firePropertyChange(PROPERTY_VIDEOPANEL_DATAFILE, prev, dataFile); // $NON-NLS-1$
		OSPLog.fine("Data file: " + file); //$NON-NLS-1$
	}

	/**
	 * Gets the file where data is saved.
	 *
	 * @return the data file
	 */
	public File getDataFile() {
		return dataFile;
	}

	/**
	 * Gets the default path for the saveAs method.
	 *
	 * @return the relative path to the file
	 */
	public String getFilePath() {
		return defaultFileName;
	}

	/**
	 * Sets the drawing space to imagespace or worldspace.
	 *
	 * @param imagespace <code>true</code> to draw in imagespace
	 */
	public void setDrawingInImageSpace(boolean imagespace) {
		drawingInImageSpace = imagespace;
		if (imagespace) {
			setAutoscaleX(false);
			setAutoscaleY(false);
		} else {
			setAutoscaleX(true);
			setAutoscaleY(true);
		}
		firePropertyChange(PROPERTY_VIDEOPANEL_IMAGESPACE, null, Boolean.valueOf(imagespace)); 
		repaint();
	}

	/**
	 * Returns true if this is drawing in image space rather than world space.
	 *
	 * @return <code>true</code> if drawing in image space
	 */
	public boolean isDrawingInImageSpace() {
		return drawingInImageSpace;
	}

	/**
	 * Gets the video player.
	 *
	 * @return the video player
	 */
	public VideoPlayer getPlayer() {
		return player;
	}

	/**
	 * Shows or hides the video player.
	 *
	 * @param visible <code>true</code> to show the player
	 */
	public void setPlayerVisible(final boolean visible) {
		if (visible == playerVisible) {
			return;
		}
		Runnable setPlayerVis = new Runnable() {
			@Override
			public void run() {
				playerVisible = visible;
				if (playerVisible) {
					add(player, BorderLayout.SOUTH);
				} else {
					remove(player);
				}
				repaint();
			}

		};
		if (OSPRuntime.isJS)
			setPlayerVis.run();
		else
			SwingUtilities.invokeLater(setPlayerVis);
	}

	/**
	 * Gets the video player visibility.
	 *
	 * @return <code>true</code> if the player is visible
	 */
	public boolean isPlayerVisible() {
		return playerVisible;
	}

	/**
	 * Gets the current step number.
	 *
	 * @return the current step number
	 */
	public int getStepNumber() {
		return getPlayer().getStepNumber();
	}

	/**
	 * Gets the current frame number.
	 *
	 * @return the frame number
	 */
	public int getFrameNumber() {
		return getPlayer().getFrameNumber();
	}

//	/**
//	 * Overrides DrawingPanel getDrawables method.
//	 *
//	 * @return a list of Drawable objects
//	 */
//	public ArrayList<Drawable> getDrawables() {
//		return (isDrawingInImageSpace() ? (ArrayList<Drawable>) 
//				(Object) getDrawables(Trackable.class) : super.getDrawables());
//	}

	@Override
	public ArrayList<Drawable> getDrawables() {
		ArrayList<Drawable> list = super.getDrawables();
		if (isDrawingInImageSpace()) {
			for (Drawable d : list) {
				if (!Trackable.class.isInstance(d)) {
					list.remove(d);
				}
			}
		}
		return list;
	}

	/**
	 * Must clone if removing Trackable
	 */
	@Override
	public synchronized ArrayList<Drawable> getDrawablesNoClone() {
		return getDrawables();
	}

	/**
	 * Adds a drawable object to the drawable list.
	 *
	 * @param drawable the drawable object
	 */
	@Override
	public synchronized void addDrawable(Drawable drawable) {
		if (drawable == null) {
			return;
		}
		if (drawable instanceof Video) {
			setVideo((Video) drawable);
		} else {
			super.addDrawable(drawable);
		}
		repaint();
	}

	/**
	 * Removes a drawable object from the drawable list.
	 *
	 * @param drawable the drawable object
	 */
	@Override
	public synchronized void removeDrawable(Drawable drawable) {
		if (drawable == video) {
			setVideo(null);
		} else {
			super.removeDrawable(drawable);
		}
	}

	/**
	 * Removes all objects of the specified class.
	 *
	 * @param c the class to remove
	 */
	@Override
	public synchronized <T extends Drawable> void removeObjectsOfClass(Class<T> c) {
		if (video.getClass() == c) {
			setVideo(null);
		} else {
			super.removeObjectsOfClass(c);
		}
	}

	/**
	 * Removes all drawable objects except the video. To remove the video, use
	 * setVideo(null);
	 */
	@Override
	public synchronized void clear() {
		super.clear();
		// add back the video
		if (video != null) {
			super.addDrawable(video);
		}
	}

	/**
	 * Adds a video filter class to the map of available filters.
	 *
	 * @param filterClass the filter class to add
	 */
	public void addFilter(Class<? extends Filter> filterClass) {
		if (Filter.class.isAssignableFrom(filterClass)) {
			filterClasses.put(filterClass.getName(), filterClass);
		}
	}

	/**
	 * Removes a video filter class from the map of available filters.
	 *
	 * @param filterClass the filter class to remove
	 */
	public void removeFilter(Class<?> filterClass) {
		if (Filter.class.isAssignableFrom(filterClass)) {
			filterClasses.remove(filterClass.getName());
		}
	}

	/**
	 * Clears all video filter classes from the map of available filters.
	 */
	public void clearFilters() {
		filterClasses.clear();
	}

	/**
	 * Gets the map of available video filters.
	 *
	 * @return the map of available video filters
	 */
	public Map<String, Class<? extends Filter>> getFilters() {
		return filterClasses;
	}

	/**
	 * Hides the mouse box
	 */
	public void hideMouseBox() {
			setMessage(null, MessageDrawable.BOTTOM_LEFT);  // BL message box
	}

	/**
	 * Responds to property change events. VideoPanel listens for the following
	 * events: "videoclip" and "stepnumber" from VideoPlayer, "coords" and "image"
	 * from Video.
	 *
	 * @param e the property change event
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		String name = e.getPropertyName();
		//OSPLog.debug("VideoPanel.prop " + name);
		switch (name) {
		case Video.PROPERTY_VIDEO_SIZE:
			Dimension dim = (Dimension) e.getNewValue();
			setImageWidth(dim.width);
			setImageHeight(dim.height);
			break;
		case Video.PROPERTY_VIDEO_COORDS:
			// replace current coords with video's new coords
			coords = video.getCoords();
			break;
		case AsyncVideoI.PROPERTY_ASYNCVIDEOI_IMAGEREADY:
			// ignored - no longer necessary -- see PROPERTY_VIDEO_FRAMENUMBER
			break;
		case Video.PROPERTY_VIDEO_IMAGE:
		case Video.PROPERTY_VIDEO_VIDEOVISIBLE:
			repaint();
			break;
		case AsyncVideoI.PROPERTY_ASYNCVIDEOI_READY:
			Video newVideo = (Video) e.getNewValue();
			newVideo.removePropertyChangeListener(AsyncVideoI.PROPERTY_ASYNCVIDEOI_READY, this);
			if (loader == null) {
				// video is newVideo here
				setImageSize(newVideo, false);
				coords = newVideo.getCoords();
			} else {
				loader.finalizeLoading();
			}
			repaint();
			break;
		case VideoPlayer.PROPERTY_VIDEOPLAYER_STEPNUMBER: // from VideoPlayer
			repaint();
			break;
		case VideoPlayer.PROPERTY_VIDEOPLAYER_VIDEOCLIP: // from VideoPlayer
			// set video field
			// update property change listeners
			((VideoClip) e.getOldValue()).removeListener(this);
			VideoClip clip = (VideoClip) e.getNewValue();
			clip.addListener(this);
			// replace current video with new clip's video
			if (video != null) {
				video.removeListener(this);
				super.removeDrawable(video);
			}
			video = clip.getVideo();
			if (video != null) {
				// general place where video is defined
				videoLoading = null;
				video.addListener(this);
				// synchronize coords
				if (video.isMeasured()) {
					coords = video.getCoords();
				} else {
					video.setCoords(coords);
				}
				synchronized (drawableList) {
					drawableList.add(0, video); // put video at back
				}
				Dimension d = video.getImageSize(true);
				if (d.width > 0) {
					setImageWidth(d.width);
					setImageHeight(d.height);
				}
				break;
			}
			repaint();
		}
	}

	/**
	 * Imports Data from a source into a DataTrack. This method returns null, but
	 * superclass TrackerPanel overrides it and returns a DataTrack track. Source
	 * object may be String path, JPanel controlPanel, Tool tool, etc
	 * 
	 * @param data   the Data to import
	 * @param source the data source (may be null)
	 * @return the DataTrack with the Data (may return null)
	 */
	public DataTrack importData(Data data, Object source) {
		return null;
	}

	/**
	 * Overrides DrawingPanel paintEverything method.
	 *
	 * @param g the graphics context to draw on
	 */
	@Override
	protected void paintEverything(Graphics g) {
		// increase bottom gutter to make room for the player
		if (playerVisible) {
			bottomGutter += player.height;
		}
		super.paintEverything(g);
		// restore bottom gutter
		if (playerVisible) {
			bottomGutter -= player.height;
		}
	}

	/**
	 * Overrides DrawingPanel scale method to handle drawing in imagespace
	 *
	 * @param drawables the list of drawable objects
	 */
	@Override
	protected void scale(ArrayList<Drawable> drawables) {
		if (drawingInImageSpace) {
			// scale to image units
			xminPreferred = -imageBorder * imageWidth + xOffset;
			xmaxPreferred = imageWidth + imageBorder * imageWidth + xOffset;
			yminPreferred = imageHeight + imageBorder * imageHeight + yOffset;
			ymaxPreferred = -imageBorder * imageHeight + yOffset;
		}
		super.scale(drawables);
	}

	/**
	 * Overrides DrawingPanel checkImage method so offscreenImage will not include
	 * the videoPlayer.
	 *
	 * @return <code>true</code> if the image is correctly sized
	 */
	protected boolean checkImage() {
		Dimension d = getSize();
		if (playerVisible) {
			d.height -= player.height; // don't include player area
		}
		if ((d.width <= 2) || (d.height <= 2)) {
			return false; // image is too small
		}
		if ((offscreenImage == null) || (d.width != offscreenImage.getWidth())
				|| (d.height != offscreenImage.getHeight())) {
			offscreenImage = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
		}
		if (offscreenImage == null) {
			return false;
		}
		return true; // the buffered image exists and is the correct size
	}

	/**
	 * Gets the world coordinates of the last mouse event
	 *
	 * @return world coordinates of last mouse event
	 */
	public Point2D getWorldMousePoint() {
		// get coordinates of mouse
		pt.setLocation(getMouseX(), getMouseY());
		// transform if nec
		if (isDrawingInImageSpace()) {
			int n = getFrameNumber();
			AffineTransform toWorld = getCoords().getToWorldTransform(n);
			toWorld.transform(pt, pt);
		}
		return pt;
	}

	/**
	 * Gets a XYCoordinateStringBuilder for a TPoint to display its coordinates.
	 * This default implementation returns the static TPoint string builder.
	 *
	 * @param point the TPoint
	 * @return the XYCoordinateStringBuilder
	 */
	public XYCoordinateStringBuilder getXYCoordinateStringBuilder(TPoint point) {
		return TPoint.xyStringBuilder;
	}

	// ______________________________ object loader ________________________________

	/**
	 * Returns an XML.ObjectLoader to save and load data for this object.
	 *
	 * @return the object loader
	 */
	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

	/**
	 * A class to save and load data for this object.
	 */
	public static class Loader implements XML.ObjectLoader, FinalizableLoader {
		public XMLControlElement control;
		public VideoClip clip;
		protected VideoPanel videoPanel;
		protected boolean finalized;

		/**
		 * Return a new VideoPanel.
		 *
		 * @param control the control
		 * @return the newly created object
		 */
		@Override
		public Object createObject(XMLControl control) {
			return new VideoPanel();
		}

		/**
		 * Loads an object with data from an XMLControl.
		 *
		 * @param control the control
		 * @param obj     the VideoPanel from createObject 
		 * 
		 * @return the final VideoPanel (not necessarily obj)
		 */
		@Override
		public Object loadObject(XMLControl control, Object obj) {	
			this.control = (XMLControlElement) control;
			videoPanel = (VideoPanel) obj;
			// load the video clip
			if (videoPanel.progress >= VideoIO.PROGRESS_VIDEO_READY || getClip(control)) {
				finalizeLoading();
			}
			return videoPanel;

		}

		/**
		 * Common to both VideoPanel and TrackerPanel, this method obtains the VideoClip
		 * and possibly its basePath, then either finalizes the panel or schedules that
		 * finalization for when an asynchronous video (SwingJS) is ready.
		 * 
		 * Note that this method will be called multiple times if the the video is
		 * IncrementallyLoadable (Xuggle, Java).
		 * 
		 * @param control
		 * @return false if deferring finalization (TrackerPanel AsyncVideo)
		 */
		public boolean getClip(XMLControl control) {
			if (!control.getPropertyNamesRaw().contains("videoclip")) {
				videoPanel.progress = VideoIO.PROGRESS_VIDEO_READY;
				return true;
			}
			if (clip == null)
				clip = (VideoClip) control.getObject("videoclip"); //$NON-NLS-1$
			// check for partially loaded videos
			if (clip != null) {
				Video video = clip.getVideo();
				if (video instanceof IncrementallyLoadable) {
					// Xuggle video only
					IncrementallyLoadable iVideo = (IncrementallyLoadable) clip.getVideo();
					if (iVideo.getLoadableFrameCount() <= 1) {
						XMLControl child = control.getChildControl("videoclip"); //$NON-NLS-1$
						if (child != null && child.getPropertyNamesRaw().contains("video_framecount")) {
							int frameCount = child.getInt("video_framecount");
							iVideo.setLoadableFrameCount(frameCount);
						}
					}
					try {
						if (iVideo.loadMoreFrames(VideoIO.incrementToLoad)) {
							videoPanel.setResourceLoading(clip.getVideo());
							videoPanel.framesLoaded = iVideo.getLoadedFrameCount();
							videoPanel.progress = VideoIO.progressForFraction(iVideo.getLoadedFrameCount(),
									iVideo.getLoadableFrameCount());
							return false;
						}
						// done loading
						// clip requires one last load to finalize
						if (VideoIO.loadIncrementally) {
							control.getObject("videoclip"); //$NON-NLS-1$
						}
						videoPanel.progress = VideoIO.PROGRESS_VIDEO_READY;
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else if (video instanceof AsyncVideoI) {
					videoPanel.framesLoaded = ((AsyncVideoI) video).getLoadedFrameCount();
					videoPanel.progress = ((AsyncVideoI) video).getProgress();
				} else {
					videoPanel.progress = VideoIO.PROGRESS_VIDEO_READY;
				}
			}

			XMLControl child = control.getChildControl("videoclip"); //$NON-NLS-1$
			if (child != null) {
				if (!OSPRuntime.unzipFiles) {
					child.setBasepath(control.getBasepath());
				}
			}
			if (clip != null) {
				Video video = clip.getVideo();
				if (video instanceof AsyncVideoI) {
					videoPanel.setLoader(this);
					video.addPropertyChangeListener(AsyncVideoI.PROPERTY_ASYNCVIDEOI_READY, videoPanel);
					return false;
				}
			}
			return true;
		}
		
		public Video finalizeClip() {
			Video video = (clip == null ? null : clip.getVideo());
			if (clip != null) {
				if (video instanceof AsyncVideoI) {
					clip.loader.finalizeLoading();
				}
				videoPanel.getPlayer().setVideoClip(clip);
			}
			return video;
		}
		
		@Override
		public void finalizeLoading() {
			videoPanel.setLoader(null);
			finalizeClip();
			videoPanel.setCoords((ImageCoordSystem) control.getObject("coords")); //$NON-NLS-1$
			Collection<?> drawables = (Collection<?>) control.getObject("drawables"); //$NON-NLS-1$			
			if (drawables != null) {
				Iterator<?> it = drawables.iterator();
				while (it.hasNext()) {
					videoPanel.addDrawable((Drawable) it.next());
				}
			}
		}

		@Override
		public boolean isFinalized() {
			return finalized;
		}

		@Override
		public void finalize() {
			OSPLog.finalized(this);
		}


		/**
		 * Saves object data to an XMLControl.
		 *
		 * @param control the control to save to
		 * @param obj     the object to save
		 */
		@Override
		public void saveObject(XMLControl control, Object obj) {
			VideoPanel vidPanel = (VideoPanel) obj;
			// save the video clip and coords
			control.setValue("videoclip", vidPanel.getPlayer().getVideoClip()); //$NON-NLS-1$
			control.setValue("coords", vidPanel.getCoords()); //$NON-NLS-1$
			// save the drawables
			ArrayList<Drawable> list = vidPanel.getDrawables();
			list.remove(vidPanel.getVideo()); // the video is saved by videoclip
			if (!list.isEmpty()) {
				control.setValue("drawables", list); //$NON-NLS-1$
			}
		}

		public void dispose() {
			control.dispose();
			control = null;
			videoPanel = null;
			finalized = true;
		}


	}

	public void setLoader(FinalizableLoader loader) {
		this.loader = loader;
	}

	public void setResourceLoading(Video video) {
		this.videoLoading = video;
	}

	public void setProgress(int p) {
		progress = p;
	}
	
	public int getProgress() {
		return progress;
	}
	
	/**
	 * Video loading has failed. 
	 */
	public void releaseResources() {
		if (videoLoading != null && videoLoading != video)
			videoLoading.dispose();		
		if (video != null)
			video.dispose();
		videoLoading = video = null;
	}

	protected void offerReloadVM(String ext, String message) {
		// TrackerPanel only
	}
	
	@Override
	public void dispose() {
		if (video != null) {
			video.removePropertyChangeListener(AsyncVideoI.PROPERTY_ASYNCVIDEOI_IMAGEREADY, this);
			video.removePropertyChangeListener(AsyncVideoI.PROPERTY_ASYNCVIDEOI_READY, this);
		}
		video = null;
		if (coords != null)
			coords.dispose();
		coords = null;
		super.dispose();
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
