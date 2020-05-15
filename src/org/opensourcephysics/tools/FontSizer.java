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
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.SwingPropertyChangeSupport;

import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.display.ResizableIcon;

/**
 * A class for setting relative font sizes by level.
 *
 * @author dobrown
 * @version 1.0
 */
public class FontSizer {

	/** Base font for text fields */
	public static final Font TEXT_FONT = new JTextField().getFont();

	/** Base font for buttons */
	public static final Font BUTTON_FONT = new JButton().getFont();

	/** Base font for menu accelerators */
	public static final Font ACCELERATOR_FONT = (Font) UIManager.get("MenuItem.acceleratorFont"); //$NON-NLS-1$

	/** Icons for checkbox, radio button and arrow menu items */
	public static final ResizableIcon CHECKBOXMENUITEM_ICON;
	public static final ResizableIcon RADIOBUTTONMENUITEM_ICON;
	public static final ResizableIcon CHECKBOX_ICON;
	public static final ResizableIcon RADIOBUTTON_ICON;
	public static final ResizableIcon ARROW_ICON;

	/** maximum font level */
	public static final int MAX_LEVEL = 9;

	// static fields
	static Object levelObj = new FontSizer();
	static PropertyChangeSupport support = new SwingPropertyChangeSupport(levelObj);
	static int level, integerFactor;
	static double levelFactor = 1.25; // size ratio per level
	static double factor;
	static Map<Font, Font> fontMap = new HashMap<Font, Font>();

	static {
		Icon baseIcon = (Icon) UIManager.get("CheckBoxMenuItem.checkIcon"); //$NON-NLS-1$
		CHECKBOXMENUITEM_ICON = new ResizableIcon(baseIcon);
		UIManager.put("CheckBoxMenuItem.checkIcon", CHECKBOXMENUITEM_ICON); //$NON-NLS-1$
		baseIcon = (Icon) UIManager.get("CheckBox.icon"); //$NON-NLS-1$
		CHECKBOX_ICON = new ResizableIcon(baseIcon);
		UIManager.put("CheckBox.icon", CHECKBOX_ICON); //$NON-NLS-1$
		baseIcon = (Icon) UIManager.get("RadioButtonMenuItem.checkIcon"); //$NON-NLS-1$
		RADIOBUTTONMENUITEM_ICON = new ResizableIcon(baseIcon);
		UIManager.put("RadioButtonMenuItem.checkIcon", RADIOBUTTONMENUITEM_ICON); //$NON-NLS-1$
		baseIcon = (Icon) UIManager.get("RadioButton.icon"); //$NON-NLS-1$
		RADIOBUTTON_ICON = new ResizableIcon(baseIcon);
		UIManager.put("RadioButton.icon", RADIOBUTTON_ICON); //$NON-NLS-1$
		baseIcon = (Icon) UIManager.get("Menu.arrowIcon"); //$NON-NLS-1$
		ARROW_ICON = new ResizableIcon(baseIcon);
		UIManager.put("Menu.arrowIcon", ARROW_ICON); //$NON-NLS-1$
	}

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
		n = Math.min(n, MAX_LEVEL);
		level = Math.max(n, 0);
		factor = getFactor(level);
		integerFactor = getIntegerFactor(level);

		CHECKBOXMENUITEM_ICON.resize(integerFactor);
		CHECKBOX_ICON.resize(integerFactor);
		ARROW_ICON.resize(integerFactor);
		RADIOBUTTONMENUITEM_ICON.resize(integerFactor);
		RADIOBUTTON_ICON.resize(integerFactor);

		Font font = getResizedFont(TEXT_FONT, level);
		UIManager.put("OptionPane.messageFont", font); //$NON-NLS-1$
		UIManager.put("TextField.font", font); //$NON-NLS-1$
		UIManager.put("ToolTip.font", font); //$NON-NLS-1$
		UIManager.put("TabbedPane.font", font); //$NON-NLS-1$

		font = getResizedFont(ACCELERATOR_FONT, level);
		UIManager.put("MenuItem.acceleratorFont", font); //$NON-NLS-1$

