package org.opensourcephysics.media.mov;

import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.media.core.Video;
import org.opensourcephysics.media.core.VideoAdapter;
import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.media.core.VideoType;
import org.opensourcephysics.tools.Resource;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * An abstract class that describes common aspects of VideoAdapters that involve
 * actual digital video sources such as MP4 (JSMovieVideo and XuggleVideo).
 * 
 * (formerly MovieVideoI interface)
 * 
 * @author hansonr
 *
 */
public abstract class MovieVideo extends VideoAdapter {

	public final static String PROPERTY_VIDEO_PROGRESS = "progress"; //$NON-NLS-1$ // see TFrame
	public final static String PROPERTY_VIDEO_STALLED = "stalled"; //$NON-NLS-1$ // see TFrame
	
	protected static final String PLATFORM_JAVA = "Java";

	protected static final double TIME_SLOP_MS = 0.1;

	protected String fileName;
	protected URL url;

	/**
	 * These are temporary only, during loading. see startTimes
	 */
	protected ArrayList<Double> frameTimes;

	/**
	 * set true to read control data start_times, duration (for rawDuration), and
	 * frame_count; false to instead scan video again
	 * 
	 */
	protected boolean allowControlData;

	/**
	 * the duration reported by the video, possibly longer for Xuggle than
	 * this.getDuration(); in seconds
	 */
	protected double rawDuration;

	/**
	 * nominal rounded frame rate -- 24, 25, 30, 50, 60...; used only for offset to center of frames
	 */
	protected int nominalFrameRate;

	/**
	 * the source platform for this video, NOT necessarily "this" platform; use getPlatform() for that.
	 * "Java" or navigator.userAgent; by default, null;
	 * 
	 * 
	 */
	private String sourcePlatform;

	protected String path;

	protected boolean isLocal;
	protected boolean isExport;

	/**
	 * frame count by this engine's calculation; may be different from
	 * frameCount, which could be coming from another creator via a 
	 * video XMLControlElement
	 */
	protected int rawFrameCount;

	/**
	 * the originating video XMLControlElement, if it exists 
	 */
	protected XMLControl control;
	protected boolean isVariableFrameRate;
	protected double frameRateMinimum;
	protected double frameRateMaximum;


//	private final Timer failDetectTimer;

	/**
	 * Get this platform's name
	 * 
	 * @return "Java" or navigator.userAgent
	 */
	abstract protected String getPlatform();

	/**
	 * seek to a time in milliseconds.
	 * 
	 * @param timeMS
	 * @return
	 */
	abstract protected boolean seekMS(double timeMS);
	
	abstract protected void finalizeLoading() throws IOException;

	abstract protected BufferedImage getImageForMSTimePoint(double timeSec);

	public MovieVideo(String fileName, String basePath, XMLControl control) throws IOException {
		this.fileName = fileName;
		boolean isJava = (getPlatform() == PLATFORM_JAVA);
		allowControlData = true;
		addFramePropertyListeners();
		isExport = (control != null && !"video".equals(control.getPropertyName()));
		baseDir = basePath; // null for Xuggle
		path = getAbsolutePath(fileName);
		Resource res = (isJava ? ResourceLoader.getResource(fileName)
				: ResourceLoader.isHTTP(path) ? new Resource(new URL(path)) : new Resource(new File(path)));
		if (res == null)
			throw new IOException("unable to create resource for " + fileName); //$NON-NLS-1$
		url = res.getURL();
		isLocal = (url.getProtocol().toLowerCase().indexOf("file") >= 0);
		path = isLocal ? res.getAbsolutePath() : url.toExternalForm();

		// set properties
		setProperty("name", XML.getName(fileName)); //$NON-NLS-1$
		setProperty("absolutePath", res.getAbsolutePath()); //$NON-NLS-1$
		if (fileName.indexOf(":") < 0) { //$NON-NLS-1$
			// if name is relative, path is name
			setProperty("path", XML.forwardSlash(fileName)); //$NON-NLS-1$
		} else if (!isJava) {
			// else path is relative to user directory
			setProperty("path", XML.getRelativePath(fileName)); //$NON-NLS-1$
		} else if (fileName.contains("!/")) {
			// else path is relative to parent directory of TRZ/ZIP
			String dir = fileName.substring(0, fileName.indexOf("!/"));
			dir = XML.getDirectoryPath(dir);
			setProperty("path", XML.getPathRelativeTo(fileName, dir)); //$NON-NLS-1$
		} else {
			// else path is absolute
			setProperty("path", res.getAbsolutePath()); //$NON-NLS-1$
		}
	}

