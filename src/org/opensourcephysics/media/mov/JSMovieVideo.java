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
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.function.Function;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.media.core.AsyncVideoI;
import org.opensourcephysics.media.core.DoubleArray;
import org.opensourcephysics.media.core.ImageCoordSystem;
import org.opensourcephysics.media.core.Video;
import org.opensourcephysics.media.core.VideoAdapter;
import org.opensourcephysics.media.core.VideoFileFilter;
import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.media.core.VideoType;
import org.opensourcephysics.tools.Resource;
import org.opensourcephysics.tools.ResourceLoader;

import javajs.async.SwingJSUtils.StateHelper;
import javajs.async.SwingJSUtils.StateMachine;
import swingjs.api.js.DOMNode;
import swingjs.api.js.HTML5Video;

/**
 * This is a video that uses HTML5/JS to read mp4, mov, and other movie video formats.
 *
 * status: Only fleshed in; not implemented.
 * 
 * @author rhanson
 */
public class JSMovieVideo extends VideoAdapter implements MovieVideoI, AsyncVideoI {
	
	public static boolean registered;

	/**
	 * Registers HTML5 video types with VideoIO class for file reading
	 *
	 * see https://en.wikipedia.org/wiki/HTML5_video#Browser_support
	 */
	static {
		// add common video types 
		for (String ext : VideoIO.JS_VIDEO_EXTENSIONS) { // {"mov", "ogg", "mp4"}
			VideoFileFilter filter = new VideoFileFilter(ext, new String[] { ext });
			MovieVideoType movieType = new JSMovieVideoType(filter);
			movieType.setRecordable(false);
			VideoIO.addVideoType(movieType);
			ResourceLoader.addExtractExtension(ext);
		}
		registered = true;
	}
  
	
	public Object getProperty(String name) {
		return super.getProperty(name);
	}

	// array of frame start times in milliseconds
	private double[] frameTimesMillis;
	private long systemStartPlayTime;
	private double frameStartPlayTime;
	private int frame;
	//private Timer failDetectTimer;

	private HTML5Video jsvideo;
	
	private JDialog videoDialog;
	private String fileName;
	private URL url;
	
	
	public JSMovieVideo(String path) throws IOException {
		this(path, (String) null);
	}

	public JSMovieVideo(String name, String basePath) throws IOException {
		Frame[] frames = Frame.getFrames();
		for (int i = 0, n = frames.length; i < n; i++) {
			if (frames[i].getName().equals("Tracker")) { //$NON-NLS-1$
				addPropertyChangeListener(PROPERTY_VIDEO_PROGRESS, (PropertyChangeListener) frames[i]); //$NON-NLS-1$
				addPropertyChangeListener(PROPERTY_VIDEO_STALLED, (PropertyChangeListener) frames[i]); //$NON-NLS-1$
				break;
			}
		}
		// timer to detect failures
//		failDetectTimer = new Timer(6000, new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				if (frame == prevFrame) {
//					firePropertyChange("stalled", null, path); //$NON-NLS-1$
//					failDetectTimer.stop();
//				}
//				prevFrame = frame;
//			}
//		});
//		failDetectTimer.setRepeats(true);
		load(name, basePath);
	}


	/**
	 * Plays the video at the current rate. Overrides VideoAdapter method.
	 * pig this and stop below overrides may not be needed--needs testing
	 */
	public void play() {
		if (getFrameCount() == 1) {
			return;
		}
		int n = getFrameNumber() + 1;
		playing = true;
		support.firePropertyChange("playing", null, new Boolean(true)); //$NON-NLS-1$
		startPlayingAtFrame(n);
	}

	/**
	 * Stops the video.
	 */
	public void stop() {
		playing = false;
		support.firePropertyChange("playing", null, new Boolean(false)); //$NON-NLS-1$
	}


	public BufferedImage getImage() {
		return (rawImage == null ? null : super.getImage());
	}
	

