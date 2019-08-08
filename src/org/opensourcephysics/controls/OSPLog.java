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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import java.util.Enumeration;
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
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.FontSizer;

/**
 * This is a viewable file-based message log for a java package.
 * It displays log records in a text pane and saves them in a temp file.
 *
 * @author Douglas Brown
 * @author Wolfgang Christian
 * @version 1.0
 */
public class OSPLog extends JFrame {
	
  public static final Level[] levels = new Level[] {Level.OFF, Level.SEVERE, Level.WARNING, Level.INFO, ConsoleLevel.ERR_CONSOLE,
			  ConsoleLevel.OUT_CONSOLE, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST, Level.ALL};
  
  /**
   * A logger that can be used by any OSP program to log and show messages.
   */
  private static OSPLog OSPLOG;
  
  public static OSPLog getOSPLog() {
	  return null;
  }

  public static JFrame showLog() {
	  return null;
  }
  
  public void setLogToFile(boolean enable) {
	  
  }
  
  /**
   * Sets the logger level.
   *
   * @param level the Level
   */
  public static void setLevel(Level level) {
	  
  }
  
  public static int getLevelValue() {
	  return 0;
  }
  
  public static boolean isLogVisible() {
	  return false;
  }
  
  public static Level parseLevel(String level) {
	  return null;
  }

  /**
   * Logs an information message.
   *
   * @param msg the message
   */
  public static void info(String msg) {
	  System.out.println(msg);
  }

  /**
   * Logs a configuration message.
   *
   * @param msg the message
   */
  public static void config(String msg) {
      System.out.println(msg);
  }

  /**
   * Logs a fine debugging message.
   *
   * @param msg the message
   */
  public static void fine(String msg) {
	  System.out.println(msg);
  }
  
  /**
   * Logs a fine debugging message.
   *
   * @param msg the message
   */
  public static void finest(String msg) {
	  System.out.println(msg);
  }
  
  /**
   * Logs a fine debugging message.
   *
   * @param msg the message
   */
  public static void finer(String msg) {
	  System.out.println(msg);
  }
  
  /**
   * Logs a fine debugging message.
   *
   * @param msg the message
   */
  public static void warning(String msg) {
	  System.out.println(msg);
  }

  /**
   * Logs a fine debugging message.
   *
   * @param msg the message
   */
  public static void severe(String msg) {
	  System.out.println(msg);
  }
  /**
   * Clears the log.
   */
  public void clear() {
    
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
 * Copyright (c) 2017  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
