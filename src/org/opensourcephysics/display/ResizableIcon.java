/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */
package org.opensourcephysics.display;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.opensourcephysics.tools.FontSizer;

/**
 * An <CODE>Icon</CODE> that can be resized.
 * 
 * @author Doug Brown
 */
public class ResizableIcon implements Icon {

	// BH 2020.03.25 - made base fields final and all private for sanity.
	// If necessary, could add getters and setters for those.
	// But SwingJS may be problematic with that. Untested.

	private final int baseWidth, baseHeight;
	private BufferedImage baseImage;
	private final Icon icon;
	private int sizeFactor, w, h;
	private int fixedSizeFactor = 0;
	private boolean isDrawn;

	/**
	 * Creates a <CODE>ResizableIcon</CODE> from the specified URL.
	 *
	 * @param location the URL for the image
	 */
	public ResizableIcon(URL location) {
		this(new ImageIcon(location));
	}

	/**
	 * Creates a <CODE>ResizableIcon</CODE> from the specified Icon.
	 * 
	 * @param the icon to resize
	 */
	public ResizableIcon(Icon icon) {
		// prevent nesting resizable icons
		while (icon instanceof ResizableIcon) {
			icon = ((ResizableIcon) icon).icon;
		}
		this.icon = icon;
		if (icon == null) {
			baseWidth = 0;
			baseHeight = 0;
		} else {
			baseWidth = icon.getIconWidth();
			baseHeight = icon.getIconHeight();
		}
		isDrawn = !(icon instanceof ImageIcon);
	}

	@Override
	public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
		if (icon == null || baseHeight < 0 || baseWidth < 0) {
			return;
		}
		if (baseImage == null
// BH baseImage is final -- or should be??
//				|| baseImage.getWidth() != baseWidth 
//				|| baseImage.getHeight() != baseHeight
		) {
			baseImage = new BufferedImage(baseWidth, baseHeight, BufferedImage.TYPE_INT_ARGB);
		}
		Graphics2D g2 = baseImage.createGraphics();
		// BH 2020.03.25 It is not necessary to clear the image first -- image will be
		// AlphaComposite.SrcOver by default
		// BH 2020.10.11 coordinate system menu does not show proper checkbox icons
		// after locking.
		// If an icon is not an image icon, then we need to clear the surface.
		if (isDrawn) {
			g2.setComposite(AlphaComposite.Clear);
			g2.fillRect(0, 0, baseWidth, baseHeight);
			g2.setComposite(AlphaComposite.SrcOver);
		}
		icon.paintIcon(c, g2, 0, 0);
		g2.dispose();
		g.drawImage(baseImage, x, y, getIconWidth(), getIconHeight(), c);
	}

	@Override
	public int getIconWidth() {
		if (fixedSizeFactor <= 0)
			setSizeFactor(FontSizer.getIntegerFactor());
		return w;
	}

	@Override
	public int getIconHeight() {
//  	setSizeFactor(FontSizer.getIntegerFactor()); // only needed for width since called first?
		return h;
	}

	/**
	 * Gets the base icon which is resized by this ResizableIcon.
	 * 
	 * @return the base icon
	 */
	public Icon getBaseIcon() {
		return icon;
	}

	/**
	 * Sets the size factor.
	 * 
	 * @param factor the desired factor
	 */
	private void setSizeFactor(int factor) {
		if (factor != sizeFactor) {
			sizeFactor = factor;
			w = baseWidth * factor;
			h = baseHeight * factor;
		}
	}

	/**
	 * Sets a fixed size factor. Factors <= 0 unfixes it.
	 * 
	 * @param factor the desired fixed factor
	 */
	public void setFixedSizeFactor(int factor) {
		if (factor != fixedSizeFactor) {
			fixedSizeFactor = factor;
			w = baseWidth * factor;
			h = baseHeight * factor;
		}
	}

}
