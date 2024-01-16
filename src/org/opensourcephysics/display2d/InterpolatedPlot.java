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

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.display.Grid;
import org.opensourcephysics.display.MeasuredImage;
import org.opensourcephysics.display.OSPRuntime;

import swingjs.api.JSUtilI;

/**
 * InterpolatedPlot creates an image of a scalar field by interpolating every
 * image pixel to an untabulated point (x,y) in the 2d data.  This interpolation smooths
 * the resulting image.
 *
 * @author     Wolfgang Christian
 * @created    February 2, 2003
 * @version    1.0
 */
public class InterpolatedPlot extends MeasuredImage implements Plot2D {
  GridData griddata;
  Grid grid;
  ColorMapper colorMap;
  boolean autoscaleZ = true;
  boolean symmetricZ = false;
  int ampIndex = 0; // amplitude index
  int leftPix, rightPix, topPix, bottomPix;
  int ixsize, iysize;
  double top, left, bottom, right;
protected int imageType;
protected byte[] pixelData;

  /**
   * Constructs an InterpolatedPlot without data.
   */
  public InterpolatedPlot() {
    this(null);
  }

  /**
   * Constructs the InterpolatedPlot using the given data storage.
   * @param _griddata
   */
  public InterpolatedPlot(GridData _griddata) {
    griddata = _griddata;
    colorMap = new ColorMapper(100, -1, 1, ColorMapper.SPECTRUM);
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
   * Gets the GridData object.
   * @return GridData
   */
  @Override
public GridData getGridData() {
    return griddata;
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
   * Sets the data to new values.
   *
   * The grid is resized to fit the new data if needed.
   *
   * @param obj
   */
  @Override
public void setAll(Object obj) {
    double[][] val = (double[][]) obj;
    copyData(val);
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
    double[][] val = (double[][]) obj;
    copyData(val);
    if(griddata.isCellData()) {
      griddata.setCellScale(xmin, xmax, ymin, ymax);
    } else {
      griddata.setScale(xmin, xmax, ymin, ymax);
    }
    update();
  }

  private void copyData(double val[][]) {
    if((griddata!=null)&&!(griddata instanceof ArrayData)) {
      throw new IllegalStateException("SetAll only supports ArrayData for data storage."); //$NON-NLS-1$
    }
    if((griddata==null)||(griddata.getNx()!=val.length)||(griddata.getNy()!=val[0].length)) {
      griddata = new ArrayData(val.length, val[0].length, 1);
      setGridData(griddata);
    }
    double[][] data = griddata.getData()[0];
    int ny = data[0].length;
    for(int i = 0, nx = data.length; i<nx; i++) {
      System.arraycopy(val[i], 0, data[i], 0, ny);
    }
  }

  /**
   * Sets the data to the given griddata.
   *
   * @param _griddata
   */
  @Override
public void setGridData(GridData _griddata) {
    griddata = _griddata;
    if(this.griddata==null) {
      return;
    }
    int nx = griddata.getNx();
    int ny = griddata.getNy();
    Grid newGrid = new Grid(nx, ny, xmin, xmax, ymin, ymax);
    if(grid!=null) {
      newGrid.setColor(grid.getColor());
      newGrid.setVisible(grid.isVisible());
    }
    grid = newGrid;
    update();
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
   * @param floor
   * @param ceil
   */
  @Override
public void setAutoscaleZ(boolean isAutoscale, double floor, double ceil) {
    autoscaleZ = isAutoscale;
    if(!autoscaleZ) {
      colorMap.setScale(floor, ceil);
    }
    update(); // recolor the image with the new scale
  }
  
  /**
   * Forces the z-scale to be symmetric about zero.
   * Forces zmax to be positive and zmin=-zmax when in autoscale mode.
   *
   * @param symmetric
   */
  @Override
public void setSymmetricZ(boolean symmetric){
	  symmetricZ=symmetric;
  }
  
  /**
   * Gets the symmetric z flag.  
   */
  @Override
public boolean isSymmetricZ(){
	  return symmetricZ;
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
    return colorMap.getFloor();
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
   * Sets the show grid option.
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
   * Sets the indexes for the data component that will be plotted.
   *
   * @param indexes the sample-component
   */
  @Override
public void setIndexes(int[] indexes) {
    ampIndex = indexes[0];
  }

  /**
   * Determines the palette type that will be used.
   * @param type
   */
  @Override
public void setPaletteType(int type) {
    colorMap.setPaletteType(type);
  }

  /**
   * Sets the colors that will be used between the floor and ceiling values.
   *
   * @param colors
   */
  @Override
public void setColorPalette(Color[] colors) {
    colorMap.setColorPalette(colors);
  }

  /**
   * Sets the floor and ceiling colors.
   *
   * @param floorColor
   * @param ceilColor
   */
  @Override
public void setFloorCeilColor(Color floorColor, Color ceilColor) {
    colorMap.setFloorCeilColor(floorColor, ceilColor);
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
   * Updates the buffered image using the data array.
   */
  @Override
public synchronized void update() {
    if(griddata==null) {
      return;
    }
    if(autoscaleZ) {
      griddata.getZRange(ampIndex, minmax);
      double ceil = minmax[1];
      double floor = minmax[0];
      if(symmetricZ){
        ceil=Math.max(Math.abs(minmax[1]),Math.abs(minmax[0]));
        floor=-ceil;
      }
      colorMap.setScale(floor, ceil);
    }
    recolorImage();
    colorMap.updateLegend(null);
  }

  /**
   * Checks if the image is the correct size.
   */
  protected synchronized void checkImage(DrawingPanel panel) {
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
    
    if((image!=null)&&(image.getWidth()==width)&&(image.getHeight()==height)&&(left==panel.pixToX(leftPix))&&(top==panel.pixToY(topPix))&&(bottom==panel.pixToX(bottomPix))&&(right==panel.pixToY(rightPix))) {
      return; // image exists, has the correct location, and is the correct size
    }
    left = panel.pixToX(leftPix);
    top = panel.pixToY(topPix);
    bottom = panel.pixToX(bottomPix);
    right = panel.pixToY(rightPix);
    if((image!=null)&&(image.getWidth()==width)&&(image.getHeight()==height)) {
      recolorImage();
      return; // image exists and is the correct size so recolor it
    }
    int size = height*width;
    if((size<4)||(height>4000)||(width>4000)) {
      image = null;
      return;
    }
    //OSPLog.finer("InterpolatedPlot image created with row="+height+" and col="+width); //$NON-NLS-1$ //$NON-NLS-2$
    imageType = 
    		(OSPRuntime.isJS ? JSUtilI.TYPE_4BYTE_HTML5
    				: BufferedImage.TYPE_4BYTE_ABGR);
    image = new BufferedImage(width, height, imageType);
    pixelData = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
    recolorImage();
  }

  /**
   * Recolors the image pixels using the data array.
   */
  protected void recolorImage() {
    if(!visible) {
      return;
    }
    // local reference for thread safety
    GridData griddata = this.griddata;
    BufferedImage image = this.image;
    if(griddata==null) {
      return;
    }
    if(griddata.isCellData()) {
      double dx = griddata.getDx();
      double dy = griddata.getDy();
      xmin = griddata.getLeft()-dx/2;
      xmax = griddata.getRight()+dx/2;
      ymin = griddata.getBottom()+dy/2;
      ymax = griddata.getTop()-dy/2;
    } else {
      xmin = griddata.getLeft();
      xmax = griddata.getRight();
      ymin = griddata.getBottom();
      ymax = griddata.getTop();
    }
    grid.setMinMax(xmin, xmax, ymin, ymax);
    if(image==null) {
      return;
    }
    if(pixelData.length != image.getWidth()*image.getHeight() * 4) {
      return;
    }
    double y = top;
    double dx = (xmax-xmin)/(ixsize-1);
    double dy = (ymin-ymax)/(iysize-1);
    if(griddata.getDx()<0) {
      dx = -dx;
    }
    if(griddata.getDy()>0) {
      dy = -dy;
    }
    writeToRaster(left, y, dx, dy);
  }

  
  protected void writeToRaster(double x0, double y, double dx, double dy) {
	    int width = image.getWidth();
	    int height = image.getHeight();
		byte[] pixels = pixelData;
		boolean isABGR = (imageType == BufferedImage.TYPE_4BYTE_ABGR);
	    for(int i = 0; i<height; i++, y += dy) {
	      double x = x0;
	      for(int j = 0; j<width; j++, x += dx) {
	        byte[] ret = colorMap.doubleToComponents(griddata.interpolate(x, y, ampIndex));
	        int pt = (i*width+j)<<2;
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
   * Shows how values map to colors.
   */
  @Override
public JFrame showLegend() {
    return colorMap.showLegend();
  }

  @Override
public boolean isMeasured() {
    return griddata!=null;
  }

  /**
   * Draws the image and the grid.
   * @param panel
   * @param g
   */
  @Override
public void draw(DrawingPanel panel, Graphics g) {
    if(!visible||(griddata==null)) {
      return;
    }
    checkImage(panel);
    if(image!=null) {
  	  // Note that the image in this case extends beyond the clipping range.
      g.drawImage(image, leftPix, topPix, panel);
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
        return new InterpolatedPlot(null);
      }
      @Override
	public void saveObject(XMLControl control, Object obj) {
        super.saveObject(control, obj);
        InterpolatedPlot plot = (InterpolatedPlot) obj;
        control.setValue("color map", plot.colorMap); //$NON-NLS-1$
      }
      @Override
	public Object loadObject(XMLControl control, Object obj) {
        super.loadObject(control, obj);
        InterpolatedPlot plot = (InterpolatedPlot) obj;
        plot.colorMap = (ColorMapper) control.getObject("color map"); //$NON-NLS-1$
        return plot;
      }

    };
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
 * Copyright (c) 2024  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