	@Override
	protected void setStartTimes() {
		if (startTimesMS == null) {
			startTimesMS = new double[frameCount];
			startTimesMS[0] = 0;
			for (int i = 1; i < startTimesMS.length; i++) {
				startTimesMS[i] = frameTimes.get(i) * 1000;
				//System.out.println("startTimes[" + i + "]=" + startTimes[i] + "\tdt=" + (startTimes[i] - startTimes[i - 1]));
			}
		}
		//System.out.println("MovieVideo.setStartTimes rawDuration=" + rawDuration + " frameCount=" + frameCount);
	}

	private void addFramePropertyListeners() {
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

	/**
	 * Gets the number of the last frame before the specified time.
	 *
	 * a 0.1-ms slop is added 
	 * 
	 * @param time the time in milliseconds
	 * @return the frame number, or -1 if not found
	 */
	protected int getFrameNumberBefore(double time) {
		time += TIME_SLOP_MS;
		for (int i = 0; i < startTimesMS.length; i++) {
			if (time < startTimesMS[i]) {
				return i - 1;
			}
		}
		return (time < rawDuration * 1000 ? startTimesMS.length - 1 : -1);
	}

	/**
	 * Set frameCount, startTimes, and rawDuration if available from the control.
	 * 
	 * Ignore if the saved platform string is "Java" and 
	 * 
	 * @param control
	 * @return null if cannot be set (older versions)
	 */
	protected XMLControl setFromControl(XMLControl control) {
		int count = control.getInt("frame_count");
		if (count == Integer.MIN_VALUE)
			return null;
		String platform = control.getString("platform");
		if (!OSPRuntime.isJS && platform.equals(PLATFORM_JAVA)) {
			//System.out.println("MovieVideo.setFromControl ignored; platform=" + getPlatform());
			return null;
		}
		sourcePlatform = (platform == null ? "unknown" : platform);
		frameCount = count;
		startTimesMS = (double[]) control.getObject("start_times");
		rawDuration = control.getDouble("duration");
		nominalFrameRate = control.getInt("frame_rate");
		//System.out.println("MovieVideo.setFromControl from " + sourcePlatform + " " + rawDuration + " " + frameCount + " " + frameRate);
		return control;
	}

	/**
	 * JavaScript only -- use MediaInfo for rate information
	 * 
	 * 
	 * @param info
	 * @return error string or null if no error
	 */
	protected String setFromMediaTrackInfo(Map<String, Object> info) {
		// this will be the media.track[] entry for @type == "Video"
		String key = null;
		try {
			double duration = Double.parseDouble((String) info.get(key = "Duration"));
			double precisionFrameRate = Double.parseDouble((String) info.get(key = "FrameRate"));
			String frameRateMode = (String) info.get(key = "FrameRate_Mode");
			if ("VFR".equals(frameRateMode)) {
				isVariableFrameRate = true;
				frameRateMinimum = Double.parseDouble((String) info.get(key = "FrameRate_Minimum"));
				frameRateMaximum = Double.parseDouble((String) info.get(key = "FrameRate_Maximum"));
			}
			frameCount = Integer.parseInt((String) info.get(key = "FrameCount"));
			nominalFrameRate = (int) Math.round(precisionFrameRate);
			rawDuration = duration;
			startTimesMS = new double[frameCount];
			for (int i = 0; i < frameCount; i++) {
				startTimesMS[i] = i * 1000 / precisionFrameRate;
			}
			System.out.println("MovieVideo: found " + frameRateMode + " FrameRate " + precisionFrameRate
					+ " FrameCount " + frameCount + " Duration " + rawDuration + " ("
					+ (precisionFrameRate * frameCount) + " calc)");
			if (isVariableFrameRate)
				System.out.println(
						"MovieVideo: Variable frame rate " + frameRateMinimum + " - " + frameRateMaximum);
			//$FALL-THROUGH$
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return "MovieVideo MediaInfo parse error for " + key + ": " + e.getMessage();
		}
	}


	static public abstract class Loader extends VideoAdapter.Loader {

		@Override
		public void saveObject(XMLControl control, Object obj) {
			super.saveObject(control, obj);
			MovieVideo vid = (MovieVideo) obj;
			control.setValue("start_times", vid.startTimesMS, 3); // in milliseconds
			double rawDuration = vid.rawDuration; // in seconds
			control.setValue("duration", rawDuration); // convert to seconds
			int fc = vid.frameCount;
			control.setValue("frame_count", fc);
			int fps = (int) Math.round(fc / rawDuration);
			control.setValue("frame_rate", fps);
			String platform = (vid.sourcePlatform == null ? vid.getPlatform() : vid.sourcePlatform);
			control.setValue("platform", platform);
			//System.out.println("MovieVideo.save " + platform + " " + rawDuration + " " + fc + " " + fps);
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
				return createVideo(control, path);
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

//	protected void startFailDetection() {
//
//		failDetectTimer.start();
//
//	}
//
//	protected void stopFailDetection() {
//
//		failDetectTimer.stop();
//
//	}
//
}
