package org.opensourcephysics.media.mov;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.Method;
import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.media.core.MediaRes;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * A factory class to manage movie engines
 * @author hansonr
 *
 */
public class MovieFactory {

	public static final String ENGINE_NONE = "none"; //$NON-NLS-1$
	public static final String ENGINE_JS = "JS"; //$NON-NLS-1$
	public static final String ENGINE_XUGGLE = "Xuggle"; //$NON-NLS-1$

	private static String xuggleClassPath = "org.opensourcephysics.media.xuggle."; //$NON-NLS-1$
	private static String xugglePropertiesPath = "org/opensourcephysics/resources/xuggle/xuggle.properties"; //$NON-NLS-1$

	private static String movieEngineName = ENGINE_NONE;

	public static boolean hadXuggleError = false;
	public static boolean xuggleIsPresent = false;
	public static boolean xuggleNeeds32bitVM = false;
	
	/**
	 * Initialize video classes to register their video types
	 * 
	 * JSMovieVideo and XuggleVideo self-register via their static initializers. 
	 * We could call anything here, and it would do the job. The action happens
	 * at the point of "JSMovieVideo." not "JSMovieVideo.registered". Likewise,
	 * for Xuggle, we just need to load the class, not actually do anything with it.
	 */
	static {
		int code = -1;
		try {
			if (OSPRuntime.isJS) {
				if (JSMovieVideoType.registered) {
					// mere request does the job
					movieEngineName = ENGINE_JS;
					xuggleIsPresent = false;
				}
			} else /** @j2sIgnore */ {
				// get xuggle status code by reflection
				Class<?> type = Class.forName(xuggleClassPath + "DiagnosticsForXuggle"); //$NON-NLS-1$
				Method m = type.getMethod("getStatusCode", (Class<?>[])null); //$NON-NLS-1$
				code = (Integer)m.invoke(type, (Object[])null);

				// try to load XuggleVideo class--will register xuggle video types
				Class.forName(xuggleClassPath + "XuggleVideo"); //$NON-NLS-1$
				xuggleIsPresent = true;
				movieEngineName = ENGINE_XUGGLE;
			}
		} catch (Throwable e) {			
			if (!OSPRuntime.isJS) {
				// failed to load xuggle
				if (code == 7) {
					xuggleNeeds32bitVM = true;
					OSPLog.config("Xuggle installed but must be run in a 32-bit Java VM on Windows.");					
				} else {
					OSPLog.config("Xuggle not installed? " + xuggleClassPath + "XuggleVideo failed"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				String jarPath = OSPRuntime.getLaunchJarPath();
				xuggleIsPresent = (jarPath!=null && ResourceLoader.getResource(jarPath+"!/"+xugglePropertiesPath)!=null); //$NON-NLS-1$
			}
		}
	}

	private static PropertyChangeListener[] errorListener = new PropertyChangeListener[] {
		new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				if (e.getPropertyName().equals("xuggle_error")) { //$NON-NLS-1$
					hadXuggleError = true;
					// so noted; remove errorListener
					OSPLog.removePropertyChangeListener(errorListener[0]);
					errorListener[0] = null;
				}
			}
	  }
	};

	static {
	  OSPLog.addPropertyChangeListener(errorListener[0]);
	}
	
	/**
	 * Creates a thumbnail file for a movie video.
	 * 
	 * @param defaultThumbnailDimension desired dimension
	 * @param sourcePath movie video source
	 * @param thumbPath path of the newly created thumbnail
	 * @return the thumbnail file, or null if failed
	 */
	public static File createThumbnailFile(Dimension defaultThumbnailDimension, String sourcePath, String thumbPath) {
		if (hasVideoEngine()) {
			if (OSPRuntime.isJS)
				return JSMovieVideo.createThumbnailFile(defaultThumbnailDimension, sourcePath, thumbPath);

			// video files: use Xuggle thumbnail tool, if available
			String className = xuggleClassPath + "XuggleThumbnailTool"; //$NON-NLS-1$
			Class<?>[] types = new Class<?>[] { Dimension.class, String.class, String.class };
			try {
				Class<?> xuggleClass = Class.forName(className);
				Method method = xuggleClass.getMethod("createThumbnailFile", types); //$NON-NLS-1$
				return (File) method.invoke(null, new Object[] { defaultThumbnailDimension, sourcePath, thumbPath });
			} catch (Exception ex) {
				OSPLog.fine("failed to create thumbnail: " + ex.toString()); //$NON-NLS-1$
			} catch (Error err) {
			}
		}
		return null;
	}

	/**
	 * Gets the name of the current movie engine.
	 * 
	 * @param forDialog if true, returns locale version of "none"
	 * @return "Xuggle" or "JS" or "none" or Locale version of "none"
	 */
	public static String getMovieEngineName(boolean forDialog) {
		return forDialog && movieEngineName == ENGINE_NONE ? 
				MediaRes.getString("VideoIO.Engine.None") :
				movieEngineName;
	}

	/**
	 * Determines if a movie engine is available
	 * 
	 * @return true if available
	 */
	public static boolean hasVideoEngine() {
		return movieEngineName != ENGINE_NONE;
	}

	/**
	 * "Starts" the thumbnail tool (if present) by instantiating it by reflection.
	 */
	public static void startMovieThumbnailTool() {
		if (OSPRuntime.isJS) {
			return; // ?? 
		}
		if (hasVideoEngine()) {
			try {
				Class.forName(xuggleClassPath + "XuggleThumbnailTool"); //$NON-NLS-1$
			} catch (ClassNotFoundException e) {
			}
		}
	}
	
	/**
	 * Updates video engine files if available
	 * 
	 * @return array of video engine names that were updated. May return null.
	 */
	public static String[] getUpdatedVideoEngines() {
		if (!OSPRuntime.isJS) /** @j2sNative */ {	
			if (movieEngineName.equals(ENGINE_XUGGLE)) {
				// copy xuggle files to codebase
				try {
					String codebase = OSPRuntime.getLaunchJarDirectory();
					if (codebase != null) {
						// call DiagnosticsForXuggle.copyXuggleJarsTo by reflection
						Class<?> clas = Class.forName("org.opensourcephysics.media.xuggle.DiagnosticsForXuggle"); //$NON-NLS-1$
						Method method = clas.getMethod("copyXuggleJarsTo", new Class[] {File.class}); //$NON-NLS-1$
						Object result = method.invoke(null, new Object[] {new File(codebase)});
						if ((Boolean)result) return new String[] {ENGINE_XUGGLE};
					}		
				} catch (Exception e) {}
			}
		}
		return null;
	}
	
	/**
	 * Show an About dialog for a video engine
	 * 
	 * @param engineName one of the allowed engine names
	 * @param requester name of a requestor. May be null.
	 */
	public static void showAbout(String engineName, String requester) {
		if (engineName==ENGINE_XUGGLE) {
			// call DiagnosticsForXuggle.aboutXuggle by reflection
			try {
				Class<?> clas = Class.forName("org.opensourcephysics.media.xuggle.DiagnosticsForXuggle"); //$NON-NLS-1$
				Method method = clas.getMethod("aboutXuggle", new Class[] {String.class}); //$NON-NLS-1$
				method.invoke(null, new Object[] {requester});
			} catch (Exception e1) {}
		}
	}

}
