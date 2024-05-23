/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

/*
 * The org.opensourcephysics.media.core package defines the Open Source Physics
 * media framework for working with video and other media.
 *
 * Copyright (c) 2024  Douglas Brown and Wolfgang Christian.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * For additional information and documentation on Open Source Physics,
 * please see <http://www.opensourcephysics.org/>.
 */
package org.opensourcephysics.media.core;
import java.io.File;
import java.util.Arrays;
import java.util.TreeSet;

/**
 * This is a FileFilter that accepts video files. Filters for single
 * container types (eg, gif, mov, avi, mp4, etc) are created by specifying
 * the container type and one or more extensions in the constructor. 
 * The no-arg constructor creates a filter that accepts all container types
 * in the collection VideoIO.singleVideoTypeFilters.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class VideoFileFilter extends VideoIO.SingleExtFileFilter implements Comparable<VideoFileFilter> {
	
	String[] extensions; // acceptable extensions
	String type = "Video"; // default type is general //$NON-NLS-1$

	/**
	 * No-arg constructor. Will accept all types in VideoIO.singleVideoTypeFilters
	 */
	public VideoFileFilter() {
		super(null, null);
	}
		
  /**
   * Constructor with container type and accepted file extensions.
   * 
   * @param containerType the container type (eg "mov" or "jpg")
   * @param extensions array of accepted extensions
   */
	public VideoFileFilter(String containerType, String[] extensions) {
		this();
		if (containerType!=null && extensions!=null && extensions.length>0) {
			type = containerType;
			this.extensions = extensions;
		}
	}
		
	/**
	 * BH fix for no need to check directories if a remote path or SwingJS temp file
	 * 
	 * @param f
	 * @param checkIfDir
	 * @return
	 */
	@Override
	public boolean accept(File f, boolean checkIfDir) {
		if (checkIfDir && f.isDirectory()) {
			return true;
		}
		if (extensions != null) {
			String extension = VideoIO.getExtension(f);
			if (extension != null) {
				for (int i = 0, n = extensions.length; i < n; i++) {
					String next = extensions[i];
					if (extension.equalsIgnoreCase(next))
						return true;
				}
			}
		} else {
			for (VideoFileFilter next : VideoIO.singleVideoTypeFilters) {
				if (next.accept(f, false))
					return true;
			}
		}
		return false;
	}
	  
	  /**
	   * Gets a description of the file types accepted by this filter.  
	   * 
	   * @return the description
	   */
	  @Override
	public String getDescription() {
	    // BH 2020.02.09 Java inefficiency during compare operation
		if (desc != null)
			return desc;
		String desc = MediaRes.getString(type.toUpperCase()+"FileFilter.Description"); //$NON-NLS-1$
  	String[] exts = getExtensions();
  	if (exts!=null) {
  		desc += " ("; //$NON-NLS-1$
  		for (int i = 0; i < exts.length; i++) {
  			if (i>0) desc += ", "; //$NON-NLS-1$
  			desc += "."+exts[i]; //$NON-NLS-1$
  		}
  		desc += ")"; //$NON-NLS-1$
  	}
  	return this.desc = desc;
  }
  
  /**
   * Gets the default extension to suggest when saving.  
   * 
   * @return the default extension
   */
  public String getDefaultExtension() {
  	String[] exts = getExtensions();
  	if (exts!=null)
  		return exts[0];
  	for (VideoFileFilter next: VideoIO.singleVideoTypeFilters) {
  		String ext = next.getDefaultExtension();
  		if (ext!=null) return ext;
  	}
		return null;
  }

  /**
   * Gets all extensions accepted by this filter.  
   * 
   * @return array of extensions
   */
  public String[] getExtensions() {
  	if (extensions!=null)
  		return extensions;
  	TreeSet<String> set = new TreeSet<String>();
  	for (VideoFileFilter next: VideoIO.singleVideoTypeFilters) {
  		String[] exts = next.getExtensions();
  		for (String ext: exts)
  			set.add(ext);
  	}
		return set.toArray(new String[set.size()]);
  }

  /**
   * Gets the container type.
   * 
   * @return the container type
   */
  public String getContainerType() {
  	return type;
  }
  
  /**
   * Compares this filter to another. Implements Comparable.
   * This compares them alphabetically by description.
   * 
   * @param filter the filter to compare
   * @return the comparison of their descriptions
   */
  @Override
	public int compareTo(VideoFileFilter filter) {  	
	  	return getDescription().compareTo(filter.getDescription());
  }
  
  @Override
  public String toString() {
	  String s = Arrays.toString(extensions);
	  return s.substring(1, s.length() - 1);
  }

}

/*
 * Open Source Physics software is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.

 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be released
 * under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2024  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
