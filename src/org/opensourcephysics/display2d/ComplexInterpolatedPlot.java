/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display2d;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import javax.swing.JFrame;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.display.Grid;
import org.opensourcephysics.display.MeasuredImage;
import org.opensourcephysics.js.JSUtil;

import swingjs.api.JSUtilI;

/**
 * ComplexInterpolatedPlot creates an image of a scalar field by interpolating every
 * image pixel to an untabulated point (x,y) in the 2d data.  This interpolation smooths
 * the resulting image.
 *
 * @author     Wolfgang Christian
 * @created    February 2, 2003
 * @version    1.0
 */
public class ComplexInterpolatedPlot extends MeasuredImage implements Plot2D {

  GridData griddata;
  //byte[][] rgbData;
  protected byte[] pixelData;
  Grid grid;
  ComplexColorMapper colorMap;
  boolean autoscaleZ = true;
  private int ampIndex = 0; // amplitude index
  private int reIndex = 1;  // real index
  private int imIndex = 2;  // imaginary index
  private int[] indexes = new int[] { ampIndex, reIndex, imIndex };
  int leftPix, rightPix, topPix, bottomPix;
  int ixsize, iysize;
  double top, left, bottom, right;

  /**
   * Constructs the ComplexInterpolatedPlot using the given 2d datset.
   * @param _griddata
   */
  public ComplexInterpolatedPlot(GridData _griddata) {
    griddata = _griddata;
    colorMap = new ComplexColorMapper(1);
    if(griddata==null) {
      grid = new Grid(1, 1, xmin, xmax, ymin, ymax);
    } else {
      grid = new Grid(griddata.getNx(), griddata.getNy(), xmin, xmax, ymin, ymax);
    }
    grid.setColor(Color.lightGray);
    grid.setVisible(false);
    update();
  }

  /**
   * Gets closest index from the given x  world coordinate.
   *
   * @param x double the coordinate
   * @return int the index
   */
  @Override
public int xToIndex(double x) {
    return griddata.xToIndex(x);
  }

  /**
   * Gets closest index from the given y  world coordinate.
   *
   * @param y double the coordinate
   * @return int the index
   */
  @Override
public int yToIndex(double y) {
    return griddata.yToIndex(y);
  }

  /**
   * Gets the x coordinate for the given index.
   *
   * @param i int
   * @return double the x coordinate
   */
  @Override
public double indexToX(int i) {
    return griddata.indexToX(i);
  }

  /**
   * Gets the y coordinate for the given index.
   *
   * @param i int
   * @return double the y coordinate
   */
  @Override
public double indexToY(int i) {
    return griddata.indexToY(i);
  }

  /**
   * Sets the data to new values.
   *
   * The grid is resized to fit the new data if needed.
   *
   * @param obj
   */
  @Override
public void setAll(Object obj) {
    double[][][] val = (double[][][]) obj;
    copyComplexData(val);
    update();
  }

  /**
   * Sets the values and the scale.
   *
   * The grid is resized to fit the new data if needed.
   *
   * @param obj array of new values
   * @param xmin double
   * @param xmax double
   * @param ymin double
   * @param ymax double
   */
  @Override
public void setAll(Object obj, double xmin, double xmax, double ymin, double ymax) {
    double[][][] val = (double[][][]) obj;
    copyComplexData(val);
    if(griddata.isCellData()) {
      griddata.setCellScale(xmin, xmax, ymin, ymax);
    } else {
      griddata.setScale(xmin, xmax, ymin, ymax);
    }
    update();
  }

  private void copyComplexData(double vals[][][]) {
    if((griddata!=null)&&!(griddata instanceof ArrayData)) {
      throw new IllegalStateException("SetAll only supports ArrayData for data storage."); //$NON-NLS-1$
    }
    if((griddata==null)||(griddata.getNx()!=vals[0].length)||(griddata.getNy()!=vals[0][0].length)) {
      griddata = new ArrayData(vals[0].length, vals[0][0].length, 3);
      setGridData(griddata);
    }
    double[][] mag = griddata.getData()[0];
    double[][] reData = griddata.getData()[1];
    double[][] imData = griddata.getData()[2];
    // current grid has correct size
    int ny = vals[0][0].length;
    for(int i = 0, nx = vals[0].length; i<nx; i++) {
      System.arraycopy(vals[0][i], 0, reData[i], 0, ny);
      System.arraycopy(vals[1][i], 0, imData[i], 0, ny);
      for(int j = 0; j<ny; j++) {
        mag[i][j] = Math.sqrt(vals[0][i][j]*vals[0][i][j]+vals[1][i][j]*vals[1][i][j]);
      }
    }
  }

