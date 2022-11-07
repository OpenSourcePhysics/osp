/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display.axes;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.display.GUIUtils;
import org.opensourcephysics.display.Interactive;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.display.PlottingPanel;
import org.opensourcephysics.display.Selectable;
import org.opensourcephysics.display.dialogs.DialogsRes;
import org.opensourcephysics.media.core.ScientificField;
import org.opensourcephysics.tools.FontSizer;
import org.opensourcephysics.tools.ResourceLoader;

/**
 * A cartesian axis class that provides interactive scale adjustment with a
 * mouse.
 */
public class CartesianInteractive extends CartesianType1 implements Selectable {
	// static constant plot regions
	public static final int INSIDE = 0, HORZ_MIN = 1, HORZ_MAX = 2, VERT_MIN = 3, VERT_MAX = 4, HORZ_AXIS = 5,
			HORZ_AXIS_MIN = 6, HORZ_AXIS_MAX = 7, VERT_AXIS = 8, VERT_AXIS_MIN = 9, VERT_AXIS_MAX = 10, HORZ_VAR = 11,
			VERT_VAR = 12;
	// instance fields
	Rectangle hitRect = new Rectangle();
	boolean drawHitRect;

	AxisMouseListener axisListener;
	int mouseRegion;
	Point mouseLoc;
	double mouseX, mouseY;
	PlottingPanel plot;
	boolean enabled = true;
	boolean altDown;
	Cursor horzCenter, horzRight, horzLeft, vertCenter, vertUp, vertDown, move;

	ScaleSetter scaleSetter;
	JPanel scaleSetterPanel;

	java.util.List<ActionListener> axisListeners = new java.util.ArrayList<ActionListener>(); // Paco