	/**
	 * Sets the frame number. Overrides VideoAdapter setFrameNumber method.
	 *
	 * @param n the desired frame number
	 */
	public void setFrameNumber(int n) {
		super.setFrameNumber(n);
//		OSPLog.finest("JSMovieVideo.setFrameNumber " + n + " " + getFrameNumber());
		state.getImage(getFrameNumber());
	}

	/**
	 * Gets the start time of the specified frame in milliseconds.
	 *
	 * @param n the frame number
	 * @return the start time of the frame in milliseconds, or -1 if not known
	 */
	public double getFrameTime(int n) {
		if ((n >= frameTimesMillis.length) || (n < 0)) {
			return -1;
		}
		return frameTimesMillis[n];
	}

	/**
	 * Gets the current frame time in milliseconds.
	 *
	 * @return the current time in milliseconds, or -1 if not known
	 */
	public double getTime() {
		return getFrameTime(getFrameNumber());
	}

	/**
	 * Sets the frame number to (nearly) a desired time in milliseconds.
	 *
	 * @param millis the desired time in milliseconds
	 */
	public void setTime(double millis) {
		millis = Math.abs(millis);
		for (int i = 0; i < frameTimesMillis.length; i++) {
			double t = frameTimesMillis[i];
			if (millis < t) { // find first frame with later start time
				setFrameNumber(i - 1);
				break;
			}
		}
	}

	/**
	 * Gets the start frame time in milliseconds.
	 *
	 * @return the start time in milliseconds, or -1 if not known
	 */
	public double getStartTime() {
		return getFrameTime(getStartFrameNumber());
	}

