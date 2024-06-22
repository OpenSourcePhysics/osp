/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JApplet;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.SwingPropertyChangeSupport;
import javax.swing.plaf.metal.MetalLookAndFeel;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.tools.FontSizer;
import org.opensourcephysics.tools.LaunchNode;
import org.opensourcephysics.tools.ResourceLoader;
import org.opensourcephysics.tools.Translator;
import org.opensourcephysics.tools.TranslatorTool;

import javajs.async.Assets;
import javajs.async.AsyncFileChooser;
import javajs.async.SwingJSUtils.Timeout;
import swingjs.api.JSUtilI;

/**
 * This class defines static methods related to the runtime environment.
 *
 * @author Douglas Brown
 * @author Wolfgang Christian
 * @author Robert M. Hanson
 * @version 1.0
 */
public class OSPRuntime {

	public static final String VERSION = "6.1.7.240621"; //$NON-NLS-1$
	public static final String RELEASE_DATE = "21 Jun 2024"; //$NON-NLS-1$
	public static final String OSP_PROPERTY_LOCALE = "locale";

	/**
	 * An interface with static methods that track implementing classes, adding them
	 * to array to "allocate" them, and running their dispose() method when
	 * "deallocation" is requested.
	 * 
	 * @author hanson
	 *
	 */
	public interface Disposable {

		public void dispose();

		static List<Object> allocated = new ArrayList<>();

		static void allocate(Disposable obj) {
			if (allocated.contains(obj))
				return;
			allocated.add(obj);
			OSPLog.notify(obj, "allocated");
		}

		static void allocate(Disposable[] objs, String name) {
			allocated.add(objs);
			OSPLog.notify(name + "[]", "allocated");
		}

		static void deallocate(Disposable[] objs) {
			for (Disposable o : (Disposable[]) objs) {
				if (o != null)
					deallocate(o);
			}
		}

		static void deallocate(Disposable[] objs, int i) {
			Disposable o = objs[i];
			if (o != null) {
				deallocate(o);
				objs[i] = null;
			}
		}

		static void deallocate(Disposable[] objs, BitSet bs) {
			for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
				deallocate(objs, i);
			}
		}

		static void deallocate(Disposable obj) {
			if (obj == null)
				return;
			obj.dispose();
			allocated.remove(obj);
			OSPLog.notify(obj, " deallocated ");
		}

		static void clearAllocation(Disposable obj) {
			if (obj == null)
				return;
			allocated.remove(obj);
			OSPLog.notify(obj, " deallocated ");
		}

		public static void deallocateAll() {
			for (Object o : allocated) {
				if (o instanceof Disposable) {
					((Disposable) o).dispose();
					OSPLog.notify(o, " deallocated ");
				} else if (o instanceof Disposable[]){
					for (Disposable ao : (Disposable[]) o) {
						if (ao != null)
							deallocate(ao);
					}
				} else {
					OSPLog.notify(o, "deallocated");
				}
			}
			allocated.clear();
		}

		static void dump() {
			int n = 0;
			for (Object o : allocated) {
				if (o instanceof Disposable[]){
					for (Disposable ao : (Disposable[]) o) {
						if (ao != null && ++n > 0)
							OSPLog.notify(ao, " still allocated!");
					}
				} else {
					++n;
					OSPLog.notify(o, "still allocated!");
				}
			}
			OSPLog.notify("" + n, "objects still allocated" + (n == 0 ? ""  : "!!!!!!!!!!!!"));
		}

	}

	public static abstract class Supported {

		private static boolean debugging = false;

		private PropertyChangeSupport support;

		public Supported() {
			support = new SwingPropertyChangeSupport(this);
		}

		/**
		 * Fires a property change event.
		 *
		 */
		public void firePropertyChange(PropertyChangeEvent e) {
			support.firePropertyChange(e);
		}

		/**
		 * Fires a property change event.
		 *
		 * @param name   the name of the property
		 * @param oldVal the old value of the property
		 * @param newVal the new value of the property
		 */
		public void firePropertyChange(String name, Object oldVal, Object newVal) {
			support.firePropertyChange(name, oldVal, newVal);
		}

		HashSet<String> pointers = new HashSet<String>();

		private void addPtr(String key) {
			boolean b = pointers.add(key);
			if (debugging)
				System.out.println(this.getClass().getSimpleName() + key + " ADD " + b);
		}

		private void removePtr(String key) {
			boolean b = pointers.remove(key);
			if (debugging)
				System.out.println(this.getClass().getSimpleName() + key + " REM " + b + " " + pointers.size()
						+ (pointers.size() == 1 ? pointers.toString() : ""));
		}

		/**
		 * Adds a PropertyChangeListener to this video clip.
		 *
		 * @param listener the object requesting property change notification
		 */
		public void addPropertyChangeListener(PropertyChangeListener listener) {
			String key = "<-" + listener.getClass().getSimpleName() + listener.hashCode();
			if (pointers.contains(key))
				return;
			addPtr(key);
			support.addPropertyChangeListener(listener);
		}

		public void addPropertyChangeListenerSafely(PropertyChangeListener listener) {
//			support.removePropertyChangeListener(listener);
			support.addPropertyChangeListener(listener);
		}

		/**
		 * Adds a PropertyChangeListener to this video clip.
		 *
		 * @param property the name of the property of interest to the listener
		 * @param listener the object requesting property change notification
		 */
		public void addPropertyChangeListener(String property, PropertyChangeListener listener) {
			String key = "/" + property + "<-" + listener.getClass().getSimpleName() + listener.hashCode();
			if (pointers.contains(key))
				return;
			addPtr(key);
			support.addPropertyChangeListener(property, listener);
		}

		/**
		 * Removes a PropertyChangeListener from this video clip.
		 *
		 * @param listener the listener requesting removal
		 */
		public void removePropertyChangeListener(PropertyChangeListener listener) {
			String key = "<-" + listener.getClass().getSimpleName() + listener.hashCode();
			if (!pointers.contains(key))
				return;
			removePtr(key);
			support.removePropertyChangeListener(listener);
		}

		/**
		 * Removes a PropertyChangeListener for a specified property.
		 *
		 * @param property the name of the property
		 * @param listener the listener to remove
		 */
		public void removePropertyChangeListener(String property, PropertyChangeListener listener) {
			if (listener == null)
				return;
			String key = "/" + property + "<-" + listener.getClass().getSimpleName() + listener.hashCode();
			if (!pointers.contains(key))
				return;
			removePtr(key);
			support.removePropertyChangeListener(property, listener);
		}

		public static void addListeners(Supported c, String[] names, PropertyChangeListener listener) {
			for (int i = names.length; --i >= 0;)
				c.addPropertyChangeListener(names[i], listener);
		}

		public static void removeListeners(Supported c, String[] names, PropertyChangeListener listener) {
			for (int i = names.length; --i >= 0;)
				c.removePropertyChangeListener(names[i], listener);
		}

		public void dispose() {
			PropertyChangeListener[] a = support.getPropertyChangeListeners();
			if (debugging)
				System.out.println(this.getClass().getSimpleName() + "------------" + a.length);
			for (int i = a.length; --i >= 0;) {
				PropertyChangeListener p = a[i];
				if (p instanceof PropertyChangeListenerProxy) {
					String prop = ((PropertyChangeListenerProxy) p).getPropertyName();
					p = ((PropertyChangeListenerProxy) p).getListener();
					removePropertyChangeListener(prop, p);
				} else {
					removePropertyChangeListener(p);
				}
			}
		}

		public static void dispose(Component c) {
			PropertyChangeListener[] a = c.getPropertyChangeListeners();
			if (debugging)
				System.out.println(c.getClass().getSimpleName() + "------------" + a.length);
			for (int i = a.length; --i >= 0;) {
				PropertyChangeListener p = a[i];
				if (p instanceof PropertyChangeListenerProxy) {
					String prop = ((PropertyChangeListenerProxy) p).getPropertyName();
					p = ((PropertyChangeListenerProxy) p).getListener();
					if (debugging)
						System.out.println(c.getClass().getSimpleName() + "/" + prop + "---remove "
								+ p.getClass().getSimpleName());
					c.removePropertyChangeListener(prop, p);
				} else {
					if (debugging)
						System.out.println(c.getClass().getSimpleName() + "---remove " + p.getClass().getSimpleName());
					c.removePropertyChangeListener(p);
					if (c instanceof PropertyChangeListener) {
						if (p instanceof Component)
							((Component) p).removePropertyChangeListener((PropertyChangeListener) c);
						else if (p instanceof Supported)
							((Supported) p).removePropertyChangeListener((PropertyChangeListener) c);
					}
				}
			}

		}
//
// add these to any class to quickly see what is going on
//		public void addPropertyChangeListener(PropertyChangeListener l) {
//			super.addPropertyChangeListener(l);
//		}
//
//		public void addPropertyChangeListener(String prop, PropertyChangeListener l) {
//			super.addPropertyChangeListener(prop, l);		
//		}
//		public void removePropertyChangeListener(PropertyChangeListener l) {
//			super.removePropertyChangeListener(l);
//		}		
//
//		public void removePropertyChangeListener(String prop, PropertyChangeListener l) {
//			super.removePropertyChangeListener(prop, l);
//		
//		}
//

	}

	/**
	 * A class to compare version strings.
	 */
	public static class Version implements Comparable<Version> {
		String ver;

		/**
		 * Constructor
		 * 
		 * @param version the version string
		 */
		public Version(String version) {
			ver = version;
		}

		@Override
		public String toString() {
			return ver;
		}
		
		public boolean isValid() {
			String[] v = this.ver.trim().split("\\."); //$NON-NLS-1$
			if (v.length >= 2 && v.length <= 4) {
				for (int i = 0; i < v.length; i++) {
					try {
						Integer.parseInt(v[i].trim());
					} catch (Exception ex) {
						return false;
					}
				}
				return true;
			}
			return false;
		}

		@Override
		public int compareTo(Version o) {
			// typical current versions (length 3-4): "6.1.2" or "6.1.2.220213"
			// typical older version (length 2): "4.97"

			// split at decimal points
			String[] v1 = this.ver.trim().split("\\."); //$NON-NLS-1$
			String[] v2 = o.ver.trim().split("\\."); //$NON-NLS-1$
			
			// pad versions to length 4	
			if (v1.length == 2)
				v1 = new String[] { v1[0], v1[1], "0", "0" };
			else if (v1.length == 3)
				v1 = new String[] { v1[0], v1[1], v1[2], "0" };
			
			if (v2.length == 2)
				v2 = new String[] { v2[0], v2[1], "0", "0" };
			else if (v2.length == 3)
				v2 = new String[] { v2[0], v2[1], v2[2], "0" };
			
			for (int i = 0; i < v1.length; i++) {
				if (Integer.parseInt(v1[i]) < Integer.parseInt(v2[i])) {
					return -1;
				} else if (Integer.parseInt(v1[i]) > Integer.parseInt(v2[i])) {
					return 1;
				}
			}
			return 0;
		}
	}

	private static boolean isMac;
	

	public static int macOffset; // shifts LR message box on Mac to avoid drag hot spot.

	// BH note: Cannot use "final" here because then the constant will be set before
	// the transpiler ever sees it.
	public static boolean isJS = /** @j2sNative true || */
			false;
	
	// browser Navigator parameters added by WC
