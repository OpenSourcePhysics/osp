/*
load * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <https://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.LibraryResource.Attachment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This provides static methods for getting a LibraryCollection from ComPADRE.
 * Adapted from code written for EJS by Francisco Esquembre.
 * 
 * @author Francisco Esquembre, Douglas Brown
 * @version 1.0
 */
@SuppressWarnings("javadoc")
public class LibraryComPADRE {

	public static final String OSP_INFO_URL = "https://www.compadre.org/osp/online_help/EjsDL/OSPCollection.html"; //$NON-NLS-1$
	public static final String EJS_SERVER_TREE = "https://www.compadre.org/osp/services/REST/osp_jars.cfm?verb=Identify&OSPType=EJS%20Model&AttachedDocument=Source%20Code"; //$NON-NLS-1$
	public static final String EJS_SERVER_RECORDS = "https://www.compadre.org/osp/services/REST/osp_jars.cfm?OSPType=EJS%20Model&AttachedDocument=Source%20Code"; //$NON-NLS-1$
	public static final String EJS_COLLECTION_NAME = "OSP EJS Collection"; //$NON-NLS-1$
	public static final String EJS_INFO_URL = "https://www.compadre.org/osp/online_help/EjsDL/DLModels.html"; //$NON-NLS-1$
	public static final String TRACKER_SERVER_TREE = "https://www.compadre.org/osp/services/REST/osp_tracker.cfm?verb=Identify&OSPType=Tracker"; //$NON-NLS-1$
	public static final String TRACKER_SERVER_RECORDS = "https://www.compadre.org/osp/services/REST/osp_tracker.cfm?OSPType=Tracker"; //$NON-NLS-1$
	public static final String TRACKER_COLLECTION_NAME = "OSP Tracker Collection"; //$NON-NLS-1$
	public static final String TRACKER_INFO_URL = "https://physlets.org/tracker/library/comPADRE_collection.html"; //$NON-NLS-1$
	public static final String PRIMARY_ONLY = "&OSPPrimary=Subject"; //$NON-NLS-1$
	public static final String GENERIC_COLLECTION_NAME = "ComPADRE OSP Collection"; //$NON-NLS-1$
	public static final String ABOUT_OSP = "About OSP and ComPADRE"; //$NON-NLS-1$
	public static final String HOST = "www.compadre.org"; //$NON-NLS-1$
	public static final String COMPADRE_QUERY = "https://www.compadre.org/osp/services/REST/osp"; //$NON-NLS-1$
	public static String desiredOSPType; // if non-null, <osp-type> must contain this string

	/**
	 * Loads a collection using a specified comPADRE search query.
	 * 
	 * @param collection the LibraryCollection to load
	 * @param query      the search query
	 * @return true if successfully loaded
	 */
	protected static boolean load(LibraryCollection collection, String query) {
		try {
			URL url = new URL(query);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document doc = factory.newDocumentBuilder().parse(url.openStream());
//      writeXmlFile(doc, "compadre_catalog.txt"); // for testing
			NodeList nodeList = doc.getElementsByTagName("Identify"); //$NON-NLS-1$
			boolean success = false;
			for (int i = 0; i < nodeList.getLength(); i++) {
				success = loadSubtrees(collection, nodeList.item(i).getChildNodes(), "osp-subject", "") || success; //$NON-NLS-1$ //$NON-NLS-2$
			}
			return success;
		} catch (Exception e) {
			OSPLog.warning("failed to load ComPADRE collection " + query + " " + e); //$NON-NLS-1$
		}
		return false;
	}

