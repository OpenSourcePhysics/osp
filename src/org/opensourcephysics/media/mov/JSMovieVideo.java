/*
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
 * please see <https://www.compadre.org/osp/>.
 */
package org.opensourcephysics.media.mov;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.function.Function;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.media.core.AsyncVideoI;
import org.opensourcephysics.media.core.DoubleArray;
import org.opensourcephysics.media.core.ImageCoordSystem;
import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.tools.Resource;
import org.opensourcephysics.tools.ResourceLoader;

import javajs.async.SwingJSUtils.StateHelper;
import javajs.async.SwingJSUtils.StateMachine;
import swingjs.api.js.DOMNode;
import swingjs.api.js.HTML5Video;
import swingjs.api.js.JSFunction;

/**
 * This is a video that uses HTML5/JS to read mp4, mov, and other movie video formats.
 *
 * status: Only fleshed in; not implemented.
 * 
 * @author rhanson
 */
public class JSMovieVideo extends MovieVideo implements AsyncVideoI {
	
	private static final int FORCE_TO_START = -99;

	boolean debugHTMLVideo = false; // voluminous event information

	boolean allowControlData = false; // false to not read control data and instead scan video
	
	State state;
	public String err;

	
	/**
	 * Registers HTML5 video types with VideoIO class for file reading
	 *
	 * see https://en.wikipedia.org/wiki/HTML5_video#Browser_support
	 */
	static {
	   JSMovieVideoType.register();
	}
  	
	@Override
	public Object getProperty(String name) {
		return super.getProperty(name);
	}

	private int frame;

	private HTML5Video jsvideo;	
	
	private JDialog videoDialog;
	private String fileName;
	private URL url;

	protected int progress;

	/**
	 * 
	 * @param fileName
	 * @param basePath if from XMLControl for VideoClip or from ExportZipDialog
	 * @param control  if going or from control
	 * @throws IOException
	 */
	public JSMovieVideo(String fileName, String basePath, XMLControl control) throws IOException {
		boolean isExport = (control != null && !"video".equals(control.getPropertyName()));
		addFramePropertyListeners();
		this.baseDir = basePath;
		this.fileName = fileName;
		String path = getAbsolutePath(fileName);
		Resource res = (ResourceLoader.isHTTP(path) ? new Resource(new URL(path)) : new Resource(new File(path)));
		url = res.getURL();
		boolean isLocal = (url.getProtocol().toLowerCase().indexOf("file") >= 0); //$NON-NLS-1$
		path = isLocal ? res.getAbsolutePath() : url.toExternalForm();
		OSPLog.finest("JSMovieVideo loading " + path + " local?: " + isLocal); //$NON-NLS-1$ //$NON-NLS-2$
		if (!VideoIO.checkMP4(path, null, null)) {
			frameNumber = Integer.MIN_VALUE;
			return;
		}
		// set properties
		setProperty("name", XML.getName(fileName)); //$NON-NLS-1$
		setProperty("absolutePath", res.getAbsolutePath()); //$NON-NLS-1$
		if (fileName.indexOf(":") < 0) { //$NON-NLS-1$
			// if name is relative, path is name
			setProperty("path", XML.forwardSlash(fileName)); //$NON-NLS-1$
		} else {
			// else path is relative to user directory
			setProperty("path", XML.getRelativePath(fileName)); //$NON-NLS-1$
		}
		if (isExport) {
			// no need to actually load anthing?
			return;
		}
		firePropertyChange(PROPERTY_VIDEO_PROGRESS, fileName, 0);
		frame = 0;
		//startFailDetection();
		if (state == null)
			state = new State(allowControlData ? control : null);
		state.load(path);

	}

	/**
	 * Plays the video at the current rate. Overrides VideoAdapter method.
	 */
	@Override
	public void play() {
		if (getFrameCount() == 1) {
			return;
		}
		int n = getFrameNumber() + 1;
		playing = true;
		firePropertyChange(PROPERTY_VIDEO_PLAYING, null, Boolean.TRUE); //$NON-NLS-1$
		setFrameNumber(n);
	}

	/**
	 * Stops the video.
	 */
	@Override
	public void stop() {
		playing = false;
		firePropertyChange(PROPERTY_VIDEO_PLAYING, null, Boolean.FALSE); //$NON-NLS-1$
	}


	@Override
	public BufferedImage getImage() {
		return (rawImage == null ? null : super.getImage());
	}
	
