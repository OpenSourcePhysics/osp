/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.controls;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.XMLFormatter;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import org.opensourcephysics.display.GUIUtils;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.FontSizer;

/**
 * This is a viewable file-based message log for a java package. It displays log
 * records in a text pane and saves them in a temp file.
 *
 * @author Douglas Brown
 * @author Wolfgang Christian
 * @version 1.0
 */
@SuppressWarnings("serial")
public class OSPLog  {

	class OSPFrame extends JFrame {
		public OSPFrame(String string) {
			super(string);
		}
		
		@Override
		protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
			super.firePropertyChange(propertyName, oldValue, newValue);
		}
		
		@Override
		public void setVisible(boolean vis) {
			FontSizer.setFonts(this, FontSizer.getLevel());
			super.setVisible(vis);
		}

	}
	
	OSPFrame frame;
	
	/**
	 * Set true to enable OSPLog.notify   (finalization)
	 */
	private static final boolean doNotify = false;
	

	/**
	 * set to false to use standard System.out in Eclipse testing
	 */
	private static final boolean divertSysOut = false;
	
	
	static int nLog;

	public final static PrintStream realSysout = System.out;

	private StringBuffer logBuffer = new StringBuffer();

	public static class LoggerPrintStream extends PrintStream {

		
		protected boolean isErr;

		public LoggerPrintStream(LoggerOutputStream out, boolean isErr) {
			super(out);
			this.isErr = isErr;
		}

		@Override
		public void println(String msg) {
			((LoggerOutputStream) out).println(msg);

		}

		@Override
		public void print(String msg) {
			((LoggerOutputStream) out).print(msg);
		}

	}

	List<LogRecord> logRecords = new ArrayList<>();

	/**
	 * A logger that can be used by any OSP program to log and show messages.
	 */
	private static OSPLog OSPLOG;
	private static JFileChooser chooser;
	protected static Style black, red, blue, green, magenta, gray;
	protected static final Color DARK_GREEN = new Color(0, 128, 0), DARK_BLUE = new Color(0, 0, 128),
			DARK_RED = new Color(128, 0, 0);
	public static final Level[] levels = new Level[] { Level.OFF, Level.SEVERE, Level.WARNING, Level.INFO,
			ConsoleLevel.ERR_CONSOLE, ConsoleLevel.OUT_CONSOLE, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST,
			Level.ALL };


	private static Level defaultLevel = ConsoleLevel.OUT_CONSOLE;
	protected static boolean logConsole = true;

	// instance fields
	private Logger logger;
	private Handler fileHandler;
	private OSPLogHandler logHandler;
	private JTextPane logPane;
	private String logFileName;
	private String tempFileName;
	private JPanel logPanel;
	private JPopupMenu popup;
	private ButtonGroup popupGroup, menubarGroup;
	private String pkgName;
	private String bundleName;
	private JMenuItem logToFileItem;
	private boolean hasPermission = true;

	protected int myPopupFontLevel;

	private static LogRecord[] messageStorage = new LogRecord[128];
	private static int messageIndex = 0;
	static String eol = "\n"; //$NON-NLS-1$
	static String logdir = "."; //$NON-NLS-1$
	static String slash = "/"; //$NON-NLS-1$

	private static boolean useFrame = true;

	private static boolean useMessageFrame() {
		return (OSPRuntime.appletMode || OSPRuntime.isApplet);
	}

	static {
		try { // system properties may not be readable in some environments
			eol = System.getProperty("line.separator", eol); //$NON-NLS-1$
			logdir = System.getProperty("user.dir", logdir); //$NON-NLS-1$
			slash = System.getProperty("file.separator", "/"); //$NON-NLS-1$//$NON-NLS-2$
		} catch (Exception ex) {
			/** empty block */
		}
	}

	/**
	 * Gets the OSPLog that can be shared by multiple OSP packages.
	 *
	 * @return the shared OSPLog
	 */
	public static OSPLog getOSPLog() {
		if (OSPLOG == null && !useMessageFrame()) { // cannot redirect applet streams
			OSPLOG = new OSPLog("org.opensourcephysics", null); //$NON-NLS-1$
		}
		return OSPLOG;
	}

	public static void getOSPLog(boolean useFrame) {
		OSPLog.useFrame  = useFrame;
		getOSPLog();
	}

	/**
	 * Sets the directory where the log file will be saved if logging is enabled.
	 * 
	 * @param dir String
	 */
	public void setLogDir(String dir) {
		logdir = dir;
	}

	/**
	 * Gets the directory where the log file will be saved if logging is enabled.
	 * 
	 * @param dir String
	 */
	public String getLogDir() {
		return logdir;
	}

	/**
	 * Determines if the shared log is visible.
	 *
	 * @return true if visible
	 */
	public static boolean isLogVisible() {
		if (useMessageFrame() && MessageFrame.APPLET_MESSAGEFRAME != null) {
			return MessageFrame.APPLET_MESSAGEFRAME.isVisible();
		} else if (OSPLOG != null) {
			return OSPLOG.isVisible();
		}
		return false;
	}

	/**
	 * Sets the visibility of this log.
	 *
	 * @param true to set visible
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			Point p = new JFrame().getLocation(); // default location of new frame or dialog
			JFrame log = OSPLog.getFrame();
			if (log.getLocation().x == p.x && log.getLocation().y == p.y) {
				// center on screen AFTER setting visible so GUI will be complete
				SwingUtilities.invokeLater(() -> {
					// center on screen
					Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
					int x = (dim.width - log.getBounds().width) / 2;
					int y = (dim.height - log.getBounds().height) / 2;
					log.setLocation(x, y);
				});
			}
		}
		
		try {
			if (visible) {
				createGUI();
				if (logPane == null) {
					logPane = getTextPane();
					postAllRecords();
					FontSizer.setFonts(this, FontSizer.getLevel());
					frame.pack();
				}
			}
			if (useMessageFrame()) {
				org.opensourcephysics.controls.MessageFrame.showLog(visible);
			} else {
				frame.setVisible(visible);
			}
		} catch (Throwable t) {
			realSysout.println("OSPLog???" + t);

		}
	}

	private void postAllRecords() {
		for (int i = 0, n = logRecords.size(); i < n; i++) {
			logHandler.publish(logRecords.get(i));
		}
		logRecords.clear();
	}

	/**
	 * Determines if the log is visible.
	 *
	 * @return true if visible
	 */
	public boolean isVisible() {
		if (useMessageFrame()) {
			return org.opensourcephysics.controls.MessageFrame.isLogVisible();
		}
		return frame.isVisible();
	}

	/**
	 * Shows the log when it is invoked from the event queue.
	 */
	public static JFrame showLog() {
		if (useMessageFrame()) {
			return org.opensourcephysics.controls.MessageFrame.showLog(true);
		}
		getOSPLog().setVisible(true);
		Logger logger = OSPLOG.getLogger();
		for (int i = 0, n = messageStorage.length; i < n; i++) {
			LogRecord record = messageStorage[(i + messageIndex) % n];
			if (record != null) {
				logger.log(record);
			}
		}
		messageIndex = 0;
		return getOSPLog().frame;
	}

	/**
	 * Shows the log.
	 */
	public static void showLogInvokeLater() {
		Runnable doLater = new Runnable() {
			@Override
			public void run() {
				showLog();
			}

		};
		java.awt.EventQueue.invokeLater(doLater);
	}

	/**
	 * Gets the logger level value.
	 * 
	 * @return the current level value
	 */
	public static int getLevelValue() {
		if (useMessageFrame()) {
			return org.opensourcephysics.controls.MessageFrame.getLevelValue();
		}
		try {
			Level level = getOSPLog().getLogger().getLevel();
			if (level != null)
				return level.intValue();
		} catch (Exception ex) { // throws security exception if the caller does not have
									// LoggingPermission("control").
		}
		return -1;
	}

	/**
	 * Sets the logger level.
	 *
	 * @param level the Level
	 */
	public static void setLevel(Level level) {
		if (useMessageFrame()) {
			org.opensourcephysics.controls.MessageFrame.setLevel(level);
		} else {
			setLevel(level, getOSPLog());
		}
	}

	private static void setLevel(Level level, OSPLog ospLog) {
		try {
			ospLog.getLogger().setLevel(level);
		} catch (Exception ex) { // throws security exception if the caller does not have
									// LoggingPermission("control").
			// keep the current level
		}
		// refresh the level menus if the menubar exists
		if ((ospLog == null) || (ospLog.menubarGroup == null)) {
			return;
		}
		for (int i = 0; i < 2; i++) {
			Enumeration<AbstractButton> e = ospLog.menubarGroup.getElements();
			if (i == 1) {
				e = ospLog.popupGroup.getElements();
			}
			while (e.hasMoreElements()) {
				JMenuItem item = (JMenuItem) e.nextElement();
				if (ospLog.getLogger().getLevel().toString().equals(item.getActionCommand())) {
					item.setSelected(true);
					break;
				}
			}
		}

	}

	/**
	 * Returns the Level with the specified name, or null if none.
	 *
	 * @param level the Level
	 */
	public static Level parseLevel(String level) {
		for (int i = 0; i < levels.length; i++) {
			if (levels[i].getName().equals(level)) {
				return levels[i];
			}
		}
		return null;
	}

	/**
	 * Logs a severe error message.
	 *
	 * @param msg the message
	 */
	public static void severe(String msg) {
		if (useMessageFrame()) {
			org.opensourcephysics.controls.MessageFrame.severe(msg);
		} else {
			log(Level.SEVERE, msg);
		}
	}

	/**
	 * Logs a warning message.
	 *
	 * @param msg the message
	 */
	public static void warning(String msg) {
		if (useMessageFrame()) {
			org.opensourcephysics.controls.MessageFrame.warning(msg);
		} else {
			log(Level.WARNING, msg);
		}
	}

	/**
	 * Logs an information message.
	 *
	 * @param msg the message
	 */
	public static void info(String msg) {
		if (useMessageFrame()) {
			org.opensourcephysics.controls.MessageFrame.info(msg);
		} else {
			log(Level.INFO, msg);
		}
	}

	/**
	 * Logs a configuration message.
	 *
	 * @param msg the message
	 */
	public static void config(String msg) {
		if (useMessageFrame()) {
			org.opensourcephysics.controls.MessageFrame.config(msg);
		} else {
			log(Level.CONFIG, msg);
		}
	}

	/**
	 * Logs a fine debugging message.
	 *
	 * @param msg the message
	 */
	public static void fine(String msg) {
		if (useMessageFrame()) {
			org.opensourcephysics.controls.MessageFrame.fine(msg);
		} else {
			log(Level.FINE, msg);
		}
	}

	/**
	 * Clears the Log.
	 *
	 * @param msg the message
	 */
	public static void clearLog() {
		messageIndex = 0;
		if (useMessageFrame()) {
			org.opensourcephysics.controls.MessageFrame.clear();
		} else {
			OSPLOG.clear();
		}
	}

	/**
	 * Logs a finer debugging message.
	 *
	 * @param msg the message
	 */
	public static void finer(String msg) {
		System.gc();
		System.out.println("OSPLog.finer " + msg);
		if (useMessageFrame()) {
			org.opensourcephysics.controls.MessageFrame.finer(msg);
		} else {
			log(Level.FINER, msg);
		}
	}

	/**
	 * Logs a finest debugging message.
	 *
	 * @param msg the message
	 */
	public static void finest(String msg) {
		if (useMessageFrame()) {
			org.opensourcephysics.controls.MessageFrame.finest(msg);
		} else {
			log(Level.FINEST, msg);
		}
	}

	/**
	 * Sets whether console messages are logged.
	 *
	 * @param log true to log console messages
	 */
	public static void setConsoleMessagesLogged(boolean log) {
		logConsole = log;
	}

	/**
	 * Gets whether console messages are logged.
	 *
	 * @return true if console messages are logged
	 */
	public static boolean isConsoleMessagesLogged() {
		return logConsole;
	}

	/**
	 * Constructs an OSPLog for a specified package.
	 *
	 * @param pkg the package
	 */
	public OSPLog(Package pkg) {
		this(pkg.getName(), null);
	}

	/**
	 * Constructs an OSPLog for a specified package and resource bundle.
	 *
	 * @param pkg                the package
	 * @param resourceBundleName the name of the resource bundle
	 */
	public OSPLog(Package pkg, String resourceBundleName) {
		this(pkg.getName(), resourceBundleName);
	}

	/**
	 * Constructs an OSPLog for a specified class.
	 *
	 * @param type the class
	 */
	public OSPLog(Class<?> type) {
		this(type, null);
	}

	/**
	 * Constructs an OSPLog for a specified class and resource bundle.
	 *
	 * @param type               the class
	 * @param resourceBundleName the name of the resource bundle
	 */
	public OSPLog(Class<?> type, String resourceBundleName) {
		this(type.getPackage().getName(), resourceBundleName);
	}

	/**
	 * Gets the log panel so it can be displayed in a dialog or other container.
	 *
	 * @return a JPanel containing the log
	 */
	public JPanel getLogPanel() {
		createGUI();
		return logPanel;
	}

	/**
	 * Clears the log.
	 */
	public void clear() {
		if (logPane != null)
			logPane.setText(null);
		logBuffer.setLength(0);
	}

	/**
	 * Saves the log to a text file specified by name.
	 *
	 * @param fileName the file name
	 * @return the name of the file
	 */
	public String saveLog(String fileName) {
		if ((fileName == null) || fileName.trim().equals("")) { //$NON-NLS-1$
			return saveLogAs();
		}
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
			out.write(getText());
			out.flush();
			out.close();
			return fileName;
		} catch (IOException ex) {
			return null;
		}
	}

	/**
	 * Allows getting the text from the logBuffer
	 * 
	 * @return text only, no formatting
	 * 
	 */
	private String getText() {
		return logPane == null ? logBuffer.toString() : logPane.getText();
	}

	/**
	 * Saves a log to a text file selected with a chooser.
	 *
	 * @return the name of the file
	 */
	public String saveLogAs() {
		int result = getChooser().showSaveDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
			File file = getChooser().getSelectedFile();
			// check to see if file already exists
			if (file.exists()) {
				int selected = JOptionPane.showConfirmDialog(frame,
						ControlsRes.getString("OSPLog.ReplaceExisting_dialog_message") + file.getName() //$NON-NLS-1$
								+ ControlsRes.getString("OSPLog.question_mark"), //$NON-NLS-1$
						ControlsRes.getString("OSPLog.ReplaceFile_dialog_title"), //$NON-NLS-1$
						JOptionPane.YES_NO_CANCEL_OPTION);
				if (selected != JOptionPane.YES_OPTION) {
					return null;
				}
			}
			String fileName = XML.getRelativePath(file.getAbsolutePath());
			return saveLog(fileName);
		}
		return null;
	}

	/**
	 * Saves the xml-formatted log records to a file specified by name.
	 *
	 * @param fileName the file name
	 * @return the name of the file
	 */
	public String saveXML(String fileName) {
		if (useMessageFrame()) {
			logger.log(Level.FINE, "Cannot save XML file when running as an applet."); //$NON-NLS-1$
			return null; // cannot log to file in applet mode
		}
		if ((fileName == null) || fileName.trim().equals("")) { //$NON-NLS-1$
			return saveXMLAs();
		}
		// open temp file and get xml string
		String xml = read(tempFileName);
		// add closing tag to xml
		Handler fileHandler = getFileHandler();
		String tail = fileHandler.getFormatter().getTail(fileHandler);
		xml = xml + tail;
		// write the xml
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
			out.write(xml);
			out.flush();
			out.close();
			return fileName;
		} catch (IOException ex) {
			return null;
		}
	}

	/**
	 * Saves the xml-formatted log records to a file selected with a chooser.
	 *
	 * @return the name of the file
	 */
	public String saveXMLAs() {
		int result = getChooser().showSaveDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
			File file = getChooser().getSelectedFile();
			// check to see if file already exists
			if (file.exists()) {
				int selected = JOptionPane.showConfirmDialog(frame,
						ControlsRes.getString("OSPLog.ReplaceExisting_dialog_message") + file.getName() //$NON-NLS-1$
								+ ControlsRes.getString("OSPLog.question_mark"), //$NON-NLS-1$
						ControlsRes.getString("OSPLog.ReplaceFile_dialog_title"), //$NON-NLS-1$
						JOptionPane.YES_NO_CANCEL_OPTION);
				if (selected != JOptionPane.YES_OPTION) {
					return null;
				}
			}
			logFileName = XML.getRelativePath(file.getAbsolutePath());
			return saveXML(logFileName);
		}
		return null;
	}

	/**
	 * Opens a text file selected with a chooser and writes the contents to the log.
	 */
	public void open() {
		OSPRuntime.getChooser().showOpenDialog(null, new Runnable() {

			@Override
			public void run() {
				File file = OSPRuntime.getChooser().getSelectedFile();
				String fileName = XML.getRelativePath(file.getAbsolutePath());
				fileName = XML.getRelativePath(fileName);
				open(fileName);
			}

		}, null);
	}

	/**
	 * Opens a text file specified by name and writes the contents to the log.
	 *
	 * @param fileName the file name
	 * @return the file name
	 */
	public void open(String fileName) {
		String data = read(fileName);
		logBuffer.setLength(0);
		logBuffer.append(data);
		if (logPane != null) {
			logPane.setText(data);
		}
	}

	/**
	 * Gets the logger.
	 *
	 * @return the logger
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Enables logging to a file.
	 *
	 * @param enable true to log to a file
	 */
	public void setLogToFileAction(boolean enable) {
		if (useMessageFrame()) {
			logger.log(Level.FINE, "Cannot log to file when running as an applet."); //$NON-NLS-1$
			return; // cannot log to file in applet mode
		}
		if (enable && fileHandler == null) {
			logToFileItem.setSelected(true);
			logger.addHandler(getFileHandler());
		} else if (fileHandler != null) {
			logToFileItem.setSelected(false);
			logger.removeHandler(fileHandler);
			// BH 2020.04.05 -- don't we want to close it so we can look at it then?
			fileHandler.close();
			fileHandler = null;
			logger.log(Level.INFO, tempFileName + " closed"); //$NON-NLS-1$ "

		}
	}

	/*
	 * //Uncomment this method to test the OSPLog. public static void main(String[]
	 * args) { JMenuBar menubar = getOSPLog().getJMenuBar(); JMenu menu = new
	 * JMenu("Test"); menubar.add(menu); String[] levels = new String[] { "SEVERE",
	 * "WARNING", "INFO", "CONFIG", "FINE", "FINER", "FINEST" }; for(int i =
	 * 0;i<levels.length;i++) { JMenuItem item = new JMenuItem(levels[i]);
	 * menu.add(item, 0); item.setActionCommand(levels[i]);
	 * item.addActionListener(new ActionListener() { public void
	 * actionPerformed(ActionEvent e) {
	 * OSPLOG.getLogger().log(Level.parse(e.getActionCommand()),
	 * "Testing "+e.getActionCommand()); } }); } menu = new JMenu("Console");
	 * menubar.add(menu); JMenuItem item = new JMenuItem("Console Out");
	 * menu.add(item, 0); item.addActionListener(new ActionListener() { public void
	 * actionPerformed(ActionEvent e) { System.out.println("Out console message.");
	 * } }); item = new JMenuItem("Console Err"); menu.add(item, 0);
	 * item.addActionListener(new ActionListener() { public void
	 * actionPerformed(ActionEvent e) {
	 * System.err.println("Error console message."); } }); item = new
	 * JMenuItem("Exception"); menu.add(item, 0); item.addActionListener(new
	 * ActionListener() { public void actionPerformed(ActionEvent e) { double[] x =
	 * null; x[0] = 1; } }); OSPLOG.setVisible(true);
	 * OSPLOG.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); }
	 */

	/**
	 * Fires a property change event. Needed to expose protected method.
	 */
	protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		frame.firePropertyChange(propertyName, oldValue, newValue);
	}

	/**
	 * Creates the GUI.
	 */
	protected void createGUI() {
		if (logPanel != null || frame == null)
			return;
		// create the panel, text pane and scroller
		logPanel = new JPanel(new BorderLayout());
		logPanel.setPreferredSize(new Dimension(480, 240));
		frame.setContentPane(logPanel);
		// create the menus
		createMenus();
		frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
	}

	private JTextPane getTextPane() {
		JTextPane textPane = GUIUtils.newJTextPane();
		textPane.setEditable(false);
		textPane.setAutoscrolls(true);
		JScrollPane textScroller = new JScrollPane(textPane);
		textScroller.setWheelScrollingEnabled(true);
		logPanel.add(textScroller, BorderLayout.CENTER);
		// create the colored styles
		black = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
		red = textPane.addStyle("red", black); //$NON-NLS-1$
		StyleConstants.setForeground(red, DARK_RED);
		blue = textPane.addStyle("blue", black); //$NON-NLS-1$
		StyleConstants.setForeground(blue, DARK_BLUE);
		green = textPane.addStyle("green", black); //$NON-NLS-1$
		StyleConstants.setForeground(green, DARK_GREEN);
		magenta = textPane.addStyle("magenta", black); //$NON-NLS-1$
		StyleConstants.setForeground(magenta, new Color(155, 0, 155));// darker than Color.MAGENTA);
		gray = textPane.addStyle("gray", black); //$NON-NLS-1$
		StyleConstants.setForeground(gray, Color.GRAY);
		textPane.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				try {
					if (OSPRuntime.isPopupTrigger(e)) {
						// show popup menu
						if (popup != null) {
							FontSizer.setFonts(popup);
							popup.show(logPane, e.getX(), e.getY() + 8);
						}
					}
				} catch (Exception ex) {
					System.err.println("Error in mouse action."); //$NON-NLS-1$
					System.err.println(ex.toString());
					ex.printStackTrace();
				}
			}

		});
		return textPane;
	}

	/**
	 * Creates and initializes the logger.
	 *
	 * @return the logger
	 */
	protected Logger createLogger() {
		// get the package logger and reference the ResourceBundle it will use
		if (bundleName != null) {
			try {
				logger = Logger.getLogger(pkgName, bundleName);
			} catch (Exception ex) {
				logger = Logger.getLogger(pkgName);
			}
		} else {
			logger = Logger.getLogger(pkgName);
		}
		try {
			logger.setLevel(defaultLevel);
			// add a log handler for this log
			logHandler = new OSPLogHandler(null, this);
			logHandler.setFormatter(new ConsoleFormatter());
			logHandler.setLevel(Level.ALL);
			// ignore parent handlers (specifically root console handler)
			OSPRuntime.class.getClass(); // force the static methods to execute
			logger.setUseParentHandlers(false);
			logger.addHandler(logHandler);
		} catch (SecurityException ex) {
			hasPermission = false;
		}
		return logger;
	}

	/**
	 * Gets the file handler using lazy instantiation.
	 *
	 * @return the Handler
	 */
	protected synchronized Handler getFileHandler() {
		if (fileHandler != null) {
			return fileHandler;
		}
		try {
			// add a file handler with file name equal to short package name
			int i = pkgName.lastIndexOf("."); //$NON-NLS-1$
			if (i > -1) {
				pkgName = pkgName.substring(i + 1);
			}
			if (logdir.endsWith(slash)) {
				tempFileName = logdir + pkgName + ".log"; //$NON-NLS-1$
			} else {
				tempFileName = logdir + slash + pkgName + ".log"; //$NON-NLS-1$
			}
			fileHandler = new FileHandler(tempFileName);
			fileHandler.setFormatter(new XMLFormatter());
			fileHandler.setLevel(Level.ALL);
			logger.addHandler(fileHandler);
			logger.log(Level.INFO, "Logging to file enabled. File = " + tempFileName); //$NON-NLS-1$
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return fileHandler;
	}

	/**
	 * Creates the popup menu.
	 */
	protected void createMenus() {
		if (!hasPermission || frame == null) {
			return;
		}
		popup = new JPopupMenu();
		JMenu menu = new JMenu(ControlsRes.getString("OSPLog.Level_menu")); //$NON-NLS-1$
		popup.add(menu);
		popupGroup = new ButtonGroup();
		for (int i = 0; i < levels.length; i++) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(levels[i].getName());
			menu.add(item, 0);
			popupGroup.add(item);
			if (logger.getLevel().toString().equals(levels[i].toString())) {
				item.setSelected(true);
			}
			item.setActionCommand(levels[i].getName());
			item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					logger.setLevel(Level.parse(e.getActionCommand()));
					Enumeration<AbstractButton> e2 = menubarGroup.getElements();
					while (e2.hasMoreElements()) {
						JMenuItem item = (JMenuItem) e2.nextElement();
						if (logger.getLevel().toString().equals(item.getActionCommand())) {
							item.setSelected(true);
							break;
						}
					}
				}

			});
		}
		popup.addSeparator();
		Action openAction = new AbstractAction(ControlsRes.getString("OSPLog.Open_popup")) { //$NON-NLS-1$
			@Override
			public void actionPerformed(ActionEvent e) {
				open();
			}

		};

		openAction.setEnabled(!useMessageFrame());
		popup.add(openAction);
		Action saveAsAction = new AbstractAction(ControlsRes.getString("OSPLog.SaveAs_popup")) { //$NON-NLS-1$
			@Override
			public void actionPerformed(ActionEvent e) {
				saveLogAs();
			}

		};
		saveAsAction.setEnabled(!useMessageFrame());
		popup.add(saveAsAction);
		popup.addSeparator();
		Action clearAction = new AbstractAction(ControlsRes.getString("OSPLog.Clear_popup")) { //$NON-NLS-1$
			@Override
			public void actionPerformed(ActionEvent e) {
				clear();
			}

		};
		popup.add(clearAction);
		// create menubar
		JMenuBar menubar = new JMenuBar();
		frame.setJMenuBar(menubar);
		menu = new JMenu(ControlsRes.getString("OSPLog.File_menu")); //$NON-NLS-1$
		menubar.add(menu);
		menu.add(openAction);
		menu.add(saveAsAction);
		menu = new JMenu(ControlsRes.getString("OSPLog.Edit_menu")); //$NON-NLS-1$
		menubar.add(menu);
		menu.add(clearAction);
		menu = new JMenu(ControlsRes.getString("OSPLog.Level_menu")); //$NON-NLS-1$
		menubar.add(menu);
		menubarGroup = new ButtonGroup();
		for (int i = 0; i < levels.length; i++) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(levels[i].getName());
			menu.add(item, 0);
			menubarGroup.add(item);
			if (logger.getLevel().toString().equals(levels[i].toString())) {
				item.setSelected(true);
			}
			item.setActionCommand(levels[i].getName());
			item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					logger.setLevel(Level.parse(e.getActionCommand()));
					Enumeration<AbstractButton> e2 = popupGroup.getElements();
					while (e2.hasMoreElements()) {
						JMenuItem item = (JMenuItem) e2.nextElement();
						if (logger.getLevel().toString().equals(item.getActionCommand())) {
							item.setSelected(true);
							break;
						}
					}
				}

			});
		}
		JMenu prefMenu = new JMenu(ControlsRes.getString("OSPLog.Options_menu")); //$NON-NLS-1$
		menubar.add(prefMenu);
		logToFileItem = new JCheckBoxMenuItem(ControlsRes.getString("OSPLog.LogToFile_check_box")); //$NON-NLS-1$
		logToFileItem.setSelected(false);
		logToFileItem.setEnabled(!useMessageFrame());
		logToFileItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setLogToFileAction(((JCheckBoxMenuItem) e.getSource()).isSelected());
			}

		});
		prefMenu.add(logToFileItem);
	}

	/**
	 * Gets a file chooser.
	 *
	 * @return the chooser
	 */
	protected static JFileChooser getChooser() {
		if (chooser == null) {
			chooser = new JFileChooser(new File(OSPRuntime.chooserDir));
		}
		FontSizer.setFonts(chooser);
		return chooser;
	}

	/**
	 * Reads a file.
	 *
	 * @param fileName the name of the file
	 * @return the file contents as a String
	 */
	protected String read(String fileName) {
		File file = new File(fileName);
		StringBuffer buffer = null;
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			buffer = new StringBuffer();
			String line = in.readLine();
			while (line != null) {
				buffer.append(line + XML.NEW_LINE);
				line = in.readLine();
			}
			in.close();
			return buffer.toString();
		} catch (IOException ex) {
			logger.warning(ex.toString());
			return "";
		}
	}

	/**
	 * Singleton logging frame.
	 * 
	 * Option to write character-by-character (Java) or message-by-message
	 * (JavaScript)
	 * 
	 * @param name
	 * @param resourceBundleName
	 */
	@SuppressWarnings("resource")
	private OSPLog(String name, String resourceBundleName) {
		if (useFrame) {
			frame = new OSPFrame(ControlsRes.getString("OSPLog.DefaultTitle")); 
			frame.setName("LogTool"); // identify this as a tool //$NON-NLS-1$
		}
		bundleName = resourceBundleName;
		pkgName = name;
		ConsoleLevel.class.getName(); // force ConsoleLevel to load static constants
		// create the logger
		createLogger();
		LoggerOutputStream loggerOut = new LoggerOutputStream(ConsoleLevel.OUT_CONSOLE, System.out);
		LoggerOutputStream loggerErr = new LoggerOutputStream(ConsoleLevel.ERR_CONSOLE, System.err);
		LoggerPrintStream loggerOutPrint = new LoggerPrintStream(loggerOut, false);
		LoggerPrintStream loggerErrPrint = new LoggerPrintStream(loggerErr, true);
		try {
			if (divertSysOut) {
				System.err.println("OSPLog installed in System.out and System.err");
				System.setOut(loggerOutPrint);
				System.setErr(loggerErrPrint);
			} else {
				System.err.println("OSPLog divertSysOut = false -- bypassing OSPLog.setOut");
			}
		} catch (SecurityException ex) {
			/** empty block */
		}
		setLevel(defaultLevel, this);

	}

	private static void log(Level level, String msg) {
		if (OSPRuntime.dontLog)
			return;
		LogRecord record = new LogRecord(level, msg);

		String className = null, methodName = null;
		if (OSPRuntime.isJS) {

			OSPRuntime.showStatus("OSPLog[" + level + "] " + msg);

			try {
				/**
				 * @j2sNative var o = arguments.callee.caller.caller; methodName = o &&
				 *            o.exName; className = o && o.exClazz && o.exClazz.__CLASS_NAME__;
				 */
			} catch (Throwable t) {
				// ignore -- something went wrong
			}
		} else {
			StackTraceElement stack[] = (new Throwable()).getStackTrace();
			// find the first method not in class OSPLog
			for (int i = 0; i < stack.length; i++) {
				StackTraceElement el = stack[i];
				className = el.getClassName();
				if (!className.equals("org.opensourcephysics.controls.OSPLog")) { //$NON-NLS-1$
					// set the source class and method
					methodName = el.getMethodName();
					break;
				}
			}
		}
		if (methodName != null) {
			record.setSourceClassName(className);
			record.setSourceMethodName(methodName);
		}

		if (OSPLOG != null) {
			OSPLOG.getLogger().log(record);
		} else {
			messageStorage[messageIndex++] = record;
			messageIndex = messageIndex % messageStorage.length;
		}
	}

	/**
	 * A class that formats a record as if this were the console.
	 */
	class ConsoleFormatter extends SimpleFormatter {
		/**
		 * Formats the record as if this were the console.
		 *
		 * @param record LogRecord
		 * @return String
		 */
		@Override
		public String format(LogRecord record) {
			String message = formatMessage(record);
			if ((record.getLevel().intValue() == ConsoleLevel.OUT_CONSOLE.intValue())
					|| (record.getLevel().intValue() == ConsoleLevel.ERR_CONSOLE.intValue())) {
				StringBuffer sb = new StringBuffer();
				if ((message.length() > 0) && (message.charAt(0) == '\t')) {
					message = message.replaceFirst("\t", "    "); //$NON-NLS-1$//$NON-NLS-2$
				} else {
					sb.append("CONSOLE: "); //$NON-NLS-1$
				}
				sb.append(message);
				sb.append(OSPLog.eol);
				// new line after message
				if (record.getThrown() != null) {
					try {
						StringWriter sw = new StringWriter();
						PrintWriter pw = new PrintWriter(sw);
						record.getThrown().printStackTrace(pw);
						pw.close();
						sb.append(sw.toString());
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
				return sb.toString();
			}
			return super.format(record);
		}

	}

	/**
	 * A class that writes an output stream to the logger.
	 */
	class LoggerOutputStream extends OutputStream {
		StringBuffer buffer = new StringBuffer();
		ConsoleLevel level;
//		private LoggerPrintStream jsPrintStream;

		LoggerOutputStream(ConsoleLevel level, OutputStream oldStream) {
			this.level = level;
		}

//		public void setJSPrintStream(LoggerPrintStream loggerPrintStream) {
//			jsPrintStream = loggerPrintStream;
//
//		}

		public void print(String msg) {
			buffer.append(msg);
		}

		public void println(String msg) {
			buffer.append(msg);
			logMsg();
		}

		/**
		 * Write a character to the buffer. This method is not called from within OSP.
		 * 
		 */
		@Override
		public void write(int c) throws IOException {
			if (c == '\n') {
				logMsg();
			} else {
				buffer.append((char) c);
			}
		}

		/**
		 * If by write(int), no EOL here; if by println, just no EOL at the end.
		 * 
		 */
		private void logMsg() {
			LogRecord record = new LogRecord(level, buffer.toString());
			OSPLog.getOSPLog().getLogger().log(record);
			buffer = new StringBuffer();
		}

	}

	/**
	 * A handler class for a text log.
	 */
	class OSPLogHandler extends Handler {

		OSPLog ospLog;

		/**
		 * Constructor OSPLogHandler
		 * 
		 * @param textPane
		 */
		public OSPLogHandler(JTextPane textPane, OSPLog log) {
			logPane = textPane; // will be null initially
			ospLog = log;
		}

		public void setTextPane(JTextPane textPane) {
			logPane = textPane;
		}

		@Override
		public void publish(LogRecord record) {
			if (!isLoggable(record)) {
				return;
			}
			if (logPane == null) {
				logRecords.add(record);
				return;
			}
			String msg = getFormatter().format(record);

			Style style = OSPLog.green; // default style
			int val = record.getLevel().intValue();
			if (val == ConsoleLevel.ERR_CONSOLE.intValue()) {
				if (msg.indexOf("OutOfMemory") > -1) //$NON-NLS-1$
					ospLog.firePropertyChange(OSPRuntime.PROPERTY_ERROR_OUTOFMEMORY, null, OSPRuntime.OUT_OF_MEMORY_ERROR); //$NON-NLS-1$
				if (!OSPLog.logConsole)
					return;
				style = OSPLog.magenta;
			} else if (val == ConsoleLevel.OUT_CONSOLE.intValue()) {
				if (msg.indexOf("ERROR org.ffmpeg") > -1) //$NON-NLS-1$
					ospLog.firePropertyChange("ffmpeg_error", null, msg); //$NON-NLS-1$
				else if (msg.indexOf("JNILibraryLoader") > -1) {//$NON-NLS-1$
					ospLog.firePropertyChange("xuggle_error", null, msg); //$NON-NLS-1$
				}
				if (!OSPLog.logConsole)
					return;
				style = OSPLog.gray;
			} else if (val >= Level.WARNING.intValue()) {
				style = OSPLog.red;
			} else if (val >= Level.INFO.intValue()) {
				style = OSPLog.black;
			} else if (val >= Level.CONFIG.intValue()) {
				style = OSPLog.green;
			} else if (val >= Level.FINEST.intValue()) {
				style = OSPLog.blue;
			}
			try {
				Document doc = logPane.getDocument();
				doc.insertString(doc.getLength(), msg + '\n', style);
				// scroll to display new message
				Rectangle rect = logPane.getBounds();
				rect.y = rect.height;
				logPane.scrollRectToVisible(rect);
			} catch (BadLocationException ex) {
				System.err.println(ex);
			}
		}

		@Override
		public void flush() {
			/** empty block */
		}

		@Override
		public void close() {
			/** empty block */
		}

	}

	public static void debug(String msg) {
		realSysout.println("OSPLog.debug " + msg);
	}

	public static void setFonts(int level) {
		OSPLog log = getOSPLog();
		FontSizer.setFonts(log.frame);
	}

	public static void finalized(Object c) {
		notify(c, "finalized");
	}

	public static void notify(Object c, String msg) {
		if (doNotify)
			OSPLog.finer((c instanceof String ? c : c.getClass().getSimpleName()) + " " + msg);		
	}

	public static JFrame getFrame() {
		return getOSPLog().frame;
	}

	public static void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
		if (getFrame() != null)
			getFrame().addPropertyChangeListener(propertyChangeListener);
	}

	public static void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
		if (getFrame() != null)
			getFrame().removePropertyChangeListener(propertyChangeListener);
	}

	public static void addPropertyChangeListener(String name,
			PropertyChangeListener propertyChangeListener) {
		if (getFrame() != null)
			getFrame().addPropertyChangeListener(name, propertyChangeListener);
	}

}

/**
 * A formatter class that formats a log record as an osp xml string
 */
class OSPLogFormatter extends java.util.logging.Formatter {
	XMLControl control = new XMLControlElement();

	/**
	 * Format the given log record and return the formatted string.
	 *
	 * @param record the log record to be formatted
	 * @return the formatted log record
	 */
	@Override
	public String format(LogRecord record) {
		control.saveObject(record);
		return control.toXML();
	}

}

/**
 * A class to save and load LogRecord data in an XMLControl. Note: this is in a
 * very primitive state for testing only
 */
class OSPLogRecordLoader extends XMLLoader {
	@Override
	public void saveObject(XMLControl control, Object obj) {
		LogRecord record = (LogRecord) obj;
		control.setValue("message", record.getMessage()); //$NON-NLS-1$
		control.setValue("level", record.getLevel().toString()); //$NON-NLS-1$
	}

	@Override
	public Object createObject(XMLControl control) {
		String message = control.getString("message"); //$NON-NLS-1$
		String level = control.getString("level"); //$NON-NLS-1$
		return new LogRecord(Level.parse(level), message);
	}

	@Override
	public Object loadObject(XMLControl control, Object obj) {
		return obj;
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
 * https://www.compadre.org/osp
 */
