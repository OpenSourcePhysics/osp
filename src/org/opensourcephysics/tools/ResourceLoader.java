/*
 * Open Source Physics software is free software as described near the bottom of
 * this code file.
 *
 * For additional information and documentation on Open Source Physics please
 * see: <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;
import java.applet.AudioClip;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.display.DisplayRes;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.js.JSUtil;

/**
 * This defines static methods for loading resources.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class ResourceLoader {
	
	private final static String encoding = "UTF-8"; //$NON-NLS-1$
    final static Charset defaultCharset = Charset.forName(encoding);


  @SuppressWarnings("javadoc")
	public static final FileFilter OSP_CACHE_FILTER;
  protected static final String WIN_XP_DEFAULT_CACHE = "/Local Settings/Application Data/OSP/Cache"; //$NON-NLS-1$
  protected static final String WINDOWS_DEFAULT_CACHE = "/AppData/Local/OSP/Cache"; //$NON-NLS-1$
  protected static final String OSX_DEFAULT_CACHE = "/Library/Caches/OSP"; //$NON-NLS-1$
  protected static final String LINUX_DEFAULT_CACHE = "/.config/OSP/Cache"; //$NON-NLS-1$
  protected static final String SEARCH_CACHE_SUBDIRECTORY = "Search"; //$NON-NLS-1$
  
  public static final String TRACKER_TEST_URL = null; // needed for tracker

  protected static final String WEB_CONNECTED_TEST_URL = OSPRuntime.WEB_CONNECTED_TEST_URL;

  protected static ArrayList<String> searchPaths = new ArrayList<String>();                        // search paths
  protected static ArrayList<String> appletSearchPaths = new ArrayList<String>();                  // search paths for apples
  protected static int maxPaths = 20;                                                              // max number of paths in history
  protected static Hashtable<String, Resource> resources = new Hashtable<String, Resource>();      // cached resources
  protected static boolean cacheEnabled = OSPRuntime.resCacheEnabled;
  protected static boolean canceled=false;
	protected static Map<String, URLClassLoader> zipLoaders = (OSPRuntime.checkZipLoaders
			? new TreeMap<String, URLClassLoader>()
			: null); // maps path to zipLoader
  protected static URLClassLoader xsetZipLoader; // zipLoader of current xset
  protected static Set<String> extractExtensions = new TreeSet<String>();
  protected static ArrayList<String> pathsNotFound = new ArrayList<String>();
  protected static File ospCache;
  protected static boolean zipURLsOK;
  protected static boolean webConnected;
  protected static String downloadURL = ""; //$NON-NLS-1$
  
  static {
  	OSP_CACHE_FILTER = new FileFilter() {
  		@Override
		public boolean accept(File file) {
  			return file.isDirectory() && file.getName().startsWith("osp-"); //$NON-NLS-1$
  		}
  	};
  	Runnable runner = new Runnable() {
  		@Override
		public void run() {
	  		webConnected = isWebConnected();//.http://www.opensourcephysics.org"); 
  		}
  	};
  	new Thread(runner).start();
  }

  /**
   * Private constructor to prevent instantiation.
   */
  private ResourceLoader() {
    /** empty block */
  }

  /**
   * Gets a resource specified by name. If no resource is found using the name
   * alone, the searchPaths are searched.
   *
   * @param name the file or URL name
   * @return the Resource, or null if none found
   */
  public static Resource getResource(String name) {
    return getResource(name, true);
  }

  /**
   * Gets a resource specified by name. If no resource is found using the name
   * alone, the searchPaths are searched. This will find a zip file as a URL
   * resource, unlike the getResource(String) method.
   *
   * @param name the file or URL name
   * @return the Resource, or null if none found
   */
  public static Resource getResourceZipURLsOK(String name) {
  	zipURLsOK = true;
    Resource res = getResource(name, true);
  	zipURLsOK = false;
    return res;
  }

  /**
	 * Gets a resource specified by name. If no resource is found using the name
	 * alone, the searchPaths are searched. Files are searched only if searchFile is
	 * true.
   *
   * @param name the file or URL name
   * @param searchFiles true to search files
   * @return the Resource, or null if none found
   */
  public static Resource getResource(String name, boolean searchFiles) {
    try {
      URL url = getAppletResourceURL(name); // added by W. Christian
      if(url!=null) {
        return new Resource(url);
      }
		} catch (Exception ex) {
		}
    return getResource(name, Resource.class, searchFiles);
  }

  /**
   * Gets a resource specified by name and Class. If no resource is found using
   * the name alone, the searchPaths are searched.
   *
   * @param name the file or URL name
   * @param type the Class providing default ClassLoader resource loading
   * @return the Resource, or null if none found
   */
  public static Resource getResource(String name, Class<?> type) {
    return getResource(name, type, true);
  }

	/**
	 * Gets a resource specified by name and Class. If no resource is found using
	 * the name alone, the searchPaths are searched. Files are searched only if
	 * searchFile is true.
	 *
	 * @param name        the file or URL name
	 * @param type        the Class providing default ClassLoader resource loading
	 * @param searchFiles true to search files
	 * @return the Resource, or null if none found
	 */
	private static Resource getResource(String name, Class<?> type, boolean searchFiles) {
		if ((name == null) || name.equals("")) { //$NON-NLS-1$
			return null;
		}
		pathsNotFound.clear();
		// Remove leading and trailing inverted commas (added by Paco)
		if (name.startsWith("\"")) { //$NON-NLS-1$
			name = name.substring(1);
		}
		if (name.endsWith("\"")) { //$NON-NLS-1$
			name = name.substring(0, name.length() - 1);
		}
		while (name.startsWith("./")) { //$NON-NLS-1$
			name = name.substring(2);
		}
		
		if (OSPRuntime.isJS) {
			// could be file:/TEMP; could be what?
			//System.out.println("RL " + name);
			if (!isHTTP(name) && !name.startsWith("/") && name.indexOf(OSPRuntime.tempDir) < 0) {
				if (false)//
				name = OSPRuntime.tempDir + name;
			}
		} else
		/**
		 * not applicable -- Tracker Java applet only
		 * 
		 * @j2sNative
		 * 
		 */
		{
			// removed by Transpiler
			if (OSPRuntime.isAppletMode() || (OSPRuntime.applet != null)) { 
				// added by Paco
				Resource appletRes = null;
				// following code added by Doug Brown 2009/11/14
				if (type == OSPRuntime.applet.getClass()) {
					try {
						URL url = getTypeResource(type, name);
						appletRes = createResource(url);
						if (appletRes != null) {
							return appletRes;
						}
					} catch (Exception ex) {
						// url was not found
					}
				} // end code added by Doug Brown 2009/11/14
				for (Iterator<String> it = searchPaths.iterator(); it.hasNext();) {
					String path = getPath(it.next(), name);
					appletRes = findResourceInClass(path, type, searchFiles);
					if (appletRes != null) {
						return appletRes;
					}
				}
				appletRes = findResourceInClass(name, type, searchFiles);
				if (appletRes != null) {
					return appletRes;
				}
			}
		}
		
		// look for resource with name only
		Resource res = findResource(name, type, searchFiles);
		if (res != null) {
			return res;
		}
		pathsNotFound.add(name);
		StringBuffer err = new StringBuffer("Not found: " + name); //$NON-NLS-1$
		err.append(" [searched " + name); //$NON-NLS-1$
		// look for resource in searchPaths
		for (String next : searchPaths) {
			String path = getPath(next, name);
			if (pathsNotFound.contains(path))
				continue;
			res = findResource(path, type, searchFiles);
			if (res != null) {
				return res;
			}
			pathsNotFound.add(path);
			err.append(";" + path); //$NON-NLS-1$
		}
		err.append("]"); //$NON-NLS-1$
		OSPLog.fine(err.toString());
		return null;
	}

  /**
	 * Gets a resource specified by base path and name. If base path is relative and
	 * no resource is found using the base alone, the searchPaths are searched.
   *
   * @param basePath the base path
   * @param name the file or URL name
   * @return the Resource, or null if none found
   */
  public static Resource getResource(String basePath, String name) {
    return getResource(basePath, name, Resource.class);
  }

  /**
	 * Gets a resource specified by base path and name. If base path is relative and
	 * no resource is found using the base alone, the searchPaths are searched.
	 * Files are searched only if searchFile is true.
   *
   * @param basePath the base path
   * @param name the file or URL name
   * @param searchFiles true to search files
   * @return the Resource, or null if none found
   */
  public static Resource getResource(String basePath, String name, boolean searchFiles) {
    return getResource(basePath, name, Resource.class, searchFiles);
  }

  /**
   * Gets a resource specified by base path, name and class. If base path is
	 * relative and no resource is found using the base alone, the searchPaths are
	 * searched.
   *
   * @param basePath the base path
   * @param name the file or URL name
   * @param type the Class providing ClassLoader resource loading
   * @return the Resource, or null if none found
   */
  public static Resource getResource(String basePath, String name, Class<Resource> type) {
    return getResource(basePath, name, type, true);
  }

  /**
 * Gets a resource specified by base path, name and class. If base path is
	 * relative and no resource is found using the base alone, the searchPaths are
	 * searched. Files are searched only if searchFile is true.
 *
 * @param basePath the base path
 * @param name the file or URL name
 * @param type the Class providing ClassLoader resource loading
 * @param searchFiles true to search files
 * @return the Resource, or null if none found
 */
  public static Resource getResource(String basePath, String name, Class<Resource> type, boolean searchFiles) {
  	if(basePath==null) {
      return getResource(name, type);
    }
    if(name.startsWith("./")) { //$NON-NLS-1$   
      name = name.substring(2);
    }
    // look for resource with basePath and name
    pathsNotFound.clear();
    String path = getPath(basePath, name);
    Resource res = findResource(path, type, searchFiles);
    if(res!=null) {
      return res;
    }
    // keep looking only if base path is relative
    if(basePath.startsWith("/")||(basePath.indexOf(":/")>-1)) { //$NON-NLS-1$ //$NON-NLS-2$
      return null;
    }
    pathsNotFound.add(path);
    StringBuffer err = new StringBuffer("Not found: "+path); //$NON-NLS-1$
    err.append(" [searched "+path); //$NON-NLS-1$
    if(OSPRuntime.applet!=null) {                  // applet mode
      String docBase = OSPRuntime.applet.getDocumentBase().toExternalForm();
      docBase = XML.getDirectoryPath(docBase)+"/"; //$NON-NLS-1$
      path = getPath(getPath(docBase, basePath), name);
    	if (!pathsNotFound.contains(path)) {
	      res = findResource(path, type, searchFiles);
	      if(res!=null) {
	        return res;
	      }
	      pathsNotFound.add(path);
	      err.append(";"+path);                        //$NON-NLS-1$
    	}
      String codeBase = OSPRuntime.applet.getCodeBase().toExternalForm();
      if(!codeBase.equals(docBase)) {
        path = getPath(getPath(codeBase, basePath), name);
      	if (!pathsNotFound.contains(path)) {
	        res = findResource(path, type, searchFiles);
	        if(res!=null) {
	          return res;
	        }
	        pathsNotFound.add(path);
	        err.append(";"+path);                      //$NON-NLS-1$
      	}
      }
    }
    // look for resource in searchPaths
    for(Iterator<String> it = searchPaths.iterator(); it.hasNext(); ) {
      path = getPath(getPath(it.next(), basePath), name);
    	if (pathsNotFound.contains(path))
    		continue;
      res = findResource(path, type, searchFiles);
      if(res!=null) {
        return res;
      }
      pathsNotFound.add(path);
      err.append(";"+path); //$NON-NLS-1$
    }
    err.append("]"); //$NON-NLS-1$
    OSPLog.fine(err.toString());
    return null;
  }

  /**
   * Adds a path at the beginning of the searchPaths list.
   *
   * @param base the base path to add
   */
  public static void addSearchPath(String base) {
    if((base==null)||base.equals("")||(maxPaths<1)) { //$NON-NLS-1$
      return;
    }
    synchronized(searchPaths) {
      if(searchPaths.contains(base)) {
        searchPaths.remove(base);
      } else {
        OSPLog.fine("Added path: "+base);   //$NON-NLS-1$
      }
      searchPaths.add(0, base);
      while(searchPaths.size()>Math.max(maxPaths, 0)) {
        base = searchPaths.get(searchPaths.size()-1);
        OSPLog.fine("Removed path: "+base); //$NON-NLS-1$
        searchPaths.remove(base);
      }
    }
  }

  /**
   * Removes a path from the searchPaths list.
   *
   * @param base the base path to remove
   */
  public static void removeSearchPath(String base) {
    if((base==null)||base.equals("")) { //$NON-NLS-1$
      return;
    }
    synchronized(searchPaths) {
      if(searchPaths.contains(base)) {
        OSPLog.fine("Removed path: "+base); //$NON-NLS-1$
        searchPaths.remove(base);
      }
    }
  }

  /**
	 * Adds a search path at the beginning of the applet's search path list. Added
	 * by Wolfgang Christian.
   *
   * @param base the base path to add
   */
  public static void addAppletSearchPath(String base) {
    if((base==null)||(maxPaths<1)) {
      return;
    }
	base=base.trim();
		if (!base.endsWith("/")) //$NON-NLS-1$
			base = base + "/"; //$NON-NLS-1$
    synchronized(appletSearchPaths) {
      if(appletSearchPaths.contains(base)) {
        appletSearchPaths.remove(base);                 // search path will be added to top of list later
      } else {
        OSPLog.fine("Applet search path added: "+base); //$NON-NLS-1$
      }
      appletSearchPaths.add(0, base);
      while(appletSearchPaths.size()>Math.max(maxPaths, 0)) {
        base = appletSearchPaths.get(appletSearchPaths.size()-1);
        OSPLog.fine("Removed path: "+base);             //$NON-NLS-1$
        appletSearchPaths.remove(base);
      }
    }
  }

  /**
	 * Removes a path from the applet search path list. Added by Wolfgang Christian.
   *
   * @param base the base path to remove
   */
  public static void removeAppletSearchPath(String base) {
    if((base==null)||base.equals("")) { //$NON-NLS-1$
      return;
    }
    synchronized(appletSearchPaths) {
      if(appletSearchPaths.contains(base)) {
        OSPLog.fine("Applet search path removed: "+base); //$NON-NLS-1$
        appletSearchPaths.remove(base);
      }
    }
  }

  /**
   * Sets the cacheEnabled property.
   *
   * @param enabled true to enable the cache
   */
  public static void setCacheEnabled(boolean enabled) {
    cacheEnabled = enabled;
  }

  /**
   * Gets the cacheEnabled property.
   *
   * @return true if the cache is enabled
   */
  public static boolean isCacheEnabled() {
    return cacheEnabled;
  }

  /**
	 * Adds an extension to the end of the extractExtensions list. Files with this
	 * extension found inside jars are extracted before loading.
   *
   * @param extension the extension to add
   */
  public static void addExtractExtension(String extension) {
    if((extension==null)||extension.equals("")) { //$NON-NLS-1$
      return;
    }
    if(!extension.startsWith(".")) { //$NON-NLS-1$
      extension = "."+extension;     //$NON-NLS-1$
    }
    OSPLog.finest("Added extension: "+extension); //$NON-NLS-1$
    synchronized(extractExtensions) {
      extractExtensions.add(extension);
    }
  }

  /**
   * Cancels the current operation when true.
   *
   * @param cancel true to cancel
   */
  public static void setCanceled(boolean cancel) {
  	canceled = cancel;
  }
  
  /**
   * Determines if the current operation is canceled.
   *
   * @return true if canceled
   */
  public static boolean isCanceled() {
  	return canceled;
  }
  
  // ___________________________ convenience methods _________________________
  
  /**
   * Opens and returns an input stream. May return null.
   * 
   * @param path the path
   * @return the input stream
   */
  public static InputStream openInputStream(String path) {
    Resource res = getResource(path);
    return(res==null) ? null : res.openInputStream();
  }

  /**
   * Opens and returns a reader. May return null.
   * 
   * @param path the path
   * @return the reader
   */
  public static Reader openReader(String path) {
    Resource res = getResource(path);
    return(res==null) ? null : res.openReader();
  }

  /**
   * Gets a string. May return null.
   * 
   * @param path the path
   * @return the string
   */
  public static String getString(String path) {
    Resource res = getResource(path);
    return(res==null) ? null : res.getString();
  }

  /**
   * Gets an icon. May return null.
   * 
   * @param path the path
   * @return the icon
   */
  public static Icon getIcon(String path) {
    URL url = getAppletResourceURL(path); // added by W. Christian
    if(url!=null) {
      return new ImageIcon(url);
    }
    url = getImageZipResource(path);
    if (url != null)
    	return new ImageIcon(url);
    Resource res = getResource(path);
    return(res==null) ? null : res.getIcon();
  }


  /**
   * Gets a buffered image. May return null.
   * 
   * @param path the path
   * @return the image
   */
  public static BufferedImage getBufferedImage(String path) {
    Resource res = getResource(path);
    return(res==null) ? null : res.getBufferedImage();
  }

  /**
  * Gets a buffered image. May return null.
  * 
  * @param path the path
   * @param bufferedImageType one of the types defined by the BufferedImage class
  * @return the image
  */
 public static BufferedImage getBufferedImage(String path, int bufferedImageType) {
   Resource res = getResource(path);
   return(res==null) ? null : res.getBufferedImage(bufferedImageType);
 }

  /**
   * Gets an audio clip. May return null.
   * 
   * @param path the path
   * @return the audio clip
   */
  public static AudioClip getAudioClip(String path) {
    Resource res = getResource(path);
    return(res==null) ? null : res.getAudioClip();
  }
  
  /**
   * Sets the directory for cached files.
   * 
   * @param newCache the desired cache directory
   */
  public static void setOSPCache(File newCache) {
  	if (newCache!=null && !newCache.equals(ospCache)) {
  		// reject new cache if it is a subdirectory of the current cache!
  		if (ospCache!=null && newCache.getAbsolutePath().contains(ospCache.getAbsolutePath())) {
        Toolkit.getDefaultToolkit().beep();
  			return;
  		}
      if (!newCache.exists() || !newCache.isDirectory()) {
      	if (!newCache.mkdirs()) {
          Toolkit.getDefaultToolkit().beep();
          return;
      	}
      }
      if (!newCache.canWrite()) {
        Toolkit.getDefaultToolkit().beep();
        return;
      }
      if (ospCache!=null) {
	      // copy existing cached files into new cache and delete old cache
		  	File[] hostDirectories = ospCache.listFiles(OSP_CACHE_FILTER);
      	for (File host: hostDirectories) {
      		String hostname = host.getName();
      		File newHost = new File(newCache, hostname);
      		copyAllFiles(host, newHost);
      	}
      	
      	// copy search cache files into new cache
      	File searchCache = new File(ospCache, SEARCH_CACHE_SUBDIRECTORY);
      	File newSearchCache = new File(newCache, SEARCH_CACHE_SUBDIRECTORY);
    		copyAllFiles(searchCache, newSearchCache);
    		
      	// clear prev cache, including search cache
    		clearOSPCache(ospCache, true); 
    		// delete prev cache only if it is empty
		  	File[] files = ospCache.listFiles();
		  	if (files!=null && files.length==0) {
		  		ospCache.delete();
				}
      }
    	ospCache = newCache;
  	}
  }

  /**
   * Gets the directory for cached files.
   * 
   * @return the OSP cache
   */
  public static File getOSPCache() {
  	return ospCache;
  }
  
  /**
   * Gets the default directory for cached files.
   * 
   * @return the default OSP cache
   */
  public static File getDefaultOSPCache() {
  	String cacheDir = OSPRuntime.tempDir;
  	String userHome = OSPRuntime.getUserHome();
		if (!OSPRuntime.isJS && userHome!=null) {
	  	userHome += "/"; //$NON-NLS-1$
			if (OSPRuntime.isMac()) {
				cacheDir = userHome+OSX_DEFAULT_CACHE;
			} else if (OSPRuntime.isLinux()) {
				cacheDir = userHome+LINUX_DEFAULT_CACHE;
			} else if (OSPRuntime.isWindows()) {
				String os = System.getProperty("os.name", "").toLowerCase(); //$NON-NLS-1$ //$NON-NLS-2$
				if (os.indexOf("xp")>-1) //$NON-NLS-1$
					cacheDir = userHome+WIN_XP_DEFAULT_CACHE;
				else
					cacheDir = userHome+WINDOWS_DEFAULT_CACHE;
			}
		}
		if (cacheDir == null)
			return null;
		return new File(cacheDir);
  }
  
  /**
   * Uses a JFileChooser to select a cache directory.
	 * 
   * @param parent  a component to own the file chooser
   * @return the chosen file
   */
  public static File chooseOSPCache(Component parent) {
    JFileChooser chooser = new JFileChooser(getOSPCache());
    if (OSPRuntime.isMac())
      chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    else
    	chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    javax.swing.filechooser.FileFilter folderFilter = new javax.swing.filechooser.FileFilter() {
      // accept directories only
      @Override
	public boolean accept(File f) {
				if (f == null)
					return false;
        return f.isDirectory();
      }
      @Override
	public String getDescription() {
        return ToolsRes.getString("LibraryTreePanel.FolderFileFilter.Description"); //$NON-NLS-1$
      } 	     	
    };
    chooser.setAcceptAllFileFilterUsed(false);
    chooser.addChoosableFileFilter(folderFilter);
    String text = ToolsRes.getString("ResourceLoader.FileChooser.Cache"); //$NON-NLS-1$
    chooser.setDialogTitle(text);
  	FontSizer.setFonts(chooser, FontSizer.getLevel());
	  int result = chooser.showDialog(parent, text);
    if (result==JFileChooser.APPROVE_OPTION) {
      return chooser.getSelectedFile();
    }
  	return null;
  }
  

  
	/**
	 * Determines if a path defines a file in the OSP cache.
	 * 
	 * @param path the path
	 * @return true if path is in the OSP cache
	 */
	public static boolean isOSPCachePath(String path) {
		File cacheDir = getOSPCache();
		if (cacheDir == null)
			cacheDir = new File(OSPRuntime.tempDir); 
		String cachePath = XML.forwardSlash(cacheDir.getAbsolutePath()) + "/osp-"; //$NON-NLS-1$
		if (XML.forwardSlash(path).contains(cachePath))
			return true;
		return false;
	}
  
  /**
   * Gets the cache file associated with a URL path.
   * 
   * @param urlPath the path to the file
   * @return the cache file
   */
  public static File getOSPCacheFile(String urlPath) {
  	return getOSPCacheFile(urlPath, null);
  }
	
	/**
   * Gets the cache file associated with a URL path.
   * 
   * @param urlPath the path to the file
   * @param name name of the file (may be null)
   * @return the cache file
   */
  public static File getOSPCacheFile(String urlPath, String name) {
  	File cacheDir = getOSPCache();
  	if (cacheDir==null)
			cacheDir = new File(OSPRuntime.tempDir); 
  	return getCacheFile(cacheDir, urlPath, name);
  }

  /**
   * Gets the search cache directory.
   * 
   * @return the search cache
   */
  public static File getSearchCache() {
  	File ospCache = getOSPCache();
  	if (ospCache==null) {
  		ospCache = getDefaultOSPCache();
  	}
		if (ospCache == null)
			return null;
  	File searchCache = new File(ospCache, SEARCH_CACHE_SUBDIRECTORY);
  	searchCache.mkdirs();
  	return searchCache;
  }
  
	/**
   * Gets the search cache (XML) file associated with a URL path.
   * 
   * @param urlPath the path to the file
   * @return the search cache file
   */
  public static File getSearchCacheFile(String urlPath) {
		String filename = XML.getName(urlPath);
  	String basename = XML.stripExtension(filename);
  	String ext = XML.getExtension(filename);
  	if (ext!=null) 
  		basename += "_"+ext; //$NON-NLS-1$
		// following line needed to clean up long extensions associated with ComPADRE
		// query paths
  	basename = basename.replace('&', '_').replace('?', '_').replace('=', '_');
  	filename = basename+".xml"; //$NON-NLS-1$
  	return getCacheFile(getSearchCache(), urlPath, filename);
  }

	/**
   * Gets the cache file associated with a base cache directory and URL path.
	 * 
   * @param cacheFile the base cache directory
   * @param urlPath the URL path to the original file
   * @param name name of the file (may be null)
   * @return the cache file
   */
  private static File getCacheFile(File cacheFile, String urlPath, String name) {
	  // SwingJS this might be null. 
		String cachePath = (cacheFile == null ? "./" :  XML.forwardSlash(cacheFile.getAbsolutePath()));
		urlPath = getURIPath(urlPath);
  		
		String host = ""; //$NON-NLS-1$
		String path = ""; //$NON-NLS-1$
		String filename = ""; //$NON-NLS-1$
		try {
			URL url = new URL(urlPath);
			host = url.getHost().replace('.', '_');
			path = getNonURIPath(url.getPath());
			
			int n = path.indexOf(cachePath);
			if (n>-1) {
				path = path.substring(n+cachePath.length());
			}
			// if path is local, strip drive letter
			n = path.lastIndexOf(":"); //$NON-NLS-1$
			if (n>-1) {
				path = path.substring(n+1);
			}
			// strip leading slash
  		while (path.startsWith("/")) { //$NON-NLS-1$
  			path = path.substring(1);
  		}
			String pathname = XML.getName(path);
			if (!"".equals(pathname)) { //$NON-NLS-1$
				path = XML.getDirectoryPath(path);
			}
			path = path.replace('.', '_').replace("!", ""); //$NON-NLS-1$ //$NON-NLS-2$
			filename = name==null? pathname: name;
		} catch (MalformedURLException e) {				
		}
		
		if ("".equals(host)) //$NON-NLS-1$
			host = "local_machine"; //$NON-NLS-1$		
		if (!path.startsWith("osp-"))  //$NON-NLS-1$
			cacheFile = new File(cacheFile, "osp-"+host); //$NON-NLS-1$
		if (!"".equals(path)) //$NON-NLS-1$
			cacheFile = new File(cacheFile, path);
		if (!"".equals(filename)) //$NON-NLS-1$
			cacheFile = new File(cacheFile, filename);
		
		return cacheFile;
  }

	/**
	 * Downloads a file from the web to the OSP Cache.
	 * 
	 * @param urlPath         the path to the file
	 * @param fileName        the name to assign the downloaded file
	 * @param alwaysOverwrite true to overwrite an existing file, if any
	 * @return the downloaded file, or null if failed to download
	 */
	public static File downloadToOSPCache(String urlPath, String fileName, boolean alwaysOverwrite) {
		if (fileName == null)
			return null;
		File target = getOSPCacheFile(urlPath, fileName);
		File file = download(urlPath, target, alwaysOverwrite);
		if (file == null && webConnected) {
			webConnected = isWebConnected(); // $NON-NLS-1$
			if (!webConnected) {
				JOptionPane.showMessageDialog(null,
						ToolsRes.getString("LibraryBrowser.Dialog.ServerUnavailable.Message"), //$NON-NLS-1$
						ToolsRes.getString("LibraryBrowser.Dialog.ServerUnavailable.Title"), //$NON-NLS-1$
						JOptionPane.WARNING_MESSAGE);
			} else {
				JOptionPane.showMessageDialog(null,
						ToolsRes.getString("ResourceLoader.Dialog.FailedToDownload.Message1") //$NON-NLS-1$
								+ "\n" + ToolsRes.getString("ResourceLoader.Dialog.FailedToDownload.Message2") //$NON-NLS-1$ //$NON-NLS-2$
								+ "\n" + ToolsRes.getString("ResourceLoader.Dialog.FailedToDownload.Message3"), //$NON-NLS-1$ //$NON-NLS-2$
						ToolsRes.getString("ResourceLoader.Dialog.FailedToDownload.Title"), //$NON-NLS-1$
						JOptionPane.ERROR_MESSAGE);
			}
		}
		return file;
	}
  
  /**
   * Returns the HTML code for a local or web HTML page.
	 * 
   * @param path the path to the HTML page 
   * @return the HTML code, or null if not found or not HTML
   */
	public static String getHTMLCode(String path) {
		Resource res = getResourceZipURLsOK(path);
		if (res == null)
			return null;
		String html = res.getString();
		if (html!=null && html.trim().startsWith("<!DOCTYPE html")) //$NON-NLS-1$
			return html;
		return null;
	}
	
	public static void getHTMLCodeAsync(String path, Function<String, Void> whenDone) {
		getResourceZipURLsOKAsync(path, new Function<Resource, Void>() {

			@Override
			public Void apply(Resource res) {
				if (res == null) {
					whenDone.apply(null);
				} else {
					String html = res.getString();
					whenDone.apply(html != null && html.trim().startsWith("<!DOCTYPE html") ? html : null); //$NON-NLS-1$
				}
				return null;
			}

		});
	}

	private static void getResourceZipURLsOKAsync(String path, Function<Resource, Void> whenDone) {
		// TODO -- this will be a BIG chore
		whenDone.apply(getResourceZipURLsOK(path));
	}

  /**
   * Returns the title, if any, of an HTML page.
	 * 
   * @param code the HTML code 
   * @return the title, or null if none defined
   */
	public static String getTitleFromHTMLCode(String code) {
		if (code == null)
			return null;
		String[] parts = code.split("<title>"); //$NON-NLS-1$
		if (parts.length>1) {
			parts = parts[1].split("</title>"); //$NON-NLS-1$
			if (parts.length>1) {
				return parts[0].trim();
			}
		}
		return null;
	}
	
  /**
   * Returns the first stylesheet link, if any, in an HTML page.
	 * 
   * @param code the HTML code 
   * @return the first stylesheet link found, or null if none
   */
	public static String getStyleSheetFromHTMLCode(String code) {
		if (code == null)
			return null;
		// typical tag is in head: <link rel="stylesheet" type="text/css"
		// href="myFolder/myStyleSheet.css">
		String[] parts = code.split("<head>"); //$NON-NLS-1$
		if (parts.length>1) {
			parts = parts[1].split("</head>"); //$NON-NLS-1$
			if (parts.length>1) {
				parts = parts[0].split("<link"); //$NON-NLS-1$
				if (parts.length>1) {
					// skip parts[0]
					for (int i=1; i<parts.length; i++) {
			  		if (parts[i].contains("\"stylesheet\"")) { //$NON-NLS-1$
			  			parts = parts[i].split("href"); //$NON-NLS-1$
							if (parts.length>1) {
								parts = parts[1].split("\""); //$NON-NLS-1$
								if (parts.length > 1)
									return parts[1];
							}
			  		}
					}
				}

			}
		}  	
		return null;
	}
	
  /**
   * Copies an HTML file with associated images and stylesheet to the OSP cache.
	 * Note this does NOT overwrite cache files--to replace, delete them before
	 * calling this method
	 * 
   * @param htmlPath the path to the source HTML file
   * @return the copied File, or null if failed
   */
  public static File copyHTMLToOSPCache(String htmlPath) {
		if (htmlPath == null)
			return null;
  	// read html text
  	String htmlCode = null;
  	Resource res = getResourceZipURLsOK(htmlPath);
  	if (res!=null) {
  		// if the resource is a file in the OSP cache, return it 
	  	if (res.getFile()!=null && isOSPCachePath(htmlPath)) {
	  		return res.getFile();
	  	}
  		htmlCode = res.getString();
  	}
  	if (htmlCode!=null) {
  		String htmlBasePath = XML.getDirectoryPath(htmlPath);
  		File htmlTarget = getOSPCacheFile(htmlPath);
  		File targetDirectory = htmlTarget.getParentFile();
  		targetDirectory.mkdirs();
  		
	  	// look for image references
	  	File imageDir = new File(targetDirectory, "images"); //$NON-NLS-1$
  		String img = "<img "; //$NON-NLS-1$
  		String pre = "src=\""; //$NON-NLS-1$
  		String post = "\""; //$NON-NLS-1$
  		
  		String temp = htmlCode;
  		int j = temp.indexOf(img);
			if (j > -1)
				imageDir.mkdirs();
  		while (j>-1) {
  			temp = temp.substring(j+img.length());
  			j = temp.indexOf(pre);
  			temp = temp.substring(j+pre.length());
  			j = temp.indexOf(post);
  			if (j>-1) {
  				String next = temp.substring(0, j); // the image path specified in the html itself
  				String path = XML.getResolvedPath(next, htmlBasePath);
  	      res = getResourceZipURLsOK(path);
  				if (res!=null) {
      			// copy image and replace image path in html code
  					String filename = XML.getName(next);
      			File imageTarget = download(path, new File(imageDir, filename), false);
      			if (imageTarget!=null) {
							path = XML.getPathRelativeTo(imageTarget.getAbsolutePath(),
									targetDirectory.getAbsolutePath());
	      			if (!next.equals(path)) {
	      				htmlCode = htmlCode.replace(pre+next+post, pre+path+post);
	      			}      				
      			}
  				}
  			}
  			j = temp.indexOf(img);				
  		}
	  	
	  	// if separate stylesheet is used, copy to cache and replace in HTML code
	  	String css = getStyleSheetFromHTMLCode(htmlCode);
	  	if (css!=null && !isHTTP(css)) { 
	  		res = getResourceZipURLsOK(XML.getResolvedPath(css, htmlBasePath));
	  		if (res!=null) {
	  			String cssName = XML.getName(css);
	  			File cssTarget = new File(targetDirectory, cssName);
    			cssTarget = download(res.getAbsolutePath(), cssTarget, false);
    			if (cssTarget!=null && !cssName.equals(css)) {
      			htmlCode = htmlCode.replace(css, cssName);      				
    			}
	  		}
	  	}
  		
	  	// write modified html text into target file
      try {
	      FileWriter fout = new FileWriter(htmlTarget);
	      fout.write(htmlCode);
	      fout.close();
		  	return htmlTarget;
	    } catch(Exception ex) {
	      ex.printStackTrace();
	    }
  	}
  	return null;
  }
  
  /**
	 * Copies a source file to a target file. If source file is a directory, copies
	 * contents of directory, including subdirectories
   *
   * @param inFile the source
   * @param outFile the target
   * @return true if all files successfully copied
   */
  public static boolean copyAllFiles(File inFile, File outFile) {
  	if (inFile.isDirectory()) {
  		outFile.mkdir();
  		boolean success = true;
  		for (File in: inFile.listFiles()) {
  			String filename = in.getName();
  			File out = new File(outFile, filename);
  			success = success && copyAllFiles(in, out);
  		}
  		return success;
  	}
  	byte[] buffer = new byte[16384]; // 2^14
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
		} catch (IOException ex) {
    	return false;
    }
  	return true;
  }


  
  /**
	 * Clears an OSP cache. Always deletes "osp-host" directories in cache. Deletes
	 * search cache if requested.
   * 
   * @param cache the cache to clear
   * @param clearSearchCache true to clear the search cache
   * @return true if successfully cleared
   */
  public static boolean clearOSPCache(File cache, boolean clearSearchCache) {
		if (cache == null || !cache.canWrite())
			return false;
  	boolean success = true;
  	File[] files = cache.listFiles(OSP_CACHE_FILTER);
		if (files == null)
			return true;
    for(File next: files) {
    	success = success && deleteFile(next);
    }
    if (clearSearchCache) {
    	File searchCache = new File(cache, SEARCH_CACHE_SUBDIRECTORY);
    	success = success && deleteFile(searchCache);    	
    }
    if (!success) {
			JOptionPane.showMessageDialog(null, ToolsRes.getString("ResourceLoader.Dialog.UnableToClearCache.Message1") //$NON-NLS-1$
					+"\n"+ToolsRes.getString("ResourceLoader.Dialog.UnableToClearCache.Message2"), //$NON-NLS-1$ //$NON-NLS-2$ 
					ToolsRes.getString("ResourceLoader.Dialog.UnableToClearCache.Title"), //$NON-NLS-1$ 
					JOptionPane.WARNING_MESSAGE);
    }
    return success;
  }

  /**
   * Clears an OSP cache host directory.
   * 
   * @param hostDir the cache host directory to clear
   * @return true if successfully cleared
   */
  public static boolean clearOSPCacheHost(File hostDir) {
		if (hostDir == null || !hostDir.canWrite())
			return true;
		if (!OSP_CACHE_FILTER.accept(hostDir))
			return false;
  	boolean success = deleteFile(hostDir);
    if (!success) {
			JOptionPane.showMessageDialog(null, ToolsRes.getString("ResourceLoader.Dialog.UnableToClearCache.Message1") //$NON-NLS-1$
					+"\n"+ToolsRes.getString("ResourceLoader.Dialog.UnableToClearCache.Message2"), //$NON-NLS-1$ //$NON-NLS-2$ 
					ToolsRes.getString("ResourceLoader.Dialog.UnableToClearCache.Title"), //$NON-NLS-1$ 
					JOptionPane.WARNING_MESSAGE);
    }
    return success;
  }

  /**
	 * Deletes a file or folder. In case of a folder, deletes all contents and the
	 * folder itself.
   * 
   * @param file the file to delete
   * @return true if deleted
   */
  public static boolean deleteFile(File file) {
    if(file.isDirectory()) {
      File[] files = file.listFiles();
      for(File next: files) {
        deleteFile(next);
      }
    }
    return file.delete();
  }
  
  /**
	 * Gets the list of files in a directory and its subdirectories that are
	 * accepted by a FileFilter.
   * 
   * @param directory the directory to search
   * @param filter the FileFilter
   * @return the list of files
   */
  public static List<File> getFiles(File directory, FileFilter filter) {
    List<File> results = new ArrayList<File>();
    if (directory.isFile()) {
    	results.add(directory);
    	return results;
    }
    File[] contents = directory.listFiles(filter);
    for (File file: contents) {
      if (file.isDirectory()) {
        List<File> deeperList = getFiles(file, filter);
        results.addAll(deeperList);
			} else {
        results.add(file);       	
      }
    }
    return results;
  }
  
  private static Map<String, Map<String, ZipEntry>> htZipContents = new HashMap<>();

  /**
   * zip contents caching can save time in complex loading. 
   * 
   */
	public static void clearZipCache() {
		htZipContents.clear();
	}

	/**
	 * Retrieve the ZipEntry for a file in a given zip file.
	 * 
	 * Note that in JavaScript only, the ZipEntry returned contains a reference to
	 * the underlying ByteArrayInputStream ultimately backing this zip file. This
	 * allows us to retrieve the file data directly from the ZipEntry using
	 * jsutil.getZipBytes(zipEntry).
	 * 
	 * 
	 * @param zipFile
	 * @param fileName
	 * @param isContains fileName is a fragment of the entry name, not the other way
	 *                   around
	 * @return the ZipEntry for this file, possibly cached.
	 */
	public static ZipEntry findZipEntry(String zipFile, String fileName, boolean isContains) {
		if (!isContains) {
			return getZipContents(zipFile).get(fileName);
		} 
		for ( Entry<String, ZipEntry> entry : getZipContents(zipFile).entrySet()) {
		  	if (entry.getKey().indexOf(fileName) >= 0)
		  		return entry.getValue();
		}
		return null;
	}

	public static void findZipEntryAsync(String zipFile, String fileName, boolean isContains,
			Function<ZipEntry, Void> whenDone) {
		getZipContentsAsync(zipFile, new Function<Map<String, ZipEntry>, Void>() {

			@Override
			public Void apply(Map<String, ZipEntry> map) {
				if (!isContains) {
					whenDone.apply(map.get(fileName));
				} else {
					for (Entry<String, ZipEntry> entry : map.entrySet()) {
						if (entry.getKey().indexOf(fileName) >= 0) {
							whenDone.apply(entry.getValue());
							return null;
						}
					}
					whenDone.apply(null);
				}
				return null;
			}
		});
	}

	public static void getZipContentsAsync(String zipPath, Function<Map<String, ZipEntry>, Void> whenDone) {
		URL url = getURLWithCachedBytes(zipPath); // BH carry over bytes if we have them already
		Map<String, ZipEntry> fnames = htZipContents.get(url.toString());
		if (fnames != null) {
			whenDone.apply(fnames);
			return;
		}
		getURLContentsAsync(url, new Function<byte[], Void>() {

			@Override
			public Void apply(byte[] bytes) {
				try {
					whenDone.apply(readZipContents(new ByteArrayInputStream(bytes), url));
				} catch (Exception ex) {
					whenDone.apply(null);
				}
				return null;
			}

		});
	}

	/**
	 * Gets the contents of a zip file.
	 * 
	 * @param zipPath the path to the zip file
	 * @return a set of file names in alphabetical order
	 */
	public static Map<String, ZipEntry> getZipContents(String zipPath) {
		URL url = getURLWithCachedBytes(zipPath); // BH carry over bytes if we have them already
		Map<String, ZipEntry> fileNames = htZipContents.get(url.toString());
		if (fileNames != null)
			return fileNames;
		try {
			// Scan URL zip stream for files.
			return readZipContents(url.openStream(), url);
		} catch (Exception ex) {
			return null;
		}
	}

	private static Map<String, ZipEntry> readZipContents(InputStream is, URL url) throws IOException {
		HashMap<String, ZipEntry> fileNames = new HashMap<String, ZipEntry>();
		if (OSPRuntime.doCacheZipContents)
			htZipContents.put(url.toString(), fileNames);
		ZipInputStream input = new ZipInputStream(is);
		ZipEntry zipEntry = null;
		int n = 0;
		while ((zipEntry = input.getNextEntry()) != null) {
			if (zipEntry.isDirectory() || zipEntry.getSize() == 0)
				continue;
			n++;
			String fileName = zipEntry.getName();
			fileNames.put(fileName, zipEntry); // Java has no use for the ZipEntry, but JavaScript can read it.
		}
		input.close();
		OSPLog.finest("ResourceLoader: " + n + " zip entries found in " + url); //$NON-NLS-1$
		return fileNames;
	}
  
  /**
   * 
	 * Convert a file path to a URL, retrieving any cached file data, as from DnD.
	 * Do not do any actual data transfer.
   * 
   * @param path 
   * @return
   */
  private static URL getURLWithCachedBytes(String path) {
	URL url = null;
	try {
		url = new URL(getURIPath(path));
		OSPRuntime.addJSCachedBytes(url);
	} catch (MalformedURLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}   
  	return url;
}

	/**
	 * Unzips a ZIP file into the given directory. ZIP file may be on a server. Can
	 * be canceled using the static setCanceled(boolean) method. Note this does not
	 * warn of possible overwrites.
	 * 
	 * @param zipPath         the (url) path to the zip file
	 * @param targetDir       target directory to save the extracted files
	 * @param alwaysOverwrite true to overwrite existing files, if any
	 * @return the Set of extracted files
	 */
	public static Set<File> unzip(String zipPath, File targetDir, boolean alwaysOverwrite) {
		if (targetDir == null)
			targetDir = new File(OSPRuntime.tempDir); // $NON-NLS-1$
		OSPLog.finer("unzipping " + zipPath + " to " + targetDir); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			URL url = getURLWithCachedBytes(zipPath);
			BufferedInputStream bufIn = new BufferedInputStream(url.openStream());
			ZipInputStream input = new ZipInputStream(bufIn);
			ZipEntry zipEntry = null;
			Set<File> fileSet = new HashSet<File>();
			byte[] buffer = new byte[1024];
			setCanceled(false);
			while ((zipEntry = input.getNextEntry()) != null) {
				if (zipEntry.isDirectory())
					continue;
				if (isCanceled()) {
					input.close();
					return null;
				}
				String filename = zipEntry.getName();
				File file = new File(targetDir, filename);
				if (!alwaysOverwrite && file.exists()) {
					fileSet.add(file);
					continue;
				}
				file.getParentFile().mkdirs();
				try {
					if (OSPRuntime.isJS) {
						OSPRuntime.jsutil.streamToFile(input, file);
					} else {
						FileOutputStream output = new FileOutputStream(file);
						int bytesRead;
						while ((bytesRead = input.read(buffer)) != -1)
							output.write(buffer, 0, bytesRead);
						output.close();
					}
					input.closeEntry();
					fileSet.add(file);
				} catch (Exception e) {
					continue;
				}
			}
			input.close();
			return fileSet;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}  
  
  /**
   * Downloads a file from the web to a target File.
   * 
   * @param urlPath the path to the file
   * @param target the target file
   * @param alwaysOverwrite true to overwrite an existing file, if any
   * @return the downloaded file, or null if failed
   */
  public static File download(String urlPath, File target, boolean alwaysOverwrite) {
		if (target == null || target.getParentFile() == null)
			return null;
  	// compare urlPath with previous attempt and, if identical, check web connection
  	if (!webConnected || downloadURL.equals(urlPath)) {
  		webConnected = isWebConnected(); 
  	}
  	if (!webConnected) {
			JOptionPane.showMessageDialog(null, ToolsRes.getString("LibraryBrowser.Dialog.ServerUnavailable.Message"), //$NON-NLS-1$
  				ToolsRes.getString("LibraryBrowser.Dialog.ServerUnavailable.Title"), //$NON-NLS-1$
  				JOptionPane.WARNING_MESSAGE); 
  		return null;
  	}

  	urlPath = getURIPath(urlPath);
		target.getParentFile().mkdirs();
    if (alwaysOverwrite || !target.exists()) {
	  	OSPLog.finer("downloading "+urlPath+" to "+target); //$NON-NLS-1$ //$NON-NLS-2$
	  	downloadURL = urlPath;
  		try {
  			Resource res = getResourceZipURLsOK(urlPath);
				InputStream reader = res.openInputStream();
        FileOutputStream writer = new FileOutputStream(target);
        byte[] buffer = new byte[65536]; // 2^14 = 64K
        int bytesRead = 0;
        while ((bytesRead = reader.read(buffer)) > 0) { 
        	writer.write(buffer, 0, bytesRead);
        }  
        writer.close();
        reader.close();
			} catch (Exception ex) {
			} 
	  	downloadURL = ""; //$NON-NLS-1$
    }
    if (target.exists()) {
    	return target; 
    }
    return null;
  }

	public static File extractFileFromZIP(String source, File target, boolean alwaysOverwrite) {
		return extractFileFromZIP(source, target, alwaysOverwrite, true);
	}

	/**
	 * Extracts a file from a ZIP archive to a target file. ZIP archive may be on a
	 * server.
	 * 
	 * @param source          the path of the file to be extracted (eg
	 *                        "http:/www.server/folder/images.zip!/image1.png")
	 * @param target          target file to save
	 * @param alwaysOverwrite true to overwrite existing files, if any
	 * @return the extracted file
	 */
	public static File extractFileFromZIP(String source, File target, boolean alwaysOverwrite,
			boolean forceFileCreation) {
		if (!alwaysOverwrite && target.exists())
			return target;
		try {
			getZipEntryBytes(source, target);
			return target;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * A general osp method that can be used to pull bytes or create a File from a
	 * zip file, efficiently caching a map of ZipEntry data in JavaScript that can
	 * directly seek and extract bytes from that data.
	 * 
	 * The ZipEntry is cached automatically.
	 * 
	 * 
	 * @param jarSource [zipFile]!/[entryPath] in jar-protocol URL format.
	 * @param target    the File object to used, or null if only byte[] return is
	 *                  desired
	 * @return In JavaScript, all bytes are always returned; in Java, all bytes are
	 *         returned only when target is null -- otherwise, only one buffer's
	 *         worth of bytes are returned. In either case, a null byte[] return
	 *         means something went wrong, as of course also does IOException.
	 * 
	 * @throws IOException
	 */
	public static byte[] getZipEntryBytes(String jarSource, File target) throws IOException {
		String[] parts = getJarURLParts(jarSource);
		return (parts == null ? null : getZipEntryBytes(parts[0], parts[1], target));
	}


	public static void getZipEntryBytesAsync(String source, File target, Function<byte[], Void> whenDone) {
		String[] parts = getJarURLParts(source);
		if (parts == null)
			whenDone.apply(null);
		else if (OSPRuntime.isJS) 
			getZipEntryBytesJSAsync(parts[0], parts[1], target, whenDone);
		else {
			try {
				whenDone.apply(getZipEntryBytes(parts[0], parts[1], target));
			} catch (IOException e) {
				whenDone.apply(null);
			}
		}
	}

	private static void getZipEntryBytesJSAsync(String zipFile, String entryPath, File target, Function<byte[], Void> whenDone) {
	    // JS only
		boolean isContains = entryPath.startsWith("*");
		if (isContains) {
			entryPath = entryPath.substring(1);
		}
		findZipEntryAsync(zipFile, entryPath, isContains, new Function<ZipEntry, Void>() {

			@Override
			public Void apply(ZipEntry ze) {
				if (ze == null) {
					whenDone.apply(null);
				} else {
					byte[] bytes = OSPRuntime.jsutil.getZipBytes(ze);
					if (bytes != null && target != null) {
						// Note that this will NOT download to the user, as we are not
						// creating a FileOutputStream.
						OSPRuntime.jsutil.setFileBytes(target, bytes);
					}
					whenDone.apply(bytes);
				}
				return null;
			}
		});
	}


	/**
	 * A general osp method that can be used to pull bytes or create a File from a
	 * zip file, efficiently caching a map of ZipEntry data in JavaScript that can
	 * directly seek and extract bytes from that data.
	 * 
	 * The ZipEntry is cached automatically. 
	 * 
	 * 
	 * @param zipFile
	 * @param entryPath
	 * @param target      the File object to used, or null if only byte[] return is
	 *                    desired
	 * @return In JavaScript, all bytes are always returned; in Java, all bytes are
	 *         returned only when target is null -- otherwise, only one buffer's
	 *         worth of bytes are returned. In either case, a null byte[] return
	 *         means something went wrong, as of course also does IOException.
	 * 
	 * @throws IOException
	 */
	public static byte[] getZipEntryBytes(String zipFile, String entryPath, File target) throws IOException {
		byte[] bytes = null;
		boolean isContains = entryPath.startsWith("*");
		if (isContains) {
			entryPath = entryPath.substring(1);
		}
		if (OSPRuntime.isJS) {
			// Direct seek to known zip entry.
			ZipEntry ze = findZipEntry(zipFile, entryPath, isContains);
			if (ze == null)
				return null;
			bytes = OSPRuntime.jsutil.getZipBytes(ze);
			if (bytes != null && target != null) {
				// Note that this will NOT download to the user, as we are not 
				// creating a FileOutputStream.
				OSPRuntime.jsutil.setFileBytes(target, bytes);
			}
		} else /** @j2sIgnore */
		{
			// Java Only -- none of this is necessary in SwingJS, since we can 
			// directly access the ZipEntry data. 
			URL url = getURLWithCachedBytes(zipFile);
			ZipInputStream input = new ZipInputStream(url.openStream());
			ZipEntry zipEntry = null;
			while ((zipEntry = input.getNextEntry()) != null) {
				// BH 2020.04.25 allow both a in b and b in a
				if (!zipEntry.isDirectory() && (isContains ? zipEntry.getName().contains(entryPath)
						 : entryPath.contains(zipEntry.getName())))
					break;
			}
			if (zipEntry != null) {
				if (target != null)
					target.getParentFile().mkdirs();
				bytes = new byte[1024];
				int bytesRead;
				OutputStream output = (target == null ? new ByteArrayOutputStream() : new FileOutputStream(target));
				while ((bytesRead = input.read(bytes)) != -1)
					output.write(bytes, 0, bytesRead);
				output.close();
				input.closeEntry();
				if (target == null) {
					bytes = ((ByteArrayOutputStream) output).toByteArray();
				}
			}
			input.close();
		}
		if (bytes == null)
			throw new IOException("No bytes were found for " + zipFile + "!/" + entryPath);
		return bytes;
	}

	/**
	 * Divide [jarfile]!/[path] into String[] {jarfile,path}
	 * 
	 * String source can be from a url such as jar:file:/... or file:/... or from
	 * File.absolutePath().
	 * 
	 * @return null if "!/" is not found, { jarfile, null } if path would be "", and
	 *         { jarfile, path } otherwise.
	 */
	public static String[] getJarURLParts(String source) {
		int n = source.indexOf("!/");
		if (n < 0)
			return null;
		String jarfile = source.substring(0, n).replace("jar:",  "").replace("file:",  "");
		while (jarfile.startsWith("//"))
			jarfile = jarfile.substring(1);
		return new String[] { jarfile , (n == source.length() - 2 ? null : source.substring(n + 2)) };
	}

/**
	 * Return true if there is any internet signal at all. The exact URL is
	 * negotiable, but it must be CORS allowed.
   * 
   * @return
   */
  public static boolean isWebConnected() {
	  return isURLAvailable(WEB_CONNECTED_TEST_URL);
  }
  
  /**
   * Determines if a url path is available (ie both valid and connected).
   *
   * @param urlPath the path in URI form
   * @return true if available
   */
  public static boolean isURLAvailable(String urlPath) {
	  if (JSUtil.isJS) {
		  // BH 2020.03.02 trying this to see if it is reasonable
		  // This specific URL will be replaced with a known CORS server by j2sApplet.js
		  urlPath = "https://INTERNET.TEST";
	  }
	  try {
      // make a URL, open a connection, get content
      URL url = new URL(urlPath);
      HttpURLConnection urlConnect = (HttpURLConnection)url.openConnection();
      urlConnect.getContent();	
	  } catch (Exception ex) {
		  return false;
	  }
	  return true;
  }

  /**
   * Removes protocol and "%20" from URI paths.
   *
   * @param uriPath the path in URI form
   * @return the path
   */
  public static String getNonURIPath(String uriPath) {
		if (uriPath == null)
			return null;
  	String path = uriPath;
//		String path = XML.forwardSlash(uriPath.trim());
//  	boolean isJarOrFile = false;
  	// remove jar protocol, if any
    if (path.startsWith("jar:")) {                     //$NON-NLS-1$
      path = path.substring(4);
//      isJarOrFile = true;
    }
  	// remove file protocol, if any
    if (path.startsWith("file:")) {                     //$NON-NLS-1$
      path = path.substring(5);
//      isJarOrFile = true;
    }
    // remove all but one leading slash
    // commented out by DB 2016-07-08 to enable opening local network files
//    if (isJarOrFile) {
//	    while (path.startsWith("//")) { //$NON-NLS-1$
//	      path = path.substring(1);
//	    }
//    }
    // remove last leading slash if drive is specified
    if (path.startsWith("/") && path.indexOf(":")>-1) { //$NON-NLS-1$ //$NON-NLS-2$
      path = path.substring(1);
    }
    // replace "%20" with space
    int j = path.indexOf("%20");                       //$NON-NLS-1$
    while(j>-1) {
      String s = path.substring(0, j);
      path = s+" "+path.substring(j+3);                //$NON-NLS-1$
      j = path.indexOf("%20");                         //$NON-NLS-1$
    }
//    // replace "%26" with "&"
//    j = path.indexOf("%26");                           //$NON-NLS-1$
//    while(j>-1) {
//      String s = path.substring(0, j);
//      path = s+"&"+path.substring(j+3);                //$NON-NLS-1$
//      j = path.indexOf("%26");                         //$NON-NLS-1$
//    }
  	return path;
  }

  /**
   * Converts a path to URI form (spaces replaced by "%20", etc).
   *
   * @param path the path
   * @return the path in URI form
   */
  public static String getURIPath(String path) {
		if (path == null)
			return null;
		// trim and change backslashes to forward slashes
		path = XML.forwardSlash(path.trim());
		// add forward slash at end if needed
		if (!path.equals("")  //$NON-NLS-1$
				&& XML.getExtension(path) == null && !path.endsWith("/")) //$NON-NLS-1$
			path += "/"; //$NON-NLS-1$
    // replace spaces with "%20"
    int i = path.indexOf(" ");                       //$NON-NLS-1$
    while(i>-1) {
      String s = path.substring(0, i);
      path = s+"%20"+path.substring(i+1);            //$NON-NLS-1$
      i = path.indexOf(" ");                         //$NON-NLS-1$
    }
//    // replace "&" with "%26"
//    i = path.indexOf("&");                           //$NON-NLS-1$
//    while(i>-1) {
//      String s = path.substring(0, i);
//      path = s+"%26"+path.substring(i+1);            //$NON-NLS-1$
//      i = path.indexOf("&");                         //$NON-NLS-1$
//    }
    // add file protocol if path to local file
    if (path.startsWith("/"))
    	return "file:" + path;
    // BH because nonURIPath can return //./xxx
    
		if (!path.equals("")  //$NON-NLS-1$
				&& !isHTTP(path) && !path.startsWith("jar:") //$NON-NLS-1$
				&& !path.startsWith("file:/")) { //$NON-NLS-1$
			String protocol = OSPRuntime.isWindows()? "file:/": "file://"; //$NON-NLS-1$ //$NON-NLS-2$
			path = protocol+path;
		}
  	return path;
  }

  // ______________________________ private methods ___________________________

  /**
	 * Gets the resource URL using the applet's class loader. Added by Wolfgang
	 * Christian.
   *
   * @param name of the resource
   * @return URL of the Resource, or null if none found
   */
  private static URL getAppletResourceURL(String name) {
		if (JSUtil.isJS)
			return null;
    if((OSPRuntime.applet==null)||(name==null)||name.trim().equals("")) { //$NON-NLS-1$
      return null;
    }
		if (isHTTP(name)) { // $NON-NLS-1$ // open a direct connection for http and https
							// resources
    	try {
			return new java.net.URL(name);
		} catch (MalformedURLException e) {
			//e.printStackTrace();
		} 
    }
    name=name.trim();  // remove whitespace
    if(!name.startsWith("/")) { //$NON-NLS-1$ // try applet search paths for relative paths
      for(Iterator<String> it = appletSearchPaths.iterator(); it.hasNext(); ) {
        String path = it.next();
        String tempName=name;                   // tempName may change
    	if(tempName.startsWith("../")) {        //$NON-NLS-1$   
    		  tempName = tempName.substring(3);     //remove prefix
              path=path.substring(0, path.length()-1); // drop trailing slash
              int last=path.lastIndexOf("/"); //$NON-NLS-1$ // find last directory slash
					path = (last > 0) ? path.substring(0, last) : "/"; //$NON-NLS-1$ // drop last directory if it
																		// exists
        }else if(tempName.startsWith("./")) {         //$NON-NLS-1$   
        	tempName = tempName.substring(2);     //remove reference to current directory
        } 
        URL url = OSPRuntime.applet.getClass().getResource(path+tempName);
        if(url!=null) {
          return url;
        }
      }
    }
    return OSPRuntime.applet.getClass().getResource(name); // url not found in applet search paths
  }

  /**
   * Creates a Resource from a file path, if it is appropriate to do so.
   *
   * @param path the file path
   * @return the resource, if any
   */
  static private Resource createFileResource(String path) {
      // don't create file resources when in applet mode
	    // ignore paths that refer to zip or jar files
		return (OSPRuntime.applet != null || isHTTP(path) || path.indexOf(".zip") > -1 || path.indexOf(".jar") > -1 //$NON-NLS-1$ //$NON-NLS-2$
				|| path.indexOf(".trz") > -1 //$NON-NLS-1$
						? null
						: createFileResource(new File(path)));
  }

  /**
	 * Creates a Resource from a file. Checks that this is appropriate are assumed
	 * to have already been done.
   *
   * @param path the file path
   * @return the resource, if any
   */
	private static Resource createFileResource(File file) {
    try {
      if(file.exists()&&file.canRead()) {
        Resource res = new Resource(file);
        if(file.getName().endsWith("xset")) {                                    //$NON-NLS-1$
          xsetZipLoader = null;
        }
        OSPLog.finer("loaded file resource "+XML.forwardSlash(res.getAbsolutePath())); //$NON-NLS-1$
        return res;
      }
    } catch(Exception ex) {
      /** empty block */
    }
    return null;
  }

  /**
   * Creates a Resource from a URL.
   *
   * @param path the url path
   * @return the resource, if any
   */
  static private Resource createURLResource(String path) {
    // ignore paths that refer to zip or jar files unless explicitly OK
		if (!zipURLsOK && (path.indexOf(".zip") > -1 || path.indexOf(".jar") > -1 || path.indexOf(".trz") > -1)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      return null;
    }
    Resource res = null;
    // following added by Doug Brown 2009/11/14
    if(!JSUtil.isJS && OSPRuntime.applet!=null) {
      try { // let applet class try to get it first
        URL url = getAppletResourceURL(path);
        res = createResource(url);
      } catch(Exception ex) {
        /** empty block */
      }
    } // end code added by Doug Brown 2009/11/14
    if(res==null) {
      // if path includes protocol, use it directly
      if(path.indexOf(":/")>-1) {  //$NON-NLS-1$
        try {
//          URL url = new URL(path); // changed to use URI path 2011/09/11 DB
          URL url = getURLWithCachedBytes(path);          
          res = createResource(url);
        } catch(Exception ex) {
          /** empty block */
        }
      }
      // else if applet mode and relative path, search document and code base
      else {
        if((OSPRuntime.applet!=null)&&!path.startsWith("/")) {             //$NON-NLS-1$
          // first check document base
          URL docBase = OSPRuntime.applet.getDocumentBase();
          try {
            // following added by Doug Brown 2009/11/14
            String basePath = docBase.toString();
            // strip query, if any, from document base
            int n = basePath.indexOf("?");                                 //$NON-NLS-1$
            if(n>-1) {
              docBase = new URL(basePath.substring(0, n));
            }
            // end code added by Doug Brown 2009/11/14
            URL url = new URL(docBase, path);
            res = createResource(url);
          } catch(Exception ex) {
            /** empty block */
          }
          if(res==null) {
            URL codeBase = OSPRuntime.applet.getCodeBase();
            String s = XML.getDirectoryPath(docBase.toExternalForm())+"/"; //$NON-NLS-1$
            if(!codeBase.toExternalForm().equals(s)) {
              try {
                URL url = new URL(codeBase, path);
                res = createResource(url);
              } catch(Exception ex) {
                /** empty block */
              }
            }
          }
        }
      }
    }
    if(res!=null) {
      if(path.endsWith(".xset")) {                                  //$NON-NLS-1$
        xsetZipLoader = null;
      }
      OSPLog.finer("loaded URL resource "+XML.forwardSlash(res.getAbsolutePath())); //$NON-NLS-1$
    }
    return res;
  }

	/**
	 * Creates a Resource from within a zip or jar file.
	 *
	 * @param path the file path
	 * @return the resource, if any
	 */
//  @SuppressWarnings("resource") // Java 7
	static private Resource createZipResource(String path) {
		// convert to non-URI form
		path = getNonURIPath(path);

		// get separate zip base and relative file name
		String base = null;
		String fileName = path;
		// look for zip or jar base path
		int i = path.indexOf("zip!/"); //$NON-NLS-1$
		if (i == -1) {
			i = path.indexOf("jar!/"); //$NON-NLS-1$
		}
		if (i == -1) {
			i = path.indexOf("exe!/"); //$NON-NLS-1$
		}
		if (i == -1) {
			i = path.indexOf("trz!/"); //$NON-NLS-1$
		}
		if (i > -1) {
			base = path.substring(0, i + 3);
			fileName = path.substring(i + 5);
		}

		if (base == null) {
			if (path.endsWith(".zip") //$NON-NLS-1$
					|| path.endsWith(".trz") //$NON-NLS-1$
					|| path.endsWith(".jar") //$NON-NLS-1$
					|| path.endsWith(".exe")) { //$NON-NLS-1$
				String name = XML.stripExtension(XML.getName(path));
				base = path;
				fileName = name + ".xset"; //$NON-NLS-1$
			} else if (path.endsWith(".xset")) { //$NON-NLS-1$
				base = path.substring(0, path.length() - 4) + "zip"; //$NON-NLS-1$
			}
		}

		// if loading from a web file, download to OSP cache
		boolean isZip = base != null && (base.endsWith(".zip") || base.endsWith(".jar") || base.endsWith(".trz")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		boolean deleteOnExit = ospCache == null;
		if (isZip && isHTTP(path)) { // $NON-NLS-1$
			String zipFileName = XML.getName(base);
			File zipFile = downloadToOSPCache(base, zipFileName, false);
			if (zipFile != null) {
				if (deleteOnExit)
					zipFile.deleteOnExit();
				base = zipFile.getAbsolutePath();
				path = base + "!/" + fileName; //$NON-NLS-1$
			}
		}

		URL url = null;
		ZipEntry ze = null;
		if (base != null) { // car.trz
			ze = findZipEntry(base, fileName, false);
			if (ze != null) {
				url = getJarURLForFile(path);
			}
			if (url == null && zipLoaders != null) {
				// use existing zip loader, if any
				URLClassLoader zipLoader = zipLoaders.get(base);
				if (zipLoader != null) {
					url = zipLoader.findResource(fileName);
				} else {
					url = findInJarPath(base, fileName);
				}
			}
		}
		// if not found, use xset zip loader, if any
		if ((url == null) && zipLoaders != null && (xsetZipLoader != null)) {
			url = xsetZipLoader.findResource(fileName);
			if (url != null) {
				Iterator<String> it = zipLoaders.keySet().iterator();
				while (it.hasNext()) {
					Object key = it.next();
					if (zipLoaders.get(key) == xsetZipLoader) {
						base = (String) key;
						break;
					}
				}
			}
		}
		String launchJarPath;
		// if still not found, use launch jar loader, if any
		if ((url == null) && zipLoaders != null && ((launchJarPath = OSPRuntime.getLaunchJarPath()) != null)) {
			// BH n/a for SwingJS
			URLClassLoader zipLoader = zipLoaders.get(launchJarPath);
			if (zipLoader != null) {
				url = zipLoader.findResource(fileName);
			} else {
				url = findInJarPath(launchJarPath, fileName);
			}
			if (url != null) {
				base = launchJarPath;
			}
		}

		if (url != null) { // successfully found url
			// extract file if extension is flagged for extraction
			String ext = "." + XML.getExtension(url.toString());
			if (extractExtensions.contains(ext)) {
				String targetPath = fileName;
				File zip = new File(base);
				String parent = zip.getParent();
				// if target path is relative, resolve wrt parent folder of zip file
				if (parent != null && !targetPath.startsWith("/") //$NON-NLS-1$
						&& fileName.indexOf(":/") == -1) { //$NON-NLS-1$
					targetPath = XML.getResolvedPath(fileName, parent);
				}
				File target = new File(targetPath);
				if (!target.exists()) {
					if (OSPRuntime.isJS && ze != null) {
						OSPRuntime.jsutil.setFileBytes(target,  OSPRuntime.jsutil.getZipBytes(ze));
					} else {
						target = extract2(zip, fileName, target);
						if (deleteOnExit)
							target.deleteOnExit();
					}
				}
				return createFileResource(target);
			}
			try {
				Resource res = createResource(url);
				if ((res == null) || (res.getAbsolutePath().indexOf(path) == -1)) {
					return null;
				}
				OSPLog.finer("loaded zip resource " + XML.forwardSlash(res.getAbsolutePath())); //$NON-NLS-1$
				return res;
			} catch (IOException ex) {
				/** empty block */
			}
		}
		return null;
	}

	/**
	 * C:/temp/car.trz!/Car in a loop with friction.trk
	 * 
	 * becomes
	 * 
	 * jar:file:C:/temp/car.trz!/Car in a loop with friction.trk
	 * 
	 * 
	 * @param path
	 * @return jar URL
	 */
	public static URL getJarURLForFile(String fileName) {
		// URL constructor takes "jar" for any ZIP-based file (per Wikipedia)
		
		try {
			return new URL("jar", null, new URL("file", null, fileName).toString()); //$NON-NLS-1$ //$NON-NLS-2$

		} catch (MalformedURLException e) {
		}
		return null;
	}

	/**
	 * Look for a file in a JAR file or base path. 
	 * 
	 * @param launchJarPath
	 * @param fileName
	 * @param ZipEntry      -- JavaScript can use this to directly retrieve the
	 *                      byte[] data
	 * @return found url
	 * 
	 * @author hansonr 
	 */
	private static URL findInJarPath(String launchJarPath, String fileName) {
		try {
			// create new zip loader
			URL[] urls = new URL[] { new URL("file", null, launchJarPath) }; //$NON-NLS-1$
			URLClassLoader zipLoader = new URLClassLoader(urls);
			URL url = zipLoader.findResource(fileName);
			if (url == null) { // workaround works in IE?
				URL classURL = Resource.class.getResource("/" + launchJarPath); //$NON-NLS-1$
				if (classURL != null) {
					urls = new URL[] { classURL };
					zipLoader = new URLClassLoader(urls);
					url = zipLoader.findResource(fileName);
				}
			}
			if (url != null) {
				zipLoaders.put(launchJarPath, zipLoader);
				if (fileName.endsWith("xset")) { //$NON-NLS-1$
					xsetZipLoader = zipLoader;
				}
			}
			return url;
		} catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Creates a Resource from a class resource, typically in a jar file.
	 *
	 * @param name the resource name
	 * @param type the class providing the classloader
	 * @return the resource, if any
	 */
	static private Resource createClassResource(String name, Class<?> type) {
		// ignore any name that has a protocol
		if (name.indexOf(":/") != -1) { //$NON-NLS-1$
			return null;
		}
		String originalName = name;
		int i = name.indexOf("jar!/"); //$NON-NLS-1$
		if (i == -1) {
			i = name.indexOf("exe!/"); //$NON-NLS-1$
		}
		if (i != -1) {
			name = name.substring(i + 5);
		}
		Resource res = null;
		if (!name.startsWith("/")) {
			// /TEMP/ for example
			try { // check relative to root of jarfile containing specified class
					// BH now allows for retrieving cached data
				URL url = getTypeResource(type, "/" + name); //$NON-NLS-1$
				res = createResource(url);
			} catch (Exception ex) {
				// url data were not found in the j2s/ directory
			}
		}
		if (res == null) {
			try { // check relative to specified class
				URL url = getTypeResource(type, name);

				res = createResource(url);
			} catch (Exception ex) {
				/** empty block */
			}
		}
		// if resource is found, log and set launchJarName if not yet set
		if (res != null) {
			String path = XML.forwardSlash(res.getAbsolutePath());
			// don't return resources from Java runtime system jars
			if ((path.indexOf("/jre") > -1) && (path.indexOf("/lib") > -1)) { //$NON-NLS-1$ //$NON-NLS-2$
				return null;
			}
			// don't return resources that don't contain original name
			if (!getNonURIPath(path).contains(originalName) && !path.contains(originalName)) {
				return null;
			}
			if (name.endsWith("xset")) { //$NON-NLS-1$
				xsetZipLoader = null;
			}
			OSPLog.finer("loaded class resource: " + path); //$NON-NLS-1$
			OSPRuntime.setLaunchJarPath(path);
		}
		return res; // may be null
	}

  /**
   * First check to see if we have cached bytes. 
	 * 
   * @param type
   * @param path
   * @return
   */
	private static URL getTypeResource(Class<?> type, String path) {
		byte[] bytes = OSPRuntime.getCachedBytes(path);
		if (bytes != null) {
			return getURLWithCachedBytes(path);
		}
	return type.getResource(path);
}

	/**
	 * Creates a Resource.
	 *
	 * @param url the URL
	 * @return the resource, if any
	 * @throws IOException
	 */
	static private Resource createResource(URL url) throws IOException {
		if (url == null) {
			return null;
		}
		String path = url.toExternalForm();
		URL working = url;
		String content = null;

		// web-based zip files require special handling
		int n = path.toLowerCase().indexOf(".zip!/"); //$NON-NLS-1$
		if (n == -1)
			n = path.toLowerCase().indexOf(".jar!/"); //$NON-NLS-1$
		if (n == -1)
			n = path.toLowerCase().indexOf(".trz!/"); //$NON-NLS-1$
		if (n > -1 && isHTTP(path)) { 
			// check that url is accessible
			content = path.substring(n + 6);
			working = new URL(path.substring(0, n + 4));
			InputStream stream = working.openStream();
			if (stream.read() == -1) {
				stream.close();
				return null;
			}
			stream.close();
		}

		// BH 2020.03.28 URLs are checked when created; only a URI might not have a
		// target at this point. 2020.04.02 DB added following to check for target
		
		// check that url is a real target
		InputStream stream = working.openStream();
		if (stream.read() == -1) {
			stream.close();
			return null;
		}
		stream.close();
		
		Resource res = new Resource(working, content);
		return res;
	}


	private static Resource findResource(String path, Class<?> type, boolean searchFiles) {
		return findResource(path, type, searchFiles, false);
	}
	
	private static Resource findResource(String path, Class<?> type, boolean searchFiles, boolean createOnly) {

		if (!isHTTP(path)) {
			// BH 2020.04.23 don't do this for https://./xxxx
			path = path.replaceAll("/\\./", "/"); // This eliminates any embedded /./ //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (type == null) {
			type = Resource.class;
		}
		Resource res = null;

		// look for cached resource
		if (cacheEnabled) {
			res = resources.get(path);
			if ((res != null) && (searchFiles || (res.getFile() == null))) {
				OSPLog.finest("Found in cache: " + path); //$NON-NLS-1$
				return res;
			}
		}

		if (OSPRuntime.checkTempDirCache && !ResourceLoader.isHTTP(path) && new File(OSPRuntime.tempDir + path).exists()
				|| createOnly) {
			res = createClassResource(path = OSPRuntime.tempDir + path, type);
		} else if ((searchFiles && (res = createFileResource(path)) != null) || (res = createURLResource(path)) != null
				|| (res = createZipResource(path)) != null || (res = createClassResource(path, type)) != null) {
			// res is not null;
		}
		if (res != null && cacheEnabled) {
			resources.put(path, res);
		}
		return res;
	}


	  /**
	 * Never called -- was for applet only Finds the resource using only the class
	 * resource loader
	   */

		private static Resource findResourceInClass(String path, Class<?> type, boolean searchFiles) { 
		  // added by Paco
		  return findResource(path, type, searchFiles, true);
		}
  /**
   * Gets a path from a base path and file name.
   *
   * @param base the base path
   * @param name the file name
   * @return the path
   */
  private static String getPath(String base, String name) {
    if(base==null) {
      base = ""; //$NON-NLS-1$
    }
    if(base.endsWith(".jar") || base.endsWith(".zip") || base.endsWith(".trz")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      base += "!"; //$NON-NLS-1$
    }
    String path = XML.getResolvedPath(name, base);
    // correct the path so that it works with Mac
    if(OSPRuntime.isMac()&&path.startsWith("file:/")&&!path.startsWith("file:///")) { //$NON-NLS-1$ //$NON-NLS-2$
      path = path.substring(6);
      while(path.startsWith("/")) {                                                   //$NON-NLS-1$
        path = path.substring(1);
      }
      path = "file:///"+path;                                                         //$NON-NLS-1$
    }
    return path;
  }

	/**
	 * 
	 * Fix HTML being loaded from remote sources into HTMLDocuments and displayed
	 * using JEditorPane.
	 * 
	 * @param htmlStr
	 * @param url
	 * @return
	 * @author hansonr
	 */
	public static String fixHTTPS(String htmlStr, URL url) {
		if (url != null) {
			String dir = url.getPath();
			dir = dir.substring(0, dir.lastIndexOf("/") + 1);
			String base = "https://" + url.getHost() + "/" + dir;			
			htmlStr = htmlStr.replace("src=\"http", "#SH#");
			htmlStr = htmlStr.replace("src=\"", "src=\"" + base);
			htmlStr = htmlStr.replace("#SH#","src=\"http");
		}
		htmlStr = htmlStr.replace("http://physlets", "https://physlets"); 
		return htmlStr;
	}

	
	// BH 2020.04.14 from EjsTool

	public static String extractFiles(String modelPath, File sourceDir, List<Object> finalList,
			File destinationDirectory) {
		// Extract files
		destinationDirectory.mkdirs();
		int policy = NO;
		for (Iterator<?> it = finalList.iterator(); it.hasNext();) {
			String resource = (String) it.next();
			File targetFile = resource.startsWith("./") ? new File(destinationDirectory, resource.substring(2)) //$NON-NLS-1$
					: new File(sourceDir, resource);
			if (targetFile.exists()) {
				switch (policy) {
				case NO_TO_ALL:
					continue;
				case YES_TO_ALL:
					break; // will overwrite
				default:
					switch (policy = confirmOverwrite(resource)) {
					case NO_TO_ALL:
					case NO:
						continue;
					default: // Do nothing, i.e., will overwrite the file
					}
				}
			}
			String originalName = resource.startsWith("./") ? modelPath + resource.substring(2) : resource; //$NON-NLS-1$
			File result = extract(originalName, targetFile, true); 
			if (result == null)
				return originalName;
		}
		return null;
	}

	// BH 2020.04.14 from JarFile

	  /**
	   * Extracts a given file from a compressed (ZIP, JAR or TRZ) file
	 * 
	   * @param source File The compressed file to extract the file from
	   * @param filename String The path of the file to extract
	 * @param destination String The full (or relative to whatever the current user
	 *                    directory is) path where to save the extracted file
	   * @return File The extracted file, null if failed
	   */ 
		static private File extract2(File source, String fileName, File target) {
			String targetName = target.toString();
			System.out.println("RL extracting " + fileName + " " + targetName + " from " + source);
			int flen = (fileName.endsWith("/") ? fileName.length() : 0);
			FileOutputStream fos = null;
			try (FileInputStream fis = new FileInputStream(source);
					ZipInputStream zis = new ZipInputStream(fis);) {
				ZipEntry ze;
				while ((ze = zis.getNextEntry()) != null && flen >= 0) {
					String name = ze.getName();
					if (flen == 0 && name.equals(fileName)) {
						flen = -1;
//						break;
					} else if (flen > 0 && name.startsWith(fileName)) {
						target = new File(targetName + name.substring(flen));
					} else {
						continue;
					}
					File parent = target.getParentFile();
					if (parent != null) {
						parent.mkdirs();
					}
					fos = new FileOutputStream(target);
					ResourceLoader.getLimitedStreamBytes(zis, ze.getSize(), fos);
					fos.close();
					System.out.println("RL extracted " + targetName + " " + target.length());
				}
			} catch (IOException e2) {
				return null;
			}
			return target;
		}
	  
// BH 2020.04.14 abandoned
//		
//		  private static Map<String, Map<String, ZipEntry>> jarContents = new HashMap<String, Map<String, ZipEntry>>(); // added by D Brown 2007-10-31
//
//	  /**
//	   * Extracts a given file from a compressed (ZIP, JAR or TRZ) file
//	   * Extensive changes by D Brown 2007-10-31
//	   * @param source File The compressed file to extract the file from
//	   * @param filename String The path of the file to extract
//	   * @param target File The target file for the extracted file
//	   * @return File The extracted file, null if failed
//	   */
//	  static private File extract0(File source, String filename, File target) {
//	    if((source.exists()==false)||(filename==null)||(filename.trim().length()<1)||(target==null)) {
//	      return null;
//	    }
//		System.out.println("extracting " + filename + " " + target + " from " + source);
//	    boolean isDirectory = filename.endsWith("/"); //$NON-NLS-1$
//	    try {
//	    	
//	      // get contents Map of filename to ZipEntry for source jar
//	      Map<String, ZipEntry> contents = jarContents.get(source.getPath());
//	      if(contents==null) {
//	        // create new Map and fill it
//	        contents = new HashMap<String, ZipEntry>();
//	        jarContents.put(source.getPath(), contents);
//	        OSPRuntime.addJSCachedBytes(source);
//	        ZipInputStream input = new ZipInputStream(new FileInputStream(source));
//	        ZipEntry zipEntry = null;
//	        while((zipEntry = input.getNextEntry())!=null) {
//	          if(zipEntry.isDirectory()) {
//	            continue;                               // don't include directories
//	          }
//	          contents.put(zipEntry.getName(), zipEntry);
//	        }
//	        input.close();
//	      }
//	      if(isDirectory) {
//	        // target is a directory: extract all contained files
//	        Iterator<String> it = contents.keySet().iterator();
//	        while(it.hasNext()) {
//	          String next = it.next();
//	          if(next.startsWith(filename)) {
//	            // next is in the directory, so extract it
//	            ZipEntry zipEntry = contents.get(next);
//	            // construct new target for the file
//	            int n = filename.length();
//	            File newTarget = new File(target, zipEntry.getName().substring(n));
//	            extract0(source, next, newTarget);
//	          }
//	        }
//	        return target;
//	      }
//	      // target is a file
//	      ZipEntry entry = contents.get(filename);      
//	      ZipFile input = new ZipFile(source);
//	      InputStream in = input.getInputStream(entry); // A stream to read the entry
//	      
//	      File parent = target.getParentFile();
//	      if(parent!=null) {
//	        parent.mkdirs();
//	      }
//	      int bytesRead;
//	      byte[] buffer = new byte[1024];
//	      FileOutputStream output = new FileOutputStream(target);
//	      while((bytesRead = in.read(buffer))!=-1) {
//	        output.write(buffer, 0, bytesRead);
//	      }
//	      output.close();
//	      input.close();
//			System.out.println("extracted " + target + " " + target.length());
//	      return target;
//	    } catch(Exception ex) {
//	      ex.printStackTrace();
//	    }
//	    return null;
//	  }
//
		static public File extract(String filename, File target) {
			return extract(filename, target, true);
		}

	/**
	 * Extracts a file using the ResourceLoader utility
	 * 
	 * @param filename String The path of the file to extract
	 * @param target   File The target file for the extracted file
	 * @return File The extracted file, null if failed
	 */
	static public File extract(String filename, File target, boolean forceFileCreation) {
		// System.out.println("Extract filename="+filename); //$NON-NLS-1$
		if ((filename == null) || (filename.trim().length() <= 0) || (target == null)) {
			return null;
		}
		try {
			InputStream inputStream = null;
			if (OSPRuntime.applet != null) {// added by Wolfgang Christian
				// URL url=OSPRuntime.applet.getClass().getResource(filename);
				// inputStream=url.openStream();
				inputStream = OSPRuntime.applet.getClass().getResourceAsStream(filename);
			}
			if (inputStream == null) { // use resource loader when not an applet
				if (isHTTP(filename)) { // $NON-NLS-1$
					int n = filename.toLowerCase().indexOf(".zip!/"); //$NON-NLS-1$
					if (n == -1)
						n = filename.toLowerCase().indexOf(".jar!/"); //$NON-NLS-1$
					if (n == -1)
						n = filename.toLowerCase().indexOf(".trz!/"); //$NON-NLS-1$
					if (n > -1) {
						File extracted = extractFileFromZIP(filename, target, false, forceFileCreation);
						if (extracted != null)
							return extracted;
					}
				} else {
					Resource is = getResource(filename, false);
					inputStream = (is == null ? null : is.openInputStream());
				}
			}
			if (inputStream == null) {
				return null;
			}
			BufferedInputStream input = new BufferedInputStream(inputStream);
			target.getParentFile().mkdirs();
			int bytesRead;
			byte[] buffer = new byte[1024];
			FileOutputStream output = new FileOutputStream(target);
			while ((bytesRead = input.read(buffer)) != -1) {
				output.write(buffer, 0, bytesRead);
			}
			output.close();
			input.close();
			return target;
		} catch (Exception exc) {
			System.err.println("JarTool extract resource error.  Filename=" + filename); //$NON-NLS-1$
			exc.printStackTrace();
			return null;
		}
	}

	  // -----------------------------------
	  //        Private methods
	  // -----------------------------------

	  static public int confirmOverwrite(String filename) {
	    return confirmOverwrite(filename, false);
	  }

	  // ---- Localization
	  static private final String JAR_TOOL_BUNDLE_NAME = "org.opensourcephysics.resources.tools.tools"; //$NON-NLS-1$
	  
		public static ResourceBundle getBundle(String bundleName, Locale resourceLocale) {
			return ResourceBundle.getBundle(bundleName, resourceLocale, ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_PROPERTIES));
		}

		public static ResourceBundle getBundle(String bundleName) {
			return ResourceBundle.getBundle(bundleName, Locale.getDefault(), ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_PROPERTIES));
		}

	  static private ResourceBundle res = getBundle(JAR_TOOL_BUNDLE_NAME);

	  static public void setLocale(Locale locale) {
		  res = getBundle(JAR_TOOL_BUNDLE_NAME, locale);
	  }

	  static public final int YES = 0;
	  static public final int NO = 1;
	  static public final int YES_TO_ALL = 2;
	  static public final int NO_TO_ALL = 3;
	  static public final int CANCEL = 4;
	  

	  static private class OverwriteValue {
		    int value = NO;

		    OverwriteValue(int val) {
		      value = val;
		    }

		  }

	  /**
	   * Whether to overwrite an existing file.
	 * 
	   * @param file File
	   * @return boolean
	   */
	  static public int confirmOverwrite(String filename, boolean canCancel) {
	    final JDialog dialog = new JDialog();
	    final OverwriteValue returnValue = new OverwriteValue(NO);
	    java.awt.event.MouseAdapter mouseListener = new java.awt.event.MouseAdapter() {
	      @Override
		public void mousePressed(java.awt.event.MouseEvent evt) {
	        AbstractButton button = (AbstractButton) (evt.getSource());
	        String aCmd = button.getActionCommand();
	        if(aCmd.equals("yes")) {             //$NON-NLS-1$   
	          returnValue.value = YES;
	        } else if(aCmd.equals("no")) {       //$NON-NLS-1$   
	          returnValue.value = NO;
	        } else if(aCmd.equals("yesToAll")) { //$NON-NLS-1$   
	          returnValue.value = YES_TO_ALL;
	        } else if(aCmd.equals("noToAll")) {  //$NON-NLS-1$   
	          returnValue.value = NO_TO_ALL;
	        } else if(aCmd.equals("cancel")) {  //$NON-NLS-1$   
	          returnValue.value = CANCEL;
	        }
	        dialog.setVisible(false);
	      }

	    };
	    JButton yesButton = new JButton(res.getString("JarTool.Yes")); //$NON-NLS-1$
	    yesButton.setActionCommand("yes"); //$NON-NLS-1$
	    yesButton.addMouseListener(mouseListener);
	    JButton noButton = new JButton(res.getString("JarTool.No")); //$NON-NLS-1$
	    noButton.setActionCommand("no"); //$NON-NLS-1$
	    noButton.addMouseListener(mouseListener);
	    JButton yesToAllButton = new JButton(res.getString("JarTool.YesToAll")); //$NON-NLS-1$
	    yesToAllButton.setActionCommand("yesToAll"); //$NON-NLS-1$
	    yesToAllButton.addMouseListener(mouseListener);
	    JButton noToAllButton = new JButton(res.getString("JarTool.NoToAll")); //$NON-NLS-1$
	    noToAllButton.setActionCommand("noToAll"); //$NON-NLS-1$
	    noToAllButton.addMouseListener(mouseListener);
	    JButton cancelButton = new JButton(res.getString("JarTreeDialog.Button.Cancel")); //$NON-NLS-1$
	    cancelButton.setActionCommand("cancel"); //$NON-NLS-1$
	    cancelButton.addMouseListener(mouseListener);
	    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
	    buttonPanel.add(yesButton);
	    buttonPanel.add(yesToAllButton);
	    buttonPanel.add(noButton);
	    buttonPanel.add(noToAllButton);
		if (canCancel)
			buttonPanel.add(cancelButton);
	    JLabel label = new JLabel(DisplayRes.getString("DrawingFrame.ReplaceExisting_message")+" "+ //$NON-NLS-1$ //$NON-NLS-2$
	      filename+DisplayRes.getString("DrawingFrame.QuestionMark")); //$NON-NLS-1$
	    label.setHorizontalAlignment(SwingConstants.CENTER);
	    label.setBorder(new javax.swing.border.EmptyBorder(10, 10, 10, 10));
	    dialog.setTitle(DisplayRes.getString("DrawingFrame.ReplaceFile_option_title")); //$NON-NLS-1$
	    dialog.getContentPane().setLayout(new java.awt.BorderLayout(5, 0));
	    dialog.getContentPane().add(label, java.awt.BorderLayout.CENTER);
	    dialog.getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);
	    dialog.addWindowListener(new java.awt.event.WindowAdapter() {
	      @Override
		public void windowClosing(java.awt.event.WindowEvent event) {
	        returnValue.value = NO;
	      }

	    });
	    dialog.validate();
	    dialog.pack();
	    dialog.setLocationRelativeTo(null);
	    dialog.setModal(true);
	    dialog.setVisible(true);
	    return returnValue.value;
	  }

	public static boolean isHTTP(String path) {
		  return path.startsWith("http:") || path.startsWith("https:");
	  }

	/**
	 * From Resource.
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static InputStream openZipEntryStream(URL url) throws IOException {
		// BH: Use JarInputStream, as it is more reliable and oh so easier.
		
		if (OSPRuntime.isJS) {
			byte[] bytes = OSPRuntime.jsutil.getURLBytes(url);
			if (bytes == null) {
				bytes = getZipEntryBytes(url.toString(), null);
				OSPRuntime.jsutil.setURLBytes(url, bytes);
			}
			return (bytes == null ? null : new ByteArrayInputStream(bytes));
		}
	
		if (url.getProtocol() != "jar")
			url = new URL("jar", null, url.toString());
		return url.openStream();
//		String zipContent = url.toString();
//		BufferedInputStream bufIn = new BufferedInputStream(url.openStream());
//		ZipInputStream input = new ZipInputStream(bufIn);
//		ZipEntry zipEntry = null;
//		while ((zipEntry = input.getNextEntry()) != null) {
//			if (zipEntry.isDirectory())
//				continue;
//			String filename = zipEntry.getName();
//			if (zipContent.contains(filename)) {
//				return input;
//			}
//		}
//		return null;
	}

	public static InputStream openStream(URL url) throws IOException {
		// TODO SwingJS
		return url.openStream();
	}

	  /**
	   * Gets an image. May return null.
	   * 
	   * @param path the path
	   * @return the image
	   */
	  public static Image getImage(String path) {
	    URL url = getAppletResourceURL(path); // added by W. Christian
	    if(url!=null) {
	      return new ImageIcon(url).getImage();
	    }
	    Resource res = getResource(path);
	    return(res==null) ? null : res.getImage();
	  }

	  static String myPath = "/org/opensourcephysics/resources/";
	  static String imagezipPath = myPath + "images.zip";
	  
	  public static void setAssetPath(String filePath, String assetsZip) {
	  	myPath=filePath;
	  	imagezipPath=myPath+assetsZip;
			OSPLog.debug("imagezipPath="+imagezipPath);
	  }
		  
	  
	public static URL getImageZipResource(String imagePath) {
		
		if (OSPRuntime.isJS) {
			String[] parts = null;
			if (imagePath.indexOf("!/") < 0) {
				// "/org/opensourcephysics/resources/controls/images/xxxxx.jpg"
				int pt = 0;
				if (imagePath.indexOf(myPath) == 0 && (pt = imagePath.indexOf("/images/")) >= 0) {
					String fileName = imagePath.substring(myPath.length(), pt) + imagePath.substring(pt + 7);
				    parts = new String[] {imagezipPath, fileName};
				}
			} else {
				parts = getJarURLParts(imagePath);				
			}
			try {
				ZipEntry ze;
				if (parts != null && (ze = findZipEntry(parts[0], parts[1], false)) != null) {
					URL ret = new URL("file", null, imagePath);
					OSPRuntime.jsutil.setURLBytes(ret, OSPRuntime.jsutil.getZipBytes(ze));
					if (ret != null)
						return ret;			
				}
			} catch (IOException e) {
				// fall back
			}
		}
		return ResourceLoader.class.getResource(imagePath);
	}

	/**
	 * Just get the URL contents as a byte array
	 * 
	 * @param url
	 * @return byte[]
	 * 
	 * @author hansonr
	 */
	public static byte[] getURLContents(URL url) {
		try {
			if (OSPRuntime.isJS) {				
				// Java 9!			return new String(url.openStream().readAllBytes());
				return OSPRuntime.jsutil.readAllBytes(url.openStream());
			}
			return ResourceLoader.getLimitedStreamBytes(url.openStream(), -1, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Ret the URL contents as a string
	 * 
	 * @param url
	 * @return byte[]
	 * 
	 * @author hansonr
	 */
	public static void getURLContentsAsync(URL url, Function<byte[], Void> whenDone) {
		try {
			if (OSPRuntime.isJS) {
				// Java 9! return new String(url.openStream().readAllBytes());
				OSPRuntime.getURLBytesAsync(url, whenDone);
				return;
			}
			whenDone.apply(ResourceLoader.getLimitedStreamBytes(url.openStream(), -1, null));
			return;
		} catch (IOException e) {
			e.printStackTrace();
			whenDone.apply(null);
		}
	}

	/**
	 * From javajs.Rdr
	 * 
	 */
	public static byte[] getLimitedStreamBytes(InputStream is, long n, OutputStream out) throws IOException {
	
		// Note: You cannot use InputStream.available() to reliably read
		// zip data from the web.
	
		boolean toOut = (out != null);
		int buflen = (n > 0 && n < 1024 ? (int) n : 1024);
		byte[] buf = new byte[buflen];
		byte[] bytes = (out == null ? new byte[n < 0 ? 4096 : (int) n] : null);
		int len = 0;
		int totalLen = 0;
		if (n < 0)
			n = Integer.MAX_VALUE;
		while (totalLen < n && (len = is.read(buf, 0, buflen)) > 0) {
			totalLen += len;
			if (toOut) {
				out.write(buf, 0, len);
			} else {
				if (totalLen > bytes.length)
					bytes = Arrays.copyOf(bytes, totalLen * 2);
				System.arraycopy(buf, 0, bytes, totalLen - len, len);
				if (n != Integer.MAX_VALUE && totalLen + buflen > bytes.length)
					buflen = bytes.length - totalLen;
				}
		}
		if (toOut) 
			return null;
		if (totalLen == bytes.length)
			return bytes;
		buf = new byte[totalLen];
		System.arraycopy(bytes, 0, buf, 0, totalLen);
		return buf;
	}

	public static File copyURLtoFile(String urlPath, String filePath) { 	
		File f = new File(filePath);
		try {
			if (OSPRuntime.isJS) {
				FileOutputStream fos = new FileOutputStream(f);
				OSPRuntime.jsutil.transferTo(new URL(urlPath).openStream(), fos);
			} else {
				Path path = f.toPath();
				Files.createDirectories(path.getParent());
				Files.write(path, getURLContents(new URL(urlPath)));
			}
		} catch (Exception e) { 
			e.printStackTrace();
		}
		return f;
	}

	public static void copyURLtoFileAsync(String urlPath, String filePath, Function<File, Void> whenDone) {
		
		File f = new File(filePath);
		
		try {
			  if (OSPRuntime.isJS) { 
				getURLContentsAsync(new URL(urlPath), new Function<byte[], Void>() {

					@Override
					public Void apply(byte[] bytes) {
							FileOutputStream fos;
							try {
								fos = new FileOutputStream(f);
								OSPRuntime.jsutil.transferTo(new ByteArrayInputStream(bytes), fos);
								fos.close();
								whenDone.apply(f);
							} catch (IOException e) {
							}
						return null;
}

				});
				} else {
					Path path = f.toPath();
					Files.createDirectories(path.getParent());
					Files.write(path, getURLContents(new URL(urlPath)));
				}

		} catch (Exception e) {
			e.printStackTrace();
		}
		whenDone.apply(f);
	}


	public static BufferedReader readerForStream(InputStream stream, String encoding) {
		if (encoding == null)
			encoding = "UTF-8";
		try {
			return (stream == null ? null : new BufferedReader(new InputStreamReader(stream, encoding)));
		} catch (UnsupportedEncodingException e) {
			return null;
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
 * Copyright (c) 2007 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