  /**
   * Gets the GridData object.
   * @return GridData
   */
  @Override
public GridData getGridData() {
    return griddata;
  }

  /**
   * Sets the data storage to the given value.
   *
   * @param _griddata the new data storage
   */
  @Override
public void setGridData(GridData _griddata) {
    griddata = _griddata;
    if(griddata==null) {
      return;
    }
    Grid newgrid = new Grid(griddata.getNx(), griddata.getNy());
    newgrid.setColor(Color.lightGray);
    if(grid!=null) {
      newgrid.setColor(grid.getColor());
      newgrid.setVisible(grid.isVisible());
    } else {
      newgrid.setColor(Color.lightGray);
    }
    grid = newgrid;
  }

  /**
   * Sets the indexes for the data components that will be plotted.
   *
   * Indexes determine the postion of the amplitude, real-component, and imaginary-component
   * in the data array.
   *
   * @param indexes the sample-component indexes
   */
  @Override
public void setIndexes(int[] indexes) {
    ampIndex = indexes[0];
    reIndex = indexes[1];
    imIndex = indexes[2];
    indexes = new int[] { ampIndex, reIndex, imIndex };
  }

  /**
   * Sets the autoscale flag and the floor and ceiling values for the colors.
   *
   * If autoscaling is true, then the min and max values of z are span the colors.
   *
   * If autoscaling is false, then floor and ceiling values limit the colors.
   * Values below min map to the first color; values above max map to the last color.
   *
   * @param isAutoscale
   * @param ceil
   */
  public void setAutoscaleZ(boolean isAutoscale, double ceil) {
    autoscaleZ = isAutoscale;
    if(autoscaleZ) {
      update();
    } else {
      colorMap.setScale(ceil);
    }
  }

  /**
   * Sets the autoscale flag and the floor and ceiling values for the colors.
   *
   * If autoscaling is true, then the min and max values of z span the colors.
   *
   * If autoscaling is false, then floor and ceiling values limit the colors.
   * Values below min map to the first color; values above max map to the last color.
   *
   * @param isAutoscale
   * @param floor
   * @param ceil
   */
  @Override
public void setAutoscaleZ(boolean isAutoscale, double floor, double ceil) {
    setAutoscaleZ(isAutoscale, ceil);
  }
  
  /**
   * Forces the z-scale to be symmetric about zero.
   * Not applicable in complex map because amplitude is always positive
   *
   * @param symmetric
   */
  @Override
public void setSymmetricZ(boolean symmetric){
	  
  }
  
  /**
   * Gets the symmetric z flag.  
   */
  @Override
public boolean isSymmetricZ(){
	  return false;
  }

  /**
   * Gets the autoscale flag for z.
   *
   * @return boolean
   */
  @Override
public boolean isAutoscaleZ() {
    return autoscaleZ;
  }

  /**
   * Gets the floor for scaling the z data.
   * @return double
   */
  @Override
public double getFloor() {
    return 0;
  }

  /**
   * Gets the ceiling for scaling the z data.
   * @return double
   */
  @Override
public double getCeiling() {
    return colorMap.getCeil();
  }

  /**
   * Sets the floor and ceiling colors.
   *
   * @param floorColor
   * @param ceilColor
   */
  @Override
public void setFloorCeilColor(Color floorColor, Color ceilColor) {
    colorMap.setCeilColor(ceilColor);
  }

  /**
   * Sets the show gridline option.
   *
   * @param  showGrid
   */
  @Override
public void setShowGridLines(boolean showGrid) {
    grid.setVisible(showGrid);
  }

  /**
   *  Sets the color for grid line boundaries
   *
   * @param  c
   */
  @Override
public void setGridLineColor(Color c) {
    grid.setColor(c);
  }

