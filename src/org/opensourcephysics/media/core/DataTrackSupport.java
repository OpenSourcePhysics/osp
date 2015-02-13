package org.opensourcephysics.media.core;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.Timer;
import javax.swing.event.SwingPropertyChangeSupport;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.display.Data;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.LocalJob;
import org.opensourcephysics.tools.Tool;

/**
 *  A class to support the RMI transfer of data to the DataTrackTool.
 *
 * @author Douglas Brown
 */
public class DataTrackSupport {

	public static final String HOST = "localhost"; //$NON-NLS-1$
	public static final int PORT = 1099;
	private static Tool remoteTool;
	private static Timer timer;
  private static PropertyChangeSupport support = new SwingPropertyChangeSupport(new Object());
  
  /**
   * Gets a remote copy of the tracker.DataTrackTool.
   *
   * @return the Tool, or null if not available
   */
  public static Tool getRemoteTool() {
		if (remoteTool!=null) return remoteTool;
		try {
			Registry registry = LocateRegistry.getRegistry(HOST, PORT);
			remoteTool = (Tool)registry.lookup("DataTrackTool"); //$NON-NLS-1$
			return remoteTool;
		} catch (Exception ex) {
		}
  	return null;
  }
  
  /**
   * Sends data to the remote DataTrackTool. Data must include (x, y) positions.
   *
   * @param data the data to send
   * @param replyTo a Tool to which the DataTrackTool can reply (may be null)
   * @return true if the data was sent successfully
   */
  public static boolean sendData(Data data, Tool replyTo) {
  	Tool tool = getRemoteTool();
  	if (tool==null) return false;
  	try {
			tool.send(new LocalJob(data), replyTo);
			return true;
		} catch (RemoteException e) {
		}
  	return false;
  }
  
  /**
   * Determines if data was requested (eg by Tracker) when the current process was started.
   *
   * @return true if data was requested
   */
  public static boolean isDataRequested() {
  	return System.getenv("DATA_REQUESTED")!=null; //$NON-NLS-1$
  }
  
  /**
   * Determines if Tracker is available to receive data on this machine.
   * This returns true if a tracker.jar file is found in the OSP preference
   * TRACKER_HOME. Requires Tracker version 5.00 or above.
   *
   * @return true if Tracker is available (whether or not it is running)
   */
  public static boolean isTrackerAvailable() {
		String trackerHome = (String)OSPRuntime.getPreference("TRACKER_HOME"); //$NON-NLS-1$
		if (trackerHome==null) {
			return false;
		}
		File file = new File(trackerHome, "tracker.jar"); //$NON-NLS-1$
		if (!file.exists()) {
			return false;
		}
		return true;
  }
  