	/**
	 * Loads a collection with subtree collections that meet the specified
	 * requirements.
	 * 
	 * @param collection       the LibraryCollection to load
	 * @param nodeList         a list of Nodes
	 * @param attributeType    the desired attribute
	 * @param serviceParameter the desired service parameter
	 * @return true if at least one subtree collection was loaded
	 */
	protected static boolean loadSubtrees(LibraryCollection collection, NodeList nodeList, String attributeType,
			String serviceParameter) {
		boolean success = false;
		String dblClick = "...";//ToolsRes.getString("LibraryComPADRE.Description.DoubleClick"); //$NON-NLS-1$
		for (int i = 0; i < nodeList.getLength(); i++) {
			if (!(nodeList.item(i) instanceof Element))
				continue;
			Element node = (Element) nodeList.item(i);
			if (node.getNodeName().equals("sub-tree-set") && attributeType.equals(node.getAttribute("type"))) { //$NON-NLS-1$ //$NON-NLS-2$
				List<Node> subTrees = getAllChildren(node, "sub-tree"); //$NON-NLS-1$
				if (subTrees.size() > 0) { // node has subcategories
					String unclassifiedURL = null;
					for (int j = 0; j < subTrees.size(); j++) {
						if (!(subTrees.get(j) instanceof Element))
							continue;
						Element subtree = (Element) subTrees.get(j);
						String name = subtree.getAttribute("name"); //$NON-NLS-1$
						String serviceParam = subtree.getAttribute("service-parameter"); //$NON-NLS-1$

						// BH 2020.11.12 presumes file not https here

						serviceParam = serviceParameter + "&" + ResourceLoader.getNonURIPath(serviceParam); //$NON-NLS-1$
						if (name.equals("Unclassified")) { // unclassified node is processed last and adds //$NON-NLS-1$
															// its records to the parent
							unclassifiedURL = serviceParam;
							continue;
						}
						LibraryCollection subCollection = new LibraryCollection(name);
						collection.addResource(subCollection);
						success = true;
						if (getAllChildren(subtree, "sub-tree-set").isEmpty()) { // has no subcategories //$NON-NLS-1$
							String nodeName = "<h2>" + name + "</h2><blockquote>"; //$NON-NLS-1$ //$NON-NLS-2$
							subCollection.setDescription(nodeName + dblClick + "</blockquote>"); //$NON-NLS-1$
							subCollection.setTarget(serviceParam);
						} else
							loadSubtrees(subCollection, subtree.getChildNodes(), attributeType + "-detail", //$NON-NLS-1$
									serviceParam);
					}
					if (unclassifiedURL != null) {
						collection.setTarget(unclassifiedURL);
					}
				}
			}
		}
		return success;
	}

	/**
	 * Loads ComPADRE records into a LibraryTreeNode collection.
	 * 
	 * @param treeNode the LibraryTreeNode to load--note record MUST be a collection
	 * @return true if one or more ComPADRE records were successfully loaded
	 */
	protected static void loadResources(LibraryTreeNode treeNode, Runnable onSuccess, Runnable onFailure) {
		if (!(treeNode.record instanceof LibraryCollection))
			return;
		//OSPLog.debug("LibraryComPADRE loading resources for "+treeNode.record.getName());
		LibraryCollection collection = (LibraryCollection) treeNode.record;
		boolean[] success = new boolean[1];
		int[] index = new int[1];

		Runnable whenDone = new Runnable() {

			@Override
			public void run() {
				// clear the collection target and description
				collection.setDescription(null);
				collection.setTarget(null);
				if (success[0]) {
					onSuccess.run();
				} else {
					onFailure.run();
				}
			}

		};

		try {
			String urlPath = treeNode.getAbsoluteTarget();
			URL url = new URL(urlPath);

			ResourceLoader.getURLContentsAsync(url, (bytes) -> {
				
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					int n = 0;
					Document doc;
					NodeList list = null;
					try {
						doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
						list = doc.getElementsByTagName("record"); //$NON-NLS-1$
						n = list.getLength();
					} catch (SAXException | IOException | ParserConfigurationException e) {
						e.printStackTrace();
					}
					if (n == 0) {
						collection.setDescription(null);
						collection.setTarget(null);
						onFailure.run();
						return null;
					}

					Runnable[] nextIndex = new Runnable[1];

					Runnable onFound = new Runnable() {

						@Override
						public void run() {
							success[0] = true;
							org.opensourcephysics.tools.LibraryComPADRE.start(nextIndex[0]);
						}

					};
					Runnable onNothingNew = new Runnable() {

						@Override
						public void run() {
							success[0] = true;
							org.opensourcephysics.tools.LibraryComPADRE.start(nextIndex[0]);
						}

					};

					int ni = n;

					NodeList l = list;
					nextIndex[0] = new Runnable() {

						@Override
						public void run() {
							if (index[0] >= ni) {
								whenDone.run();
							} else if (l != null) {
								// BH 2023.03.29 there was a bug in the transpiler requiring
								// explicit calling of static methods from lambda functions. 
								// no longer necessary, but this also fixes it.
								org.opensourcephysics.tools.LibraryComPADRE.loadNode(l.item(index[0]++), collection, treeNode, urlPath, onFound, onNothingNew);
							}

						}

					};
					org.opensourcephysics.tools.LibraryComPADRE.start(nextIndex[0]);
					return null;
			});

		} catch (Exception e) {
			whenDone.run();
			e.printStackTrace();
		}

	}