  /**
   * Updates the buffered image using the data array.
   */
  @Override
public synchronized void update() {
    if(autoscaleZ&&(griddata!=null)) {
      griddata.getZRange(ampIndex, minmax);
      colorMap.setScale(minmax[1]);
    }
    recolorImage();
  }

  /**
 * Expands the z scale so as to enhance values close to zero.
 *
 * @param expanded boolean
 * @param expansionFactor double
 */
  @Override
public void setExpandedZ(boolean expanded, double expansionFactor) {
    if(expanded&&(expansionFactor>0)) {
      ZExpansion zMap = new ZExpansion(expansionFactor);
      colorMap.setZMap(zMap);
    } else {
      colorMap.setZMap(null);
    }
  }

  /**
   * used to indicate to the ColorMapper what kind of pixels to write, 
   * TYPE_4BYTE_ABGR for Java or TYPE_4BYTE_HTML5 for JavaScript
   */
  protected int imageType;

  /**
   * Checks if the image is the correct size.
   */
  protected void checkImage(DrawingPanel panel) {
    int lPix, rPix, bPix, tPix;
    if(griddata.isCellData()) {
      double dx = griddata.getDx();
      double dy = griddata.getDy();
      lPix = panel.xToPix(griddata.getLeft()-dx/2);
      rPix = panel.xToPix(griddata.getRight()+dx/2);
      bPix = panel.yToPix(griddata.getBottom()+dy/2);
      tPix = panel.yToPix(griddata.getTop()-dy/2);
    } else {
      lPix = panel.xToPix(griddata.getLeft());
      rPix = panel.xToPix(griddata.getRight());
      bPix = panel.yToPix(griddata.getBottom());
      tPix = panel.yToPix(griddata.getTop());
    }

    leftPix = Math.min(lPix, rPix);
    rightPix = Math.max(lPix, rPix);
    bottomPix = Math.max(bPix, tPix);
    topPix = Math.min(bPix, tPix);
    ixsize = rightPix-leftPix+1;
    iysize = bottomPix-topPix+1;
    leftPix = Math.max(0, leftPix);
    rightPix = Math.min(rightPix, panel.getWidth());
    topPix = Math.max(0, topPix);
    bottomPix = Math.min(bottomPix, panel.getHeight());
    int height = bottomPix-topPix+1;
    int width = rightPix-leftPix+1;
    boolean haveImage = (image!=null)&&(image.getWidth()==width)&&(image.getHeight()==height);
    if(haveImage&&(left==panel.pixToX(leftPix))&&(top==panel.pixToY(topPix))&&(bottom==panel.pixToX(bottomPix))&&(right==panel.pixToY(rightPix))) {
      return; // image exists, has the correct location, and is the correct size
    }
    left = panel.pixToX(leftPix);
    top = panel.pixToY(topPix);
    bottom = panel.pixToX(bottomPix);
    right = panel.pixToY(rightPix);
    if(haveImage) {
      recolorImage();
      return; // image exists and is the correct size so recolor it
    }
    int size = height*width;
    if(size<4) {
      image = null;
      return;
    }
    OSPLog.finer("ComplexInterpolatedPlot image created with width="+width+" and height="+height); //$NON-NLS-1$ //$NON-NLS-2$


    // BH: While this does work, there is a much simpler way:
    // Just select Java's TYPE_4BYTE_ABGR option 
    // We do have to set the pixels opaque then, but we
    // also get to define a buffer that is identical to HTML5
    // using type -6. 
//    ComponentColorModel ccm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] {8, 8, 8}, false, // hasAlpha
//      false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
//    BandedSampleModel csm = new BandedSampleModel(DataBuffer.TYPE_BYTE, width, height, width, new int[] {0, 1, 2}, new int[] {0, 0, 0});
//    rgbData = new byte[3][size];
//    DataBuffer databuffer = new DataBufferByte(rgbData, size);
//    WritableRaster raster = Raster.createWritableRaster(csm, databuffer, new Point(0, 0));
//    image = new BufferedImage(ccm, raster, false, null);

    
    imageType = 
    		(JSUtil.isJS ? JSUtilI.TYPE_4BYTE_HTML5
    				: BufferedImage.TYPE_4BYTE_ABGR);
    image = new BufferedImage(width, height, imageType);
    pixelData = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
    update();
  }

