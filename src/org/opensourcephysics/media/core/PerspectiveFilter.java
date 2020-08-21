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
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.display.GUIUtils;
import org.opensourcephysics.display.Interactive;
import org.opensourcephysics.display.OSPRuntime;

/**
 * This is a Filter that corrects perspective in the source image.
 *
 * @author Douglas Brown
 * @version 1.0
 */
@SuppressWarnings("serial")
public class PerspectiveFilter extends Filter {

	public static final String PROPERTY_PERSPECTIVEFILTER_CORNERLOCATION = "cornerlocation";
	public static final String PROPERTY_PERSPECTIVEFILTER_FIXED = "fixed";
	
	// static fields
	private static Color defaultColor = Color.RED;
	// instance fields
	private double[][] matrix = new double[3][3]; // perspective transform matrix
	private double[][] temp1 = new double[3][3]; // intermediate matrix
	private double[][] temp2 = new double[3][3]; // intermediate matrix
	private double[] xOut, yOut, xIn, yIn; // pixel positions on input and output images
	private int interpolation = 2; // neighborhood size for color interpolation
	private Quadrilateral quad;
	private QuadEditor inputEditor, outputEditor;
	private Point2D.Double[][] inCornerPoints = new Point2D.Double[10][];
	private Point2D.Double[][] outCornerPoints = new Point2D.Double[10][];
	private TreeSet<Integer> inKeyFrames = new TreeSet<Integer>();
	private TreeSet<Integer> outKeyFrames = new TreeSet<Integer>();
	private boolean fixedIn = false, fixedOut = true;
	private int fixedKey = 0;
	private PropertyChangeListener videoListener;

	// inspector fields
	private Inspector inspector;
	private boolean disposing = false;