	/**
	 * Constructs a set of interactive axes for a plotting panel.
	 *
	 * @param panel the PlottingPanel
	 */
	public CartesianInteractive(PlottingPanel panel) {
		super(panel);
		plot = panel;
		axisListener = new AxisMouseListener();
		panel.addMouseListener(axisListener);
		panel.addMouseMotionListener(axisListener);
		panel.addKeyListener(new java.awt.event.KeyAdapter() {
			@Override
			public void keyPressed(java.awt.event.KeyEvent e) {
				if (!enabled)
					return;
				if ((mouseRegion == INSIDE) && !drawingPanel.isFixedScale()
						&& (e.getKeyCode() == java.awt.event.KeyEvent.VK_ALT)) {
					altDown = true;
					plot.setMouseCursor(getPreferredCursor());
				}
			}

			@Override
			public void keyReleased(java.awt.event.KeyEvent e) {
				if (!enabled)
					return;
				if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ALT) {
					altDown = false;
					plot.setMouseCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
				}
			}

		});
//		scaleSetter = new ScaleSetter();
//		// create transparent scaleSetterPanel with no LayoutManager
//		scaleSetterPanel = new JPanel(null);
//		scaleSetterPanel.setOpaque(false);
//		scaleSetterPanel.add(scaleSetter);

	}

	/**
	 * Gets the current plot region containing the mouse.
	 *
	 * @return one of the static plot regions defined by CartesianInteractive, or -1
	 */
	public int getMouseRegion() {
		return mouseRegion;
	}

	/**
	 * Draws the axes.
	 *
	 * @param panel the drawing panel
	 * @param g     the graphics context
	 */
	@Override
	public void draw(DrawingPanel panel, Graphics g) {
		super.draw(panel, g);
		if (drawHitRect) {
			g.drawRect(hitRect.x, hitRect.y, hitRect.width, hitRect.height);
		}
//		if (!panel.isFixedScale() && scaleSetter != null && scaleSetter.isVisible())
//			scaleSetter.updateValues();
	}

	// overrides CartesianType1 method
	@Override
	public double getX() {
		return Double.isNaN(mouseX) ? plot.pixToX(plot.getMouseIntX()) : mouseX;
	}

	// overrides CartesianType1 method
	@Override
	public double getY() {
		return Double.isNaN(mouseY) ? plot.pixToY(plot.getMouseIntY()) : mouseY;
	}

	// implements Selectable
	@Override
	public void setSelected(boolean selectable) {
	}

	// implements Selectable
	@Override
	public boolean isSelected() {
		return false;
	}

	// implements Selectable
	@Override
	public void toggleSelected() {
	}

	// implements Selectable
	@Override
	public Cursor getPreferredCursor() {
		switch (mouseRegion) {
		case HORZ_AXIS_MIN:
			if (horzLeft == null) {
				// create cursor
				String imageFile = "/org/opensourcephysics/resources/tools/images/horzleft.gif"; //$NON-NLS-1$
				Image im = ResourceLoader.getImage(imageFile);
				horzLeft = GUIUtils.createCustomCursor(im, new Point(16, 16), "Horizontal Left", //$NON-NLS-1$
						Cursor.W_RESIZE_CURSOR);
			}
			return horzLeft;
		case HORZ_AXIS_MAX:
			if (horzRight == null) {
				// create cursor
				String imageFile = "/org/opensourcephysics/resources/tools/images/horzright.gif"; //$NON-NLS-1$
				Image im = ResourceLoader.getImage(imageFile);
				horzRight = GUIUtils.createCustomCursor(im, new Point(16, 16), "Horizontal Right", //$NON-NLS-1$
						Cursor.E_RESIZE_CURSOR);
			}
			return horzRight;
		case HORZ_AXIS:
			if (horzCenter == null) {
				// create cursor
				String imageFile = "/org/opensourcephysics/resources/tools/images/horzcenter.gif"; //$NON-NLS-1$
				Image im = ResourceLoader.getImage(imageFile);
				horzCenter = GUIUtils.createCustomCursor(im, new Point(16, 16), "Horizontal Center", //$NON-NLS-1$
						Cursor.MOVE_CURSOR);
			}
			return horzCenter;
		case VERT_AXIS_MIN:
			if (vertDown == null) {
				// create cursor
				String imageFile = "/org/opensourcephysics/resources/tools/images/vertdown.gif"; //$NON-NLS-1$
				Image im = ResourceLoader.getImage(imageFile);
				vertDown = GUIUtils.createCustomCursor(im, new Point(16, 16), "Vertical Down", Cursor.S_RESIZE_CURSOR); //$NON-NLS-1$
			}
			return vertDown;
		case VERT_AXIS_MAX:
			if (vertUp == null) {
				// create cursor
				String imageFile = "/org/opensourcephysics/resources/tools/images/vertup.gif"; //$NON-NLS-1$
				Image im = ResourceLoader.getImage(imageFile);
				vertUp = GUIUtils.createCustomCursor(im, new Point(16, 16), "Vertical Up", Cursor.N_RESIZE_CURSOR); //$NON-NLS-1$
			}
			return vertUp;
		case VERT_AXIS:
			if (vertCenter == null) {
				// create cursor
				String imageFile = "/org/opensourcephysics/resources/tools/images/vertcenter.gif"; //$NON-NLS-1$
				Image im = ResourceLoader.getImage(imageFile);
				vertCenter = GUIUtils.createCustomCursor(im, new Point(16, 16), "Vertical Center", Cursor.MOVE_CURSOR); //$NON-NLS-1$
			}
			return vertCenter;
		case INSIDE:
			if (move == null) {
				// create cursor
				String imageFile = "/org/opensourcephysics/resources/tools/images/movecursor.gif"; //$NON-NLS-1$
				Image im = ResourceLoader.getImage(imageFile);
				move = GUIUtils.createCustomCursor(im, new Point(16, 16), "Move All Ways", Cursor.MOVE_CURSOR); //$NON-NLS-1$
			}
			return move;
		case HORZ_VAR:
		case VERT_VAR:
			return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
		}
		return Cursor.getDefaultCursor();
	}

	// implements Interactive
	@Override
	public boolean isEnabled() {
		return enabled;
	}

	// implements Interactive
	@Override
	public void setEnabled(boolean enable) {
		enabled = enable;
	}

	public void addAxisListener(ActionListener listener) {
		axisListeners.add(listener);
	} // Paco

	// implements Interactive
	@Override
	public Interactive findInteractive(DrawingPanel panel, int xpix, int ypix) {
		if (drawingPanel.isFixedScale()) {
			return null;
		}
		if (mouseRegion >= HORZ_MIN) {
			return this;
		}
		if (mouseRegion == -1) {
			return this;
		}
		if ((mouseRegion == INSIDE) && altDown) {
			return this;
		}
		return null;
	}

	// implements Interactive
	@Override
	public void setXY(double x, double y) {
	}

	// implements Measurable
	@Override
	public boolean isMeasured() {
		return true;
	}

	// implements Measurable
	@Override
	public double getXMin() {
		return drawingPanel.getXMin();
	}

	// implements Measurable
	@Override
	public double getXMax() {
		return drawingPanel.getXMax();
	}

	// implements Measurable
	@Override
	public double getYMin() {
		return drawingPanel.getYMin();
	}

	// implements Measurable
	@Override
	public double getYMax() {
		return drawingPanel.getYMax();
	}

	/**
	 * Hides the scale setter.
	 */
	public void hideScaleSetter() {
		if (scaleSetter != null) {
//			scaleSetter.autoscaleCheckbox.requestFocusInWindow();
			scaleSetter.setVisible(null);
			plot.repaint();
		}
	}

	/**
	 * Resizes fonts by the specified factor.
	 *
	 * @param factor the factor
	 * @param panel  the drawing panel on which these axes are drawn
	 */
	@Override
	public void resizeFonts(double factor, DrawingPanel panel) {
		super.resizeFonts(factor, panel);
		if (scaleSetter != null) {
			scaleSetter.updateFont();
		}
	}

	/**
	 * Reports whether this provides a popup menu for setting the horizontal axis
	 * variable.
	 *
	 * @return true if this has a popup menu with horizontal axis variables
	 */
	protected boolean hasHorzVariablesPopup() {
		return false;
	}

	/**
	 * Gets a popup menu with horizontal axis variables. This default method returns
	 * null; subclasses should override to return a popup with associated action for
	 * setting horizontal axis variable.
	 *
	 * @return the popup menu
	 */
	protected JPopupMenu getHorzVariablesPopup() {
		return null;
	}

	/**
	 * Reports whether this provides a popup menu for setting the vertical axis
	 * variable.
	 *
	 * @return true if this has a popup menu with vertical axis variables
	 */
	protected boolean hasVertVariablesPopup() {
		return false;
	}

	/**
	 * Gets a popup menu with vertical axis variables. This default method returns
	 * null; subclasses should override to return a popup with associated action for
	 * setting vertical axis variable.
	 *
	 * @return the popup menu
	 */
	protected JPopupMenu getVertVariablesPopup() {
		return null;
	}

	/**
	 * Finds the plot region containing the specified point.
	 *
	 * @param p the point
	 * @return one of the static regions defined by CartesianInteractive
	 */
	protected int findRegion(Point p, boolean isPress) {
		int l = drawingPanel.getLeftGutter();
		int r = drawingPanel.getRightGutter();
		int t = drawingPanel.getTopGutter();
		int b = drawingPanel.getBottomGutter();
		Dimension plotDim = drawingPanel.getSize();
		// horizontal axis
		int reg = -1;
		while (true) {
			int axisLen = plotDim.width - r - l;
			hitRect.setSize(axisLen / 4, 12);
			hitRect.setLocation(l + axisLen / 2 - hitRect.width / 2, plotDim.height - b - hitRect.height / 2);
			if (hitRect.contains(p)) {
				reg = HORZ_AXIS;
				break;
			}
			hitRect.setLocation(l + 4, plotDim.height - b - hitRect.height / 2);
			if (hitRect.contains(p)) {
				reg = HORZ_AXIS_MIN;
				break;
			}
			hitRect.setLocation(l + axisLen - hitRect.width - 4, plotDim.height - b - hitRect.height / 2);
			if (hitRect.contains(p)) {
				reg = HORZ_AXIS_MAX;
				break;
			}
			// vertical axis
			axisLen = plotDim.height - t - b;
			hitRect.setSize(12, axisLen / 4);
			hitRect.setLocation(l - hitRect.width / 2, t + axisLen / 2 - hitRect.height / 2);
			if (hitRect.contains(p)) {
				reg = VERT_AXIS;
				break;
			}
			hitRect.setLocation(l - hitRect.width / 2, t + 4);
			if (hitRect.contains(p)) {
				reg = VERT_AXIS_MAX;
				break;
			}
			hitRect.setLocation(l - hitRect.width / 2, t + axisLen - hitRect.height - 4);
			if (hitRect.contains(p)) {
				reg = VERT_AXIS_MIN;
				break;
			}
//			int offset = 8; // approx distance from axis to hitRect for scale setter
			int offset = 0; // approx distance from axis to hitRect for scale setter
			// horizontal variable
			Graphics g = drawingPanel.getGraphics();
			int xw = xLine.getWidth(g) + offset;
			int xh = xLine.getHeight(g);
			int yw = yLine.getHeight(g);
			int yh = yLine.getWidth(g) + offset;
			g.dispose();
			hitRect.setSize(xw, xh);
			int x = (int) (xLine.getX() - xw / 2);
			int y = (int) (xLine.getY() - xh / 2 - xLine.getFontSize() / 3);
			hitRect.setLocation(x, y);
			if (hitRect.contains(p) && hasHorzVariablesPopup()) {
				reg = HORZ_VAR;
				break;
			}
			// vertical variable: drawn sideways, so width<->height reversed
			hitRect.setSize(yw, yh);
			x = (int) (yLine.getX() - yw / 2 - yLine.getFontSize() / 3);
			y = (int) (yLine.getY() - yh / 2 - 1);
			hitRect.setLocation(x, y);
			if (hitRect.contains(p) && hasVertVariablesPopup()) {
				reg = VERT_VAR;
				break;
			}
			// inside
			if (!((p.x < l) || (p.y < t) || (p.x > plotDim.width - r) || (p.y > plotDim.height - b))) {
				reg = INSIDE;
				break;
			}
			// scale setter regions 1 - 4
			// horizontal min
			return getScaleSetter().findRegion(p, hitRect, plotDim, offset, l, r, t, b, isPress);
		}
		return reg;
	}

	/**
	 * Gets the scale setter.
	 *
	 * @return the ScaleSetter dialog
	 */
	protected ScaleSetter getScaleSetter() {
		if (scaleSetter == null) {
			scaleSetter = new ScaleSetter();
			plot.getGlassPane().add(scaleSetter);
			// BH opted to dispense with scaleSetterPanel, since
			// scaleSetter itself is a panel, and the added panel adds nothing
			// I have left it borderless opaque for now; we can revise that
			// in ScaleSetter if desired.

			// create transparent scaleSetterPanel with no LayoutManager
			// scaleSetterPanel = new JPanel(null);
			// scaleSetterPanel.setOpaque(false);
			// scaleSetterPanel.add(scaleSetter);
		}

		// refresh autoscale checkbox text if needed (eg, new Locale)
		String s = DialogsRes.SCALE_AUTO;
		if (!s.equals(scaleSetter.autoscaleCheckbox.getText())) {
			scaleSetter.autoscaleCheckbox.setText(s);
		}
		return scaleSetter;
	}

  /**
   * Clears the format cache, called when decimal separator changed
   */
  public void clearFormats() {
  	htFormats.clear();
  	if (scaleSetter != null) {
  		scaleSetter.scaleField.setDecimalSeparator(
  				OSPRuntime.getCurrentDecimalSeparator());
  		scaleSetter.updateValues();
  	}
  }
  
	/**
	 * A dialog with value field and autoscale checkbox.
	 */
	public class ScaleSetter extends JPanel {
		Action scaleAction;
		JCheckBox autoscaleCheckbox;
		int region; // determines which axis and end are active
		boolean pinned = false; // prevents hiding this when true
		private String text;
		private int loc;
		private String constraint;
		private Dimension size;

		ScientificField scaleField = new ScientificField(6, 3) {
			@Override
			public Dimension getPreferredSize() {
				Dimension dim = super.getPreferredSize();
				dim.width -= 4;
				return dim;
			}

		};
		Dimension fieldDim = scaleField.getPreferredSize();

		private ScaleSetter() {
			super(new BorderLayout());
			scaleAction = new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					scaleField.setBackground(Color.white);
					pinned = false;
					boolean auto = autoscaleCheckbox.isSelected();
					boolean horzAxis = true;
					double min = auto ? Double.NaN : scaleField.getValue();
					double max = min;
					switch (region) {
					default:
						return; // BH 2020.02.28 region can be 0.
					case HORZ_MIN:
						max = drawingPanel.isAutoscaleXMax() ? Double.NaN : drawingPanel.getXMax();
						break;
					case HORZ_MAX:
						min = drawingPanel.isAutoscaleXMin() ? Double.NaN : drawingPanel.getXMin();
						break;
					case VERT_MIN:
						horzAxis = false;
						max = drawingPanel.isAutoscaleYMax() ? Double.NaN : drawingPanel.getYMax();
						break;
					case VERT_MAX:
						horzAxis = false;
						min = drawingPanel.isAutoscaleYMin() ? Double.NaN : drawingPanel.getYMin();
					}
					if (horzAxis) {
						drawingPanel.setPreferredMinMaxX(min, max);
					} else {
						drawingPanel.setPreferredMinMaxY(min, max);
					}
					drawingPanel.paintImmediately(0, 0, drawingPanel.getWidth(), drawingPanel.getHeight());
					updateValues();
				}

			};
			autoscaleCheckbox = new JCheckBox();
			autoscaleCheckbox.setBorder(BorderFactory.createEmptyBorder(1, 2, 2, 1));
			autoscaleCheckbox.setBackground(drawingPanel.getBackground());
			autoscaleCheckbox.setHorizontalTextPosition(SwingConstants.RIGHT);
			autoscaleCheckbox.addActionListener(scaleAction);
			scaleField.addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					autoscaleCheckbox.setSelected(false);
					scaleAction.actionPerformed(null);
				}

			});
			scaleField.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					if (scaleField.getBackground() == Color.yellow) {
						autoscaleCheckbox.setSelected(false);
						scaleAction.actionPerformed(null);
					}
				}

			});
			scaleField.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					pinned = true;
					if (e.getClickCount() == 2) {
						scaleField.selectAll();
					}
				}
				@Override
				public void mouseExited(MouseEvent e) {
					if (pinned)
						return;
					Point p = e.getPoint();
					Point loc = scaleField.getLocation();
					p.x += loc.x;
					p.y += loc.y;
					
					Rectangle rect = new Rectangle(scaleSetter.getSize());
					if (!rect.contains(p)) {
						hideIfInactive();
					}
				}

			});
			add(scaleField, BorderLayout.CENTER);
			updateFont();
		}

		public int findRegion(Point p, Rectangle hitRect, Dimension plotDim, int offset, int l, int r, int t, int b,
				boolean isPress) {
			hitRect.setLocation(l - 12, plotDim.height - b + 6 + offset);
			double xmin = drawingPanel.getXMin();
			double xmax = drawingPanel.getXMax();
			double ymin = drawingPanel.getYMin();
			double ymax = drawingPanel.getYMax();
			hitRect.setSize(fieldDim);
			if (hitRect.contains(p)) {
				if (isPress)
					scaleField.setExpectedRange(xmin, xmax);
				set(offset, BorderLayout.NORTH, HORZ_MIN);
				return HORZ_MIN;
			}
			// horizontal max
			hitRect.setLocation(plotDim.width - r - fieldDim.width + 12, plotDim.height - b + 6 + offset);
			if (hitRect.contains(p)) {
				if (isPress)
					scaleField.setExpectedRange(xmin, xmax);
				set(offset, BorderLayout.NORTH, HORZ_MAX);
				return HORZ_MAX;
			}
			// vertical min
			hitRect.setLocation(l - fieldDim.width - 1 - offset, plotDim.height - b - fieldDim.height + 8);
			if (hitRect.contains(p)) {
				if (isPress)
					scaleField.setExpectedRange(ymin, ymax);
				set(offset, BorderLayout.EAST, VERT_MIN);
				return VERT_MIN;
			}
			// vertical max
			hitRect.setLocation(l - fieldDim.width - 1 - offset, t - 8);
			if (hitRect.contains(p)) {
				if (isPress)
					scaleField.setExpectedRange(ymin, ymax);
				set(offset, BorderLayout.EAST, VERT_MAX);
				return VERT_MAX;
			}
			return -1;
		}

		public void updateFont() {
			constraint = text = null;
			FontSizer.setFont(this);
			FontSizer.setFont(autoscaleCheckbox);
		}

		public void updateValues() {
			if (scaleField.getBackground() == Color.yellow)
				return;
			switch (region) {
			case HORZ_MIN:
				scaleField.setValue(drawingPanel.getXMin());
				autoscaleCheckbox.setSelected(drawingPanel.isAutoscaleXMin());
				break;
			case HORZ_MAX:
				scaleField.setValue(drawingPanel.getXMax());
				autoscaleCheckbox.setSelected(drawingPanel.isAutoscaleXMax());
				break;
			case VERT_MIN:
				scaleField.setValue(drawingPanel.getYMin());
				autoscaleCheckbox.setSelected(drawingPanel.isAutoscaleYMin());
				break;
			case VERT_MAX:
				scaleField.setValue(drawingPanel.getYMax());
				autoscaleCheckbox.setSelected(drawingPanel.isAutoscaleYMax());
			}
		}

		public void set(int offset, String constraint, int loc) {
			String text = scaleField.getText();
			if (this.loc == loc && this.constraint == constraint && text.equals(this.text))
				return;
			boolean doValidate = false;
			if (this.constraint != constraint) {
				add(autoscaleCheckbox, constraint);
				doValidate = true;
			} else if (!text.equals(this.text)) {
				doValidate = true;
			}
			if (doValidate)
				size = getPreferredSize();
			validate();
			this.text = text;
			this.constraint = constraint;
			this.loc = loc;
			Point fieldLoc = scaleField.getLocation(); // relative to scaleSetter
			Point hitLoc = hitRect.getLocation(); // relative to plotPanel
			switch (loc) {
			case HORZ_MIN:
				setBounds(hitLoc.x - fieldLoc.x, hitLoc.y - fieldLoc.y - offset, size.width, size.height);
				break;
			case HORZ_MAX:
				setBounds(hitLoc.x - fieldLoc.x, hitLoc.y - fieldLoc.y - offset, size.width, size.height);
				break;
			case VERT_MIN:
				setBounds(Math.max(hitLoc.x, 1) - fieldLoc.x, hitLoc.y - fieldLoc.y, size.width, size.height);
				break;
			case VERT_MAX:
				setBounds(Math.max(hitLoc.x, 1) - fieldLoc.x, hitLoc.y - fieldLoc.y, size.width, size.height);
				break;
			}
		}
		
		public void setVisible(Point p) {
			// JPanel gp = plot.getGlassPane();
			if (p == null) {
				super.setVisible(false);
				return;
			}
			setVisible(true);
//			if (scaleSetterPanel.getParent() != gp) {
//				gp.add(scaleSetterPanel);
//			}
//			OSPLog.debug("CartInter.scaleSetter" + scaleSetterPanel);
//			scaleSetterPanel.setVisible(true);
		}
		
		@Override
		public void setVisible(boolean b) {
			if (!b) {
				setVisible(null);
				return;
			}
			updateValues();
			super.setVisible(b);
		}

		void hideIfInactive() {
			if ((scaleField.getBackground() != Color.yellow) && (scaleField.getSelectedText() == null) && !pinned) {
				hideScaleSetter();
			}
		}

		void setRegion(int mouseRegion) {
			if (region != mouseRegion) {
				autoscaleCheckbox.requestFocusInWindow();
				if (scaleField.getBackground() == Color.yellow) {
					autoscaleCheckbox.setSelected(false);
					scaleAction.actionPerformed(null);
				}
				region = mouseRegion;
				pinned = false;
				updateValues();
				scaleField.select(20, 20); // clears selection and places caret at end
				scaleField.requestFocusInWindow();
			}
		}

	}

	/**
	 * A mouse listener for handling interactivity
	 */
	class AxisMouseListener extends javax.swing.event.MouseInputAdapter {
		@Override
		public void mouseMoved(MouseEvent e) {
			if (!enabled)
				return; // Paco
			altDown = e.isAltDown();
			Point p = e.getPoint();
			drawHitRect = false;
			mouseRegion = findRegion(p, false);
			switch (mouseRegion) {
			case HORZ_MIN:
			case HORZ_MAX:
			case VERT_MIN:
			case VERT_MAX:
				if (!drawingPanel.isFixedScale() && scaleSetter != null) {
					getScaleSetter().setRegion(mouseRegion);
					scaleSetter.setVisible(p);
				}
				return;
			case HORZ_VAR:
			case VERT_VAR:
				drawHitRect = true;
				break;
			default:
				break;
			}
			if (scaleSetter != null)
				scaleSetter.hideIfInactive();
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (!enabled)
				return; // Paco
			double dx = 0, dy = 0, min = 0, max = 0;
			switch (mouseRegion) {
			case INSIDE:
				if (!altDown || drawingPanel.isFixedScale()) {
					return;
				}
				dx = (mouseLoc.x - e.getX()) / plot.getXPixPerUnit();
				min = plot.getXMin() + dx;
				max = plot.getXMax() + dx;
				dx = 0;
				plot.setPreferredMinMaxX(min, max);
				dy = (e.getY() - mouseLoc.y) / plot.getYPixPerUnit();
				min = plot.getYMin() + dy;
				max = plot.getYMax() + dy;
				break;
			case HORZ_AXIS:
				dx = (mouseLoc.x - e.getX()) / plot.getXPixPerUnit();
				min = plot.getXMin() + dx;
				max = plot.getXMax() + dx;
				break;
			case HORZ_AXIS_MIN:
				dx = 2 * (mouseLoc.x - e.getX()) / plot.getXPixPerUnit();
				min = plot.getXMin() + dx;
				max = plot.isAutoscaleXMax() ? Double.NaN : plot.getXMax();
				break;
			case HORZ_AXIS_MAX:
				dx = 2 * (mouseLoc.x - e.getX()) / plot.getXPixPerUnit();
				min = plot.isAutoscaleXMin() ? Double.NaN : plot.getXMin();
				max = plot.getXMax() + dx;
				break;
			case VERT_AXIS:
				dy = (e.getY() - mouseLoc.y) / plot.getYPixPerUnit();
				min = plot.getYMin() + dy;
				max = plot.getYMax() + dy;
				break;
			case VERT_AXIS_MIN:
				dy = 2 * (e.getY() - mouseLoc.y) / plot.getYPixPerUnit();
				min = plot.getYMin() + dy;
				max = plot.isAutoscaleYMax() ? Double.NaN : plot.getYMax();
				break;
			case VERT_AXIS_MAX:
				dy = 2 * (e.getY() - mouseLoc.y) / plot.getYPixPerUnit();
				min = plot.isAutoscaleYMin() ? Double.NaN : plot.getYMin();
				max = plot.getYMax() + dy;
				break;
			}
			if (dx != 0) {
				plot.setPreferredMinMaxX(min, max);
			} else if (dy != 0) {
				plot.setPreferredMinMaxY(min, max);
			}
			// Call any registered listener: Paco
			for (ActionListener listener : axisListeners)
				listener.actionPerformed(new ActionEvent(CartesianInteractive.this, e.getID(), "axis dragged")); //$NON-NLS-1$

			plot.invalidateImage();
			plot.repaint();
			mouseLoc = e.getPoint();
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if (!enabled)
				return; // Paco
			plot.requestFocusInWindow();
			altDown = e.isAltDown();
			mouseLoc = e.getPoint();
			mouseX = plot.pixToX(plot.getMouseIntX());
			mouseY = plot.pixToY(plot.getMouseIntY());
			mouseRegion = findRegion(mouseLoc, true);
			switch (mouseRegion) {
			case HORZ_MIN:
			case HORZ_MAX:
			case VERT_MIN:
			case VERT_MAX:
				if (scaleSetter != null && !drawingPanel.isFixedScale()) {
					scaleSetter.setVisible(true);
				}
				return;
			case HORZ_VAR:
				drawHitRect = false;
				getHorzVariablesPopup().show(plot, mouseLoc.x - 20, mouseLoc.y - 12);
				break;
			case VERT_VAR:
				drawHitRect = false;
				getVertVariablesPopup().show(plot, mouseLoc.x - 20, mouseLoc.y - 12);
				break;
			default:
				break;
			}
			plot.repaint();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (!enabled)
				return; // Paco
			mouseX = Double.NaN;
			mouseY = Double.NaN;
		}

		@Override
		public void mouseExited(MouseEvent e) {
			if (!enabled)
				return; // Paco
			Point p = e.getPoint();
			// BH should not be necessary to convert modifiers to text.
			Rectangle rect = new Rectangle(plot.getSize());
			if (!rect.contains(p) && (scaleSetter != null)
					&& "".equals(InputEvent.getModifiersExText(e.getModifiersEx()))) { //$NON-NLS-1$
				hideScaleSetter();
			}
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
