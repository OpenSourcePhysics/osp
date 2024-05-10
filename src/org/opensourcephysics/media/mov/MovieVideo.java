package org.opensourcephysics.media.mov;

import java.awt.Frame;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.media.core.Video;
import org.opensourcephysics.media.core.VideoAdapter;
import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.media.core.VideoType;

/**
 * An abstract class that describes common aspects of VideoAdapters that
 * involve actual digital video sources such as MP4 (JSMovieVideo and XuggleVideo).
 * 
 * (formerly MovieVideoI interface)
 * 
 * @author hansonr
 *
 */
public abstract class MovieVideo extends VideoAdapter {
	
	public final static String PROPERTY_VIDEO_PROGRESS = "progress"; //$NON-NLS-1$ // see TFrame
	public final static String PROPERTY_VIDEO_STALLED  = "stalled"; //$NON-NLS-1$  // see TFrame

	protected ArrayList<Double> frameTimes;


	// load,save for video frames would be here
	
//	private final Timer failDetectTimer;


	protected void addFramePropertyListeners() {
		Frame[] frames = Frame.getFrames();
		for (int i = 0, n = frames.length; i < n; i++) {
			if (frames[i].getName().equals("Tracker")) { //$NON-NLS-1$
				addPropertyChangeListener(PROPERTY_VIDEO_PROGRESS, (PropertyChangeListener) frames[i]);
				addPropertyChangeListener(PROPERTY_VIDEO_STALLED, (PropertyChangeListener) frames[i]);
				break;
			}
		}
		// timer to detect failures
//		failDetectTimer = new Timer(5000, new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				if (VideoIO.isCanceled()) {
//					failDetectTimer.stop();
//					return;
//				}
//				if (frameRefs[FRAME] == frameRefs[PREVFRAME]) {
//					firePropertyChange(PROPERTY_VIDEO_STALLED, null, fileName);
//					failDetectTimer.stop();
//				}
//				frameRefs[PREVFRAME] = frameRefs[FRAME];
//			}
//		});
//		failDetectTimer.setRepeats(true);

	}

	@Override
	protected void setStartTimes() {
		startTimes = new double[frameCount];
		startTimes[0] = 0;
		for (int i = 1; i < startTimes.length; i++) {
			startTimes[i] = frameTimes.get(i) * 1000;
			System.out.println("startTimes["+i+"]=" + startTimes[i] + "\tdt=" + (startTimes[i] - startTimes[i-1]));
		}
	}
	
	static public abstract class Loader extends VideoAdapter.Loader {

		@Override
		public void saveObject(XMLControl control, Object obj) {
			super.saveObject(control, obj);
		}

		public void setVideo(String path, MovieVideo video, String engine) {
			String ext = XML.getExtension(path);
			VideoType type = VideoIO.getVideoType(engine, ext);
			if (type != null)
				video.setProperty("video_type", type); //$NON-NLS-1$
		}

		@Override
		public Object createObject(XMLControl control) {
			try {
				String fullpath = control.getString("absolutePath");
				if (fullpath != null)
					return createVideo(control, fullpath);
				String path = control.getString("path"); //$NON-NLS-1$
				if (OSPRuntime.checkTempDirCache)
					path = OSPRuntime.tempDir + path;
				return createVideo(path);
			} catch (IOException ex) {
				OSPLog.fine(ex.getMessage());
				return null;
			}
		}

		@Override
		protected Video createVideo(String path) throws IOException {
			// not used.
			return null;
		}

		/**
		 * Implemented in JSMovieVideo and XuggleMovieVideo. 
		 * 
		 * @param control
		 * @param path
		 * @return
		 * @throws IOException
		 */
		abstract protected Object createVideo(XMLControl control, String path) throws IOException;

	}

	protected void startFailDetection() {
		
//		failDetectTimer.start();

	}

	protected void stopFailDetection() {
		
		//		failDetectTimer.stop();
		
	}

}