	/**
	 * Constructs a PerspectiveFilter object.
	 */
	public PerspectiveFilter() {
		quad = new Quadrilateral();
		refresh();
		hasInspector = true;
		videoListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				int n = (Integer) e.getNewValue();
				refreshCorners(n);
			}
		};
	}

	/**
	 * Gets whether this filter is enabled.
	 *
	 * @return <code>true</code> if this is enabled.
	 */
	@Override
	public boolean isEnabled() {
		boolean disabled = !superIsEnabled();
		boolean editingInput = inspector != null && inspector.isVisible()
				&& inspector.tabbedPane.getSelectedComponent() == inputEditor;
		if (disabled || editingInput)
			return false;
		return true;
	}

	/**
	 * Gets whether the super-class of this filter is enabled.
	 *
	 * @return <code>true</code> if super is enabled.
	 */
	public boolean isSuperEnabled() {
		return super.isEnabled();
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
		super.refresh();
		if (inspector != null) {
			inspector.setTitle(MediaRes.getString("Filter.Perspective.Title")); //$NON-NLS-1$
			inspector.tabbedPane.setTitleAt(0, MediaRes.getString("PerspectiveFilter.Tab.Input")); //$NON-NLS-1$
			inspector.tabbedPane.setTitleAt(1, MediaRes.getString("PerspectiveFilter.Tab.Output")); //$NON-NLS-1$

			inspector.helpButton.setText(MediaRes.getString("PerspectiveFilter.Button.Help")); //$NON-NLS-1$
			inspector.colorButton.setText(MediaRes.getString("PerspectiveFilter.Button.Color")); //$NON-NLS-1$
			ableButton.setText(super.isEnabled() ? MediaRes.getString("Filter.Button.Disable") : //$NON-NLS-1$
					MediaRes.getString("Filter.Button.Enable")); //$NON-NLS-1$
			inputEditor.refreshGUI();
			outputEditor.refreshGUI();
			inputEditor.refreshFields();
			outputEditor.refreshFields();
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		if (vidPanel != null && vidPanel.getVideo() != null) {
			vidPanel.removePropertyChangeListener("selectedpoint", quad); //$NON-NLS-1$
			Video video = vidPanel.getVideo();
			video.removePropertyChangeListener(Video.PROPERTY_VIDEO_NEXTFRAME, videoListener);
			removePropertyChangeListener(PROPERTY_FILTER_VISIBLE, vidPanel);
		}
	}

	/**
	 * Sets the video panel.
	 * 
	 * @param panel the video panel
	 */
	@Override
	public void setVideoPanel(VideoPanel panel) {
		VideoPanel prevPanel = vidPanel;
		super.setVideoPanel(panel);
		if (vidPanel != null) {
			// filter added
			Video video = vidPanel.getVideo();
			video.removePropertyChangeListener(Video.PROPERTY_VIDEO_NEXTFRAME, videoListener);
			video.addPropertyChangeListener(Video.PROPERTY_VIDEO_NEXTFRAME, videoListener);
			vidPanel.propertyChange(new PropertyChangeEvent(this, "perspective", null, this)); //$NON-NLS-1$
		} else if (prevPanel != null) {
			// filter removed
			prevPanel.removeDrawable(quad);
			Video video = prevPanel.getVideo();
			video.removePropertyChangeListener(Video.PROPERTY_VIDEO_NEXTFRAME, videoListener);
			prevPanel.propertyChange(new PropertyChangeEvent(this, "perspective", this, null)); //$NON-NLS-1$
		}
	}

	/**
	 * Sets the fixed position behavior (all frames identical).
	 * 
	 * @param fix true to set the corner positions the same in all frames
	 * @param in  true for input corners, false for output
	 */
	public void setFixed(boolean fix, boolean in) {
		if (isFixed(in) != fix) {
			String filterState = new XMLControlElement(this).toXML();

			if (in)
				fixedIn = fix;
			else
				fixedOut = fix;

			if (isFixed(in)) {
				TreeSet<Integer> keyFrames = in ? inKeyFrames : outKeyFrames;
				keyFrames.clear();
				saveCorners(fixedKey, in); // save input corners
			}
			support.firePropertyChange(PROPERTY_PERSPECTIVEFILTER_FIXED, filterState, null); //$NON-NLS-1$
		}
	}

	/**
	 * Gets the fixed position behavior.
	 * 
	 * @return true if fixed
	 */
	public boolean isFixed(boolean in) {
		return in ? fixedIn : fixedOut;
	}

	/**
	 * Sets the location of a corner.
	 * 
	 * @param frameNumber the video frame number
	 * @param cornerIndex the corner index (0-3)
	 * @param x           the x-position
	 * @param y           the y-position
	 */
	public void setCornerLocation(int frameNumber, int cornerIndex, double x, double y) {
		boolean in = cornerIndex < 4;
		Corner[] corners = in ? quad.inCorners : quad.outCorners;
		corners[cornerIndex].setXY(x, y);
	}

	/**
	 * Gets the color.
	 * 
	 * @return the color
	 */
	public Color getColor() {
		return quad.color;
	}

	/**
	 * Gets the index associated with a corner point.
	 * 
	 * @param corner the corner
	 * @return the index
	 */
	public int getCornerIndex(Corner corner) {
		for (int i = 0; i < 4; i++) {
			if (corner == quad.inCorners[i])
				return i;
			if (corner == quad.outCorners[i])
				return i + 4;
		}
		return -1;
	}

	/**
	 * Gets the corner associated with an index.
	 * 
	 * @param index the index (0-7)
	 * @return the corner
	 */
	public Corner getCorner(int index) {
		Corner[] corners = index < 4 ? quad.inCorners : quad.outCorners;
		return corners[index % 4];
	}

	/**
	 * Deletes the key frame associated with a corner.
	 * 
	 * @param frameNumber the frame number
	 * @param corner      the corner
	 */
	public void deleteKeyFrame(int frameNumber, Corner corner) {
		int index = getCornerIndex(corner);
		if (index == -1)
			return;
		boolean isInput = index < 4 ? true : false;
		TreeSet<Integer> keyFrames = isInput ? inKeyFrames : outKeyFrames;
		int key = getKeyFrame(frameNumber, isInput);
		if (key == 0)
			return; // can't delete frame 0
		keyFrames.remove(key);
		Point2D[][] cornerPoints = index < 4 ? inCornerPoints : outCornerPoints;
		cornerPoints[key] = null;
		refreshCorners(vidPanel.getFrameNumber());
	}

	/**
	 * Sets the inspector tab to input or output.
	 * 
	 * @param enable true to show the input tab, false to show the output tab
	 */
	public void setInputEnabled(boolean enable) {
		if (inspector == null)
			return;
		inspector.tabbedPane.setSelectedComponent(enable ? inputEditor : outputEditor);
	}

	/**
	 * Determines if the inspector tab is the input tab.
	 * 
	 * @return true if the input tab is shown
	 */
	public boolean isInputEnabled() {
		if (inspector == null)
			return false;
		return inspector.tabbedPane.getSelectedComponent() == inputEditor;
	}

	/**
	 * Determines if the inspector is active.
	 * 
	 * @return true if the active
	 */
	public boolean isActive() {
		if (inspector == null)
			return false;
		return inspector.tabbedPane.isEnabled();
	}

	/**
	 * Determines if the inspector has been instantiated.
	 * 
	 * @return true if the inspector exists
	 */
	public boolean hasInspector() {
		return inspector != null;
	}

	// _____________________________ private methods _______________________

	/**
	 * Creates the input and output images and ColorConvertOp.
	 *
	 * @param image a new input image
	 */
	@Override
	protected void initializeSubclass() {
		xIn = new double[nPixelsIn];
		yIn = new double[nPixelsIn];
		// output positions are integer pixels
		xOut = new double[nPixelsIn];
		yOut = new double[nPixelsIn];
		for (int j = 0, p = 0; j < h; j++) {
			for (int i = 0; i < w; i++, p++) {
				yOut[p] = j;
				xOut[p] = i;
			}
		}
		// initialize corner positions
		if (inKeyFrames.isEmpty()) {
			quad.inCorners[0].setLocation(w / 4, h / 4);
			quad.inCorners[1].setLocation(3 * w / 4, h / 4);
			quad.inCorners[2].setLocation(3 * w / 4, 3 * h / 4);
			quad.inCorners[3].setLocation(w / 4, 3 * h / 4);
			quad.outCorners[0].setLocation(w / 4, h / 4);
			quad.outCorners[1].setLocation(3 * w / 4, h / 4);
			quad.outCorners[2].setLocation(3 * w / 4, 3 * h / 4);
			quad.outCorners[3].setLocation(w / 4, 3 * h / 4);
			saveCorners(0, true);
			saveCorners(0, false);
		}
	}

	/**
	 * Sets the output image pixels to a rotated version of the input pixels.
	 */
	@Override
	protected void setOutputPixels() {
		getPixelsIn();
		getPixelsOut();
		// set up transform matrix based on quad input/output corners

		// set temp1 to transform output to square
		getQuadToSquare(temp1, quad.outCorners[0].x, quad.outCorners[0].y, quad.outCorners[1].x,
				quad.outCorners[1].y, quad.outCorners[2].x, quad.outCorners[2].y,
				quad.outCorners[3].x, quad.outCorners[3].y);

		// set temp2 to transform square to input
		getSquareToQuad(temp2, quad.inCorners[0].x, quad.inCorners[0].y, quad.inCorners[1].x,
				quad.inCorners[1].y, quad.inCorners[2].x, quad.inCorners[2].y, quad.inCorners[3].x,
				quad.inCorners[3].y);
		// concatenate temp2 to temp1 to obtain transform matrix output->input
		concatenate(temp1, temp2);

		// transform the output (pixel) positions to input positions
		transform(xOut, yOut, xIn, yIn);

		// find output pixel values by interpolating input pixels
		for (int i = 0; i < nPixelsIn; i++) {
			pixelsOut[i] = getColor(xIn[i], yIn[i], w, h, pixelsIn);
		}
	}

	/**
	 * Transforms arrays of position coordinates using the current transform matrix.
	 * 
	 * @param xSource array of source x-coordinates
	 * @param ySource array of source y-coordinates
	 * @param xTrans  array of transformed x-coordinates
	 * @param yTrans  array of transformed y-coordinates
	 */
	private void transform(double[] xSource, double[] ySource, double[] xTrans, double[] yTrans) {
		int n = xSource.length;
		for (int i = 0; i < n; i++) {
			double w = matrix[2][0] * xSource[i] + matrix[2][1] * ySource[i] + matrix[2][2];
			if (w == 0) {
				xTrans[i] = xSource[i];
				yTrans[i] = ySource[i];
			} else {
				xTrans[i] = (matrix[0][0] * xSource[i] + matrix[0][1] * ySource[i] + matrix[0][2]) / w;
				yTrans[i] = (matrix[1][0] * xSource[i] + matrix[1][1] * ySource[i] + matrix[1][2]) / w;
			}

		}
	}

	private	int[] rgb = new int[4], values = new int[4];

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
			values[0] = pixelValues[row * w + col];
			values[1] = pixelValues[row * w + col + 1];
			values[2] =	pixelValues[(row + 1) * w + col];
			values[3] = pixelValues[(row + 1) * w + col + 1];
			

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
	 * Creates a transform matrix to map a unit square onto a quadrilateral.
	 *
	 * (0, 0) -> (x0, y0) (1, 0) -> (x1, y1) (1, 1) -> (x2, y2) (0, 1) -> (x3, y3)
	 */
	private void getSquareToQuad(double[][] matrix, double x0, double y0, double x1, double y1, double x2, double y2,
			double x3, double y3) {

		double dx3 = x0 - x1 + x2 - x3;
		double dy3 = y0 - y1 + y2 - y3;

		matrix[2][2] = 1.0F;
		if ((dx3 == 0.0F) && (dy3 == 0.0F)) {
			matrix[0][0] = x1 - x0;
			matrix[0][1] = x2 - x1;
			matrix[0][2] = x0;
			matrix[1][0] = y1 - y0;
			matrix[1][1] = y2 - y1;
			matrix[1][2] = y0;
			matrix[2][0] = 0.0F;
			matrix[2][1] = 0.0F;
		} else {
			double dx1 = x1 - x2;
			double dy1 = y1 - y2;
			double dx2 = x3 - x2;
			double dy2 = y3 - y2;
			double invdet = 1.0F / (dx1 * dy2 - dx2 * dy1);

			matrix[2][0] = (dx3 * dy2 - dx2 * dy3) * invdet;
			matrix[2][1] = (dx1 * dy3 - dx3 * dy1) * invdet;
			matrix[0][0] = x1 - x0 + matrix[2][0] * x1;
			matrix[0][1] = x3 - x0 + matrix[2][1] * x3;
			matrix[0][2] = x0;
			matrix[1][0] = y1 - y0 + matrix[2][0] * y1;
			matrix[1][1] = y3 - y0 + matrix[2][1] * y3;
			matrix[1][2] = y0;
		}
	}

	/**
	 * Creates a transform matrix to map a quadrilateral onto a unit square.
	 *
	 * (x0, y0) -> (0, 0) (x1, y1) -> (1, 0) (x2, y2) -> (1, 1) (x3, y3) -> (0, 1)
	 */
	private void getQuadToSquare(double[][] matrix, double x0, double y0, double x1, double y1, double x2, double y2,
			double x3, double y3) {
		// get square to quad and convert to adjoint
		getSquareToQuad(matrix, x0, y0, x1, y1, x2, y2, x3, y3);
		// get sub-determinants
		double m00 = matrix[1][1] * matrix[2][2] - matrix[1][2] * matrix[2][1];
		double m01 = matrix[1][2] * matrix[2][0] - matrix[1][0] * matrix[2][2];
		double m02 = matrix[1][0] * matrix[2][1] - matrix[1][1] * matrix[2][0];
		double m10 = matrix[0][2] * matrix[2][1] - matrix[0][1] * matrix[2][2];
		double m11 = matrix[0][0] * matrix[2][2] - matrix[0][2] * matrix[2][0];
		double m12 = matrix[0][1] * matrix[2][0] - matrix[0][0] * matrix[2][1];
		double m20 = matrix[0][1] * matrix[1][2] - matrix[0][2] * matrix[1][1];
		double m21 = matrix[0][2] * matrix[1][0] - matrix[0][0] * matrix[1][2];
		double m22 = matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0];
		// transpose
		matrix[0][0] = m00;
		matrix[0][1] = m10;
		matrix[0][2] = m20;
		matrix[1][0] = m01;
		matrix[1][1] = m11;
		matrix[1][2] = m21;
		matrix[2][0] = m02;
		matrix[2][1] = m12;
		matrix[2][2] = m22;
	}

	/**
	 * Concatenates two transform matrices and puts the result into the transform
	 * matrix.
	 */
	private void concatenate(double[][] m1, double[][] m2) {
		matrix[0][0] = m1[0][0] * m2[0][0] + m1[1][0] * m2[0][1] + m1[2][0] * m2[0][2];
		matrix[1][0] = m1[0][0] * m2[1][0] + m1[1][0] * m2[1][1] + m1[2][0] * m2[1][2];
		matrix[2][0] = m1[0][0] * m2[2][0] + m1[1][0] * m2[2][1] + m1[2][0] * m2[2][2];
		matrix[0][1] = m1[0][1] * m2[0][0] + m1[1][1] * m2[0][1] + m1[2][1] * m2[0][2];
		matrix[1][1] = m1[0][1] * m2[1][0] + m1[1][1] * m2[1][1] + m1[2][1] * m2[1][2];
		matrix[2][1] = m1[0][1] * m2[2][0] + m1[1][1] * m2[2][1] + m1[2][1] * m2[2][2];
		matrix[0][2] = m1[0][2] * m2[0][0] + m1[1][2] * m2[0][1] + m1[2][2] * m2[0][2];
		matrix[1][2] = m1[0][2] * m2[1][0] + m1[1][2] * m2[1][1] + m1[2][2] * m2[1][2];
		matrix[2][2] = m1[0][2] * m2[2][0] + m1[1][2] * m2[2][1] + m1[2][2] * m2[2][2];
	}

	private double[][] getCornerData(Point2D.Double[] cornerPoints) {
		double[][] data = new double[4][2];
		for (int i = 0; i < 4; i++) {
			data[i][0] = cornerPoints[i].x;
			data[i][1] = cornerPoints[i].y;
		}
		return data;
	}

	private void refreshCorners(int frameNumber) {
//		// gIn can be null if memory limit was exceeded when trying to instantiate with
//		// large images
//		
//		// BH -- but we are not necessarily going to use gIn
//		if (gIn == null && source != null && input != source)
//			return;

		int key = getKeyFrame(frameNumber, true); // input
		for (int i = 0; i < 4; i++) {
			quad.inCorners[i].setLocation(inCornerPoints[key][i]);
		}
		key = getKeyFrame(frameNumber, false); // output
		for (int i = 0; i < 4; i++) {
			quad.outCorners[i].setLocation(outCornerPoints[key][i]);
		}
	}

	private void saveCorners(int frameNumber, boolean in) {
		if (isFixed(in))
			frameNumber = fixedKey;
		ensureCornerCapacity(frameNumber);
		TreeSet<Integer> keyFrames = in ? inKeyFrames : outKeyFrames;
		Point2D.Double[][] cornerPoints = in ? inCornerPoints : outCornerPoints;
		Corner[] corners = in ? quad.inCorners : quad.outCorners;
		keyFrames.add(frameNumber);
		if (cornerPoints[frameNumber] == null) {
			cornerPoints[frameNumber] = new Point2D.Double[4];
			for (int i = 0; i < 4; i++) {
				cornerPoints[frameNumber][i] = new Point2D.Double();
			}
		}
		for (int i = 0; i < 4; i++) {
			cornerPoints[frameNumber][i].setLocation(corners[i]);
		}
	}

	private void loadCornerData(double[][][] cornerData, boolean in) {
		ensureCornerCapacity(cornerData.length);
		TreeSet<Integer> keyFrames = in ? inKeyFrames : outKeyFrames;
		keyFrames.clear();
		Point2D.Double[][] cornerPoints = in ? inCornerPoints : outCornerPoints;
		for (int j = 0; j < cornerData.length; j++) {
			if (cornerData[j] == null)
				continue;
			keyFrames.add(j);
			if (cornerPoints[j] == null) {
				cornerPoints[j] = new Point2D.Double[4];
				for (int i = 0; i < 4; i++) {
					cornerPoints[j][i] = new Point2D.Double();
				}
			}
			for (int i = 0; i < 4; i++) {
				cornerPoints[j][i].setLocation(cornerData[j][i][0], cornerData[j][i][1]);
			}
		}
	}

	private void ensureCornerCapacity(int index) {
		int length = inCornerPoints.length;
		if (length < index + 1) {
			Point2D.Double[][] newArray = new Point2D.Double[index + 10][];
			System.arraycopy(inCornerPoints, 0, newArray, 0, length);
			inCornerPoints = newArray;
		}
		length = outCornerPoints.length;
		if (length < index + 1) {
			Point2D.Double[][] newArray = new Point2D.Double[index + 10][];
			System.arraycopy(outCornerPoints, 0, newArray, 0, length);
			outCornerPoints = newArray;
		}
	}

	private void trimCornerPoints() {
		int length = inCornerPoints.length;
		for (int i = length; i > 0; i--) {
			if (inCornerPoints[i - 1] != null) {
				Point2D.Double[][] newArray = new Point2D.Double[i][];
				System.arraycopy(inCornerPoints, 0, newArray, 0, i);
				inCornerPoints = newArray;
				break;
			}
		}
		length = outCornerPoints.length;
		for (int i = length; i > 0; i--) {
			if (outCornerPoints[i - 1] != null) {
				Point2D.Double[][] newArray = new Point2D.Double[i][];
				System.arraycopy(outCornerPoints, 0, newArray, 0, i);
				outCornerPoints = newArray;
				break;
			}
		}
	}

	private int getKeyFrame(int frameNumber, boolean in) {
		if (isFixed(in))
			return fixedKey;
		TreeSet<Integer> keyFrames = in ? inKeyFrames : outKeyFrames;
		int key = 0;
		for (int i : keyFrames) {
			if (i <= frameNumber)
				key = i;
		}
		return key;
	}

	/**
	 * Inner Inspector class to control filter parameters
	 */
	private class Inspector extends InspectorDlg {

		JButton helpButton, colorButton;
		JTabbedPane tabbedPane;
		JPanel contentPane;

		/**
		 * Constructs the Inspector.
		 */
		public Inspector() {
			super("Filter.Perspective.Title");
		}

		/**
		 * Creates the visible components.
		 */
		@Override
		void createGUI() {
			tabbedPane = new JTabbedPane();
			inputEditor = new QuadEditor(true);
			outputEditor = new QuadEditor(false);
			outputEditor.selectedShapeIndex = 1;
			tabbedPane.addTab("", inputEditor); //$NON-NLS-1$
			tabbedPane.addTab("", outputEditor); //$NON-NLS-1$
			tabbedPane.setSelectedComponent(inputEditor);
			// add change listener after adding tabs to prevent start-up event firing
			tabbedPane.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					if (disposing)
						return;
					refresh();
					support.firePropertyChange(PROPERTY_FILTER_IMAGE, null, null); // $NON-NLS-1$
					support.firePropertyChange(PROPERTY_FILTER_TAB, null, null); // $NON-NLS-1$
				}
			});
			helpButton = new JButton();
			helpButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String s = MediaRes.getString("PerspectiveFilter.Help.Message1") //$NON-NLS-1$
							+ "\n" + MediaRes.getString("PerspectiveFilter.Help.Message2") //$NON-NLS-1$ //$NON-NLS-2$
							+ "\n" + MediaRes.getString("PerspectiveFilter.Help.Message3") //$NON-NLS-1$ //$NON-NLS-2$
							+ "\n" + MediaRes.getString("PerspectiveFilter.Help.Message4") //$NON-NLS-1$ //$NON-NLS-2$
							+ "\n\n" + MediaRes.getString("PerspectiveFilter.Help.Message5") //$NON-NLS-1$ //$NON-NLS-2$
							+ "\n  " + MediaRes.getString("PerspectiveFilter.Help.Message6") //$NON-NLS-1$ //$NON-NLS-2$
							+ "\n  " + MediaRes.getString("PerspectiveFilter.Help.Message7") //$NON-NLS-1$ //$NON-NLS-2$
							+ "\n  " + MediaRes.getString("PerspectiveFilter.Help.Message8") //$NON-NLS-1$ //$NON-NLS-2$
							+ "\n      " + MediaRes.getString("PerspectiveFilter.Help.Message9"); //$NON-NLS-1$ //$NON-NLS-2$
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(vidPanel), s,
							MediaRes.getString("PerspectiveFilter.Help.Title"), //$NON-NLS-1$
							JOptionPane.INFORMATION_MESSAGE);
				}
			});
			colorButton = new JButton();
			colorButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					// show color chooser dialog with color of this filter's quad
					Color newColor = JColorChooser.showDialog(null,
							MediaRes.getString("PerspectiveFilter.Dialog.Color.Title"), //$NON-NLS-1$
							quad.color);
					if (newColor != null) {
						quad.color = newColor;
						support.firePropertyChange(PROPERTY_FILTER_COLOR, null, newColor); 
					}
				}
			});
			// ableButton already has action listener to enable/disable this filter
			ableButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					// refresh button state
					boolean enable = !superIsEnabled();
					colorButton.setEnabled(enable);
					tabbedPane.setEnabled(enable);
					inputEditor.setEnabled(enable);
					outputEditor.setEnabled(enable);
				}
			});

			// add components to content pane
			contentPane = new JPanel(new BorderLayout());
			setContentPane(contentPane);
			JPanel buttonbar = new JPanel(new FlowLayout());
			buttonbar.add(helpButton);
			buttonbar.add(colorButton);
			buttonbar.add(ableButton);
			buttonbar.add(closeButton);
			contentPane.add(buttonbar, BorderLayout.SOUTH);
			contentPane.add(tabbedPane, BorderLayout.CENTER);
		}

		/**
		 * Initializes this inspector
		 */
		void initialize() {
			refresh();
			pack();
		}

		@Override
		public void dispose() {
			disposing = true;
			contentPane.remove(tabbedPane);
			tabbedPane.removeAll();
			super.dispose();
			disposing = false;
		}

		@Override
		public void setVisible(boolean vis) {
			if (vis == isVisible())
				return;
			super.setVisible(vis);

			if (vidPanel != null) {
				if (vis) {
					vidPanel.addDrawable(quad);
					support.firePropertyChange(PROPERTY_FILTER_VISIBLE, null, null);
					PerspectiveFilter.this.removePropertyChangeListener(PROPERTY_FILTER_VISIBLE, vidPanel);
					PerspectiveFilter.this.addPropertyChangeListener(PROPERTY_FILTER_VISIBLE, vidPanel);
					vidPanel.removePropertyChangeListener("selectedpoint", quad); //$NON-NLS-1$
					vidPanel.addPropertyChangeListener("selectedpoint", quad); //$NON-NLS-1$
				} else {
					support.firePropertyChange(PROPERTY_FILTER_VISIBLE, null, null);
					PerspectiveFilter.this.removePropertyChangeListener(PROPERTY_FILTER_VISIBLE, vidPanel);
					vidPanel.removePropertyChangeListener("selectedpoint", quad); //$NON-NLS-1$
					vidPanel.removeDrawable(quad);
					// fire MOUSE_RELEASED event to ensure full deselection in Tracker
					java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new MouseEvent(vidPanel,
							MouseEvent.MOUSE_RELEASED, 0, MouseEvent.BUTTON1_MASK, -100, -100, 1, false));
				}
			}
			boolean enable = superIsEnabled();
			colorButton.setEnabled(enable);
			tabbedPane.setEnabled(enable);
			inputEditor.setEnabled(enable);
			outputEditor.setEnabled(enable);
			support.firePropertyChange("image", null, null); //$NON-NLS-1$
		}
	}

	public class Corner extends TPoint {

		/**
		 * Overrides TPoint setXY method.
		 *
		 * @param x the x coordinate
		 * @param y the y coordinate
		 */
		@Override
		public void setXY(double x, double y) {
			super.setXY(x, y);
			boolean in = !PerspectiveFilter.this.isEnabled();
			Corner[] corners = in ? quad.inCorners : quad.outCorners;
			QuadEditor editor = in ? inputEditor : outputEditor;
			if (editor.shapes[editor.selectedShapeIndex].equals("Rectangle")) { //$NON-NLS-1$
				if (this == corners[0]) {
					corners[3].x = x;
					corners[1].y = y;
				} else if (this == corners[1]) {
					corners[2].x = x;
					corners[0].y = y;
				} else if (this == corners[2]) {
					corners[1].x = x;
					corners[3].y = y;
				} else if (this == corners[3]) {
					corners[0].x = x;
					corners[2].y = y;
				}
			}
			saveCorners(vidPanel == null ? 0 : vidPanel.getFrameNumber(), in);
			editor.refreshFields();
			if (editor == outputEditor) {
				PerspectiveFilter.this.support.firePropertyChange("image", null, null); //$NON-NLS-1$
			}

			// fire cornerlocation event
			PerspectiveFilter.this.support.firePropertyChange(PROPERTY_PERSPECTIVEFILTER_CORNERLOCATION, null, this); //$NON-NLS-1$

			if (vidPanel != null)
				vidPanel.repaint();
		}

	}

	/**
	 * Inner Quadrilateral class draws and provides interactive control of input and
	 * output quadrilateral shapes.
	 */
	private class Quadrilateral implements Trackable, Interactive, PropertyChangeListener {

		private Corner[] inCorners = new Corner[4]; // input image corner positions (image units)
		private Corner[] outCorners = new Corner[4]; // output image corner positions
		private Point2D.Double[] screenPts = new Point2D.Double[4];
		private GeneralPath path = new GeneralPath();
		private Stroke stroke = new BasicStroke(2);
		private Stroke cornerStroke = new BasicStroke();
		private Shape selectionShape = new Rectangle(-4, -4, 8, 8);
		private Shape cornerShape = new Ellipse2D.Double(-5, -5, 10, 10);
		private Shape[] hitShapes = new Shape[4];
		private Shape[] drawShapes = new Shape[5];
		private Corner selectedCorner;
		private AffineTransform transform = new AffineTransform();
		private TextLayout[] textLayouts = new TextLayout[4];
		private Font font = new JTextField().getFont();
		private Point p = new Point();
		private Color color = defaultColor;

		public Quadrilateral() {
			for (int i = 0; i < inCorners.length; i++) {
				inCorners[i] = new Corner();
				outCorners[i] = new Corner();
				textLayouts[i] = new TextLayout(String.valueOf(i), font, OSPRuntime.frc);
				screenPts[i] = new Point2D.Double();
			}
		}

		/**
		 * Responds to property change events.
		 *
		 * @param e the property change event
		 */
		@Override
		public void propertyChange(PropertyChangeEvent e) {
			Corner prev = selectedCorner;
			selectedCorner = null;
			for (int i = 0; i < 4; i++) {
				if (e.getNewValue() == inCorners[i])
					selectedCorner = inCorners[i];
				else if (e.getNewValue() == outCorners[i])
					selectedCorner = outCorners[i];
			}
			if (selectedCorner != prev && vidPanel != null)
				vidPanel.repaint();
		}

		@Override
		public void draw(DrawingPanel panel, Graphics g) {
			if (!superIsEnabled())
				return;
			VideoPanel vidPanel = (VideoPanel) panel;
			Corner[] corners = PerspectiveFilter.this.isEnabled() ? outCorners : inCorners;
			for (int i = 0; i < 4; i++) {
				Point pt = corners[i].getScreenPosition(vidPanel);
				screenPts[i].setLocation(pt.x, pt.y);
				transform.setToTranslation(screenPts[i].x, screenPts[i].y);
				Shape s = corners[i] == selectedCorner ? selectionShape : cornerShape;
				Stroke sk = corners[i] == selectedCorner ? stroke : cornerStroke;
				hitShapes[i] = transform.createTransformedShape(s);
				drawShapes[i] = sk.createStrokedShape(hitShapes[i]);
			}
			path.reset();
			path.moveTo((float) screenPts[0].x, (float) screenPts[0].y);
			path.lineTo((float) screenPts[1].x, (float) screenPts[1].y);
			path.lineTo((float) screenPts[2].x, (float) screenPts[2].y);
			path.lineTo((float) screenPts[3].x, (float) screenPts[3].y);
			path.closePath();
			drawShapes[4] = stroke.createStrokedShape(path);
			Graphics2D g2 = (Graphics2D) g;
			Color gcolor = g2.getColor();
			g2.setColor(color);
			Font gfont = g.getFont();
			g2.setFont(font);
			if (OSPRuntime.setRenderingHints)
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			for (int i = 0; i < drawShapes.length; i++) {
				g2.fill(drawShapes[i]);
			}
			for (int i = 0; i < textLayouts.length; i++) {
				p.setLocation(screenPts[i].x - 4 - font.getSize(), screenPts[i].y - 6);
				textLayouts[i].draw(g2, p.x, p.y);
			}
			g2.setFont(gfont);
			g2.setColor(gcolor);
		}

		/**
		 * Finds the interactive drawable object located at the specified pixel
		 * position.
		 *
		 * @param panel the drawing panel
		 * @param xpix  the x pixel position on the panel
		 * @param ypix  the y pixel position on the panel
		 * @return the first corner that is hit
		 */
		@Override
		public Interactive findInteractive(DrawingPanel panel, int xpix, int ypix) {
			if (!superIsEnabled())
				return null;
			if (!(panel instanceof VideoPanel) || !isEnabled())
				return null;
			for (int i = 0; i < hitShapes.length; i++) {
				if (hitShapes[i] != null && hitShapes[i].contains(xpix, ypix)) {
					if (!PerspectiveFilter.this.isEnabled())
						return inCorners[i];
					return outCorners[i];
				}
			}
			return null;
		}

		/**
		 * Return value is ignored since not measured
		 *
		 * @return 0
		 */
		@Override
		public double getX() {
			return 0;
		}

		/**
		 * Return value is ignored since not measured
		 *
		 * @return 0
		 */
		@Override
		public double getY() {
			return 0;
		}

		/**
		 * Empty setX method.
		 *
		 * @param x the x position
		 */
		@Override
		public void setX(double x) {
		}

		/**
		 * Empty setY method.
		 *
		 * @param y the y position
		 */
		@Override
		public void setY(double y) {
		}

		/**
		 * Empty setXY method.
		 *
		 * @param x the x position
		 * @param y the y position
		 */
		@Override
		public void setXY(double x, double y) {
		}

		/**
		 * Sets whether this responds to mouse hits.
		 *
		 * @param enabled <code>true</code> if this responds to mouse hits.
		 */
		@Override
		public void setEnabled(boolean enabled) {
		}

		/**
		 * Gets whether this responds to mouse hits.
		 *
		 * @return <code>true</code> if this responds to mouse hits.
		 */
		@Override
		public boolean isEnabled() {
			return true;
		}

		/**
		 * Reports whether information is available to set min/max values.
		 *
		 * @return <code>false</code> since Quadrilateral knows only image coordinates
		 */
		@Override
		public boolean isMeasured() {
			return false;
		}

		/**
		 * Gets the minimum x needed to draw this object.
		 *
		 * @return 0
		 */
		@Override
		public double getXMin() {
			return getX();
		}

		/**
		 * Gets the maximum x needed to draw this object.
		 *
		 * @return 0
		 */
		@Override
		public double getXMax() {
			return getX();
		}

		/**
		 * Gets the minimum y needed to draw this object.
		 *
		 * @return 0
		 */
		@Override
		public double getYMin() {
			return getY();
		}

		/**
		 * Gets the maximum y needed to draw this object.
		 *
		 * @return 0
		 */
		@Override
		public double getYMax() {
			return getY();
		}

	}

	private class QuadEditor extends JPanel {

		DecimalField[][] fields = new DecimalField[4][2];
		boolean isInput;
		String[] shapes = { "Any", "Rectangle" }; //$NON-NLS-1$ //$NON-NLS-2$
		int selectedShapeIndex;
		JComboBox<String> shapeDropdown = new JComboBox<>();
		JLabel shapeLabel = new JLabel();
		JCheckBox fixedCheckbox;
		boolean refreshing;
		Box[] boxes = new Box[4];
		TitledBorder cornersBorder = BorderFactory.createTitledBorder(""); //$NON-NLS-1$

		QuadEditor(boolean input) {
			super(new BorderLayout());
			isInput = input;
			ActionListener cornerSetter = new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					TPoint[] corners = isInput ? quad.inCorners : quad.outCorners;
					for (int i = 0; i < fields.length; i++) {
						if (e.getSource() == fields[i][0] || e.getSource() == fields[i][1]) {
							corners[i].setXY(fields[i][0].getValue(), fields[i][1].getValue());
						}
					}
					refreshFields();
					PerspectiveFilter.this.support.firePropertyChange("image", null, null); //$NON-NLS-1$
				}
			};
			JPanel fieldPanel = new JPanel(new GridLayout(2, 2));
			fieldPanel.setBorder(cornersBorder);
			for (int i = 0; i < fields.length; i++) {
				fields[i][0] = new DecimalField(4, 1);
				fields[i][0].addActionListener(cornerSetter);
				fields[i][1] = new DecimalField(4, 1);
				fields[i][1].addActionListener(cornerSetter);
				boxes[i] = Box.createHorizontalBox();
				JLabel label = new JLabel(i + ":  x"); //$NON-NLS-1$
				label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));
				boxes[i].add(label);
				boxes[i].add(fields[i][0]);
				label = new JLabel("y"); //$NON-NLS-1$
				label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));
				boxes[i].add(label);
				boxes[i].add(fields[i][1]);
				boxes[i].setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
			}
			fieldPanel.add(boxes[0]);
			fieldPanel.add(boxes[1]);
			fieldPanel.add(boxes[3]);
			fieldPanel.add(boxes[2]);

			shapeLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
			shapeDropdown.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 8));
			shapeDropdown.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (refreshing)
						return;
					selectedShapeIndex = shapeDropdown.getSelectedIndex();
					if (shapes[selectedShapeIndex].equals("Rectangle")) { //$NON-NLS-1$
						TPoint[] corners = isInput ? quad.inCorners : quad.outCorners;
						for (int i = 0; i < 4; i++) {
							corners[i].setXY(corners[i].x, corners[i].y);
						}
						if (vidPanel != null)
							vidPanel.repaint();
					}
				}
			});

			fixedCheckbox = new JCheckBox();
