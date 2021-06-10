/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.controls;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.swing.JOptionPane;

import org.opensourcephysics.tools.Resource;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * This is a basic xml control for storing data.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public final class XMLControlElement extends XMLNode implements XMLControl {
	// static constants

	public static final int ALWAYS_DECRYPT = 0;

	public static final int PASSWORD_DECRYPT = 3;

	public static final int NEVER_DECRYPT = 5;
	// static fields

	public static int compactArraySize = 0;
	protected static String encoding = "UTF-8"; //$NON-NLS-1$

	// instance fields
	protected Class<?> theClass = null;

	protected Map<String, Integer> counts = new HashMap<String, Integer>(); // maps numbered names to counts
	protected Object object;

	protected int level;
	private ArrayList<String> propNames = new ArrayList<String>();
	private ArrayList<XMLProperty> props = new ArrayList<XMLProperty>();
	protected BufferedReader input;
	protected BufferedWriter output;

	public boolean canWrite;
	protected boolean valid = false;
	protected boolean readFailed = false;
	protected String version;
	protected String doctype = "osp10.dtd"; //$NON-NLS-1$
	private String basepath;

	@Override
	public String getBasepath() {
		return basepath;
	}

	@Override
	public void setBasepath(String basepath) {
		this.basepath = basepath;
	}

	private String password;
	private int decryptPolicy = ALWAYS_DECRYPT;

	/**
	 * something being passed to the nascent control
	 */
	private Object data;

	private Map<String, XMLControl> childMap;

	private XMLControl[] childControls;

	private Map<String, XMLProperty> propMap;

	/**
	 * see TrackerPanel.Loader
	 * 
	 * @return
	 */
	public Object getData() {
		return data;
	}

	/**
	 * Constructs an empty control for the Object class.
	 */
	public XMLControlElement() {

		/** empty block */
	}

	/**
	 * Constructs an empty control for the specified class.
	 *
	 * @param type the class.
	 */
	public XMLControlElement(Class<?> type) {
		setObjectClass(type);
	}

	/**
	 * Constructs and loads a control with the specified object.
	 *
	 * @param obj the object.
	 */
	public XMLControlElement(Object obj) {
		setObjectClass(obj.getClass());
		saveObject(obj);
	}

	/**
	 * Constructs a control with the specified parent.
	 *
	 * @param parent the parent.
	 */
	public XMLControlElement(XMLProperty parent) {
		this.parent = parent;
		level = parent.getLevel();
	}

	/**
	 * BH we need to read the File object directly, as it will have the data in it
	 * already.
	 * 
	 * @param xmlFile
	 */
	public XMLControlElement(File xmlFile) {
		try {
			String data = getFileData(xmlFile);
			if (data != null) {
				readData(data);
				return;
			}
		} catch (Exception ex) {
			OSPLog.warning("Failed to read xml: " + xmlFile + ex.getMessage()); //$NON-NLS-1$
		}
		readFailed = true;
	}

	/**
	 * BH by far the simplest way to read a file in its entirety as byte[] or String
	 * 
	 * @param xmlFile
	 * @return
	 */
	private String getFileData(File xmlFile) {
		byte[] bytes = null;
		try {
			bytes = ResourceLoader.getURLContents(xmlFile.toURI().toURL(), false);
		} catch (MalformedURLException e) {
		}
		return (bytes == null ? null : new String(bytes));
	}

	/**
	 * Constructs a control and reads the specified input. Input may be a file name
	 * or an xml string
	 *
	 * @param input the input string
	 */
	public XMLControlElement(String input) {
		this();
		readData(input);
	}

	/**
	 * Constructs a copy of the specified XMLControl.
	 *
	 * @param control the XMLControl to copy.
	 */
	public XMLControlElement(XMLControl control) {
		this();
		readXML(control.toXML());
	}

	/**
	 * Locks the control's interface. Values sent to the control will not update the
	 * display until the control is unlocked. Not implemented.
	 *
	 * @param lock boolean
	 */
	@Override
	public void setLockValues(boolean lock) {

		/** empty block */
	}

	/**
	 * Sets a property with the specified name and boolean value.
	 *
	 * @param name  the name
	 * @param value the boolean value
	 */
	@Override
	public void setValue(String name, boolean value) {
		if (name == null) {
			return;
		}
		setXMLProperty(name, XMLProperty.TYPE_BOOLEAN, String.valueOf(value), false); //$NON-NLS-1$
	}

	/**
	 * Sets a property with the specified name and double value.
	 *
	 * @param name  the name
	 * @param value the double value
	 */
	@Override
	public void setValue(String name, double value) {
		if (name == null) {
			return;
		}
		setXMLProperty(name, XMLProperty.TYPE_DOUBLE, String.valueOf(value), false); //$NON-NLS-1$
	}

	/**
	 * Sets a property with the specified name and int value.
	 *
	 * @param name  the name
	 * @param value the int value
	 */
	@Override
	public void setValue(String name, int value) {
		if (name == null) {
			return;
		}
		setXMLProperty(name, XMLProperty.TYPE_INT, String.valueOf(value), false); //$NON-NLS-1$
	}

	/**
	 * Sets a property with the specified name and object value.
	 *
	 * @param name the name
	 * @param obj  the object
	 */
	@Override
	public void setValue(String name, Object obj) {
		setValue(name, obj, XMLPropertyElement.defaultWriteNullFinalArrayElements);
	}

	/**
	 * Sets a property with the specified name and object value.
	 *
	 * @param name                  the name
	 * @param obj                   the object
	 * @param writeNullFinalElement true to write a final null array element (if
	 *                              needed)
	 */
	public void setValue(String name, Object obj, boolean writeNullFinalElement) {
		if (name == null) {
			return;
		}
		// clear the property if obj is null
		if (obj == null) {
			boolean childRemoved = false;
			if (getPropMap().containsKey(name)) {
				Iterator<XMLProperty> it = props.iterator();
				while (it.hasNext()) {
					XMLProperty prop = it.next();
					if (name.equals(prop.getPropertyName())) {
						it.remove();
						propNames.remove(name);
						getPropMap().remove(name);
						if (getChildMap().remove(name) != null) {
							childRemoved = true;
						}
						break;
					}
				}
			}
			if (childRemoved) {
				childControls = null;
			}
			return;
		}
		if (obj instanceof Boolean) {
			setValue(name, ((Boolean) obj).booleanValue());
			return;
		}
		int type = XMLProperty.getDataType(obj);
		switch (type) {
		case XMLProperty.TYPE_UNKNOWN:
			break;
		case XMLProperty.TYPE_INT:
		case XMLProperty.TYPE_DOUBLE:
			obj = obj.toString();
			// fall through
		default:
			setXMLProperty(name, type, obj, writeNullFinalElement);
			break;			
		}
	}

	/**
	 * Gets the boolean value of the specified named property.
	 *
	 * @param name the name
	 * @return the boolean value, or false if none found
	 */
	@Override
	public boolean getBoolean(String name) {
		XMLProperty prop = getXMLProperty(name);
		if (prop != null && prop.getPropertyType() == XMLProperty.TYPE_BOOLEAN) { //$NON-NLS-1$
			return "true".equals(prop.getPropertyContent().get(0)); //$NON-NLS-1$
		} else if (prop != null && prop.getPropertyType() == XMLProperty.TYPE_STRING) { //$NON-NLS-1$
			return "true".equals(prop.getPropertyContent().get(0)); //$NON-NLS-1$
		}
		return false;
	}

	/**
	 * Gets the double value of the specified named property.
	 *
	 * @param name the name
	 * @return the double value, or Double.NaN if none found
	 */
	@Override
	public double getDouble(String name) {
		XMLProperty prop = getXMLProperty(name);
		switch (prop == null ? XMLProperty.TYPE_UNKNOWN : prop.getPropertyType()) {
		case XMLProperty.TYPE_DOUBLE:
		case XMLProperty.TYPE_INT:
		case XMLProperty.TYPE_STRING:
			try {
				return Double.parseDouble((String) prop.getPropertyContent().get(0));
			} catch (Exception ex) {
			}
			break;
		}
		return Double.NaN;
	}

	/**
	 * Gets the int value of the specified named property.
	 *
	 * @param name the name
	 * @return the int value, or Integer.MIN_VALUE if none found
	 */
	@Override
	public int getInt(String name) {
		XMLProperty prop = getXMLProperty(name);
		switch (prop == null ? XMLProperty.TYPE_UNKNOWN : prop.getPropertyType()) {
		case XMLProperty.TYPE_INT:
		case XMLProperty.TYPE_STRING:
			try {
				return Integer.parseInt((String) prop.getPropertyContent().get(0));
			} catch (Exception ex) {
			}
			break;
		case XMLProperty.TYPE_OBJECT:
			XMLControl control = (XMLControl) prop.getPropertyContent().get(0);
			if (control.getObjectClass() == OSPCombo.class) {
				OSPCombo combo = (OSPCombo) control.loadObject(null);
				return combo.getSelectedIndex();
			}
			break;
		}
		return Integer.MIN_VALUE;
	}

	/**
	 * Gets the string value of the specified named property.
	 *
	 * @param name the name
	 * @return the string value, or null if none found
	 */
	@Override
	public String getString(String name) {
		XMLProperty prop = getXMLProperty(name);
		int type = (prop == null ? XMLProperty.TYPE_UNKNOWN : prop.getPropertyType());
		
		if (type == XMLProperty.TYPE_STRING) {
			return XML.removeCDATA((String) prop.getPropertyContent().get(0));
		} 
		if (name.equals("basepath") && (getRootControl() != null)) { //$NON-NLS-1$
			return getRootControl().basepath;
		} 
		if (type == XMLProperty.TYPE_OBJECT) {
			XMLControl control = (XMLControl) prop.getPropertyContent().get(0);
			if (control.getObjectClass() == OSPCombo.class) {
				OSPCombo combo = (OSPCombo) control.loadObject(null);
				return combo.toString();
			}
		}
		return null;
	}

	/**
	 * Gets the object value of the specified named profperty.
	 *
	 * @param name the name
	 * @return the object, or null if not found
	 */
	@Override
	public Object getObject(String name) {
		XMLProperty prop = getXMLProperty(name);
		if (prop != null) {
			switch (prop.getPropertyType()) {
			case XMLProperty.TYPE_OBJECT: //$NON-NLS-1$
				return objectValue(prop);
			case XMLProperty.TYPE_ARRAY: //$NON-NLS-1$
				return arrayValue(prop);
			case XMLProperty.TYPE_COLLECTION: //$NON-NLS-1$
				return collectionValue(prop);
			case XMLProperty.TYPE_INT: //$NON-NLS-1$
				return Integer.valueOf(intValue(prop));
			case XMLProperty.TYPE_DOUBLE: //$NON-NLS-1$
				return new Double(doubleValue(prop));
			case XMLProperty.TYPE_BOOLEAN: //$NON-NLS-1$
				return Boolean.valueOf(booleanValue(prop));
			case XMLProperty.TYPE_STRING: //$NON-NLS-1$
				return stringValue(prop);
			}
		}
		return null;
	}

	/**
	 * Gets the set of property names.
	 *
	 * @return a set of names
	 */
	@Override
	public Collection<String> getPropertyNames() {
		synchronized (propNames) {
			return new ArrayList<String>(propNames);
		}
	}

	@Override
	public Collection<String> getPropertyNamesRaw() {
		return propNames;
	}

	/**
	 * Gets the type of the specified property. Returns null if the property is not
	 * found.
	 *
	 * @param name the property name
	 * @return the type
	 */
	@Override
	public int getPropertyType(String name) {
		XMLProperty prop = getXMLProperty(name);
		return (prop == null ? XMLProperty.TYPE_UNKNOWN : prop.getPropertyType());
	}

	/**
	 * Sets the password. Files are encrypted when the password is non-null.
	 *
	 * @param pass the password or phrase
	 */
	public void setPassword(String pass) {
		password = pass;
		if (getObjectClass() != Cryptic.class) {
			setValue("xml_password", pass); //$NON-NLS-1$
		}
	}

	/**
	 * Gets the password.
	 *
	 * @return the password
	 */
	public String getPassword() {
		if (password == null) {
			password = getString("xml_password"); //$NON-NLS-1$
		}
		return password;
	}

	/**
	 * Sets the decryption policy.
	 *
	 * @param policy the decryption policy: NEVER_DECRYPT, PASSWORD_DECRYPT or
	 *               ALWAYS_DECRYPT
	 */
	public void setDecryptPolicy(int policy) {
		if (policy == NEVER_DECRYPT) {
			decryptPolicy = NEVER_DECRYPT;
		} else if (policy == PASSWORD_DECRYPT) {
			decryptPolicy = PASSWORD_DECRYPT;
		} else {
			decryptPolicy = ALWAYS_DECRYPT;
		}
	}

	static Object sync = new Object();

	public void readAsync(String name, Function<String, Void> whenDone) {
		synchronized (sync) {
			// OSPLog.finest("reading " + name); //$NON-NLS-1$
			Resource res = ResourceLoader.getResource(name);
			if (res == null) {
				processReader(name, null, null, whenDone);
			} else if (res.getFile() != null) {
				// synchronous for file
				processReader(name, res, res.openReader(), whenDone);
			} else {
				ResourceLoader.getURLContentsAsync(res.getURL(), (bytes) -> {
					if (bytes == null) {
						processReader(name, null, null, whenDone);
					} else {
						processReader(name, res, ResourceLoader.readerForStream(new ByteArrayInputStream(bytes), null),
								whenDone);
					}
					return null;
				});
			}
		}
	}

	/**
	 * Read this file resource
	 * @param name
	 * @param res
	 * @param in
	 * @param whenDone will be passed a File[] array or null
	 */
	protected void processReader(String name, Resource res, BufferedReader in, Function<String, Void> whenDone) {
		if (res == null) {
			OSPLog.warning("Could not open " + name);
			readFailed = true;
			whenDone.apply(null);
			return;
		}
		read(in);
		whenDone.apply(readFailed ? null : setPath(name, res));
	}

	/**
	 * read raw data or xml
	 * 
	 * @param input
	 */
	private void readData(String input) {
		if (input.startsWith("<?xml")) { //$NON-NLS-1$
			readXML(input);
		} else {
			read(input);
		}
	}

	/**
	 * Reads data into this control from a named source.
	 *
	 * @param fileName the name
	 * @return the path of the opened document or null if failed
	 */
	@Override
	public String read(String fileName) {
		synchronized (sync) {
			//OSPLog.debug("XMLControlElement.reading " + fileName); //$NON-NLS-1$
			Resource res = ResourceLoader.getResource(fileName);
			if (res != null) {
				try {
					BufferedReader in = res.openReader();
					if (in != null) {
						// BH just avoids the NullPointerException, particularly for prefs files
						read(in);
						return setPath(fileName, res);
					}
				} catch (Exception e) {
				}
			}
			OSPLog.warning("Could not open " + res);
			readFailed = true;
		}
		return null;
	}

	/**
	 * Reads the control from an xml string.
	 *
	 * @param xml the xml string
	 */
	@Override
	public void readXML(String xml) {
		readXML(xml, null);
	}
	
	private boolean readXML(String xml, String requiredType) {
		if (readInput(xml == null ? null : new BufferedReader(new StringReader(xml)), requiredType)) {
			canWrite = false;
		}
		return !readFailed;
	}

	/**
	 * Reads the control from a Reader, setting readFailed if there are any problems.
	 *
	 * @param in the Reader
	 */
	@Override
	public void read(Reader in) {
		readInput(in == null ? null : in instanceof BufferedReader ? (BufferedReader) in : new BufferedReader(in), null);
		try {
			if (input != null)
				input.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Reads data into this control from a named source if the source specifies the
	 * same class as the current className.
	 * 
	 * BH: never called
	 *
	 * @param name the name
	 * @param type the class
	 * @return the path of the opened document or null if failed
	 */
	public String readForClass(String name, Class<?> type) {
		Resource res = ResourceLoader.getResource(name);
		return (readInput(res == null ? null : new BufferedReader(res.openReader()), type.getName()) 
				? setPath(name, res) : null);
	}

	/**
	 * Reads this control from an xml string if the xml specifies the same class as
	 * the current className. Utilized in org.colos.ejs.library.Simulation
	 *
	 * @param xml  the xml string
	 * @param type the class
	 * @return true if successfully read
	 */
	public boolean readXMLForClass(String xml, Class<?> type) {
		return readXML(xml, type.getName());
	}

	private String setPath(String name, Resource res) {
		String path = XML.getDirectoryPath(name);
		if (path.length() == 0) {
			basepath = XML.getDirectoryPath(res.getAbsolutePath());
		} else {
			ResourceLoader.addSearchPath(path);
			basepath = path;
		}
		File file = res.getFile();
		canWrite = ((file != null) && file.canWrite());
		return res.getAbsolutePath();
	}

	/**
	 * Returns true if the most recent read operation failed.
	 *
	 * @return <code>true</code> if the most recent read operation failed
	 */
	@Override
	public boolean failedToRead() {
		return readFailed;
	}

	/**
	 * Writes this control as an xml file with the specified name.
	 *
	 * @param fileName the file name
	 * @return the path of the saved document or null if failed
	 */
	@Override
	public String write(String fileName) {
		canWrite = true;
		int n = fileName.lastIndexOf("/"); //$NON-NLS-1$
		if (n < 0) {
			n = fileName.lastIndexOf("\\"); //$NON-NLS-1$
		}
		if (n > 0) {
			String dir = fileName.substring(0, n + 1);
			File file = new File(dir);
			if (!file.exists() && !file.mkdirs()) {
				canWrite = false;
				return null;
			}
		}
		try {
			File file = new File(fileName);
			if (file.exists() && !file.canWrite()) {
				JOptionPane.showMessageDialog(null,
						ControlsRes.getString("Dialog.ReadOnly.Message") + ": " + file.getPath(), //$NON-NLS-1$ //$NON-NLS-2$
						ControlsRes.getString("Dialog.ReadOnly.Title"), //$NON-NLS-1$
						JOptionPane.PLAIN_MESSAGE);
				canWrite = false;
				return null;
			}

			FileOutputStream stream = new FileOutputStream(file);
			java.nio.charset.Charset charset = java.nio.charset.Charset.forName(encoding);
			write(new OutputStreamWriter(stream, charset));
			// add search path to ResourceLoader
			if (file.exists()) {
				String path = XML.getDirectoryPath(file.getCanonicalPath());
				ResourceLoader.addSearchPath(path);
			}
			// write dtd if valid
			if (isValid()) {
				// replace xml file name with dtd file name
				if (fileName.indexOf("/") != -1) { //$NON-NLS-1$
					fileName = fileName.substring(0, fileName.lastIndexOf("/") + 1) + getDoctype(); //$NON-NLS-1$
				} else if (fileName.indexOf("\\") != -1) { //$NON-NLS-1$
					fileName = fileName.substring(0, fileName.lastIndexOf("\\") + 1) + getDoctype(); //$NON-NLS-1$
				} else {
					fileName = doctype;
				}
				writeDocType(new FileWriter(fileName));
			}

			if (/** @j2sNative true || */
			file.exists()) {
				return XML.getAbsolutePath(file);
			}
		} catch (IOException ex) {
			canWrite = false;
			OSPLog.warning(ex.getMessage());
		}
		return null;
	}

	/**
	 * Writes this control to a Writer.
	 *
	 * @param out the Writer
	 */
	@Override
	public void write(Writer out) {
		try {
			output = new BufferedWriter(out);
			String xml = toXML();
			// if password-protected, encrypt the xml string and save the cryptic
			if (getPassword() != null) {
				Cryptic cryptic = new Cryptic(xml);
				XMLControl control = new XMLControlElement(cryptic);
				xml = control.toXML();
			}
			output.write(xml);
			output.flush();
			output.close();
		} catch (IOException ex) {
			OSPLog.info(ex.getMessage());
		}
	}

	/**
	 * Writes the DTD to a Writer.
	 *
	 * @param out the Writer
	 */
	public void writeDocType(Writer out) {
		try {
			output = new BufferedWriter(out);
			output.write(XML.getDTD(getDoctype()));
			output.flush();
			output.close();
		} catch (IOException ex) {
			OSPLog.info(ex.getMessage());
		}
	}

	/**
	 * Returns this control as an xml string.
	 *
	 * @return the xml string
	 */
	@Override
	public String toXML() {
		return toString();
	}

	/**
	 * Sets the valid property.
	 *
	 * @param valid <code>true</code> to write the DTD and DocType
	 */
	public void setValid(boolean valid) {
		this.valid = valid;
	}

	/**
	 * Gets the valid property. When true, this writes the DTD and defines the
	 * DocType when writing an xml document. Note: the presence or absense of the
	 * DocType header and DTD has no effect on the read() methods--this will always
	 * read a well-formed osp document and ignore a non-osp document.
	 *
	 * @return <code>true</code> if this is valid
	 */
	public boolean isValid() {
		return valid && (XML.getDTD(getDoctype()) != null);
	}

	/**
	 * Sets the version.
	 *
	 * @param vers the version data
	 */
	public void setVersion(String vers) {
		version = vers;
	}

	/**
	 * Gets the version. May return null.
	 *
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Sets the doctype. Not yet implemented since only one doctype is defined.
	 *
	 * @param name the doctype resource name
	 */
	public void setDoctype(String name) {
		if (XML.getDTD(name) != null) {
			// check that name is accepted, etc
			// could make acceptable names be public String constants?
		}
	}

	/**
	 * Gets the doctype. May return null.
	 *
	 * @return the doctype
	 */
	public String getDoctype() {
		return doctype;
	}

	/**
	 * Sets the class of the object for which this element stores data.
	 *
	 * @param type the <code>Class</code> of the object
	 */
	public void setObjectClass(Class<?> type) {
		if (object != null && !type.isInstance(object)) {
			throw new RuntimeException(
					object + " " + ControlsRes.getString("XMLControlElement.Exception.NotInstanceOf") + " " + type); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		theClass = type;
		className = type.getName();
	}

	/**
	 * Gets the class of the object for which this element stores data.
	 *
	 * @return the <code>Class</code> of the object
	 */
	@Override
	public Class<?> getObjectClass() {
		if (className == null || (theClass != null && theClass.getName().equals(className))) {
			return theClass;
		}
		theClass = null;
		try {
			return theClass = Class.forName(className);
		} catch (ClassNotFoundException ex) {
			try {
				ClassLoader loader = XML.getClassLoader();
				return (loader == null ? null : (theClass = loader.loadClass(className)));
			} catch (ClassNotFoundException e) {
			}
			return theClass;
		}
	}

	/**
	 * Gets the name of the object class for which this element stores data.
	 *
	 * @return the object class name
	 */
	@Override
	public String getObjectClassName() {
		return className;
	}

	/**
	 * Saves an object's data in this element.
	 *
	 * @param obj the object to save.
	 */
	@Override
	public void saveObject(Object obj) {
		if (obj == null) {
			obj = object;
		}
		Class<?> type = getObjectClass();
		if ((type == null) || type.equals(Object.class)) {
			if (obj == null) {
				return;
			}
			type = obj.getClass();
		}
		if (type.isInstance(obj)) {
			object = obj;
			className = obj.getClass().getName();
			clearValues();
			XML.ObjectLoader loader = XML.getLoader(type);
			loader.saveObject(this, obj);
		}
	}

	/**
	 * BH! THIS METHOD COULD FAIL IN JAVASCRIPT because importAll is false
	 * 
	 * 111 references !!!
	 * 
	 * 
	 * Loads an object with data from this element. This asks the user for approval
	 * and review before importing data from mismatched classes.
	 *
	 * @param obj the object to load
	 * @return the loaded object
	 */
	@Override
	public Object loadObject(Object obj) {
		return loadObject(obj, false, false);
	}

	public Object loadObject(Object obj, Object data) {
		this.data = data;
		return loadObject(obj, false, false);
	}

	/**
	 * BH! THIS METHOD WILL FAIL IN JAVASCRIPT when autoImport is false
	 * 
	 * (not called in OSP or Tracker?)
	 * 
	 * 
	 * 
	 * Loads an object with data from this element. This asks the user to review
	 * data from mismatched classes before importing it.
	 *
	 * @param obj        the object to load
	 * @param autoImport true to automatically import data from mismatched classes
	 * @return the loaded object
	 */
	public Object loadObject(Object obj, boolean autoImport) {
		return loadObject(obj, autoImport, false);
	}

	/**
	 * Loads an object with data from this element.
	 * 
	 * false for BOTH parameters autoImport and importAll will FAIL IN JAVASCRIPT
	 * 
	 *
	 * @param obj        the object to load
	 * @param autoImport true to automatically import data from mismatched classes
	 * @param importAll  true to import all importable data
	 * @return the loaded object
	 */
	public Object loadObject(Object obj, boolean autoImport, boolean importAll) {
		Class<?> myType = getObjectClass();
		XML.ObjectLoader loader = null;
		Class<?> oclass = (obj == null ? null : obj.getClass());
		// we must establish relationship between the control's XML type
		// and the object's Java type
		if (myType == null) {
			if (oclass == null) {
				return null;
			}
			if (!autoImport) {
				int result = JOptionPane.showConfirmDialog(null,
						ControlsRes.getString("XMLControlElement.Dialog.UnknownClass.Message") + " \"" + className //$NON-NLS-1$ //$NON-NLS-2$
								+ "\"" + XML.NEW_LINE //$NON-NLS-1$
								+ ControlsRes.getString("XMLControlElement.Dialog.MismatchedClass.Query") + " \"" //$NON-NLS-1$ //$NON-NLS-2$
								+ obj.getClass().getName() + "\"", //$NON-NLS-1$
						ControlsRes.getString("XMLControlElement.Dialog.MismatchedClass.Title"), //$NON-NLS-1$
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (result != JOptionPane.YES_OPTION) {
					return obj;
				}
			}

			// BH -

			if (!importInto(obj, importAll)) {
				return obj;
			}
			myType = oclass;
		} else if (obj == null) {
			oclass = myType;
		}
		try {
			loader = XML.getLoader(myType);

			// BH 2020.02.13 adding check for null obj
			// type may be
			if (myType == oclass || oclass != null && loader.getClass() == XML.getLoader(oclass).getClass()) {
				autoImport = true;
				importAll = true;
			}
		} catch (Exception ex) {

			/** empty block */
		}
		if (obj != null && myType != oclass && !myType.isInstance(obj)) {
			if (!autoImport) {
				int result = JOptionPane.showConfirmDialog(null,
						ControlsRes.getString("XMLControlElement.Dialog.MismatchedClass.Message") + " \"" //$NON-NLS-1$ //$NON-NLS-2$
								+ myType.getName() + "\"" + XML.NEW_LINE //$NON-NLS-1$
								+ ControlsRes.getString("XMLControlElement.Dialog.MismatchedClass.Query") + " \"" //$NON-NLS-1$ //$NON-NLS-2$
								+ obj.getClass().getName() + "\"", //$NON-NLS-1$
						ControlsRes.getString("XMLControlElement.Dialog.MismatchedClass.Title"), //$NON-NLS-1$
						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (result != JOptionPane.YES_OPTION) {
					return obj;
				}
			}
			if (!importInto(obj, importAll)) {
				return obj;
			}
			loader = XML.getLoader(myType = oclass);
		}
		if (obj == null) { // if obj is null, try to create a new one
			obj = (object == null ? loader.createObject(this) : object);
			if (obj == null || !myType.isInstance(obj)) {
				return obj;
			}
		}
		return object = obj = loader.loadObject(this, obj);
	}

	/**
	 * Clears all properties.
	 */
	@Override
	public void clearValues() {
		props.clear();
		propNames.clear();
		if (propMap != null)
			propMap.clear();
		if (childMap != null) {
			childMap.clear();
			childControls = null;
		}
	}

	/**
	 * Method required by the Control interface.
	 *
	 * @param s the string
	 */
	@Override
	public void println(String s) {
		System.out.println(s);
	}

	/**
	 * Method required by the Control interface.
	 */
	@Override
	public void println() {
		System.out.println();
	}

	/**
	 * Method required by the Control interface.
	 *
	 * @param s the string
	 */
	@Override
	public void print(String s) {
		System.out.print(s);
	}

	/**
	 * Method required by the Control interface.
	 */
	@Override
	public void clearMessages() {

		/** empty block */
	}

	/**
	 * Method required by the Control interface.
	 *
	 * @param s the string
	 */
	@Override
	public void calculationDone(String s) {

		/** empty block */
	}

	/**
	 * Gets the property name.
	 *
	 * @return a name
	 */
	@Override
	public String getPropertyName() {
		XMLProperty parent = getParentProperty();
		// if no class name, return parent name
		if (className == null) {
			if (parent == null) {
				return "null"; //$NON-NLS-1$
			}
			return parent.getPropertyName();
		}
		// else if array or collection item, return numbered class name
		else if (this.isArrayOrCollectionItem()) {
			if (this.name == null) {
				// add numbering or name property
				String myName = this.getString("name"); //$NON-NLS-1$
				if (myName != null && !"".equals(myName)) { //$NON-NLS-1$
					name = className.substring(className.lastIndexOf(".") + 1); //$NON-NLS-1$
					name += " \"" + myName + "\""; //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					XMLProperty root = this;
					while (root.getParentProperty() != null) {
						root = root.getParentProperty();
					}
					if (root instanceof XMLControlElement) {
						XMLControlElement rootControl = (XMLControlElement) root;
						name = className.substring(className.lastIndexOf(".") + 1); //$NON-NLS-1$
						name = rootControl.addNumbering(name);
					}
				}
			}
			return "" + name; //$NON-NLS-1$
		}
		// else if this has a parent, return its name
		else if (parent != null) {
			return parent.getPropertyName();
			// else return the short class name
		} else {
			return className.substring(className.lastIndexOf(".") + 1); //$NON-NLS-1$
		}
	}

	/**
	 * Gets the property class.
	 *
	 * @return the class
	 */
	@Override
	public Class<?> getPropertyClass() {
		return getObjectClass();
	}

	/**
	 * Gets the level of this property relative to the root.
	 *
	 * @return a non-negative integer
	 */
	@Override
	public int getLevel() {
		return level;
	}

	/**
	 * Gets the property content of this control.
	 *
	 * @return a list of XMLProperties
	 */
	@Override
	public List<Object> getPropertyContent() {
		return new ArrayList<Object>(props);
	}

	/**
	 * Gets the property content of this control.
	 *
	 * @return a list of XMLProperties
	 */
	@Override
	public List<XMLProperty> getPropsRaw() {
		return props;
	}

	/**
	 * Gets the named XMLControl child of this property. May return null.
	 *
	 * @param name the property name
	 * @return the XMLControl
	 */
	@Override
	public XMLControl getChildControl(String name) {
		return getChildMap().get(name);
	}

	/**
	 * Gets the XMLControl children of this property. The returned array has length
	 * for type "object" = 1, "collection" and "array" = 0+, other types = 0.
	 *
	 * @return an XMLControl array
	 */
	@Override
	public XMLControl[] getChildControls() {
		if (childControls == null) {
			ArrayList<XMLControl> list = new ArrayList<XMLControl>();
			Iterator<XMLProperty> it = props.iterator();
			while (it.hasNext()) {
				XMLProperty prop = it.next();
				if (prop.getPropertyType() == XMLProperty.TYPE_OBJECT) { //$NON-NLS-1$
					list.add((XMLControl) prop.getPropertyContent().get(0));
				}
			}
			childControls = list.toArray(new XMLControl[list.size()]);
		}
		return childControls;
	}

	/**
	 * Gets the root control.
	 *
	 * @return the root control
	 */
	public XMLControlElement getRootControl() {
		if (parent == null) {
			return this;
		}
		XMLProperty prop = parent;
		while (prop.getParentProperty() != null) {
			prop = prop.getParentProperty();
		}
		if (prop instanceof XMLControlElement) {
			return (XMLControlElement) prop;
		}
		return null;
	}

	/**
	 * Appends numbering to a specified name. Increments the number each time this
	 * is called for the same name.
	 *
	 * @param name the name
	 * @return the name with appended numbering
	 */
	public String addNumbering(String name) {
		Integer count = counts.get(name);
		if (count == null) {
			count = Integer.valueOf(0);
		}
		count = Integer.valueOf(count.intValue() + 1);
		counts.put(name, count);
		return name + " " + count.toString(); //$NON-NLS-1$
	}

	/**
	 * Returns the string xml representation.
	 *
	 * @return the string xml representation
	 */
	@Override
	public String toString() {
		StringBuffer xml = new StringBuffer(""); //$NON-NLS-1$
		// write the header if this is the top level
		if (getLevel() == 0) {
			xml.append("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>"); //$NON-NLS-1$ //$NON-NLS-2$
			if (isValid()) {
				xml.append(XML.NEW_LINE + "<!DOCTYPE object SYSTEM \"" + doctype + "\">"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		// write the opening tag
		xml.append(XML.NEW_LINE + indent(getLevel()) + "<object class=\"" + className + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		// write the version if this is the top level
		if ((version != null) && (getLevel() == 0)) {
			xml.append(" version=\"" + version + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		// write the property content and closing tag
		if (props.isEmpty()) {
			xml.append("/>"); //$NON-NLS-1$
		} else {
			xml.append(">"); //$NON-NLS-1$
			Iterator<XMLProperty> it = props.iterator();
			while (it.hasNext()) {
				xml.append(it.next().toString());
			}
			xml.append(XML.NEW_LINE + indent(getLevel()) + "</object>"); //$NON-NLS-1$
		}
		return xml.toString();
	}

	// ____________________________ static methods _________________________________

	/**
	 * Returns a list of objects of a specified class within this control.
	 *
	 * @param type the Class
	 * @return the list of objects
	 */
	public <T> List<T> getObjects(Class<T> type) {
		return getObjects(type, false);
	}

	/**
	 * Returns a list of objects of a specified class within this control.
	 *
	 * @param type       the Class
	 * @param useChooser true to allow user to choose
	 * @return the list of objects
	 */
	public <T> List<T> getObjects(Class<T> type, boolean useChooser) {
		java.util.List<XMLProperty> props;
		if (useChooser) {
			// BH never used?

			/**
			 * @j2sNative alert("XMLControlElement.getObjects with chooser called -- not
			 *            configured to be asynchronous");
			 */

			String name = type.getName();
			name = name.substring(name.lastIndexOf(".") + 1); //$NON-NLS-1$
			// select objects using an xml tree chooser
			XMLTreeChooser chooser = new XMLTreeChooser(
					ControlsRes.getString("XMLControlElement.Chooser.SelectObjectsOfClass.Title"), //$NON-NLS-1$
					ControlsRes.getString("XMLControlElement.Chooser.SelectObjectsOfClass.Label") + " " + name, null); //$NON-NLS-1$ //$NON-NLS-2$
			props = chooser.choose(this, type);
		} else {
			// select all objects of desired type using an xml tree
			XMLTree tree = new XMLTree(this);
			tree.setHighlightedClass(type);
			tree.selectHighlightedProperties();
			props = tree.getSelectedProperties();
		}

		List<T> objects = new ArrayList<T>();
		Iterator<XMLProperty> it = props.iterator();
		while (it.hasNext()) {
			XMLControl prop = (XMLControl) it.next();
			objects.add(type.cast(prop.loadObject(null)));
		}
		return objects;
	}

	/**
	 * Returns a copy of this control.
	 *
	 * @return a clone
	 */
	@Override
	public Object clone() {
		return new XMLControlElement(this);
	}

	// ____________________________ private methods
	// _________________________________

	/**
	 * Determines if this is (the child of) an array or collection item.
	 *
	 * @return true if this is an array or collection item
	 */
	private boolean isArrayOrCollectionItem() {
		XMLProperty parent = getParentProperty();
		if (parent != null) {
			parent = parent.getParentProperty();
			return ((parent != null) && ("arraycollection".indexOf(parent.getPropertyType()) >= 0)); //$NON-NLS-1$
		}
		return false;
	}

	/**
	 * Prepares this control for importing into the specified object.
	 *
	 * @param obj       the importing object
	 * @param importAll true to import all
	 * @return <code>true</code> if the data is imported
	 */
	private boolean importInto(Object obj, boolean importAll) {
		// get the list of importable properties
		XMLControl control = new XMLControlElement(obj);
		Collection<String> list = control.getPropertyNames();
		list.retainAll(this.getPropertyNamesRaw());
		// add property values
		Collection<String> names = new ArrayList<String>();
		Collection<Object> values = new ArrayList<Object>();
		for (Iterator<XMLProperty> it = props.iterator(); it.hasNext();) {
			XMLProperty prop = it.next();
			String propName = prop.getPropertyName();
			if (!list.contains(propName)) {
				continue;
			}
			names.add(propName); // keeps names in same order as values
			if (prop.getPropertyType() == XMLProperty.TYPE_OBJECT) { //$NON-NLS-1$
				values.add(prop.getPropertyClass().getSimpleName());
			} else {
				values.add(prop.getPropertyContent().get(0));
			}
		}
		if (names.isEmpty() || importAll) {
			return processImport(control, names);
		}

		// BH! This next call will return FALSE FOR JAVASCRIPT because we can't make
		// this central method asynchronous. :(

		// choose the properties to import
		// BH This one has to be synchronous and so will fail in JavaScript.

		boolean[] isOK = new boolean[1];

		ListChooser chooser = new ListChooser(ControlsRes.getString("XMLControlElement.Chooser.ImportObjects.Title"), //$NON-NLS-1$
				ControlsRes.getString("XMLControlElement.Chooser.ImportObjects.Label"), new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						if (e.getID() == ActionEvent.ACTION_PERFORMED) {
							processImport(control, names);
							isOK[0] = true;
						}
					}

				});
		chooser.choose(names, names, values, null, null, null);
		// beween these two statements in Java, the above actionPerformed will occur.
		// but JavaScript is going to return prior to that.
		return isOK[0];
	}

	private boolean processImport(XMLControl control, Collection<String> names) {
		// names list now contains property names to keep
		Iterator<XMLProperty> it = props.iterator();
		while (it.hasNext()) {
			String name = it.next().getPropertyName();
			if (!names.contains(name)) {
				it.remove();
				propNames.remove(name);
				getPropMap().remove(name);
			}
		}
		// add object properties not in the names list to this control
		Iterator<String> it2 = control.getPropertyNamesRaw().iterator();
		while (it2.hasNext()) {
			String name = it2.next();
			if (names.contains(name)) {
				continue;
			}
			switch (control.getPropertyType(name)) {
			case XMLProperty.TYPE_INT: //$NON-NLS-1$
				setValue(name, control.getInt(name));
				break;
			case XMLProperty.TYPE_DOUBLE: //$NON-NLS-1$
				setValue(name, control.getDouble(name));
				break;
			case XMLProperty.TYPE_BOOLEAN: //$NON-NLS-1$
				setValue(name, control.getBoolean(name));
				break;
			case XMLProperty.TYPE_STRING: //$NON-NLS-1$
				setValue(name, control.getString(name));
				break;
			default:
				setValue(name, control.getObject(name));
				break;
			}
		}
		return true;
	}

	/**
	 * Sets an XML property.
	 *
	 * @param name                       the name
	 * @param type                       the type
	 * @param value                      the value
	 * @param writeNullFinalArrayElement true to write a final null array element
	 *                                   (if needed)
	 */
	private void setXMLProperty(String name, int type, Object value, boolean writeNullFinalArrayElement) {
		// remove any previous property with the same name
		XMLPropertyElement prop = new XMLPropertyElement(this, name, type, value, writeNullFinalArrayElement);
		
		if (propNames.contains(name)) {
			Iterator<XMLProperty> it = props.iterator();
			for (int i = 0; it.hasNext(); i++) {
				XMLProperty p = it.next();
				if (p.getPropertyName().equals(name)) {
					it.remove();
					setProperty(name, prop, i);
					return;
				}
			}
		} else {
			propNames.add(name);
		}		
		setProperty(name, prop, -1);
	}

	private void setProperty(String name, XMLProperty prop, int i) {
		if (i < 0)
			props.add(prop);
		else
			props.add(i, prop);
		getPropMap().put(name, prop);
		if (prop.getPropertyType() == XMLProperty.TYPE_OBJECT) { //$NON-NLS-1$
			getChildMap().put(name, ((XMLControl) prop.getPropertyContent().get(0)));
			childControls = null;
		}

	}

	private Map<String, XMLProperty> getPropMap() {
		if (propMap == null) {
			propMap = new HashMap<String, XMLProperty>();
		}
		return propMap;
	}

	/**
	 * Gets a named property. May return null.
	 *
	 * @param name the name
	 * @return the XMLProperty
	 */
	private XMLProperty getXMLProperty(String name) {
		return (name == null ? null : getPropMap().get(name));
	}


	/**
	 * Reads this control from the current input, optionally require className.
	 * 
	 * @param in
	 * @param className
	 */
	private boolean readInput(BufferedReader in, String className) {
		if (in == null) {
			readFailed = true;
			return false;
		}
		//long t0 = Performance.now(0);
		this.input = in;
		readFailed = false;
		try {
			// get document root opening tag line
			String openingTag = input.readLine();
			int count = 0;
			while (openingTag != null && openingTag.indexOf("<object class=") < 0) { //$NON-NLS-1$
				count++;
				if (count > 9) {
					// stop reading at 10 lines
					readFailed = true;
					return false;
				}
				openingTag = input.readLine();
			}
			// read this element from the root
			if (openingTag != null) {
				// get version, if any
				version = XML.getAttr(openingTag, "version", version);
				readObject(this, openingTag, className);
			} else {
				readFailed = true;
				return false;
			}
		} catch (Exception ex) {
			readFailed = true;
			OSPLog.warning("Failed to read xml: " + ex.getMessage()); //$NON-NLS-1$
			return false;
		}
		// if object class is Cryptic, decrypt and inspect
		if (Cryptic.class.equals(getObjectClass())) {
			Cryptic cryptic = (Cryptic) loadObject(null);
			// get the decrypted xml
			String xml = cryptic.decrypt();
			// return if decrypted xml is not readable by a test control
			XMLControl test = new XMLControlElement(xml);
			if (test.failedToRead()) {
				return false;
			}
			// keep current password for possible verification needs
			String pass = password;
			// get the password from the test control
			password = test.getString("xml_password"); //$NON-NLS-1$
			// return if decrypt policy is NEVER or unverified PASSWORD
			switch (decryptPolicy) {
			case NEVER_DECRYPT:
				return false;
			case PASSWORD_DECRYPT:
				if ((password != null) && !password.equals("") && //$NON-NLS-1$
						!password.equals(pass)) {
					if (!Password.verify(password, null)) {
						readFailed = true; // BH Yes??? was not here
						return false;
					}
				}
			}
			// otherwise read the decrypted xml into this control
			clearValues();
			object = null;
			className = Object.class.getName();
			theClass = null;
			return readXML(xml, null);
		}
		//OSPLog.debug("!!! " + Performance.now(t0) + " XMLControlElement.readData " + className);
		return !readFailed;
	}


	//	/**
//	 * Checks to see if the input is for the specified class.
//	 */
//	private boolean isInputForClass(Class<?> type) {
//		try {
//			// get document root tag
//			String xml = input.readLine();
//			if (!xml.startsWith("<"))
//				return false;
//			while ((xml != null) && (xml.indexOf("<object") == -1)) { //$NON-NLS-1$
//				xml = input.readLine();
//			}
//			// check class name
//			if (xml != null) {
//				xml = xml.substring(xml.indexOf("class=") + 7); //$NON-NLS-1$
//				String className = xml.substring(0, xml.indexOf("\"")); //$NON-NLS-1$
//				if (className.equals(type.getName())) {
//					return true;
//				}
//			}
//		} catch (Exception ex) {
//			ex.printStackTrace();
//		}
//		return false;
//	}

	/**
	 * Reads the current input into an XMLcontrolElement.
	 *
	 * @param control the control to load
	 * @param xml     the xml opening tag line
	 * @param requiredType required class name or null
	 * @return the loaded element
	 * @throws IOException
	 */
	private XMLControlElement readObject(XMLControlElement control, String xml, String requiredType) throws IOException {
		control.clearValues();
		String className = getClassName(xml);
		if (requiredType != null && !className.equals(requiredType)) {
			readFailed = true;
			return null;
		}
		control.className = className;
		// look for closing object tag on same line
		if (xml.indexOf("/>") != -1) { //$NON-NLS-1$
			input.readLine();
			return control;
		}
		// read and process input lines
		XMLProperty prop = control;
		xml = input.readLine();
		while (xml != null) {
			// closing object tag
			if (xml.indexOf("</object>") >= 0) { //$NON-NLS-1$
				input.readLine();
				return control;
			}
			// opening property tag
			else if (xml.indexOf("<property") >= 0) { //$NON-NLS-1$
				control.addProperty(readProperty(new XMLPropertyElement(prop), xml));
			}
			xml = input.readLine();
		}
		return control;
	}

	private void addProperty(XMLProperty child) {
		String name = child.getPropertyName();
		propNames.add(name);
		setProperty(name, child, -1);
	}

	private Map<String, XMLControl> getChildMap() {
		if (childMap == null) {
//			XMLControl[] children = getChildControls();
			childMap = new HashMap<String, XMLControl>();
//			for (int i = 0; i < children.length; i++) {
//				childMap.put(children[i].getPropertyName(), children[i]);
//			}
		}
		return childMap;
	}

	/**
	 * Reads the current input into a property element.
	 *
	 * @param prop the property element to load
	 * @param xml  the xml opening tag line
	 * @return the loaded property element
	 * @throws IOException
	 */
	private XMLPropertyElement readProperty(XMLPropertyElement prop, String xml) throws IOException {
		// set property name
		prop.name = XML.getAttr(xml, "name", null); //$NON-NLS-1$
		// set property type
		prop.type = XMLProperty.getTypeCode(XML.getAttr(xml, "type", null)); //$NON-NLS-1$
		// set property content and className
		switch (prop.type) {
		case XMLProperty.TYPE_ARRAY: //$NON-NLS-1$
		case XMLProperty.TYPE_COLLECTION: //$NON-NLS-1$
			prop.className = getClassName(xml);
			if (xml.indexOf("/>") >= 0) { // property closing tag on same line //$NON-NLS-1$
				return prop;
			}
			xml = input.readLine();
			while (xml.indexOf("<property") >= 0) { //$NON-NLS-1$
				prop.content.add(readProperty(new XMLPropertyElement(prop), xml));
				xml = input.readLine();
			}
			break;
		case XMLProperty.TYPE_OBJECT: //$NON-NLS-1$
			// add XMLControl unless value is null
			if (xml.indexOf(">null</property") < 0) { //$NON-NLS-1$
				XMLControlElement control = readObject(new XMLControlElement(prop), input.readLine(), null);
				prop.content.add(control);
				prop.className = control.className;
			}
			break;
		case XMLProperty.TYPE_STRING:
			int pt = xml.indexOf(XML.CDATA_PRE); 
			if (pt >= 0) {
				String s = xml.substring(pt + XML.CDATA_PRE_LEN);
				while ((pt = s.indexOf(XML.CDATA_POST_PROP)) < 0) { // look for end tag //$NON-NLS-1$
					s += XML.NEW_LINE + input.readLine();
				}
				prop.content.add(s.substring(0, pt));
				break;
			}
			// fall through
		default:			
			// int, double, boolean or string types
			// BH 2020.11.21 was 
			//if (xml.indexOf(XML.CDATA_PRE) != -1) {
				//String s = xml.substring(xml.indexOf(XML.CDATA_PRE));
			// but that includes CDATA_PRE in the contents		
				String s = xml.substring(xml.indexOf(">") + 1); //$NON-NLS-1$
				while ((pt = s.indexOf("</property>")) < 0) { // look for end tag //$NON-NLS-1$
					s += XML.NEW_LINE + input.readLine();
				}
				prop.content.add(s.substring(0, pt));
			break;
		}
		return prop;
	}

	/**
	 * Returns a space for indentation.
	 *
	 * @param level the indent level
	 * @return the space
	 */
	private String indent(int level) {
		String space = ""; //$NON-NLS-1$
		for (int i = 0; i < XML.INDENT * level; i++) {
			space += " "; //$NON-NLS-1$
		}
		return space;
	}

	/**
	 * Returns the object value of the specified property. May return null.
	 *
	 * @param prop the property
	 * @return the array
	 */
	private Object objectValue(XMLProperty prop) {
		if (prop.getPropertyType() != XMLProperty.TYPE_OBJECT) { //$NON-NLS-1$
			return null;
		}
		if (prop.getPropertyContent().isEmpty())
			return null;
		Object content = prop.getPropertyContent().get(0);
		if (!(content instanceof XMLControl))
			return null;
		XMLControl control = (XMLControl) content;
		return control.loadObject(null);
	}

	/**
	 * Returns the double value of the specified property.
	 *
	 * @param prop the property
	 * @return the value
	 */
	private double doubleValue(XMLProperty prop) {
		if (prop.getPropertyType() != XMLProperty.TYPE_DOUBLE) { //$NON-NLS-1$
			return Double.NaN;
		}
		return Double.parseDouble((String) prop.getPropertyContent().get(0));
	}

	/**
	 * Returns the double value of the specified property.
	 *
	 * @param prop the property
	 * @return the value
	 */
	private int intValue(XMLProperty prop) {
		if (prop.getPropertyType() != XMLProperty.TYPE_INT) { //$NON-NLS-1$
			return Integer.MIN_VALUE;
		}
		return Integer.parseInt((String) prop.getPropertyContent().get(0));
	}

	/**
	 * Returns the boolean value of the specified property.
	 *
	 * @param prop the property
	 * @return the value
	 */
	private boolean booleanValue(XMLProperty prop) {
		return prop.getPropertyContent().get(0).equals("true"); //$NON-NLS-1$
	}

	/**
	 * Returns the string value of the specified property.
	 *
	 * @param prop the property
	 * @return the value
	 */
	private String stringValue(XMLProperty prop) {
		return (prop.getPropertyType() == XMLProperty.TYPE_STRING ? //$NON-NLS-1$
				XML.removeCDATA((String) prop.getPropertyContent().get(0)) : null);
	}

	/**
	 * Returns the array value of the specified property. May return null.
	 *
	 * @param prop the property
	 * @return the array
	 */
	private Object arrayValue(XMLProperty prop) {
		if (prop.getPropertyType() != XMLProperty.TYPE_ARRAY) { //$NON-NLS-1$
			return null;
		}
		Class<?> componentType = prop.getPropertyClass().getComponentType();
		List<?> content = prop.getPropertyContent();
		// if no content, return a zero-length array
		if (content.isEmpty()) {
			return Array.newInstance(componentType, 0);
		}
		// determine the format from the first item
		XMLProperty first = (XMLProperty) content.get(0);
		if (first.getPropertyName().equals("array")) { //$NON-NLS-1$
			// create the array from an array string
			Object obj = first.getPropertyContent().get(0);
			if (obj instanceof String) {
				return arrayValue((String) obj, componentType);
			}
			return null;
		}
		// create the array from a list of properties
		// determine the length of the array
		XMLProperty last = (XMLProperty) content.get(content.size() - 1);
		String index = last.getPropertyName();
		int n = Integer.parseInt(index.substring(1, index.indexOf("]"))); //$NON-NLS-1$
		// create the array
		Object array = Array.newInstance(componentType, n + 1);
		// populate the array
		Iterator<?> it = content.iterator();
		while (it.hasNext()) {
			XMLProperty next = (XMLProperty) it.next();
			index = next.getPropertyName();
			n = Integer.parseInt(index.substring(1, index.indexOf("]"))); //$NON-NLS-1$
			switch (next.getPropertyType()) {
			case XMLProperty.TYPE_OBJECT:
				Array.set(array, n, objectValue(next));
				break;
			case XMLProperty.TYPE_INT:
				int val = intValue(next);
				if (Object.class.isAssignableFrom(componentType)) {
					Array.set(array, n, Integer.valueOf(val));
				} else {
					Array.setInt(array, n, val);
				}
				break;
			case XMLProperty.TYPE_DOUBLE:
				double d = doubleValue(next);
				if (Object.class.isAssignableFrom(componentType)) {
					Array.set(array, n, new Double(d));
				} else {
					Array.setDouble(array, n, d);
				}
				break;
			case XMLProperty.TYPE_BOOLEAN:
				boolean b = booleanValue(next);
				if (Object.class.isAssignableFrom(componentType)) {
					Array.set(array, n, Boolean.valueOf(b));
				} else {
					Array.setBoolean(array, n, b);
				}
				break;
			case XMLProperty.TYPE_STRING:
				Array.set(array, n, stringValue(next));
				break;
			case XMLProperty.TYPE_ARRAY:
				Array.set(array, n, arrayValue(next));
				break;
			case XMLProperty.TYPE_COLLECTION:
				Array.set(array, n, collectionValue(next));
				break;
			}
		}
		return array;
	}

	/**
	 * Returns the array value of the specified array string. May return null. An
	 * array string must start and end with braces and contain only int, double and
	 * boolean types.
	 *
	 * @param arrayString   the array string
	 * @param componentType the component type of the array
	 * @return the array
	 */
	private Object arrayValue(String arrayString, Class<?> componentType) {
		if (!(arrayString.startsWith("{") && arrayString.endsWith("}"))) { //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
		// trim the outer braces
		String trimmed = arrayString.substring(1, arrayString.length() - 1);
		if (componentType.isArray()) {
			// create and collect the array elements from substrings
			ArrayList<Object> list = new ArrayList<Object>();
			ArrayList<Boolean> isNull = new ArrayList<Boolean>();
			Class<?> arrayType = componentType.getComponentType();

			int i = trimmed.indexOf("{"); //$NON-NLS-1$
			int j = indexOfClosingBrace(trimmed, i);
			int k = trimmed.indexOf(","); //$NON-NLS-1$
			while (j > 0) {
//        if (k<i) { // first comma is before opening brace
				if (k > -1 && k < i) { // first comma is before opening brace
					isNull.add(true);
					trimmed = trimmed.substring(k + 1);
				} else {
					String nextArray = trimmed.substring(i, j + 1);
					Object obj = arrayValue(nextArray, arrayType);
					list.add(obj);
					isNull.add(false);
					trimmed = trimmed.substring(j + 1);
					if (trimmed.startsWith(",")) // comma following closing brace //$NON-NLS-1$
						trimmed = trimmed.substring(1);
				}
				i = trimmed.indexOf("{"); //$NON-NLS-1$
//        j = trimmed.indexOf("}"); //$NON-NLS-1$
				j = indexOfClosingBrace(trimmed, i);
				k = trimmed.indexOf(","); //$NON-NLS-1$
			}
			// look for trailing null elements
			while (k > -1) {
				isNull.add(true);
				trimmed = trimmed.substring(k + 1);
				k = trimmed.indexOf(","); //$NON-NLS-1$
			}
			if (trimmed.length() > 0) { // last element (after final comma) is null
				isNull.add(true);
			}
			// create the array
			Object array = Array.newInstance(componentType, isNull.size());
			// populate the array
			Boolean[] hasNoElement = isNull.toArray(new Boolean[0]);
			Iterator<Object> it = list.iterator();
			for (int n = 0; n < hasNoElement.length; n++) {
				if (!hasNoElement[n] && it.hasNext()) {
					Object obj = it.next();
					Array.set(array, n, obj);
				}
			}
			return array;
		}
		// collect element substrings separated by commas
		ArrayList<String> list = new ArrayList<String>();
		while (!trimmed.equals("")) { //$NON-NLS-1$
			int i = trimmed.indexOf(","); //$NON-NLS-1$
			if (i > -1) {
				list.add(trimmed.substring(0, i));
				trimmed = trimmed.substring(i + 1);
			} else {
				list.add(trimmed);
				break;
			}
		}
		// create the array
		Object array = Array.newInstance(componentType, list.size());
		// populate the array
		Iterator<String> it = list.iterator();
		int n = 0;
		while (it.hasNext()) {
			if (componentType == Integer.TYPE) {
				int i = Integer.parseInt(it.next());
				Array.setInt(array, n++, i);
			} else if (componentType == Double.TYPE) {
				double x = Double.parseDouble(it.next());
				Array.setDouble(array, n++, x);
			} else if (componentType == Boolean.TYPE) {
				boolean bool = it.next().equals("true"); //$NON-NLS-1$
				Array.setBoolean(array, n++, bool);
			}
		}
		return array;
	}

	/**
	 * Returns the collection value of the specified property. May return null.
	 *
	 * @param prop the property
	 * @return the array
	 */
	@SuppressWarnings("unchecked")
	private Object collectionValue(XMLProperty prop) {
		if (prop.getPropertyType() != XMLProperty.TYPE_COLLECTION) { //$NON-NLS-1$
			return null;
		}
		Class<?> classType = prop.getPropertyClass();
		try {
			// create the collection
			Collection<Object> c = (Collection<Object>) classType.newInstance();
			List<Object> content = prop.getPropertyContent();
			// populate the array
			Iterator<Object> it = content.iterator();
			while (it.hasNext()) {
				XMLProperty next = (XMLProperty) it.next();
				switch (next.getPropertyType()) {
				case XMLProperty.TYPE_OBJECT:
					c.add(objectValue(next));
					break;
				case XMLProperty.TYPE_STRING: //$NON-NLS-1$
					c.add(stringValue(next));
					break;
				case XMLProperty.TYPE_ARRAY: //$NON-NLS-1$
					c.add(arrayValue(next));
					break;
				case XMLProperty.TYPE_COLLECTION: //$NON-NLS-1$
					c.add(collectionValue(next));
					break;
				}
			}
			return c;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns the index of the closing brace corresponding to the opening brace at
	 * the given index in an array string.
	 *
	 * @param arrayString         the array string
	 * @param indexOfOpeningBrace the index of the opening brace
	 * @return the index of the closing brace
	 */
	private int indexOfClosingBrace(String arrayString, int indexOfOpeningBrace) {
		int pointer = indexOfOpeningBrace + 1;
		int n = 1; // count up/down for opening/closing braces
		int opening = arrayString.indexOf("{", pointer); //$NON-NLS-1$
		int closing = arrayString.indexOf("}", pointer); //$NON-NLS-1$
		while (n > 0) {
			if (opening > -1 && opening < closing) {
				n++;
				pointer = opening + 1;
				opening = arrayString.indexOf("{", pointer); //$NON-NLS-1$
			} else if (closing > -1) {
				n--;
				pointer = closing + 1;
				closing = arrayString.indexOf("}", pointer); //$NON-NLS-1$
			} else
				return -1;
		}
		return pointer - 1;
	}

	public static String getClassName(String xml) {
		// set class name
		try {
			String className = XML.getAttr(xml, "class", "");
			int i = className.lastIndexOf("."); //$NON-NLS-1$
			if (i >= 0) {
				String packageName = className.substring(0, i);
				if (packageName.endsWith("org.opensourcephysics.media")) { //$NON-NLS-1$
					className = packageName + ".core" + className.substring(i); //$NON-NLS-1$
				}
			}
			return className;
		} catch (Exception e) {
			return "";
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
 * Copyright (c) 2017 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
