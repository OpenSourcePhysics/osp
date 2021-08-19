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

// uses Abstract Window Toolkit (awt)
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import javax.swing.JLabel;
import javax.swing.JViewport;
import javax.swing.border.EmptyBorder;
import org.opensourcephysics.tools.FontSizer;

/**
 * PixelRectangle demonstrates how to draw an object using the AWT drawing API.
 *
 * @author Wolfgang Christian, Jan Tobochnik, Harvey Gould
 * @version 1.0 05/16/05
 */
public class MessageDrawable implements Drawable {
	public static final int BOTTOM_LEFT = 0;
	public static final int BOTTOM_RIGHT = 1;
	public static final int TOP_RIGHT = 2;
	public static final int TOP_LEFT = 3;
	String tlStr = null; // "top left";
	String trStr = null; // "top right";
	String blStr = null; // "bottom left";
	String brStr = null; // "bottom right";

	protected Font font;
	protected String fontname = "TimesRoman"; // The logical name of the font to use //$NON-NLS-1$
	protected int fontsize = 12; // The font size
	protected int fontstyle = Font.PLAIN; // The font style
	protected boolean ignoreRepaint = false;
	/**
	 * JLabel mode uses a panel reference
	 */
	private JLabel[] labels;
	private DrawingPanel panel;
	private ComponentListener listener;

	/**
	 * Constructs a MessageDrawable using graphics
	 *
	 */
	public MessageDrawable() {
		this(null);
	}

	/**
	 * Constructs a MessageDrawable optionally creating JLabels 
	 * correctly positioned on a DrawingPanel
	 * @param panel
	 */
	public MessageDrawable(DrawingPanel panel) {
		this.panel = panel;
		if (panel != null) {
			labels = new JLabel[4];
			panel.addComponentListener(listener = new ComponentListener() {

				@Override
				public void componentResized(ComponentEvent e) {
					moveToView((DrawingPanel) e.getComponent());
				}

				@Override
				public void componentMoved(ComponentEvent e) {
					moveToView((DrawingPanel) e.getComponent());
				}

				@Override
				public void componentShown(ComponentEvent e) {
				}

				@Override
				public void componentHidden(ComponentEvent e) {
				}
			});
		}
		font = new Font(fontname, fontstyle, fontsize);
		FontSizer.addListener(FontSizer.PROPERTY_LEVEL, (e)-> {
			if (e.getPropertyName().equals(FontSizer.PROPERTY_LEVEL)) { // $NON-NLS-1$
				int level = ((Integer) e.getNewValue()).intValue();
				setFontLevel(level);
			}
		}); 
	}

	public void setIgnoreRepaint(boolean ignore) {
		ignoreRepaint = ignore;
	}

