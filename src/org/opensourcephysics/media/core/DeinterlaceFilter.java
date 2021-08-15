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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;

/**
 * This is a Filter that returns only one field of an interlaced video image.
 *
 * @author Douglas Brown
 * @version 1.0
 */
@SuppressWarnings("serial")
public class DeinterlaceFilter extends Filter {
	// instance fields

	private boolean isOdd;
	private Inspector inspector;
	private JRadioButton odd;
	private JRadioButton even;

	/**
	 * Constructs a default DeinterlaceFilter object.
	 */
	public DeinterlaceFilter() {
		hasInspector = true;
	}

	/**
	 * Sets the field to odd or even.
	 *
	 * @param odd true to extract the odd field
	 */
	public void setOdd(boolean odd) {
		boolean prev = isOdd;
		isOdd = odd;
		firePropertyChange("odd", prev, odd); //$NON-NLS-1$
	}

	/**
	 * Gets whether the extracted field is odd.
	 *
	 * @return true if the odd field is extracted
	 */
	public boolean isOdd() {
		return isOdd;
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
		odd.setText(MediaRes.getString("Filter.Deinterlace.Button.Odd")); //$NON-NLS-1$
		even.setText(MediaRes.getString("Filter.Deinterlace.Button.Even")); //$NON-NLS-1$
		boolean enabled = isEnabled();
		odd.setEnabled(enabled);
		even.setEnabled(enabled);			
		inspector.setTitle(MediaRes.getString("Filter.Deinterlace.Title")); //$NON-NLS-1$
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

	/**
	 * Sets the output image pixels to a doubled version of the input field pixels.
	 */
	@Override
	protected void setOutputPixels() {

		// ...in...odd/out...even/out
		// .........n==w....n==0
		// ...4......4.......4
		// ...3......3.......2
		// ...2......3.......2
		// ...1......1.......0
		// ...0......1.......0
		getPixelsIn();
		getPixelsOut();
		int off = (isOdd ? w : 0);
		// copy every other row in pixelsIn to two rows in pixelsOut
		int p = 0;
		for (int i = 0, n = h - 1; i < n; i += 2, p += w) {
			for (int j = 0; j < w; j++, p++) {
				pixelsOut[p] = pixelsOut[p + w] = pixelsIn[p + off];
			}
		}
		if ((h % 2) != 0) {
			// one odd row left; just copy the last row
			for (int j = 0; j < w; j++, p++) {
				pixelsOut[p] = pixelsIn[p];
			}
		}
	}

	/**
	 * Inner Inspector class to control filter parameters
	 */
	private class Inspector extends InspectorDlg {
		// instance fields
		ButtonGroup group;

		/**
		 * Constructs the Inspector.
		 */
		public Inspector() {
			super("Filter.Deinterlace.Title");
		}

		/**
		 * Creates the visible components.
		 */
		@Override
		void createGUI() {
			// create radio buttons
			odd = new JRadioButton();
			even = new JRadioButton();
			// create radio button group
			group = new ButtonGroup();
			group.add(odd);
			group.add(even);
			ActionListener select = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setOdd(group.isSelected(odd.getModel()));
				}

			};
			even.addActionListener(select);
			odd.addActionListener(select);
			JPanel panel = new JPanel(new FlowLayout());
			panel.add(odd);
			panel.add(even);
			// assemble buttons
			JPanel buttonbar = new JPanel(new FlowLayout());
			buttonbar.add(ableButton);
			buttonbar.add(closeButton);
			JPanel contentPane = new JPanel(new BorderLayout());
			setContentPane(contentPane);
			contentPane.add(panel, BorderLayout.CENTER);
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
			if (isOdd) {
				group.setSelected(odd.getModel(), true);
			} else {
				group.setSelected(even.getModel(), true);
			}
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
			DeinterlaceFilter filter = (DeinterlaceFilter) obj;
			if (filter.isOdd()) {
				control.setValue("field", "odd"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				control.setValue("field", "even"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if ((filter.getFrame() != null) && (filter.inspector != null) && filter.inspector.isVisible()) {
				int x = filter.inspector.getLocation().x - filter.frame.getLocation().x;
				int y = filter.inspector.getLocation().y - filter.frame.getLocation().y;
				control.setValue("inspector_x", x); //$NON-NLS-1$
				control.setValue("inspector_y", y); //$NON-NLS-1$
			}
		}

		/**
		 * Creates a new filter.
		 *
		 * @param control the control
		 * @return the new filter
		 */
		@Override
		public Object createObject(XMLControl control) {
			return new DeinterlaceFilter();
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
			final DeinterlaceFilter filter = (DeinterlaceFilter) obj;
			if (control.getPropertyNamesRaw().contains("field")) { //$NON-NLS-1$
				if (control.getString("field").equals("odd")) { //$NON-NLS-1$ //$NON-NLS-2$
					filter.setOdd(true);
				} else {
					filter.setOdd(false);
				}
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
