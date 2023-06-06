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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.display.GUIUtils;
import org.opensourcephysics.display.OSPRuntime;

/**
 * This is a Filter that applies radial transformations to an image.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class BarrelPincushionFilter extends Filter {

	// static fields
	protected static double minAlpha = -0.3, maxAlpha = 0.3;

	// instance fields
	private double[] xOut, yOut, xIn, yIn; // pixel positions on input and output images
	private double pixelsToCorner; // half image diagonal in pixels
	private boolean isValidTransform = false, updatingDisplay = false, dimensionsChanged = false;

	// parameters
	private int interpolation = 1; // neighborhood size for color interpolation
	private double alpha = 0.2;
	private double fixedRadius = 0.7;
	private double scaleFactor= 1;

	// inspector and circle
	private Inspector inspector;

	/**
	 * Constructor.
	 */
	public BarrelPincushionFilter() {
		super();
		refresh();
		hasInspector = true;
	}

	@Override
	protected InspectorDlg newInspector() {
		return inspector = new Inspector();
	}

	@Override
	protected InspectorDlg initInspector() {
		inspector.initialize();
		return inspector;
	}

	/**
	 * Refreshes this filter's GUI
	 */
	@Override
	public void refresh() {
		if (inspector == null || !haveGUI)
			return;
		super.refresh();
		ableButton.setText(isEnabled() ? 
				MediaRes.getString("Filter.Button.Disable") : //$NON-NLS-1$
				MediaRes.getString("Filter.Button.Enable")); //$NON-NLS-1$		
		inspector.refreshGUI();
	}
	
	public void setAlpha(double a) {
		alpha = Math.max(minAlpha, Math.min(maxAlpha, a));
		isValidTransform = false;
		setFixedRadius(0.5);
		firePropertyChange(PROPERTY_FILTER_IMAGE, null, null); //$NON-NLS-1$
	}

	/**
	 * Sets the fixed radius fraction. Pixels at this distance from the image center
	 * remain fixed.
	 * 
	 * @param fraction the fixed radius as a fraction of the corner radius
	 */
	public void setFixedRadius(double fraction) {
		if (Double.isNaN(fraction))
			return;
		fraction = Math.abs(fraction);
		fraction = Math.min(fraction, 1);
		fraction = Math.max(fraction, 0.2);
//		if (fixedRadius != fraction) {
			fixedRadius = fraction;
			scaleFactor = 1 / getStretchFactor(fixedRadius * pixelsToCorner);
			System.out.println("pig scaleFactor "+scaleFactor);
			if (inspector != null)
				inspector.updateDisplay();
			isValidTransform = false;
			firePropertyChange(PROPERTY_FILTER_IMAGE, null, null); //$NON-NLS-1$
//		}
	}

	// _____________________________ private methods _______________________

	/**
	 * Creates the input and output images.
	 *
	 * @param image a new input image
	 */
	@Override
	protected void initializeSubclass() {
		pixelsToCorner = Math.sqrt(w * w + h * h) / 2;
		isValidTransform = false;
	}

	@Override
	protected void initializeSource(BufferedImage image) {
		int prevW = w, prevH = h;
		super.initializeSource(image);
		dimensionsChanged = (w != prevW || h != prevH);
	}
	
	/**
	 * Sets the output image pixels to a transformed version of the input pixels.
	 *
	 * @param input the input image
	 */
	@Override
	protected void setOutputPixels() {
		getPixelsIn();
		getPixelsOut();
		// output positions are integer pixel positions--recreate when dimensions change
		if (dimensionsChanged) {
			xOut = new double[w * h];
			yOut = new double[w * h];
			for (int i = 0; i < w; i++) {
				for (int j = 0; j < h; j++) {
					xOut[j * w + i] = i;
					yOut[j * w + i] = j;
				}
			}
		}
		
		// if needed, map the output (corrected) pixel positions to input pixel
		// positions
		if (!isValidTransform || xIn == null || dimensionsChanged) {
			xIn = new double[w * h];
			yIn = new double[w * h];
			transform(xOut, yOut, xIn, yIn);
		}

		// find output pixel color values by interpolating input pixel colors
		for (int i = 0; i < nPixelsIn; i++) {
			pixelsOut[i] = getColor(xIn[i], yIn[i], w, h, pixelsIn);
		}
		
		dimensionsChanged = false;
	}

	/**
	 * Transforms arrays of pixel position coordinates for source to output
	 * conversion.
	 * 
	 * @param xSource array of source x-coordinates
	 * @param ySource array of source y-coordinates
	 * @param xTrans  array of transformed x-coordinates
	 * @param yTrans  array of transformed y-coordinates
	 */
	private void transform(double[] xSource, double[] ySource, double[] xTrans, double[] yTrans) {

		double xCenter = w / 2.0, yCenter = h / 2.0;

		int n = xSource.length;
		for (int i = 0; i < n; i++) {
			double dx = xSource[i] - xCenter;
			double dy = ySource[i] - yCenter;
			double r = Math.sqrt(dx * dx + dy * dy);

			double stretch = getStretchFactor(r) * scaleFactor;
			double extra = 0.0001;
			xTrans[i] = xCenter + stretch * dx + extra;
			yTrans[i] = yCenter + stretch * dy + extra;
		}

		isValidTransform = true;
	}

	/**
	 * Gets the stretch factor at a given radius in the output image (source radius
	 * = output radius * stretch factor).
	 * 
	 * @param rOut the distance from the output image center, in pixels
	 * @return the stretch factor
	 */
	private double getStretchFactor(double rOut) {
		if (rOut == 0)
			return 1;
		
	  // normalize r to the range [0; 1]
	  double rn = rOut / pixelsToCorner;
	  	  
	  // find the corrected normalized radius
	  int n = 0;
	  double rnPrev = rn;
	  double rnNext = transform(rn, rnPrev);
	  // iterate until change is less than 0.1 pixel
	  while (n < 10 && Math.abs(rnPrev - rnNext) > 0.1/pixelsToCorner) {
	  	n++;
	  	rnPrev = rnNext;
	  	rnNext = transform(rn, rnPrev);
	  }
	  // restore original range
	  double rIn = rnNext * pixelsToCorner;
	
		double ratio = rIn / rOut;
		return ratio;
		// adjust so stretch = 1 when rFixed = rOut
	}
	
	private double transform(double rOrig, double rPrev) {
		return rOrig / (1.0 - alpha * rPrev*rPrev);
	}
	
	
	// pig code to do barrel/pincushion correction in shader