	/**
	 * Sets the frame number. Overrides VideoAdapter setFrameNumber method.
	 *
	 * @param n the desired frame number
	 */
	@Override
	public void setFrameNumber(int n) {
		if (n < 0) {
			// force super
			this.frameNumber = n;
			n = 0;
		}
		super.setFrameNumber(n);
		state.getImage(getFrameNumber());
	}
	
	public void setFrameNumberContinued(int n, double t) {
		BufferedImage bi = HTML5Video.getImage(jsvideo, BufferedImage.TYPE_INT_RGB);
		if (bi == null)
			return;
		rawImage = bi;
		invalidateVideoAndFilter();
		// just repaints VideoPanel
		//System.out.println("JSMV imageready for " + n + " t=" + t);
		notifyFrame(n, false);
		firePropertyChange(PROPERTY_ASYNCVIDEOI_IMAGEREADY, null, n);
		if (isPlaying()) {
			SwingUtilities.invokeLater(() -> {
				continuePlaying();
			});
		}
	}

	@Override
	public double getDuration() {
		return jsvideo == null ? -1 : HTML5Video.getDuration(jsvideo) * 1000;
	}
	
	/**
	 * Sets the relative play rate. Overrides VideoAdapter method.
	 *
	 * @param rate the relative play rate.
	 */
	@Override
	public void setRate(double rate) {
		super.setRate(rate);
		if (isPlaying()) {
			setFrameNumber(getFrameNumber());
		}
	}

	/**
	 * Disposes of this video.
	 */
	@Override
	public void dispose() {
		super.dispose();
		DOMNode.dispose(jsvideo);
		videoDialog.dispose();
		
	}
//______________________________  private methods _________________________

	/**
	 * Plays the next time-appropriate frame at the current rate.
	 */
	protected void continuePlaying() {
		int n = getFrameNumber();
		if (n < getEndFrameNumber()) {
			setFrameNumber(++n);
		} else if (looping) {
			setFrameNumber(getStartFrameNumber());
		} else {
			stop();
		}
	}

	@Override
	public String getTypeName() {
		return MovieFactory.ENGINE_JS;
	}

	private class State implements StateMachine {
				
		ArrayList<Double> frameTimes;

		static final int STATE_ERROR              = -99;

		static final int STATE_IDLE               = -1;

		static final int STATE_SET_FROM_CONTROL   = 30;

		static final int STATE_PLAY_WITH_CALLBACK = 40;
		static final int STATE_PLAY_ALL_INIT      = 41;
		static final int STATE_PLAY_ALL_DONE      = 42;

		static final int STATE_FIND_FRAMES_INIT   = 00;
		static final int STATE_FIND_FRAMES_LOOP   = 01;
		static final int STATE_FIND_FRAMES_WAIT   = 02;
		static final int STATE_FIND_FRAMES_READY  = 03;
		
		static final int STATE_FIND_FRAMES_DONE   = 99;
		
		static final int STATE_LOAD_VIDEO_INIT    = 10;
		static final int STATE_LOAD_VIDEO_READY   = 12;
		
		static final int STATE_GET_IMAGE_INIT     = 20;
		static final int STATE_GET_IMAGE_READY    = 22;

//		static final int STATE_GET_IMAGE_READY2  = 23;

		
		
		private StateHelper helper;
		private double t;
		private double dt = 0;
		/**
		 * offset to add to all frame times when sending setCurrentTime
		 */
		private double offset = 0;
		private double lastT = -1;
		
