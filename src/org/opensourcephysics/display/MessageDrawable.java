/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see: 
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display;

/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see: 
 * <http://www.opensourcephysics.org/>
 */

import java.awt.*; // uses Abstract Window Toolkit (awt)
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.opensourcephysics.tools.FontSizer;

/**
 * PixelRectangle demonstrates how to draw an object using the AWT drawing API.
 *
 * @author Wolfgang Christian, Jan Tobochnik, Harvey Gould
 * @version 1.0 05/16/05
 */
public class MessageDrawable implements Drawable {
	String tlStr = null; // "top left";
	String trStr = null; // "top right";
	String blStr = null; // "bottom left";
	String brStr = null; // "bottom right";

	protected Font font;
	protected String fontname = "TimesRoman"; // The logical name of the font to use //$NON-NLS-1$
	protected int fontsize = 14; // The font size
	protected int fontstyle = Font.PLAIN; // The font style

	protected PropertyChangeListener guiChangeListener;

	/**
	 * Constructs a MessageDrawable.
	 *
	 */
	public MessageDrawable() {
		font = new Font(fontname, fontstyle, fontsize);
		guiChangeListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				if (e.getPropertyName().equals("level")) { //$NON-NLS-1$
					int level = ((Integer) e.getNewValue()).intValue();
					setFontLevel(level);
				}
			}
		};
		FontSizer.addPropertyChangeListener("level", guiChangeListener); //$NON-NLS-1$
	}

	/**
	 * Sets the font level.
	 *
	 * @param level the level
	 */
	protected void setFontLevel(int level) {
		font = FontSizer.getResizedFont(font, level);
	}

	/**
	 * Sets the font factor.
	 *
	 * @param factor the factor
	 */
	public void setFontFactor(double factor) {
		font = FontSizer.getResizedFont(font, factor);
	}

	/**
	 * Shows a message in a yellow text box in the lower right hand corner.
	 *
	 * @param msg
	 */
	public void setMessage(String msg) {
		brStr = msg;
	}

	/**
	 * Shows a message in a yellow text box.
	 *
	 * location 0=bottom left location 1=bottom right location 2=top right location
	 * 3=top left
	 *
	 * @param msg
	 * @param location
	 */
	public void setMessage(String msg, int location) {
		switch (location) {
		case 0: // usually used for mouse coordinates
			blStr = msg;
			break;
		case 1:
			brStr = msg;
			break;
		case 2:
			trStr = msg;
			break;
		case 3:
			tlStr = msg;
			break;
		}
	}

	/**
	 * Draws this rectangle using the AWT drawing API. Required to implement the
	 * Drawable interface.
	 *
	 * @param panel DrawingPanel
	 * @param g     Graphics
	 */
	public void draw(DrawingPanel panel, Graphics g) {
		g = g.create();
//		/** @j2sNative g.unclip$I(-3); */
		Font oldFont = g.getFont();
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		int height = fm.getAscent() + 4; // string height
		int width = 0; // string width
		g.setClip(0, 0, panel.getWidth(), panel.getHeight());
		// this method implements the Drawable interface
		if (tlStr != null && !tlStr.equals("")) { // draw tl message
			g.setColor(Color.YELLOW);
			width = fm.stringWidth(tlStr) + 6; // current string width
			g.fillRect(1, 0, width, height); // fills rectangle
			g.setColor(Color.BLACK);
			g.drawRect(1, 0, width, height);
			g.drawString(tlStr, 3, height - 4);
		}

		if (trStr != null && !trStr.equals("")) { // draw tr message
			g.setColor(Color.YELLOW);
			width = fm.stringWidth(trStr) + 8; // current string width
			g.fillRect(panel.getWidth() - width - 1, 0, width, height); // fills rectangle
			g.setColor(Color.BLACK);
			g.drawRect(panel.getWidth() - width - 1, 0, width, height); // fills rectangle
			g.drawString(trStr, panel.getWidth() - width + 4, height - 4);
		}
		if (blStr != null && !blStr.equals("")) { // draw bl message
			g.setColor(Color.YELLOW);
			width = fm.stringWidth(blStr) + 6; // current string width
			g.fillRect(1, panel.getHeight() - height-1, width, height); // fills rectangle
			g.setColor(Color.BLACK);
			g.drawRect(1, panel.getHeight() - height-1, width, height);
			g.drawString(blStr, 3, panel.getHeight() - 4);
		}
		if (brStr != null && !brStr.equals("")) { // draw br message
			g.setColor(Color.YELLOW);
			width = fm.stringWidth(brStr) + 8; // current string width
			g.fillRect(panel.getWidth() - width - 1, panel.getHeight() - height-1, width, height); // fills rectangle
			g.setColor(Color.BLACK);
			g.drawRect(panel.getWidth() - width - 1, panel.getHeight() - height-1, width, height); // fills rectangle
			g.drawString(brStr, panel.getWidth() - width + 4, panel.getHeight() - 4);
		}
//		/** @j2sNative g.unclip$I(3); */
		g.setFont(oldFont);
		g.dispose();
	}
}