//	precision mediump float;
//
//	uniform sampler2D texture_diffuse;
//	uniform vec2 image_dimensions;
//	uniform float alphax;
//	uniform float alphay;
//
//	varying vec4 pass_Color;
//	varying vec2 pass_TextureCoord;
//
//	void main(void) {
//
//	  // Normalize the u,v coordinates in the range [-1;+1]
//	  float x = (2.0 * pass_TextureCoord.x - 1.0) / 1.0;
//	  float y = (2.0 * pass_TextureCoord.y - 1.0) / 1.0;
//	  
//	  // Calculate l2 norm
//	  float r = x*x + y*y;
//	  
//	  // Calculate the deflated or inflated new coordinate (reverse transform)
//	  float x3 = x / (1.0 - alphax * r);
//	  float y3 = y / (1.0 - alphay * r); 
//	  float x2 = x / (1.0 - alphax * (x3 * x3 + y3 * y3));
//	  float y2 = y / (1.0 - alphay * (x3 * x3 + y3 * y3));	
//	  
//	  // Forward transform
//	  // float x2 = x * (1.0 - alphax * r);
//	  // float y2 = y * (1.0 - alphay * r);
//
//	  // De-normalize to the original range
//	  float i2 = (x2 + 1.0) * 1.0 / 2.0;
//	  float j2 = (y2 + 1.0) * 1.0 / 2.0;
//
//	  if(i2 >= 0.0 && i2 <= 1.0 && j2 >= 0.0 && j2 <= 1.0)
//	    gl_FragColor = texture2D(texture_diffuse, vec2(i2, j2));
//	  else
//	    gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
//	}	

	/**
	 * Get the interpolated color at a non-integer position (between pixels points).
	 * 
	 * @param x           the x-coordinate of the position
	 * @param y           the y-coordinate of the position
	 * @param w           the width of the image
	 * @param h           the height of the image
	 * @param pixelValues the color values of the pixels in the image
	 */
	private int getColor(double x, double y, int w, int h, int[] pixelValues) {
		// get base pixel position
		int col = (int) Math.floor(x);
		int row = (int) Math.floor(y);
		if (col < 0 || col >= w || row < 0 || row >= h) {
			return 0; // black if not in image
		}
		if (col + 1 == w || row + 1 == h) {
			return pixelValues[row * w + col];
		}

		double u = col == 0 ? x : x % col;
		double v = row == 0 ? y : y % row;

		if (interpolation == 2) {
			// get 2x2 neighborhood pixel values
			int[] values = new int[] { pixelValues[row * w + col], pixelValues[row * w + col + 1],
					pixelValues[(row + 1) * w + col], pixelValues[(row + 1) * w + col + 1] };

			int[] rgb = new int[4];
			for (int j = 0; j < 4; j++) {
				rgb[j] = (values[j] >> 16) & 0xff; // red
			}
			int r = bilinearInterpolation(u, v, rgb);
			for (int j = 0; j < 4; j++) {
				rgb[j] = (values[j] >> 8) & 0xff; // green
			}
			int g = bilinearInterpolation(u, v, rgb);
			for (int j = 0; j < 4; j++) {
				rgb[j] = (values[j]) & 0xff; // blue
			}
			int b = bilinearInterpolation(u, v, rgb);
			return (r << 16) | (g << 8) | b;
		}

		// if not interpolating, return value of nearest neighbor
		return u < 0.5 ? v < 0.5 ? pixelValues[row * w + col] : pixelValues[(row + 1) * w + col]
				: v < 0.5 ? pixelValues[row * w + col + 1] : pixelValues[(row + 1) * w + col + 1];
	}

	/**
	 * Returns a bilinear interpolated pixel color (int value) at a given point
	 * relative to (0,0).
	 * 
	 * @param x      the x-position relative to 0,0 (0<=x<1)
	 * @param x      the y-position relative to 0,0 (0<=y<1)
	 * @param values array of pixels color values [value(0,0), value(0,1),
	 *               value(1,0), value(1,1)]
	 * @return the interpolated color value
	 */
	private int bilinearInterpolation(double x, double y, int[] values) {
		return (int) ((1 - y) * ((1 - x) * values[0] + x * values[2]) + y * ((1 - x) * values[1] + x * values[3]));
	}

	/**
	 * Inner Inspector class to control filter parameters
	 */
	private class Inspector extends InspectorDlg {

		JButton helpButton;
		JPanel contentPane;
		JSlider alphaSlider;
		JLabel alphaLabel;
		NumberField alphaField;

		/**
		 * Constructs the Inspector.
		 */
		public Inspector() {
			super("RadialDistortionFilter.Inspector.Title");
		}

		/**
		 * Creates the visible components.
		 */
		@Override
		void createGUI() {
			helpButton = new JButton();
			helpButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
//					String s = MediaRes.getString("RadialDistortionFilter.Help.Message1") //$NON-NLS-1$
//							+ "\n" + MediaRes.getString("RadialDistortionFilter.Help.Message2") //$NON-NLS-1$ //$NON-NLS-2$
//							+ "\n" + MediaRes.getString("RadialDistortionFilter.Help.Message3") //$NON-NLS-1$ //$NON-NLS-2$
//							+ "\n" + MediaRes.getString("RadialDistortionFilter.Help.Message4") //$NON-NLS-1$ //$NON-NLS-2$
//							+ "\n" + MediaRes.getString("RadialDistortionFilter.Help.Message5") //$NON-NLS-1$ //$NON-NLS-2$
//							+ "\n\n" + MediaRes.getString("RadialDistortionFilter.Help.Message6") //$NON-NLS-1$ //$NON-NLS-2$
//							+ "\n" + MediaRes.getString("RadialDistortionFilter.Help.Message7"); //$NON-NLS-1$ //$NON-NLS-2$
//
//					for (int i = 0; i < PROJECTION_TYPES.size(); i++) {
//						String type = PROJECTION_TYPES.get(i);
//						s += "\n    " + (i + 1) + ". " //$NON-NLS-1$ //$NON-NLS-2$
//								+ MediaRes.getString("RadialDistortionFilter.Help.Message." + type); //$NON-NLS-1$
//					}
//
//					s += "\n\n" + MediaRes.getString("RadialDistortionFilter.Help.Message8") //$NON-NLS-1$ //$NON-NLS-2$
//							+ "\n    " + MediaRes.getString("RadialDistortionFilter.Help.Message9") //$NON-NLS-1$ //$NON-NLS-2$
//							+ "\n    " + MediaRes.getString("RadialDistortionFilter.Help.Message10") //$NON-NLS-1$ //$NON-NLS-2$
//							+ "\n    " + MediaRes.getString("RadialDistortionFilter.Help.Message11"); //$NON-NLS-1$ //$NON-NLS-2$
//
//					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(vidPanel), s,
//							MediaRes.getString("RadialDistortionFilter.Help.Title"), //$NON-NLS-1$
//							JOptionPane.INFORMATION_MESSAGE);
				}
			});
			
			Border space = BorderFactory.createEmptyBorder(2, 2, 2, 2);

			space = BorderFactory.createEmptyBorder(2, 4, 2, 4);

			int aMax = (int) (300 * maxAlpha);
			int aMin = (int) (300 * minAlpha);
			int a = (int) (300 * alpha);

			alphaSlider = new JSlider(aMin, aMax, a);
			alphaSlider.setBorder(space);
			alphaSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					int i = alphaSlider.getValue();
					setAlpha(i / 300.0);
				}
			});
			alphaSlider.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent e) {
					updateDisplay();
				}
			});

			space = BorderFactory.createEmptyBorder(2, 4, 2, 2);
			alphaLabel = new JLabel();
			alphaLabel.setBorder(space);

			alphaField = new NumberField(3);
			alphaField.setMaxValue(maxAlpha);
			alphaField.setMinValue(minAlpha);
			alphaField.setUnits("%"); //$NON-NLS-1$
			alphaField.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					alpha = alphaField.getValue();
					updateDisplay();
					alphaField.selectAll();
				}

			});
			alphaField.addFocusListener(new FocusListener() {
				@Override
				public void focusGained(FocusEvent e) {
					alphaField.selectAll();
				}

				@Override
				public void focusLost(FocusEvent e) {
					alpha = alphaField.getValue();
					updateDisplay();
				}

			});

			// add components to content pane
			contentPane = new JPanel(new BorderLayout());
			setContentPane(contentPane);

			JPanel buttonbar = new JPanel(new FlowLayout());
			contentPane.add(buttonbar, BorderLayout.SOUTH);
			buttonbar.add(helpButton);
			buttonbar.add(ableButton);
			buttonbar.add(closeButton);

			space = BorderFactory.createEmptyBorder(2, 2, 2, 4);