	protected static void start(Runnable runnable) {
		// DB 11.13.20 Bob, why run this runnable in a separate thread?
 		runnable.run();
		//new Thread(runnable).start();
	}
	
	protected static void loadNode(Node node, LibraryCollection collection, LibraryTreeNode treeNode, String urlPath,
			Runnable onFound, Runnable onNothingNew) {
		try {
			boolean found = false;
			Attachment attachment = null;
			if (isDesiredOSPType(node)) {
				if ("EJS".equals(desiredOSPType) && !isTrackerType(node)) { //$NON-NLS-1$
					attachment = getAttachment(node, new String[] {"Source Code"}); //$NON-NLS-1$
				} else {
					attachment = getAttachment(node, new String[] {"Main", "Primary"}); //$NON-NLS-1$
					if (attachment == null) {
						attachment = getAttachment(node, new String[] {"Supplemental"}); //$NON-NLS-1$
					}
				}
			}
			if (attachment == null) {
				onNothingNew.run();
				return;
			}
			// ignore if there is no associated attachment

			// create and add a new record to this node's collection
			String name = getChildValue(node, "title"); //$NON-NLS-1$
			LibraryResource record = new LibraryResource(name);
			collection.addResource(record);

			if (setRecord(record, node, attachment, treeNode)) {
				found = true;
				OSPRuntime.showStatus(name);
				record.setProperty("reload_url", urlPath); //$NON-NLS-1$
			}
			if (found) {
				onFound.run();
			} else {
				onNothingNew.run();
			}

		} catch (Exception e) {
			OSPLog.debug("LibraryComPADRE exception " + e.getMessage());
		}

	}

	/**
	 * Reloads a ComPADRE record into a LibraryTreeNode.
	 * 
	 * @param treeNode the LibraryTreeNode to reload
	 * @return true if successfully reloaded
	 */
	protected static void reloadResource(LibraryTreeNode treeNode, String urlPath, Runnable whenDone) {
		try {
			LibraryResource record = treeNode.record;
			URL url = new URL(urlPath);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document doc = factory.newDocumentBuilder().parse(url.openStream());

			// look through all ComPADRE records for a match
			NodeList list = doc.getElementsByTagName("record"); //$NON-NLS-1$
			int n = list.getLength();
			for (int i = 0; i < n; i++) { // process nodes
				Node node = list.item(i);
				Attachment attachment = null;
				System.out.println(node.getNodeName());
				if (isDesiredOSPType(node)) {
					if ("EJS".equals(desiredOSPType) && !isTrackerType(node)) { //$NON-NLS-1$
						attachment = getAttachment(node, new String[] {"Source Code"}); //$NON-NLS-1$
					} else {
						attachment = getAttachment(node, new String[] {"Main", "Primary"}); //$NON-NLS-1$
						if (attachment == null) {
							attachment = getAttachment(node, new String[] {"Supplemental"}); //$NON-NLS-1$
						}
					}
				}
				// ignore if there is no associated attachment
				if (attachment == null)
					continue;

				// check to see that the target is the one desired
				String downloadURL = processURL(attachment.url);
				if (!downloadURL.equals(record.getTarget()))
					continue;

				setRecord(record, node, attachment, treeNode);
			}
		} catch (Exception e) {
		}
	}

