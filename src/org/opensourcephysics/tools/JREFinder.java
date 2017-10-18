package org.opensourcephysics.tools;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.display.OSPRuntime;

	/**
	 * JREFinder finds Java JRE directories.
	 * 
	 * @author Douglas Brown
	 * @version 1.0
	 */
public class JREFinder {

	private static final JREFinder JRE_FINDER = new JREFinder();
	private static boolean isReady = false, isSearching = false;
	private static TreeSet<File> allJREs = new TreeSet<File>();
	private static TreeSet<File> searchedAndFoundJRE = new TreeSet<File>();
	
	private JavaFilter javaFilter;	
	
	/**
	 * Gets the singleton JREFinder.
	 * @return the JREFinder
	 */
	public static JREFinder getFinder() {
		return JRE_FINDER;		
	}
	
	public static boolean isReady() {
		return isReady;
	}
	
//	public static void main(String[] args) {
//		TreeSet<File> jres = getManager().findJREs();
//		System.out.println("all JREs:");
//		for (File next: jres) {
//			System.out.println(next.getAbsolutePath());
//		}
//		jres = getManager().getJREs(32);
//		System.out.println();
//		System.out.println("32-bit JREs:");
//		for (File next: jres) {
//			System.out.println(next.getAbsolutePath());
//		}
//		jres = getManager().getJREs(64);
//		System.out.println();
//		System.out.println("64-bit JREs:");
//		for (File next: jres) {
//			System.out.println(next.getAbsolutePath());
//		}
//		jres = getManager().getPublicJREs(32);
//		System.out.println();
//		System.out.println("public 32-bit JREs:");
//		for (File next: jres) {
//			System.out.println(next.getAbsolutePath());
//		}
//		jres = getManager().getPublicJREs(64);
//		System.out.println();
//		System.out.println("public 64-bit JREs:");
//		for (File next: jres) {
//			System.out.println(next.getAbsolutePath());
//		}
//		System.out.println();
//		System.out.println("default 32-bit JRE: "+getManager().getDefaultJRE(32));
//		System.out.println();
//		System.out.println("default 64-bit JRE: "+getManager().getDefaultJRE(64));
//		
//	}
	
