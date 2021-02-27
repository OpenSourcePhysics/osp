/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.BitSet;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLLoader;

/**
 * A Dataset that can highlight selected points.
 *
 * @author Doug Brown
 * @created Dec 14 2005
 * @version 1.0
 */
@SuppressWarnings("serial")
public class HighlightableDataset extends Dataset implements Interactive {
	// instance fields
	private BitSet highlighted;
	private BitSet previous;
	private Color highlightColor = new Color(255, 255, 0, 128);
	private Rectangle2D.Double[] hitShapes = new Rectangle2D.Double[16];
	private int hitIndex = -1;
	private final double[][] screenCoordinates = new double[2][];
	private int previousLen;

	/**
	 * Default constructor.
	 */
	public HighlightableDataset() {
		super();
	}

	/**
	 * Constructor specifying the marker color.
	 *
	 * @param markerColor marker color
	 */
	public HighlightableDataset(Color markerColor) {
		super(markerColor);
	}

	/**
	 * Clear all data from this Dataset.
	 */
	@Override
	public void clear() {
		previousLen = xpoints.length;
		super.clear();
		if (previous == null) {
			// when called from super()
			previous = new BitSet();
			highlighted = new BitSet();
		}
		previous.clear();
		previous.or(highlighted);
		highlighted.clear();
	}

	/**
	 * Restores previous highlights.
	 */
	public void restoreHighlights() {
		if (previous != null && previousLen == xpoints.length) {
			highlighted.clear();
			highlighted.or(previous);
		}
	}

	/**
	 * Clears highlights.
	 */
	public void clearHighlights() {
		highlighted.clear();
	}

	/**
	 * Sets the highlighted flag for the specified point.
	 *
	 * @param i         the array index
	 * @param highlight true to highlight the point
	 */
	public void setHighlighted(int i, boolean highlight) {
		highlighted.set(i, highlight);
	}

	/**
	 * Gets the highlighted flag for the specified point.
	 *
	 * @param n the array index
	 * @return true if point is highlighted
	 */
	public boolean isHighlighted(int n) {
		return highlighted.get(n);
	}