	/**
	 * Sets a LibraryTreeNode to a ComPADRE record.
	 * 
	 * @param treeNode the LibraryTreeNode to set
	 * @return true if successfully set
	 */
	protected static boolean setRecord(LibraryResource record, Node node, Attachment attachment,
			LibraryTreeNode treeNode) {
		try {
			// get the node data and create the HTML code
			String downloadURL = processURL(attachment.url);
			record.setTarget(downloadURL);
			String name = getChildValue(node, "title"); //$NON-NLS-1$
			record.setName(name);
			record.setProperty("download_filename", attachment.filename); //$NON-NLS-1$
			String type = getChildValue(node, "osp-type"); //$NON-NLS-1$
			if (isDesiredOSPType(node)) {
				if ("EJS".equals(desiredOSPType) && !isTrackerType(node)) { //$NON-NLS-1$
					type = LibraryResource.EJS_TYPE;
					record.setType(type);
				} else if ("Tracker".equals(desiredOSPType)) { //$NON-NLS-1$
					type = LibraryResource.TRACKER_TYPE;
					record.setType(type);
				} else {
					record.setType(LibraryResource.UNKNOWN_TYPE);
				}
			}

			String description = getChildValue(node, "description"); //$NON-NLS-1$
			String infoURL = getChildValue(node, "information-url"); //$NON-NLS-1$
			String thumbnailURL = getChildValue(node, "thumbnail-url"); //$NON-NLS-1$
			String authors = ""; //$NON-NLS-1$
			for (Node next : getAllChildren(getFirstChild(node, "contributors"), "contributor")) { //$NON-NLS-1$ //$NON-NLS-2$
				Element el = (Element) next;
				if ("Author".equals(el.getAttribute("role"))) //$NON-NLS-1$ //$NON-NLS-2$
					authors += getNodeValue(next) + ", "; //$NON-NLS-1$
			}
			if (authors.endsWith(", ")) //$NON-NLS-1$
				authors = authors.substring(0, authors.length() - 2);

			if (OSPRuntime.doCacheThumbnail) {
				// cache the thumbnail only if Java
				File cachedFile = ResourceLoader.getOSPCacheFile(thumbnailURL);
				String cachePath = cachedFile.getAbsolutePath();
				record.setThumbnail(cachePath);
				if (!cachedFile.exists()) {
					// asynchronously cache the thumbnail. 
					// BH 2020.11.14 Q: Is this necessary?
					treeNode.new ThumbnailLoader(thumbnailURL, cachePath, "LibraryComPADR.setRecord").runMe();
				}
				thumbnailURL = ResourceLoader.getURIPath(cachePath);
			} else {
				// BH no need to do this, and we can't access a local image this way in HTML, anyway.
				record.setThumbnail(thumbnailURL);
			}

			// get the html code and set the description
			String htmlCode = LibraryResource.getHTMLBody(name, type, thumbnailURL, description, authors, null, infoURL,
					attachment);
			record.setDescription(htmlCode);

			// convert <osp-subject> information into keywords and add metadata to the
			// library record
			record.setMetadata(null);
			ArrayList<String> words = new ArrayList<String>();
			for (Node next : getAllChildren(node, "osp-subject")) { //$NON-NLS-1$
				String[] subjects = getNodeValue(next).split(" / "); //$NON-NLS-1$
				for (String s : subjects) {
					// skip "General" subject--too vague
					if (s.equals("General")) //$NON-NLS-1$
						continue;
					if (!words.contains(s))
						words.add(s);
				}
			}
			if (!words.isEmpty()) {
				StringBuffer buf = new StringBuffer();
				for (String s : words) {
					buf.append(s + ", "); //$NON-NLS-1$
				}
				String keywords = buf.toString();
				keywords = keywords.substring(0, keywords.length() - 2);
				record.addMetadata(new LibraryResource.Metadata("keywords", keywords)); //$NON-NLS-1$
			}
			if (!"".equals(authors)) //$NON-NLS-1$
				record.addMetadata(new LibraryResource.Metadata("author", authors)); //$NON-NLS-1$

			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Returns data for a downloadable DOM Node attachment.
	 * 
	 * @param node           the DOM Node
	 * @param attachmentType the attachment type
	 * @return Attachment, or null if no attachment found
	 */
	protected static Attachment getAttachment(Node node, String[] attachmentTypes) {
		String id = getChildValue(node, "file-identifier"); //$NON-NLS-1$
		NodeList childList = node.getChildNodes();
		Attachment attachment = null;
		for (int i = 0, n = childList.getLength(); i < n; i++) {
			Node child = childList.item(i);
			if (!child.getNodeName().equals("attached-document")) //$NON-NLS-1$
				continue;
			boolean matchID = id.equals(getChildValue(child, "file-identifier"));
			Node fileTypeNode = getFirstChild(child, "file-type"); //$NON-NLS-1$
			for (int j = 0; j < attachmentTypes.length; j++) {
				if (fileTypeNode != null 
						&& attachmentTypes[j].equals(getNodeValue(fileTypeNode))) {
					Node urlNode = getFirstChild(child, "download-url"); //$NON-NLS-1$
					if (urlNode != null) { // found downloadable attachment
						// create attachment if null, replace if matching file-ID found
						if (attachment == null || matchID) { //$NON-NLS-1$
							String attachmentURL = getNodeValue(urlNode);
							Element fileNode = (Element) getFirstChild(child, "file-name"); //$NON-NLS-1$
							attachment = new Attachment(node, attachmentTypes[j], attachmentURL, getNodeValue(fileNode),
									fileNode == null ? 0 : Integer.parseInt(fileNode.getAttribute("file-size"))); //$NON-NLS-1$
						}
					}
				}				
			}
		}
		return attachment;
	}

	/**
	 * Returns the first child node with the given name.
	 * 
	 * @param parent the parent Node
	 * @param name   the child name
	 * @return the first child Node found, or null if none
	 */
	protected static Node getFirstChild(Node parent, String name) {
		NodeList childList = parent.getChildNodes();
		for (int i = 0, n = childList.getLength(); i < n; i++) {
			Node child = childList.item(i);
			if (child.getNodeName().equals(name))
				return child;
		}
		return null;
	}

	/**
	 * Returns all child nodes with the given name.
	 * 
	 * @param parent the parent Node
	 * @param name   the name
	 * @return a list of Nodes (may be empty)
	 */
	protected static List<Node> getAllChildren(Node parent, String name) {
		java.util.List<Node> list = new ArrayList<Node>();
		NodeList childrenList = parent.getChildNodes();
		for (int i = 0, n = childrenList.getLength(); i < n; i++) {
			Node child = childrenList.item(i);
			if (child.getNodeName().equals(name))
				list.add(child);
		}
		return list;
	}

	/**
	 * Gets the value of a Node.
	 * 
	 * @param node the Node
	 * @return the value
	 */
	protected static String getNodeValue(Node node) {
		if (node != null) {
			for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
				if (child.getNodeType() == Node.TEXT_NODE)
					return child.getNodeValue();
			}
		}
		return null;
	}

	/**
	 * Gets the value of the first child node with a given name.
	 * 
	 * @param parent the parent Node
	 * @param name   the name of the child
	 * @return the value of the first child found, or null if none
	 */
	protected static String getChildValue(Node parent, String name) {
		Node node = getFirstChild(parent, name);
		return (node == null ? null : getNodeValue(node));
	}

	/**
	 * Replaces "&amp" with "&" in HTML code.
	 * 
	 * @param url the HTML code
	 * @return the clean URL string
	 */
	protected static String processURL(String url) {
		StringBuffer processed = new StringBuffer();
		int index = url.indexOf("&amp;"); //$NON-NLS-1$
		while (index >= 0) {
			processed.append(url.subSequence(0, index + 1));
			url = url.substring(index + 5);
			index = url.indexOf("&amp;"); //$NON-NLS-1$
		}
		processed.append(url);
		return processed.toString();
	}

	/**
	 * Writes a DOM document to a file for testing.
	 * 
	 * @param doc      the Document
	 * @param filename the filename to write to
	 * @return the String contents of the document
	 */
	protected static String writeXmlFile(Document doc, String filename) {
		try {
			// Prepare the DOM document for writing
			Source source = new DOMSource(doc);

			// Prepare the output file
			File file = new File(filename);
			Result result = new StreamResult(file);

			// Write the DOM document to the file
			Transformer xformer = TransformerFactory.newInstance().newTransformer();
			xformer.transform(source, result);
			return ResourceLoader.getString(filename);
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * Returns a descriptive name for a given ComPADRE path (query).
	 * 
	 * @param path the query string
	 * @return the name of the collection
	 */
	public static String getCollectionName(String path) {
		if (path.startsWith(EJS_SERVER_TREE))
			return EJS_COLLECTION_NAME;
		if (path.startsWith(TRACKER_SERVER_TREE))
			return TRACKER_COLLECTION_NAME;
		return GENERIC_COLLECTION_NAME;
	}

	/**
	 * Returns the LibraryCollection for a given ComPADRE path (query).
	 * 
	 * @param path the query string
	 * @return the collection
	 */
	protected static LibraryCollection getCollection(String path) {
		String name = getCollectionName(path);
		boolean primarySubjectOnly = path.indexOf(PRIMARY_ONLY) > -1;
		LibraryCollection collection = new LibraryCollection(name);
		if (name.equals(EJS_COLLECTION_NAME)) {
			collection.setHTMLPath(EJS_INFO_URL);
		} else if (name.equals(TRACKER_COLLECTION_NAME)) {
			collection.setHTMLPath(TRACKER_INFO_URL);
		}
		LibraryResource aboutOSP = new LibraryResource(ABOUT_OSP);
		aboutOSP.setHTMLPath(OSP_INFO_URL);
		collection.addResource(aboutOSP);
		load(collection, path);
		String base = EJS_SERVER_RECORDS;
		if (name.equals(TRACKER_COLLECTION_NAME)) {
			base = TRACKER_SERVER_RECORDS;
		}
		if (primarySubjectOnly)
			base += PRIMARY_ONLY;
		collection.setBasePath(base);

//		LibraryBrowser.addSearchMapResource(collection);
		return collection;
	}

	/**
	 * Returns the collection path for an EJS or tracker tree.
	 * 
	 * @param path               the ComPADRE query string
	 * @param primarySubjectOnly true to limit results to their primary subject
	 * @return the corrected ComPADRE query string
	 */
	protected static String getCollectionPath(String path, boolean primarySubjectOnly) {
		boolean isPrimary = path.endsWith(PRIMARY_ONLY);
		if (isPrimary && primarySubjectOnly)
			return path;
		if (!isPrimary && !primarySubjectOnly)
			return path;
		if (!isPrimary && primarySubjectOnly)
			return path + PRIMARY_ONLY;
		return path.substring(0, path.length() - PRIMARY_ONLY.length());
	}

	/**
	 * Determines if a path is a ComPADRE query.
	 * 
	 * @param path the path
	 * @return true if path starts with the standard partial ComPADRE query string
	 */
	protected static boolean isComPADREPath(String path) {
		if (path != null && path.startsWith(COMPADRE_QUERY))
			return true;
		return false;
	}

	/**
	 * Determines if a query path limits results to the primary subject only.
	 * 
	 * @param path the path
	 * @return true if path contains a primary-subject-only flag
	 */
	protected static boolean isPrimarySubjectOnly(String path) {
		return path.indexOf(PRIMARY_ONLY) > -1;
	}

	/**
	 * Determines if a node has an <osp-type> consistent with desiredOSPType.
	 * 
	 * @param node the node
	 * @return true if is of the desired <osp-type>
	 */
	protected static boolean isDesiredOSPType(Node node) {
		List<Node> nodes = getAllChildren(node, "osp-type"); //$NON-NLS-1$
		String s;
		for (Node next : nodes) {
			// if desiredOSPType has not been specified then all osp-types are valid
			if (desiredOSPType == null
					|| (s = getNodeValue(next)).contains(desiredOSPType)
					|| s.contains("Tracker")
					)
				return true;
		}
		return false;
	}
	
	protected static boolean isTrackerType(Node node) {
		List<Node> nodes = getAllChildren(node, "osp-type"); //$NON-NLS-1$
		for (Node next : nodes) {
			// if desiredOSPType has not been specified then all osp-types are valid
			if (getNodeValue(next).contains("Tracker"))
				return true;
		}
		return false;
	}


}