  /**
   * Determines if a JRE path points to a 32-bit VM.
   * @param jrePath the JRE path
   * @return true if 32-bit
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
			// no OSX VMs can be run in 32-bit mode?
			return false;
		}
		// on linux, assume all JREs have same bitness as current VM?
		return OSPRuntime.getVMBitness()==32;
	}

//	public TreeSet<String> getAllJREs(int vmBitness) {
//		// first look at extension directories
//    Set<File> extDirs = findJavaExtensionDirectories();
//    TreeSet<String> jreDirs = new TreeSet<String>();
//		try {
//			String x86 = System.getenv("ProgramFiles(x86)"); //$NON-NLS-1$
//			// iterate through extDirs to fill jreDirs
//			for (File next: extDirs) {
//				// move up two levels from lib/ext
//				File javaFile = next.getParentFile().getParentFile();
//				if (OSPRuntime.isWindows()) {					
//					// if 32-bit VM specified, eliminate 64-bit
//					if (vmBitness==32 && x86!=null && !next.getPath().contains(x86)) {
//						continue;
//					}
//					// if 64-bit VM specified, eliminate 32-bit
//					if (vmBitness==64 && x86!=null && next.getPath().contains(x86)) {
//						continue;
//					}
//
//					javaFile = new File(javaFile, "bin/java.exe"); //$NON-NLS-1$
//					if (!javaFile.exists()) continue;
//					// ignore symlink files
//					try {
//						if (!javaFile.getCanonicalPath().equals(javaFile.getAbsolutePath()))
//							continue;
//					} catch (IOException e) {
//					}
//
//					String jrePath = OSPRuntime.getJREPath(javaFile);
//					jreDirs.add(jrePath);
//				}
//			  else {
//					javaFile = new File(javaFile, "bin/java"); //$NON-NLS-1$
//					if (!javaFile.exists()) continue;
//					String jrePath = OSPRuntime.getJREPath(javaFile);
//					// only Apple-installed VMs (in /System/Library) support 32-bit operation
//					if (OSPRuntime.isMac() && vmBitness==32 && !jrePath.startsWith("/System/Library")) //$NON-NLS-1$
//						continue;
//					
//					jreDirs.add(jrePath);
//			  }
//			}
//		} catch (Exception e) {
//		}
//		
//		// pig reuse some code below?
//		// OSX: search for jres in /Library and /System/Library
//		// typical:  /Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home
//		// typical:  /System/Library/Java/JavaVirtualMachines/1.6.0jdk/Contents/Home
//		if (OSPRuntime.isMac()) {
//			Set<File> homeDirs = new TreeSet<File>();
//			
//			String[] libraries = new String[] {"/System/Library", "/Library"};  //$NON-NLS-1$//$NON-NLS-2$
//			for (int i=0; i<libraries.length; i++) {
//				File library = new File(libraries[i]);
//				if (library.exists()) {
//					// search children
//					String[] subDirs = library.list();
//					if (subDirs!=null && subDirs.length>0) {
//						for (String next: subDirs) {
//							File nextFile = new File(library, next);
//							if (next.contains("Java")) { //$NON-NLS-1$
//								findJREHomes(nextFile, homeDirs);
//							}
//							// if subdirectory is an extensions folder, add it
//							else if (nextFile.getName().contains("Internet Plug-Ins")) { //$NON-NLS-1$
//								String[] children = nextFile.list();
//								if (children!=null && children.length>0) {
//									for (String childName: children) {
//										if (childName.contains("Java")) { //$NON-NLS-1$
//											findJREHomes(new File(nextFile, childName), homeDirs);
//										}
//									}
//								}
//							}
//						}
//					}				
//				} // end library search
//			}
//			for (File home: homeDirs) {
//				// only Apple-installed VMs (in /System/Library) support 32-bit operation
//				if (vmBitness==32 && !home.getAbsolutePath().startsWith("/System/Library")) //$NON-NLS-1$
//					continue;
//				
//				jreDirs.add(home.getAbsolutePath());													
//			}
//		} // end OSX search
//			
//		// log results for trouble-shooting
//		StringBuffer buf =new StringBuffer(vmBitness+"-bit JREs: "); //$NON-NLS-1$
//		for (String next: jreDirs) {
//			buf.append(next+", "); //$NON-NLS-1$
//		}
//		OSPLog.fine(buf.toString());
//    return jreDirs;
//	}
	
  /**
   * Returns all public and private JREs of a given bitness (32 or 64).
   * 
	 * Windows: search in \Java
	 * 		typical 64-bit jdk: Program Files\Java\jdkX.X.X_XX\jre\bin\javaw.exe
   *    typical 64-bit jre: Program Files\Java\jreX.X.X_XX\bin\javaw.exe
   *                     or Program Files\Java\jreX\bin\javaw.exe
   *                     or Program Files\Java\jre-X\bin\javaw.exe
	 * 		typical 32-bit jre: as above, but in Program Files (x86)\Java\
	 *
	 * OS X: search in: /JavaVirtualMachines
	 * 		typical: /System/Library/Java/JavaVirtualMachines/X.X.X.jdk/Contents/Home/bin/javaw.exe pig??
	 * 
	 * Linux: search in: /jvm
	 * 		typical: /usr/lib/jvm/java-X-openjdk/jre/bin/javaw.exe
	 * 		      or /usr/lib/jvm/java-X.X.X-openjdk/jre/bin/javaw.exe
	 * 		      or /usr/lib/jvm/java-X-sun-X.X.X.XX/jre/bin/javaw.exe
	 * 		      or /usr/lib/jvm/java-X.X.X-sun/jre/bin/javaw.exe
	 *
   * @param vmBitness the bitness desired
   * @return a Set of java JRE directory paths
   */
	public TreeSet<File> getJREs(int vmBitness) {
		TreeSet<File> results = findJREs();
		for (Iterator<File> it=results.iterator(); it.hasNext();) {
			String path = it.next().getPath();
			if (vmBitness==32 && !is32BitVM(path)) {
				it.remove();
			}
			else if (vmBitness!=32 && is32BitVM(path)) {
				it.remove();
			}
		}
		return results;
	}
	
  /**
   * Finds the default JRE of a given bitness (32 or 64). A public JRE is returned if possible. 
   * 
   * @param vmBitness the bitness desired
   * @return the default JRE directory, or null if none found
   */
	public File getDefaultJRE(int vmBitness) {
		// first look at public JREs
		File JRE = null;
		TreeSet<File> jreDirs = getPublicJREs(vmBitness);
		for (File next: jreDirs) {
			// use the first one (highest version) since in reverse alphanumerical order
			if (JRE==null) JRE = next;
		}
		// if none found, look for jre associated with a jdk
		if (JRE==null) {
	    jreDirs = getJREs(vmBitness);
			for (File next: jreDirs) {
				// use the first one (highest version) since in reverse alphanumerical order
				if (JRE==null) JRE = next;
			}
		}
    return JRE;
	}
	