		private ActionListener onevent = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				//System.out.println("JSMovieVideo.onevent state=" + helper.getState() + " " + e);
				switch (helper.getState()) {
				case STATE_FIND_FRAMES_WAIT:
					helper.setState(STATE_FIND_FRAMES_READY);
					break;
				case STATE_PLAY_ALL_INIT:
					helper.setState(STATE_PLAY_ALL_DONE);
					break;
				}
				next(StateHelper.UNCHANGED);
			}
			
		};
		private Object[] readyListener;
		private double duration;
		private int thisFrame = -1;
		
		private double[] htmlFrameTimings;

		private Object[] debugListeners;

		private boolean canSeek = true;

		private String playThroughOrSeeked = "canplaythrough";

		private XMLControl control;

		
		State(XMLControl control) {
			helper = new StateHelper(this);
			this.control = control;
		}

		public void load(String path) {
			helper.next(STATE_LOAD_VIDEO_INIT);
		}

		protected void next(int stateNext) {
			helper.delayedState(10, stateNext);		
		}

		public void getImage(int n) {
			//System.out.println("JSMV.getImage " + thisFrame + " " + n);
			if (thisFrame == n)
				return;
			thisFrame = n;
			t = JSMovieVideo.this.getFrameTime(n)/1000.0;
			//OSPLog.finest("JSMovieVideo.state.getImage " + n + " " + t);
			next(STATE_GET_IMAGE_INIT);
		}

		private void dispose()  {
			removeReadyListener();
		}
		
		private void seekToNextFrame() {
			// canSeek only
				try {
					
					@SuppressWarnings("unused")
					Runnable next = new Runnable() {

						@Override
						public void run() {
							next(STATE_FIND_FRAMES_READY);
						}
						
					};
					
					JSFunction f = /** @j2sNative
					 	function() {next.run$()}
					||*/null;
					jsvideo.seekToNextFrame().then(f,null);
				} catch (Throwable e) {
					err = "JSMovieVideo cannot seek to next Frame";
					e.printStackTrace();
				}
		}

		private void setReadyListener(String event) {
			if (readyListener != null)
				return;
			readyListener = HTML5Video.addActionListener(jsvideo, onevent, event);
			if (debugHTMLVideo) {
				// this will create an action listener for ALL events. 
				System.out.println("JSMovieVideo.debugging=true; Setting listener for " + event);
				debugListeners = HTML5Video.addActionListener(jsvideo, new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						Object[] o = (Object[]) e.getSource();
						@SuppressWarnings("unused")
						Object jsEvent = o[1];
						Object target = /** @j2sNative jsEvent.target.currentTime || */ null;
						System.out.println("JSMovieVideo.debugging.actionPerformed " + e.getActionCommand() + " " + target);
					}

				});
				for (int i = 0; i < debugListeners.length; i += 2) {
					System.out.println("listening for " + debugListeners[i]);
				}

			}
		}

		private void removeReadyListener() {
			HTML5Video.removeActionListener(jsvideo, readyListener);
			readyListener = null;
			if (debugHTMLVideo) {
				HTML5Video.removeActionListener(jsvideo, debugListeners);
				debugListeners = null;
			}
		}

		private XMLControl setFromControl() {
			XMLControl videoControl = control.getChildControl("video");
			XMLControl clipControl = control.getChildControl("clipcontrol");
			if (videoControl == null || clipControl == null)
				return null;
			frameCount = videoControl.getInt("frameCount");
			if (frameCount == Integer.MIN_VALUE) {
				frameCount = videoControl.getInt("stepCount");
				int stepSize = videoControl.getInt("stepCount");
				if (stepSize != 1 || frameCount == Integer.MIN_VALUE)
					return null;
			}
			dt = clipControl.getDouble("delta_t") * 1000; // in milliseconds
			if (Double.isNaN(dt)) {
				return null;
			}
			htmlFrameTimings = new double[frameCount];
			double controlDuration = dt * frameCount;
			dt = dt * duration / controlDuration;
			for (int i = 1; i < frameCount; i++) {
				htmlFrameTimings[i] = dt * i;
			}
			return control;
		}

		@Override
		public boolean stateLoop() {
			System.out.println("JSMovieVideo.stateLoop " + helper.getState());
			JSMovieVideo v = JSMovieVideo.this;
			while (helper.isAlive()) {
				switch (v.err == null ? helper.getState() : STATE_ERROR) {
				case STATE_IDLE:
					return false;
				case STATE_LOAD_VIDEO_INIT:
					v.videoDialog = HTML5Video.createDialog(null, v.url, 500, false, new Function<HTML5Video, Void>() {

						@Override
						public Void apply(HTML5Video video) {
							v.jsvideo = video;
							canSeek = (DOMNode.getAttr(v.jsvideo, "seekToNextFrame") != null);
							if (!canSeek) {
								playThroughOrSeeked = "seeked";
							}
							next(STATE_LOAD_VIDEO_READY);
							return null;
						}

					});
					return true;
				case STATE_LOAD_VIDEO_READY:
					v.videoDialog.setVisible(true);
					Dimension d = HTML5Video.getSize(v.jsvideo);
					v.size.width = d.width;
					v.size.height = d.height;
					duration = HTML5Video.getDuration(v.jsvideo);
					int estimatedFrameCount = (int) (duration / 0.033334);
					//this is all this does: HTML5Video.getFrameCount(v.jsvideo);
					v.setFrameCount(estimatedFrameCount);
					OSPLog.finer("JSMovieVideo LOAD_VIDEO_READY " + v.size + "\n duration:" + duration + " est. frameCount:" + estimatedFrameCount);
					if (v.size.width == 0) {
						cantRead();
						helper.next(STATE_FIND_FRAMES_READY);
					} else {
						if (control != null)
							control = setFromControl();
						helper.next(control != null ? STATE_SET_FROM_CONTROL : canSeek ? STATE_FIND_FRAMES_INIT : STATE_PLAY_WITH_CALLBACK);
					}
					continue;
				case STATE_SET_FROM_CONTROL:
					setFromControl();
					helper.next(STATE_FIND_FRAMES_INIT);
					continue;
				case STATE_PLAY_WITH_CALLBACK:
					htmlFrameTimings = new double[4000];
					HTML5Video.requestVideoFrameCallback(v.jsvideo, htmlFrameTimings);
					helper.next(STATE_PLAY_ALL_INIT);
					continue;
				case STATE_PLAY_ALL_INIT:
					setReadyListener("ended");
					// Chrome will use this to gather a simple HTML5 array
					// of timings from the Video.requestVideoFrameCallback() return. 
					// interestingly, it can report twice per frame. This is almost
					// certainly because the GPU is doubling the playback rate of 
				    // the video with interpolations. The actual times seem oddly
					// incorrect, but we don't actually use them anyway.
					HTML5Video.startVideo(v.jsvideo);
					return false;
				case STATE_PLAY_ALL_DONE:
					removeReadyListener();
					helper.setState(STATE_FIND_FRAMES_INIT);
					continue;
				case STATE_FIND_FRAMES_INIT:
					v.err = null;
					if (duration == 0)
						duration = HTML5Video.getDuration(v.jsvideo);
					lastT = t = 0.0;
					dt = 0;
					frameTimes = new ArrayList<Double>();
					frameTimes.add(t);
					if (canSeek) {
						setCurrentTime(v, 0);
						setReadyListener(playThroughOrSeeked);
						helper.setState(STATE_FIND_FRAMES_LOOP);
						continue;
					}
					HTML5Video.cancelVideoFrameCallback(v.jsvideo);
					double[] timings = (double[]) (Object) htmlFrameTimings;
					setTimes(timings);
					helper.setState(STATE_FIND_FRAMES_DONE);
					return false;
				case STATE_FIND_FRAMES_LOOP:
					if (t >= duration) {
						helper.setState(STATE_FIND_FRAMES_DONE);
						continue;
					}
					helper.setState(STATE_FIND_FRAMES_WAIT);
					seekToNextFrame();
					return false;
				case STATE_FIND_FRAMES_WAIT:
					return false;
				case STATE_FIND_FRAMES_READY:
					if (VideoIO.isCanceled()) {
						v.firePropertyChange(PROPERTY_VIDEO_PROGRESS, v.fileName, null);
						dispose();
						v.err = "Canceled by user"; //$NON-NLS-1$
						progress = VideoIO.PROGRESS_VIDEO_CANCELED;
						return false;
					}
					t = HTML5Video.getCurrentTime(v.jsvideo);
					if (t > lastT && t < duration) {
						lastT = t;
						frameTimes.add(t);
						v.firePropertyChange(PROPERTY_VIDEO_PROGRESS, v.fileName, v.frame++);
						progress = VideoIO.progressForFraction(v.frame, v.frameCount);
					}
					helper.setState(STATE_FIND_FRAMES_LOOP);
					continue;
				case STATE_FIND_FRAMES_DONE:
					helper.setState(STATE_IDLE);
					v.initializeMovie(frameTimes, duration);
					frameTimes = null;
					thisFrame = -1;
					progress = VideoIO.PROGRESS_VIDEO_READY;
					continue;
				case STATE_GET_IMAGE_INIT:
					// this is the starting point for triggering 
					// asyncronous image creation from the video anytime during
					// animation of the frames in Tracker. 
					helper.setState(STATE_GET_IMAGE_READY);
					setReadyListener(playThroughOrSeeked );
					setCurrentTime(v, offset + t);
					return true;
				case STATE_GET_IMAGE_READY:
					//about 0.1 sec to seek OSPLog.debug(Performance.timeCheckStr("JSMovieVideo.getImage ready", Performance.TIME_MARK));
					v.setFrameNumberContinued(thisFrame, t);
					return false;
				///////////////////////////////////////
				}
				return false;
			}
			return false;

		}

		private void setCurrentTime(JSMovieVideo v, double t) {
			System.out.println("setting ct " + t);
			HTML5Video.setCurrentTime(v.jsvideo, t);		
		}

		protected void setTimes(double[] times) {
			int n = (int) times[0] + 1;
			times[0] = times[1];
			if (n < 2) {
				// format was read, but probably image size was (0,0) meaning
				// codec wasn't read. (Have duration, just not actual images.)
				cantRead();
				next(STATE_FIND_FRAMES_DONE);
			} else {
				int nFrames = 1;
				for (int i = 1; i < n; i++) {
					  dt = times[i] - times[i - 1];
					  if (dt > 0) {
						  nFrames++;
						  System.out.println("dt " + nFrames + "\t" + dt);
					  }
				}
				dt = duration / (nFrames + 1); // you might think this should be nFrames
				  System.out.println("dt finally " + dt);
				offset = dt/2;
				t = 0;
				for (int i = 1; i < nFrames; i++) {
					t += dt;
					frameTimes.add(Double.valueOf(t));
				}
				next(STATE_FIND_FRAMES_DONE);
				frame = nFrames;
			}
		}
	}	
