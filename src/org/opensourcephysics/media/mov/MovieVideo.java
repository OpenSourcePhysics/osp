package org.opensourcephysics.media.mov;

import java.awt.Frame;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

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
	 * this.getDuration().
	 */
	protected double rawDuration;
	protected int frameRate;
	protected boolean isExport;
	protected String path;
	protected boolean isLocal;
	protected String platform;

	protected double[] rawStartTimes;

	protected int rawFrameCount;

	protected XMLControl control;


	// load,save for video frames would be here

//	private final Timer failDetectTimer;

	public MovieVideo(String fileName, String basePath, XMLControl control) throws IOException {
		this.fileName = fileName;
		platform = /** @j2sNative navigator.userAgent || "?" || */
				"Java";
		boolean isJava = (platform == "Java");
		allowControlData = true;
		addFramePropertyListeners();
		isExport = (control != null && !"video".equals(control.getPropertyName()));
		baseDir = basePath; // null for Xuggle
		path = getAbsolutePath(fileName);
		Resource res = (isJava ? ResourceLoader.getResource(fileName) : ResourceLoader.isHTTP(path) ? new Resource(new URL(path)) : new Resource(new File(path)));
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
	 * @param time the time in milliseconds
	 * @return the frame number, or -1 if not found
	 */
	protected int getFrameNumberBefore(double time) {
		for (int i = 0; i < startTimes.length; i++) {
			if (time < startTimes[i]) {
				return i - 1;
			}
		}
		return (time < rawDuration * 1000 ? startTimes.length - 1 : -1);
	}

	/**
	 * Gets the number of the last frame before the specified time for the "raw"
	 * start times - the ones for this engine, not the ones from controls.
	 *
	 * @param time the time in milliseconds
	 * @return the frame number, or -1 if not found
	 */
	protected int getRawFrameNumberBefore(double time) {
		if (rawStartTimes == null)
			return getFrameNumberBefore(time);
		for (int i = 0; i < rawStartTimes.length; i++) {
			if (time < rawStartTimes[i]) {
				return i - 1;
			}
		}
		return (time < rawDuration * 1000 ? rawStartTimes.length - 1 : -1);
	}

	@Override
	protected void setStartTimes() {
		if (startTimes == null) {
			startTimes = new double[frameCount];
			startTimes[0] = 0;
			for (int i = 1; i < startTimes.length; i++) {
				startTimes[i] = frameTimes.get(i) * 1000;
				System.out.println(
						"startTimes[" + i + "]=" + startTimes[i] + "\tdt=" + (startTimes[i] - startTimes[i - 1]));
			}
		}
		System.out.println("MovieVideo.setStartTimes rawDuration=" + rawDuration + " frameCount=" + frameCount);
	}

	/**
	 * Set frameCount, startTimes, and rawDuration if available from the control.
	 * 
	 * @param control
	 * @return null if cannot be set (older versions)
	 */
	public XMLControl setFromControl(XMLControl control) {
		int count = control.getInt("frame_count");
		if (count == Integer.MIN_VALUE)
			return null;
		frameCount = count;
		startTimes = (double[]) control.getObject("start_times");
		rawDuration = control.getDouble("duration");
		frameRate = control.getInt("frame_rate");
		platform = control.getString("platform");
		System.out.println(
				"MovieVideo.setFromControl from " + platform + " " + rawDuration + " " + frameCount + " " + frameRate);
		return control;
	}

	static public abstract class Loader extends VideoAdapter.Loader {

		@Override
		public void saveObject(XMLControl control, Object obj) {
			super.saveObject(control, obj);
			MovieVideo vid = (MovieVideo) obj;
			control.setValue("start_times", vid.startTimes); // in milliseconds
			double rawDuration = vid.rawDuration; // in seconds
			control.setValue("duration", rawDuration); // convert to seconds
			int fc = vid.frameCount;
			control.setValue("frame_count", fc);
			int fps = (int) Math.round(fc / rawDuration);
			control.setValue("frame_rate", fps);
			control.setValue("platform", vid.platform);
			System.out.println("MovieVideo.save " + vid.platform + " " + rawDuration + " " + fc + " " + fps);
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

	protected void startFailDetection() {

//		failDetectTimer.start();

	}

	protected void stopFailDetection() {

		// failDetectTimer.stop();

	}

}
