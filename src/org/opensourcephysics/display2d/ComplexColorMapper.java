/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display2d;
import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.opensourcephysics.display.InteractivePanel;
import org.opensourcephysics.display.axes.XAxis;
import org.opensourcephysics.display.axes.XYAxis;
import org.opensourcephysics.js.JSUtil;

public class ComplexColorMapper {
  static final double PI2 = Math.PI*2;
  static final double COLOR_ERR = 1.0E-9;
  private double ceil;
  private Color ceilColor = Color.lightGray;
  private JFrame legendFrame;
  double[] reds = new double[256];
  double[] greens = new double[256];
  double[] blues = new double[256];
  protected ZExpansion zMap = null;

  /**
   * Constructor ComplexColorMapper
   * @param _ceil
   */
  public ComplexColorMapper(double _ceil) {
    ceil = _ceil;
    if(zMap!=null) {
      zMap.setMinMax(0, ceil);
    }
    initColors();
  }

  public static JFrame showPhaseLegend() {
    InteractivePanel dp = new InteractivePanel();
    dp.setPreferredSize(new java.awt.Dimension(300, 66));
    dp.setPreferredGutters(0, 0, 0, 35);
    dp.setClipAtGutter(false);
    JFrame legendFrame = new JFrame("Complex Phase"); //$NON-NLS-1$
    legendFrame.setResizable(false);
    legendFrame.setContentPane(dp);
    int numPts = 360;
    GridPointData pointdata = new GridPointData(numPts, 1, 3);
    double[][][] data = pointdata.getData();
    double theta = -Math.PI, delta = 2*Math.PI/(numPts);
    for(int i = 0, n = data.length; i<n; i++) {
      data[i][0][2] = 0.999;
      data[i][0][3] = Math.cos(theta);
      data[i][0][4] = Math.sin(theta);
      theta += delta;
    }
    pointdata.setScale(-Math.PI, Math.PI, 0, 1);
    Plot2D plot = new ComplexGridPlot(pointdata);
    plot.setShowGridLines(false);
    plot.update();
    dp.addDrawable(plot);
    XAxis xaxis = new XAxis(""); //$NON-NLS-1$
    xaxis.setLocationType(XYAxis.DRAW_AT_LOCATION);
    xaxis.setLocation(-0.5);
    xaxis.setEnabled(true);
    dp.addDrawable(xaxis);
    legendFrame.pack();
    legendFrame.setVisible(true);
    return legendFrame;
  }

  /**
   * Shows the phase legend.
   */
  public JFrame showLegend() {
    InteractivePanel dp = new InteractivePanel();
    dp.setPreferredSize(new java.awt.Dimension(300, 66));
    dp.setPreferredGutters(0, 0, 0, 35);
    dp.setClipAtGutter(false);
    if((legendFrame==null)||!legendFrame.isDisplayable()) {
      legendFrame = new JFrame("Complex Phase"); //$NON-NLS-1$
    }
    legendFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    legendFrame.setResizable(false);
    legendFrame.setContentPane(dp);
    int numPts = 360;
    GridPointData pointdata = new GridPointData(numPts, 1, 3);
    double[][][] data = pointdata.getData();
    double theta = -Math.PI, delta = 2*Math.PI/(numPts);
    for(int i = 0, n = data.length; i<n; i++) {
      data[i][0][2] = 0.999;
      data[i][0][3] = Math.cos(theta);
      data[i][0][4] = Math.sin(theta);
      theta += delta;
    }
    pointdata.setScale(-Math.PI, Math.PI, 0, 1);
    Plot2D plot = new ComplexGridPlot(pointdata);
    plot.setShowGridLines(false);
    plot.update();
    dp.addDrawable(plot);
    XAxis xaxis = new XAxis(""); //$NON-NLS-1$
    xaxis.setLocationType(XYAxis.DRAW_AT_LOCATION);
    xaxis.setLocation(-0.5);
    xaxis.setEnabled(true);
    dp.addDrawable(xaxis);
    legendFrame.pack();
    legendFrame.setVisible(true);
    return legendFrame;
  }

  /**
   * Sets the z scale.
   * @param _ceil
   */
  public void setScale(double _ceil) {
    ceil = _ceil;
    if(zMap!=null) {
      zMap.setMinMax(0, ceil);
    }
  }

  /**
   * Converts a phase angle in the range [-Pi,Pi] to hue, saturation, and brightness.
   *
   * @param phi phase angle
   * @return the HSB color
   */
  public Color phaseToColor(double phi) {
    double b = 1; // brightness
    double h = (double) ((Math.PI+phi)/PI2);
    int index = ((int) (255*h));
    return new Color((int) (b*reds[index]), (int) (b*greens[index]), (int) (b*blues[index]));
  }

