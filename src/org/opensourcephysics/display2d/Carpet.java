/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display2d;

/**
 * Real version of ComplexCarpet
 * 
 * not tested
 * 
 *
 */
public class Carpet extends InterpolatedPlot {
	/**
	 * Constructor Carpet
	 * 
	 * @param griddata
	 */
	public Carpet(GridData griddata) {
		super(griddata);
		setShowGridLines(false);
	}

	public void setTopRow(double[][] line) {
		if (image == null) {
			return;
		}
		if (pixelData.length != image.getWidth() * image.getHeight()) {
			return;
		}
		if (griddata instanceof ArrayData) { // replace top row of array
			for (int c = 0; c < line.length; c++) {
				double[][] data = griddata.getData()[c];
				int len = data[0].length - 1;
				for (int ix = 0, nx = data.length; ix < nx; ix++) {
					System.arraycopy(data[ix], 0, data[ix], 1, len);
					data[ix][0] = line[c][ix];
				}
			}
		} else {
			double[][][] data = griddata.getData();
			for (int ix = 0, nx = data.length; ix < nx; ix++) {
				int len = line.length;
				for (int ny = data[0].length - 1, iy = ny; iy > 0; iy--) {
					System.arraycopy(data[ix][iy - 1], 2, data[ix][iy], 2, len);
				}
				for (int c = 0; c < len; c++) {
					data[ix][0][2 + c] = line[c][ix];
				}
			}
		}
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();
		double x0 = griddata.getLeft();
		double y = griddata.getTop();
		double dx = (griddata.getRight() - x0) / (imageWidth - 1);
		double dy = (griddata.getBottom() - y) / (imageHeight - 1);
		int nr = 1 + (int) Math.abs(griddata.getDy() / dy);
		int offset = nr * imageWidth;
		int length = imageWidth * imageHeight * 4 - offset;
		System.arraycopy(pixelData, 0, pixelData, offset, length);
		writeToRaster(x0, y, dx, dy);
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
