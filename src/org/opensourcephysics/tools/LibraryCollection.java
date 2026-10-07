/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import java.util.ArrayList;
import java.util.Set;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;

/**
 * This represents a collection of library resources.
 * 
 * One of the three public Libary* files, with LibraryResource and LibraryBrowser
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class LibraryCollection extends LibraryResource {

	private ArrayList<LibraryResource> resources = new ArrayList<LibraryResource>();

	/**
	 * Constructor.
	 *
	 * @param name the name of the collection
	 */
	public LibraryCollection(String name) {
		super(name);
	}

	/**
	 * Gets the type of resource.
	 *
	 * @return collection type
	 */
	@Override
	public String getType() {
		return COLLECTION_TYPE;
	}

	/**
	 * Overrides LibraryResource method.
	 * 
	 * @param type ignored
	 * @return false, since never changes
	 */
	@Override
	public boolean setType(String type) {
		return false;
	}

	/**
	 * Sets the target of this collection.
	 * 
	 * @param path the target path
	 * @return true if changed
	 */
	@Override
	public boolean setTarget(String path) {
		path = (path == null ? "" : path.trim()); //$NON-NLS-1$
		if (path.equals(target))
			return false;
		target = path;
		return true;
	}

	/**
	 * Adds a resource to the end of this collection.
	 *
	 * @param resource the resource
	 */
	public void addResource(LibraryResource resource) {
		if (resource == null)
			return;
		resources.add(resource);
		resource.parent = this;
	}

	/**
	 * Inserts a resource into this collection at a specified index.
	 *
	 * @param resource the resource
	 * @param index    the index
	 * @return true if inserted
	 */
	public boolean insertResource(LibraryResource resource, int index) {
		if (!resources.contains(resource)) {
			resources.add(index, resource);
  		resource.parent = this;
  		return true;
  	}
  	return false;
	}

	/**
	 * Removes a resource from this collection.
	 *
	 * @param resource the resource to remove
	 */
	public void removeResource(LibraryResource resource) {
		resources.remove(resource);
	}

	public void removeResourceAt(int i) {
		resources.remove(i);
	}
	
	/**
	 * Gets the array of resources in this collection.
	 *
	 * @return an array of resources
	 */
	@Override
	public ArrayList<LibraryResource> getResources() {
		return resources;
	}

	/**
	 * Gets the array of resources in this collection.
	 *
	 * @return an array of resources
	 */
	public LibraryResource[] getResourceArray() {
		return resources.toArray(new LibraryResource[resources.size()]);
	}

	
	//.toArray(new LibraryResource[resources.size()]);
	
	/**
	 * Clears resources.
	 */
	public void clearResources() {
		resources.clear();
	}

	/**
	 * From a search, we want only the selected records.
	 */
	@Override
	public LibraryCollection getClone(Set<LibraryResource> selected) {
		LibraryCollection collection = new LibraryCollection(getName());
		copyFieldsTo(collection);
		for (LibraryResource next : getResources()) {
			if (selected == null || selected.contains(next))
				collection.addResource(next.getClone(selected));
		}
		return collection;
	}

//_____________________________  static methods  ____________________________

	/**
	 * Returns an ObjectLoader to save and load data for this class.
	 *
	 * @return the object loader
	 */
	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

	/**
	 * The ObjectLoader class to save and load LibraryCollection data.
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
			XML.getLoader(LibraryResource.class).saveObject(control, obj);
			LibraryCollection collection = (LibraryCollection) obj;
			if (!collection.resources.isEmpty()) {
				control.setValue("resources", 
						collection.getResources().toArray(new LibraryResource[0])); //$NON-NLS-1$
			}
		}

		/**
		 * Creates a new object.
		 *
		 * @param control the XMLControl with the object data
		 * @return the newly created object
		 */
		@Override
		public Object createObject(XMLControl control) {
			String name = control.getString("name"); //$NON-NLS-1$
			return new LibraryCollection(name);
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
			XML.getLoader(LibraryResource.class).loadObject(control, obj);
			LibraryCollection collection = (LibraryCollection) obj;
			collection.resources.clear();
			Object o = control.getObject("resources"); //$NON-NLS-1$
			LibraryResource[] resources = (o == null ? null : o instanceof LibraryResource[] ? (LibraryResource[]) o : (LibraryResource[])((ArrayList<?>)o).toArray(new LibraryResource[0]));
			if (resources != null) {
				for (LibraryResource next : resources) {
					collection.addResource(next);
				}
			} else {
				String target = control.getString("target");
				collection.setTarget(target);
			}
			return collection;
		}
	}

	@Override
	public int compareTo(LibraryResource resource) {
		int c = super.compareTo(resource);
		if (c != EQUAL || !(resource instanceof LibraryCollection))
			return c;
		ArrayList<LibraryResource> r2 = resource.getResources();
		switch (Integer.compare(resources.size(), r2.size())) {
			case 1:
				return BEFORE;
			case -1:
				return AFTER;
		}
		for (int i = resources.size(); --i >= 0;) {
			int result = resources.get(i).compareTo(r2.get(i));
			if (result != EQUAL)
				return result;
		}
		return EQUAL;
	}

	@Override
	public int hashCode() {
		int h = super.hashCode();
		ArrayList<LibraryResource> resources = getResources();
		for (int i = resources.size(); --i >= 0;) {
			h |= resources.get(i).hashCode();
		}
		return h;
	}
	
	@Override
	public int getResourceCount() {
		return (resources == null ? 0 : resources.size());
	}

	/**
	 * Determine if any child on this path has been selected. 
	 * 
	 * (If not, we will remove it.)
	 */
	@Override
	public boolean hasSelectedResources(Set<LibraryResource> selected) {
		for (int i = getResourceCount(); --i >= 0;) {
			LibraryResource resource = resources.get(i);
			if (resource.getResourceCount() > 0) {
				if (resource.hasSelectedResources(selected))
					return true;
			} else if(selected.contains(resource)) {
				return true;
			}
		}
		return false;
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
 * Copyright (c) 2026 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
