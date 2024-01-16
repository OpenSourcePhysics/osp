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
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;

/**
 * This is a Filter that applies a log transform to the source.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class LogFilter extends Filter {
	// instance fields
	private int level = Integer.MAX_VALUE;
	private int[] lookup = new int[256];
	private boolean grayscale;
	
	// inspector fields
	private Inspector inspector;
	private JLabel levelLabel, highlightLabel, shadowLabel;
	private IntegerField field;
	private JSlider slider;
	private JCheckBox grayCheckbox;

	/**
	 * Constructs a LogFilter object.
	 */
	public LogFilter() {
		hasInspector = true;
		setLevel(0);
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
		levelLabel.setText(MediaRes.getString("Filter.Log.Label.Level")); //$NON-NLS-1$
		highlightLabel.setText(MediaRes.getString("Filter.Log.Label.Highlights")); //$NON-NLS-1$
		shadowLabel.setText(MediaRes.getString("Filter.Log.Label.Shadows")); //$NON-NLS-1$
		slider.setToolTipText(MediaRes.getString("Filter.Log.ToolTip.Level")); //$NON-NLS-1$
		field.setToolTipText(MediaRes.getString("Filter.Log.ToolTip.Level")); //$NON-NLS-1$
		grayCheckbox.setText(MediaRes.getString("Filter.Log.Checkbox.Grayscale")); //$NON-NLS-1$
//		levelLabel.setEnabled(isEnabled());
		field.setEnabled(isEnabled());
		slider.setEnabled(isEnabled());
		grayCheckbox.setEnabled(isEnabled());
		grayCheckbox.setSelected(grayscale);
		inspector.setTitle(MediaRes.getString("Filter.Log.Title")); //$NON-NLS-1$
		inspector.pack();
	}

	// _____________________________ private methods _______________________

	/**
	 * Creates the input and output images.
	 *
	 * @param image a new input image
	 */
	@Override
	protected void initializeSubclass() {
		// nothing to do
	}

	final static float[] hsb = new float[3];
	/**
	 * Sets the output image pixels.
	 */
	@Override
	protected void setOutputPixels() {
		getPixelsIn();
		getPixelsOut();
		for (int i = 0; i < nPixelsIn; i++) {
			int pixel = pixelsIn[i];
			int r = (pixel >> 16) & 0xff;
			int g = (pixel >> 8) & 0xff;
			int b = pixel & 0xff;
			if (grayscale) {
				// this is fast
				int gray = getGray(r, g, b);
				gray = lookup[gray];
				pixelsOut[i] = (gray << 16) | (gray << 8) | gray;
			}
			else {
				// much slower
				Color.RGBtoHSB(r, g, b, hsb);
				int gray = (int)(hsb[2]*255);
				gray = lookup[gray];
				pixelsOut[i] = Color.HSBtoRGB(hsb[0], hsb[1], (float)(gray/255.0));
			}
		}
	}

	/**
	 * Returns the grayscale brightness.
	 *
	 * @return the brightness
	 */
	private final static int getGray(int r, int g, int b) {
		return (int) ((r + g + b) / 3);
	}

	/**
	 * Sets the level
	 * 
	 * @param level integer between +/-100
	 */
	private void setLevelOrig(int level) {
		if (level != 0 && this.level == level)
			return;
		this.level = level;
		double lim = Math.abs(0.01 * level);
		double a = Math.min(1, 5 * lim);
		lim = a * Math.exp(6 * lim);
		double b = lim / Math.log10(lim + 1);
		double c = level == 0? 1: 255 / lim;
		for (int i = 0; i < 256; i++) {
			lookup[i] = (level == 0 ? i: (int) Math.round(c * b * Math.log10(1 + i/c)));
		}
		if (level < 0) {
			int[] newLookup = new int[256];
			for (int i = 0; i < 256; i++) {
				newLookup[i] = 255 - lookup[255 - i];
			}
			lookup = newLookup;
		}
		firePropertyChange("level", null, null);
	}

	/**
	 * Sets the level
	 * 
	 * @param level integer between +/-100
	 */
	private void setLevel(int level) {
		if (this.level == level)
			return;
		this.level = level;
		if (level == 0) {
			for (int i = 0; i < 256; i++) {
				lookup[i] = i;
			}
			return;
		} 
		double lim = Math.abs(0.01 * level);
		lim = Math.min(1, 5 * lim) * Math.exp(6 * lim);
		double c = 255 / lim;
		double bc = 255 / Math.log10(lim + 1);
		if (level > 0) {
			for (int i = 0; i < 256; i++) {
				lookup[i] = (int) Math.round(bc * Math.log10(1 + i / c));
			}
		} else {
			for (int i = 0; i < 256; i++) {
				lookup[i] = 255 - ((int) Math.round(bc * Math.log10(1 + (255 - i) / c)));
			}
		}
		firePropertyChange("level", null, null);
	}

	
	private int getLevel() {
		return level;
	}
	
	private void setGrayscale(boolean gray) {
		if (grayscale == gray)
			return;
		grayscale = gray;
		firePropertyChange("grayscale", null, null);
	}

	/**
	 * Inner Inspector class to control filter parameters
	 */
	private class Inspector extends InspectorDlg {
		/**
		 * Constructs the Inspector.
		 */
		public Inspector() {
			super("Filter.GrayScale.Title"); //$NON-NLS-1$
		}

		/**
		 * Creates the visible components.
		 */
		@Override
		void createGUI() {
			// create components
			levelLabel = new JLabel();
			levelLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
			highlightLabel = new JLabel();
			highlightLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
			shadowLabel = new JLabel();
			shadowLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
			field = new IntegerField(3);
			field.setMaxValue(100);
			field.setMinValue(-100);
			field.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setLevel(field.getIntValue());
					updateDisplay();
					field.selectAll();
				}

			});
			field.addFocusListener(new FocusListener() {
				@Override
				public void focusGained(FocusEvent e) {
					field.selectAll();
				}

				@Override
				public void focusLost(FocusEvent e) {
					setLevel(field.getIntValue());
					updateDisplay();
				}

			});
			slider = new JSlider(0, 0, 0);
			slider.setMaximum(100);
			slider.setMinimum(-100);
			slider.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
			slider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					int i = slider.getValue();
					if (i != getLevel()) {
						setLevel(i);
						updateDisplay();
					}
				}

			});
			grayCheckbox = new JCheckBox();
			grayCheckbox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setGrayscale(grayCheckbox.isSelected());
				}
			});

			
			// add components to content pane
			JPanel contentPane = new JPanel(new BorderLayout());
			setContentPane(contentPane);
			JPanel panel = new JPanel();
			panel.add(highlightLabel);
			panel.add(slider);
			panel.add(shadowLabel);
			contentPane.add(panel, BorderLayout.NORTH);
			
			panel = new JPanel();
			panel.add(levelLabel);
			panel.add(field);
			panel.add(grayCheckbox);
			contentPane.add(panel, BorderLayout.CENTER);

			JPanel buttonbar = new JPanel();
			buttonbar.add(ableButton);
			buttonbar.add(closeButton);
			contentPane.add(buttonbar, BorderLayout.SOUTH);
		}

		/**
		 * Initializes this inspector
		 */
		void initialize() {
			refresh();
			updateDisplay();
		}

		/**
		 * Updates this inspector to reflect the current filter settings.
		 */
		void updateDisplay() {
			field.setIntValue(getLevel());
			slider.setValue(getLevel());
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
			LogFilter filter = (LogFilter) obj;
			control.setValue("level", filter.getLevel()); //$NON-NLS-1$
			control.setValue("grayscale", filter.grayscale); //$NON-NLS-1$
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
			return new LogFilter();
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
			final LogFilter filter = (LogFilter) obj;
			if (control.getPropertyNamesRaw().contains("level")) { //$NON-NLS-1$
				filter.setLevel(control.getInt("level")); //$NON-NLS-1$
			}
			filter.grayscale = control.getBoolean("grayscale");
			filter.inspectorX = control.getInt("inspector_x"); //$NON-NLS-1$
			filter.inspectorY = control.getInt("inspector_y"); //$NON-NLS-1$
			return obj;
		}

	}
	
	public static void main(String[] args) {
		LogFilter f;
		int[] l;
		for (int i = -100; i <= 100; i++) {
			f = new LogFilter();
			f.setLevelOrig(i);
			l = f.lookup;
			f = new LogFilter();
			f.setLevel(i);
			if (!Arrays.equals(l, f.lookup)) {
				System.out.println(Arrays.toString(l));
				System.out.println(Arrays.toString(f.lookup));
				throw new NumberFormatException("oops");
			}
		}
		System.out.println("OK");
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