	/**
	 * Sets the start frame to (nearly) a desired time in milliseconds.
	 *
	 * @param millis the desired start time in milliseconds
	 */
	public void setStartTime(double millis) {
		millis = Math.abs(millis);
		for (int i = 0; i < frameTimesMillis.length; i++) {
			double t = frameTimesMillis[i];
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
	public double getEndTime() {
		int n = getEndFrameNumber();
		if (n < getFrameCount() - 1)
			return getFrameTime(n + 1);
		return getDuration();
	}

	/**
	 * Sets the end frame to (nearly) a desired time in milliseconds.
	 *
	 * @param millis the desired end time in milliseconds
	 */
	public void setEndTime(double millis) {
		millis = Math.abs(millis);
		millis = Math.min(getDuration(), millis);
		for (int i = 0; i < frameTimesMillis.length; i++) {
			double t = frameTimesMillis[i];
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
		return HTML5Video.getDuration(jsvideo);
	}

	/**
	 * Sets the relative play rate. Overrides VideoAdapter method.
	 *
	 * @param rate the relative play rate.
	 */
	public void setRate(double rate) {
		super.setRate(rate);
		if (isPlaying()) {
			startPlayingAtFrame(getFrameNumber());
		}
	}

	/**
	 * Disposes of this video.
	 */
	public void dispose() {
		super.dispose();
		DOMNode.dispose(jsvideo);
		videoDialog.dispose();
		
	}
//______________________________  private methods _________________________

	/**
	 * Sets the system and frame start times.
	 * 
	 * @param frameNumber the frame number at which playing will start
	 */
	private void startPlayingAtFrame(int frameNumber) {
		// systemStartPlayTime is the system time when play starts
		systemStartPlayTime = System.currentTimeMillis();
		// frameStartPlayTime is the frame time where play starts
		frameStartPlayTime = getFrameTime(frameNumber);
		setFrameNumber(frameNumber);
	}

	/**
	 * Plays the next time-appropriate frame at the current rate.
	 */
	private void continuePlaying() {
		int n = getFrameNumber();
		if (n < getEndFrameNumber()) {
//			
//			long elapsedTime = System.currentTimeMillis() - systemStartPlayTime;
//			double frameTime = frameStartPlayTime + getRate() * elapsedTime;
//			int frameToPlay = getFrameNumberBefore(frameTime);
//			while (frameToPlay > -1 && frameToPlay <= n) {
//				elapsedTime = System.currentTimeMillis() - systemStartPlayTime;
//				frameTime = frameStartPlayTime + getRate() * elapsedTime;
//				frameToPlay = getFrameNumberBefore(frameTime);
//			}
//			if (frameToPlay == -1) {
//				frameToPlay = getEndFrameNumber();
//			}
			setFrameNumber(++n);
		} else if (looping) {
			startPlayingAtFrame(getStartFrameNumber());
		} else {
			stop();
		}
	}

	/**
	 * Gets the number of the last frame before the specified time.
	 *
	 * @param time the time in milliseconds
	 * @return the frame number, or -1 if not found
	 */
	private int getFrameNumberBefore(double time) {
		for (int i = 0; i < frameTimesMillis.length; i++) {
			if (time < frameTimesMillis[i])
				return i - 1;
		}
		// if not found, see if specified time falls in last frame
		int n = frameTimesMillis.length - 1;
		// assume last and next-to-last frames have same duration
		double endTime = 2 * frameTimesMillis[n] - frameTimesMillis[n - 1];
		if (time < endTime)
			return n;
		return -1;
	}

	/**
	 * Loads a video specified by name.
	 *
	 * @param fileName the video file name
	 * @throws IOException
	 */
	private void load(String fileName, String basePath) throws IOException {
		this.baseDir = XML.getDirectoryPath(basePath);
		this.fileName = fileName;
		Resource res = ResourceLoader.getResource(getAbsolutePath(fileName));
		if (res == null) {
			throw new IOException("unable to create resource for " + fileName); //$NON-NLS-1$
		}
		url = res.getURL();
		//file = res.getFile();
		
		boolean isLocal = url.getProtocol().toLowerCase().indexOf("file") > -1; //$NON-NLS-1$
		String path = isLocal ? res.getAbsolutePath() : url.toExternalForm();
		OSPLog.finest("JSMovieVideo loading " + path + " local?: " + isLocal); //$NON-NLS-1$ //$NON-NLS-2$
				// set properties
		setProperty("name", XML.getName(fileName)); //$NON-NLS-1$
		setProperty("absolutePath", res.getAbsolutePath()); //$NON-NLS-1$
		if (fileName.indexOf(":") == -1) { //$NON-NLS-1$
			// if name is relative, path is name
			setProperty("path", XML.forwardSlash(fileName)); //$NON-NLS-1$
		} else {
			// else path is relative to user directory
			setProperty("path", XML.getRelativePath(fileName)); //$NON-NLS-1$
		}
		firePropertyChange(PROPERTY_VIDEO_PROGRESS, fileName, 0); //$NON-NLS-1$
		frame = 0;
		//failDetectTimer.start();
		if (state == null)
			state = new State();
		state.load(path);
		
	}

	State state;
	public String err;

	private class State implements StateMachine {
		
		ArrayList<Double> seconds;

		static final int STATE_ERROR             = -99;

		static final int STATE_IDLE              = -1;
		static final int STATE_FIND_FRAMES_INIT  = 00;
		static final int STATE_FIND_FRAMES_LOOP  = 01;
		static final int STATE_FIND_FRAMES_READY = 02;
		static final int STATE_FIND_FRAMES_DONE  = 99;
		
		static final int STATE_LOAD_VIDEO_INIT    = 10;
		static final int STATE_LOAD_VIDEO_READY   = 12;
		
		static final int STATE_GET_IMAGE_INIT    = 20;
		static final int STATE_GET_IMAGE_READY   = 22;
		static final int STATE_GET_IMAGE_READY2  = 23;
		
		
		private StateHelper helper;
		private double t;
		private double dt = 0;
		
		private ActionListener canplaythrough = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				next(StateHelper.UNCHANGED);
			}
			
		};
		private Object[] readyListener;
		private double duration;
		private int thisFrame = 0;

		public Runnable whenReady;

		private boolean debugging = false; // voluminous event information
		
		State() {
			helper = new StateHelper(this);
		}

		protected void next(int stateNext) {
			helper.delayedState(10, stateNext);		
		}

		public void load(String path) {
			helper.next(STATE_LOAD_VIDEO_INIT);
		}

		public void getImage(int n) {
			if (thisFrame == n)
				return;
			thisFrame = n;
			t = JSMovieVideo.this.getFrameTime(n)/1000.0;
			//OSPLog.finest("JSMovieVideo.state.getImage " + n + " " + t);
			next(STATE_GET_IMAGE_INIT);
		}

		public void findAllFrames(Runnable r) {
			helper.next(STATE_FIND_FRAMES_INIT);
		}

		private void dispose()  {
			HTML5Video.removeActionListener(jsvideo, readyListener);
		}
		
		private void seekToNextFrame() {
			if (dt == 0) {
				try {
					jsvideo.seekToNextFrame();
				} catch (Throwable e) {
					err = "JSMovieVideo cannot seek to next Frame";
					e.printStackTrace();
				}
			} else {
				HTML5Video.setCurrentTime(jsvideo, t = t + dt);
			}
		}

		private void setReadyListener() {
			if (readyListener != null)
				return;
			readyListener = HTML5Video.addActionListener(jsvideo, canplaythrough, "canplaythrough", "ended");
			if (debugging) {
				HTML5Video.addActionListener(jsvideo, new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						OSPLog.finest("JSMovieVideo.actionPerformed " + e.getActionCommand());
					}

				});

			}
		}