//	/**
//	 * Sets the initial image.
//	 *
//	 * @param image the image
//	 */
//	private void setImage(BufferedImage image) {
//		rawImage = image;
//		size = new Dimension(image.getWidth(), image.getHeight());
//		refreshBufferedImage();
//	}

	@Override
	protected void setFrameCount(int n) {
		super.setFrameCount(n);
		// create coordinate system and relativeAspects
		coords = new ImageCoordSystem(frameCount, this);
		aspects = new DoubleArray(frameCount, 1);

	}

	void initializeMovie(ArrayList<Double> frameTimes, double duration) {
		videoDialog.setVisible(false);
		// clean up temporary objects
		// throw IOException if no frames were loaded
		if (frameTimes.size() == 0) {
			firePropertyChange(PROPERTY_VIDEO_PROGRESS, fileName, frame);
			dispose();
			err = "no frames"; //$NON-NLS-1$
		}

		// set initial video clip properties
		setFrameCount(frameTimes.size());
		OSPLog.debug(
				"JSMovieVideo " + size + "\n duration:" + duration + " act. frameCount:" + frameCount);
		startFrameNumber = 0;
		endFrameNumber = frameCount - 1;
		// create startTimes array
		startTimes = new double[frameCount];
		// BH 2020.09.11 note: this was i=1, but then mp4 initial frame was double
		// length
		for (int i = 0; i < frameCount; i++) {
			startTimes[i] = frameTimes.get(i).doubleValue() * 1000;
			System.out.println("startTimes[" + i + "]=" + startTimes[i]);
		}
		// set the duration to exactly what xuggle sets it as:
		// 
		firePropertyChange(PROPERTY_VIDEO_PROGRESS, fileName, frame); // to TFrame
		frameNumber = -1;
		// to VideoClip: time to initArray()
		firePropertyChange(PROPERTY_ASYNCVIDEOI_HAVEFRAMES, null, this); 
		// to VideoPanel aka TrackerPanel: all set!
		firePropertyChange(PROPERTY_ASYNCVIDEOI_READY, fileName, this); 
		setFrameNumber(FORCE_TO_START);
}

	public void cantRead() {
		JOptionPane.showMessageDialog(null, "Video file format or compression method could not be read.");
		VideoIO.setCanceled(true);
	}

	/**
	 * Returns an XML.ObjectLoader to save and load JSMovieVideo data.
	 *
	 * @return the object loader
	 */
	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

	public static File createThumbnailFile(Dimension defaultThumbnailDimension, String sourcePath, String thumbPath) {
		return null;
	}

	@Override
	public int getProgress() {
		return progress;
	}

	@Override
	public int getLoadedFrameCount() {
		return frame;
	}

	/**
	 * A class to save and load JSMovieVideo data.
	 */
	static public class Loader extends MovieVideo.Loader {

		@Override
		protected JSMovieVideo createVideo(XMLControl control, String path) throws IOException {
			JSMovieVideo video = new JSMovieVideo(path, null, control);
			if (video.getFrameNumber() < 0)
				return null;
			setVideo(path, video, MovieFactory.ENGINE_JS);
			return video;
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