  /**
   * Launches Tracker and waits for it to register a DataTrackTool.
   * The returns true as soon as Tracker is launched. When the tool is 
   * registered and available, a PropertyChangeListener is notified.  
   *
   * @param listener a PropertyChangeListener to notify when the tool is registered
   * @return true if Tracker was launched
   */
  public static boolean launchTracker(final PropertyChangeListener listener) {
  	if (!isTrackerAvailable()) return false;
  	
	  support.addPropertyChangeListener(listener);
	  
  	// if available, return true and inform listener
  	if (getRemoteTool()!=null) {
  	  support.firePropertyChange("DataTrackTool", null, remoteTool); //$NON-NLS-1$
  		return true;
  	}
  	
  	// launch Tracker in separate VM and request that it register the DataTrackTool
  	
		// get the path to the tracker.jar file
		String trackerHome = (String)OSPRuntime.getPreference("TRACKER_HOME"); //$NON-NLS-1$
		String trackerPath = new File(trackerHome, "tracker.jar").getAbsolutePath(); //$NON-NLS-1$
		
		// assemble the command
		final ArrayList<String> cmd = new ArrayList<String>();
		cmd.add("java"); //$NON-NLS-1$
		cmd.add("-classpath"); //$NON-NLS-1$
		cmd.add(trackerPath);
		cmd.add("org.opensourcephysics.cabrillo.tracker.deploy.TrackerStarter"); //$NON-NLS-1$

		// create ProcessBuilder to execute the command
		final ProcessBuilder builder = new ProcessBuilder(cmd);
		
		// set environment variable
		Map<String, String> env = builder.environment();
		env.put("DATA_TRACK_TOOL_REQUESTED", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		
		// log the command
		String message = "launching Tracker with command "; //$NON-NLS-1$
		for (String next: cmd) {
			message += next + " "; //$NON-NLS-1$
		}
		OSPLog.config(message);
		
		// start the process
		startProcess(builder);
									
		// start timer to look for remoteTool
		if (timer==null) {
			timer = new Timer(500, new ActionListener() {
	      public void actionPerformed(ActionEvent e) {
	      	if (remoteTool!=null) {
	      		timer.stop();
    				timer=null;
	      	  support.firePropertyChange("DataTrackTool", null, remoteTool); //$NON-NLS-1$
	      		return;
	      	}
	      	remoteTool = getRemoteTool();
	      }
      });
		}		
		timer.setInitialDelay(1000);
		timer.setRepeats(true);
		timer.start();
		return true;
  }
  
  /**
   * Determines if a jar file contains this class. Jar files that do are assumed
   * by Tracker to be models able to generate and send data to the DataTrackTool.
   *
   * @param jarPath the path to a jar file
   * @return true if the jar contains this (DataTrackSupport) class
   */
  public static boolean isDataTrackSupported(String jarPath) {
		try {
			JarFile jar = new JarFile(jarPath);
	    // iterate thru JarFile entries
	    for(Enumeration<JarEntry> en = jar.entries(); en.hasMoreElements(); ) {
	      JarEntry entry = en.nextElement();
	      String name = entry.getName();
	      // return true if DataTrackSupport class is found
	      if(name.endsWith(DataTrackSupport.class.getSimpleName()+".class")) {  //$NON-NLS-1$
	      	return true;
	      }
	    }
		} catch (IOException ex) {
		}
		return false;
  }
  
  /**
   * Launches a model class and optionally requests that it immediately send data.
   *
   * @param jarPath the path to a (model) jar file
   * @param requestData true to request data
   */
  public static void launchModel(String jarPath, boolean requestData) {
		// assemble the command
		final ArrayList<String> cmd = new ArrayList<String>();
		cmd.add("java"); //$NON-NLS-1$
		cmd.add("-jar"); //$NON-NLS-1$
		cmd.add(jarPath);

		// create ProcessBuilder to execute the command
		final ProcessBuilder builder = new ProcessBuilder(cmd);
		
		if (requestData) {
			// set DATA_REQUESTED environment variable
			Map<String, String> env = builder.environment();
			env.put("DATA_REQUESTED", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		// log the command
		String message = ""; //$NON-NLS-1$
		for (String next: cmd) {
			message += next + " "; //$NON-NLS-1$
		}
		OSPLog.config(message);
							
		// start the process
		startProcess(builder);
  }
  
  /**
   * Starts a ProcessBuilder and handles its output and error streams.
   *
   * @param builder the ProcessBuilder
   */
  private static void startProcess(final ProcessBuilder builder) {
		// start the process and wait for it to finish
		Runnable runner = new Runnable() {
			public void run() {
				try {
					Process process = builder.start();
					// read output stream from the process--important so process doesn't block
	        InputStream is = process.getInputStream();
	        InputStreamReader isr = new InputStreamReader(is);
	        BufferedReader br = new BufferedReader(isr);
	        String line;
	        while ((line = br.readLine()) != null) {
	            System.out.println(line);
	        }
			    br.close();
	        
					int result = process.waitFor();
					// if process returns with exit code > 0, print it's error stream
					if (result > 0) {
						isr = new InputStreamReader(process.getErrorStream());
						br = new BufferedReader(isr);
		        while ((line = br.readLine()) != null) {
	            System.err.println(line);
		        }
				    br.close();
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}					
			}
		};
		
		new Thread(runner).start();  	
  }
  
}
