/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <https://www.compadre.org/osp/>
 */

package org.opensourcephysics.controls;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.FontSizer;

/**
 * This is a viewable file-based message log for a java package.
 * It displays log records in a text pane and saves them in a temp file.
 * Modified for JavaScript library.
 *
 * @author Douglas Brown
 * @author Wolfgang Christian
 * @version 1.0
 */
public class OSPLog extends JFrame {
  /**
   * A logger that can be used by any OSP program to log and show messages.
   */
  private static OSPLog OSPLOG;
  private static JFileChooser chooser;
  protected static Style black, red, blue, green, magenta, gray;
  protected static final Color DARK_GREEN = new Color(0, 128, 0), DARK_BLUE = new Color(0, 0, 128), DARK_RED = new Color(128, 0, 0);
  public static final Level[] levels = new Level[] {Level.OFF, Level.SEVERE, Level.WARNING, Level.INFO, ConsoleLevel.ERR_CONSOLE, ConsoleLevel.OUT_CONSOLE, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST, Level.ALL};
  private static Level defaultLevel = ConsoleLevel.OUT_CONSOLE;
	public static final int OUT_OF_MEMORY_ERROR=1;
	protected static boolean logConsole = true;
	
  // instance fields
  private Logger logger;
  private JTextPane textPane;
  private JPanel logPanel;
  private JPopupMenu popup;
  private ButtonGroup popupGroup, menubarGroup;
  private String pkgName;
  private String bundleName;
  private boolean hasPermission = true;
  private static LogRecord[] messageStorage = new LogRecord[128];
  private static int messageIndex = 0;
  static String eol = "\n";   //$NON-NLS-1$
  static String logdir = "."; //$NON-NLS-1$
  static String slash = "/";  //$NON-NLS-1$

  static {
    try {                                                // system properties may not be readable in some environments
      eol = System.getProperty("line.separator", eol);   //$NON-NLS-1$
      logdir = System.getProperty("user.dir", logdir);  //$NON-NLS-1$
      slash = System.getProperty("file.separator", "/"); //$NON-NLS-1$//$NON-NLS-2$
    } catch(Exception ex) {
      /** empty block */
    }
  }

  /**
   * Gets the OSPLog that can be shared by multiple OSP packages.
   *
   * @return the shared OSPLog
   */
  public static OSPLog getOSPLog() {
    return OSPLOG;
  }

  /**
   * Sets the directory where the log file will be saved if logging is enabled.
   * @param dir String
   */
  public void setLogDir(String dir) {
    logdir = dir;
  }

  /**
   * Gets the directory where the log file will be saved if logging is enabled.
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
    if(MessageFrame.APPLET_MESSAGEFRAME!=null) {
      return MessageFrame.APPLET_MESSAGEFRAME.isVisible();
    }
    return false;
  }

  /**
   * Sets the visibility of this log.
   *
   * @param true to set visible
   */
  public void setVisible(boolean visible) {
      org.opensourcephysics.controls.MessageFrame.showLog(visible); 
  }

  /**
   * Determines if the log is visible.
   *
   * @return true if visible
   */
  public boolean isVisible() {
      return org.opensourcephysics.controls.MessageFrame.isLogVisible();
  }

  /**
   * Shows the log when it is invoked from the event queue.
   */
  public static JFrame showLog() {
      return org.opensourcephysics.controls.MessageFrame.showLog(true);
  
  }

  /**
   * Gets the logger level value.
   * @return the current level value
   */
  public static int getLevelValue() {
      return org.opensourcephysics.controls.MessageFrame.getLevelValue();
  }

  /**
   * Sets the logger level.
   *
   * @param level the Level
   */
  public static void setLevel(Level level) {
      org.opensourcephysics.controls.MessageFrame.setLevel(level);
  }

