/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import java.awt.Font;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.Icon;
import javax.swing.JButton;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.display.ResizableIcon;
import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.media.core.VideoType;
import org.opensourcephysics.media.mov.MovieFactory;
import org.w3c.dom.Node;

/**
 * This represents a library resource.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class LibraryResource implements Comparable<LibraryResource> {
	
	static class Attachment {
		Node node;
		String type;
		String url;
		String filename;
		int size;
		
		Attachment(Node node, String type, String url, String filename, int size) {
			this.node = node;
			this.type = type;
			this.url = url;
			this.filename = filename;
			this.size = size;
		}
		
		@Override
		public String toString() {
			return "[Attachment " + node 
					+ " " + type + " " 
					+ url + " " + 
					filename + " " + 
					size + "]";
		}
	}



	// static constants
	public static final String META_AUTHOR = "Author"; //$NON-NLS-1$
	public static final String META_CONTACT = "Contact"; //$NON-NLS-1$
	public static final String META_KEYWORDS = "Keywords"; //$NON-NLS-1$
	public static final String[] META_TYPES = { META_AUTHOR, META_CONTACT, META_KEYWORDS };
	public static final String UNKNOWN_TYPE = "Unknown"; //$NON-NLS-1$
	public static final String COLLECTION_TYPE = "Collection"; //$NON-NLS-1$
	public static final String TRACKER_TYPE = "Tracker"; //$NON-NLS-1$
	public static final String EJS_TYPE = "EJS"; //$NON-NLS-1$
	public static final String VIDEO_TYPE = "Video"; //$NON-NLS-1$
	public static final String IMAGE_TYPE = "Image"; //$NON-NLS-1$
	public static final String HTML_TYPE = "HTML"; //$NON-NLS-1$
	public static final String PDF_TYPE = "PDF"; //$NON-NLS-1$
	protected static final String[] RESOURCE_TYPES = { TRACKER_TYPE, EJS_TYPE, VIDEO_TYPE, IMAGE_TYPE, HTML_TYPE,
			PDF_TYPE, UNKNOWN_TYPE };

	// static fields
	protected static List<String> allResourceTypes = new ArrayList<String>();
	protected static ResizableIcon htmlIcon, videoIcon, trackerIcon, ejsIcon, pdfIcon, unknownIcon, imageIcon;
	protected static DecimalFormat megabyteFormat;
	protected static Font bodyFont = new JButton().getFont().deriveFont(12f);
	protected static Font h1Font = bodyFont.deriveFont(24f);
	protected static Font h2Font = bodyFont.deriveFont(16f);
	protected static Font h3Font = bodyFont.deriveFont(14f);

	static {
		allResourceTypes.add(COLLECTION_TYPE);
		for (String next : RESOURCE_TYPES)
			allResourceTypes.add(next);
		String imageFile = "/org/opensourcephysics/resources/tools/images/html.gif"; //$NON-NLS-1$
		htmlIcon = ResourceLoader.getResizableIcon(imageFile);
		imageFile = "/org/opensourcephysics/resources/tools/images/pdf.gif"; //$NON-NLS-1$
		pdfIcon = ResourceLoader.getResizableIcon(imageFile);
		imageFile = "/org/opensourcephysics/resources/tools/images/video.gif"; //$NON-NLS-1$
		videoIcon = ResourceLoader.getResizableIcon(imageFile);
		imageFile = "/org/opensourcephysics/resources/tools/images/portrait.gif"; //$NON-NLS-1$
		imageIcon = ResourceLoader.getResizableIcon(imageFile);
		imageFile = "/org/opensourcephysics/resources/tools/images/tracker_icon_16.png"; //$NON-NLS-1$
		trackerIcon = ResourceLoader.getResizableIcon(imageFile);
		imageFile = "/org/opensourcephysics/resources/tools/images/ejsicon.gif"; //$NON-NLS-1$
		ejsIcon = ResourceLoader.getResizableIcon(imageFile);
		imageFile = "/org/opensourcephysics/resources/tools/images/question_mark.gif"; //$NON-NLS-1$
		unknownIcon = ResourceLoader.getResizableIcon(imageFile);
		try {
			megabyteFormat = (DecimalFormat) NumberFormat.getInstance();
			megabyteFormat.applyPattern("0.0"); //$NON-NLS-1$
		} catch (Exception e) {
		}

	}

	// instance fields
	private String name = ""; //$NON-NLS-1$
	private String description = ""; //$NON-NLS-1$
	private String basePath = ""; // base path for target and/or HTML //$NON-NLS-1$
	private String htmlPath = ""; // rel or abs path to HTML page that describes this resource //$NON-NLS-1$
	protected String target = ""; // rel or abs path to target //$NON-NLS-1$
	private String type = UNKNOWN_TYPE;
	protected String displayName;
	private String thumbnail;
	private Map<String, String> properties = new TreeMap<String, String>();
	private TreeSet<Metadata> metadata;
	protected String[][] loaderMetadata;
	protected LibraryCollection parent;
	protected String collectionPath; // used to open source collection of resources found in searches
	protected List<String> treePath; // used to define tree paths of resources found in searches

	/**
	 * Constructor.
	 *
	 * @param name the name of the resource
	 */
	public LibraryResource(String name) {
		setName(name);
	}

	/**
	 * Gets the name of this resource (never null).
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of this resource.
	 * 
	 * @param aName the name
	 * @return true if changed
	 */
	public boolean setName(String aName) {
		aName = aName == null ? "" : aName.trim(); //$NON-NLS-1$
		if (!aName.equals(name)) {
			name = aName;
			return true;
		}
		return false;
	}

	/**
	 * Gets the base path.
	 *
	 * @return the base path
	 */
	public String getBasePath() {
		return basePath;
	}

	/**
	 * Sets the base path of this resource.
	 * 
	 * @param path the base path
	 * @return true if changed
	 */
	public boolean setBasePath(String path) {
		path = (path == null ? "" : path.trim()); //$NON-NLS-1$
		if (!path.equals(basePath)) {
			basePath = path;
			return true;
		}
		return false;
	}

	/**
	 * Returns the first base path found in this or its ancestors.
	 *
	 * @return the base path
	 */
	protected String getInheritedBasePath() {
		return (!"".equals(basePath) ? basePath : parent != null ? parent.getInheritedBasePath() : ""); //$NON-NLS-1$
	}

	/**
	 * Gets the target of this resource (file name or comPADRE command).
	 *
	 * @return the target
	 */
	public String getTarget() {
//		OSPLog.debug("LibraryResource.getTarget " + name + " target=" + target);
		return ("".equals(target) ? null : target); //$NON-NLS-1$
	}

	/**
	 * Gets the absolute path to the target. Note: this is needed for the
	 *
	 * @return the absolute target, or empty String if none
	 */
	public String getAbsoluteTarget() {
		return ("".equals(target) ? "" : XML.getResolvedPath(target, getInheritedBasePath())); //$NON-NLS-1$
	}

	/**
	 * Sets the target of this resource.
	 * 
	 * @param path the target path
	 * @return true if changed
	 */
	public boolean setTarget(String path) {
		path = path == null ? "" : path.trim(); //$NON-NLS-1$
		if (path.equals(target)) {
			return false;
		}
		thumbnail = null;
		target = path;
		setType(getTypeFromPath(path, getHTMLPath()));
		return true;
	}

	static String getTypeFromPath(String path, String htmlPath) {
		if (path == null)
			path = "";
		String ext = (path.length() >= 4 ? XML.getExtension(path).toLowerCase() : htmlPath == null ? "" : "html");
		if (ext.contains("&trackerset"))
			ext = "trk";
		switch (ext) {
		case "html":
			return LibraryResource.HTML_TYPE;
		case "trk":
		case "trz":
		case "zip":
			return LibraryResource.TRACKER_TYPE;
		case "pdf":
			return LibraryResource.PDF_TYPE;
		case "ejs":
			return LibraryResource.EJS_TYPE;
		default:
			ArrayList<VideoType> types = VideoIO.getVideoTypesForPath(ext);
			switch (types.size() == 0 ? "" : types.get(0).getTypeName()) {
			default:
				break;
			case VideoType.TYPE_IMAGE:
				return LibraryResource.IMAGE_TYPE;
			case MovieFactory.ENGINE_XUGGLE:
			case MovieFactory.ENGINE_JS:
				return LibraryResource.VIDEO_TYPE;
			}
			//$FALL-THROUGH$
		case "":
			return LibraryResource.UNKNOWN_TYPE;
		}
	}

	/**
	 * Gets the path to the html page displayed in the browser.
	 *
	 * @return the html path
	 */
	public String getHTMLPath() {
		return htmlPath;
	}

	/**
	 * Sets the html path of this resource.
	 * 
	 * @param path the html path
	 * @return true if changed
	 */
	public boolean setHTMLPath(String path) {
		path = (path == null ? "" : path.trim()); //$NON-NLS-1$
		if (!path.equals(htmlPath)) {
			htmlPath = path;
			if (!(this instanceof LibraryCollection) && getTarget() == null) {
				if (path.equals("")) {//$NON-NLS-1$
					setType(LibraryResource.UNKNOWN_TYPE);
				} else {
					setType(LibraryResource.HTML_TYPE);
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Gets the absolute path to the html page displayed in the browser.
	 *
	 * @return the absolute html path, or empty String if none
	 */
	private String getAbsoluteHTMLPath() {
		return ("".equals(htmlPath) ? "" : XML.getResolvedPath(htmlPath, getInheritedBasePath())); //$NON-NLS-1$
	}

	/**
	 * Gets the description, which must be in html code.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description of this resource. Note: the description must be in html
	 * code, since it is displayed in the html pane of the LibraryTreePanel if the
	 * html path is empty.
	 * 
	 * @param desc the description in HTML code
	 * @return true if changed
	 */
	public boolean setDescription(String desc) {
		desc = desc == null ? "" : desc.trim(); //$NON-NLS-1$
		if (!desc.equals(description)) {
			description = desc;
			return true;
		}
		return false;
	}

	/**
	 * Gets the type of resource.
	 *
	 * @return the one of the static constant types defined in this class
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the type of this resource. The types are static constants defined in
	 * this class.
	 * 
	 * @param type the type
	 * @return true if changed
	 */
	public boolean setType(String type) {
		if (this.type.equals(type))
			return false;
		for (String next : allResourceTypes) {
			if (next.equals(type)) {
				this.type = next;
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets the metadata.
	 * 
	 * @return the Set of Metadata (may be null)
	 */
	public TreeSet<Metadata> getMetadata() {
		return metadata;
	}

	/**
	 * Gets the first metadata of a specified type.
	 * 
	 * @param key the type
	 * @return Metadata, or null if none
	 */
	public Metadata getMetadata(String key) {
		if (metadata == null)
			return null;
		for (Metadata next : metadata) {
			if (next.data[0].equals(key))
				return next;
		}
		return null;
	}

	/**
	 * Sets the metadata. This replaces all previously added metadata.
	 * 
	 * @param data a Set of Metadata (may be null)
	 */
	public void setMetadata(TreeSet<Metadata> data) {
		metadata = data;
	}

	/**
	 * Adds a Metadata object to the metadata.
	 * 
	 * @param data the Metadata
	 */
	public void addMetadata(Metadata data) {
		// don't add meta tags commonly included by web page builders
		String key = data.data[0].toLowerCase();
		String[] exclude = new String[] {"generator", "progid", "originator",
				"referrer", "theme-color", "color-scheme", "viewport", "googlebot",
				"robots"};
		for (int i = 0; i < exclude.length; i++) {
			if (key.equals(exclude[i]))
				return;
		}
		if (metadata == null)
			metadata = new TreeSet<Metadata>();
		// standardize display of predefined metadata types
		for (String type : META_TYPES) {
			if (type.toLowerCase().equals(data.getData()[0])) {
				data.getData()[0] = type;
			}
		}
		metadata.add(data);
	}

	/**
	 * Removes a Metadata object from the metadata.
	 * 
	 * @param data the Metadata
	 * @return true if removed
	 */
	public boolean removeMetadata(Metadata data) {
		if (metadata == null)
			metadata = new TreeSet<Metadata>();
		for (Iterator<Metadata> it = metadata.iterator(); it.hasNext();) {
			if (it.next().equals(data)) {
				it.remove();
				return true;
			}
		}
		return false;
	}

	/**
	 * Sets an arbitrary String property.
	 * 
	 * @param name  the name of the property
	 * @param value the value of the property
	 */
	public void setProperty(String name, String value) {
		properties.put(name, value);
	}

	/**
	 * Gets a property value. May return null.
	 * 
	 * @param name the name of the property
	 * @return the value of the property
	 */
	public String getProperty(String name) {
		return properties.get(name);
	}

	/**
	 * Returns the names of all defined properties.
	 * 
	 * @return a set of names
	 */
	public Set<String> getPropertyNames() {
		return properties.keySet();
	}

	/**
	 * Gets the icon for the tree node associated with this resource.
	 *
	 * @return the icon
	 */
	public Icon getIcon() {
		ResizableIcon icon = null;
		if (type == TRACKER_TYPE)
			icon = trackerIcon;
		if (type == EJS_TYPE)
			icon = ejsIcon;
		if (type == IMAGE_TYPE)
			icon = imageIcon;
		if (type == VIDEO_TYPE)
			icon = videoIcon;
		if (type == HTML_TYPE)
			icon = htmlIcon;
		if (type == PDF_TYPE)
			icon = pdfIcon;
		return icon;
	}

	/**
	 * Gets the thumbnail of this resource, if any.
	 *
	 * @return the thumbnail
	 */
	public String getThumbnail() {
		return thumbnail;
	}

	/**
	 * Sets the thumbnail for this resource.
	 *
	 * @param imagePath the path to a thumbnail image
	 */
	public void setThumbnail(String imagePath) {
		thumbnail = imagePath;
	}

	/**
	 * Gets the collection path for this resource. May return null.
	 *
	 * @return the collection path of this or an ancestor
	 */
	public String getCollectionPath() {
		if (collectionPath != null)
			return collectionPath;
		if (parent != null)
			return parent.getCollectionPath();
		return null;
	}

	/**
	 * Gets a title for tabs. May return null.
	 * 
	 * @param path the path to the xml file associated with this resource (may be
	 *             null)
	 * @return the title
	 */
	public String getTitle(String path) {
		// title is name of this resource or, if unnamed, file name or server and file
		// name
		String title = getName();
		if (title.equals("") && path != null) { //$NON-NLS-1$
			String fileName = XML.getName(path);
			String basePath = XML.getDirectoryPath(path);
			if (ResourceLoader.isHTTP(basePath)) {
				basePath = basePath.substring(5);
				while (basePath.startsWith("/")) { //$NON-NLS-1$
					basePath = basePath.substring(1);
				}

				int i = basePath.indexOf("/"); //$NON-NLS-1$
				if (i > -1)
					basePath = basePath.substring(0, i);
				title = basePath + ": " + fileName; //$NON-NLS-1$
			} else {
				title = XML.getName(path);
			}
		}
		displayName = title;
		return title;
	}

	@Override
	public String toString() {
		return getDisplayString();
	}

	public String getDisplayString() {
		if (!getName().equals("")) //$NON-NLS-1$
			return getName();
		if (displayName != null)
			return displayName;
		if (collectionPath != null && parent == null)
			return getTitle(collectionPath);
		if (this instanceof LibraryCollection)
			return ToolsRes.getString("LibraryCollection.Name.Default"); //$NON-NLS-1$
		return ToolsRes.getString("LibraryResource.Name.Default"); //$NON-NLS-1$
	}

	/**
	 * Compares this to the specified resource.
	 *
	 * @param resource the resource to compare
	 * @return 0 if equal, otherwise alphabetical name order
	 */
	@Override
	public int compareTo(LibraryResource resource) {
		int BEFORE = -1;
		int EQUAL = 0;
		int AFTER = 1;

		if (this == resource)
			return EQUAL;

		// compare names
		int result;
		if ((result = getName().compareTo(resource.getName())) != EQUAL)
			return result;

		// compare absolute targets
		String tar1 = getAbsoluteTarget();
		String tar2 = resource.getAbsoluteTarget();
		if ((result = (tar1 == null && tar2 == null ? EQUAL
				: tar1 == null ? AFTER : tar2 == null ? BEFORE : tar1.compareTo(tar2))) != EQUAL)
			return result;

		// compare HTML paths
		String html1 = getAbsoluteHTMLPath();
		String html2 = resource.getAbsoluteHTMLPath();
		if ((result = (html1 == null && html2 == null ? EQUAL :
			html1 == null ? AFTER : html2 == null ? BEFORE : html1.compareTo(html2))) != EQUAL)
			return result;

		// compare type
		if ((result = getType().compareTo(resource.getType())) != EQUAL)
			return result;

//  	// compare metadata
//		Set<Metadata> meta1 = this.getMetadata();
//		Set<Metadata> meta2 = resource.getMetadata();
//  	if (meta1!=null || meta2!=null) {
//  		if (meta1==null) return AFTER;
//  		if (meta2==null) return BEFORE;
//  		if (meta1.size()>meta2.size()) return BEFORE;
//  		if (meta1.size()<meta2.size()) return AFTER;
//  		// both have metadata sets of same size
//    	for (Metadata next1: meta1) {
//    		if (meta2.contains(next1)) continue; // same metadata in both
//    		String key = next1.getData()[0];
//    		for (Metadata next2: meta2) {
//    			// same key found
//    			if (next2.getData()[0].equals(key)) {
//    				return next1.getData()[1].compareTo(next2.getData()[1]);
//    			}
//    		}
//    	} 		
//  	}

		// if collection, compare child resources
		if (this instanceof LibraryCollection && resource instanceof LibraryCollection) {
			LibraryResource[] children1 = ((LibraryCollection) this).getResources();
			LibraryResource[] children2 = ((LibraryCollection) resource).getResources();
			if (children1.length > children2.length)
				return BEFORE;
			if (children1.length < children2.length)
				return AFTER;
			for (int i = 0; i < children1.length; i++) {
				result = children1[i].compareTo(children2[i]);
				if (result != EQUAL)
					return result;
			}
		}

		return EQUAL;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof LibraryResource && compareTo((LibraryResource) obj) == 0);
	}

	/**
	 * Gets a clone of this resource.
	 *
	 * @return the clone
	 */
	public LibraryResource getClone() {
		boolean isCollection = this instanceof LibraryCollection;
		LibraryResource resource = isCollection ? new LibraryCollection(getName()) : new LibraryResource(getName());
		resource.setBasePath(getBasePath());
		resource.setTarget(getTarget());
		resource.setHTMLPath(getHTMLPath());
		resource.setDescription(getDescription());
		resource.setType(getType());
		for (String next : getPropertyNames()) {
			resource.setProperty(next, getProperty(next));
		}
		if (getMetadata() != null) {
			for (Metadata next : getMetadata()) {
				resource.addMetadata(new Metadata(next.getData()[0], next.getData()[1]));
			}
		}
		if (isCollection) {
			LibraryCollection thisCollection = (LibraryCollection) this;
			for (LibraryResource next : thisCollection.getResources()) {
				((LibraryCollection) resource).addResource(next.getClone());
			}

		}
		// lines below are for search results
		resource.collectionPath = getCollectionPath();
		resource.treePath = getTreePath(null);
		return resource;
	}

	/**
	 * Gets the tree path for this node.
	 * 
	 * @param pathComponents a List of Strings in root-to-leaf order that represent
	 *                       this path
	 * @return the tree path
	 */
	protected List<String> getTreePath(List<String> pathComponents) {
		if (pathComponents == null)
			pathComponents = new ArrayList<String>();
		if (parent != null)
			parent.getTreePath(pathComponents);
		pathComponents.add(getDisplayString());
		return pathComponents;
	}

	/**
	 * Gets the html code for a resource with specified properties. Note this code
	 * is for display in the LibraryBrowser, and has no stylesheet of its own.
	 * 
	 * @param title         the name of the resource
	 * @param resourceType  one of the LibraryResource defined types
	 * @param thumbnailPath path to the thumbnail image file
	 * @param description   a description of the resource
	 * @param authors       authors
	 * @param contact       author contact information or institution
	 * @param moreInfoURL   link to external HTML with more information about the
	 *                      resource
	 * @param attachment    String[] {downloadURL, filename, sizeInBytes} (used for
	 *                      ComPADRE)
	 * @param data          Map of metadata names to values
	 *
	 * @return the html code
	 */
	public static String getHTMLCode(String title, String resourceType, String thumbnailPath, String description,
			String authors, String contact, String moreInfoURL, Attachment attachment, Map<String, String> data) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">"); //$NON-NLS-1$
		buffer.append("\n  <html>"); //$NON-NLS-1$
		buffer.append("\n    <head>"); //$NON-NLS-1$
		buffer.append("\n" + getStyleSheetCode()); //$NON-NLS-1$
		buffer.append("\n      <meta http-equiv=\"content-type\" content=\"text/html;charset=iso-8859-1\">"); //$NON-NLS-1$
		if (data != null) {
			for (String name : data.keySet()) {
				String value = data.get(name);
				buffer.append("\n      <meta name=\"" + name + "\" content=\"" + value + "\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		if (title != null && !title.equals("")) { //$NON-NLS-1$
			buffer.append("\n      <title>" + title + "</title>"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		buffer.append("\n    </head>\n"); //$NON-NLS-1$
		buffer.append("\n    <body>"); //$NON-NLS-1$
		buffer.append(getHTMLBody(title, resourceType, thumbnailPath, description, authors, contact, moreInfoURL,
				attachment));
		buffer.append("\n    </body>"); //$NON-NLS-1$
		buffer.append("\n  </html>"); //$NON-NLS-1$
		return buffer.toString();
	}

	/**
	 * Gets html <body> code for a resource with specified properties.
	 * 
	 * @param title         the name of the resource
	 * @param resourceType  one of the LibraryResource defined types
	 * @param thumbnailPath path to the thumbnail image file
	 * @param description   a description of the resource
	 * @param authors       authors
	 * @param contact       author contact information or institution
	 * @param moreInfoURL   link to external HTML with more information about the
	 *                      resource
	 * @param attachment    (used for ComPADRE)
	 *
	 * @return the html path
	 */
	protected static String getHTMLBody(String title, String resourceType, String thumbnailPath, String description,
			String authors, String contact, String moreInfoURL, Attachment attachment) {
		StringBuffer buffer = new StringBuffer();

		if (title != null && !title.equals("")) { //$NON-NLS-1$
			buffer.append("\n      <h2>" + title + "</h2>"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		buffer.append("\n      <blockquote>"); //$NON-NLS-1$
		if (resourceType != null && !resourceType.equals("")) { //$NON-NLS-1$
			if (allResourceTypes.contains(resourceType)) {
				resourceType = ToolsRes.getString("LibraryResource.Type." + resourceType); //$NON-NLS-1$
			}
			buffer.append("\n        <b>" + resourceType + "</b>"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (thumbnailPath != null && !thumbnailPath.equals("")) { //$NON-NLS-1$

			thumbnailPath = "<p><img src=\"" + thumbnailPath + "\""; //$NON-NLS-1$ //$NON-NLS-2$
			if (title != null && !title.equals("")) { //$NON-NLS-1$
				thumbnailPath += " alt=\"" + title + "\""; //$NON-NLS-1$ //$NON-NLS-2$
			}
			buffer.append("\n        " + thumbnailPath + "></p>"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (description != null && !description.equals("")) { //$NON-NLS-1$
			if (!description.startsWith("<p>")) { //$NON-NLS-1$
				description = "<p>" + insertLineBreaks(description) + "</p>"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			buffer.append("\n        " + description); //$NON-NLS-1$
		}
		if (authors != null && !authors.equals("")) { //$NON-NLS-1$
			String authorTitle = ToolsRes.getString("LibraryTreePanel.Label.Author"); //$NON-NLS-1$
			buffer.append("\n        <p><b>" + authorTitle + ":  </b>" + authors + "</p>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (contact != null && !contact.equals("")) { //$NON-NLS-1$
			String contactTitle = ToolsRes.getString("LibraryTreePanel.Label.Contact"); //$NON-NLS-1$
			buffer.append("\n        <p><b>" + contactTitle + ":  </b>" + contact + "</p>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (attachment != null && attachment.filename != null) {
			String filename = attachment.filename;
			String resTitle = ToolsRes.getString("LibraryResource.Description.Resource"); //$NON-NLS-1$
			int bytes = attachment.size;
			megabyteFormat.setDecimalFormatSymbols(OSPRuntime.getDecimalFormatSymbols());
			String size = " (" + megabyteFormat.format(bytes / 1048576.0) + "MB)"; //$NON-NLS-1$ //$NON-NLS-2$
			buffer.append("\n        <p><b>" + resTitle + ":  </b>" + filename + size + "</p>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (moreInfoURL != null && !moreInfoURL.equals("")) { //$NON-NLS-1$
			try {
				new URL(moreInfoURL); // throws exception if malformed
				String infoTitle = ToolsRes.getString("LibraryComPADRE.Description.InfoField"); //$NON-NLS-1$
				buffer.append("\n        <p><b>" + infoTitle + "  </b><a href=\"" + moreInfoURL + "\">" + moreInfoURL //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						+ "</a></p>"); //$NON-NLS-1$
			} catch (MalformedURLException e) {
			}
		}
		buffer.append("\n      </blockquote>"); //$NON-NLS-1$
		return buffer.toString();
	}

	/**
	 * Returns the body style for a stylesheet.
	 * 
	 * @return the body style
	 */
	protected static String getBodyStyle() {
		return "body {\n" + //$NON-NLS-1$
				"  font-family: Verdana, Arial, Helvetica, sans-serif;\n" + //$NON-NLS-1$
				"  font-size: " + bodyFont.getSize() + "pt;\n" + //$NON-NLS-1$ //$NON-NLS-2$
				"  color: #405050;\n" + //$NON-NLS-1$
				"  background-color: #FFFFFF;\n" + //$NON-NLS-1$
				"}\n"; //$NON-NLS-1$
	}

	/**
	 * Returns the H1 heading style for a stylesheet.
	 * 
	 * @return the H1 heading style
	 */
	protected static String getH1Style() {
		return "h1 {\n" + //$NON-NLS-1$
				"  font-size: " + h1Font.getSize() + "pt;\n" + //$NON-NLS-1$ //$NON-NLS-2$
				"  text-align: center;\n" + //$NON-NLS-1$
				"}\n"; //$NON-NLS-1$
	}

	/**
	 * Returns the H2 heading style for a stylesheet.
	 * 
	 * @return the H2 heading style
	 */
	protected static String getH2Style() {
		return "h2 {\n" + //$NON-NLS-1$
				"  font-size: " + h2Font.getSize() + "pt;\n" + //$NON-NLS-1$ //$NON-NLS-2$
				"}\n"; //$NON-NLS-1$
	}

	/**
	 * Returns the H3 heading style for a stylesheet.
	 * 
	 * @return the H3 heading style
	 */
	protected static String getH3Style() {
		return "h3 {\n" + //$NON-NLS-1$
				"  font-size: " + h3Font.getSize() + "pt;\n" + //$NON-NLS-1$ //$NON-NLS-2$
				"}\n"; //$NON-NLS-1$
	}

	/**
	 * Returns the P heading style for a stylesheet.
	 * 
	 * @return the P heading style
	 */
	protected static String getPStyle() {
		return "p, li, h4, h5 {\n" + //$NON-NLS-1$
					"  font-family: Verdana, Arial, Helvetica, sans-serif;\n" + //$NON-NLS-1$
					"  font-size: " + bodyFont.getSize() + "pt;\n" + //$NON-NLS-1$ //$NON-NLS-2$
					"  color: #405050;\n" + //$NON-NLS-1$
					"  background-color: #FFFFFF;\n" + //$NON-NLS-1$
					"}\n"; //$NON-NLS-1$
	}
	
	/**
	 * Returns all HTML styles for a stylesheet.
	 * 
	 * @return the styles
	 */
	protected static String getHTMLStyles() {
		return getBodyStyle() + getH1Style() + getH2Style() + getH3Style() + getPStyle();
	}

	/**
	 * Returns the H2 heading style for a stylesheet.
	 * 
	 * @return the H2 heading style
	 */
	protected static String getStyleSheetCode() {
		return "<style TYPE=\"text/css\">\n" + //$NON-NLS-1$
				"<!--\n" + //$NON-NLS-1$
				getHTMLStyles() + "-->\n" + //$NON-NLS-1$
				"</style>\n"; //$NON-NLS-1$
	}

	/**
	 * Inserts HTML line breaks where new lines occur in text.
	 * 
	 * @param text the text
	 * @return the text with HTML line breaks
	 */
	protected static String insertLineBreaks(String text) {
		String[] parts = text.split("\n"); //$NON-NLS-1$
		StringBuffer buf = new StringBuffer();
		int last = parts.length - 1;
		for (int i = 0; i <= last; i++) {
			String next = parts[i];
			buf.append(next);
			if (i < last)
				buf.append("<br>"); //$NON-NLS-1$
		}
		return buf.toString();
	}

	/**
	 * A Comparable class for metadata key-value pairs.
	 */
	protected static class Metadata implements Comparable<Metadata> {

		private String[] data;

		public Metadata() {
			data = new String[] { "", "" }; //$NON-NLS-1$ //$NON-NLS-2$
		}

		public Metadata(String key, String value) {
			key = key == null ? "" : key; //$NON-NLS-1$
			value = value == null ? "" : value; //$NON-NLS-1$
			data = new String[] { key, value };
		}

		public String[] getData() {
			return data;
		}

		public void clearData() {
			data[0] = ""; //$NON-NLS-1$
			data[1] = ""; //$NON-NLS-1$
		}

		@Override
		public int compareTo(Metadata meta) {
			int result = data[0].compareTo(meta.data[0]);
//			return result; // can't have multiple metadata with the same key
			return result == 0 ? data[1].compareTo(meta.data[1]) : result;
		}

		@Override
		public String toString() {
			return data[0] + ": " + data[1]; //$NON-NLS-1$
		}
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
	 * The ObjectLoader class to save and load LibraryResource data.
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
			LibraryResource res = (LibraryResource) obj;
			control.setValue("name", res.name); //$NON-NLS-1$
			if (!"".equals(res.description)) //$NON-NLS-1$
				control.setValue("description", res.description); //$NON-NLS-1$
			if (!"".equals(res.htmlPath)) //$NON-NLS-1$
				control.setValue("html_path", res.htmlPath); //$NON-NLS-1$
			if (!"".equals(res.basePath)) //$NON-NLS-1$
				control.setValue("base_path", res.basePath); //$NON-NLS-1$
			if (!"".equals(res.target)) //$NON-NLS-1$
				control.setValue("target", res.getTarget()); //$NON-NLS-1$
//			if (res.thumbnail != null)
//				control.setValue("thumbnail", res.getThumbnail()); //$NON-NLS-1$
			if (!UNKNOWN_TYPE.equals(res.type))
				control.setValue("type", res.type); //$NON-NLS-1$
			if (res.metadata != null && res.metadata.size() > 0) {
				int len = res.metadata.size();
				String[][] data = new String[len][];
				int i = 0;
				for (Metadata next : res.metadata) {
					data[i] = next.data;
					i++;
				}
				control.setValue("metadata", data); //$NON-NLS-1$
			}
			if (!res.getPropertyNames().isEmpty()) {
				ArrayList<String[]> props = new ArrayList<String[]>();
				for (String name : res.getPropertyNames()) {
					props.add(new String[] { name, res.getProperty(name) });
				}
				control.setValue("properties", props.toArray(new String[props.size()][2])); //$NON-NLS-1$
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
			return new LibraryResource(name);
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
			LibraryResource res = (LibraryResource) obj;
			// name is loaded in createObject() method
			res.setDescription(control.getString("description")); //$NON-NLS-1$
			res.setBasePath(control.getString("base_path")); //$NON-NLS-1$
			String target = control.getString("target"); //$NON-NLS-1$
			if (target != null) {
				//OSPLog.debug("LibraryResource.loadObject " + res.name + " target=" + target);
				res.target = target;
			}
			res.setHTMLPath(control.getString("html_path")); //$NON-NLS-1$
			res.setType(control.getString("type")); //$NON-NLS-1$
//			res.setThumbnail(control.getString("thumbnail")); //$NON-NLS-1$
			String[][] data = (String[][]) control.getObject("metadata"); //$NON-NLS-1$
			if (data != null) {
				res.loaderMetadata = data;
				if (res.getMetadata() != null)
					res.getMetadata().clear();
				for (int i = 0; i < data.length; i++) {
					String[] next = data[i];
					res.addMetadata(new Metadata(next[0], next[1]));
				}
			}
			String[][] props = (String[][]) control.getObject("properties"); //$NON-NLS-1$
			if (props != null) {
				for (String[] next : props) {
					res.setProperty(next[0], next[1]);
				}
			}
//			LibraryBrowser.addMapResource(res);
			return res;
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
 * Copyright (c) 2024 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
