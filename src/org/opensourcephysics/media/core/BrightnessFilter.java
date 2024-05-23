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
 * Copyright (c) 2024  Douglas Brown and Wolfgang Christian.
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

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;

/**
 * This is a Filter that changes the brightness and contrast of a source image.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class BrightnessFilter extends Filter {
	// instance fields
	private int defaultBrightness = 0;
	private double defaultContrast = 50;
	private int brightness = defaultBrightness, previousBrightness;
	private double contrast = defaultContrast, previousContrast;
	private double slope;
	private double offset1;
	private double offset2;
	// used by inspector
	private Inspector inspector;
	private JLabel brightnessLabel;
	private IntegerField brightnessField;
	private JSlider brightnessSlider;
	private JLabel contrastLabel;
	private NumberField contrastField;
	private JSlider contrastSlider;

	/**
	 * Constructs a default BrightnessFilter object.
	 */
	public BrightnessFilter() {
		setBrightness(defaultBrightness);
		setContrast(defaultContrast);
		hasInspector = true;
	}

	/**
	 * Sets the contrast.
	 *
	 * @param contrast the contrast.
	 */
	public void setContrast(double contrast) {
		if (previousState == null) {
			previousState = new XMLControlElement(this).toXML();
			previousBrightness = brightness;
			previousContrast = contrast;
		}
		changed = changed || this.contrast != contrast;
		Double prev = Double.valueOf(this.contrast);
		this.contrast = contrast;
		updateFactors();
		firePropertyChange("contrast", prev, Double.valueOf(contrast)); //$NON-NLS-1$
	}

	/**
	 * Gets the contrast.
	 *
	 * @return the contrast.
	 */
	public double getContrast() {
		return contrast;
	}

	/**
	 * Sets the brightness.
	 *
	 * @param brightness the brightness.
	 */
	public void setBrightness(int brightness) {
		if (previousState == null) {
			previousState = new XMLControlElement(this).toXML();
			previousBrightness = this.brightness;
			previousContrast = this.contrast;
		}
		changed = changed || this.brightness != brightness;
		Integer prev = Integer.valueOf(this.brightness);
		this.brightness = brightness;
		updateFactors();
		firePropertyChange(PROPERTY_FILTER_BRIGHTNESS, prev, Integer.valueOf(brightness)); 
	}

	/**
	 * Gets the brightness.
	 *
	 * @return the brightness.
	 */
	public int getBrightness() {
		return brightness;
	}

	/**
	 * Determines if the filter settings have changed.
	 * 
	 * @return true if changed
	 */
	@Override
	public boolean isChanged() {
		if (!changed)
			return false;
		// changes have occurred so compare final and initial states
		return previousBrightness != brightness || previousContrast != contrast;
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
	 * Clears this filter
	 */
	@Override
	public void clear() {
		setBrightness(defaultBrightness);
		setContrast(defaultContrast);
		if (inspector != null) {
			inspector.updateDisplay();
		}
	}

	/**
	 * Refreshes this filter's GUI
	 */
	@Override
	public void refresh() {
		if (inspector == null || !haveGUI)
			return;
		super.refresh();
		brightnessLabel.setText(MediaRes.getString("Filter.Brightness.Label.Brightness")); //$NON-NLS-1$
		brightnessSlider.setToolTipText(MediaRes.getString("Filter.Brightness.ToolTip.Brightness")); //$NON-NLS-1$
		contrastLabel.setText(MediaRes.getString("Filter.Brightness.Label.Contrast")); //$NON-NLS-1$
		contrastSlider.setToolTipText(MediaRes.getString("Filter.Brightness.ToolTip.Contrast")); //$NON-NLS-1$
		boolean enabled = isEnabled();
		brightnessLabel.setEnabled(enabled);
		brightnessSlider.setEnabled(enabled);
		brightnessField.setEnabled(enabled);
		contrastLabel.setEnabled(enabled);
		contrastSlider.setEnabled(enabled);
		contrastField.setEnabled(enabled);
		clearButton.setText(MediaRes.getString("Dialog.Button.Reset")); //$NON-NLS-1$
		inspector.setTitle(MediaRes.getString("Filter.Brightness.Title")); //$NON-NLS-1$
		inspector.updateDisplay();
		inspector.pack();
	}

	@Override
	public void dispose() {
		super.dispose();
		inspector = null;
	}

	// _____________________________ private methods _______________________


	@Override
	protected void initializeSubclass() {
		// nothing to do	
	}
	
	/**
	 * Sets the output image pixels to a bright version of the input pixels.
	 */
	@Override
	protected void setOutputPixels() {
		getPixelsIn();
		getPixelsOut();
		int pixel, r, g, b;
		for (int i = 0; i < nPixelsIn; i++) {
			pixel = pixelsIn[i];
			r = (pixel >> 16) & 0xff; // red
			r = Math.max((int) (slope * (r + offset1) + offset2), 0);
			r = Math.min(r, 255);
			g = (pixel >> 8) & 0xff; // green
			g = Math.max((int) (slope * (g + offset1) + offset2), 0);
			g = Math.min(g, 255);
			b = (pixel) & 0xff; // blue
			b = Math.max((int) (slope * (b + offset1) + offset2), 0);
			b = Math.min(b, 255);
			pixelsOut[i] = (r << 16) | (g << 8) | b;
		}
	}

	/**
	 * Updates factors used to convert pixel values
	 */
	private void updateFactors() {
		double theta = Math.PI * contrast / 200;
		double sin = Math.sin(theta);
		offset1 = sin * sin * brightness - 127;
		double cos = Math.cos(theta);
		offset2 = 127 + cos * cos * brightness;
		slope = sin / cos;
	}

	/**
	 * Inner Inspector class to control filter parameters
	 */
	private class Inspector extends InspectorDlg {

		protected Inspector() {
			super("Filter.Brightness.Title");
		}

		/**
		 * Creates the visible components.
		 */
		@Override
		void createGUI() {
			setTitle(MediaRes.getString("Filter.Brightness.Title")); //$NON-NLS-1$
			addWindowFocusListener(new java.awt.event.WindowAdapter() {
				@Override
				public void windowLostFocus(java.awt.event.WindowEvent e) {
					if (isChanged() && previousState != null) {
						changed = false;
						firePropertyChange(Video.PROPERTY_VIDEO_FILTERCHANGED, previousState, BrightnessFilter.this); //$NON-NLS-1$
						previousState = null;
					}
				}
			});

			// create brightness components
			brightnessLabel = new JLabel();
			brightnessLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
			brightnessField = new IntegerField(3);
			brightnessField.setMaxValue(128);
			brightnessField.setMinValue(-128);
			brightnessField.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setBrightness(brightnessField.getIntValue());
					updateDisplay();
					brightnessField.selectAll();
				}

			});
			brightnessField.addFocusListener(new FocusListener() {
				@Override
				public void focusGained(FocusEvent e) {
					brightnessField.selectAll();
				}

				@Override
				public void focusLost(FocusEvent e) {
					setBrightness(brightnessField.getIntValue());
					updateDisplay();
				}

			});
			brightnessSlider = new JSlider(0, 0, 0);
			brightnessSlider.setMaximum(128);
			brightnessSlider.setMinimum(-128);
			brightnessSlider.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
			brightnessSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					int i = brightnessSlider.getValue();
					if (i != getBrightness()) {
						setBrightness(i);
						updateDisplay();
					}
				}

			});
			// create contrast components
			contrastLabel = new JLabel();
			contrastField = new DecimalField(4, 1);
			contrastField.setMaxValue(100);
			contrastField.setMinValue(0);
			contrastField.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setContrast(contrastField.getValue());
					updateDisplay();
					contrastField.selectAll();
				}

			});
			contrastField.addFocusListener(new FocusListener() {
				@Override
				public void focusGained(FocusEvent e) {
					contrastField.selectAll();
				}

				@Override
				public void focusLost(FocusEvent e) {
					setContrast(contrastField.getValue());
					updateDisplay();
				}

			});
			contrastSlider = new JSlider(0, 0, 0);
			contrastSlider.setMaximum(100);
			contrastSlider.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
			contrastSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					int i = contrastSlider.getValue();
					if (i != (int) getContrast()) {
						setContrast(i);
						updateDisplay();
					}
				}

			});
			// add components to JPanel with GridBag layout
			JLabel[] labels = new JLabel[] { brightnessLabel, contrastLabel };
			JTextField[] fields = new JTextField[] { brightnessField, contrastField };
			JSlider[] sliders = new JSlider[] { brightnessSlider, contrastSlider };
			GridBagLayout gridbag = new GridBagLayout();
			JPanel panel = new JPanel(gridbag);