		font = getResizedFont(BUTTON_FONT, level);
		UIManager.put("OptionPane.buttonFont", font); //$NON-NLS-1$

		support.firePropertyChange("level", null, Integer.valueOf(level)); //$NON-NLS-1$
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
	 * Increments the font level.
	 */
	public static void levelUp() {
		setLevel(level + 1);
	}

	/**
	 * Decrements the font level.
	 */
	public static void levelDown() {
		setLevel(level - 1);
	}

	/**
	 * Gets the current factor.
	 *
	 * @return the factor
	 */
	public static double getFactor() {
		return factor;
	}

	/**
	 * Gets the current integer factor.
	 *
	 * @return the integer factor
	 */
	public static int getIntegerFactor() {
		return integerFactor;
	}

	/**
	 * Sets the fonts of an object to a specified level.
	 *
	 * @param obj   the object
	 * @param level the level
	 */
	public static void setFonts(Object obj, int level) {

		if (obj == null || !OSPRuntime.allowSetFonts)
			return; // BH 2020.04.23 may be the case for missing buttons in Tracker
		
		if (obj instanceof Object[]) {
			for (Object next : ((Object[]) obj)) {
				setFonts(next, level);
			}
			return;
		}
		
		if (obj instanceof Collection) {
			for (Object next : ((Collection<?>) obj)) {
				setFonts(next, level);
			}
			return;
		}
		
		double factor = getFactor(level);

		if (obj instanceof Container) {
			setFontFactor((Container) obj, factor);
		} else if (obj instanceof TitledBorder) {
			setFontFactor((TitledBorder) obj, factor);
		} else if (obj instanceof Component) {
			setFontFactor((Component) obj, factor);
		}
	}

	/**
	 * Resizes a font to a specified level.
	 *
	 * @param font  the font
	 * @param level the level
	 * @return the resized font
	 */
	public static Font getResizedFont(Font font, int level) {
		return getResizedFont(font, getFactor(level));
	}

	/**
	 * Resizes a font by a specified factor.
	 *
	 * @param font   the font
	 * @param factor the factor
	 * @return the resized font
	 */
	public static Font getResizedFont(Font font, double factor) {
		if (font == null) {
			return null;
		}
		// get base font for this font
		Font base = fontMap.get(font);
		if (base == null) {
			base = font;
			fontMap.put(font, base);
		}
		// derive new font from base
		float size = (float) (base.getSize() * factor);
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
		for (int i = 0; i < level; i++) {
			factor *= levelFactor;
		}
		return factor;
	}

	/**
	 * Gets the integer factor corresponding to a specified level.
	 *
	 * @param level the level
	 * @return the integer factor
	 */
	public static int getIntegerFactor(int level) {
		return Math.round(Math.round(getFactor(level)));
	}

