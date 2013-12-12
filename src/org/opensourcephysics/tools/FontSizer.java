/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JMenu;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.SwingPropertyChangeSupport;

/**
 * A class for setting relative font sizes by level.
 *
 * @author dobrown
 * @version 1.0
 */
public class FontSizer {
  // static fields
  static Object levelObj = new FontSizer();
  static PropertyChangeSupport support = new SwingPropertyChangeSupport(levelObj);
  static int level;
  static double levelFactor = 1.35; // size ratio per level
  static Map<Font, Font> fontMap = new HashMap<Font, Font>();

  /**
   * Private constructor to prevent instantiation.
   */
  private FontSizer() {
    /** empty block */
  }

  /**
   * Sets the font level and informs all listeners.
   *
   * @param n a non-negative integer level
   */
  public static void setLevel(int n) {
    level = Math.abs(n);
    support.firePropertyChange("level", null, new Integer(level)); //$NON-NLS-1$
  }

  /**
   * Gets the current font level.
   *
   * @return the level
   */
  public static int getLevel() {
    return level;
  }

  /**
   * Increments the font level and informs all listeners.
   */
  public static void levelUp() {
    level++;
    support.firePropertyChange("level", null, new Integer(level)); //$NON-NLS-1$
  }

  /**
   * Decrements the font level and informs all listeners.
   */
  public static void levelDown() {
    level--;
    level = Math.max(level, 0);
    support.firePropertyChange("level", null, new Integer(level)); //$NON-NLS-1$
  }

  /**
   * Sets the fonts of an object to a specified level.
   *
   * @param obj the object
   * @param level the level
   */
  public static void setFonts(Object obj, int level) {
    double factor = getFactor(level);
    if(obj instanceof Container) {
      setFontFactor((Container) obj, factor);
    } else if(obj instanceof TitledBorder) {
      setFontFactor((TitledBorder) obj, factor);
    } else if(obj instanceof Component) {
      setFontFactor((Component) obj, factor);
    }
  }

  /**
   * Resizes a font to a specified level.
   *
   * @param font the font
   * @param level the level
   * @return the resized font
   */
  public static Font getResizedFont(Font font, int level) {
    return getResizedFont(font, getFactor(level));
  }

  /**
   * Resizes a font by a specified factor.
   *
   * @param font the font
   * @param factor the factor
   * @return the resized font
   */
  public static Font getResizedFont(Font font, double factor) {
    if(font==null) {
      return null;
    }
    // get base font for this font
    Font base = fontMap.get(font);
    if(base==null) {
      base = font;
      fontMap.put(font, base);
    }
    // derive new font from base
    float size = (float) (base.getSize()*factor);
    font = base.deriveFont(size);
    fontMap.put(font, base);
    return font;
  }

  /**
   * Gets the factor corresponding to a specified level.
   *
   * @param level the level
   * @return the factor
   */
  public static double getFactor(int level) {
    // get font size factor (= levelFactor^level)
    double factor = 1.0;
    for(int i = 0; i<level; i++) {
      factor *= levelFactor;
    }
    return factor;
  }

  /**
   * Adds a PropertyChangeListener.
   *
   * @param property the name of the property (only "level" accepted)
   * @param listener the object requesting property change notification
   */
  public static void addPropertyChangeListener(String property, PropertyChangeListener listener) {
    if(property.equals("level")) { //$NON-NLS-1$
      support.addPropertyChangeListener(property, listener);
    }
  }

  /**
   * Removes a PropertyChangeListener.
   *
   * @param property the name of the property (only "level" accepted)
   * @param listener the listener requesting removal
   */
  public static void removePropertyChangeListener(String property, PropertyChangeListener listener) {
    support.removePropertyChangeListener(property, listener);
  }

  //_______________________________ private methods ____________________________

  /**
   * Increases a container's normal font sizes by the specified factor.
   *
   * @param c a container
   * @param factor the factor
   */
  private static void setFontFactor(Container c, double factor) {
    // get resized container font
    Font font = getResizedFont(c.getFont(), factor);
    // Added by Paco. This must be here because JComponent inherits from Container
    if(c instanceof javax.swing.JComponent) {
      Border border = ((javax.swing.JComponent) c).getBorder();
      if(border instanceof TitledBorder) {
        setFontFactor((TitledBorder) border, factor);
      }
    }
    // End of added by Paco
    if(c instanceof JMenu) {
      setMenuFont((JMenu) c, font);
    } else {
      c.setFont(font);
      // iterate through child components
      for(int i = 0; i<c.getComponentCount(); i++) {
        Component co = c.getComponent(i);
        if((co instanceof Container)) {
          setFontFactor((Container) co, factor);
        } else {
          setFontFactor(co, factor);
        }
      }
    }
    c.repaint();
  }

  /**
   * Increases a component's normal font sizes by the specified factor.
   *
   * @param c a component
   * @param factor the factor
   */
  private static void setFontFactor(Component c, double factor) {
    // get resized component font
    Font font = getResizedFont(c.getFont(), factor);
    c.setFont(font);
    // Added by Paco
    if(c instanceof javax.swing.JComponent) {
      Border border = ((javax.swing.JComponent) c).getBorder();
      if(border instanceof TitledBorder) {
        setFontFactor((TitledBorder) border, factor);
      }
    }
    // End of added by Paco
  }

  /**
   * Increases a titled border's normal font size by the specified factor.
   *
   * @param b a titled border
   * @param factor the factor
   */
  private static void setFontFactor(TitledBorder b, double factor) {
    // get resized border font
    Font font = getResizedFont(b.getTitleFont(), factor);
    b.setTitleFont(font);
  }

  /**
   * Sets the menu font.
   *
   * @param m a menu
   * @param font the font
   */
  private static void setMenuFont(JMenu m, Font font) {
    m.setFont(font);
    for(int i = 0; i<m.getMenuComponentCount(); i++) {
      m.getMenuComponent(i).setFont(font);
      if(m.getMenuComponent(i) instanceof JMenu) {
        setMenuFont((JMenu) m.getMenuComponent(i), font);
      }
    }
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
 * Copyright (c) 2007  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