  /**
   * Returns the Level with the specified name, or null if none.
   *
   * @param level the Level
   */
  public static Level parseLevel(String level) {
    for(int i = 0; i<levels.length; i++) {
      if(levels[i].getName().equals(level)) {
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
      org.opensourcephysics.controls.MessageFrame.severe(msg);
  }

  /**
   * Logs a warning message.
   *
   * @param msg the message
   */
  public static void warning(String msg) {
      org.opensourcephysics.controls.MessageFrame.warning(msg);
  }

  /**
   * Logs an information message.
   *
   * @param msg the message
   */
  public static void info(String msg) {
      org.opensourcephysics.controls.MessageFrame.info(msg);
  }

  /**
   * Logs a configuration message.
   *
   * @param msg the message
   */
  public static void config(String msg) {
      org.opensourcephysics.controls.MessageFrame.config(msg);
  }

  /**
   * Logs a fine debugging message.
   *
   * @param msg the message
   */
  public static void fine(String msg) {
      org.opensourcephysics.controls.MessageFrame.fine(msg);
  }

  /**
   * Clears the Log.
   *
   * @param msg the message
   */
  public static void clearLog() {
      org.opensourcephysics.controls.MessageFrame.clear();
  }

  /**
   * Logs a finer debugging message.
   *
   * @param msg the message
   */
  public static void finer(String msg) {
      org.opensourcephysics.controls.MessageFrame.finer(msg);
  }

  /**
   * Logs a finest debugging message.
   *
   * @param msg the message
   */
  public static void finest(String msg) {
      org.opensourcephysics.controls.MessageFrame.finest(msg);
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
   * @param pkg the package
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
   * @param type the class
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
    return logPanel;
  }

  /**
   * Clears the log.
   */
  public void clear() {
    textPane.setText(null);
  }


  /**
   * Gets the logger.
   *
   * @return the logger
   */
  public Logger getLogger() {
    return logger;
  }


  /*
   *  //Uncomment this method to test the OSPLog.
   * public static void main(String[] args) {
   *   JMenuBar menubar = getOSPLog().getJMenuBar();
   *   JMenu menu = new JMenu("Test");
   *   menubar.add(menu);
   *   String[] levels = new String[] {
   *     "SEVERE", "WARNING", "INFO", "CONFIG", "FINE", "FINER", "FINEST"
   *   };
   *   for(int i = 0;i<levels.length;i++) {
   *     JMenuItem item = new JMenuItem(levels[i]);
   *     menu.add(item, 0);
   *     item.setActionCommand(levels[i]);
   *     item.addActionListener(new ActionListener() {
   *       public void actionPerformed(ActionEvent e) {
   *         OSPLOG.getLogger().log(Level.parse(e.getActionCommand()), "Testing "+e.getActionCommand());
   *       }
   *     });
   *   }
   *   menu = new JMenu("Console");
   *   menubar.add(menu);
   *   JMenuItem item = new JMenuItem("Console Out");
   *   menu.add(item, 0);
   *   item.addActionListener(new ActionListener() {
   *     public void actionPerformed(ActionEvent e) {
   *       System.out.println("Out console message.");
   *     }
   *   });
   *   item = new JMenuItem("Console Err");
   *   menu.add(item, 0);
   *   item.addActionListener(new ActionListener() {
   *     public void actionPerformed(ActionEvent e) {
   *       System.err.println("Error console message.");
   *     }
   *   });
   *   item = new JMenuItem("Exception");
   *   menu.add(item, 0);
   *   item.addActionListener(new ActionListener() {
   *     public void actionPerformed(ActionEvent e) {
   *       double[] x = null;
   *       x[0] = 1;
   *     }
   *   });
   *   OSPLOG.setVisible(true);
   *   OSPLOG.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
   * }
   */

  /**
   * Fires a property change event. Needed to expose protected method.
   */
  protected void firePropertyChange(String propertyName,
      Object oldValue, Object newValue) {
  	super.firePropertyChange(propertyName, oldValue, newValue);
  }
  
  /**
   * Creates the GUI.
   */
  protected void createGUI() {
    // create the panel, text pane and scroller
    logPanel = new JPanel(new BorderLayout());
    logPanel.setPreferredSize(new Dimension(480, 240));
    setContentPane(logPanel);
    textPane = new JTextPane() {
      public void paintComponent(Graphics g) {
        if(OSPRuntime.antiAliasText) {
          Graphics2D g2 = (Graphics2D) g;
          RenderingHints rh = g2.getRenderingHints();
          rh.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
          rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        super.paintComponent(g);
      }

    };
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
    StyleConstants.setForeground(magenta, Color.MAGENTA);
    gray = textPane.addStyle("gray", black); //$NON-NLS-1$
    StyleConstants.setForeground(gray, Color.GRAY);
    // create the logger
    // create the menus
    createMenus();
    pack();
    textPane.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        try {
          if(OSPRuntime.isPopupTrigger(e)) {
            // show popup menu
            if(popup!=null) {
            	FontSizer.setFonts(popup, FontSizer.getLevel());
              popup.show(textPane, e.getX(), e.getY()+8);
            }
          }
        } catch(Exception ex) {
          System.err.println("Error in mouse action."); //$NON-NLS-1$
          System.err.println(ex.toString());
          ex.printStackTrace();
        }
      }

    });
  }
  
