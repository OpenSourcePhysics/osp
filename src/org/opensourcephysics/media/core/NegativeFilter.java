/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

/*
 * The org.opensourcephysics.media.core package defines the Open Source Physics
 * media framework for working with video and other media.
 *
 * Copyright (c) 2017  Douglas Brown and Wolfgang Christian.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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
 * For additional information and documentation on Open Source Physics,
 * please see <http://www.opensourcephysics.org/>.
 */
package org.opensourcephysics.media.core;



/**
 * This is a Filter that produces a negative version of the source.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class NegativeFilter extends Filter {
	// instance fields

	/**
	 * Constructs a NegativeFilter object.
	 */
	public NegativeFilter() {
		refresh();
	}

	@Override
	protected InspectorDlg newInspector() {
		return null;
	}

	@Override
	protected InspectorDlg initInspector() {
		return null;
	}
	
	// _____________________________ private methods _______________________

	/**
	 * Creates the input and output images and ColorConvertOp.
	 *
	 * @param image a new input image
	 */
	@Override
	protected void initializeSubclass() {
		// nothing to do
	}

	/**
	 * Sets the output image pixels to the negative of the input pixels.
	 *
	 * @param input the input image
	 */
	@Override
	protected void setOutputPixels() {
		getPixelsIn();
		getPixelsOut();
		for (int i = 0; i < pixelsIn.length; i++) {
			int pixel = pixelsIn[i];
			int r = 255 - ((pixel >> 16) & 0xff); // neg red
			int g = 255 - ((pixel >> 8) & 0xff); // neg green
			int b = 255 - ((pixel) & 0xff); // neg blue
			pixelsOut[i] = (r << 16) | (g << 8) | b;
		}
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
