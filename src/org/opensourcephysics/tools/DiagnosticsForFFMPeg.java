package org.opensourcephysics.tools;

import java.io.File;
import java.lang.reflect.Method;
import java.text.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;

import javax.swing.*;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.media.core.VideoIO;

/**
 * Checks to see if FFMPeg is installed and working.
 * 
 * @author Wolfgang Christian
 * @author Douglas Brown
 * @version 1.0
 */
public class DiagnosticsForFFMPeg {

	@SuppressWarnings("javadoc")
	public static final String FFMPEG_URL = "http://www.compadre.org/osp/items/detail.cfm?ID=11606"; //$NON-NLS-1$
	public static final String BRIDJVERSION = "0.7-SNAPSHOT";
	static String newline = System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
	static String[] ffmpegJarNames = new String[] {
			"ffmpeg.jar", "bridj-" + BRIDJVERSION + "-unix-only.jar" }; //$NON-NLS-1$ //$NON-NLS-2$
	static int vmBitness;
	static String javaExtDirectory;
	static String pathEnvironment, pathValue, ffmpegHome;
	static String requester;
	static File[] ffmpegHomeJars, javaExtensionJars;

	static { // added by W. Christian
		try {
			String name = "org.opensourcephysics.media.ffmpeg.FFMPegIO"; //$NON-NLS-1$
			Class<?> ffmpegClass = Class.forName(name);
			Method method = ffmpegClass.getMethod("registerWithVideoIO"); //$NON-NLS-1$
			method.invoke(null, (Object[]) null);
		} catch (Exception ex) {
		} catch (Error err) {
		}

		vmBitness = OSPRuntime.getVMBitness();

		ffmpegHomeJars = null;
		javaExtensionJars = null;

	}

	private DiagnosticsForFFMPeg() {
	}