	/**
	 * Sets the highlight color.
	 *
	 * @param color the color
	 */
	public void setHighlightColor(Color color) {
		highlightColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 128);
	}

	/**
	 * Draw this Dataset in the drawing panel.
	 *
	 * @param drawingPanel the drawing panel
	 * @param g            the graphics
	 */
	@Override
	public void draw(DrawingPanel drawingPanel, Graphics g) {
		if (!drawable())
			return;
		setScreenCoordinates(drawingPanel);
		int offset = getMarkerSize() + 4;
		setHitShapes(offset);
		Graphics2D g2 = (Graphics2D) g.create();
		drawClip(g2, drawingPanel, offset);
		super.drawData(drawingPanel, g2);
		g2.setColor(highlightColor);
		for (int i = highlighted.nextSetBit(0); i >= 0; i = highlighted.nextSetBit(i + 1)) {
			if (i >= hitShapes.length) {
				//System.out.println("HighlightableDataset.draw Danger, Will Robinson!");
			} else if (hitShapes[i] != null) {
				g2.fill(hitShapes[i]);
			}
		}
		g2.dispose();
	}

	private void setScreenCoordinates(DrawingPanel drawingPanel) {
		double[] xValues = getXPointsRaw();
		double[] yValues = getYPointsRaw(); // can't be shifted
		if (screenCoordinates[0] == null || screenCoordinates[0].length != index) {
			screenCoordinates[0] = new double[index];
			screenCoordinates[1] = new double[index];
		}
		for (int i = 0; i < index; i++) {
			if (Double.isNaN(yValues[i])) {
				screenCoordinates[1][i] = Double.NaN;
			} else {
				screenCoordinates[0][i] = drawingPanel.xToPix(xValues[i]);
				screenCoordinates[1][i] = drawingPanel.yToPix(yValues[i]);
			}
		}
	}

	/**
	 * Create the rectangular highlights.
	 * @param offset
	 */
	private void setHitShapes(int offset) {
		int edge = 2 * offset;
		if (hitShapes.length < index)
			hitShapes = new Rectangle2D.Double[index];
		for (int i = 0; i < index; i++) {
			double yp = screenCoordinates[1][i];
			if (Double.isNaN(yp)) {
				hitShapes[i] = null;
				continue;
			}
			double xp = screenCoordinates[0][i];
			if (hitShapes[i] == null)
				hitShapes[i] = new Rectangle2D.Double(xp - offset, yp - offset, edge, edge);
			else
				hitShapes[i].setRect(xp - offset, yp - offset, edge, edge);
		}
	}

	/**
	 * Returns the Interactive object at a specified pixel position. Implements
	 * Interactive.
	 *
	 * @param panel the drawing panel
	 * @param xpix  the x pixel position on the panel
	 * @param ypix  the y pixel position on the panel
	 * @return the object
	 */
	@Override
	public Interactive findInteractive(DrawingPanel panel, int xpix, int ypix) {
		// return hits only within active plot area
		Dimension dim = panel.getSize();
		int l = panel.getLeftGutter();
		int r = panel.getRightGutter();
		if ((xpix < l) || (xpix > dim.width - r)) {
			return null;
		}
		int t = panel.getTopGutter();
		int b = panel.getBottomGutter();
		if ((ypix < t) || (ypix > dim.height - b)) {
			return null;
		}
		hitIndex = -1;
		for (int i = 0, n = Math.min(hitShapes.length,  index); i < n; i++) {
			if (hitShapes[i] != null && hitShapes[i].contains(xpix, ypix)) {
				hitIndex = i;
				return this;
			}
		}
		return null;
	}

	/**
	 * Gets the most recent hit index.
	 *
	 * @return the hit index
	 */
	public int getHitIndex() {
		return hitIndex;
	}

	/**
	 * Gets the screen coordinates of all data points.
	 *
	 * @return screen coordinates
	 */
	public double[][] getScreenCoordinates() {
		return screenCoordinates;
	}

	/**
	 * Implements Interactive.
	 *
	 * @param enabled ignored
	 */
	@Override
	public void setEnabled(boolean enabled) {
	}

	/**
	 * Implements Interactive.
	 *
	 * @return true
	 */
	@Override
	public boolean isEnabled() {
		return true;
	}

	/**
	 * Implements Interactive.
	 *
	 * @param x ignored
	 * @param y ignored
	 */
	@Override
	public void setXY(double x, double y) {
	}

	/**
	 * Implements Interactive.
	 *
	 * @param x ignored
	 */
	@Override
	public void setX(double x) {
	}

	/**
	 * Implements Interactive.
	 *
	 * @param y ignored
	 */
	@Override
	public void setY(double y) {
	}

	/**
	 * Implements Interactive.
	 *
	 * @return the x value at the current hit index, or Double.NaN if none
	 */
	@Override
	public double getX() {
		return (hitIndex >= 0 ? xpoints[hitIndex] : Double.NaN);
	}

	/**
	 * Implements Interactive.
	 *
	 * @return the y value at the current hit index, or Double.NaN if none
	 */
	@Override
	public double getY() {
		return (hitIndex >= 0 ? ypoints[hitIndex] : Double.NaN);
	}

	/**
	 * Returns the XML.ObjectLoader for this class.
	 *
	 * @return the object loader
	 */
	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

	/**
	 * A class to save and load Dataset data in an XMLControl.
	 */
	private static class Loader extends XMLLoader {
		@Override
		public void saveObject(XMLControl control, Object obj) {
			XML.getLoader(Dataset.class).saveObject(control, obj);
			HighlightableDataset data = (HighlightableDataset) obj;
			control.setValue("highlighted", toArray(data.highlighted)); //$NON-NLS-1$
		}

		protected boolean[] toArray(BitSet bs) {
			boolean[] b = new boolean[bs.length()];
			for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) 
				b[i] = true;
			return b;
		}


		@Override
		public Object createObject(XMLControl control) {
			return new HighlightableDataset();
		}

		@Override
		public Object loadObject(XMLControl control, Object obj) {
			XML.getLoader(Dataset.class).loadObject(control, obj);
			HighlightableDataset data = (HighlightableDataset) obj;
			boolean[] highlighted = (boolean[]) control.getObject("highlighted"); //$NON-NLS-1$
			if (highlighted != null) {
				for (int i = highlighted.length; --i >= 0;)
				data.highlighted.set(i, highlighted[i]);
			}
			return data;
		}

	}

	public void setHighlights(BitSet bs) {
		highlighted.clear();
		highlighted.or(bs);
	}

	public BitSet getHighlightedBS() {
		return highlighted;
	}

	public int getHighlightCount() {
		return highlighted.cardinality();
	}

	public int getValidYCount() {
		return index - getNaNCount(ypoints, index);
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
