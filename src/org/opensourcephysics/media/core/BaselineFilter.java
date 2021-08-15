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
 * Copyright (c) 2017 Douglas Brown and Wolfgang Christian.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
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
 * For additional information and documentation on Open Source Physics, please
 * see <http://www.opensourcephysics.org/>.
 */
package org.opensourcephysics.media.core;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.ResourceLoader;

import javajs.async.AsyncFileChooser;

/**
 * This is a Filter that subtracts a baseline image from the source image.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class BaselineFilter extends Filter {
	// instance fields
	private BufferedImage baseline;

	private int[] baselinePixels;
	private Inspector inspector;
	private String imagePath;
	private JButton loadButton;
	private JButton captureButton;

	/**
	 * Constructs a default BaselineFilter.
	 */
	public BaselineFilter() {
		hasInspector = true;
	}

	/**
	 * Captures the current video frame to use as baseline image.
	 */
	public void capture() {
		if ((vidPanel == null) || (vidPanel.getVideo() == null)) {
			return;
		}
		setBaselineImage(vidPanel.getVideo().getImage());
	}

	/**
	 * Loads a baseline image from the specified path.
	 *
	 * @param path the image path
	 */
	public void load(String path) {
		BufferedImage image = ResourceLoader.getBufferedImage(path);
		if (image != null) {
			imagePath = path;
			setBaselineImage(image);
		} else {
			JOptionPane.showMessageDialog(vidPanel, "\"" + path + "\" " + //$NON-NLS-1$ //$NON-NLS-2$
					MediaRes.getString("Filter.Baseline.Dialog.NotImage.Message"), //$NON-NLS-1$
					MediaRes.getString("Filter.Baseline.Dialog.NotImage.Title"), //$NON-NLS-1$
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	/**
	 * Loads an image with a file chooser.
	 */
	public void load() {
		AsyncFileChooser chooser = OSPRuntime.getChooser();
//		FontSizer.setFonts(chooser, FontSizer.getLevel());
		chooser.showOpenDialog(null, new Runnable() {
			@Override
			public void run() {
				File file = chooser.getSelectedFile();
				load(file.getAbsolutePath());
			}
		}, null);
	}

	/**
	 * Sets the baseline image.
	 *
	 * @param image the image
	 */
	public void setBaselineImage(BufferedImage image) {
		baseline = image;
		if (image != null) {
			int wi = image.getWidth();
			int ht = image.getHeight();
			if ((wi >= w) && (ht >= h)) {
				getRaster(image).getDataElements(0, 0, w, h, baselinePixels);
			} else {
				JOptionPane.showMessageDialog(vidPanel,
						MediaRes.getString("Filter.Baseline.Dialog.SmallImage.Message1") + " (" + wi + "x" + ht + ") " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
								MediaRes.getString("Filter.Baseline.Dialog.SmallImage.Message2") + " (" + w + "x" + h //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								+ ").", //$NON-NLS-1$
						MediaRes.getString("Filter.Baseline.Dialog.SmallImage.Title"), //$NON-NLS-1$
						JOptionPane.INFORMATION_MESSAGE);
			}
		}
		firePropertyChange("baseline", null, null); //$NON-NLS-1$
	}

	/**
	 * Gets the baseline image being subtracted.
	 *
	 * @return the image
	 */
	public BufferedImage getBaselineImage() {
		return baseline;
	}

	/**
	 * Implements abstract Filter method.
	 *
	 * @return the inspector
	 */
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
		setBaselineImage(null);
	}

	/**
	 * Refreshes this filter's GUI
	 */
	@Override
	public void refresh() {
		if (inspector == null || !haveGUI)
			return;
		super.refresh();
		loadButton.setText(MediaRes.getString("Filter.Baseline.Button.Load")); //$NON-NLS-1$
		captureButton.setText(MediaRes.getString("Filter.Baseline.Button.Capture")); //$NON-NLS-1$
		captureButton.setText(MediaRes.getString("Filter.Baseline.Button.Capture")); //$NON-NLS-1$
		loadButton.setEnabled(isEnabled());
		captureButton.setEnabled(isEnabled());
		inspector.setTitle(MediaRes.getString("Filter.Baseline.Title")); //$NON-NLS-1$
		inspector.pack();
	}

	// _____________________________ private methods _______________________

	/**
	 * Creates new input, output and baseline images.
	 *
	 * @param image a new source image
	 */
	@Override
	protected void initializeSubclass() {
		baselinePixels = new int[nPixelsIn];
	}

	/**
	 * Sets the output to an image-subtracted version of the input.
	 */
	@Override
	protected void setOutputPixels() {
		getPixelsIn();
		getPixelsOut();
		if (baseline != null) {
			int pixel, base, r, g, b;
			for (int i = 0; i < nPixelsIn; i++) {
				pixel = pixelsIn[i];
				base = baselinePixels[i];
				r = (pixel >> 16) & 0xff; // red
				r = r - ((base >> 16) & 0xff);
				r = Math.max(r, 0);
				g = (pixel >> 8) & 0xff; // green
				g = g - ((base >> 8) & 0xff);
				g = Math.max(g, 0);
				b = pixel & 0xff; // blue
				b = b - (base & 0xff);
				b = Math.max(b, 0);
				pixelsOut[i] = (r << 16) | (g << 8) | b;
			}
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
			super("Filter.Baseline.Title"); //$NON-NLS-1$
		}

		/**
		 * Creates the visible components.
		 */
		@Override
		void createGUI() {
			// create buttons
			loadButton = new JButton();
			loadButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					load();
				}

			});
			// create buttons
			captureButton = new JButton();
			captureButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					capture();
				}

			});
			// add components to content pane
			JPanel buttonbar = new JPanel(new FlowLayout());
			setContentPane(buttonbar);
			buttonbar.add(ableButton);
			buttonbar.add(loadButton);
			buttonbar.add(captureButton);
			buttonbar.add(clearButton);
			buttonbar.add(closeButton);
		}

		/**
		 * Initializes this inspector
		 */
		void initialize() {
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
			BaselineFilter filter = (BaselineFilter) obj;
			if (filter.imagePath != null) {
				control.setValue("imagepath", filter.imagePath); //$NON-NLS-1$
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
			return new BaselineFilter();
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
			final BaselineFilter filter = (BaselineFilter) obj;
			if (control.getPropertyNamesRaw().contains("imagepath")) { //$NON-NLS-1$
				filter.load(control.getString("imagepath")); //$NON-NLS-1$
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