//			setContentPane(panel);
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.EAST;
			int i = 0;
			for (; i < labels.length; i++) {
				c.gridy = i;
				c.fill = GridBagConstraints.NONE;
				c.weightx = 0.0;
				c.gridx = 0;
				c.insets = new Insets(5, 5, 0, 2);
				gridbag.setConstraints(labels[i], c);
				panel.add(labels[i]);
				c.fill = GridBagConstraints.HORIZONTAL;
				c.gridx = 1;
				c.insets = new Insets(5, 0, 0, 0);
				gridbag.setConstraints(fields[i], c);
				panel.add(fields[i]);
				c.gridx = 2;
				c.insets = new Insets(5, 0, 0, 0);
				c.weightx = 1.0;
				gridbag.setConstraints(sliders[i], c);
				panel.add(sliders[i]);
			}
			
			JPanel buttonbar = new JPanel();
			buttonbar.add(ableButton);
			buttonbar.add(clearButton);
			buttonbar.add(closeButton);
			
			JPanel contentPane = new JPanel(new BorderLayout());
			contentPane.add(panel, BorderLayout.NORTH);
			setContentPane(contentPane);
			contentPane.add(buttonbar, BorderLayout.SOUTH);
		}

		/**
		 * Initializes this inspector
		 */
		void initialize() {
			updateDisplay();
		}

		/**
		 * Updates this inspector to reflect the current filter settings.
		 */
		void updateDisplay() {
			brightnessField.setIntValue(getBrightness());
			contrastField.setValue(getContrast());
			brightnessSlider.setValue(getBrightness());
			contrastSlider.setValue((int) getContrast());
		}

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
			BrightnessFilter filter = (BrightnessFilter) obj;
			control.setValue("brightness", filter.getBrightness()); //$NON-NLS-1$
			control.setValue("contrast", filter.getContrast()); //$NON-NLS-1$
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
			return new BrightnessFilter();
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
			final BrightnessFilter filter = (BrightnessFilter) obj;
			if (control.getPropertyNamesRaw().contains("brightness")) { //$NON-NLS-1$
				filter.setBrightness(control.getInt("brightness")); //$NON-NLS-1$
			}
			if (control.getPropertyNamesRaw().contains("contrast")) { //$NON-NLS-1$
				filter.setContrast(control.getDouble("contrast")); //$NON-NLS-1$
			}
			filter.inspectorX = control.getInt("inspector_x"); //$NON-NLS-1$
			filter.inspectorY = control.getInt("inspector_y"); //$NON-NLS-1$
			filter.previousState = null;
			filter.changed = false;
			if (filter.inspector != null) {
				filter.inspector.updateDisplay();
			}
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
 * Copyright (c) 2024 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
