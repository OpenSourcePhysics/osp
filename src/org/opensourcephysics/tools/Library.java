/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.LibraryBrowser.LibraryHistory;

/**
 * A Library for a LibraryBrowser. Maintains lists of collection paths and
 * imported sub-libraries.
 *
 * @author Douglas Brown
 */
public class Library {

	/**
	 * A class that combines an ArrayList with a matching HashMap. 
	 * Sort of like a LinkedHashMap with a .get(i) method.
	 * 
	 * The class can be of TYPE_LIB(1) or TYPE_NAME(2). The first
	 * maps a path to a Library object; the second just to a name.
	 * 
	 * @author hanso
	 *
	 */
	public static class ListMap extends ArrayList<String> {
		
		private final static int TYPE_LIB = 1;
		private final static int TYPE_NAME = 2;
		
		HashMap<String, Library> libMap;
		HashMap<String, String> nameMap;
	
		int type;
		
		protected ListMap(int type) {
			this.type = type;
		}
		
	    @Override
		public void clear() {
	    	super.clear();
	    	if (libMap != null)
	    		libMap.clear();
	    	if (nameMap != null)
	    		nameMap.clear();
	    }

	    @Override 
	    public String get(int i){
	    	return super.get(i);
	    }
	    
	    private boolean addItem(String path) {
	    	return super.add(path);
	    }
		@Override
		@Deprecated
		public boolean add(String path) {
			throw new RuntimeException("LibraryMap can't use add");				
		}
		

		public Library getLibrary(String path) {
			return (libMap == null ? null : libMap.get(path));
		}
		
		public String getLibraryName(String path) {
			Library lib = getLibrary(path);
			return (lib == null ? null : lib.getName());
		}
		
		public String getMappedName(String path) {
			return (nameMap == null ? null : nameMap.get(path));
		}

		public String getPath(int i) {
			return get(i);
		}
		public String getMappedName(int i) {
			return getMappedName(get(i));
		}

		public boolean addName(String path, String name) {
		    ensure(TYPE_NAME);
			path = path.trim();
			// don't add duplicate paths
			if (contains(path))
				return false;
			addItem(path);
			nameMap.put(path,  name.trim());
			return true;
		}


		private void ensure(int requiredType) {
			if (requiredType != type)
				throw new RuntimeException("invalid entry to LibraryMap");
			switch (type) {
			case TYPE_NAME:
				if (nameMap == null)
					nameMap = new HashMap<>();
				break;
			case TYPE_LIB:
				if (libMap == null)
					libMap = new HashMap<>();
				break;
			}
		}

		public Collection<String> getNames() {
			ensure(TYPE_NAME);
			return nameMap.values();
		}
		
		private boolean addLibrary(String path, Library library) {
		    ensure(TYPE_LIB);
			if (contains(path))
				return false;
			addItem(path);
			libMap.put(path, library);
			return true;
		}

		private static boolean addAndCreateLibrary(String path, ListMap listMap) {
		    listMap.ensure(TYPE_LIB);
			if (listMap.contains(path))
				return false;
			synchronized (listMap) {
				XMLControl control = new XMLControlElement(path);
				if (control.failedToRead() || control.getObjectClass() != Library.class) {
					return false;
				}
				Library library = new Library();
				control.loadObject(library);
				listMap.addItem(path);
				listMap.libMap.put(path, library);
			}
			return true;
		}

		public String[] toStringArray() {
			return toArray(new String[size()]);
		}

	}

	private String name; // name of the library
	
	ListMap pathNameMap = new ListMap(ListMap.TYPE_NAME);
	ListMap comPADREMap = new ListMap(ListMap.TYPE_NAME);
	ListMap ospLibMap = new ListMap(ListMap.TYPE_LIB);
	ListMap importedLibMap = new ListMap(ListMap.TYPE_LIB);
	ListMap subpathLibMap = new ListMap(ListMap.TYPE_LIB);
	