		@Override
		public boolean stateLoop() {
			JSMovieVideo v = JSMovieVideo.this;
			while (helper.isAlive()) {
				switch (v.err == null ? helper.getState() : STATE_ERROR) {
				case STATE_IDLE:
					return false;
				case STATE_LOAD_VIDEO_INIT:
					v.videoDialog = HTML5Video.createDialog(null, v.url, 500, new Function<HTML5Video, Void>() {

						@Override
						public Void apply(HTML5Video video) {
							v.jsvideo = video;
							next(STATE_LOAD_VIDEO_READY);
							return null;
						}

					});
					return true;
				case STATE_LOAD_VIDEO_READY:
					v.videoDialog.setVisible(true);
					v.size = HTML5Video.getSize(v.jsvideo);
					duration = HTML5Video.getDuration(v.jsvideo);
					int n = HTML5Video.getFrameCount(v.jsvideo);
					v.setFrameCount(n);
					OSPLog.finer("JSMovieVideo " + v.size + "\n duration:" + duration + " est. v.frameCount:" + n);
					setReadyListener();
					findAllFrames(null);
					continue;
				case STATE_FIND_FRAMES_INIT:
					v.err = null;
					setReadyListener();
					duration = HTML5Video.getDuration(v.jsvideo);
					t = 0.0;
					dt = (DOMNode.getAttr(v.jsvideo, "seekToNextFrame") == null ? 0.033334 : 0.0);
					seconds = new ArrayList<Double>();
					helper.setState(STATE_FIND_FRAMES_LOOP);
					continue;
				case STATE_FIND_FRAMES_LOOP:
					if (t >= duration) {
						helper.setState(STATE_FIND_FRAMES_DONE);
						continue;
					}
					helper.setState(STATE_FIND_FRAMES_READY);
					seekToNextFrame();
					return true; // asynchronous
				case STATE_FIND_FRAMES_READY:
					if (VideoIO.isCanceled()) {
						// failDetectTimer.stop();
						v.firePropertyChange(PROPERTY_VIDEO_PROGRESS, v.fileName, null); //$NON-NLS-1$
						// clean up temporary objects
						dispose();
						v.err = "Canceled by user"; //$NON-NLS-1$
						return false;
					}
					t = HTML5Video.getCurrentTime(v.jsvideo);
					// frameTimeStamps.put(frame, Long.valueOf((long) (t * 1000)));
					seconds.add(Double.valueOf(t));
					v.firePropertyChange(PROPERTY_VIDEO_PROGRESS, v.fileName, v.frame); //$NON-NLS-1$
					v.frame++;
					//OSPLog.finest("JSMovieVideo frame " + frame + " " + t);
					helper.setState(STATE_FIND_FRAMES_LOOP);
					continue;
				case STATE_FIND_FRAMES_DONE:
					helper.setState(STATE_IDLE);
					v.videoDialog.setVisible(false);
					// clean up temporary objects
					// throw IOException if no frames were loaded
					if (seconds.size() == 0) {
						v.firePropertyChange(PROPERTY_VIDEO_PROGRESS, v.fileName, null); //$NON-NLS-1$
						dispose();
						v.err = "no frames"; //$NON-NLS-1$
					}

					// set initial video clip properties
					v.setFrameCount(seconds.size());
					OSPLog.finer("JSMovieVideo " + v.size + "\n duration:" + duration + " act. frameCount:" + v.frameCount);
					v.startFrameNumber = 0;
					v.endFrameNumber = v.frameCount - 1;
					// create startTimes array
					v.frameTimesMillis = new double[v.frameCount];
					for (int i = 1; i < v.frameTimesMillis.length; i++) {
						v.frameTimesMillis[i] = seconds.get(i).doubleValue()*1000;
					}
					seconds = null;

					v.firePropertyChange(PROPERTY_VIDEO_PROGRESS, v.fileName, null); //$NON-NLS-1$ // to TFrame
					thisFrame = -1;
					v.setFrameNumber(0);
					// failDetectTimer.stop();
//					if (img == null) {
//						dispose();
//						throw new IOException("No images"); //$NON-NLS-1$
//					}
//					setImage(img);
					continue;
				case STATE_GET_IMAGE_INIT:
					helper.setState(STATE_GET_IMAGE_READY);
					setReadyListener();
					HTML5Video.setCurrentTime(v.jsvideo, t);
					return true;
				case STATE_GET_IMAGE_READY:
					BufferedImage bi = HTML5Video.getImage(v.jsvideo);
					if (bi != null) {
						v.isValidImage = false;
						v.isValidFilteredImage = false;
						if (v.rawImage == null) {
							v.rawImage = bi;
							v.firePropertyChange(PROPERTY_VIDEO_ASYNC_READY, null, Integer.valueOf(v.frameCount));
							return true;
						}
							v.rawImage = bi;
							v.firePropertyChange(Video.PROPERTY_VIDEO_FRAMENUMBER, null, new Integer(thisFrame)); //$NON-NLS-1$
							v.firePropertyChange(AsyncVideoI.PROPERTY_VIDEO_IMAGE_READY, null, bi); //$NON-NLS-1$
							if (v.isPlaying()) {
								Runnable runner = new Runnable() {
								public void run() {
									v.continuePlaying();
								}
								};
								SwingUtilities.invokeLater(runner);
							}
					}
					return false;
				///////////////////////////////////////
				}
				return false;
			}
			return false;

		}
	}	
	/**
	 * Sets the initial image.
	 *
	 * @param image the image
	 */
	private void setImage(BufferedImage image) {
		rawImage = image;
		size = new Dimension(image.getWidth(), image.getHeight());
		refreshBufferedImage();
	}

	private void setFrameCount(int n) {
		frameCount = n;
		// create coordinate system and relativeAspects
		coords = new ImageCoordSystem(frameCount);
		coords.addPropertyChangeListener(JSMovieVideo.this);
		aspects = new DoubleArray(frameCount, 1);

	}

	/**
	 * Returns an XML.ObjectLoader to save and load JSMovieVideo data.
	 *
	 * @return the object loader
	 */
	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

	/**
	 * A class to save and load JSMovieVideo data.
	 */
	static public class Loader extends VideoAdapter.Loader {

		@Override
		protected VideoAdapter createVideo(String path) throws IOException {
			VideoAdapter video = new JSMovieVideo(path);
			String ext = XML.getExtension(path);
			VideoType VideoType = VideoIO.getMovieType(ext);
			if (VideoType != null)
				video.setProperty("video_type", VideoType); //$NON-NLS-1$
			return video;
		}
	}

	public static File createThumbnailFile(Dimension defaultThumbnailDimension, String sourcePath, String thumbPath) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String getTypeName() {
		return MovieFactory.ENGINE_JS;
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
