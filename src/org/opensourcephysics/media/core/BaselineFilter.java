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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * This is a Filter that subtracts a baseline image from the source image.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class BaselineFilter extends Filter {
	// instance fields
	private BufferedImage baseline, baselineCopy, thumbnail;

	private int[] baselinePixels;
	private Inspector inspector;
	private String imagePath;
	private JButton loadButton, captureButton, saveButton;
	private JLabel imageLabel;
	private JPanel contentPane;
	private Border imageBorder, emptyBorder;
	private JPanel nullBaselinePanel;

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
		imagePath = null;
	}

	/**
	 * Saves the current baseline image.
	 */
	public void save() {
		if (baseline == null) {
			return;
		}
		VideoIO.getChooserFilesAsync("save image", (File[] f) -> {
			if (f != null && f[0] != null && f[0].getParent() != null) {
				String path = f[0].getPath();
				if (!VideoIO.jpgFileFilter.accept(f[0])) {
					path = XML.stripExtension(path) + ".png";
				}
				File file = VideoIO.writeImageFile(baselineCopy, path);
				if (file != null) {
					imagePath = file.getPath();
				}
			}
			return null;
		});
	}
	
	/**
	 * Loads a baseline image from the specified path.
	 *
	 * @param path the image path
	 */
	public void load(String path) {
		BufferedImage image = ResourceLoader.getBufferedImage(path);
		if (image != null) {
			String prevPath = imagePath;
			imagePath = path;
			if (!setBaselineImage(image)) {
				imagePath = prevPath;
			}
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
		VideoIO.getChooserFilesAsync("open image", (File[] f) -> {
			if (f != null && f[0] != null && VideoIO.imageFileFilter.accept(f[0])) {
				String path = f[0].getPath();
				load(path);
			}
			return null;
		});
	}

	/**
	 * Sets the baseline image.
	 *
	 * @param image the image
	 * @return true if the image was accepted
	 */
	public boolean setBaselineImage(BufferedImage image) {
		if (image != null) {
			int wi = image.getWidth();
			int ht = image.getHeight();
			
			if ((wi >= w) && (ht >= h)) { // acceptable dimensions
				// create copy for saving
				baselineCopy = new BufferedImage(wi, ht, image.getType());
				Graphics2D g2 = baselineCopy.createGraphics();
				g2.drawImage(image, 0, 0, null);
				g2.dispose();
				
				baseline = image;
				getRaster(baseline).getDataElements(0, 0, w, h, baselinePixels);
				
			} else {
				JOptionPane.showMessageDialog(vidPanel.getTopLevelAncestor(),
						MediaRes.getString("Filter.Baseline.Dialog.SmallImage.Message1") + " (" + wi + "x" + ht + ") " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
								MediaRes.getString("Filter.Baseline.Dialog.SmallImage.Message2a") + "\n" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
								MediaRes.getString("Filter.Baseline.Dialog.SmallImage.Message2b") + " (" + w + "x" + h //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								+ ").", //$NON-NLS-1$
						MediaRes.getString("Filter.Baseline.Dialog.SmallImage.Title"), //$NON-NLS-1$
						JOptionPane.INFORMATION_MESSAGE);
				return false;
			}
		}
		else {
			baseline = null;
			baselineCopy = null;
			imagePath = null;
		}
		
		// add thumbnail image to inspector if it exists
		if (inspector != null) {
			thumbnail = getThumbnailImage();
			imageLabel.setIcon(thumbnail == null? null: new ImageIcon(thumbnail));
			String none = "(" + MediaRes.getString("Filter.Baseline.Message.NoImage") + ")"; //$NON-NLS-1$
			imageLabel.setText(thumbnail == null? none: null);
			imageLabel.setBorder(thumbnail == null? emptyBorder: imageBorder);
			if (thumbnail == null) {
				contentPane.remove(nullBaselinePanel);
				contentPane.add(imageLabel, BorderLayout.NORTH);
			}
			else {
				nullBaselinePanel.add(imageLabel);
				contentPane.add(nullBaselinePanel, BorderLayout.NORTH);			
			}
			saveButton.setEnabled(baseline != null);
			inspector.pack();
			firePropertyChange("baseline", null, null); //$NON-NLS-1$
		}
		
		return true;
	}

	/**
	 * Gets the baseline image being subtracted.
	 *
	 * @return the image
	 */
	public BufferedImage getBaselineImage() {
		return baselineCopy;
	}
	
	private BufferedImage getThumbnailImage() {
		if (baseline == null)
			return null;
		int imageW = baseline.getWidth();
		int imageH = baseline.getHeight();
		int w, h;
		if (imageW > imageH) {
			w = Math.min(9 * contentPane.getWidth() / 10, imageW);
			h = Math.min(w * imageH / imageW, imageH);			
		}
		else {
			h = Math.min(9 * contentPane.getWidth() / 10, imageH);
			w = Math.min(h * imageW / imageH, imageW);						
		}
		if (thumbnail == null || thumbnail.getWidth() != w || thumbnail.getHeight() != h) {
			thumbnail = new BufferedImage(w, h, baseline.getType());
		}
		Graphics2D g2 = thumbnail.createGraphics();
//    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);  
		g2.drawImage(baseline, 0, 0, w, h, 0, 0, baseline.getWidth(), baseline.getHeight(), null);
		g2.dispose();
		return thumbnail;
	}

	/**
	 * Used to resize thumbnanil image when font size is changed.
	 */
	public void resizeThumbnail() {
		if (thumbnail == null) {
			contentPane.remove(imageLabel);
		}
		else {
			contentPane.remove(nullBaselinePanel);
		}		
		inspector.pack();
		setBaselineImage(baselineCopy);
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
		imagePath = null;
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
		saveButton.setText(MediaRes.getString("Dialog.Button.Save")); //$NON-NLS-1$
		loadButton.setEnabled(isEnabled());
		captureButton.setEnabled(isEnabled());
		saveButton.setEnabled(baseline != null);
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
		if (!setBaselineImage(baselineCopy)) {
			setBaselineImage(null);			
		}
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
		else {
			System.arraycopy(pixelsIn, 0, pixelsOut, 0, nPixelsIn);
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
			captureButton = new JButton();
			captureButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					capture();
					refresh();
				}

			});
			saveButton = new JButton();
			saveButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					save();
				}

			});
			imageLabel = new JLabel();
			imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
			Border line = BorderFactory.createLineBorder(Color.black);
			emptyBorder = BorderFactory.createEmptyBorder(4, 4, 0, 4);
			imageBorder = BorderFactory.createCompoundBorder(emptyBorder, line);
			nullBaselinePanel = new JPanel();
			
			// add components to content pane
			contentPane = new JPanel(new BorderLayout());
			setContentPane(contentPane);
			
			JPanel buttonbar = new JPanel(new FlowLayout());
			contentPane.add(buttonbar, BorderLayout.SOUTH);
			buttonbar.add(ableButton);
			buttonbar.add(loadButton);
			buttonbar.add(captureButton);
			buttonbar.add(saveButton);
			buttonbar.add(clearButton);
			buttonbar.add(closeButton);
			
		}

		/**
		 * Initializes this inspector
		 */
		void initialize() {
			SwingUtilities.invokeLater(() -> {
				setBaselineImage(baselineCopy); // may be null
			});			
//			refresh();
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
			if (filter.baseline != null && filter.imagePath == null) {
				int n = JOptionPane.showConfirmDialog(null, 
						MediaRes.getString("Filter.Baseline.Dialog.SaveImage.Text"), //$NON-NLS-1$
						MediaRes.getString("Filter.Baseline.Dialog.SaveImage.Title"), //$NON-NLS-1$ 
						JOptionPane.YES_NO_OPTION); //$NON-NLS-1$
				if (n == JOptionPane.YES_OPTION) {
					filter.save();
				}
			}
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