//	private static boolean isOSX=false;
//	private static boolean isiOS=false;
//	private static boolean isiPad=false;
//	private static boolean isAndroid=false;	
//  private static boolean isMobile=false;
  private static String userAgent="";	//navigator user agent
  //skip loading for testing mobile devices
	private static boolean skipDisplayOfPDF = false;// isMobile;// true;// isJS; // for TrackerIO, for now.

	
	private static void readMobileParam(){
		/** @j2sNative
		 * this.userAgent=navigator.userAgent;
		 * this.isOSX=this.userAgent.match('OS X')!=null;
		 * this.isiOS= this.userAgent.match('iOS')!=null;
		 * this.isiPad= this.userAgent.match('iPad')!=null;
		 * var touchpoints= navigator.maxTouchPoints;
		 * var isiPadPro= (touchpoints>2 && this.isOSX);
		 * this.isiPad =  this.isiPad || isiPadPro;
		 * this.isAndroid= this.userAgent.match('Android')!=null;
		 * this.isMobile=this.isiOS||this.isiPad||this.isAndroid;
		 * this.skipDisplayOfPDF=this.isMobile;
		 */
	 }
	
	public static boolean getSkipDisplayOfPDF() {
		if(isJS)readMobileParam();
		return skipDisplayOfPDF;
	}
	
	public static String getUserAgent() {
		if(isJS)readMobileParam();	
		return userAgent;  // user agent should already be set.
	}

	static {
		/** @j2sNative
		 * window.addEventListener("online", function(){alert("Internet reconnected.")});
		 * window.addEventListener("offline", function(){alert("Internet Disconnected.")})
		 *  
		 */
	}
	
	static {
		try {
			// system properties may not be readable in some environments
			isMac = (/** @j2sNative 1 ? false : */
			(System.getProperty("os.name", "").toLowerCase().startsWith("mac"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			macOffset = (isMac && !isJS) ? 16 : 0;
		} catch (SecurityException ex) {
		}
	}

	public static JSUtilI jsutil;

	static {
		try {
			if (isJS) {
				jsutil = ((JSUtilI) Class.forName("swingjs.JSUtil").newInstance());

				jsutil.addDirectDatabaseCall("."); // Assume ALL sites are CORS enabled
//				// note - this is only for https
//				String[] corsEnabled = new String[] { "www.physlets.org", "https://physlets.org",
//						// "www.opensourcephysics.org",
//						"www.compadre.org/osp", "www.compadre.org/profiles" };
//				for (int i = corsEnabled.length; --i >= 0;) {
//					jsutil.addDirectDatabaseCall(corsEnabled[i]);
//				}
			}

		} catch (Exception e) {
			OSPLog.warning("OSPRuntime could not create jsutil");
		}
	}

//	private static String browser = (isJS ? null : "JAVA"); 

	public static String getBrowserName() {
		String sUsrAg = /** @j2sNative navigator.userAgent || */
				"";
		String sBrowser;
		// from https://developer.mozilla.org/en-US/docs/Web/API/Window/navigator

		// The order matters here, and this may report false positives for unlisted
		// browsers.

		if (sUsrAg.indexOf("Firefox") > -1) {
			sBrowser = "Mozilla Firefox";
			// "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:61.0) Gecko/20100101
			// Firefox/61.0"
		} else if (sUsrAg.indexOf("SamsungBrowser") > -1) {
			sBrowser = "Samsung Internet";
			// "Mozilla/5.0 (Linux; Android 9; SAMSUNG SM-G955F Build/PPR1.180610.011)
			// AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/9.4 Chrome/67.0.3396.87
			// Mobile Safari/537.36
		} else if (sUsrAg.indexOf("Opera") > -1 || sUsrAg.indexOf("OPR") > -1) {
			sBrowser = "Opera";
			// "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_0) AppleWebKit/537.36 (KHTML,
			// like Gecko) Chrome/70.0.3538.102 Safari/537.36 OPR/57.0.3098.106"
		} else if (sUsrAg.indexOf("Trident") > -1) {
			sBrowser = "Microsoft Internet Explorer";
			// "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; .NET4.0C; .NET4.0E; Zoom
			// 3.6.0; wbx 1.0.0; rv:11.0) like Gecko"
		} else if (sUsrAg.indexOf("Edge") > -1) {
			sBrowser = "Microsoft Edge";
			// "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like
			// Gecko) Chrome/58.0.3029.110 Safari/537.36 Edge/16.16299"
		} else if (sUsrAg.indexOf("Chrome") > -1) {
			sBrowser = "Google Chrome or Chromium";
			// "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko)
			// Ubuntu Chromium/66.0.3359.181 Chrome/66.0.3359.181 Safari/537.36"
		} else if (sUsrAg.indexOf("Safari") > -1) {
			sBrowser = "Apple Safari";
			// "Mozilla/5.0 (iPhone; CPU iPhone OS 11_4 like Mac OS X) AppleWebKit/605.1.15
			// (KHTML, like Gecko) Version/11.0 Mobile/15E148 Safari/604.1 980x1306"
		} else {
			sBrowser = "unknown";
		}
		return sBrowser;
	}
	
	public static final char DECIMAL_SEPARATOR_COMMA = ',';
	public static final char DECIMAL_SEPARATOR_PERIOD = '.';

	public static boolean isApplet = false;

//	public static boolean useSearchMap = isJS; // does not cache 

	public static boolean isBHTest = isJS;

	public static boolean dontLog = isJS; // for OSPLog

	public static boolean allowBackgroundNodeLoading = false; // LibraryTreePanel - do background node loading

	public static boolean allowAutopaste = !isJS; // for TFrame and TToolbar

	/**
	 * HighlightDataSet -- Firefox has problems with canvas clip/unclip getting
	 * slower and slower and slower
	 */
	public static boolean allowDatasetClip = (getBrowserName() != "Mozilla Firefox");

	public static boolean allowLibClipboardPasteCheck = !isJS;

	public static boolean allowSetFonts = true;// !isBHTest; // for testing

	public static boolean allowAsyncURL = isJS; // for OSPRuntime and Library SwingWorkers

	/** Load Translator Tool, if available. */
	public static boolean loadTranslatorTool = !isJS; // OSPControl

	public static boolean autoAddLibrary = !isJS; // for TrackerIO

	public static final boolean checkImages = false; // LibraryTreeNode

	public static boolean checkTempDirCache = isJS; // for ResourceLoader.

	public static boolean checkZipLoaders = !isJS; // for ResourceLloader

	public static boolean doCacheThumbnail = !isJS;

	public static boolean doCacheLibaryRecord = !isJS;

	public static boolean doCacheZipContents = true;// isJS; // for ResourceLoader

	public static boolean doScrollToPath = false; // testing problem with scrolling

	public static boolean drawDontFillAxes = true; // isJS for CoordAxesStep -- don't understand
	// why this is not working now in Java; never worked in JavaScript

	public static boolean logToJ2SMonitor = isJS; // for OSPRuntime

	public static boolean resCacheEnabled = isJS; // for ResourceLoader

	public static boolean setRenderingHints = (!isJS && !isMac);

	public static boolean embedVideoAsObject = isJS;

	public static boolean useZipAssets = isJS;

	public static boolean unzipFiles = !isJS; // for TrackerIO

	static {
		// Assets.setDebugging(true);
		addAssets("osp", "osp-assets.zip", "org/opensourcephysics/resources");
		if (!isJS && !unzipFiles)
			OSPLog.warning("OSPRuntime.unzipFiles setting is false for BH testing");
		if (OSPRuntime.skipDisplayOfPDF) {
			OSPLog.warning("OSPRuntime.skipDisplayOfPDF true for BH testing");
		}
	}
	public static final String tempDir = System.getProperty("java.io.tmpdir"); //$NON-NLS-1$ // BH centralized

	/**
	 * BH AsyncFileChooser extends JFileChooser, so all of the methods of
	 * JFileChooser are still available. In particular, the SAVE action needs no
	 * changes. But File reading requires asynchronous action in SwingJS.
	 * 
	 */
	private static AsyncFileChooser chooser;

	public final static int WEB_CONNECTED_TEST_JAVA_TIMEOUT_MS = 1000;
	public final static int WEB_CONNECTED_TEST_JS_TIMEOUT_MS = 1000;
	public final static String WEB_CONNECTED_TEST_URL = "https://www.compadre.org/osp/services/REST/osp_tracker.cfm?verb=Identify";

	// BH test option public final static String WEB_CONNECTED_TEST_URL =
	// "https://cactus.nci.nih.gov/chemical/structure/caffeine/file?format=sdf&get3d=true";

	/**
	 * Disables drawing for faster start-up and to avoid screen flash in Drawing
	 * Panels.
	 */
	volatile public static boolean disableAllDrawing = false;

	/** Load Video Tool, if available. */
	public static boolean loadVideoTool = true;

	/** Load Export Tool, if available. */
	public static boolean loadExportTool = true;

	/** Load Data Tool, if available. */
	public static boolean loadDataTool = true;

	/** Load OSP Log, if available. */
	public static boolean loadOSPLog = true;

	/** Storage of separator value */
	private static DecimalFormatSymbols dfs = new DecimalFormatSymbols();

	/** Array of default OSP Locales. */
	public static Locale[] defaultLocales = new Locale[] { Locale.ENGLISH, new Locale("es"), new Locale("de"), //$NON-NLS-1$ //$NON-NLS-2$
			new Locale("da"), new Locale("sk"), Locale.TAIWAN }; //$NON-NLS-1$ //$NON-NLS-2$

	/** Set <I>true</I> if a program is being run within Launcher. */
	protected static boolean launcherMode = false;

	/** True if text components should try and anti-alias text. */
	public static boolean antiAliasText = false;

	/** True if running as an applet. */
	public static boolean appletMode;

	/** Static reference to an applet for document/code base access. */
	public static JApplet applet;

	public static void setApplet(JApplet a) {
		applet = a;
		isApplet = true;
	}

	/** True if launched by WebStart. */
	public static boolean webStart;

	/** True if launched by WebStart. */
//  public static boolean J3D;

	/**
	 * True if users allowed to author internal parameters such as Locale strings.
	 */
	protected static boolean authorMode = true;

	/** Path of the launch jar, if any. */
	private static String launchJarPath;

	/** Path of the launch jar, if any. */
	private static String launchJarName;

	/** The launch jar, if any. */
	private static JarFile launchJar = null;

	/** Build date of the launch jar, if known. */
	private static String buildDate;

	/** The default decimal separator */
	private static char defaultDecimalSeparator;

	/** The preferred decimal separator, if any */
	private static String preferredDecimalSeparator = null;
	
	/** Minus sign, may be used instead of default hyphen-minus */
	public static final char MINUS = '\u2212'; //$NON-NLS-1$

	/** File Chooser starting directory. */
	public static String chooserDir;

	/** User home directory. */
	public static String userhomeDir;

	/** Location of OSP icon. */
	public static final String OSP_ICON_FILE = "/org/opensourcephysics/resources/controls/images/osp_icon.gif"; //$NON-NLS-1$

	/** True if always launching in single vm (applet mode, etc). */
	public static boolean launchingInSingleVM;
	// look and feel types
	@SuppressWarnings("javadoc")
	public final static String CROSS_PLATFORM_LF = "CROSS_PLATFORM"; //$NON-NLS-1$
	@SuppressWarnings("javadoc")
	public final static String NIMBUS_LF = "NIMBUS"; //$NON-NLS-1$
	@SuppressWarnings("javadoc")
	public final static String SYSTEM_LF = "SYSTEM"; //$NON-NLS-1$
	@SuppressWarnings("javadoc")
	public final static String METAL_LF = "METAL"; //$NON-NLS-1$
	public final static String GTK_LF = "GTK"; //$NON-NLS-1$
	public final static String MOTIF_LF = "MOTIF"; //$NON-NLS-1$
	public final static String WINDOWS_LF = "WINDOWS"; //$NON-NLS-1$
	public final static String DEFAULT_LF = "DEFAULT"; //$NON-NLS-1$
	public final static LookAndFeel DEFAULT_LOOK_AND_FEEL = UIManager.getLookAndFeel(); // save the default before we //
																						// change LnF
	public final static boolean DEFAULT_LOOK_AND_FEEL_DECORATIONS = JFrame.isDefaultLookAndFeelDecorated();
	public final static HashMap<String, String> LOOK_AND_FEEL_TYPES = new HashMap<String, String>();

	public static final String PROPERTY_ERROR_OUTOFMEMORY = "error";

	/** Preferences XML control */
	private static XMLControl prefsControl;

	/** Preferences path */
	private static String prefsPath;

	/** Preferences filename */
	private static String prefsFileName = "osp.prefs"; //$NON-NLS-1$

	/**
	 * Sets default properties for OSP.
	 */
	static {
		try { // set the user home and default directory for the chooser // system properties
				// may not be readable in some contexts
			chooserDir = System.getProperty("user.dir", null); //$NON-NLS-1$
			String userhome = getUserHome();
			if (userhome != null) {
				userhomeDir = XML.forwardSlash(userhome);
			}
		} catch (Exception ex) {
			chooserDir = null;
		}
		// fill the look and feel map
		LOOK_AND_FEEL_TYPES.put(METAL_LF, "javax.swing.plaf.metal.MetalLookAndFeel"); //$NON-NLS-1$
		LOOK_AND_FEEL_TYPES.put(NIMBUS_LF, "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel"); //$NON-NLS-1$
		LOOK_AND_FEEL_TYPES.put(GTK_LF, "com.sun.java.swing.plaf.gtk.GTKLookAndFeel"); //$NON-NLS-1$
		LOOK_AND_FEEL_TYPES.put(MOTIF_LF, "com.sun.java.swing.plaf.motif.MotifLookAndFeel"); //$NON-NLS-1$
		LOOK_AND_FEEL_TYPES.put(WINDOWS_LF, "com.sun.java.swing.plaf.windows.WindowsLookAndFeel"); //$NON-NLS-1$
		LOOK_AND_FEEL_TYPES.put(CROSS_PLATFORM_LF, UIManager.getCrossPlatformLookAndFeelClassName());
		LOOK_AND_FEEL_TYPES.put(SYSTEM_LF, UIManager.getSystemLookAndFeelClassName());
		LOOK_AND_FEEL_TYPES.put(DEFAULT_LF, DEFAULT_LOOK_AND_FEEL.getClass().getName());

		NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
		if (format instanceof DecimalFormat) {
			setDefaultDecimalSeparator(((DecimalFormat) format).getDecimalFormatSymbols().getDecimalSeparator());
		} else {
			setDefaultDecimalSeparator(org.opensourcephysics.numerics.Util.newDecimalFormat("0")
					.getDecimalFormatSymbols().getDecimalSeparator());
		}

//		dfs.setDecimalSeparator(currentDecimalSeparator = defaultDecimalSeparator);
//		dfs.setMinusSign(MINUS);

//	try {
//	  Class.forName("com.sun.j3d.utils.universe.SimpleUniverse"); //$NON-NLS-1$
//	  J3D= true; 
//	} catch (NoClassDefFoundError e) { // Do not complain
//	  J3D=  false; 
//	} catch (ClassNotFoundException e) {
//	  J3D=  false; 
//	}
	}

	public static class TextLayout {

		class JSTL  {

			//private LineMetrics lm;

			JSTL() {
			   // not necessary; could implement if needed? lm = font.getLineMetrics(text, frc);	
			}
			
			void draw(Graphics2D g, float x, float y) {
				boolean testing = false;
				g.setFont(font);
				g.drawString(text, x, y);
				if (testing) {
					java.awt.font.TextLayout t0 = new java.awt.font.TextLayout(text, font, frc);
					g.setColor(Color.red);
					int w = (int) t0.getBounds().getWidth();
					t0.draw(g, x + w, y);
					t0.draw(g, x - w, y);
				}

			}

			Rectangle2D getBounds() {
				return font.getStringBounds(text, frc);
			}

			float getAscent() {
				return font.getLineMetrics(text, frc).getAscent();
			}

			float getDescent() {
				return font.getLineMetrics(text, frc).getDescent();
			}

			float getLeading() {
				return font.getLineMetrics(text, frc).getLeading();
			}

		}

		private String text;
		private Font font;
		
		private java.awt.font.TextLayout tl;

		public TextLayout(String text, Font font) {
			//System.out.println("TextLayout for " + text);
			this.text = text;
			this.font = font;
		}

		public void draw(Graphics g, float x, float y) {
			getTL();
			//System.out.println("TextLayout draw " + text + " " + x + " " + y);
			tl.draw((Graphics2D) g, x, y);
		}

		public Rectangle2D getBounds() {
			getTL();
			//System.out.println("TextLayout bounds " + text + " " + tl.getBounds() + " A=" + tl.getAscent() + " D=" + tl.getDescent() + " L=" + tl.getLeading());
			return tl.getBounds();
		}
		
		private java.awt.font.TextLayout getTL() {
			if (tl == null) {
				if (isJS) {
					tl = (java.awt.font.TextLayout) (Object) new JSTL();
				} else {
					tl = new java.awt.font.TextLayout(text, font, frc);
				}
			}
			return tl;
		}


	}
	
	public static FontRenderContext frc = new FontRenderContext(//
			null, // no AffineTransform
			false, // no antialiasing
			false); // no fractional metrics

	public static LaunchNode activeNode;
	private static char currentDecimalSeparator;
	public static boolean launcherAllowEJSModel = true;

	public static final Integer OUT_OF_MEMORY_ERROR = 1; // Integer here, because it will be e.newValue()
	public static boolean outOfMemory = false;

	/**
	 * Private constructor to prevent instantiation.
	 */
	private OSPRuntime() {
		/** empty block */
	}

	/**
	 * Gets the user home directory.
	 * 
	 * @return the user home
	 */
	public static String getUserHome() {
		String home = System.getProperty("user.home"); //$NON-NLS-1$
		if (isLinux()) {
			String homeEnv = System.getenv("HOME"); //$NON-NLS-1$
			if (homeEnv != null) {
				home = homeEnv;
			}
		}
		return home == null ? "." : home; //$NON-NLS-1$
	}

	/**
	 * Gets the download directory.
	 * 
	 * @return the download directory
	 */
	public static File getDownloadDir() {
		String home = getUserHome();
		File downloadDir = new File(home + "/Downloads"); //$NON-NLS-1$
		if (isLinux()) {
			// get XDG_DOWNLOAD_DIR if possible--usually "$HOME/xxx" but may be absolute
			String xdgDir = home + "/.config/user-dirs.dirs"; //$NON-NLS-1$
			String xdgText = ResourceLoader.getString(xdgDir);
			if (xdgText != null) {
				String[] split = xdgText.split("XDG_"); //$NON-NLS-1$
				for (String next : split) {
					if (next.contains("DOWNLOAD_DIR")) { //$NON-NLS-1$
						// get name between quotes
						int n = next.indexOf("\""); //$NON-NLS-1$
						if (n > -1) {
							next = next.substring(n + 1);
							n = next.indexOf("\""); //$NON-NLS-1$
							if (n > -1) {
								next = next.substring(0, n);
								// substitute home for $HOME
								if (next.startsWith("$HOME")) { //$NON-NLS-1$
									next = home + next.substring(5);
								}
								File f = new File(next);
								if (f.exists()) {
									downloadDir = f;
								}
							}
						}
					}
				}
			}
		}
		return downloadDir;
	}

	/**
	 * Shows the about dialog.
	 * 
	 * @param parent
	 */
	public static void showAboutDialog(Component parent) {
		String date = getLaunchJarBuildDate();
		if ("".equals(date))
			date = RELEASE_DATE;

		String vers = "OSP Library " + VERSION; //$NON-NLS-1$
		if (date != null) {
			vers += "\njar manifest date " + date; //$NON-NLS-1$
		}
		if (isJS)
			vers += "\n\nJavaScript transcription created using the\n"
					+ "java2script/SwingJS framework developed at\n St. Olaf College.\n";
		String aboutString = vers + "\n" //$NON-NLS-1$
				+ "Open Source Physics Project \n" + "www.opensourcephysics.org"; //$NON-NLS-1$ //$NON-NLS-2$
		JOptionPane.showMessageDialog(parent, aboutString, "About Open Source Physics", //$NON-NLS-1$
				JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Sets the look and feel of the user interface. Look and feel user interfaces
	 * are:
	 *
	 * NIMBUS_LF: com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel METAL_LF:
	 * javax.swing.plaf.metal.MetalLookAndFeel GTK_LF:
	 * com.sun.java.swing.plaf.gtk.GTKLookAndFeel MOTIF_LF:
	 * com.sun.java.swing.plaf.motif.MotifLookAndFeel WINDOWS_LF:
	 * com.sun.java.swing.plaf.windows.WindowsLookAndFeel DEFAULT_LF: the default
	 * look and feel in effect when this class was loaded CROSS_PLATFORM_LF: the
	 * cross platform look and feel; usually METAL_LF SYSTEM_LF: the operating
	 * system look and feel
	 *
	 * @param useDefaultLnFDecorations
	 * @param lookAndFeel
	 *
	 * @return true if successful
	 */
	public static boolean setLookAndFeel(boolean useDefaultLnFDecorations, String lookAndFeel) {
		boolean found = true;
		LookAndFeel currentLookAndFeel = UIManager.getLookAndFeel();
		try {
			if ((lookAndFeel == null) || lookAndFeel.equals(DEFAULT_LF)) {
				UIManager.setLookAndFeel(DEFAULT_LOOK_AND_FEEL);
				useDefaultLnFDecorations = DEFAULT_LOOK_AND_FEEL_DECORATIONS;
			} else if (lookAndFeel.equals(CROSS_PLATFORM_LF)) {
				lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
				UIManager.setLookAndFeel(lookAndFeel);
			} else if (lookAndFeel.equals(SYSTEM_LF)) {
				lookAndFeel = UIManager.getSystemLookAndFeelClassName();
				UIManager.setLookAndFeel(lookAndFeel);
			} else if (lookAndFeel.equals(NIMBUS_LF)) {
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel"); //$NON-NLS-1$
			} else if (lookAndFeel.equals(METAL_LF)) {
				// MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
				UIManager.setLookAndFeel(new MetalLookAndFeel());
			} else if (lookAndFeel.equals(GTK_LF)) {
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel"); //$NON-NLS-1$
			} else if (lookAndFeel.equals(MOTIF_LF)) {
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel"); //$NON-NLS-1$
			} else if (lookAndFeel.equals(WINDOWS_LF)) {
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel"); //$NON-NLS-1$
			} else {
				UIManager.setLookAndFeel(lookAndFeel); // LnF can be set using a fully qualified path
			}
			JFrame.setDefaultLookAndFeelDecorated(useDefaultLnFDecorations);
			JDialog.setDefaultLookAndFeelDecorated(useDefaultLnFDecorations);
		} catch (Exception ex) {
			found = false;
		}
		if (!found) { // keep current look and feel
			try {
				UIManager.setLookAndFeel(currentLookAndFeel);
			} catch (Exception e) {
			}
		}
		return found;
	}

	/**
	 * Returns true if newly created <code>JFrame</code>s or <code>JDialog</code>s
	 * should have their Window decorations provided by the current look and feel.
	 * This is only a hint, as certain look and feels may not support this feature.
	 *
	 * @return true if look and feel should provide Window decorations.
	 * @since 1.4
	 */
	public static boolean isDefaultLookAndFeelDecorated() {
		return JFrame.isDefaultLookAndFeelDecorated();
	}

	/**
	 * Determines if OS is Windows
	 *
	 * @return true if Windows
	 */
	public static boolean isWindows() {
		try { // system properties may not be readable in some environments
			return (System.getProperty("os.name", "").toLowerCase().startsWith("windows")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} catch (SecurityException ex) {
			return false;
		}
	}

	/**
	 * Determines if OS is Mac
	 *
	 * @return true if Mac
	 */
	public static boolean isMac() {
		return isMac;
	}

	/**
	 * Determines if OS is Linux
	 *
	 * @return true if Linux
	 */
	public static boolean isLinux() {
		try { // system properties may not be readable in some environments
			return (System.getProperty("os.name", "").toLowerCase().startsWith("linux")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} catch (SecurityException ex) {
			return false;
		}
	}

	/**
	 * Determines if OS is Vista
	 *
	 * @return true if Vista
	 */
	static public boolean isVista() {
		if (System.getProperty("os.name", "").toLowerCase().indexOf("vista") > -1) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return true;
		}
		return false;
	}

	static public boolean hasJava3D() {
		return false;
//		if (true)
//			return false; // Java 3D is no longer supported
//		try {
//			if (isMac) { // extra testing for Mac
//				boolean tryIt = true;
//				String home = System.getProperty("java.home");//$NON-NLS-1$
//				String version = System.getProperty("java.version"); //$NON-NLS-1$
//				if (version.indexOf("1.7") < 0 && version.indexOf("1.8") < 0) //$NON-NLS-1$ //$NON-NLS-2$
//					tryIt = true;
//				else
//					tryIt = (new java.io.File(home + "/lib/ext/j3dcore.jar")).exists(); //$NON-NLS-1$
//				if (!tryIt)
//					return false;
//			}
//		} catch (Exception exc) {
//			return false;
//		} // Any problem! Do not complain and quit
//		// look for J3D class
//		try {
//			Class.forName("com.sun.j3d.utils.universe.SimpleUniverse"); //$NON-NLS-1$
//			return true;
//		} catch (Error e) { // do not not complain
//			return false;
//		} catch (Exception e) { // do not not complain
//			return false;
//		}
	}

	/**
	 * Determines if an InputEvent is a popup trigger.
	 * 
	 * @param e the input event
	 * @return true if event is a popup trigger
	 */
	static public boolean isPopupTrigger(InputEvent e) {
		if (e instanceof MouseEvent) {
			MouseEvent me = (MouseEvent) e;
			if (me.isShiftDown())
				return false;
			return (me.isPopupTrigger()) || (me.getButton() == MouseEvent.BUTTON3) || (me.isControlDown() && isMac);
		}
		return false;
	}

	/**
	 * Determines if launched by WebStart
	 *
	 * @return true if launched by WebStart
	 */
	public static boolean isWebStart() {
		if (!webStart) {
			try {
				webStart = System.getProperty("javawebstart.version") != null; //$NON-NLS-1$
				// once true, remains true
			} catch (Exception ex) {
			}
		}
		return webStart;
	}

	/**
	 * Determines if running as an applet
	 *
	 * @return true if running as an applet
	 */
	public static boolean isAppletMode() {
		return appletMode;
	}

	/**
	 * Determines if running in author mode
	 *
	 * @return true if running in author mode
	 */
	public static boolean isAuthorMode() {
		return authorMode;
	}

	/**
	 * Sets the authorMode property. AuthorMode allows users to author internal
	 * parameters such as Locale strings.
	 *
	 * @param b boolean
	 */
	public static void setAuthorMode(boolean b) {
		authorMode = b;
	}

	/**
	 * Sets the launcherMode property to true if applications in this VM are
	 * launched by Launcher. LauncherMode disables access to properties, such as
	 * Locale, that affect the VM.
	 *
	 * @param b boolean
	 */
	public static void setLauncherMode(boolean b) {
		launcherMode = b;
	}

	/**
	 * Gets the launcherMode property. Returns true if applications in this VM are
	 * launched by Launcher. LauncherMode disables access to properties, such as
	 * Locale, that affect the VM.
	 *
	 * @return boolean
	 */
	public static boolean isLauncherMode() {
		return launcherMode || "true".equals(System.getProperty("org.osp.launcher")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Sets the launch jar path.
	 * 
	 * @param path the path
	 */
	public static void setLaunchJarPath(String path) {
		if ((path == null) || (launchJarPath != null)) {
			return;
		}
		// make sure the path ends with or contains a jar file
		if (!path.endsWith(".jar") && !path.endsWith(".exe")) { //$NON-NLS-1$ //$NON-NLS-2$
			int n = path.indexOf(".jar!"); //$NON-NLS-1$
			if (n == -1) {
				n = path.indexOf(".exe!"); //$NON-NLS-1$
			}
			if (n > -1) {
				path = path.substring(0, n + 4);
			} else {
				return;
			}
		}
		if (path.startsWith("jar:")) { //$NON-NLS-1$
			path = path.substring(4, path.length());
		}
		if (path.startsWith("file:/")) { //$NON-NLS-1$
			path = path.substring(5, path.length());
			if (path.contains(":")) { //$NON-NLS-1$ // windows drive eg C:/
				path = path.substring(1, path.length());
			}
		}
		try {
			// check that file exists and set launchJarPath to file path
			File file = new File(path);
			if (!file.exists())
				return;
			path = XML.forwardSlash(file.getCanonicalPath());
		} catch (Exception ex) {
		}
		OSPLog.finer("Setting launch jar path to " + path); //$NON-NLS-1$
		launchJarPath = path;
		launchJarName = path.substring(path.lastIndexOf("/") + 1); //$NON-NLS-1$
	}

	/**
	 * Gets the launch jar name, if any.
	 * 
	 * @return launch jar path, or null if not launched from a jar
	 */
	public static String getLaunchJarName() {
		return launchJarName;
	}

	/**
	 * Gets the launch jar path, if any.
	 * 
	 * @return launch jar path, or null if not launched from a jar
	 */
	public static String getLaunchJarPath() {
		return launchJarPath;
	}

	/**
	 * Gets the window.location.href when JavaScript code is running in an html
	 * page.
	 * 
	 * @return window.location.href
	 */
	public static String getDocbase() {
		String base = "";
		if (!isJS)
			return null;
		/**
		 * @j2sNative console.log("href="+window.location.href);
		 *            base=""+window.location.href;
		 */
		int last = base.lastIndexOf('/'); // look for path/document.html
		if (last < 1) {
			return base;
		}
		base = base.substring(0, last + 1); // strip document from url
		return base;
	}

	/**
	 * Gets the launch jar directory, if any.
	 * 
	 * @return path to the directory containing the launch jar. May be null.
	 */
	public static String getLaunchJarDirectory() {
		if (OSPRuntime.isApplet) {
			return null;
		}
		return (launchJarPath == null) ? null : XML.getDirectoryPath(launchJarPath);
	}

	/**
	 * Gets the jar from which the progam was launched.
	 * 
	 * @return JarFile
	 */
	public static JarFile getLaunchJar() {
		if (launchJar != null) {
			return launchJar;
		}
		if (launchJarPath == null) {
			return null;
		}
		boolean isWebFile = ResourceLoader.isHTTP(launchJarPath);
		if (!isWebFile) {
			launchJarPath = ResourceLoader.getNonURIPath(launchJarPath);
		}
		try {
			if (!isApplet && !isWebFile) { // application mode
				launchJar = new JarFile(launchJarPath);
			} else { // applet mode
				URL url;
				if (isWebFile) {
					// create a URL that refers to a jar file on the web
					url = new URL("jar:" + launchJarPath + "!/"); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					// create a URL that refers to a local jar file
					url = new URL("jar:file:/" + launchJarPath + "!/"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				// get the jar
				JarURLConnection conn = (JarURLConnection) url.openConnection();
				launchJar = conn.getJarFile();
			}
		} catch (Exception ex) {
			// ex.printStackTrace();
			OSPLog.fine(ex.getMessage());
		}
		return launchJar;
	}

	/**
	 * Gets a manifest attribute.
	 * 
	 * @return the String value or "" if not known
	 */
	public static String getManifestAttribute(JarFile jarFile, String attribute) {
		//System.out.println("reading attribute="+attribute);
		if(OSPRuntime.isJS) {
			System.err.println("getManifestAttribute from jar not allowed in JavaScript");
			return "";
		}
		try {
			java.util.jar.Attributes att = jarFile.getManifest().getMainAttributes();
			return att.getValue(attribute);
		} catch (Exception e) {
		}
		return "";
	}

	/**
	 * Gets the launch jar build date.
	 * 
	 * @return the build date, or "" if not launched from a jar or date not known
	 */
	public static String getLaunchJarBuildDate() {
		if(OSPRuntime.isJS) {
			try {
        BufferedReader inFile = new BufferedReader(new FileReader("MANIFEST.MF"));
        String nextLine = inFile.readLine();
        while(nextLine!=null) {
          //System.out.println(nextLine);
          if(nextLine.startsWith("Build-Date:")) {
          	String date = nextLine.substring(11);
          	inFile.close();
          	return date;
          	//return(nextLine+"  from JS");
          }
          nextLine = inFile.readLine();
        }
        inFile.close();
      } catch(Exception ex) {
        System.err.println(ex.getMessage());
      }		
			return "";
		}
		if (buildDate == null) {
			JarFile jarfile = getLaunchJar();
			buildDate = getManifestAttribute(jarfile, "Build-Date");
		}
		return buildDate;
	}

	/**
	 * Gets the java executable file for a given jre path. May return null.
	 * 
	 * @param jrePath the path to a java jre or jdk VM
	 * @return the Java executable
	 */
	public static File getJavaFile(String jrePath) {
		if (jrePath == null)
			return null;
		File file = new File(jrePath);
		jrePath = XML.forwardSlash(jrePath);
		if (jrePath.endsWith("/lib/ext")) { //$NON-NLS-1$
			jrePath = jrePath.substring(0, jrePath.length() - 8);
			file = new File(jrePath);
		}
		if (!jrePath.endsWith("/bin/java") && !jrePath.endsWith("/bin/java.exe")) { //$NON-NLS-1$ //$NON-NLS-2$
			if (jrePath.endsWith("/bin")) { //$NON-NLS-1$
				file = file.getParentFile();
			}
			if (isWindows()) {
				// typical jdk: Program Files\Java\jdkX.X.X_XX\jre\bin\java.exe
				// typical jre: Program Files\Java\jreX.X.X_XX\bin\java.exe
				// or Program Files\Java\jreX\bin\java.exe
				// typical 32-bit jdk in 64-bit Windows: Program
				// Files(x86)\Java\jdkX.X.X_XX\jre\bin\java.exe
				if (file.getParentFile() != null && file.getParentFile().getName().indexOf("jre") > -1) { //$NON-NLS-1$
					file = file.getParentFile();
				}
				if (file.getParentFile() != null && file.getParentFile().getName().indexOf("jdk") > -1) { //$NON-NLS-1$
					file = file.getParentFile();
				}
// master:
// TODO: Doug, check commit 8db541 2019-11-12
		  		if (file.getName().indexOf("jdk")>-1) { //$NON-NLS-1$
		  			File jreFile = new File(file, "jre/bin/java.exe"); //$NON-NLS-1$
		  			if (jreFile.exists()) file = jreFile;
		  			else {
		  				// newer jdks do NOT have a jre included so just use them directly
		  				file = new File(file, "bin/java.exe"); //$NON-NLS-1$
		  			}
		  		}
// SwingJS (old):
//				if (file.getName().indexOf("jdk") > -1) //$NON-NLS-1$
//					file = new File(file, "jre/bin/java.exe"); //$NON-NLS-1$
				else if (file.getName().indexOf("jre") > -1) { //$NON-NLS-1$
					file = new File(file, "bin/java.exe"); //$NON-NLS-1$
				} else
					file = null;
			} else if (isMac) {
				// typical jdk public:
				// /System/Library/Java/JavaVirtualMachines/X.X.X.jdk/Contents/Home/jre
				// jdk private: /System/Library/Java/JavaVirtualMachines/X.X.X.jdk/Contents/Home
				// in Tracker.app:
				// /Applications/Tracker.app/Contents/PlugIns/Java.runtime/Contents/Home/jre
				// also in /Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home
				// also sometimes in /Library/Java...??
				// symlink at: /Library/Java/Home/bin/java??
				if (file.getName().endsWith("jdk")) { //$NON-NLS-1$
					File parent = file;
					file = new File(parent, "Contents/Home/jre/bin/java"); //$NON-NLS-1$
					if (!file.exists()) {
						file = new File(parent, "Contents/Home/bin/java"); //$NON-NLS-1$ )
					}
				} else
					file = new File(file, "bin/java"); //$NON-NLS-1$
			} else if (isLinux()) {
				// typical: /usr/lib/jvm/java-X-openjdk/jre/bin/java
				// bundled: /opt/tracker/jre/bin/java
				// symlink at: /usr/lib/jvm/java-X.X.X-openjdk/jre/bin/java
				// sun versions: java-X-sun and java-X.X.X-sun
				if ("jre".equals(file.getName())) { //$NON-NLS-1$
					file = new File(file, "bin/java"); //$NON-NLS-1$
				} else {
					if (file.getParentFile() != null && file.getParentFile().getName().indexOf("jre") > -1) { //$NON-NLS-1$
						file = file.getParentFile();
					}
					if (file.getParentFile() != null && file.getParentFile().getName().indexOf("jdk") > -1) { //$NON-NLS-1$
						file = file.getParentFile();
					}
					if (file.getParentFile() != null && file.getParentFile().getName().indexOf("sun") > -1) { //$NON-NLS-1$
						file = file.getParentFile();
					}
					if (file.getName().indexOf("jdk") > -1 //$NON-NLS-1$
							|| file.getName().indexOf("sun") > -1) //$NON-NLS-1$
						file = new File(file, "jre/bin/java"); //$NON-NLS-1$
					else
						file = null;
				}
			}
		}
		// resolve symlinks to their targets
		if (file != null) {
			try {
				file = file.getCanonicalFile();
			} catch (IOException e) {
				file = null;
			}
		}
		if (file != null && file.exists())
			return file;
		return null;
	}
	
	/**
	 * Gets the major version number. For VERSION 6.0.1, the major is 6.
	 * 
	 * @return the major version, or 0 if fails
	 */
	public static int getMajorVersion() {
		String[] v = VERSION.trim().split("\\."); //$NON-NLS-1$
		try {
			return Integer.parseInt(v[0]);
		} catch (Exception ex) {
			return 0;
		}		
	}

	/**
	 * Gets the bitness of the current Java VM. Note this identifies only 32- and
	 * 64-bit VMs as of Jan 2011.
	 * 
	 * @return 64 if 64-bit VM, otherwise 32
	 */
	public static int getVMBitness() {
		String s = System.getProperty("java.vm.name"); //$NON-NLS-1$
		s += "-" + System.getProperty("os.arch"); //$NON-NLS-1$ //$NON-NLS-2$
		s += "-" + System.getProperty("sun.arch.data.model"); //$NON-NLS-1$ //$NON-NLS-2$
		return s.indexOf("64") > -1 ? 64 : 32; //$NON-NLS-1$
	}

	/**
	 * Gets the java VM path for a given Java executable file.
	 * 
	 * @param javaFile the Java executable file
	 * @return the VM path
	 */
	public static String getJREPath(File javaFile) {
		if (javaFile == null)
			return null;
		String javaPath = XML.forwardSlash(javaFile.getAbsolutePath());
		// all java command files should end with /bin/java or /bin/java.exe
		if (XML.stripExtension(javaPath).endsWith("/bin/java")) //$NON-NLS-1$
			return javaFile.getParentFile().getParent();
		return ""; //$NON-NLS-1$
	}

	/**
	 * Gets Locales for languages that have properties files in the core library.
	 * 
	 * @return Locale[]
	 */
	public static Locale[] getDefaultLocales() {
		return defaultLocales;
	}

	/**
	 * Gets Locales for languages that have properties files in the core library.
	 * Locales are returned with English first, then in alphabetical order.
	 * 
	 * @return Locale[]
	 */
	public static Locale[] getInstalledLocales() {
		ArrayList<Locale> list = new ArrayList<Locale>();
		if (OSPRuntime.isJS)
			return list.toArray(new Locale[0]);
		java.util.TreeMap<String, Locale> languages = new java.util.TreeMap<String, Locale>();
		list.add(Locale.ENGLISH); // english is first in list
		if (getLaunchJarPath() != null) {
			// find available locales
			JarFile jar = getLaunchJar();
			if (jar != null) {
				for (Enumeration<?> e = jar.entries(); e.hasMoreElements();) {
					JarEntry entry = (JarEntry) e.nextElement();
					String path = entry.toString();
					int n = path.indexOf(".properties"); //$NON-NLS-1$
					if (path.indexOf(".properties") > -1) { //$NON-NLS-1$
						int m = path.indexOf("display_res_"); //$NON-NLS-1$
						if (m > -1) {
							String loc = path.substring(m + 12, n);
							if (loc.equals("zh_TW")) { //$NON-NLS-1$
								Locale next = Locale.TAIWAN;
								languages.put(getDisplayLanguage(next).toLowerCase(), next);
							} else if (loc.equals("zh_CN")) { //$NON-NLS-1$
								Locale next = Locale.CHINA;
								languages.put(getDisplayLanguage(next).toLowerCase(), next);
							} else if (loc.equals("en_US")) { //$NON-NLS-1$
								continue;
							} else {
								Locale next;
								if (!loc.contains("_")) //$NON-NLS-1$
									next = new Locale(loc);
								else {
									String lang = loc.substring(0, 2);
									String country = loc.substring(3);
									next = new Locale(lang, country, ""); //$NON-NLS-1$
								}
								if (!next.equals(Locale.ENGLISH)) {
									languages.put(getDisplayLanguage(next).toLowerCase(), next);
								}
							}
						}
					}
				}
				for (String s : languages.keySet()) {
					list.add(languages.get(s));
				}
			} else {
				defaultLocales = new Locale[] { Locale.ENGLISH };
				return defaultLocales;
			}
		}
		return list.toArray(new Locale[0]);
	}

	/**
	 * Gets the display language for a given Locale. This returns the language name
	 * in the locale's own language, but substitutes the equivalent of
	 * SIMPLIFIED_CHINESE and TRADITIONAL_CHINESE for those locales.
	 * 
	 * @param locale the Locale
	 * @return the display language
	 */
	public static String getDisplayLanguage(Locale locale) {
		if (locale.equals(Locale.CHINA))
			return "\u7b80\u4f53\u4e2d\u6587"; //$NON-NLS-1$
		if (locale.equals(Locale.TAIWAN))
			return "\u7e41\u4f53\u4e2d\u6587"; //$NON-NLS-1$
		return locale.getDisplayLanguage(locale);
	}

	/**
	 * Gets DecimalFormatSymbols that use the preferred decimal separator, if any.
	 * If no preference, the default separator for the current locale is used.
	 * 
	 * @return the DecimalFormatSymbols
	 */
	public static DecimalFormatSymbols getDecimalFormatSymbols() {
		return dfs;
	}

	/**
	 * Sets the default decimal separator.
	 * 
	 * Sets the current DecimalFormatSymbols value to the given value as long as
	 * there is no preferred decimal separator.
	 * 
	 * @param c a decimal separator
	 */
	public static void setDefaultDecimalSeparator(char c) {
		if (c == DECIMAL_SEPARATOR_PERIOD || c == DECIMAL_SEPARATOR_COMMA)
			defaultDecimalSeparator = c;
		else
			defaultDecimalSeparator = DECIMAL_SEPARATOR_PERIOD;
		if (preferredDecimalSeparator == null)
			dfs.setDecimalSeparator(currentDecimalSeparator = defaultDecimalSeparator);
	}

	/**
	 * 
	 * Sets the preferred decimal separator. 
	 * Must be DECIMAL_SEPARATOR_PERIOD or DECIMAL_SEPARATOR_COMMA.
	 * 
	 * @param separator If non-null, assigns the given value to the current
	 *                  DecimalFormatSymbols value; if null, resets the
	 *                  DecimalFormatSymbols value to the default value.
	 * 
	 */
	public static void setPreferredDecimalSeparator(String separator) {
		// only allow comma and period
		if (separator != null 
				&& separator.charAt(0) != DECIMAL_SEPARATOR_PERIOD
				&& separator.charAt(0) != DECIMAL_SEPARATOR_COMMA)
			separator = null;
		preferredDecimalSeparator = separator;
		dfs.setDecimalSeparator(
				currentDecimalSeparator = (separator == null ? defaultDecimalSeparator : separator.charAt(0)));
	}

	public static char getCurrentDecimalSeparator() {
		return currentDecimalSeparator;
	}

	/**
	 * Gets the preferred decimal separator. May return null.
	 * 
	 * @return the separator, if any
	 */
	public static String getPreferredDecimalSeparator() {
		return preferredDecimalSeparator;
	}

	/**
	 * Gets the default search paths, typically used for autoloading. Search paths
	 * are platform-specific "appdata", user home and code base, in that order.
	 * 
	 * @return the default search paths
	 */
	public static ArrayList<String> getDefaultSearchPaths() {
		ArrayList<String> paths = new ArrayList<String>();
		if (isWindows()) {
			String appdata = System.getenv("LOCALAPPDATA"); //$NON-NLS-1$
			if (appdata != null) {
				File dir = new File(appdata, "OSP"); //$NON-NLS-1$
				if (!dir.exists())
					dir.mkdir();
				if (dir.exists()) {
					paths.add(XML.forwardSlash(dir.getAbsolutePath()));
				}
			}
		} else if (userhomeDir != null && isMac) {
			File dir = new File(userhomeDir, "Library/Application Support"); //$NON-NLS-1$
			if (dir.exists()) {
				dir = new File(dir, "OSP"); //$NON-NLS-1$
				if (!dir.exists())
					dir.mkdir();
				if (dir.exists()) {
					paths.add(XML.forwardSlash(dir.getAbsolutePath()));
				}
			}
		} else if (userhomeDir != null && isLinux()) {
			File dir = new File(userhomeDir, ".config"); //$NON-NLS-1$
			if (dir.exists()) {
				dir = new File(dir, "OSP"); //$NON-NLS-1$
				if (!dir.exists())
					dir.mkdir();
				if (dir.exists()) {
					paths.add(XML.forwardSlash(dir.getAbsolutePath()));
				}
			}
		}
		if (userhomeDir != null) {
			paths.add(userhomeDir);
		}
		String codebase = getLaunchJarDirectory();
		if (codebase != null) {
			paths.add(XML.forwardSlash(codebase));
		}
		return paths;
	}

	/**
	 * Gets a named preference object. The object must be cast to the correct type
	 * by the user.
	 * 
	 * @param name the name of the preference
	 * @return the object (may be null)
	 */
	public static Object getPreference(String name) {
		XMLControl control = getPrefsControl();
		return control.getObject(name);
	}

	/**
	 * Sets a named preference object. The object can be anything storable in an
	 * XMLControl--eg, String, Collection, OSP object, Boolean, Double, Integer
	 * 
	 * @param name the name of the preference
	 * @param pref the object (may be null)
	 */
	public static void setPreference(String name, Object pref) {
		XMLControl control = getPrefsControl();
		control.setValue(name, pref);
	}

	/**
	 * Saves the current preference XMLControl by writing to a file.
	 */
	public static void savePreferences() {
		XMLControl control = getPrefsControl();
		File file = new File(prefsPath, prefsFileName);
		control.write(file.getAbsolutePath());
	}

	/**
	 * Gets the preferences XML file if it exists.
	 * 
	 * @return the file, or null if none exists
	 */
	public static File getPreferencesFile() {
		getPrefsControl(); // ensures the prefs path is defined and writes file if needed
		File file = new File(prefsPath, prefsFileName);
		if (file.exists())
			return file;
		return null;
	}

	/**
	 * Gets the preference XMLControl. This will load the control from the prefs
	 * file if it can be found, otherwise create a new one.
	 * 
	 * @return the XMLControl
	 */
	private static XMLControl getPrefsControl() {
		if (prefsControl == null) {
			// try to load prefs control from default search paths
			ArrayList<String> dirs = getDefaultSearchPaths();
			for (String dir : dirs) {
				File file = new File(dir, prefsFileName);
				if (!file.exists()) {
					// try with leading dot
					file = new File(dir, "." + prefsFileName); //$NON-NLS-1$
					if (file.exists()) {
						prefsFileName = "." + prefsFileName; //$NON-NLS-1$
					}
				}
				if (file.exists()) {
					XMLControl test = new XMLControlElement(file);
					if (!test.failedToRead()) {
						prefsControl = test;
						prefsPath = XML.forwardSlash(dir);
						break;
					}
				}
			}
			if (prefsControl == null) {
				prefsControl = new XMLControlElement();
				// by default, save prefs in first default search directory
				prefsPath = XML.forwardSlash(dirs.get(0));
				if (prefsPath.equals(userhomeDir)) {
					// if saving in user home, add leading dot to hide
					prefsFileName = "." + prefsFileName; //$NON-NLS-1$
				}
				File file = new File(prefsPath, prefsFileName);
				// BH 2020.02.13 don't want to download this file.
				if (!isJS)
					prefsControl.write(file.getAbsolutePath());
			}
		}
		return prefsControl;
	}

	/**
	 * Gets the translator, if any.
	 * 
	 * @return translator, or null if none available
	 */
	public static Translator getTranslator() {
		return (loadTranslatorTool ? TranslatorTool.getTool() : null);
	}

	/**
	 * Gets a file chooser. The choose is static and will therefore be the same for
	 * all OSPFrames.
	 *
	 * @return the chooser
	 */
	public static AsyncFileChooser getChooser() {
		if (chooser != null) {
			FontSizer.setFonts(chooser, FontSizer.getLevel());
			return chooser;
		}
		try {
			chooser = (chooserDir == null) ? new AsyncFileChooser() : new AsyncFileChooser(new File(chooserDir));
		} catch (Exception e) {
			System.err.println("Exception in OSPFrame getChooser=" + e); //$NON-NLS-1$
			return null;
		}
		javax.swing.filechooser.FileFilter defaultFilter = chooser.getFileFilter();
		javax.swing.filechooser.FileFilter xmlFilter = new javax.swing.filechooser.FileFilter() {
			// accept all directories and *.xml files.
			@Override
			public boolean accept(File f) {
				if (f == null) {
					return false;
				}
				if (f.isDirectory()) {
					return true;
				}
				String extension = null;
				String name = f.getName();
				int i = name.lastIndexOf('.');
				if ((i > 0) && (i < name.length() - 1)) {
					extension = name.substring(i + 1).toLowerCase();
				}
				if ((extension != null) && (extension.equals("xml"))) { //$NON-NLS-1$
					return true;
				}
				return false;
			}

			// the description of this filter
			@Override
			public String getDescription() {
				return DisplayRes.getString("OSPRuntime.FileFilter.Description.XML"); //$NON-NLS-1$
			}

		};
		javax.swing.filechooser.FileFilter txtFilter = new javax.swing.filechooser.FileFilter() {
			// accept all directories and *.txt files.
			@Override
			public boolean accept(File f) {
				if (f == null) {
					return false;
				}
				if (f.isDirectory()) {
					return true;
				}
				String extension = null;
				String name = f.getName();
				int i = name.lastIndexOf('.');
				if ((i > 0) && (i < name.length() - 1)) {
					extension = name.substring(i + 1).toLowerCase();
				}
				if ((extension != null) && extension.equals("txt")) { //$NON-NLS-1$
					return true;
				}
				return false;
			}

			// the description of this filter
			@Override
			public String getDescription() {
				return DisplayRes.getString("OSPRuntime.FileFilter.Description.TXT"); //$NON-NLS-1$
			}

		};
		chooser.addChoosableFileFilter(xmlFilter);
		chooser.addChoosableFileFilter(txtFilter);
		chooser.setFileFilter(defaultFilter);
		FontSizer.setFonts(chooser, FontSizer.getLevel());
		return chooser;
	}

	/**
	 * Uses a JFileChooser to ask for a name.
	 * 
	 * @param chooser JFileChooser
	 * @return String The absolute pah of the filename. Null if cancelled
	 */
	static public String chooseFilename(JFileChooser chooser) {
		return chooseFilename(chooser, null, true);
	}

	/**
	 * Uses a JFileChooser to ask for a name.
	 * 
	 * @param chooser JFileChooser
	 * @param parent  Parent component for messages
	 * @param toSave  true if we will save to the chosen file, false if we will read
	 *                from it
	 * @return String The absolute pah of the filename. Null if cancelled
	 */
	static public String chooseFilename(JFileChooser chooser, Component parent, boolean toSave) {
		String fileName = null;
		int result;
		if (toSave) {
			result = chooser.showSaveDialog(parent);
		} else {
			result = chooser.showOpenDialog(parent);
		}
		if (result == JFileChooser.APPROVE_OPTION) {
			chooserDir = chooser.getCurrentDirectory().toString();
			File file = chooser.getSelectedFile();
			// check to see if file exists
			if (toSave) { // saving: check if the file will be overwritten
				if (file.exists()) {
					int selected = JOptionPane.showConfirmDialog(parent,
							DisplayRes.getString("DrawingFrame.ReplaceExisting_message") + " " + file.getName() //$NON-NLS-1$ //$NON-NLS-2$
									+ DisplayRes.getString("DrawingFrame.QuestionMark"), //$NON-NLS-1$
							DisplayRes.getString("DrawingFrame.ReplaceFile_option_title"),
							JOptionPane.YES_NO_CANCEL_OPTION);
					if (selected != JOptionPane.YES_OPTION) {
						return null;
					}
				}
			} else { // Reading: check if thefile actually exists
				if (!file.exists()) {
					JOptionPane.showMessageDialog(parent,
							DisplayRes.getString("GUIUtils.FileDoesntExist") + " " + file.getName(), //$NON-NLS-1$ //$NON-NLS-2$
							DisplayRes.getString("GUIUtils.FileChooserError"), //$NON-NLS-1$
							JOptionPane.ERROR_MESSAGE);
					return null;
				}
			}
			fileName = file.getAbsolutePath();
			if ((fileName == null) || fileName.trim().equals("")) { //$NON-NLS-1$
				return null;
			}
		}
		return fileName;
	}

	/**
	 * Creates a JFileChooser with given title, description and extensions
	 * 
	 * @param title       the title
	 * @param description a description string
	 * @param extensions  an array of allowed extensions
	 * @return the JFileChooser
	 */
	static public AsyncFileChooser createChooser(String title, String description, String[] extensions) {
		AsyncFileChooser chooser = createChooser(description, extensions, null);
		chooser.setDialogTitle(title);
		return chooser;
	}

	/**
	 * Creates a JFileChooser with given description and extensions
	 * 
	 * @param description String A description string
	 * @param extensions  String[] An array of allowed extensions
	 * @return JFileChooser
	 */
	static public AsyncFileChooser createChooser(String description, String[] extensions) {
		return createChooser(description, extensions, null);
	}

	/**
	 * Creates a JFileChooser with given description and extensions
	 * 
	 * @param description String A description string
	 * @param extensions  String[] An array of allowed extensions
	 * @param homeDir     File The target directory when the user clicks the home
	 *                    icon
	 * @return JFileChooser
	 */
	static public AsyncFileChooser createChooser(String description, String[] extensions, final File homeDir) {
		AsyncFileChooser chooser = new AsyncFileChooser(new File(chooserDir));
		ExtensionFileFilter filter = new ExtensionFileFilter();
		for (int i = 0; i < extensions.length; i++) {
			filter.addExtension(extensions[i]);
		}
		filter.setDescription(description);
		if (homeDir != null) {
			chooser.setFileSystemView(new javax.swing.filechooser.FileSystemView() {
				@Override
				public File createNewFolder(File arg0) throws IOException {
					return javax.swing.filechooser.FileSystemView.getFileSystemView().createNewFolder(arg0);
				}

				@Override
				public File getHomeDirectory() {
					return homeDir;
				}

			});
		}
		chooser.setFileFilter(filter);
		FontSizer.setFonts(chooser, FontSizer.getLevel());
		return chooser;
	}

	/**
	 * This file filter matches all files with a given set of extensions.
	 */
	static private class ExtensionFileFilter extends javax.swing.filechooser.FileFilter {
		private String description = ""; //$NON-NLS-1$
		private java.util.ArrayList<String> extensions = new java.util.ArrayList<String>();

		/**
		 * Adds an extension that this file filter recognizes.
		 * 
		 * @param extension a file extension (such as ".txt" or "txt")
		 */
		public void addExtension(String extension) {
			if (!extension.startsWith(".")) { //$NON-NLS-1$
				extension = "." + extension; //$NON-NLS-1$
			}
			extensions.add(extension.toLowerCase());
		}

		@Override
		public String toString() {
			return description;
		}

		/**
		 * Sets a description for the file set that this file filter recognizes.
		 * 
		 * @param aDescription a description for the file set
		 */
		public void setDescription(String aDescription) {
			description = aDescription;
		}

		/**
		 * Returns a description for the file set that this file filter recognizes.
		 * 
		 * @return a description for the file set
		 */
		@Override
		public String getDescription() {
			return description;
		}

		@Override
		public boolean accept(File f) {
			if (f == null)
				return false;
			if (f.isDirectory()) {
				return true;
			}
			String name = f.getName().toLowerCase();
			// check if the file name ends with any of the extensions
			for (int i = 0; i < extensions.size(); i++) {
				if (name.endsWith(extensions.get(i))) {
					return true;
				}
			}
			return false;
		}

	}

	/**
	 * Cache (or clear) J2S._javaFileCache for this file.
	 * 
	 * @param file
	 * @param isAdd
	 */
	public static void cacheJSFile(File file, boolean isAdd) {
		if (isJS) {
			jsutil.cachePathData(file.getAbsolutePath(), (isAdd ? jsutil.getBytes(file) : null));
		}
	}

	/**
	 * Fetch bytes cached for temporary or DnD files in JavaScript.
	 * 
	 * @param path
	 * @return
	 */
	public static byte[] getCachedBytes(String path) {
		return (isJS ? jsutil.getCachedBytes(path) : null);
	}

	public static byte[] addJSCachedBytes(Object URLorURIorFile) {
		return (isJS ? jsutil.addJSCachedBytes(URLorURIorFile) : null);
	}

	public static boolean isJSTemp(String path) {
		return isJS && path.startsWith("/TEMP/");
	}
	
	public static void displayURL(String url) throws IOException {

		if (isJS) {
			jsutil.displayURL(url, "_blank");
		} else {
			/**
			 * totally ignore
			 * 
			 * @j2sNative
			 * 
			 * 
			 */
			{
				// try the old way
				org.opensourcephysics.desktop.ostermiller.Browser.init();
				url = url.replaceAll("\\s+", "");  //WC: ostermiller fails with leading whitespace in URL
				org.opensourcephysics.desktop.ostermiller.Browser.displayURL(url);
			}

		}
	}

	public static void showStatus(String msg) {
		if (isJS && logToJ2SMonitor)
			jsutil.showStatus(msg, true);
	}

	public static void getURLBytesAsync(URL url, Function<byte[], Void> whenDone) {
		if (allowAsyncURL) {
			jsutil.getURLBytesAsync(url, whenDone);
		} else {
			whenDone.apply(jsutil.getURLBytes(url));
		}
	}

	/**
	 * BH created this to consolidate all the isEventDispatchThread calls, but those
	 * were all using runner.run() directly, and he has had no issues with other
	 * programs. It is Swing, after all....
	 * 
	 * @param runner
	 */
	@Deprecated
	public static void postEvent(Runnable runner) {
		// BH I am not seeing any posts that would use invokeLater
		if (isJS || SwingUtilities.isEventDispatchThread()) {
			runner.run();
		} else {
			SwingUtilities.invokeLater(runner);

		}
	}

	public static void dispatchEventWait(Runnable runner) {
		if (isJS || SwingUtilities.isEventDispatchThread())
			runner.run();
		else
			try {
				SwingUtilities.invokeAndWait(runner);
			} catch (InvocationTargetException | InterruptedException e) {
				e.printStackTrace();
			}
	}

	/**
	 * Set the "app" property of the HTML5 applet object, for example,
	 * "testApplet.app", to point to the Jalview instance. This will be the object
	 * that page developers use that is similar to the original Java applet object
	 * that was accessed via LiveConnect.
	 * 
	 * @param j
	 */
	public static void setAppClass(Object j) {
		if (isJS) {
			jsutil.setAppletAttribute("app", j);
		}
	}

	public static int setTimeout(String name, int msDelay, boolean cancelPending, Runnable r) {
		return Timeout.setTimeout(name, msDelay, cancelPending, r);
	}

	/**
	 * clean up all pending anything
	 * 
	 */
	public static void exit() {
		Timeout.cancelTimeoutsByName(null);
	}

	public static Clipboard getClipboard() {
		return Toolkit.getDefaultToolkit().getSystemClipboard();
	}

	/**
	 * SwingJS may not have all UI actions already defined
	 * 
	 * @param im
	 * @param ks
	 * @param actionKey
	 * @param am
	 * @param pasteAction
	 */
	public static void setOSPAction(InputMap im, KeyStroke ks, String actionKey, ActionMap am, Action pasteAction) {
		Object key = im.get(ks);
		if (key == null) {
			im.put(ks, key = actionKey);
		}
		am.put(key, pasteAction);
	}

	/**
	 * Pastes from the clipboard and returns the pasted string.
	 *
	 * @return the pasted string, or null if none
	 */
	public static String paste(Consumer<String> whenDone) {
		if (isJS) {
			if (whenDone != null)
				jsutil.getClipboardText(whenDone);
			return null;
		}
		Transferable data = null;
		try {
			Clipboard clipboard = getClipboard();
			data = clipboard.getContents(null);
		} catch (Exception e) {
		}
		if ((data != null) && data.isDataFlavorSupported(DataFlavor.stringFlavor)) {
			try {
				String s = (String) data.getTransferData(DataFlavor.stringFlavor);
				if (whenDone != null)
					whenDone.accept(s);
				return s;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return null;
	}

	public static void copy(String s, ClipboardOwner owner) {
		StringSelection stringSelection = new StringSelection(s);
		getClipboard().setContents(stringSelection, owner == null ? stringSelection : owner);
	}

	/**
	 * Register a TransferHandler for the onpaste event for this component. This
	 * registration will consume the jQuery paste event. The returned MimeType is
	 * text/plain and will match DataFlavor.plainTextFlavor
	 * 
	 * @param c
	 * @param handler
	 */
	public static void setJSClipboardPasteListener(Component c, TransferHandler handler) {
		jsutil.setPasteListener(c, handler);
	}

	public static void addAssets(String name, String zipPath, String path) {
		if (useZipAssets) {
			try {
				Object val = (isJS ? jsutil.getAppletInfo("assets") : null);
				if (val == null)
					val = "DEFAULT";
				if ((val instanceof String)) {
					// assets String parameter defined - JavaScript only
					switch (((String) val).toUpperCase()) {
					case "DEFAULT":
						if (!isJS) {
							zipPath = OSPRuntime.class.getClassLoader().getResource(zipPath).toString();
						}
						Assets.add(new Assets.Asset(name, zipPath, path));
						break;
					case "NONE":
						// JavaScript only
						break;
					default:
						// JavaScript only
						Assets.add(val);
						break;
					}
				} else {
					Assets.add(val);
				}
			} catch (Throwable e) {
				OSPLog.warning("Error reading assets path. ");
				System.err.println("Error reading assets path.");
			}
		}
	}

	/**
	 * Get the used and max from jvaa.lang.management.memoryMXBean.
	 * 
	 * @return [ used, max ]
	 */
	public static long[] getMemory() {
		if (isJS)
			return new long[] { 0, Long.MAX_VALUE };
		java.lang.management.MemoryMXBean memory = java.lang.management.ManagementFactory.getMemoryMXBean();
		return new long[] { memory.getHeapMemoryUsage().getUsed() / (1024 * 1024),
				memory.getHeapMemoryUsage().getMax() / (1024 * 1024) };
	}

	public static String getMemoryStr() {
		if (isJS)
			return "";
		long[] m = getMemory();
		return m[0] + "/" + m[1];
	}

	/**
	 * Create a simple one-time Timer and start it. 
	 * @param ms
	 * @param a
	 * @return
	 */
	public static Timer trigger(int ms, ActionListener a) {
		Timer timer = new Timer(ms, a);
		timer.setRepeats(false);
		timer.start();
		return timer;
	}

	/**
	 * Displays a JColorChooser and returns the selected color.
	 *
	 * @param color the initial color to select
	 * @param title the title for the dialog
	 * @param whenDone TODO
	 * @return the newly selected color. or initial color if cancelled
	 */
	public static void chooseColor(final Color color, String title, Consumer<Color> whenDone) {
		final JColorChooser chooser = new JColorChooser();
		chooser.setColor(color);
		ActionListener cancelListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				chooser.setColor(color);
			}
		};
		ActionListener okListener = (e) -> {
			whenDone.accept(chooser.getColor());
		};
		JDialog dialog = JColorChooser.createDialog(null, title, true, chooser, okListener, cancelListener);
		FontSizer.setFonts(dialog, FontSizer.getLevel());
		dialog.setVisible(true);
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
