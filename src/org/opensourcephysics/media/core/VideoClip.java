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

import java.awt.Frame;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JOptionPane;
import javax.swing.event.SwingPropertyChangeSupport;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.media.core.VideoIO.FinalizableLoader;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * This defines a subset of video frames called steps.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class VideoClip {

	public final static String PROPERTY_VIDEOCLIP_FRAMECOUNT = "framecount";//$NON-NLS-1$
	public final static String PROPERTY_VIDEOCLIP_STARTFRAME = "startframe";//$NON-NLS-1$
	public static final String PROPERTY_VIDEOCLIP_STARTTIME = "starttime";//$NON-NLS-1$
	public static final String PROPERTY_VIDEOCLIP_STEPCOUNT = "stepcount";//$NON-NLS-1$
	public static final String PROPERTY_VIDEOCLIP_STEPSIZE = "stepsize";//$NON-NLS-1$

	public void addListener(PropertyChangeListener c) {
		addPropertyChangeListener(Trackable.PROPERTY_ADJUSTING, c); 
		addPropertyChangeListener(PROPERTY_VIDEOCLIP_FRAMECOUNT, c); 
		addPropertyChangeListener(PROPERTY_VIDEOCLIP_STARTFRAME, c); 
		addPropertyChangeListener(PROPERTY_VIDEOCLIP_STARTTIME, c); 
		addPropertyChangeListener(PROPERTY_VIDEOCLIP_STEPCOUNT, c); 
		addPropertyChangeListener(PROPERTY_VIDEOCLIP_STEPSIZE, c); 
	}

	public void removeListener(PropertyChangeListener c) {
		removePropertyChangeListener(Trackable.PROPERTY_ADJUSTING, c); 
		removePropertyChangeListener(PROPERTY_VIDEOCLIP_FRAMECOUNT, c); 
		removePropertyChangeListener(PROPERTY_VIDEOCLIP_STARTFRAME, c); 
		removePropertyChangeListener(PROPERTY_VIDEOCLIP_STARTTIME, c); 
		removePropertyChangeListener(PROPERTY_VIDEOCLIP_STEPCOUNT, c); 
		removePropertyChangeListener(PROPERTY_VIDEOCLIP_STEPSIZE, c); 
	}


	// instance fields
	public boolean changeEngine; // signals that user wishes to change preferred video engine

	private int startFrame = 0;
	private int stepSize = 1;
	private int stepCount = 10; // default stepCount is 10 if video is null
	private int nullVideoFrameCount = stepCount; // default frameCount same as stepCount
	private int maxFrameCount = 300000; // approx 2h45m at 30fps
	private double startTime = 0; // start time in milliseconds
	protected boolean isDefaultStartTime = true;
	protected Video video = null;
	private int[] stepFrames;
	ClipInspector inspector;
	private PropertyChangeSupport support;
	private boolean playAllSteps = true;
	private boolean isDefaultState;
	private boolean isAdjusting = false;
	private int endFrame;
	protected String readoutType;
	protected String videoPath;
	protected double savedStartTime; // used when DataTrack sets start time
	protected boolean startTimeIsSaved = false; // used when DataTrack sets start time
	protected int extraFrames = 0; // extends clip length past video end

	/**
	 * Constructs a VideoClip.
	 *
	 * @param video the video
	 */
	public VideoClip(Video video) {
		support = new SwingPropertyChangeSupport(this);
		this.video = video;
		if (video == null) {
			updateArray();
			isDefaultState = true;
			return;
		}
		video.setProperty("videoclip", this); //$NON-NLS-1$
		setStartFrameNumber(video.getStartFrameNumber());
		if (video.getFrameCount() > 1) {
			setStepCount(video.getEndFrameNumber() - startFrame + 1);
		}
		updateArray();
		isDefaultState = true;
	}

	public FinalizableLoader loader;

	/**
	 * Gets the video.
	 *
	 * @return the video
	 */
	public Video getVideo() {
		return video;
	}

	/**
	 * Gets the video path.
	 *
	 * @return the video path
	 */
	public String getVideoPath() {
		if (video != null)
			return (String) video.getProperty("absolutePath"); //$NON-NLS-1$
		return videoPath;
	}

	/**
	 * Sets the start frame number.
	 *
	 * @param start the desired start frame number
	 * @return true if changed
	 */
	public boolean setStartFrameNumber(int start) {
		int maxEndFrame = getLastFrameNumber();
		return setStartFrameNumber(start, maxEndFrame);
	}

	/**
	 * Sets the start frame number.
	 *
	 * @param start    the desired start frame number
	 * @param maxStart start frame number that cannot be exceeded
	 * @return true if changed
	 */
	public boolean setStartFrameNumber(int start, int maxStart) {
		int prevStart = getStartFrameNumber();
		int prevEnd = getEndFrameNumber();
		// can't start before first frame number or after max end frame
		start = Math.max(start, getFirstFrameNumber());
		start = Math.min(start, maxStart);

		if (video != null && video.getFrameCount() > 1) {
			// set video end frame to last video frame (temporary)
			video.setEndFrameNumber(video.getFrameCount() - 1);
			// set video start frame number to desired start frame
			int vidStart = Math.max(0, start);
			video.setStartFrameNumber(vidStart);

			// set start frame to frame number of first video frame
			startFrame = Math.max(0, video.getStartFrameNumber());
		} else { // no video or single-frame video
			startFrame = start;
			updateArray();
		}
		start = getStartFrameNumber();

		// reset end frame
		setEndFrameNumber(prevEnd);

		if (prevStart != start) {
			isDefaultState = false;
			support.firePropertyChange(PROPERTY_VIDEOCLIP_STARTFRAME, null, Integer.valueOf(start));
		}
		return prevStart != start;
	}

	/**
	 * Gets the start frame number.
	 *
	 * @return the start frame number
	 */
	public int getStartFrameNumber() {
		return startFrame;
	}

	/**
	 * Sets the step size.
	 *
	 * @param size the desired step size
	 * @return true if changed
	 */
	public boolean setStepSize(int size) {
		isDefaultState = false;
		if (size == 0) {
			return false;
		}
		size = Math.abs(size);
		if (video != null && video.getFrameCount() > 1) {
			int maxSize = Math.max(video.getFrameCount() - startFrame - 1 + extraFrames, 1);
			size = Math.min(size, maxSize);
		}
		if (stepSize == size) {
			return false;
		}

		// get current end frame
		int endFrame = getEndFrameNumber();
		stepSize = size;
		// set stepCount to near value
		stepCount = 1 + (endFrame - getStartFrameNumber()) / stepSize;
		updateArray();
		support.firePropertyChange(PROPERTY_VIDEOCLIP_STEPSIZE, null, Integer.valueOf(size));

		// reset end frame
		setEndFrameNumber(endFrame);

		return true;
	}

	/**
	 * Gets the step size.
	 *
	 * @return the step size
	 */
	public int getStepSize() {
		return stepSize;
	}

	/**
	 * Sets the step count.
	 *
	 * @param count the desired number of steps
	 */
	public void setStepCount(int count) {
		if (count == 0) {
			return;
		}
		count = Math.abs(count);
		if (video != null) {
			if (video.getFrameCount() > 1) {
				int end = video.getFrameCount() - 1 + extraFrames;
				int maxCount = 1 + (int) ((end - startFrame) / (1.0 * stepSize));
				count = Math.min(count, maxCount);
			}
			int end = startFrame + (count - 1) * stepSize;
			if (end != video.getEndFrameNumber()) {
				video.setEndFrameNumber(end);
			}
		} else {
			count = Math.min(count, frameToStep(maxFrameCount - 1) + 1);
		}
		count = Math.max(count, 1);
		if (stepCount == count) {
			updateArray();
			return;
		}
		Integer prev = Integer.valueOf(stepCount);
		stepCount = count;
		updateArray();
		support.firePropertyChange(PROPERTY_VIDEOCLIP_STEPCOUNT, prev, Integer.valueOf(stepCount));
	}

	/**
	 * Gets the step count.
	 *
	 * @return the number of steps
	 */
	public int getStepCount() {
		return stepCount;
	}

	/**
	 * Sets the extra frame count. Extra frames are blank frames after the last
	 * frame of a video.
	 *
	 * @param extras the number of extra frames to display
	 */
	public void setExtraFrames(int extras) {
		int prev = extraFrames;
		extraFrames = Math.max(extras, 0);
		if (prev != extraFrames) {
			OSPLog.finest("set extra frames to " + extraFrames); //$NON-NLS-1$
			setStepCount(stepCount);
		}
	}

	/**
	 * Gets the extra frame count.
	 *
	 * @return the extra frames
	 */
	public int getExtraFrames() {
		return extraFrames;
	}

	/**
	 * Gets the frame count.
	 *
	 * @return the number of frames
	 */
	public int getFrameCount() {
		if (video != null && video.getFrameCount() > 1) {
			return Math.max(1, video.getFrameCount() + extraFrames);
		}
		int frames = getEndFrameNumber();
		return nullVideoFrameCount = Math.min(Math.max(nullVideoFrameCount, frames), maxFrameCount - 1);
	}

	/**
	 * Sets the start time.
	 *
	 * @param t0 the start time in milliseconds
	 */
	public void setStartTime(double t0) {
		isDefaultState = false;
		if (startTime == t0 || (isDefaultStartTime && Double.isNaN(t0))) {
			return;
		}
		isDefaultStartTime = Double.isNaN(t0);
		startTime = Double.isNaN(t0) ? 0.0 : t0;
		support.firePropertyChange(PROPERTY_VIDEOCLIP_STARTTIME, null, new Double(startTime));
	}

	/**
	 * Gets the start time.
	 *
	 * @return the start time in milliseconds
	 */
	public double getStartTime() {
		return startTime;
	}

	/**
	 * Gets the end frame number.
	 *
	 * @return the end frame
	 */
	public int getEndFrameNumber() {
		endFrame = startFrame + stepSize * (stepCount - 1);
		return endFrame;
	}

	/**
	 * Sets the end frame number.
	 *
	 * @param end the desired end frame
	 * @return true if the end frame number was changed
	 */
	public boolean setEndFrameNumber(int end) {
		return setEndFrameNumber(end, maxFrameCount - 1, true);
	}

	/**
	 * Sets the end frame number after adding extra frames if needed.
	 *
	 * @param end the desired end frame
	 * @return true if the end frame number was extended
	 */
	public boolean extendEndFrameNumber(int end) {
		if (video != null && getFrameCount() <= end) {
			setExtraFrames(end + 1 - getFrameCount() + extraFrames);
		}
		return setEndFrameNumber(end);
	}

	/**
	 * Sets the end frame number.
	 *
	 * @param end the desired end frame
	 * @param max the maximum allowed
	 * @return true if the end frame number was changed
	 */
	private boolean setEndFrameNumber(int end, int max, boolean onlyIfChanged) {
		int prev = getEndFrameNumber();
		if (prev == end && onlyIfChanged)
			return false;
		isDefaultState = false;
		end = Math.max(end, startFrame); // end can't be less than start

		// determine step count needed for desired end frame
		int rem = (end - startFrame) % stepSize;
		int count = (end - startFrame) / stepSize;
		if (rem * 1.0 / stepSize > 0.5) {
			count++;
		}
		while (stepToFrame(count) > max) {
			count--;
		}
		// set step count
		setStepCount(count + 1);
		end = getEndFrameNumber();

		// determine maximum step size and adjust step size if needed
		if (end != startFrame) {
			int maxStepSize = Math.max(end - startFrame, 1);
			if (maxStepSize < stepSize) {
				stepSize = maxStepSize;
			}
		}

		return prev != end;
	}

	/**
	 * Converts step number to frame number.
	 *
	 * @param stepNumber the step number
	 * @return the frame number
	 */
	public int stepToFrame(int stepNumber) {
		return startFrame + stepNumber * stepSize;
	}

	/**
	 * Converts frame number to step number. A frame number that falls between two
	 * steps maps to the previous step.
	 *
	 * @param n the frame number
	 * @return the step number
	 */
	public int frameToStep(int n) {
		return (int) ((n - startFrame) / (1.0 * stepSize));
	}

	/**
	 * Determines whether the specified frame is a step frame.
	 *
	 * @param n the frame number
	 * @return <code>true</code> if the frame is a step frame
	 */
	public boolean includesFrame(int n) {
		for (int i = 0; i < stepCount; i++) {
			if (stepFrames[i] == n) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets the clip inspector.
	 *
	 * @return the clip inspector
	 */
	public ClipInspector getClipInspector() {
		return inspector;
	}

	/**
	 * Gets the clip inspector with access to the specified ClipControl.
	 *
	 * @param control the clip control
	 * @param frame   the owner of the inspector
	 * @return the clip inspector
	 */
	public ClipInspector getClipInspector(ClipControl control, Frame frame) {
		if (inspector == null) {
			inspector = new ClipInspector(this, control, frame);
		}
		return inspector;
	}

	/**
	 * Hides the clip inspector.
	 */
	public void hideClipInspector() {
		if (inspector != null) {
			inspector.setVisible(false);
		}
	}

	/**
	 * Returns true if no properties have been set or reviewed by the user.
	 * 
	 * @return true if in a default state
	 */
	public boolean isDefaultState() {
		return isDefaultState && inspector == null;
	}

	/**
	 * Sets the adjusting flag.
	 *
	 * @param adjusting true if adjusting
	 */
	public void setAdjusting(boolean adjusting) {
		if (isAdjusting == adjusting)
			return;
		isAdjusting = adjusting;
		support.firePropertyChange(Trackable.PROPERTY_ADJUSTING, this, adjusting);
	}

	/**
	 * Gets the adjusting flag.
	 *
	 * @return true if adjusting
	 */
	public boolean isAdjusting() {
		return isAdjusting;
	}

	/**
	 * Sets the playAllSteps flag.
	 *
	 * @param all true to play all steps
	 */
	public void setPlayAllSteps(boolean all) {
		playAllSteps = all;
	}

	/**
	 * Gets the playAllSteps flag.
	 *
	 * @return true if playing all steps
	 */
	public boolean isPlayAllSteps() {
		return playAllSteps;
	}

	/**
	 * Adds a PropertyChangeListener to this video clip.
	 *
	 * @param listener the object requesting property change notification
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		support.addPropertyChangeListener(listener);
	}

	/**
	 * Adds a PropertyChangeListener to this video clip.
	 *
	 * @param property the name of the property of interest to the listener
	 * @param listener the object requesting property change notification
	 */
	public void addPropertyChangeListener(String property, PropertyChangeListener listener) {
		support.addPropertyChangeListener(property, listener);
	}

	/**
	 * Removes a PropertyChangeListener from this video clip.
	 *
	 * @param listener the listener requesting removal
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		support.removePropertyChangeListener(listener);
	}

	/**
	 * Removes a PropertyChangeListener for a specified property.
	 *
	 * @param property the name of the property
	 * @param listener the listener to remove
	 */
	public void removePropertyChangeListener(String property, PropertyChangeListener listener) {
		support.removePropertyChangeListener(property, listener);
	}

	/**
	 * Trims unneeded frames after end frame (null videos only).
	 */
	protected void trimFrameCount() {
		if (video == null || video.getFrameCount() == 1) {
			nullVideoFrameCount = getEndFrameNumber() + 1;
			support.firePropertyChange(VideoClip.PROPERTY_VIDEOCLIP_FRAMECOUNT, null,
					Integer.valueOf(nullVideoFrameCount));
		}
	}

	/**
	 * Updates the list of step frames.
	 */
	private void updateArray() {
		stepFrames = new int[stepCount];
		for (int i = 0; i < stepCount; i++) {
			stepFrames[i] = stepToFrame(i);
		}
	}

	/**
	 * Gets the first frame number.
	 * 
	 * @return the frame number
	 */
	public int getFirstFrameNumber() {
		return 0;
	}

	/**
	 * Gets the last frame number.
	 * 
	 * @return the frame number
	 */
	public int getLastFrameNumber() {
		if (video == null || video.getFrameCount() == 1) {
			return getEndFrameNumber();
		}
		return video.getFrameCount() - 1 + extraFrames;
	}

	/**
	 * Returns an XML.ObjectLoader to save and load data for this class.
	 *
	 * @return the object loader
	 */
	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

	/**
	 * A class to save and load data for this class.
	 */
	static class Loader implements XML.ObjectLoader, FinalizableLoader {
		
		private VideoClip clip;
		private String path;
		private Collection<?> filters;
		private double dt;
		private int start;
		private int stepSize;
		private int stepCount;
		private int frameCount;
		private double startTime;
		private String readoutType;
		private boolean playAllSteps;
		private String base;
		private Video video;
		
		/**
		 * Saves object data in an XMLControl.
		 *
		 * @param control the control to save to
		 * @param obj     the object to save
		 */
		@Override
		public void saveObject(XMLControl control, Object obj) {
			VideoClip clip = (VideoClip) obj;
			Video video = clip.getVideo();
			if (video != null) {
				if (video instanceof ImageVideo) {
					ImageVideo vid = (ImageVideo) video;
					if (vid.isFileBased()) {
						control.setValue("video", video); //$NON-NLS-1$
					}
					control.setValue("video_framecount", clip.getFrameCount()); //$NON-NLS-1$
				} else {
					control.setValue("video", video); //$NON-NLS-1$
					control.setValue("video_framecount", video.getFrameCount()); //$NON-NLS-1$
				}
			}
			control.setValue("startframe", clip.getStartFrameNumber()); //$NON-NLS-1$
			control.setValue("stepsize", clip.getStepSize()); //$NON-NLS-1$
			control.setValue("stepcount", clip.getStepCount()); //$NON-NLS-1$
			control.setValue("starttime", clip.startTimeIsSaved ? //$NON-NLS-1$
					clip.savedStartTime : clip.getStartTime());
			control.setValue("readout", clip.readoutType); //$NON-NLS-1$
			control.setValue("playallsteps", clip.playAllSteps); //$NON-NLS-1$
		}

		/**
		 * Creates a temporary object.
		 *
		 * @param control the XMLControl with the object data
		 * @return the newly created object
		 */
		@Override
		public Object createObject(XMLControl control) {
			return new VideoClip(null); // just a non-null placeholder
		}

		/**
		 * Loads a VideoClip with data from an XMLControl.
		 * 
		 * FinalizableLoader just extracts all necessary information from the control
		 * for AsyncVideoI
		 *
		 * @param control the XMLControl
		 * @param obj     the object
		 * @return the loaded object
		 */
		@Override
		public Object loadObject(XMLControl control, Object obj) {
			// load values common to all clips w or w/o video
			start = control.getInt("startframe"); //$NON-NLS-1$
			stepSize = control.getInt("stepsize"); //$NON-NLS-1$
			stepCount = control.getInt("stepcount"); //$NON-NLS-1$
			startTime = control.getDouble("starttime"); //$NON-NLS-1$
			readoutType = control.getString("readout"); //$NON-NLS-1$
			playAllSteps = true;
			if (control.getPropertyNamesRaw().contains("playallsteps")) { //$NON-NLS-1$
				playAllSteps = control.getBoolean("playallsteps"); //$NON-NLS-1$
			}
			frameCount = -1;
			// set total frame count first so start frame will not be limited
			if (control.getPropertyNamesRaw().contains("video_framecount")) { //$NON-NLS-1$
				frameCount = control.getInt("video_framecount"); //$NON-NLS-1$
			} else if (start != Integer.MIN_VALUE && stepSize != Integer.MIN_VALUE && stepCount != Integer.MIN_VALUE) {
				frameCount = start + stepCount * stepSize;
			}

			// if no video, load clip passed in
			boolean hasVideo = control.getPropertyNamesRaw().contains("video"); //$NON-NLS-1$
			if (!hasVideo) {
				clip = (VideoClip)obj;
				finalizeLoading();
				return obj;
			}
			
			// otherwise open the video and make a new clip with it
			base = control.getString("basepath"); //$NON-NLS-1$ ;
			ResourceLoader.addSearchPath(base);

			XMLControl child = control.getChildControl("video"); //$NON-NLS-1$

			filters = (Collection<?>) child.getObject("filters"); //$NON-NLS-1$
			dt = child.getDouble("delta_t"); //$NON-NLS-1$
			String childPath = child.getString("path");
			path = XML.getResolvedPath(childPath, base); // Critical here for TrackerSampler Mechanics
																		// FreeFall MotionDiagram video
			base = XML.getDirectoryPath(path);
			path = XML.getName(path);
			
			ArrayList<VideoType> types = VideoIO.getVideoTypesForPath(path);
			switch (types.size()) {
			case 0:
				break;
			case 1:
				video = VideoIO.getVideo(path, base, types.get(0));
				break;
			default:
				video = VideoIO.getVideo(path, base, null);
				break;
			}

			if (video == null && !VideoIO.isCanceled()) {
				if (ResourceLoader.getResource(path) != null) { // resource exists but not loaded
					OSPLog.info("\"" + path + "\" could not be opened"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				JOptionPane.showMessageDialog(null,
						"\"" + XML.getName(path) + "\" could not be opened" + (types.size() == 0
								? " because no video adapter for this type of video is available on your computer."
								: "."));
//				}
//
//				/**
//				 * Java only -- transpiler can skip this
//				 * 
//				 * @j2sNative
//				 */
//				{
//					if (ResourceLoader.getResource(path) != null) { // resource exists but not loaded
//						OSPLog.info("\"" + path + "\" could not be opened"); //$NON-NLS-1$ //$NON-NLS-2$
//					} else {
//
//						int response = JOptionPane.showConfirmDialog(null, "\"" + path + "\" " //$NON-NLS-1$ //$NON-NLS-2$
//								+ MediaRes.getString("VideoClip.Dialog.VideoNotFound.Message"), //$NON-NLS-1$
//								MediaRes.getString("VideoClip.Dialog.VideoNotFound.Title"), //$NON-NLS-1$
//								JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
//						if (response == JOptionPane.YES_OPTION) {
//							VideoIO.getChooser().setSelectedFile(new File(path));
//							@SuppressWarnings("deprecation")
//							File[] files = VideoIO.getChooserFiles("open");
//							if (files != null && files.length > 0) {
//								String path = XML.getAbsolutePath(files[0]);
//								video = VideoIO.getVideo(path, null);
//							}
//						}
//
//					}
//				}
			}
			if (video != null) {
				if (filters != null) {
					video.getFilterStack().clear();
					Iterator<?> it = filters.iterator();
					while (it.hasNext()) {
						Filter filter = (Filter) it.next();
						video.getFilterStack().addFilter(filter);
					}
				}
				if (video instanceof ImageVideo) {
					if (!Double.isNaN(dt)) {
						((ImageVideo) video).setFrameDuration(dt);
					}
				}
			}
			clip = new VideoClip(video);
			if (path != null) {
				if (!path.startsWith("/") && path.indexOf(":") == -1) { //$NON-NLS-1$ //$NON-NLS-2$
					// convert path to absolute
					path = XML.getResolvedPath(path, base);
				}
				clip.videoPath = path;
			}
			if ((video instanceof AsyncVideoI)) {
				clip.loader = this;
			} else {
				finalizeLoading();
			}

			return clip;
		}


		@Override
		public void finalizeLoading() {
			clip.loader = null;
			if (frameCount == -1) 
				frameCount = clip.getFrameCount();
			clip.setStepCount(frameCount); // this should equal or exceed the actual frameCount

			// set start frame
			if (start != Integer.MIN_VALUE) {
				clip.setStartFrameNumber(start);
			}
			// set step size
			if (stepSize != Integer.MIN_VALUE) {
				clip.setStepSize(stepSize);
			}
			// set step count
			if (stepCount != Integer.MIN_VALUE) {
				clip.setStepCount(stepCount);
			}
			// set start time
			if (!Double.isNaN(startTime)) {
				clip.startTime = startTime;
			}
			clip.readoutType = readoutType;
			clip.playAllSteps = playAllSteps; // by default
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
 * Copyright (c) 2017 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