	private HashMap<String, String> allPathsToNameMap = new HashMap<String, String>();
	
	private Set<String> noSearchSet = new TreeSet<String>();

	/**
	 * Adds and creates an OSP-sponsored library. 
	 * 
	 * OSP libraries are not under user control.
	 * 
	 * @param path the library path
	 * @return true if successfully added
	 */
	protected boolean addOSPLibrary(String path) {
		return ListMap.addAndCreateLibrary(path, ospLibMap);
	}

	/**
	 * Imports and creates a library. 
	 * 
	 * Imported libraries are managed by the user.
	 * 
	 * @param path the library path
	 * @return true if successfully imported
	 */
	protected boolean importLibrary(String path) {
		return ListMap.addAndCreateLibrary(path, importedLibMap);
	}

	/**
	 * Imports a Library if not already imported.
	 * 
	 * @param path    the path to the library
	 * @param library the library
	 * @return true if imported
	 */
	protected boolean importLibrary(String path, Library library) {
		return importedLibMap.addLibrary(path, library);
	}


	/**
	 * Adds a sublibrary. Sublibraries are shown as submenus in a Library's
	 * Collections menu. Sublibraries are not under user control.
	 * 
	 * @param path the path to the sublibrary
	 * @return true if successfully added
	 */
	protected boolean addSubLibrary(String path) {
		return ListMap.addAndCreateLibrary(path, subpathLibMap);
	}

	/**
	 * Adds a comPADRE collection. ComPADRE collections are not under user control.
	 * 
	 * @param path the comPADRE query
	 * @param name the name of the collection
	 * @return true if successfully added
	 */
	protected boolean addComPADRECollection(String path, String name) {
		return comPADREMap.addName(path, name);
	}

	/**
	 * Gets the names of all collections maintained by this library.
	 * 
	 * @return a collection of names
	 */
	protected Collection<String> getNames() {
		return pathNameMap.getNames();
	}

	/**
	 * Returns true if this library has no collections.
	 * 
	 * @return true if empty
	 */
	protected boolean isEmpty() {
		return pathNameMap.isEmpty();
	}

	/**
	 * Returns true if this library contains a collection path.
	 * 
	 * @param path     the collection path
	 * @param allLists true to search in all collection lists
	 * @return true if this contains the path
	 */
	protected boolean containsPath(String path, boolean allLists) {
		path = path.trim();
		int n = path.indexOf(LibraryComPADRE.PRIMARY_ONLY);
		if (n >= 0)
			path = path.substring(0, n);
		return pathNameMap.contains(path)
				|| (allLists && (comPADREMap.contains(path) || ospLibMap.contains(path)));
	}

	/**
	 * Adds a collection to this library.
	 * 
	 * @param path the path to the collection
	 * @param name the menu item name for the collection
	 */
	protected void addCollection(String path, String name) {
		path = path.trim();
		if (pathNameMap.contains(path))
			return;
		name = name.trim();
		pathNameMap.addName(path, name);
		allPathsToNameMap.put(path, name);
	}

	/**
	 * Renames a collection.
	 * 
	 * @param path    the path to the collection
	 * @param newName the new name
	 */
	protected void renameCollection(String path, String newName) {
		path = path.trim();
		// change only paths that have already been added
		if (!pathNameMap.contains(path))
			return;
		newName = newName.trim();
		pathNameMap.addName(path, newName);
		allPathsToNameMap.put(path, newName);
	}

	/**
	 * Returns all collection paths in this Library and sub-libraries.
	 * 
	 * @return array of paths
	 */
	protected TreeSet<String> getAllPaths() {
		TreeSet<String> paths = new TreeSet<String>();
		paths.addAll(pathNameMap);
		paths.addAll(comPADREMap);

		if (!subpathLibMap.isEmpty()) {
			for (String path : subpathLibMap) {
				Library library = subpathLibMap.getLibrary(path);
				paths.addAll(library.getAllPaths());
			}
		}
		for (String path : ospLibMap) {
			Library library = ospLibMap.getLibrary(path);
			paths.addAll(library.getAllPaths());
		}
		return paths;
	}