	/**
	 * Sets the font factor.
	 *
	 * @param factor the factor
	 */
	public void setMessageFont(Font aFont) {
		if (aFont != null)
			font = aFont;
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

	public void refreshGUI() {
		if (labels != null)
			for (int i = 0; i < 4; i++) {
				labels[i] = null;
			}
	}

	/**
	 * Shows a message in a yellow text box in the lower right hand corner.
	 *
	 * @param msg
	 */
	public void setMessage(String msg) {
		setMessage(msg, BOTTOM_RIGHT);
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
		//OSPLog.debug("MessageDrawable.setMessage " + location + " " + msg);
		if (msg != null) {
			if (msg.length() == 0)
				msg = null;
			else
				msg = TeXParser.parseTeX(msg);
		}
		switch (location) {
		case BOTTOM_LEFT: // usually used for mouse coordinates
			blStr = msg;
			break;
		case BOTTOM_RIGHT:
			brStr = msg;
			break;
		case TOP_RIGHT:
			trStr = msg;
			break;
		case TOP_LEFT:
			tlStr = msg;
			break;
		}
		if (panel != null)
			moveToView(panel);
	}

	protected void moveToView(DrawingPanel panel) {
		Rectangle port = panel.findViewRect();
		Dimension d;
		JLabel l;
		l = getLabel(panel, TOP_LEFT, tlStr);
		if (l != null) {
			d = l.getPreferredSize();
			l.setBounds(port.x, port.y, d.width, d.height);
			l.setVisible(true);
		}

		l = getLabel(panel, TOP_RIGHT, trStr);
		if (l != null) {
			d = l.getPreferredSize();
			l.setBounds(port.x + port.width - d.width, port.y, d.width, d.height);
			l.setVisible(true);
		}
		l = getLabel(panel, BOTTOM_LEFT, blStr);
		if (l != null) {
			d = l.getPreferredSize();
			l.setBounds(port.x, port.y + port.height - d.height, d.width, d.height);
			l.setVisible(true);
		}
		l = getLabel(panel, BOTTOM_RIGHT, brStr);
		if (l != null) {
			d = l.getPreferredSize();
			l.setBounds(port.x + port.width - d.width, port.y + port.height - d.height, d.width, d.height);
			l.setVisible(true);
		}
	}

	private JLabel getLabel(DrawingPanel panel, int location, String msg) {
		JLabel l = labels[location];
		if (l == null && msg != null) {
			l = labels[location] = new JLabel(msg);
			panel.add(l);
			// BH by setting the label to be opaque,
			// we allow SwingJS to use a standard CSS background.
			// Otherwise it will paint itself AND force a full panel repaint.
			l.setOpaque(true);
			l.setBackground(Color.yellow);
			// BH If we use a border, then this also forces a 
			// full panel repaint. 
			//l.setBorder(new CompoundBorder(new LineBorder(Color.black, 1), new EmptyBorder(1, 2, 1, 2)));
			l.setBorder(new EmptyBorder(0, 2, 0, 2));
			FontSizer.setFont(l);
		}
		if (msg != null) {
			l.setText(msg);
			return l;
		} 
		if (l != null) {
			l.setVisible(false);
		}
		return null;
	}

	/**
	 * Draws this rectangle using the AWT drawing API. Required to implement the
	 * Drawable interface.
	 *
	 * @param panel DrawingPanel
	 * @param g     Graphics
	 */
	@Override
	public void draw(DrawingPanel panel, Graphics g) {
		if (ignoreRepaint || this.panel != null)
			return;
		Rectangle port = panel.findViewRect();
		g = g.create();
		Font oldFont = g.getFont();
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		int vertOffset = fm.getDescent();
		int height = fm.getAscent() + 1 + vertOffset; // string height
		int width = 0; // string width
		g.setClip(0, 0, panel.getWidth(), panel.getHeight());
		// this method implements the Drawable interface
		if (tlStr != null) { // draw tl message
			g.setColor(Color.YELLOW);
			width = fm.stringWidth(tlStr) + 6; // current string width
			int x = port.x;
			int y = port.y;
			g.fillRect(x, y, width, height); // fills rectangle
			g.setColor(Color.BLACK);
			g.drawRect(x, y, width, height);
			g.drawString(tlStr, x + 4, y + height - vertOffset);
		}

		if (trStr != null) { // draw tr message
			g.setColor(Color.YELLOW);
			width = fm.stringWidth(trStr) + 8; // current string width
			int x = port.x + port.width - width;
			int y = port.y;
			g.fillRect(x - 1, y, width, height); // fills rectangle
			g.setColor(Color.BLACK);
			g.drawRect(x - 1, y, width, height); // fills rectangle
			g.drawString(trStr, x + 4, y + height - vertOffset);
		}
		if (blStr != null) { // draw bl message
			g.setColor(Color.YELLOW);
			width = fm.stringWidth(blStr) + 14; // current string width
			int x = port.x;
			int y = port.y + port.height - height;
			g.fillRect(x, y - 1, width, height); // fills rectangle
			g.setColor(Color.BLACK);
			g.drawRect(x, y - 1, width, height);
			g.drawString(blStr, x + 4, y + height - vertOffset - 1);
		}
		if (brStr != null) { // draw br message
			g.setColor(Color.YELLOW);
			width = fm.stringWidth(brStr) + 8; // current string width
			int x = port.x + port.width - width;
			int y = port.y + port.height - height;
			g.fillRect(x - 1, y - 1, width, height); // fills rectangle
			g.setColor(Color.BLACK);
			g.drawRect(x - 1, y - 1, width, height); // outlines rectangle
			g.drawString(brStr, x + 4, y + height - vertOffset - 1);
		}
		g.setFont(oldFont);
		g.dispose();
	}

	/**
	 * 
	 * Draws this message boxes on a DrawingPanel3D. Required to implement the
	 * Drawable interface.
	 *
	 * @param panel DrawingPanel
	 * @param g     Graphics
	 */
	// public void drawOn3D(DrawingPanel3D panel, Graphics g) {
	public void drawOn3D(Component panel, Graphics g) {
		if (ignoreRepaint)
			return;
		// DB if DrawingPanel is in a scrollpane then use view rect for positioning
		Rectangle port = null;
		if (panel.getParent() instanceof JViewport) {
			port = ((JViewport) panel.getParent()).getViewRect();
		}
		g = g.create();
		Font oldFont = g.getFont();
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		int vertOffset = fm.getDescent();
		int height = fm.getAscent() + 1 + vertOffset; // string height
		int width = 0; // string width
		g.setClip(0, 0, panel.getWidth(), panel.getHeight());
		// this method implements the Drawable interface
		if (tlStr != null && !tlStr.equals("")) { // draw tl message
			g.setColor(Color.YELLOW);
			width = fm.stringWidth(tlStr) + 6; // current string width
			int x = port == null ? 0 : port.x;
			int y = port == null ? 0 : port.y;
			g.fillRect(x, y, width, height); // fills rectangle
			g.setColor(Color.BLACK);
			g.drawRect(x, y, width, height);
			g.drawString(tlStr, x + 4, y + height - vertOffset);
		}

		if (trStr != null) { // draw tr message
			g.setColor(Color.YELLOW);
			width = fm.stringWidth(trStr) + 8; // current string width
			int x = port == null ? panel.getWidth() - width : port.x + port.width - width;
			int y = port == null ? 0 : port.y;
			g.fillRect(x - 1, y, width, height); // fills rectangle
			g.setColor(Color.BLACK);
			g.drawRect(x - 1, y, width, height); // fills rectangle
			g.drawString(trStr, x + 4, y + height - vertOffset);
		}
		if (blStr != null) { // draw bl message
			g.setColor(Color.YELLOW);
			width = fm.stringWidth(blStr) + 6; // current string width
			int x = port == null ? 0 : port.x;
			int y = port == null ? panel.getHeight() - height : port.y + port.height - height;
			g.fillRect(x, y - 1, width, height); // fills rectangle
			g.setColor(Color.BLACK);
			g.drawRect(x, y - 1, width, height);
			g.drawString(blStr, x + 4, y + height - vertOffset - 1);
		}
		if (brStr != null) { // draw br message
			g.setColor(Color.YELLOW);
			width = fm.stringWidth(brStr) + 8; // current string width
			int x = port == null ? panel.getWidth() - width : port.x + port.width - width;
			int y = port == null ? panel.getHeight() - height : port.y + port.height - height;
			g.fillRect(x - 1, y - 1, width, height); // fills rectangle
			g.setColor(Color.BLACK);
			g.drawRect(x - 1, y - 1, width, height); // outlines rectangle
			g.drawString(brStr, x + 4, y + height - vertOffset - 1);
		}
		g.setFont(oldFont);
		g.dispose();

	}

	public void dispose() {
		panel.removeComponentListener(listener);
		listener = null;
		panel = null;
	}

}