	/**
	 * Recolors the image pixels using the data array.
	 */
	protected void recolorImage() {
		// use references for thread safety
		GridData griddata = this.griddata;
		// byte[][] componentData = this.rgbData;
		BufferedImage image = this.image;
		if (griddata == null) {
			return;
		}
		if (griddata.isCellData()) {
			double dx = griddata.getDx();
			double dy = griddata.getDy();
			xmin = griddata.getLeft() - dx / 2;
			xmax = griddata.getRight() + dx / 2;
			ymin = griddata.getBottom() + dy / 2;
			ymax = griddata.getTop() - dy / 2;
		} else {
			xmin = griddata.getLeft();
			xmax = griddata.getRight();
			ymin = griddata.getBottom();
			ymax = griddata.getTop();
		}
		grid.setMinMax(xmin, xmax, ymin, ymax);
		if (image == null) {
			return;
		}
		if (pixelData.length != image.getWidth() * image.getHeight() * 4) {
			return;
		}
		// byte[] rgb = new byte[3];
		double y = top;
		double dx = (xmax - xmin) / (ixsize - 1);
		double dy = (ymin - ymax) / (iysize - 1);
		if (griddata.getDx() < 0) {
			dx = -dx;
		}
		if (griddata.getDy() > 0) {
			dy = -dy;
		}
		writeToRaster(left, y, dx, dy);
	}

	private double[] samples = new double[3];
	private byte[] tempRGB = new byte[3];

	protected void writeToRaster(double x0, double y, double dx, double dy) {
		int width = image.getWidth();
		int height = image.getHeight();
		byte[] pixels = pixelData;
		boolean isABGR = (imageType == BufferedImage.TYPE_4BYTE_ABGR);
		for (int j = 0; j < height; j++, y += dy) {
			double x = left;
			for (int i = 0; i < width; i++, x += dx) {
				int pt = ((dy < 0) ? j * width + i : (height - j - 1) * width + i) << 2;
				byte[] ret = colorMap.sampleToPixel(griddata.interpolate(x, y, indexes, samples), tempRGB);
				// note that -1 here will become UInt8 255 for the canvas by anding with 0xFF
				if (isABGR) {
					// Java BufferedImage.TYPE_4BYTE_ABGR
					pixels[pt++] = -1;//a;
					pixels[pt++] = ret[2];//b;
					pixels[pt++] = ret[1];//g;
					pixels[pt++] = ret[0];//r;
				} else {
					// SwingJS BufferedImage.TYPE_4BYTE_HTML5
					pixels[pt++] = ret[0];//r;
					pixels[pt++] = ret[1];//g;
					pixels[pt++] = ret[2];//b;
					pixels[pt++] = -1;//a;
				}
			}
		}
	}

/**
   * Determines the palette type that will be used.
   * Not implemented.   Only one palette type.
   * @param type
   */
  @Override
public void setPaletteType(int type) {
    // Not implemented.   Only one palette type.
  }

  /**
   * Sets the colors that will be used between the floor and ceiling values.
   * Not implemented.   Color always maps to phase.
   * @param colors
   */
  @Override
public void setColorPalette(Color[] colors) {
    // Not implemented.   Color always maps to phase.
  }

  /**
   * Shows a legend of phase angle and color.
   */
  @Override
public JFrame showLegend() {
    return colorMap.showLegend();
  }

  @Override
public boolean isMeasured() {
    return true; // image will always be created
  }

  /**
   * Draws the image and the grid.
   * @param panel
   * @param g
   */
  @Override
public synchronized void draw(DrawingPanel panel, Graphics g) {
    if(!visible||(griddata==null)) {
      return;
    }
    checkImage(panel);
    if(image!=null) {
      g.drawImage(image, leftPix, topPix, panel);
      //System.out.println("Drawing complex interpolated plot");
    }
    grid.draw(panel, g);
  }

  /**
   * Gets an XML.ObjectLoader to save and load data for this program.
   *
   * @return the object loader
   */
  public static XML.ObjectLoader getLoader() {
    return new Plot2DLoader() {
      @Override
	public Object createObject(XMLControl control) {
        return new ComplexInterpolatedPlot(null);
      }

    };
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