//  		fixedCheckbox.setSelected(isFixed(isInput));
			fixedCheckbox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (refreshing || isFixed(isInput) == fixedCheckbox.isSelected())
						return;
					setFixed(fixedCheckbox.isSelected(), isInput);
				}
			});
			// assemble editor
			Box box = Box.createHorizontalBox();
			box.add(shapeLabel);
			box.add(shapeDropdown);
			box.add(fixedCheckbox);
			add(box, BorderLayout.NORTH);
			add(fieldPanel, BorderLayout.CENTER);
			refreshGUI();
			refreshFields();
		}

		void refreshGUI() {
			refreshing = true;
			shapeLabel.setText(MediaRes.getString("PerspectiveFilter.Label.Shape")); //$NON-NLS-1$
			cornersBorder.setTitle(MediaRes.getString("PerspectiveFilter.Corners.Title")); //$NON-NLS-1$
			fixedCheckbox.setText(MediaRes.getString("PerspectiveFilter.Checkbox.Fixed")); //$NON-NLS-1$
			shapeDropdown.removeAllItems();
			if (this == inputEditor) {
				shapeDropdown.addItem(MediaRes.getString("PerspectiveFilter.Shape.Any")); //$NON-NLS-1$
			} else {
				for (int i = 0; i < shapes.length; i++) {
					shapeDropdown.addItem(MediaRes.getString("PerspectiveFilter.Shape." + shapes[i])); //$NON-NLS-1$
				}
			}
			shapeDropdown.setSelectedIndex(selectedShapeIndex);
			fixedCheckbox.setSelected(isFixed(isInput));
			refreshing = false;
		}

		void refreshFields() {
			Corner[] corners = this == outputEditor ? quad.outCorners : quad.inCorners;
			for (int i = 0; i < 4; i++) {
				fields[i][0].setValue(corners[i].x);
				fields[i][1].setValue(corners[i].y);
			}
		}

		@Override
		public void setEnabled(boolean b) {
			shapeDropdown.setEnabled(b);
			shapeLabel.setEnabled(b);
			fixedCheckbox.setEnabled(b);
			for (int i = 0; i < boxes.length; i++) {
				for (Component c : boxes[i].getComponents()) {
					c.setEnabled(b);
				}
			}
			cornersBorder.setTitleColor(b ? shapeLabel.getForeground() : GUIUtils.getDisabledTextColor());
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
			PerspectiveFilter filter = (PerspectiveFilter) obj;

			filter.trimCornerPoints();
			double[][][] data = new double[filter.inCornerPoints.length][][];
			for (int i : filter.inKeyFrames) {
				filter.refreshCorners(i);
				data[i] = filter.getCornerData(filter.inCornerPoints[i]);
			}
			control.setValue("in_corners", data); //$NON-NLS-1$
			data = new double[filter.outCornerPoints.length][][];
			for (int i : filter.outKeyFrames) {
				filter.refreshCorners(i);
				data[i] = filter.getCornerData(filter.outCornerPoints[i]);
			}
			control.setValue("out_corners", data); //$NON-NLS-1$

			if (filter.vidPanel != null) {
				VideoClip clip = filter.vidPanel.getPlayer().getVideoClip();
				control.setValue("startframe", clip.getStartFrameNumber()); //$NON-NLS-1$
				filter.refreshCorners(filter.vidPanel.getFrameNumber());
			}

			if (!filter.quad.color.equals(defaultColor)) {
				control.setValue("color", filter.quad.color); //$NON-NLS-1$
			}
			filter.addLocation(control);
			control.setValue("disabled", !filter.isSuperEnabled()); //$NON-NLS-1$
			control.setValue("fixed_in", filter.fixedIn); //$NON-NLS-1$
			control.setValue("fixed_out", filter.fixedOut); //$NON-NLS-1$
		}

		/**
		 * Creates a new filter.
		 *
		 * @param control the control
		 * @return the new filter
		 */
		@Override
		public Object createObject(XMLControl control) {
			return new PerspectiveFilter();
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
			final PerspectiveFilter filter = (PerspectiveFilter) obj;

			if (control.getPropertyNamesRaw().contains("fixed_out")) { //$NON-NLS-1$
				filter.fixedIn = control.getBoolean("fixed_in"); //$NON-NLS-1$
				filter.fixedOut = control.getBoolean("fixed_out"); //$NON-NLS-1$
			}

			double[][][] data = (double[][][]) control.getObject("in_corners"); //$NON-NLS-1$
			if (data != null) {
				filter.loadCornerData(data, true);
			}
			data = (double[][][]) control.getObject("out_corners"); //$NON-NLS-1$
			if (data != null) {
				filter.loadCornerData(data, false);
			}
			for (int i : filter.inKeyFrames) {
				filter.refreshCorners(i);
			}
			for (int i : filter.outKeyFrames) {
				filter.refreshCorners(i);
			}

			int frame = control.getInt("startframe"); //$NON-NLS-1$
			if (frame != Integer.MIN_VALUE) {
				filter.refreshCorners(frame);
			}
			if (filter.vidPanel != null) {
				filter.refreshCorners(filter.vidPanel.getFrameNumber());
			}

			if (control.getPropertyNamesRaw().contains("color")) { //$NON-NLS-1$
				filter.quad.color = (Color) control.getObject("color"); //$NON-NLS-1$
			}

			filter.inspectorX = control.getInt("inspector_x"); //$NON-NLS-1$
			filter.inspectorY = control.getInt("inspector_y"); //$NON-NLS-1$

			boolean disable = control.getBoolean("disabled"); //$NON-NLS-1$
			if (disable && filter.isSuperEnabled() || (!disable && !filter.isSuperEnabled())) {
				filter.ableButton.doClick(0);
			}
			filter.refresh();
			return obj;
		}

	}

	public boolean superIsEnabled() {
		return super.isEnabled();
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