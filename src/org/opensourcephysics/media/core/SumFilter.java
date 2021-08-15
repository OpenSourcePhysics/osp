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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;

/**
 * This is a Filter that sums pixel values from multiple images.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class SumFilter extends Filter {
	// instance fields
	protected int[] rsums, gsums, bsums;
	private int imageCount = 1;
	private double brightness = 1; // fraction of full
	private boolean mean;
	private boolean skipSum = true;
	// inspector fields
	private Inspector inspector;
	private JLabel percentLabel;
	private DecimalField percentField;
	private JSlider percentSlider;
	private JCheckBox showMeanCheckBox;
	private JLabel frameCountLabel;
	private IntegerField frameCountField;

	/**
	 * Constructs a SumFilter.
	 */
	public SumFilter() {
		hasInspector = true;
	}

	/**
	 * Sets the brightness fraction.
	 *
	 * @param fraction the brightness as a fraction of full
	 */
	public void setBrightness(double fraction) {
		if (fraction != brightness) {
			brightness = Math.abs(fraction);
			firePropertyChange(Filter.PROPERTY_FILTER_BRIGHTNESS, null, null);
		}
	}

	/**
	 * Sets the mean flag.
	 *
	 * @param mean <code>true</code> to show the mean
	 */
	public void setMean(boolean mean) {
		if (this.mean != mean) {
			this.mean = mean;
			refresh();
			firePropertyChange(Filter.PROPERTY_FILTER_MEAN, null, null);
		}
	}

	/**
	 * Overrides Filter method.
	 *
	 * @param enabled <code>true</code> to enable this filter.
	 */
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		refresh();
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
		if (source != null) {
			initializeSource(source);
			brightness = 1;
			skipSum = true;
			firePropertyChange(Filter.PROPERTY_FILTER_RESET, null, null);
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
		percentLabel.setText(MediaRes.getString("Filter.Sum.Label.Percent")); //$NON-NLS-1$
		percentField.setToolTipText(MediaRes.getString("Filter.Sum.ToolTip.Percent")); //$NON-NLS-1$
		percentSlider.setToolTipText(MediaRes.getString("Filter.Sum.ToolTip.Percent")); //$NON-NLS-1$
		showMeanCheckBox.setText(MediaRes.getString("Filter.Sum.CheckBox.ShowMean")); //$NON-NLS-1$
		frameCountLabel.setText(MediaRes.getString("Filter.Sum.Label.FrameCount")); //$NON-NLS-1$
		boolean enabled = isEnabled();
		showMeanCheckBox.setEnabled(enabled);
		frameCountLabel.setEnabled(enabled);
		frameCountField.setEnabled(enabled);
		percentLabel.setEnabled(enabled && !mean);
		percentField.setEnabled(enabled && !mean);
		percentSlider.setEnabled(enabled && !mean);
		frameCountField.setIntValue(imageCount);
		if (mean) {
			percentField.setValue(100.0 / imageCount);
			percentSlider.setValue(Math.round((float) 100.0 / imageCount));
		} else {
			percentField.setValue(brightness * 100);
			percentSlider.setValue(Math.round((float) brightness * 100));
		}
		inspector.setTitle(MediaRes.getString("Filter.Sum.Title")); //$NON-NLS-1$
		inspector.pack();
	}

	/**
	 * Requests that this filter add the next image it receives
	 */
	public void addNextImage() {
		skipSum = false;
	}

	// _____________________________ private methods _______________________

	/**
	 * Creates and initializes the input and output images.
	 *
	 * @param image a new source image
	 */
	@Override
	protected void initializeSubclass() {
		rsums = new int[nPixelsIn];
		gsums = new int[nPixelsIn];
		bsums = new int[nPixelsIn];
		getPixelsIn();
		getPixelsOut();
		System.arraycopy(pixelsIn, 0, pixelsOut, 0, nPixelsIn);
		imageCount = 0;
		addPixels();
	}

	/**
	 * Adds the input pixel RGB values to their sums.
	 */
	private void addPixels() {
		imageCount++;
		getPixelsIn();
		for (int i = 0; i < nPixelsIn; i++) {
			int pixel = pixelsIn[i];
			rsums[i] += (pixel >> 16) & 0xff; // red
			gsums[i] += (pixel >> 8) & 0xff; // green
			bsums[i] += (pixel) & 0xff; // blue
		}
		if ((inspector != null) && inspector.isVisible()) {
			refresh();
		}
	}

	/**
	 * Sets the output image pixels to the reduced sum values.
	 */
	@Override
	protected void setOutputPixels() {
		if (!skipSum) {
			addPixels();
			skipSum = true;
		}
		getPixelsOut();
		double f = (mean ? 1.0 / imageCount : brightness);
		for (int i = 0; i < nPixelsIn; i++) {
			pixelsOut[i] = (((int) Math.min(rsums[i] * f, 255)) << 16) 
					| (((int) Math.min(gsums[i] * f, 255)) << 8) 
					| ((int) Math.min(bsums[i] * f, 255));
		}
		if (mean && (inspector != null)) {
			refresh();
		}
	}

	/**
	 * Inner Inspector class to control filter parameters
	 */
	private class Inspector extends InspectorDlg {
		/**
		 * Constructs the Inspector.
		 */
		public Inspector() {
			super("Filter.Sum.Title"); //$NON-NLS-1$
		}

		/**
		 * Creates the visible components.
		 */
		@Override
		void createGUI() {
			// create components
			percentLabel = new JLabel();
			percentLabel.setHorizontalAlignment(SwingConstants.TRAILING);
			percentField = new DecimalField(3, 1);
			percentField.setMaxValue(100);
			percentField.setMinValue(0);
			percentField.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setBrightness(percentField.getValue() / 100);
					refresh();
					percentField.selectAll();
				}

			});
			percentField.addFocusListener(new FocusListener() {
				@Override
				public void focusGained(FocusEvent e) {
					percentField.selectAll();
				}

				@Override
				public void focusLost(FocusEvent e) {
					setBrightness(percentField.getValue() / 100);
					refresh();
				}

			});
			percentSlider = new JSlider(0, 100, 100);
			percentSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					if (percentSlider.isEnabled()
							&& (percentSlider.getValue() != Math.round((float) brightness * 100))) {
						setBrightness(percentSlider.getValue() / 100.0);
						refresh();
					}
				}

			});
			showMeanCheckBox = new JCheckBox();
			showMeanCheckBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setMean(showMeanCheckBox.isSelected());
					refresh();
				}

			});
			frameCountLabel = new JLabel();
			frameCountLabel.setHorizontalAlignment(SwingConstants.TRAILING);
			frameCountField = new IntegerField(3);
			frameCountField.setEditable(false);
			// add components to content pane
			JPanel contentPane = new JPanel(new BorderLayout());
			setContentPane(contentPane);
			GridBagLayout gridbag = new GridBagLayout();
			JPanel panel = new JPanel(gridbag);
			contentPane.add(panel, BorderLayout.CENTER);
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.WEST;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0.0;
			c.gridx = 0;
			c.insets = new Insets(5, 5, 0, 2);
			gridbag.setConstraints(percentLabel, c);
			panel.add(percentLabel);
			c.anchor = GridBagConstraints.EAST;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 1;
			c.insets = new Insets(5, 0, 0, 0);
			gridbag.setConstraints(percentField, c);
			panel.add(percentField);
			c.gridx = 2;
			c.weightx = 1.0;
			c.insets = new Insets(5, 0, 0, 5);
			gridbag.setConstraints(percentSlider, c);
			panel.add(percentSlider);
			c.weightx = 0.0;
			c.gridx = 0;
			c.gridy = 1;
			c.insets = new Insets(5, 5, 0, 2);
			c.anchor = GridBagConstraints.WEST;
			gridbag.setConstraints(frameCountLabel, c);
			panel.add(frameCountLabel);
			c.gridx = 1;
			c.insets = new Insets(5, 0, 0, 0);
			c.anchor = GridBagConstraints.EAST;
			gridbag.setConstraints(frameCountField, c);
			panel.add(frameCountField);
			c.gridx = 2;
			c.insets = new Insets(8, 0, 0, 0);
			gridbag.setConstraints(showMeanCheckBox, c);
			panel.add(showMeanCheckBox);
			JPanel buttonbar = new JPanel(new FlowLayout());
			buttonbar.add(ableButton);
			buttonbar.add(clearButton);
			buttonbar.add(closeButton);
			contentPane.add(buttonbar, BorderLayout.SOUTH);
		}

		/**
		 * Initializes this inspector
		 */
		void initialize() {
			showMeanCheckBox.setSelected(mean);
			refresh();
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
			SumFilter filter = (SumFilter) obj;
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
			return new SumFilter();
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
			final SumFilter filter = (SumFilter) obj;
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