	/**
	 * Returns a Map of path-to-tabname.
	 * 
	 * @return path-to-name map
	 */
	protected HashMap<String, String> getAllPathsToNameMap() {
		return allPathsToNameMap;
	}

	/**
	 * Returns a string representation of this library.
	 * 
	 * @return the name of the library
	 */
	@Override
	public String toString() {
		return getName();
	}

//_____________________ protected and private methods _________________________

	/**
	 * Sets the name of this library.
	 * 
	 * @param name the name
	 */
	protected void setName(String name) {
		if (name == null) {
			name = OSPRuntime.getUserHome().replace('\\', '/');
			if (name.endsWith("/")) { //$NON-NLS-1$
				name = name.substring(0, name.length() - 1);
			}
			name = XML.getName(name) + " " + ToolsRes.getString("Library.Name"); //$NON-NLS-1$//$NON-NLS-2$
		}
		this.name = name;
	}

	/**
	 * Gets the name of this library.
	 * 
	 * @return the name
	 */
	protected String getName() {
		return name;
	}

	/**
	 * Saves this library in an xml file.
	 * 
	 * @param path the path to the saved file
	 */
	protected void save(String path) {
		if (path != null) {
			//System.out.println("Library save to " + path);
			new XMLControlElement(this).write(path);
		}
	}

	/**
	 * Loads this library from an xml file.
	 * 
	 * @param path the path to the file
	 */
	protected void load(String path) {
		if (path != null)
			new XMLControlElement(path).loadObject(this);
	}

	/**
	 * Gets a clone of this library that is suitable for exporting. The exported
	 * library has no OSP libraries, ComPADRE collections or imported libraries.
	 * 
	 * @return a Library for export
	 */
	protected Library getCloneForExport() {
		Library lib = new Library();
		lib.pathNameMap = pathNameMap;
		lib.name = name;
		return lib;
	}

	/**
	 * Returns an ObjectLoader to save and load data for this class.
	 *
	 * @return the object loader
	 */
	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

	/**
	 * A class to save and load data for this class.
	 */
	static class Loader implements XML.ObjectLoader {

		/**
		 * Saves an object's data to an XMLControl.
		 *
		 * @param control the control to save to
		 * @param obj     the object to save
		 */
		@Override
		public void saveObject(XMLControl control, Object obj) {
			Library library = (Library) obj;
			LibraryHistory history = LibraryBrowser.getHistory();
			control.setValue("name", library.getName()); //$NON-NLS-1$
			if (!library.pathNameMap.isEmpty()) {
				String[] paths = library.pathNameMap.toStringArray();
				control.setValue("collection_paths", paths); //$NON-NLS-1$
				String[] names = new String[paths.length];
				for (int i = 0; i < paths.length; i++) {
					names[i] = library.pathNameMap.getMappedName(paths[i]);
				}
				control.setValue("collection_names", names); //$NON-NLS-1$
			}
			if (!library.subpathLibMap.isEmpty()) {
				String[] paths = library.subpathLibMap.toStringArray();
				control.setValue("sublibrary_paths", paths); //$NON-NLS-1$
			}
			if (!library.importedLibMap.isEmpty()) {
				String[] paths = library.importedLibMap.toStringArray();
				control.setValue("imported_library_paths", paths); //$NON-NLS-1$
			}

			String[] paths;
			if (history != null && history.isEnabled()) {
				paths = history.getOpenTabPaths();
				if (paths != null)
					control.setValue("open_tabs", paths); //$NON-NLS-1$
				String dir = history.getChooserDir();
				if (dir != null)
					control.setValue("chooser_directory", XML.forwardSlash(dir)); //$NON-NLS-1$

				ArrayList<String> tabs = history.getRecentTabs();
				if (tabs != null && !tabs.isEmpty()) {
					paths = tabs.toArray(new String[0]);
					control.setValue("recently_opened", paths); //$NON-NLS-1$
					String[] names = new String[paths.length];
					for (int i = 0; i < names.length; i++) {
						names[i] = library.getAllPathsToNameMap().get(paths[i]);
						if (names[i] == null)
							names[i] = XML.getName(paths[i]);
					}
					control.setValue("recently_opened_names", names); //$NON-NLS-1$
				}
			}
			if (!library.noSearchSet.isEmpty()) {
				paths = library.noSearchSet.toArray(new String[0]);
				control.setValue("no_search_paths", paths); //$NON-NLS-1$
			}
			File cache = ResourceLoader.getOSPCache();
			if (cache != null) {
				control.setValue("cache", cache.getPath()); //$NON-NLS-1$
			}
			//System.out.println("Library.Loader.saveObject\n" + control);
		}