  /**
   * Creates the popup menu.
   */
  protected void createMenus() {
    if(!hasPermission) {
      return;
    }
    popup = new JPopupMenu();
    JMenu menu = new JMenu(ControlsRes.getString("OSPLog.Level_menu")); //$NON-NLS-1$
    popup.add(menu);
    popupGroup = new ButtonGroup();
    for(int i = 0; i<levels.length; i++) {
      JRadioButtonMenuItem item = new JRadioButtonMenuItem(levels[i].getName());
      menu.add(item, 0);
      popupGroup.add(item);
      if(logger.getLevel().toString().equals(levels[i].toString())) {
        item.setSelected(true);
      }
      item.setActionCommand(levels[i].getName());
      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          logger.setLevel(Level.parse(e.getActionCommand()));
          Enumeration<AbstractButton> e2 = menubarGroup.getElements();
          while(e2.hasMoreElements()) {
            JMenuItem item = (JMenuItem) e2.nextElement();
            if(logger.getLevel().toString().equals(item.getActionCommand())) {
              item.setSelected(true);
              break;
            }
          }
        }

      });
    }

    popup.addSeparator();
    Action clearAction = new AbstractAction(ControlsRes.getString("OSPLog.Clear_popup")) { //$NON-NLS-1$
      public void actionPerformed(ActionEvent e) {
        clear();
      }

    };
    popup.add(clearAction);
    // create menubar
    JMenuBar menubar = new JMenuBar();
    setJMenuBar(menubar);
    menu = new JMenu(ControlsRes.getString("OSPLog.File_menu")); //$NON-NLS-1$
    menubar.add(menu);
    menu = new JMenu(ControlsRes.getString("OSPLog.Edit_menu")); //$NON-NLS-1$
    menubar.add(menu);
    menu.add(clearAction);
    menu = new JMenu(ControlsRes.getString("OSPLog.Level_menu")); //$NON-NLS-1$
    menubar.add(menu);
    menubarGroup = new ButtonGroup();
    for(int i = 0; i<levels.length; i++) {
      JRadioButtonMenuItem item = new JRadioButtonMenuItem(levels[i].getName());
      menu.add(item, 0);
      menubarGroup.add(item);
      if(logger.getLevel().toString().equals(levels[i].toString())) {
        item.setSelected(true);
      }
      item.setActionCommand(levels[i].getName());
      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          logger.setLevel(Level.parse(e.getActionCommand()));
          Enumeration<AbstractButton> e2 = popupGroup.getElements();
          while(e2.hasMoreElements()) {
            JMenuItem item = (JMenuItem) e2.nextElement();
            if(logger.getLevel().toString().equals(item.getActionCommand())) {
              item.setSelected(true);
              break;
            }
          }
        }

      });
    }
    JMenu prefMenu = new JMenu(ControlsRes.getString("OSPLog.Options_menu")); //$NON-NLS-1$
    menubar.add(prefMenu);
  }
  
  private OSPLog(String name, String resourceBundleName) {
    super(ControlsRes.getString("OSPLog.DefaultTitle")); //$NON-NLS-1$
    this.setName("LogTool"); // identify this as a tool //$NON-NLS-1$
    bundleName = resourceBundleName;
    pkgName = name;
    ConsoleLevel.class.getName(); // force ConsoleLevel to load static constants
    createGUI();
    setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
  }

  private static void log(Level level, String msg) {
    LogRecord record = new LogRecord(level, msg);
    // get the stack trace
    StackTraceElement stack[] = (new Throwable()).getStackTrace();
    // find the first method not in class OSPLog
    for(int i = 0; i<stack.length; i++) {
      StackTraceElement el = stack[i];
      String className = el.getClassName();
      if(!className.equals("org.opensourcephysics.controls.OSPLog")) { //$NON-NLS-1$
        // set the source class and method
        record.setSourceClassName(className);
        record.setSourceMethodName(el.getMethodName());
        break;
      }
    }
    if(OSPLOG!=null) {
      OSPLOG.getLogger().log(record);
    } else {
      messageStorage[messageIndex] = record;
      messageIndex++;
      messageIndex = messageIndex%messageStorage.length;
    }
  }

}



/**
 * A class that writes an output stream to the logger.
 */
class LoggerOutputStream extends OutputStream {
  StringBuffer buffer = new StringBuffer();
  OutputStream oldStream;
  ConsoleLevel level;

  LoggerOutputStream(ConsoleLevel level, OutputStream oldStream) {
    this.level = level;
    this.oldStream = oldStream;
  }

  public void write(int c) throws IOException {  	
    oldStream.write(c);
    if(c=='\n') {
      LogRecord record = new LogRecord(level, buffer.toString());
      OSPLog.getOSPLog().getLogger().log(record);
      buffer = new StringBuffer();
    } else {
      buffer.append((char) c);
    }
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
  public String format(LogRecord record) {
    control.saveObject(record);
    return control.toXML();
  }

}

/**
 * A class to save and load LogRecord data in an XMLControl.
 * Note: this is in a very primitive state for testing only
 */
class OSPLogRecordLoader extends XMLLoader {
  public void saveObject(XMLControl control, Object obj) {
    LogRecord record = (LogRecord) obj;
    control.setValue("message", record.getMessage());        //$NON-NLS-1$
    control.setValue("level", record.getLevel().toString()); //$NON-NLS-1$
  }

  public Object createObject(XMLControl control) {
    String message = control.getString("message"); //$NON-NLS-1$
    String level = control.getString("level");     //$NON-NLS-1$
    return new LogRecord(Level.parse(level), message);
  }

  public Object loadObject(XMLControl control, Object obj) {
    return obj;
  }

}

/*
 * Open Source Physics software is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.
 *
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
 * Copyright (c) 2019  The Open Source Physics project
 *                     https://www.compadre.org/osp
 */