  /**
   * Converts a complex number to hue, saturation, and brightness.
   * @param re
   * @param im
   * @return the HSB color
   */
  public Color complexToColor(double re, double im) {
    double b = 1; // brightness
    double h = (double) ((Math.PI+Math.atan2(im, re))/PI2);
    int index = ((int) (255*h));
    return new Color((int) (b*reds[index]), (int) (b*greens[index]), (int) (b*blues[index]));
  }
  
  private Color colorTemp = new Color(0);

	/**
	 * Converts an array of samples to hue, saturation, and brightness. Samples
	 * contains magnitude, re, and im.
	 * 
	 * @param samples
	 * @return the HSB color
	 */
	public Color samplesToColor(double[] samples) {
		double zval = samples[0];
		if (zMap != null) {
			zval = zMap.evaluate(zval);
		}
		if (zval <= 0) {
			return Color.black;
		} else if ((zMap == null) && (zval > ceil + COLOR_ERR)) {
			return ceilColor;
		} else {
			zval = Math.min(zval, ceil);
		}
		double bb = (double) (zval / ceil); // brightness
		double h = (double) ((Math.PI + Math.atan2(samples[2], samples[1])) / PI2); // hue
		int index = ((int) (255 * h));
		int r = (int) (bb * reds[index]);
		int g = (int) (bb * greens[index]);
		int b = (int) (bb * blues[index]);
		int v = (((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF)) | 0xFF000000;
		Color c = (JSUtil.isJS ? colorTemp : null);
		/**
		 * just mutate colorTemp
		 * @j2sNative
		 * 
		 * c.value = v;
		 * 
		 * 
		 */
		{
			c = new Color(v);
		}
		return c;
	}
  
    private byte[] rgbCeil = new byte[3];
    private byte[] rgbBlack = new byte[3];

	/**
	 * Return a pixel in the standard rgba format of raster.getPixel.
	 * 
	 * @param samples
	 * @param retRGB temp array to be returned  
	 * @return byte[] in the form of filling [r g b a]
	 * 
	 */
	public byte[] sampleToPixel(double[] samples, byte[]retRGB) {
		double zval = samples[0];
		if (zMap != null) {
			zval = zMap.evaluate(zval);
		}
		if (zval <= 0) {
			return rgbBlack;
		}
		if ((zMap == null) && (zval > ceil + COLOR_ERR)) {
			return rgbCeil;
		}
		double bb = (double) (Math.min(zval, ceil) / ceil); // brightness
		double h = (double) ((Math.PI + Math.atan2(samples[2], samples[1])) / PI2); // hue
		int index = ((int) (255 * h));
		retRGB[0] = (byte) (bb * reds[index]);
		retRGB[1] = (byte) (bb * greens[index]);
		retRGB[2] = (byte) (bb * blues[index]);
		return retRGB;
	}

  /**
   * Converts a vertex point array of samples to hue, saturation, and brightness.
   *
   * A vertex containing x, y, magnitude, re, and im.
   *
   * @param vertex the point
   * @return the HSB color
   */
  public Color pointToColor(double[] vertex) {
    double zval = vertex[2];
    if(zMap!=null) {
      zval = zMap.evaluate(zval);
    }
    if(zval<=0) {
      return Color.black;
    } else if(zval>ceil+COLOR_ERR) {
      return ceilColor;
    }
    double b = (double) (zval/ceil);
    double h = (double) ((Math.PI+Math.atan2(vertex[4], vertex[3]))/PI2);
    int index = ((int) (255*h));
    return new Color((int) (b*reds[index]), (int) (b*greens[index]), (int) (b*blues[index]));
    // return  Color.getHSBColor(h,1,b);
  }

  /**
   * Sets map for z values.
   *
   * @param map ZExpansion
   */
  public void setZMap(ZExpansion map) {
    zMap = map;
    if(zMap!=null) {
      zMap.setMinMax(0, ceil);
    }
  }

  /**
   * Gets the ceiling color.
   * @return
   */
  public double getCeil() {
    return ceil;
  }

  /**
   * Gets the ceiling color.
   * @return
   */
  public Color getCeilColor() {
    return ceilColor;
  }

  /**
   * Sets the ceiling.
   *
   * @param _ceilColor
   */
  public void setCeilColor(Color _ceilColor) {
    ceilColor = _ceilColor;
    rgbCeil = new byte[] {(byte)ceilColor.getRed(), (byte)ceilColor.getGreen(), (byte)ceilColor.getBlue()};
  }

  private void initColors() {
    double pi = Math.PI;
    for(int i = 0; i<256; i++) {
      double val = Math.abs(Math.sin(pi*i/255));
      blues[i] = (int) (255*val*val);
      val = Math.abs(Math.sin(pi*i/255+pi/3));
      greens[i] = (int) (255*val*val*Math.sqrt(val));
      val = Math.abs(Math.sin(pi*i/255+2*pi/3));
      reds[i] = (int) (255*val*val);
    }
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
 * Copyright (c) 2017  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