		/**
		 * Creates a new object.
		 *
		 * @param control the XMLControl with the object data
		 * @return the newly created object
		 */
		@Override
		public Object createObject(XMLControl control) {
			return new Library();
		}

		/**
		 * Loads an object with data from an XMLControl.
		 *
		 * @param control the control
		 * @param obj     the object
		 * @return the loaded object
		 */
		@Override
		public Object loadObject(XMLControl control, Object obj) {
			//System.out.println(control);
			Library library = (Library) obj;
			LibraryHistory history = LibraryBrowser.getHistory();
			library.setName(control.getString("name")); //$NON-NLS-1$
			String[] paths = (String[]) control.getObject("collection_paths"); //$NON-NLS-1$
			if (paths != null) {
				String[] names = (String[]) control.getObject("collection_names"); //$NON-NLS-1$
				library.pathNameMap.clear();
				for (int i = 0; i < paths.length; i++) {
					if (paths[i] == null || names[i] == null)
						continue;
					library.pathNameMap.addName(paths[i], names[i]);
					library.allPathsToNameMap.put(paths[i], names[i]);
				}
			}
			paths = (String[]) control.getObject("sublibrary_paths"); //$NON-NLS-1$
			if (paths != null) {
				for (String path : paths) {
					library.addSubLibrary(path);
				}
			}
			paths = (String[]) control.getObject("imported_library_paths"); //$NON-NLS-1$
			if (paths != null) {
				for (String path : paths) {
					library.importLibrary(path);
				}
			}
			paths = (String[]) control.getObject("recently_opened"); //$NON-NLS-1$
			String[] names = (String[]) control.getObject("recently_opened_names"); //$NON-NLS-1$
			if (paths != null) {
				for (String path : paths) {
					history.addRecent(path, true); // add at end
				}
				if (names != null) {
					for (int i = 0; i < names.length; i++) {
						library.getAllPathsToNameMap().put(paths[i], names[i]);
					}
				}
			}
			paths = (String[]) control.getObject("no_search_paths"); //$NON-NLS-1$
			if (paths != null) {
				for (String path : paths) {
					library.noSearchSet.add(path);
				}
			}
			history.setOpenTabPaths((String[]) control.getObject("open_tabs")); //$NON-NLS-1$
			String dir = control.getString("chooser_directory");
			if (dir != null)
				history.setChooserDir(dir); //$NON-NLS-1$
			// set cache only if it has not yet been set
			if (ResourceLoader.getOSPCache() == null) {
				ResourceLoader.setOSPCache(control.getString("cache")); //$NON-NLS-1$
			}
			return obj;
		}
	}

	protected Set<String> getNoSearchSet() {
		return noSearchSet;
	}

	protected ListMap getImportedPathToLibraryMap() {
		return importedLibMap;
	}

	protected ListMap getPathToNameMap() {
		return pathNameMap;
	}

	protected ListMap getComPADREPathToNameMap() {
		return comPADREMap;
	}

	protected ListMap getOSPPathToLibraryMap() {
		return ospLibMap;
	}

	protected ListMap getSubPathToLibraryMap() {
		return subpathLibMap;
	}

}
