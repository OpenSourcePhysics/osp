/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display2d;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JFrame;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.js.JSUtil;

/**
 * A CellLattice that displays an array where each array element can assume one
 * of 256 values.
 *
 * Values can be set between -128 and 127. Because byte values larger than 127
 * overflow to negative, values can also be set between 0 and 255. The lattice
 * is drawn as an array of rectangles to distinguish between the two possible
 * values.
 *
 * @author Wolfgang Christian
 * @created July 3, 2005
 * @version 1.0
 */
@SuppressWarnings("nls")
public class CellLattice implements ByteLattice {

	public interface OSLattice extends ByteLattice {

		SiteLattice createSiteLattice();
		void setBlock(int ix_offset, int iy_offset, int[][] val);
	}

	OSLattice lattice = null;
	static boolean isMac;
	static {
		try {
			// BH 2020.03.04 don't use CellLatticeOSX
			isMac = ((JSUtil.isJS ? "" : System.getProperty("os.name", "")).indexOf("Mac") >= 0);
		} catch (Exception ex) {
		}
	}

	/**
	 * Constructor CellLattice
	 */
	public CellLattice() {
		this(1,1);
	}

	/**
	 * Constructor CellLattice
	 * 
	 * @param nx
	 * @param ny
	 */
	public CellLattice(int nx, int ny) {
		lattice = (isMac? new CellLatticeOSX(nx, ny) : new CellLatticePC(nx, ny));
	}

	@Override
	public double getXMin() {
		return lattice.getXMin();
	}

	@Override
	public double getXMax() {
		return lattice.getXMax();
	}

	@Override
	public double getYMin() {
		return lattice.getYMin();
	}

	@Override
	public double getYMax() {
		return lattice.getYMax();
	}

	@Override
	public boolean isMeasured() {
		return lattice.isMeasured();
	}

	@Override
	public void draw(DrawingPanel panel, Graphics g) {
		lattice.draw(panel, g);
	}

	@Override
	public int getNx() {
		return lattice.getNx();
	}

	@Override
	public int getNy() {
		return lattice.getNy();
	}

	@Override
	public int indexFromPoint(double x, double y) {
		return lattice.indexFromPoint(x, y);
	}

	@Override
	public int xToIndex(double x) {
		return lattice.xToIndex(x);
	}

	@Override
	public int yToIndex(double y) {
		return lattice.yToIndex(y);
	}

	@Override
	public byte getValue(int ix, int iy) {
		return lattice.getValue(ix, iy);
	}

	@Override
	public void setValue(int ix, int iy, byte val) {
		lattice.setValue(ix, iy, val);
	}

	@Override
	public void randomize() {
		lattice.randomize();
	}

	@Override
	public void resizeLattice(int nx, int ny) {
		lattice.resizeLattice(nx, ny);
	}

	/**
	 * Sets the lattice values and scale.
	 *
	 * The lattice is resized to fit the new data if needed.
	 *
	 * @param val  int[][] the new values
	 * @param xmin double
	 * @param xmax double
	 * @param ymin double
	 * @param ymax double
	 */
	@Override
	public void setAll(byte val[][], double xmin, double xmax, double ymin, double ymax) {
		lattice.setAll(val, xmin, xmax, ymin, ymax);
	}

	@Override
	public void setBlock(int ix_offset, int iy_offset, byte[][] val) {
		lattice.setBlock(ix_offset, iy_offset, val);
	}

	@Override
	public void setBlock(byte[][] val) {
		lattice.setBlock(val);
	}

	@Override
	public void setCol(int ix, int iy_offset, byte[] val) {
		lattice.setCol(ix, iy_offset, val);
	}

	@Override
	public void setRow(int iy, int ix_offset, byte[] val) {
		lattice.setRow(iy, ix_offset, val);
	}

	@Override
	public void setShowGridLines(boolean show) {
		lattice.setShowGridLines(show);
	}

	@Override
	public void setGridLineColor(Color c) {
		lattice.setGridLineColor(c);
	}

	@Override
	public JFrame showLegend() {
		return lattice.showLegend();
	}

	@Override
	public void setVisible(boolean isVisible) {
		lattice.setVisible(isVisible);
	}

	@Override
	public void setColorPalette(Color[] colors) {
		lattice.setColorPalette(colors);
	}

	@Override
	public void setIndexedColor(int i, Color color) {
		lattice.setIndexedColor(i, color);
	}

	@Override
	public void setMinMax(double xmin, double xmax, double ymin, double ymax) {
		lattice.setMinMax(xmin, xmax, ymin, ymax);
	}

	/**
	 * Creates a new SiteLattice containing the same data as this lattice.
	 */
	public SiteLattice createSiteLattice() {
		return lattice.createSiteLattice();
	}

	/**
	 * Sets a block of cells using integer values.
	 *
	 * @param ix_offset int
	 * @param iy_offset int
	 * @param val       int[][]
	 */
	public void setBlock(int ix_offset, int iy_offset, int val[][]) {
		lattice.setBlock(ix_offset, iy_offset, val);
	}

	@Override
	public void setXMin(double xmin) {
		lattice.setXMin(xmin);
	}

	@Override
	public void setXMax(double xmax) {
		lattice.setXMax(xmax);
	}

	@Override
	public void setYMin(double ymin) {
		lattice.setYMin(ymin);
	}

	@Override
	public void setYMax(double ymax) {
		lattice.setYMax(ymax);
	}

	@Override
	public void createDefaultColors() {
		lattice.createDefaultColors();
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
