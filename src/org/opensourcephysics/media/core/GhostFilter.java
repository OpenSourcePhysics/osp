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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;

/**
 * This is a Filter that produces fading ghost images of bright objects on a
 * dark background.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class GhostFilter extends Filter {
	// instance fields
	protected double fade;
	protected double defaultFade = 0.05;
	// inspector fields
	protected Inspector inspector;
	protected JLabel fadeLabel;
	protected NumberField fadeField;
	protected JSlider fadeSlider;
	protected int[] values;

	String prefix = "Filter.Ghost";

	/**
	 * Constructs a GhostFilter object with default fade.
	 */
	public GhostFilter() {
		setFade(defaultFade);
		hasInspector = true;
	}

	/**
	 * Sets the fade.
	 *
	 * @param fade the fraction by which a ghost image fades each time it is
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
	 * 
	 * Clears ghosts.
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
		fadeLabel.setText(MediaRes.getString(prefix + ".Label.Fade")); //$NON-NLS-1$
		fadeSlider.setToolTipText(MediaRes.getString(prefix + ".ToolTip.Fade")); //$NON-NLS-1$
		boolean enabled = isEnabled();
		fadeLabel.setEnabled(enabled);
		fadeSlider.setEnabled(enabled);
		fadeField.setEnabled(enabled);
		inspector.setTitle(MediaRes.getString(prefix + ".Title")); //$NON-NLS-1$
		inspector.pack();
	}

	// _____________________________ protected methods _______________________

	/**
	 * Creates and initializes the input and output images.
	 *
	 * @param image a new source image
	 */
	@Override
	protected void initializeSubclass() {
		getPixelsIn();
		values = new int[nPixelsIn];
		for (int i = 0; i < nPixelsIn; i++) {
			int pixel = pixelsIn[i];
			values[i] = (((pixel >> 16) & 0xff) + ((pixel >> 8) & 0xff) + (pixel & 0xff)) / 3; // value
		}
	}

	/**
	 * Sets the output image pixels to a ghost of the input pixels.
	 */
	@Override
	protected void setOutputPixels() {
		getPixelsIn();
		getPixelsOut();
		for (int i = 0; i < nPixelsIn; i++) {
			int pixel = pixelsIn[i];
			// value of current input pixel
			int v = (((pixel >> 16) & 0xff) + ((pixel >> 8) & 0xff) + (pixel & 0xff)) / 3;
			int ghost = (int) ((1 - fade) * values[i]); // faded value of prev input
			if (ghost > v) {
				pixelsOut[i] = (ghost << 16) | (ghost << 8) | ghost; // grey
				values[i] = ghost;
			} else {
				pixelsOut[i] = pixel;
				values[i] = v;
			}
		}
	}

	/**
	 * Inner Inspector class to control filter parameters
	 */
	protected class Inspector extends InspectorDlg {
		/**
		 * Constructs the Inspector.
		 */
		public Inspector() {
			super(prefix + ".Title"); //$NON-NLS-1$
		}

		/**
		 * Creates the visible components.
		 */
		@Override
		void createGUI() {
			// create components
			fadeLabel = new JLabel();
			fadeLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
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
			// add components to content pane
			GridBagLayout gridbag = new GridBagLayout();
			JPanel panel = new JPanel(gridbag);
//			setContentPane(panel);
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.EAST;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0.0;
			c.gridx = 0;
			c.insets = new Insets(5, 5, 0, 2);
			gridbag.setConstraints(fadeLabel, c);
			panel.add(fadeLabel);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 1;
			c.insets = new Insets(5, 0, 0, 0);
			gridbag.setConstraints(fadeField, c);
			panel.add(fadeField);
			c.gridx = 2;
			c.insets = new Insets(5, 0, 0, 0);
			c.weightx = 1.0;
			gridbag.setConstraints(fadeSlider, c);
			panel.add(fadeSlider);
			
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
			refresh();
		}

		/**
		 * Updates this inspector to reflect the current filter settings.
		 */
		void updateDisplay() {
			fadeField.setValue(getFade());
			fadeSlider.setValue((int) (100 * getFade()));
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
			GhostFilter filter = (GhostFilter) obj;
			control.setValue("fade", filter.getFade()); //$NON-NLS-1$
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
			return new GhostFilter();
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
			final GhostFilter filter = (GhostFilter) obj;
			if (control.getPropertyNamesRaw().contains("fade")) { //$NON-NLS-1$
				filter.setFade(control.getDouble("fade")); //$NON-NLS-1$
			}
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