	/**
	 * Adds a PropertyChangeListener.
	 *
	 * @param property the name of the property (only "level" accepted)
	 * @param listener the object requesting property change notification
	 */
	public static void addPropertyChangeListener(String property, PropertyChangeListener listener) {
		if (property.equals("level")) { //$NON-NLS-1$
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

	// _______________________________ private methods ____________________________

	/**
	 * Increases a container's normal font sizes by the specified factor. This
	 * includes all JComponents and windows.
	 *
	 * @param c      a container
	 * @param factor the factor
	 */
	private static void setFontFactor(Container c, double factor) {

		try {
			if (c == null)
				return;

			// get resized container font
			Font font = getResizedFont(c.getFont(), factor);
			Icon icon = null;
			boolean resized = false;

			// Only the top ancestor needs a call to repaint.

			Component top = null;

			if (c instanceof JComponent) {
				if (c instanceof JPopupMenu.Separator) {
					return;
				}
				JComponent jc = (JComponent) c;
				Border border = jc.getBorder();
				if (border instanceof TitledBorder) {
					setFontFactor((TitledBorder) border, factor);
				}
				top = jc.getTopLevelAncestor();

				// added by Doug Brown June 2015 to resize icons along with fonts
				if (c instanceof AbstractButton) {
					AbstractButton button = (AbstractButton) c;
					icon = button.getIcon();
					resized = fixIcon(button.getSelectedIcon());
					resized = fixIcon(button.getRolloverIcon()) || resized;
					resized = fixIcon(button.getRolloverSelectedIcon()) | resized;
					if (c instanceof JMenu) {
						JMenu m = (JMenu) c;
						icon = m.getIcon();
						setFontFactor(m.getPopupMenu(), factor);
					}
				}
			} else {
				top = c;
			}
			// iterate through child components
			for (int i = 0, n = c.getComponentCount(); i < n; i++) {
				Component co = c.getComponent(i);
				if ((co instanceof Container)) {
					setFontFactor((Container) co, factor);
				} else {
					setFontFactor(co, factor);
				}
			}
			if (icon == null) {
				if (OSPRuntime.isJS) {

					// Could use reflection in SwingJS, but this is much simpler and faster
					icon = /** @j2sNative c.getIcon$ && c.getIcon$() || */
							null;
				} else {
					try {
						Method m = c.getClass().getMethod("getIcon", (Class<?>[]) null); //$NON-NLS-1$
						if (m != null) {
							icon = (Icon) m.invoke(c, (Object[]) null);
						}
					} catch (Exception e) {
					}
				}
			}
			// set the component font and its icon here

			if (c != null && !font.equals(c.getFont())) {
				c.setFont(font);
				resized = true;
			}

			resized = (icon != null && fixIcon(icon)) || resized;

			// BH actually, there is no need for a repaint
//			if (resized && c != null && c.isVisible() && top != null && top.isVisible()) {
//				scheduleRepaint(top);
//			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
//	private static boolean setMenuFont(JMenu m, Font font) {
//		if (font.equals(m.getFont())) {
//			// go no further if a menu
//			return false;
//		}
//		m.setFont(font);
//		for (int i = 0, n = m.getMenuComponentCount(); i < n; i++) {
//			Component mc = m.getMenuComponent(i);
//			if (!font.equals(mc.getFont())) {
//				mc.setFont(font);
//			}
//			if (mc instanceof JMenu) {
//				setMenuFont((JMenu) mc, font);
//			} else if (mc instanceof JMenuItem){
//				setFontFactor(mc, factor);				
//			}
//		}
//		return true;
//	}

// BH turns out there is no need to schedule a repaint -- this is Swing!	
//	
//	private static Stack<Component> repaintSet = new Stack<Component>();
//	private static Timer repaintTimer = new Timer(100, new ActionListener() {
//
//		@Override
//		public void actionPerformed(ActionEvent e) {
//			if (repaintSet.isEmpty())
//				return;
//			repaintSet.pop().repaint();
//			repaintTimer.restart();
//		}
//		
//	});
//	
//	private static void scheduleRepaint(Component top) {
//		if (repaintSet.contains(top)) {
//			return;
//		}
//		repaintTimer.stop();
//		repaintSet.add(top);
//		repaintTimer.restart();
//	}

	private static boolean fixIcon(Icon icon) {
		return (icon != null && icon instanceof ResizableIcon
				&& ((ResizableIcon) icon).resize(integerFactor));
	}

	/**
	 * Increases a component's normal font sizes by the specified factor.
	 *
	 * @param c      a component
	 * @param factor the factor
	 */
	private static void setFontFactor(Component c, double factor) {
		// get resized component font
		Font font = getResizedFont(c.getFont(), factor);
		c.setFont(font);
		// Added by Paco
		if (c instanceof JComponent) {
			Border border = ((JComponent) c).getBorder();
			if (border instanceof TitledBorder) {
				setFontFactor((TitledBorder) border, factor);
			}
		}
	}

	/**
	 * Increases a titled border's normal font size by the specified factor.
	 * @param c 
	 *
	 * @param b      a titled border
	 * @param factor the factor
	 */
	private static void setFontFactor(TitledBorder b, double factor) {
		// get resized border font
		Font font = b.getTitleFont();
		if (font == null) {
			font = UIManager.getFont("TitledBorder.font"); //$NON-NLS-1$
		}
		b.setTitleFont(getResizedFont(font, factor));
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