  /**
   * Finds all jre directories on the current machine.
   * 
   * Win: typical jdk: Program Files\Java\jdkX.X.X_XX\jre\bin\java.exe
	 *			typical jre: Program Files\Java\jreX.X.X_XX\bin\java.exe
	 *			          or Program Files\Java\jreX\bin\java.exe
	 *			          or Program Files\Java\jre-X\bin\java.exe
	 *			typical 32-bit jdk in 64-bit Windows: Program Files(x86)\Java\jdkX.X.X_XX\jre\bin\java.exe
	 * OSX: typical: /System/Library/Java/JavaVirtualMachines/X.X.X.jdk/Contents/Home/bin/java
	 *			symlink at: /Library/Java/Home/bin/java
	 *			also in /Library/Java/JavaVirtualMachines?
	 * Linux: typical: /usr/lib/jvm/java-X-openjdk/jre/bin/java 
	 *			  symlink at: /usr/lib/jvm/java-X.X.X-openjdk/jre/bin/java
	 *			  sun versions: java-X-sun and java-X.X.X-sun
   *
   * @return a set of jre files
   */
	private TreeSet<File> findJREs() {
		while (isSearching) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
			}
		}
		if (!isReady) {
			allJREs.clear();
			isSearching = true;
	    // set of "Java level" directories to search
	    Set<File> searchPaths = new TreeSet<File>();
	    
			try {
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
				if (OSPRuntime.isMac()) {
					File file = new File("/System/Library/Java"); //$NON-NLS-1$
					if (file.exists()) searchPaths.add(file);   									
					file = new File("/Library/Java"); //$NON-NLS-1$
					if (file.exists()) searchPaths.add(file);   									
				}
				if (OSPRuntime.isLinux()) {
					File file = new File("/usr/lib/jvm"); //$NON-NLS-1$
					if (file.exists()) searchPaths.add(file);   									
				}
				
				// search all searchPaths and add all JREs found
				for (File next: searchPaths) {
					findJREsInDirectory(next, allJREs);
				}
			} catch (Exception e) {
			}
			
			// pig implement for OSX and Linux
			
			isReady = true;
			isSearching = false;
			
			// log results for trouble-shooting
			StringBuffer buf =new StringBuffer("JREs found: "); //$NON-NLS-1$
			for (File next: allJREs) {
				buf.append(next.getAbsolutePath()+", "); //$NON-NLS-1$
			}
			OSPLog.fine(buf.toString());
		}
		return new TreeSet<File>(allJREs);
	}
	
  /**
   * Finds all JRE directories (recursively) in a directory and adds them to a Set.
   * Does not search symbolic links.
   *
   * @param dir the directory
   * @param jreSet the set
   * @return the set
   */
	private Set<File> findJREsInDirectory(File dir, Set<File> jreSet) {
		if (dir==null || !dir.isDirectory()) return jreSet;
		try { // don't search symlinks
			if (!dir.getCanonicalPath().equals(dir.getAbsolutePath())) {
				return jreSet;
			}
		} catch (IOException e) {
		}		
		
		// for OSX
		if (OSPRuntime.isMac()) {
			// check this directory
			File javaFile = new File(dir, "bin/java"); //$NON-NLS-1$
			if (javaFile.exists()) {
				jreSet.add(new JavaFile(dir));
				return jreSet;
			}
			
			// look for Contents if parent is plugin
			if (dir.getName().contains(".plugin")) { //$NON-NLS-1$
				File child = new File(dir, "Contents"); //$NON-NLS-1$
				if (child.exists()) {
					findJREsInDirectory(new File(child, "Home"), jreSet); //$NON-NLS-1$
				}
				return jreSet;
			}
		}
		
		File parent = dir.getParentFile();
		// search files in the directory
		String[] fileNames = dir.list();
		if (fileNames!=null && fileNames.length>0) {
			for (String next: fileNames) {
				// if javaFilter accepts next then dir is "/bin" so add parent unless it is a jdk
				if (javaFilter.accept(dir, next)) {
					synchronized(jreSet) {
						jreSet.add(new JavaFile(parent));
						if (parent.getParent().contains("jdk")) { //$NON-NLS-1$
							searchedAndFoundJRE.add(parent.getParentFile());
						}
						else searchedAndFoundJRE.add(parent);
					}
					break;
				}
				// else search the next level down
				else if (!searchedAndFoundJRE.contains(parent)){
					findJREsInDirectory(new File(dir, next), jreSet);
				}
			}
		}
		return jreSet;
	}
	
  /**
   * Returns public JREs of a given bitness (32 or 64).
   * @param vmBitness the bitness desired
   * @return a Set of java JRE directory files
   */
	private TreeSet<File> getPublicJREs(int vmBitness) {
		TreeSet<File> jreDirs = getJREs(vmBitness);
		if (OSPRuntime.isWindows()) {					
			for (Iterator<File> it=jreDirs.iterator(); it.hasNext();) {
				File next = it.next();
				if (next.getPath().indexOf("jdk")>-1) { //$NON-NLS-1$
					it.remove();
				}
			}
		}
    return jreDirs;
	}
	
	/**
	 * Private constructor.
	 */
	private JREFinder() {
		javaFilter = new JavaFilter();				
	}
	
	/**
	 * JavaFilter class identifies java files.
	 */
  private static class JavaFilter implements FilenameFilter { 
  	@Override
    public boolean accept(File dir, String name) {
      if (!dir.getPath().endsWith("bin")) return false; //$NON-NLS-1$
      if (!OSPRuntime.isMac() && !dir.getParent().contains("jre")) return false; //$NON-NLS-1$
      // reject older versions
    	if (dir.getPath().contains("1.5.") || dir.getPath().contains("-5-")  //$NON-NLS-1$ //$NON-NLS-2$
    			|| dir.getPath().contains("1.4.") || dir.getPath().contains("1.3.") //$NON-NLS-1$ //$NON-NLS-2$
    			|| dir.getPath().contains("1.2.")) return false; //$NON-NLS-1$
    	if (name.contains("javaw")) return true; //$NON-NLS-1$
      return false;
    }
  }

	/**
	 * JavaFile class is a File that sorts by Java version.
	 */
  static class JavaFile extends File {
    
  	/**
  	 * Constructor copies a File
  	 * 
  	 * @param file the file
  	 */
    public JavaFile(File file) {
    	super(file.getPath());
    }
    
    @Override
    public int compareTo(File other) {
    	String path = this.getPath();
    	String otherPath = other.getPath();
    	// public jres come before jdks
			if (!path.contains("jdk") && otherPath.contains("jdk")) { //$NON-NLS-1$ //$NON-NLS-2$
				return -1;
			}
			if (path.contains("jdk") && !otherPath.contains("jdk")) { //$NON-NLS-1$ //$NON-NLS-2$
				return 1;
			}
			// order files in reverse version order (ie latest version first)
			int comp = super.compareTo(other);
			comp = comp>0? -1: comp<0? 1: 0;
			if (OSPRuntime.isWindows()) {
				// if both versions are of form "1.X..." then compare alphanumerically
				if (path.contains("1.") && otherPath.contains("1.")) { //$NON-NLS-1$ //$NON-NLS-2$
					return comp;
				}
				// handle java 6, 7, 9 
				if (path.contains("jre6")) { //$NON-NLS-1$
					if (otherPath.contains("jre7") //$NON-NLS-1$
							|| otherPath.contains("1.8") //$NON-NLS-1$
							|| otherPath.contains("jre-9")) { //$NON-NLS-1$
						return 1;
					}
					return comp;
				}
				if (otherPath.contains("jre6")) { //$NON-NLS-1$
					if (path.contains("jre7") //$NON-NLS-1$
							|| path.contains("1.8") //$NON-NLS-1$
							|| path.contains("jre-9")) { //$NON-NLS-1$
						return -1;
					}
					return comp;
				}
				if (path.contains("jre7")) { //$NON-NLS-1$
					if (otherPath.contains("jre6")) { //$NON-NLS-1$
						return -1;
					}
					if (otherPath.contains("1.8") //$NON-NLS-1$
							|| otherPath.contains("jre-9")) { //$NON-NLS-1$
						return 1;
					}
					return comp;
				}
				if (otherPath.contains("jre7")) { //$NON-NLS-1$
					if (path.contains("jre6")) { //$NON-NLS-1$
						return 1;
					}
					if (path.contains("1.8") //$NON-NLS-1$
							|| path.contains("jre-9")) { //$NON-NLS-1$
						return -1;
					}
					return comp;
				}
				if (path.contains("jre-9")) { //$NON-NLS-1$
					if (otherPath.contains("jre6") //$NON-NLS-1$
							|| otherPath.contains("jre7") //$NON-NLS-1$
							|| otherPath.contains("1.8")) { //$NON-NLS-1$
						return -1;
					}
					return comp;
				}
				if (otherPath.contains("jre-9")) { //$NON-NLS-1$
					if (path.contains("jre6") //$NON-NLS-1$
							|| path.contains("jre7") //$NON-NLS-1$
							|| path.contains("1.8")) { //$NON-NLS-1$
						return 1;
					}
					return comp;
				}
			}
			return comp;
    }
    
  }

}