	/**
	 * Displays the About FFMPeg dialog. If working correctly, shows version,
	 * etc. If not working, shows a diagnostic message.
	 */
	public static void aboutFFMPeg() {

		int status = getStatusCode();

		if (OSPLog.getLevelValue() <= Level.CONFIG.intValue()) {
			String ffmpegHome = System.getenv("XUGGLE_HOME"); //$NON-NLS-1$
			OSPLog.config("FFMPEG_HOME = " + ffmpegHome); //$NON-NLS-1$
			OSPLog.config(pathEnvironment + " = " + pathValue); //$NON-NLS-1$

			// log ffmpeg jars
			File[] ffmpegJars = getFFMPegJarFiles(ffmpegHome); //$NON-NLS-1$
			boolean hasAllHomeJars = ffmpegJars[0] != null;
			for (int i = 1; i < ffmpegJars.length; i++) {
				hasAllHomeJars = hasAllHomeJars && ffmpegJars[i] != null;
			}
			SimpleDateFormat sdf = new SimpleDateFormat();
			String[] jarDates = new String[ffmpegJarNames.length];
			for (int i = 0; i < jarDates.length; i++) {
				jarDates[i] = ffmpegJars[i] == null ? "" : " modified " + sdf.format(ffmpegJars[i].lastModified()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			String[] jarSizes = new String[ffmpegJarNames.length];
			for (int i = 0; i < jarSizes.length; i++) {
				jarSizes[i] = ffmpegJars[i] == null ? "" : " (file size " + (ffmpegJars[i].length() / 1024) + "kB) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			String fileData = "XUGGLE_HOME FFMPeg files: "; //$NON-NLS-1$
			for (int i = 0; i < jarSizes.length; i++) {
				if (i > 0)
					fileData += ", "; //$NON-NLS-1$
				fileData += ffmpegJarNames[i]
						+ " " + jarSizes[i] + ffmpegJars[i] + jarDates[i]; //$NON-NLS-1$
			}
			OSPLog.config(fileData);

			// log current java VM and extension jars
			String javaHome = System.getProperty("java.home"); //$NON-NLS-1$
			String bitness = "(" + vmBitness + "-bit): "; //$NON-NLS-1$ //$NON-NLS-2$
			OSPLog.config("Java VM " + bitness + javaHome); //$NON-NLS-1$
			ffmpegJars = getJavaExtensionJars();
			for (int i = 0; i < jarDates.length; i++) {
				jarDates[i] = ffmpegJars[i] == null ? "" : " modified " + sdf.format(ffmpegJars[i].lastModified()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			for (int i = 0; i < jarSizes.length; i++) {
				jarSizes[i] = ffmpegJars[i] == null ? "" : " (file size " + (ffmpegJars[i].length() / 1024) + "kB) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			fileData = "Java extension FFMPeg files: "; //$NON-NLS-1$
			for (int i = 0; i < jarSizes.length; i++) {
				if (i > 0)
					fileData += ", "; //$NON-NLS-1$
				fileData += ffmpegJarNames[i]
						+ " " + jarSizes[i] + ffmpegJars[i] + jarDates[i]; //$NON-NLS-1$
			}
			OSPLog.config(fileData);

			// log tracker home jars on Windows
			  if (OSPRuntime.isWindows()) {
					String trackerHome = System.getenv("TRACKER_HOME"); //$NON-NLS-1$
				  OSPLog.config("TRACKER_HOME = "+trackerHome); //$NON-NLS-1$
				  ffmpegJars =  getFFMPegJarFiles(trackerHome);
					for (int i=0; i< jarDates.length; i++) {
						jarDates[i] = ffmpegJars[i]==null? "": " modified "+sdf.format(ffmpegJars[i].lastModified()); //$NON-NLS-1$ //$NON-NLS-2$
					}
					for (int i=0; i< jarSizes.length; i++) {
						jarSizes[i] = ffmpegJars[i]==null? "": " (file size "+(ffmpegJars[i].length()/1024)+"kB) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					fileData = "TRACKER_HOME FFMPeg files: ";			 //$NON-NLS-1$
					for (int i=0; i< jarSizes.length; i++) {
						if (i>0) fileData+=", "; //$NON-NLS-1$
						fileData+=ffmpegJarNames[i]+" "+jarSizes[i]+ffmpegJars[i]+jarDates[i]; //$NON-NLS-1$
					}
				  OSPLog.config(fileData);
			  }

		}

		if (status==0) { // ffmpeg working correctly
			ffmpegHome = " " + ffmpegHome + newline; //$NON-NLS-1$
			String fileInfo = newline;
			String path = " "+FFMPegRes.getString("FFMPeg.Dialog.Unknown"); //$NON-NLS-1$ //$NON-NLS-2$
			if (javaExtensionJars[0]!=null) {
				DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);
				Date date = new Date(javaExtensionJars[0].lastModified());
				fileInfo = " (" + format.format(date) + ")" + newline; //$NON-NLS-1$ //$NON-NLS-2$
				path = " " + javaExtensionJars[0].getAbsolutePath(); //$NON-NLS-1$
			}
			String version = getFFMPegVersion();
			String message = FFMPegRes.getString("FFMPeg.Dialog.AboutFFMPeg.Message.Version") //$NON-NLS-1$
					+ " " + version + fileInfo //$NON-NLS-1$
					+ FFMPegRes.getString("FFMPeg.Dialog.AboutFFMPeg.Message.Home") //$NON-NLS-1$
					+ ffmpegHome + newline
					+ FFMPegRes.getString("FFMPeg.Dialog.AboutFFMPeg.Message.Path") //$NON-NLS-1$ 
					+ path;
			JOptionPane.showMessageDialog(null,
					message,
					FFMPegRes.getString("FFMPeg.Dialog.AboutFFMPeg.Title"), //$NON-NLS-1$
					JOptionPane.INFORMATION_MESSAGE);
		}
		
		else { // ffmpeg not working
    	String[] diagnostic = getDiagnosticMessage(status, requester);
    	Box box = Box.createVerticalBox();
    	box.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
    	for (String line: diagnostic) {    			
  			box.add(new JLabel(line));
  		}      	
    	JOptionPane.showMessageDialog(null, box,
    			FFMPegRes.getString("FFMPeg.Dialog.BadFFMPeg.Title"),  //$NON-NLS-1$
    			JOptionPane.WARNING_MESSAGE);
		}

	}

	/**
	 * Displays the About FFMPeg dialog for Tracker or other requester.
	 * 
	 * @param request
	 *            currently only "Tracker" is supported
	 */
	public static void aboutFFMPeg(String request) {
		requester = request;
		aboutFFMPeg();
	}

	/**
	 * Gets the ffmpeg jar file (named in ffmpegJarName) found in a given
	 * directory. Always returns the file but may be null.
	 * 
	 * @param dir
	 *            the directory
	 * @return the jar file found
	 */
	public static File[] getFFMPegJarFiles(String dir) {
		// look for ffmpeg and support jars in the directory
		File[] jarFiles = new File[ffmpegJarNames.length];
		for (int i = 0; i < jarFiles.length; i++) {
			String next = ffmpegJarNames[i];
			File file = new File(dir, next);
			jarFiles[i] = file.exists() ? file : null;
		}
		return jarFiles;
	}

	/**
	 * Gets the ffmpeg jar file (named in ffmpegJarName) found in the current
	 * Java extension directory. Always returns file may be null.
	 * 
	 * @return the jar file found
	 */
	public static File[] getJavaExtensionJars() {

		File[] ffmpegFiles = new File[ffmpegJarNames.length];

		// look for ffmpeg jars in system extensions directories
		String extFolders = XML.forwardSlash(System
				.getProperty("java.ext.dirs")); //$NON-NLS-1$
		String separator = System.getProperty("path.separator"); //$NON-NLS-1$
		int n = extFolders.indexOf(separator);
		if (n == -1) { // no separators, so single path
			javaExtDirectory = extFolders;
			ffmpegFiles = getFFMPegJarFiles(extFolders);
			if (ffmpegFiles[0] != null) {
				return ffmpegFiles;
			}
		} else {
			String dir = extFolders;
			javaExtDirectory = null;
			while (ffmpegFiles[0] == null && n > -1) {
				dir = extFolders.substring(0, n);
				if (javaExtDirectory == null)
					javaExtDirectory = dir; // first one in list by default
				extFolders = extFolders.substring(n + 1);
				ffmpegFiles = getFFMPegJarFiles(dir);
				if (ffmpegFiles[0] != null) {
					javaExtDirectory = dir;
					return ffmpegFiles;
				}
				n = extFolders.indexOf(separator);
			}
		}

		return ffmpegFiles;
	}

	/**
	 * Gets a status code that identifies the current state of the FFMPeg video
	 * engine. Codes are: 0 working correctly 1 not installed
	 * (FFMPEG_HOME==null, no ffmpeg jar in current java extensions directory) 2
	 * needs reboot/login? (FFMPEG_HOME==null, ffmpeg jar in current java
	 * extensions directory) 3 FFMPEG_HOME defined but no ffmpeg jar in ffmpeg
	 * home 4 FFMPEG_HOME complete, but incorrect "PATH", "DYLD_LIBRARY_PATH",
	 * or "LD_LIBRARY_PATH" 5 FFMPEG_HOME complete, but missing ffmpeg jars in
	 * current java extensions directory 6 FFMPEG_HOME complete, but mismatched
	 * ffmpeg.jar in current java extensions directory 7 FFMPEG_HOME complete,
	 * but wrong Java VM bitness -1 none of the above
	 * 
	 * @return status code
	 */
	public static int getStatusCode() {
		javaExtensionJars = getJavaExtensionJars();
		pathEnvironment = OSPRuntime.isWindows() ? "PATH" : OSPRuntime.isMac() ? "DYLD_LIBRARY_PATH" : "LD_LIBRARY_PATH"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		pathValue = System.getenv(pathEnvironment);
		ffmpegHome = System.getenv("FFMPEG_HOME");

		// return 0 if working correctly
		if (VideoIO.getVideoType(VideoIO.ENGINE_FFMPEG, null) != null)
			return 0;

		boolean completeExt = javaExtensionJars[0]!=null;
		for (int i=1; i< javaExtensionJars.length; i++) {
			completeExt = completeExt && javaExtensionJars[i]!=null;
		}

		if (ffmpegHome == null)
			return completeExt ? 2 : 1;

		ffmpegHomeJars = getFFMPegJarFiles(ffmpegHome); //$NON-NLS-1$
		boolean completeHome = ffmpegHomeJars[0]!=null;
		for (int i=1; i< ffmpegHomeJars.length; i++) {
			completeHome = completeHome && ffmpegHomeJars[i]!=null;
		}

		if (!completeHome)
			return 3;
		if (javaExtensionJars[0] == null)
			return 5;

		long homeLength = ffmpegHomeJars[0].length();
		boolean mismatched = homeLength != javaExtensionJars[0].length();
		if (mismatched)
			return 6;
		if (!completeExt)
			return 5;

		if (vmBitness == 32 && OSPRuntime.isMac())
			return 7;

		return -1;
	}

	/**
	 * Gets a diagnostic message when FFMPeg is not working.
	 * 
	 * @param status
	 *            the status code from getStatusCode() method
	 * @return an array strings containing the message lines
	 */
	public static String[] getDiagnosticMessage(int status, String requester) {

		if (status == 0)
			return null;

		ArrayList<String> message = new ArrayList<String>();
		switch (status) {
		case 1: // not installed (XUGGLE_HOME==null, incomplete ffmpeg jars in
				// current java extensions directory)
			message.add(FFMPegRes.getString("FFMPeg.Dialog.NoFFMPeg.Message1")); //$NON-NLS-1$
			message.add(" "); //$NON-NLS-1$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.AboutFFMPeg.Message.InstallerPath")); //$NON-NLS-1$
			message.add(FFMPEG_URL);
			break;

		case 2: // needs reboot/login? (XUGGLE_HOME==null, complete ffmpeg jars
				// in current java extensions directory)
			message.add(FFMPegRes.getString("FFMPeg.Dialog.BadFFMPeg.Message")); //$NON-NLS-1$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.NoFFMPegHome.Message1")); //$NON-NLS-1$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.NoFFMPegHome.Message2")); //$NON-NLS-1$
			message.add(" "); //$NON-NLS-1$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.AboutFFMPeg.Message.InstallerPath")); //$NON-NLS-1$
			message.add(FFMPEG_URL);
			break;

		case 3: // XUGGLE_HOME defined but incomplete ffmpeg jars in ffmpeg home
			message.add(FFMPegRes.getString("FFMPeg.Dialog.BadFFMPeg.Message")); //$NON-NLS-1$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.IncompleteFFMPeg.Message1")); //$NON-NLS-1$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.IncompleteFFMPeg.Message2")); //$NON-NLS-1$
			message.add(" "); //$NON-NLS-1$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.AboutFFMPeg.Message.InstallerPath")); //$NON-NLS-1$
			message.add(FFMPEG_URL);
			break;

		case 4: // FFMPEG HOME complete, but incorrect "PATH",
				// "DYLD_LIBRARY_PATH", or "LD_LIBRARY_PATH"
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.MissingEnvironmentVariable.Message1")); //$NON-NLS-1$
			message.add("\"" + pathEnvironment + "\" " + FFMPegRes.getString("FFMPeg.Dialog.MissingEnvironmentVariable.Message2")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.MissingEnvironmentVariable.Message3")); //$NON-NLS-1$
			break;

		case 5: // FFMPEG HOME complete, but missing ffmpeg jar in current java
				// extensions directory
			String missingJars = ""; //$NON-NLS-1$
			for (int i=0; i<ffmpegJarNames.length; i++) {
				if (javaExtensionJars[i]==null) {
					if (missingJars.length()>1) missingJars += ", "; //$NON-NLS-1$
					missingJars += ffmpegJarNames[i];
				}
			}
			String source = XML.forwardSlash(ffmpegHome); //$NON-NLS-1$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.MissingJarsInExt.Message1")); //$NON-NLS-1$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.MissingJarsInExt.Message2")); //$NON-NLS-1$
			message.add(" "); //$NON-NLS-1$
			message.add(missingJars);
			message.add(" "); //$NON-NLS-1$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.MissingJarsInExt.Message3")); //$NON-NLS-1$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.MissingJarsInExt.Message4")); //$NON-NLS-1$
			message.add(" "); //$NON-NLS-1$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.SourceDirectory.Message") + " " + source); //$NON-NLS-1$ //$NON-NLS-2$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.ExtensionDirectory.Message") + " " + javaExtDirectory); //$NON-NLS-1$ //$NON-NLS-2$
			break;

		case 6: // FFMPEG HOME complete, but mismatched ffmpegr.jar in current
				// java extensions directory
			source = "/usr/share/java"; //$NON-NLS-1$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.MismatchedJar.Message1")); //$NON-NLS-1$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.MismatchedJar.Message2")); //$NON-NLS-1$
			message.add(" "); //$NON-NLS-1$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.MismatchedJar.Message3")); //$NON-NLS-1$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.MismatchedJar.Message4")); //$NON-NLS-1$
			message.add(" "); //$NON-NLS-1$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.SourceDirectory.Message") + " " + source); //$NON-NLS-1$ //$NON-NLS-2$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.ExtensionDirectory.Message") + " " + javaExtDirectory); //$NON-NLS-1$ //$NON-NLS-2$
			break;

		case 7: // FFMPEG HOME complete, but wrong Java VM bitness
			if (OSPRuntime.isMac()) {
				// wrong VM on Mac: should be 64-bit
				message.add(FFMPegRes
						.getString("FFMPeg.Dialog.WrongVMMac.Message1")); //$NON-NLS-1$
				message.add(FFMPegRes
						.getString("FFMPeg.Dialog.WrongVMMac.Message2")); //$NON-NLS-1$
				if (requester.equals("Tracker")) { //$NON-NLS-1$
					message.add(" "); //$NON-NLS-1$
					message.add(FFMPegRes
							.getString("FFMPeg.Dialog.WrongVMMac.Message3")); //$NON-NLS-1$
					message.add(FFMPegRes
							.getString("FFMPeg.Dialog.WrongVMMac.Message4")); //$NON-NLS-1$
				}
			} else {
				// wrong VM on Windows
				message.add(FFMPegRes
						.getString("FFMPeg.Dialog.WrongVMWindows.Message1")); //$NON-NLS-1$
				message.add(FFMPegRes
						.getString("FFMPeg.Dialog.WrongVMWindows.Message2")); //$NON-NLS-1$
				message.add(" "); //$NON-NLS-1$

				Collection<String> jreDirs = ExtensionsManager.getManager()
						.getPublicJREs(32);
				if (jreDirs.isEmpty()) {
					if (requester.equals("Tracker")) { //$NON-NLS-1$
						message.add(FFMPegRes
								.getString("FFMPeg.Dialog.NoVMTracker.Message1")); //$NON-NLS-1$
						message.add(FFMPegRes
								.getString("FFMPeg.Dialog.NoVMTracker.Message2")); //$NON-NLS-1$							
					} else {
						message.add(FFMPegRes
								.getString("FFMPeg.Dialog.NoVM.Message1")); //$NON-NLS-1$
						message.add(FFMPegRes
								.getString("FFMPeg.Dialog.NoVM.Message2")); //$NON-NLS-1$
						message.add(FFMPegRes
								.getString("FFMPeg.Dialog.NoVM.Message3")); //$NON-NLS-1$
						message.add(FFMPegRes
								.getString("FFMPeg.Dialog.NoVM.Message4")); //$NON-NLS-1$
					}
				} else {
					message.add(FFMPegRes
							.getString("FFMPeg.Dialog.SetVM.Message1")); //$NON-NLS-1$
					message.add(FFMPegRes
							.getString("FFMPeg.Dialog.SetVM.Message2")); //$NON-NLS-1$
				}
			}
			break;

		default: // none of the above
			message.add(FFMPegRes.getString("FFMPeg.Dialog.BadFFMPeg.Message")); //$NON-NLS-1$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.UnknownProblem.Message")); //$NON-NLS-1$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.NoFFMPegHome.Message2")); //$NON-NLS-1$
			message.add(" "); //$NON-NLS-1$
			message.add(FFMPegRes
					.getString("FFMPeg.Dialog.AboutFFMPeg.Message.InstallerPath")); //$NON-NLS-1$
			message.add(FFMPEG_URL);

		}

		return message.toArray(new String[message.size()]);
	}

	/**
	 * Gets the FFMPeg version as a String. Returns "Unknown' if FFMPeg is
	 * missing or unidentified.
	 * 
	 * @return FFMPeg version
	 */
	public static String getFFMPegVersion() {
		String ffmpegVersion = FFMPegRes.getString("FFMPeg.Dialog.Unknown"); //$NON-NLS-1$
		int status = getStatusCode();
		if (status == 0) { // ffmpeg working correctly
			try {
				String name = "org.ffmpeg.FFMPeg"; //$NON-NLS-1$
				Class<?> ffmpegClass = Class.forName(name);
				Method method = ffmpegClass.getMethod("getVersion"); //$NON-NLS-1$
				ffmpegVersion = (String) method.invoke(null, (Object[]) null);
			} catch (Exception ex) {
			} catch (Error err) {
			}
		}
		return ffmpegVersion;
	}

	/**
	 * Tests this class.
	 * 
	 * @param args
	 *            ignored
	 */
	public static void main(String[] args) {
		System.out.println(getFFMPegVersion());
		// aboutFFMPeg();
	}
}