//			Box controlPanel = Box.createVerticalBox();
//			contentPane.add(controlPanel, BorderLayout.CENTER);
//
//			Box sourceStack = Box.createVerticalBox();
//			controlPanel.add(sourceStack);

			Box box = Box.createHorizontalBox();
			box.setBorder(space);
			box.add(alphaLabel);
			box.add(alphaField);
			box.add(alphaSlider);
			contentPane.add(box, BorderLayout.CENTER);
		}

		/**
		 * Refreshes this inspector's GUI
		 */
		void refreshGUI() {
			setTitle(MediaRes.getString("RadialDistortionFilter.Inspector.Title")); //$NON-NLS-1$
			alphaLabel.setText(MediaRes.getString("RadialDistortionFilter.Label.Diameter") + ":"); //$NON-NLS-1$ //$NON-NLS-2$
			helpButton.setText(MediaRes.getString("PerspectiveFilter.Button.Help")); //$NON-NLS-1$
			boolean enabled = BarrelPincushionFilter.this.isEnabled();
			alphaLabel.setEnabled(enabled);
			alphaField.setEnabled(enabled);
			alphaSlider.setEnabled(enabled);
			repaint();
		}

		/**
		 * Initializes this inspector
		 */
		void initialize() {
			updateDisplay();
		}

		/**
		 * Updates the inspector controls to reflect the current filter settings.
		 */
		void updateDisplay() {
			if (updatingDisplay)
				return;
			updatingDisplay = true;
//			refreshScale();
//			int n = (int) Math.round(180 * sourceFOV / Math.PI);
//			sourceAngleField.setIntValue(n);
//			sourceAngleSlider.setValue(n);
//			n = (int) Math.round(180 * outputFOV / Math.PI);
//			outputAngleField.setIntValue(n);
//			n = (int) Math.round(100 * fixedRadius);
//			radiusSlider.setValue(n);
//			radiusField.setIntValue(n);
			updatingDisplay = false;
		}

		@Override
		public void setVisible(boolean vis) {
			super.setVisible(vis);
			refreshGUI();
			if (vidPanel != null) {
				if (vis) {
					firePropertyChange(PROPERTY_FILTER_VISIBLE, null, null);
					BarrelPincushionFilter.this.addPropertyChangeListener(PROPERTY_FILTER_VISIBLE, vidPanel);
				} else {
					firePropertyChange(PROPERTY_FILTER_VISIBLE, null, null);
					BarrelPincushionFilter.this.removePropertyChangeListener(PROPERTY_FILTER_VISIBLE, vidPanel);
				}
			}
			firePropertyChange(PROPERTY_FILTER_IMAGE, null, null); //$NON-NLS-1$
		}

	}

	/**
	 * for inner anonymous class; could use qualified super
	 * @return
	 */
	public boolean superIsEnabled() {
		return super.isEnabled();
	}

	/**
	 * Returns an XML.ObjectLoader to save and load filter data.
	 *
	 * @return the object loader
	 */
	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

	/**
	 * A class to save and load filter data.
	 */
	static class Loader implements XML.ObjectLoader {
		/**
		 * Saves data to an XMLControl.
		 *
		 * @param control the control to save to
		 * @param obj     the filter to save
		 */
		@Override
		public void saveObject(XMLControl control, Object obj) {
			BarrelPincushionFilter filter = (BarrelPincushionFilter) obj;

//			control.setValue("fixed_radius", filter.fixedRadius); //$NON-NLS-1$
//			control.setValue("input_type", filter.sourceProjectionType); //$NON-NLS-1$
//			control.setValue("input_fov", filter.sourceFOV); //$NON-NLS-1$
//			control.setValue("output_type", filter.outputProjectionType); //$NON-NLS-1$
			filter.addLocation(control);
		}

		/**
		 * Creates a new filter.
		 *
		 * @param control the control
		 * @return the new filter
		 */
		@Override
		public Object createObject(XMLControl control) {
			return new BarrelPincushionFilter();
		}

		/**
		 * Loads a filter with data from an XMLControl.
		 *
		 * @param control the control
		 * @param obj     the filter
		 * @return the loaded object
		 */
		@Override
		public Object loadObject(XMLControl control, Object obj) {
			final BarrelPincushionFilter filter = (BarrelPincushionFilter) obj;

//			filter.setFixedRadius(control.getDouble("fixed_radius")); //$NON-NLS-1$
//			filter.setSourceFOV(control.getDouble("input_fov")); //$NON-NLS-1$
//			filter.setSourceProjectionType(control.getString("input_type")); //$NON-NLS-1$
//			filter.setOutputProjectionType(control.getString("output_type")); //$NON-NLS-1$

			filter.inspectorX = control.getInt("inspector_x"); //$NON-NLS-1$
			filter.inspectorY = control.getInt("inspector_y"); //$NON-NLS-1$
			return obj;
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
