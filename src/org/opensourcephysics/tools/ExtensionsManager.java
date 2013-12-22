package org.opensourcephysics.tools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JOptionPane;
import javax.swing.Timer;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.display.OSPRuntime;

	/**
	 * ExtensionsManager manages Java extensions directories.
	 * Its primary use is to copy Xuggle jars and QTJava.zip into appropriate ext directories.
	 * 
	 * @author Douglas Brown
	 * @version 1.0
	 */
public class ExtensionsManager {

	private static final ExtensionsManager MANAGER = new ExtensionsManager();
	private static boolean isReady = false;
	private static Set<File> allJavaExtensionDirectories = new TreeSet<File>();
	private static int vmsFound = 0;
	private static Timer timer;
	
	String xuggleHome;
	ExtensionsFilter extFilter;	
	
	/**
	 * Main method when used as a stand-alone application.
	 * @param args ignored
	 */
	public static void main(String[] args) {
    // set up timer to allow the user to cancel searching every 30 seconds
    timer = new Timer(30000, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int selected = JOptionPane.showConfirmDialog(null,
        		ToolsRes.getString("ExtensionsManager.Dialog.SlowSearch.Message1")+"\n"+ //$NON-NLS-1$ //$NON-NLS-2$
        		ToolsRes.getString("ExtensionsManager.Dialog.SlowSearch.Message2")+" "+vmsFound+".\n"+  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        		ToolsRes.getString("ExtensionsManager.Dialog.SlowSearch.Message3"),  //$NON-NLS-1$
        		ToolsRes.getString("ExtensionsManager.Dialog.SlowSearch.Title"), //$NON-NLS-1$
              JOptionPane.YES_NO_OPTION);
          if(selected==JOptionPane.NO_OPTION) {
          	// print whatever has been found
          	synchronized(allJavaExtensionDirectories) {
          		printExtensionDirectoriesForBitrock(allJavaExtensionDirectories);
          	}
  	        System.exit(0);
          }
          else timer.restart();
      }
    });
    timer.setRepeats(true);
    timer.start();
    
		// print list of extensions to stdout for use by Bitrock installers
		Set<File> extDirs = getManager().findJavaExtensionDirectories();
		printExtensionDirectoriesForBitrock(extDirs);
		System.exit(0);
	}
	
	/**
	 * Gets the singleton ExtensionManager.
	 * @return the ExtensionManager
	 */
	public static ExtensionsManager getManager() {
		return MANAGER;		
	}
	
	public static boolean isReady() {
		return isReady;
	}
	
	/**
	 * Private constructor.
	 */
	private ExtensionsManager() {
		xuggleHome = System.getenv("XUGGLE_HOME"); //$NON-NLS-1$
		extFilter = new ExtensionsFilter();				
	}
	
  /**
   * Finds extension directories and prints a space-delimited list to System.out. 
   * A single space delimiter is parsable by Bitrock installers.
   */
	private static void printExtensionDirectoriesForBitrock(Set<File> extDirs) {
		String separator = " "; //$NON-NLS-1$
		StringBuffer buf = new StringBuffer(2);
		for (File next: extDirs) {
			String fileName = XML.forwardSlash(next.getPath());
			buf.append("\""+fileName+"\""+separator); //$NON-NLS-1$ //$NON-NLS-2$
		}
		String s = buf.toString();
		// remove last separator
		if (s.length()>=separator.length()) {
			s = s.substring(0, s.length()-separator.length());
		}
		System.out.print(s);
	}
	
  /**
   * Copies Xuggle jar files to a target directory. Does nothing if the directory
   * already contains a xuggle-xuggler.jar of the same size.
   *
   * @param dir the directory
   * @return true if jars are copied
   */
	public boolean copyXuggleJarsTo(File dir) {
		if (xuggleHome==null || dir==null) {
			return false;
		}
	  if (!new File(xuggleHome+"/share/java/jars/xuggle-xuggler.jar").exists()) { //$NON-NLS-1$
	  	return false;
	  }
    File xuggleJarDir = new File(xuggleHome+"/share/java/jars"); //$NON-NLS-1$
    String[] jarNames = DiagnosticsForXuggle.xuggleJarNames;
	  File xuggleFile = new File(xuggleJarDir, jarNames[0]);
    long fileLength = xuggleFile.length();
    File extFile = new File(dir, jarNames[0]);
    // copy xuggle jars
    if (!extFile.exists() || extFile.length()!=fileLength) {
//    	if (extFile.exists()) extFile.delete();
	    for (String next: jarNames) {
	      xuggleFile = new File(xuggleJarDir, next);
	      extFile = new File(dir, next);
	      if (!copyFile(xuggleFile, extFile)) {
	      	return false;
	      }
	    }
	    // all jars were copied
      return true;
    }
		return false;
	}
	
  /**
   * Gets the xuggle-xuggler.jar from the xuggleHome directory, if it exists.
   *
   * @return xuggle-xuggler.jar
   */
	public File getXuggleJar() {
		if (xuggleHome==null) {
			return null;
		}
		File xuggleJar = new File(xuggleHome+"/share/java/jars/xuggle-xuggler.jar"); //$NON-NLS-1$
	  if (xuggleJar.exists()) {
	  	return xuggleJar;
	  }
	  return null;
	}

	
  /**
   * Copies QTJava.zip to a target directory. Does not overwrite a later version.
   *
   * @param dir the directory
   * @return true if copied
   */
	public boolean copyQTJavaTo(File dir) {
    File qtSource = getQTJavaZip(); // file to be copied
	  if (qtSource==null)	return false;
    File extFile = new File(dir, qtSource.getName());
  	long modified = qtSource.lastModified();
    // copy qtSource to directory if newer
    if (!extFile.exists() || extFile.lastModified()<modified) {
	    if (!copyFile(qtSource, extFile))	return false;
      return true;
    }
		return false;
	}
	
  /**
   * Returns the QTJava.zip file with the latest modified date.
   *
   * @return the QTJava.zip file, or null if none found
   */
	public File getQTJavaZip() {
  	String qtJarName = "QTJava.zip"; //$NON-NLS-1$
    String[] folderNames = {
    		"C:/Program Files/QuickTime/QTSystem/", //$NON-NLS-1$
    		"C:/Program Files (x86)/QuickTime/QTSystem/", //$NON-NLS-1$
    		"C:/windows/system32/", //$NON-NLS-1$
    		"C:/windows/system/", //$NON-NLS-1$
    		"C:/winNT/system32/", //$NON-NLS-1$
    		"system/library/java/extensions/"}; //$NON-NLS-1$
    // look for most recent QTJava.zip in system folders
    long modified = 0;
    File qtSource = null; // file to be returned
    for (String next: folderNames) {
      File qtFile = new File(next+qtJarName);
    	if (!qtFile.exists()) continue;
    	long date = qtFile.lastModified();
    	if (date>modified) {
    		modified = date;
    		qtSource = qtFile;
    	}     
    }
	  return qtSource;
	}
	
  /**
   * Finds all java extension directories on the current machine.
   * Win: typical jdk: Program Files\Java\jdkX.X.X_XX\jre\bin\java.exe
	 *			typical jre: Program Files\Java\jreX.X.X_XX\bin\java.exe
	 *			          or Program Files\Java\jreX\bin\java.exe
	 *			typical 32-bit jdk in 64-bit Windows: Program Files(x86)\Java\jdkX.X.X_XX\jre\bin\java.exe
	 * OSX: typical: /System/Library/Java/JavaVirtualMachines/X.X.X.jdk/Contents/Home/bin/java
	 *			symlink at: /Library/Java/Home/bin/java
	 *			also in /Library/Java/JavaVirtualMachines?
	 * Linux: typical: /usr/lib/jvm/java-X-openjdk/jre/bin/java 
	 *			  symlink at: /usr/lib/jvm/java-X.X.X-openjdk/jre/bin/java
	 *			  sun versions: java-X-sun and java-X.X.X-sun
   *
   * @return a collection of java extension directory files
   */
	private Set<File> findAllJavaExtensionDirectories() {
		if (!isReady) {
			vmsFound = 0;
	    // set of extension directories used by the running Java VM 
	    Set<String> vmExtDirs = new TreeSet<String>();
	    // set of "Java level" directories to search
	    Set<File> searchPaths = new TreeSet<File>();
	    
			try {
				// get and parse system extension directories property into vmExtDirs
				String paths = XML.forwardSlash(System.getProperty("java.ext.dirs")); //$NON-NLS-1$   
				String separator = System.getProperty("path.separator"); //$NON-NLS-1$
				int n = paths.indexOf(separator);
				while (n>-1) {
					vmExtDirs.add(paths.substring(0, n));
					paths = paths.substring(n+1);
				  n = paths.indexOf(separator);
				}
				if (!"".equals(paths)) {//$NON-NLS-1$
					vmExtDirs.add(paths);
				}
				   
				for (String next: vmExtDirs) {
					File dir = new File(next);
					if (!dir.exists()) continue;
					synchronized(allJavaExtensionDirectories) {
						allJavaExtensionDirectories.add(dir);
						vmsFound++;
					}
					
				  if (OSPRuntime.isMac()) {
						// search path: /JavaVirtualMachines
						while (dir!=null && dir.getPath().indexOf("/JavaVirtualMachines")>-1) { //$NON-NLS-1$
							if (dir.getName().equals("JavaVirtualMachines")) { //$NON-NLS-1$
								searchPaths.add(dir);
								break;
							}
							dir = dir.getParentFile();
						}
				  }
				  else if (OSPRuntime.isLinux()) {
					  // search path: /jvm
						while (dir!=null && dir.getPath().indexOf("/jvm")>-1) { //$NON-NLS-1$
							if (dir.getName().equals("jvm")) { //$NON-NLS-1$
								searchPaths.add(dir);
								break;
							}
							dir = dir.getParentFile();
						}
				  }
				}
				
				if (OSPRuntime.isWindows()) {
				  String progfiles = System.getenv("ProgramFiles"); //$NON-NLS-1$
					String w6432 = System.getenv("ProgramW6432"); //$NON-NLS-1$
					String x86 = System.getenv("ProgramFiles(x86)"); //$NON-NLS-1$
					// add Program Files (may or may not be x86) Java directory to search path if it exists
					if (progfiles!=null) {
						File file = new File(progfiles, "Java"); //$NON-NLS-1$
						if (file.exists()) searchPaths.add(file);   				
					}    		
					// add "Program Files" Java directory to search path if it exists
					if (w6432!=null) { // 64-bit Windows
						File file = new File(w6432, "Java"); //$NON-NLS-1$
						if (file.exists()) searchPaths.add(file);   				
					}    		
					// add "Program Files (x86)" Java directory to search path if it exists
					if (x86!=null) { // 64-bit Windows
						// add x86 Java directory to search path if it exists
						File file = new File(x86, "Java"); //$NON-NLS-1$
						if (file.exists()) searchPaths.add(file);   				
					}
				}
				
				// search all searchPaths and add all extensions directories found
				for (File next: searchPaths) {
					findJavaExtensionDirectories(next, allJavaExtensionDirectories);
				}
				isReady = true;
			} catch (Exception e) {
			}
		}
		return new TreeSet<File>(allJavaExtensionDirectories);
	}
	
  /**
   * Finds java extension directories for Java 1.6 or greater.
   *
   * @return a Set of java extension directory files
   */
	private Set<File> findJavaExtensionDirectories() {
    // set of all Java extension directories on this machine
    Set<File> extDirs = findAllJavaExtensionDirectories();
    // set of extension directories to remove 
    Set<File> toExclude = new TreeSet<File>();
    synchronized(extDirs) {
	    for (File next: extDirs) {
	    	if (next.getParentFile()==null || next.getParentFile().getParentFile()==null) continue; 
	    	File javaFile = next.getParentFile().getParentFile();
	    	int n = javaFile.getPath().indexOf("jdk"); //$NON-NLS-1$
	    	n = Math.max(n, javaFile.getPath().indexOf("jre")); //$NON-NLS-1$   	
	    	int oldVersion = javaFile.getPath().indexOf("1.5."); //$NON-NLS-1$
	    	oldVersion = Math.max(oldVersion, javaFile.getPath().indexOf("-5-")); //$NON-NLS-1$   	
	    	oldVersion = Math.max(oldVersion, javaFile.getPath().indexOf("1.4.")); //$NON-NLS-1$   	
	    	oldVersion = Math.max(oldVersion, javaFile.getPath().indexOf("1.3.")); //$NON-NLS-1$   	
	    	oldVersion = Math.max(oldVersion, javaFile.getPath().indexOf("1.2.")); //$NON-NLS-1$   	
	    	oldVersion = Math.max(oldVersion, javaFile.getPath().indexOf("1.3.")); //$NON-NLS-1$   	
	    	oldVersion = Math.max(oldVersion, javaFile.getPath().indexOf("1.2.")); //$NON-NLS-1$   	
	  		if (n>-1 && oldVersion>-1) {
	  			toExclude.add(next);
	  		}
	    }
	    extDirs.removeAll(toExclude);
	    vmsFound -= toExclude.size()+1;
    }
    return extDirs;
	}
	
  /**
   * Finds all java extension directory files (recursively) in a directory.
   * Extension directories are added to a collection of Files and returned.
   * Does not search symbolic links.
   *
   * @param dir the directory
   * @param extDirs the collection to add to
   * @return the collection
   */
	private Set<File> findJavaExtensionDirectories(File dir, Set<File> extDirs) {
		if (dir==null) return extDirs;
		try { // don't search symlinks
			if (!dir.getCanonicalPath().equals(dir.getAbsolutePath()))
				return extDirs;
		} catch (IOException e) {
		}
			
		// search all children contained in the directory
		String[] fileNames = dir.list();
		if (fileNames!=null && fileNames.length>0) {
			for (String next: fileNames) {
				File subDir = new File(dir, next);
				// if subdirectory is an extensions folder, add it
				if (extFilter.accept(subDir, subDir.getName())) {
					synchronized(extDirs) {
						extDirs.add(subDir);
						vmsFound++;
					}
				}
				// else search the next level down
				else {
					findJavaExtensionDirectories(subDir, extDirs);
				}
			}
		}
		return extDirs;
	}
	
  /**
   * Returns the bitness (32 or 64) of a JRE path.
   * @param jrePath the JRE path
   * @return the bitness
   */
	public boolean is32BitVM(String jrePath) {
		if (OSPRuntime.isWindows()) {					
			String x86 = System.getenv("ProgramFiles(x86)"); //$NON-NLS-1$
			if (x86!=null) {
				return jrePath.contains(x86)? true: false;
			}
			// if x86 not defined, must be a 32-bit OS and VM?
			return true;
		}
		if (OSPRuntime.isMac()) {
			// all OSX VMs can be run in 32-bit mode?
			return true;
		}
		// on linux, assume all JREs have same bitness as current VM?
		return OSPRuntime.getVMBitness()==32;
	}

	
  /**
   * Returns public JREs of a given bitness (32 or 64).
   * @param vmBitness the bitness desired
   * @return a Set of java JRE directory paths
   */
	public TreeSet<String> getPublicJREs(int vmBitness) {
		TreeSet<String> jreDirs = getAllJREs(vmBitness);
		if (OSPRuntime.isWindows()) {					
			for (Iterator<String> it=jreDirs.iterator(); it.hasNext();) {
				String next = it.next();
				if (next.indexOf("jdk")>-1) { //$NON-NLS-1$
					it.remove();
				}
			}
		}
		// log results for trouble-shooting
		StringBuffer buf =new StringBuffer(vmBitness+"-bit JREs: "); //$NON-NLS-1$
		for (String next: jreDirs) {
			buf.append(next+", "); //$NON-NLS-1$
		}
		OSPLog.fine(buf.toString());
    return jreDirs;
	}
	
  /**
   * Returns all public and private JREs of a given bitness (32 or 64).
   * @param vmBitness the bitness desired
   * @return a Set of java JRE directory paths
   */
	public TreeSet<String> getAllJREs(int vmBitness) {
    Set<File> extDirs = findJavaExtensionDirectories();
    TreeSet<String> jreDirs = new TreeSet<String>();
		try {
			String x86 = System.getenv("ProgramFiles(x86)"); //$NON-NLS-1$
			// iterate through extDirs to fill jreDirs
			for (File next: extDirs) {
				// move up two levels from lib/ext
				File javaFile = next.getParentFile().getParentFile();
				if (OSPRuntime.isWindows()) {					
					// if 32-bit VM specified, eliminate 64-bit
					if (vmBitness==32 && x86!=null && !next.getPath().contains(x86)) {
						continue;
					}
					// if 64-bit VM specified, eliminate 32-bit
					if (vmBitness==64 && x86!=null && next.getPath().contains(x86)) {
						continue;
					}

					javaFile = new File(javaFile, "bin/java.exe"); //$NON-NLS-1$
					if (!javaFile.exists()) continue;
					String jrePath = OSPRuntime.getJREPath(javaFile);
					jreDirs.add(jrePath);
				}
			  else {
					javaFile = new File(javaFile, "bin/java"); //$NON-NLS-1$
					if (!javaFile.exists()) continue;
					String jrePath = OSPRuntime.getJREPath(javaFile);
					jreDirs.add(jrePath);
			  }
			}
		} catch (Exception e) {
		}
		// log results for trouble-shooting
		StringBuffer buf =new StringBuffer(vmBitness+"-bit JREs: "); //$NON-NLS-1$
		for (String next: jreDirs) {
			buf.append(next+", "); //$NON-NLS-1$
		}
		OSPLog.config(buf.toString());
    return jreDirs;
	}
	
  /**
   * Finds the default JRE of a given bitness (32 or 64). A public JRE is returned if possible. 
   * @param vmBitness the bitness desired
   * @return the default JRE directory path, or null if none found
   */
	public String getDefaultJRE(int vmBitness) {
		// first look at public JREs
		String JRE = null;
    Set<String> jreDirs = getPublicJREs(vmBitness);
		for (String next: jreDirs) {
			// choose the last one (highest version) since they are in alpha/numerical order
			JRE = next;
		}
		// if none found, look for jdk
		if (JRE==null) {
	    jreDirs = getAllJREs(vmBitness);
			for (String next: jreDirs) {
				// choose the last one (highest version) since they are in alpha/numerical order
				JRE = next;
			}
		}
		// log results for trouble-shooting
		OSPLog.fine(vmBitness+"-bit default JRE: "+JRE); //$NON-NLS-1$
    return JRE;
	}
	
  /**
   * Copies a source file to a target file.
   *
   * @param inFile the source
   * @param outFile the target
   * @return true if successfully copied
   */
  private boolean copyFile(File inFile, File outFile) {
  	byte[] buffer = new byte[100000];
    try {
    	InputStream in = new FileInputStream(inFile);
    	OutputStream out = new FileOutputStream(outFile);
			while (true) {
				synchronized (buffer) {
					int amountRead = in.read(buffer);
					if (amountRead == -1) {
						break;
					}
					out.write(buffer, 0, amountRead);
				}
			}
			in.close();
			out.close();
			// following line sometimes fails on Windows 7??
			outFile.setLastModified(inFile.lastModified());
		}                   
    catch (IOException ex) {
    	return false;
    }
  	return true;
  }

	/**
	 * ExtensionsFilter identifies Java extensions directories.
	 * 
	 * Windows:
	 * 		typical jdk: Program Files\Java\jdkX.X.X_XX\jre\lib\ext
   *    typical jre: Program Files\Java\jreX.X.X_XX\lib\ext
   *             and Program Files\Java\jreX\lib\ext
   *		on 64-bit Windows, 32-bit VMs are in: Program Files (x86)\Java\...
   *		non-jre: Sun\Java\lib\ext.
   *		exclude: \Program Files (x86)\Java\Java3D\x.x.x\lib\ext
   *		jre search in: \Java
	 *
	 * OS X:
	 * 		typical: /System/Library/Java/JavaVirtualMachines/X.X.X.jdk/Contents/Home/lib/ext
	 *    non-jre: /Library/Java/Extensions and/or /System/Library/Java/Extensions
	 * 		jre search in: /JavaVirtualMachines
	 * 
	 * Linux:
	 * 		typical: /usr/lib/jvm/java-X-openjdk/jre/lib/ext
	 * 		      or /usr/lib/jvm/java-X.X.X-openjdk/jre/lib/ext
	 * 		      or /usr/lib/jvm/java-X-sun-X.X.X.XX/jre/lib/ext
	 * 		      or /usr/lib/jvm/java-X.X.X-sun/jre/lib/ext
	 * 		non-jre: /usr/java/packages/lib/ext
	 * 		jre search in: /jvm
	 */
  static class ExtensionsFilter implements FilenameFilter {  	
    public boolean accept(File dir, String name) {
    	if (!dir.isDirectory()) return false;
    	// standardize paths to forward slash
      String path = XML.forwardSlash(dir.getPath());
      // accept jre extensions directories on all platforms
      if (path.endsWith("/lib/ext")) { //$NON-NLS-1$
        if (path.endsWith("/jre/lib/ext")) { //$NON-NLS-1$
        	return true;
        }
        String jre = XML.getName(path.substring(0, path.length()-8));
        if (jre.indexOf("jre")>-1) //$NON-NLS-1$
        	return true;
        if (path.indexOf("jdk")>-1 && path.indexOf("/Java/")>-1) //$NON-NLS-1$ //$NON-NLS-2$
        	return true;
        if (path.indexOf("jre")>-1 && path.indexOf("/Java/")>-1) //$NON-NLS-1$ //$NON-NLS-2$
        	return true;
      }
      // accept non-jre extensions directories
      // Linux
      if (path.endsWith("java/packages/lib/ext")) return true; //$NON-NLS-1$
      // Windows
      if (path.endsWith("Java/lib/ext")) return true; //$NON-NLS-1$
      // Mac OSX
      if (path.endsWith("Java/Extensions")) return true; //$NON-NLS-1$
      return false;
    }
  }

}
