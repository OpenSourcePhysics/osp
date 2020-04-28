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

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.media.core.AsyncVideoI;
import org.opensourcephysics.media.core.DoubleArray;
import org.opensourcephysics.media.core.ImageCoordSystem;
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
 * @author Douglas Brown
 * @version 1.0
 */
public class JSMovieVideo extends VideoAdapter implements MovieVideoI, AsyncVideoI {
	
	public static boolean registered;

	/**
	 * Registers HTML5 video types with VideoIO class for file reading
	 *
	 * see https://en.wikipedia.org/wiki/HTML5_video#Browser_support
	 */
	static {
		MovieFactory.addMovieVideoType(new JSMovieVideoType());
		// add common video types -- was JSMovieIO
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
	private double[] startTimes;
	private long systemStartPlayTime;
	private double frameStartPlayTime;
	private int frame;
	//private Timer failDetectTimer;

	private HTML5Video jsvideo;
	
	private JDialog videoDialog;
	private String fileName;
	private URL url;
	
	public JSMovieVideo(String path) throws IOException {
		Frame[] frames = Frame.getFrames();
		for (int i = 0, n = frames.length; i < n; i++) {
			if (frames[i].getName().equals("Tracker")) { //$NON-NLS-1$
				addPropertyChangeListener("progress", (PropertyChangeListener) frames[i]); //$NON-NLS-1$
				addPropertyChangeListener("stalled", (PropertyChangeListener) frames[i]); //$NON-NLS-1$
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
		load(path);
	}

	/**
	 * Plays the video at the current rate. Overrides VideoAdapter method.
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
		if (n == getFrameNumber())
			return;
		super.setFrameNumber(n);
		state.getImage(n);
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
		for (int i = 0; i < startTimes.length; i++) {
			double t = startTimes[i];
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
		for (int i = 0; i < startTimes.length; i++) {
			double t = startTimes[i];
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
		for (int i = 0; i < startTimes.length; i++) {
			double t = startTimes[i];
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
			long elapsedTime = System.currentTimeMillis() - systemStartPlayTime;
			double frameTime = frameStartPlayTime + getRate() * elapsedTime;
			int frameToPlay = getFrameNumberBefore(frameTime);
			while (frameToPlay > -1 && frameToPlay <= n) {
				elapsedTime = System.currentTimeMillis() - systemStartPlayTime;
				frameTime = frameStartPlayTime + getRate() * elapsedTime;
				frameToPlay = getFrameNumberBefore(frameTime);
			}
			if (frameToPlay == -1) {
				frameToPlay = getEndFrameNumber();
			}
			setFrameNumber(frameToPlay);
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
		for (int i = 0; i < startTimes.length; i++) {
			if (time < startTimes[i])
				return i - 1;
		}
		// if not found, see if specified time falls in last frame
		int n = startTimes.length - 1;
		// assume last and next-to-last frames have same duration
		double endTime = 2 * startTimes[n] - startTimes[n - 1];
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
	private void load(String fileName) throws IOException {
		this.fileName = fileName;
		Resource res = ResourceLoader.getResource(fileName);
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
		firePropertyChange("progress", fileName, 0); //$NON-NLS-1$
		frame = 0;
		//failDetectTimer.start();
		if (state == null)
			state = new State();
		state.load(path);
		
	}

	State state;
	public String err;

	private class State implements StateMachine {
		
		ArrayList<Double> seconds = new ArrayList<Double>();

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
		
		
		private StateHelper helper;
		private double t;
		private double dt = 0;
		
		private ActionListener canplaythrough = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				helper.next(StateHelper.UNCHANGED);
			}
			
		};
		private Object[] readyListener;
		private double duration;
		private int thisFrame = 0;
		
		State() {
			helper = new StateHelper(this);
		}

		public void load(String path) {
			helper.next(STATE_LOAD_VIDEO_INIT);
		}

		public void getImage(int n) {
			if (thisFrame == n)
				return;
			thisFrame = n;
			t = JSMovieVideo.this.getFrameTime(n);
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
			if (readyListener == null)
				readyListener = HTML5Video.addActionListener(jsvideo, canplaythrough, "canplaythrough");
		}
		@Override
		public boolean stateLoop() {
			while (helper.isAlive()) {
				switch (err == null ? helper.getState() : STATE_ERROR) {
				case STATE_IDLE:
					return false;
				case STATE_LOAD_VIDEO_INIT:
					videoDialog = HTML5Video.createDialog(null, url, 500, new Function<HTML5Video, Void>() {
						
						@Override
						public Void apply(HTML5Video video) {
							jsvideo = video;
							helper.next(STATE_LOAD_VIDEO_READY);
							return null;
						}
						
					});
					return true;
				case STATE_LOAD_VIDEO_READY:
					videoDialog.setVisible(true);
					size = HTML5Video.getSize(jsvideo);
					duration = HTML5Video.getDuration(jsvideo);
					int n = HTML5Video.getFrameCount(jsvideo);
					setFrameCount(n);
					OSPLog.finer("JSMovieVideo " + size 
							+ "\n duration:" + duration + " est. frameCount:" + n);
					setReadyListener();
					helper.setState(STATE_GET_IMAGE_READY);
					continue;
				case STATE_GET_IMAGE_INIT:
					helper.setState(STATE_GET_IMAGE_READY);
					setReadyListener();
					HTML5Video.setCurrentTime(jsvideo, t);
					return true;
				case STATE_GET_IMAGE_READY:
					BufferedImage bi = HTML5Video.getImage(jsvideo);
					if (bi != null) {
						isValidImage = false;
						isValidFilteredImage = false;
						if (rawImage == null)
							setImage(bi);
						rawImage = bi;
						firePropertyChange("framenumber", null, new Integer(thisFrame)); //$NON-NLS-1$
//						if (isPlaying()) {
//							Runnable runner = new Runnable() {
//								public void run() {
//									continuePlaying();
//								}
//							};
//							SwingUtilities.invokeLater(runner);
//						}
					}
					findAllFrames(null);
					return false;
				case STATE_FIND_FRAMES_INIT:
					err = null;
					setReadyListener();
					duration = HTML5Video.getDuration(jsvideo);
					t = 0.0;
					dt = (DOMNode.getAttr(jsvideo, "seekToNextFrame") == null ? 0.033334 : 0.0);
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
						firePropertyChange("progress", fileName, null); //$NON-NLS-1$
						// clean up temporary objects
						dispose();
						err = "Canceled by user"; //$NON-NLS-1$
						return false;
					}
					t = HTML5Video.getCurrentTime(jsvideo);
					//frameTimeStamps.put(frame, Long.valueOf((long) (t * 1000)));
					seconds.add(Double.valueOf(t * 1000));
					firePropertyChange("progress", fileName, frame); //$NON-NLS-1$
					frame++;
					OSPLog.finest("JSMovieVideo frame " + frame + " " + t);
					helper.setState(STATE_FIND_FRAMES_LOOP);
					continue;
					///////////////////////////////////////
				case STATE_FIND_FRAMES_DONE:
					// clean up temporary objects
					// throw IOException if no frames were loaded
					if (seconds.size() == 0) {
						firePropertyChange("progress", fileName, null); //$NON-NLS-1$
						dispose();
						err = "no frames"; //$NON-NLS-1$
					}

					// set initial video clip properties
					setFrameCount(seconds.size());
					OSPLog.finer("JSMovieVideo " + size 
							+ "\n duration:" + duration + " act. frameCount:" + frameCount);
					startFrameNumber = 0;
					endFrameNumber = frameCount - 1;
					// create startTimes array
					startTimes = new double[frameCount];
					startTimes[0] = 0;
					for (int i = 1; i < startTimes.length; i++) {
						startTimes[i] = seconds.get(i) * 1000;
					}

					firePropertyChange("progress", fileName, null); //$NON-NLS-1$
					// failDetectTimer.stop();
//					if (img == null) {
//						dispose();
//						throw new IOException("No images"); //$NON-NLS-1$
//					}
//					setImage(img);
					helper.setState(STATE_IDLE);
					continue;
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
	 * Returns an XML.ObjectLoader to save and load XuggleVideo data.
	 *
	 * @return the object loader
	 */
	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

	/**
	 * A class to save and load XuggleVideo data.
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
	public String getName() {
		return "JS";
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
