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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;

/**
 * This is a Filter that produces fading strobe images.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class StrobeFilter extends Filter {
	// instance fields
	protected int[] prevPixels;
	private double fade;
	private double defaultFade = 0;
	private boolean brightTrails = false;
	// inspector fields
	private Inspector inspector;
	private JLabel fadeLabel;
	private NumberField fadeField;
	private JSlider fadeSlider;
	private JRadioButton darkButton, brightButton;

	/**
	 * Constructs a StrobeFilter object with default fade.
	 */
	public StrobeFilter() {
		setFade(defaultFade);
		hasInspector = true;
	}

	/**
	 * Sets the fade.
	 *
	 * @param fade the fraction by which the strobe image fades each time it is
	 *             rendered. A fade of 0 never fades, while a fade of 1 fades
	 *             completely and so is never seen.
	 */
	public void setFade(double fade) {
		Double prev = Double.valueOf(this.fade);
		this.fade = Math.min(Math.abs(fade), 1);
		firePropertyChange("fade", prev, Double.valueOf(fade)); //$NON-NLS-1$
	}

	/**
	 * Gets the fade.
	 *
	 * @return the fade.
	 * @see #setFade
	 */
	public double getFade() {
		return fade;
	}

	/**
	 * Sets the bright trails flag. When true, trails are bright on dark backgrounds
	 * and fade to black. Otherwise trails are dark on bright backgrounds and fade
	 * to white.
	 *
	 * @param light true for bright trails on dark backgrounds
	 */
	public void setBrightTrails(boolean bright) {
		brightTrails = bright;
		clear();
	}

	/**
	 * Gets the bright trails flag.
	 *
	 * @return true if trails are bright
	 * @see #setBrightTrails
	 */
	public boolean isBrightTrails() {
		return brightTrails;
	}

	/**
	 * Overrides the setEnabled method to force reinitialization.
	 *
	 * @param enabled <code>true</code> if this is enabled.
	 */
	@Override
	public void setEnabled(boolean enabled) {
		if (isEnabled() == enabled) {
			return;
		}
		source = null;
		super.setEnabled(enabled);
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
	 * Clears strobe images.
	 */
	@Override
	public void clear() {
		source = null;
		firePropertyChange("image", null, null); //$NON-NLS-1$
	}

	/**
	 * Refreshes this filter's GUI
	 */
	@Override
	public void refresh() {
		if (inspector == null || !haveGUI)
			return;
		super.refresh();
		fadeLabel.setText(MediaRes.getString("Filter.Ghost.Label.Fade")); //$NON-NLS-1$
		fadeSlider.setToolTipText(MediaRes.getString("Filter.Ghost.ToolTip.Fade")); //$NON-NLS-1$
		brightButton.setText(MediaRes.getString("Filter.Strobe.RadioButton.Bright")); //$NON-NLS-1$
		brightButton.setToolTipText(MediaRes.getString("Filter.Strobe.RadioButton.Bright.Tooltip")); //$NON-NLS-1$
		darkButton.setText(MediaRes.getString("Filter.Strobe.RadioButton.Dark")); //$NON-NLS-1$
		darkButton.setToolTipText(MediaRes.getString("Filter.Strobe.RadioButton.Dark.Tooltip")); //$NON-NLS-1$

		boolean enabled = isEnabled();
		brightButton.setEnabled(enabled);
		darkButton.setEnabled(enabled);
		fadeLabel.setEnabled(enabled);
		fadeSlider.setEnabled(enabled);
		fadeField.setEnabled(enabled);
		inspector.setTitle(MediaRes.getString("Filter.Strobe.Title")); //$NON-NLS-1$
		inspector.pack();
	}

	// _____________________________ private methods _______________________

	/**
	 * Creates and initializes the input and output images.
	 *
	 * @param image a new source image
	 */
	@Override
	protected void initializeSubclass() {
		if (prevPixels == null || prevPixels.length != nPixelsIn)
			prevPixels = new int[nPixelsIn];
		getPixelsIn();
		System.arraycopy(pixelsIn, 0, prevPixels, 0, nPixelsIn);
	}

	/**
	 * Sets the output image pixels to a strobe of the input pixels.
	 */
	@Override
	protected void setOutputPixels() {
		getPixelsIn();
		getPixelsOut();
		int pixel, r, g, b, val, rprev, gprev, bprev, valprev;
		for (int i = 0; i < pixelsIn.length; i++) {
			pixel = pixelsIn[i];
			r = (pixel >> 16) & 0xff; // red
			g = (pixel >> 8) & 0xff; // green
			b = (pixel) & 0xff; // blue
			val = (r + g + b) / 3; // value of current input pixel
			rprev = (prevPixels[i] >> 16) & 0xff; // previous red
			gprev = (prevPixels[i] >> 8) & 0xff; // previous green
			bprev = (prevPixels[i]) & 0xff; // previous blue
			valprev = (rprev + gprev + bprev) / 3; // previous value
			if (brightTrails) { // bright trails fade to black
				valprev = (int) ((1 - fade) * valprev); // faded previous value
				if (valprev > val) {
					rprev = (int) ((1 - fade) * rprev); // faded red
					gprev = (int) ((1 - fade) * gprev); // faded green
					bprev = (int) ((1 - fade) * bprev); // faded blue
					pixelsOut[i] = (rprev << 16) | (gprev << 8) | bprev;
				} else {
					pixelsOut[i] = pixel;
				}
			} else { // dark trails fade to white
				valprev = (int) (255 - (1 - fade) * (255 - valprev)); // faded previous value
				if (val > valprev) {
					rprev = (int) (255 - (1 - fade) * (255 - rprev)); // faded red
					gprev = (int) (255 - (1 - fade) * (255 - gprev)); // faded green
					bprev = (int) (255 - (1 - fade) * (255 - bprev)); // faded blue
					pixelsOut[i] = (rprev << 16) | (gprev << 8) | bprev;
				} else {
					pixelsOut[i] = pixel;
				}
			}
			prevPixels[i] = pixelsOut[i];
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
			super("Filter.Strobe.Title"); //$NON-NLS-1$
		}

		/**
		 * Creates the visible components.
		 */
		@Override
		void createGUI() {
			// create components
			fadeLabel = new JLabel();
			fadeLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
			fadeField = new DecimalField(4, 2);
			fadeField.setMaxValue(0.5);
			fadeField.setMinValue(0);
			fadeField.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setFade(fadeField.getValue());
					updateDisplay();
					fadeField.selectAll();
				}

			});
			fadeField.addFocusListener(new FocusListener() {
				@Override
				public void focusGained(FocusEvent e) {
					fadeField.selectAll();
				}

				@Override
				public void focusLost(FocusEvent e) {
					setFade(fadeField.getValue());
					updateDisplay();
				}

			});
			fadeSlider = new JSlider(0, 0, 0);
			fadeSlider.setMaximum(50);
			fadeSlider.setMinimum(0);
			fadeSlider.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
			fadeSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					int i = fadeSlider.getValue();
					if (i != (int) (getFade() * 100)) {
						setFade(i / 100.0);
						updateDisplay();
					}
				}

			});

			ButtonGroup group = new ButtonGroup();
			ActionListener brightDarkAction = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setBrightTrails(brightButton.isSelected());
				}
			};
			brightButton = new JRadioButton();
			darkButton = new JRadioButton();
			group.add(brightButton);
			group.add(darkButton);
			darkButton.setSelected(!brightTrails);
			brightButton.addActionListener(brightDarkAction);
			darkButton.addActionListener(brightDarkAction);

			JPanel panel = new JPanel(new BorderLayout());
			setContentPane(panel);
			JPanel fadePanel = new JPanel(new FlowLayout());
			fadePanel.add(fadeLabel);
			fadePanel.add(fadeField);
			fadePanel.add(fadeSlider);
			panel.add(fadePanel, BorderLayout.NORTH);

			JPanel brightDarkBar = new JPanel(new FlowLayout());
			brightDarkBar.add(brightButton);
			brightDarkBar.add(darkButton);
			panel.add(brightDarkBar, BorderLayout.CENTER);

			JPanel buttonbar = new JPanel(new FlowLayout());
			buttonbar.add(ableButton);
			buttonbar.add(clearButton);
			buttonbar.add(closeButton);
			panel.add(buttonbar, BorderLayout.SOUTH);

		}

		/**
		 * Initializes this inspector
		 */
		void initialize() {
			updateDisplay();
			refresh();
		}

		/**
		 * Updates this inspector to reflect the current filter settings.
		 */
		void updateDisplay() {
			fadeField.setValue(getFade());
			fadeSlider.setValue((int) (100 * getFade()));
			if (isBrightTrails())
				brightButton.setSelected(true);
			else
				darkButton.setSelected(true);
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
			StrobeFilter filter = (StrobeFilter) obj;
			control.setValue("fade", filter.getFade()); //$NON-NLS-1$
			if (filter.isBrightTrails()) {
				control.setValue("bright_trails", filter.isBrightTrails()); //$NON-NLS-1$
			}
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
			return new StrobeFilter();
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
			StrobeFilter filter = (StrobeFilter) obj;
			if (control.getPropertyNamesRaw().contains("fade")) { //$NON-NLS-1$
				filter.setFade(control.getDouble("fade")); //$NON-NLS-1$
			}
			filter.setBrightTrails(control.getBoolean("bright_trails")); //$NON-NLS-1$
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
 * Copyright (c) 2024 The Open Source Physics project
 * http://www.opensourcephysics.org
 */
