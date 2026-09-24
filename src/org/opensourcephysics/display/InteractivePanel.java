/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.event.MouseInputAdapter;

/**
 * InteractivePanel is a drawing panel that invokes the handleMouseAction method in
 * Interactive objects.
 * 
 * Version 2.0 adds a custom CSS cursor when this code is transpiled to JavaScript.
 * 
 * @author Wolfgang Christian
 * @author Francisco Esquembre
 * @version 2.0 
 */
@SuppressWarnings("serial")
public class InteractivePanel extends DrawingPanel implements InteractiveMouseHandler {
	
  public static final int MOUSE_PRESSED = 1;
  public static final int MOUSE_RELEASED = 2;
  public static final int MOUSE_DRAGGED = 3;
  public static final int MOUSE_CLICKED = 4;
  public static final int MOUSE_ENTERED = 5;
  public static final int MOUSE_EXITED = 6;
  public static final int MOUSE_MOVED = 7;
  protected String cursorColor= "transparent";
  protected boolean containsInteractive = false;
  protected int mouseAction = 0;
  protected MouseEvent mouseEvent = null;
  protected InteractiveMouseHandler mouseHandler = null;
  private Interactive iaDraggable = null; // interactive object that is being dragged
  private Selectable iaSelectable = null; // interactive object that has been selected
	Object customCursor;
  
	
  /*not final*/ static String ipadStyle = ".custom-cursor {\r\n" + 
			"  width: 25px;\r\n" + 
			"  height: 25px;\r\n" + 
			"  background-color: transparent;\r\n" + 
			"  /* Make the background transparent */\r\n" + 
			"  position: absolute;\r\n" + 
			"  pointer-events: none;\r\n" + 
			"  transform: translate(-50%, -50%);\r\n" + 
			"  display: none;\r\n" + 
			"}\r\n" + 
			"\r\n" + 
			".custom-cursor::before,\r\n" + 
			".custom-cursor::after {\r\n" + 
			"  content: '';\r\n" + 
			"  position: absolute;\r\n" + 
			"  background-color: black;\r\n" + 
			"  /* Color of the crosshair */\r\n" + 
			"}\r\n" + 
			"\r\n" + 
			".custom-cursor::before {\r\n" + 
			"  width: 25px;\r\n" + 
			"  /* Horizontal line length */\r\n" + 
			"  height: 3px;\r\n" + 
			"  /* Horizontal line thickness */\r\n" + 
			"  border: 1px solid white;  /* White border */\r\n" + 
			"  box-sizing: border-box;   /* Ensures border is included in width/height */\r\n" + 
			"  top: 50%;\r\n" + 
			"  left: 0;\r\n" + 
			"  transform: translateY(-50%);\r\n" + 
			"}\r\n" + 
			"\r\n" + 
			".custom-cursor::after {\r\n" + 
			"  width: 3px;\r\n" + 
			"  /* Vertical line thickness */\r\n" + 
			"  height: 25px;\r\n" + 
			"  /* Vertical line length */\r\n" + 
			"  border: 1px solid white;  /* White border */\r\n" + 
			"  box-sizing: border-box;   /* Ensures border is included in width/height */\r\n" + 
			"  top: 0;\r\n" + 
			"  left: 50%;\r\n" + 
			"  transform: translateX(-50%);\r\n" + 
			"}";
	
	/**
     * Constructs an InteractivePanel with the given handler.
     * @param in InteractiveMouseHandler
     */
    public InteractivePanel(InteractiveMouseHandler in) {
        this();
        mouseHandler = in;
    }

	/**
	 * Constructs an InteractivePanel with an internal handler.
	 */
	public InteractivePanel() {
		super();
		isInteractive = true;
		mouseHandler = this; // this panel is the default handler
	}
	

	@Override
	protected void setMouseListeners() {
		// create and add a new mouse controller for interactive drawing
		mouseController = new IADMouseController();
		addMouseListener(mouseController);
		addMouseMotionListener(mouseController);
		addOptionController();
	}


	/**
	 * Adds a drawable object to the drawable list.
	 * 
	 * @param drawable
	 */
	@Override
	public void addDrawable(Drawable drawable) {
		super.addDrawable(drawable);
		if (drawable.isInteractive()) {
			containsInteractive = true;
		}
	}

	/**
	 * Removes all drawable objects from the drawable list.
	 */
	@Override
	public void clear() {
		super.clear();
		containsInteractive = false;
	}

	/**
	 * Sets the x axis scale based on the max and min values of all measurable
	 * objects. Autoscale flag is not respected.
	 */
	@Override
	protected void scaleX(ArrayList<Drawable> tempList) {
		double tempmin = xminPreferred;
		double tempmax = xmaxPreferred;
		super.scaleX(tempList);
		// increase but do not decrease if dragging and autoscaling
		if (autoscaleX && (mouseAction == MOUSE_DRAGGED)) {
			if (xminPreferred > tempmin) {
				xminPreferred = tempmin;
			}
			if (xmaxPreferred < tempmax) {
				xmaxPreferred = tempmax;
			}
		}
	}

	/**
	 * Sets the y axis scale based on the max and min values of all measurable
	 * objects. Autoscale flag is not respected.
	 */
	@Override
	protected void scaleY(ArrayList<Drawable> tempList) {
		double tempmin = yminPreferred;
		double tempmax = ymaxPreferred;
		super.scaleY(tempList);
		// increase but do not decrease if dragging and autoscaling
		if (autoscaleY && (mouseAction == MOUSE_DRAGGED)) {
			if (yminPreferred > tempmin) {
				yminPreferred = tempmin;
			}
			if (ymaxPreferred < tempmax) {
				ymaxPreferred = tempmax;
			}
		}
	}

	/**
	 * Sets the interactive mouse handler.
	 *
	 * The interactive mouse handler is notified whenever a mouse action occurs.
	 *
	 * @param handler the mouse handler
	 */
	public void setInteractiveMouseHandler(InteractiveMouseHandler handler) {
		mouseHandler = handler;
	}

	/**
	 * Handles mouse actions by dragging the current interactive drawable object.
	 *
	 * @param panel
	 * @param evt
	 */
	@Override
	public void handleMouseAction(InteractivePanel panel, MouseEvent evt) {
		switch (panel.getMouseAction()) {
		case MOUSE_CLICKED:
			jsCustomCursor("released", "transparent", evt);
			Interactive clickedIA = getInteractive();
			if ((panel.getMouseClickCount() < 2) || (clickedIA == null) || !(clickedIA instanceof Selectable)) {
				return;
			}
			if ((iaSelectable != null) && (iaSelectable != clickedIA)) {
				iaSelectable.setSelected(false);
			}
			iaSelectable = ((Selectable) clickedIA);
			iaSelectable.toggleSelected();
			invalidateImage(); // validImage = false;
			if (!getIgnoreRepaint()) {
				panel.repaint();
			}
			break;
		case MOUSE_DRAGGED:
			jsCustomCursor("dragged", cursorColor, evt);
			if (iaDraggable == null) {
				return; // nothing to drag
			}
			double x = panel.getMouseX();
			double y = panel.getMouseY();
			if (!autoscaleX && (evt.getX() < 1 + leftGutter)) {
				x = panel.pixToX(1 + leftGutter);
			}
			if (!autoscaleX && (evt.getX() > panel.getWidth() - 1 - rightGutter)) {
				x = panel.pixToX(panel.getWidth() - 1 - rightGutter);
			}
			if (!autoscaleY && (evt.getY() < 1 + topGutter)) {
				y = panel.pixToY(1 + topGutter);
			}
			if (!autoscaleY && (evt.getY() > panel.getHeight() - 1 - bottomGutter)) {
				y = panel.pixToY(panel.getHeight() - 1 - bottomGutter);
			}
			iaDraggable.setXY(x, y); // drag the interactive object
			invalidateImage(); // validImage = false;
			if (!getIgnoreRepaint()) {
				panel.repaint(); // repaint to keep the screen up to date
			}
			break;
		case MOUSE_RELEASED:
			jsCustomCursor("released", cursorColor, evt);
			if ((autoscaleX || autoscaleY) && !getIgnoreRepaint()) {
				panel.repaint(); // repaint to keep the screen up to date
			}
			break;
		}
	}

	/**
	 * Get the Interactive object that is currently being dragged.
	 * 
	 * @return Interactive
	 */
	public Interactive getCurrentDraggable() {
		return iaDraggable;
	}

	/**
	 * Gets the interactive object that was accessed by the last mouse event.
	 * 
	 * @return Interactive
	 */
	public Interactive getInteractive() {
		if (!containsInteractive) {
			return null; // don't check unless we have a least one Interactive
		}
		if (iaDraggable != null) {
			return iaDraggable;
		}
		if ((iaSelectable != null) && iaSelectable.isSelected()) {
			// check only selected object
			return ((Interactive) iaSelectable).findInteractive(this, mouseEvent.getX(), mouseEvent.getY());
		}
		Interactive iad = null;
		synchronized (drawableList) {
			int x = mouseEvent.getX();
			int y = mouseEvent.getY();
			int n = drawableList.size();
			for (int i = n; --i >= 0;) {
				Drawable obj = drawableList.get(i);
				// isInteractive() true is declared default in Interactive interface,
				// overriding default iSinteractive() false in Drawable
				if (obj.isInteractive() && (iad = ((Interactive) obj).findInteractive(this, x, y)) != null) {
					break;
				}
			}
		}
		return iad;
	}

	/**
	 * Shows the coordinates in the text box in the lower left hand corner.
	 *
	 * @param show
	 */
	@Override
	public void setShowCoordinates(boolean show) {
		showCoordinates = show;
	}

	/**
	 * Gets the mouse button of the last mouse event.
	 * 
	 * @return int
	 */
	public int getMouseButton() {
		switch (mouseEvent.getModifiers()) {
		case java.awt.event.InputEvent.BUTTON1_MASK:
			return 1;
		case java.awt.event.InputEvent.BUTTON2_MASK:
			return 2;
		case java.awt.event.InputEvent.BUTTON3_MASK:
			return 3;
		default:
			return 0;
		}
	}

	/**
	 * Gets the click count of the last mouse event.
	 *
	 * @return int
	 */
	public int getMouseClickCount() {
		return mouseEvent.getClickCount();
	}

	/**
	 * Gets the last mouse action.
	 * 
	 * @return int
	 */
	public int getMouseAction() {
		return mouseAction;
	}

	/**
	 * Gets the x pixel coordinate of the last mouse event.
	 * 
	 * @return pixel
	 */
	public int getMouseIntX() {
		return mouseEvent.getX();
	}

	/**
	 * Gets the y pixel coordinate of the last mouse event.
	 * 
	 * @return pixel
	 */
	public int getMouseIntY() {
		return mouseEvent.getY();
	}

	/**
	 * Gets the x world coordinate of the last mouse event.
	 * 
	 * @return coordiante
	 */
	public double getMouseX() {
		return pixToX(mouseEvent.getX());
	}

	/**
	 * Gets the y world coordinate of the last moust event
	 * 
	 * @return coordinate
	 */
	public double getMouseY() {
		return pixToY(mouseEvent.getY());
	}

	/**
	 * Saves the last mouse event.
	 * 
	 * @param type
	 * @param evt
	 */
	public void saveMouseEvent(int type, java.awt.event.MouseEvent evt) {
		mouseAction = type;
		mouseEvent = evt;
	}

	/**
	 * The inner class that will handle all mouse related events.
	 */
	protected class IADMouseController extends MouseInputAdapter {

		@Override
		public void mousePressed(MouseEvent e) {
			doMousePressed(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			doMouseReleased(e);
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			doMouseEntered(e);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			doMouseExit(e);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			doMouseClicked(e);
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			doMouseDragged(e);
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			doMouseMoved(e);
		}

	}

    @SuppressWarnings("unused")
	protected void doMousePressed(MouseEvent e) {
        mouseEvent = e;
        mouseAction = MOUSE_PRESSED;
        if(mouseHandler!=null) { // is there an object available to handle the mouse event
          mouseHandler.handleMouseAction(this, e);
          iaDraggable = null;   // force the panel to search all drawables
          iaDraggable = getInteractive();
          if(iaDraggable!=null) {
            if(iaDraggable instanceof Selectable) {
              setMouseCursor(((Selectable) iaDraggable).getPreferredCursor());
              //cursorColor= "lightgreen";
              cursorColor= "rgba(63, 255, 63, 0.5)";
            } else {
              setMouseCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
              //cursorColor= "lightskyblue";
              cursorColor= "rgba(63, 63, 255, 0.5)";
            }
          }else {
          	cursorColor= "transparent";
          }
        }
        jsCustomCursor("pressed", cursorColor, e);
        if(isShowCoordinates()) {
          String s = coordinateStrBuilder.getCoordinateString(this, e);
          if (setMessage(s, MessageDrawable.BOTTOM_LEFT) && !messagesAsJLabels)
          	repaint();
        }
	}

	@SuppressWarnings("unused")
	protected void doMouseReleased(MouseEvent e) {
    	jsCustomCursor("released", cursorColor, e);
        mouseEvent = e;
        mouseAction = MOUSE_RELEASED;
        if(mouseHandler!=null) {
          mouseHandler.handleMouseAction(this, e);
        }
        iaDraggable = null;
        if(isShowCoordinates()) {
          if (setMessage(null, MessageDrawable.BOTTOM_LEFT) && !messagesAsJLabels)
          	repaint();
        }
        setMouseCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
	}

	protected void doMouseEntered(MouseEvent e) {
    	jsCustomCursor("entered", cursorColor, e);
        if(isShowCoordinates()) {
          setMouseCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }
        mouseEvent = e;
        mouseAction = MOUSE_ENTERED;
        if(mouseHandler!=null) {
          mouseHandler.handleMouseAction(this, e);
        }
	}

	protected void doMouseExit(MouseEvent e) {
		cursorColor = "transparent";
		jsCustomCursor("exited", cursorColor, e);
		setMouseCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		mouseEvent = e;
		mouseAction = MOUSE_EXITED;
		if (mouseHandler != null) {
			mouseHandler.handleMouseAction(this, e);
		}
	}

	protected void doMouseClicked(MouseEvent e) {
    	jsCustomCursor("pressed",cursorColor, e);
        mouseEvent = e;
        mouseAction = MOUSE_CLICKED;
        if(mouseHandler==null) {
          return;
        }
        mouseHandler.handleMouseAction(this, e);
	}

	@SuppressWarnings("unused")
	protected void doMouseDragged(MouseEvent e) {
		jsCustomCursor("dragged", cursorColor, e);
		mouseEvent = e;
		mouseAction = MOUSE_DRAGGED;
		if (mouseHandler != null) {
			mouseHandler.handleMouseAction(this, e);
		}
		if (isShowCoordinates()) {
			String s = coordinateStrBuilder.getCoordinateString(this, e);
			if (setMessage(s, 0) && !messagesAsJLabels)
				repaint();
		}
	}

	protected void doMouseMoved(MouseEvent e) {
        mouseEvent = e;
        mouseAction = MOUSE_MOVED;
        iaDraggable = null;
        if(mouseHandler!=null) { // check to see if there is an interactive object
          mouseHandler.handleMouseAction(this, e);
          Interactive iad = getInteractive();
          if(iad==null) {
            setMouseCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            cursorColor= "transparent";
          } else {
            if(iad instanceof Selectable) {
              setMouseCursor(((Selectable) iad).getPreferredCursor());
              //cursorColor= "lightgreen";
              cursorColor= "rgba(63, 255, 63, 0.5)";
            } else {
              setMouseCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
              //cursorColor= "lightskyblue";
              cursorColor= "rgba(63, 63, 255, 0.5)";
            }
          }
        }
        jsCustomCursor("moved", cursorColor, e);
	}

	private static void jsCustomCursor(String action, String cursorColor, MouseEvent e) {
		// System.out.println(action +" isMobile="+OSPRuntime.isMobileWC);
		// System.out.println(" event=" + e);
		if (!OSPRuntime.cssCursor)
			return;
		
		if (ipadStyle != null) {
			/**
			 * Set this only once.
			 * 
			 * @j2sNative
			 * 
			 *  $("body").append('<style>'+ C$.ipadStyle + '</style>');
			 * 	$("body").append('<div class="custom-cursor" id="customCursor" style="z-index: 10000000;"></div>');
			 * 		
			 */		
			ipadStyle = null;
		}
		
		String x = e.getXOnScreen() + "px";
		String y = e.getYOnScreen() + "px";
		switch (action) {
		case "pressed":
		case "released":
		case "dragged":
		case "moved":
			setCustomCursor(x, y, cursorColor);
			break;
		case "entered":
			showCustomCursor(x, y, cursorColor);
			break;
		case "exited":
			hideCustomCursor(x, y);
			break;
		default:
			return;
		}
	}

	private static void setCustomCursor(String x, String y, String c) {
		/**
		 * @j2sNative 
		 * 
		 * $('#customCursor').css({"backgroundColor":c,"left": x, "top": y});
		 */
	}

	private static void showCustomCursor(String x, String y, String c) {
		/**
		 * @j2sNative
		 * 
		 * $('#customCursor').css({"backgroundColor":c, "left": x
		 *           , "top": y, "display": "block", "width":'25px',
		 *            "height":'25px'});
		 */
	}

	private static void hideCustomCursor(String x, String y) {
		/**
		 * @j2sNative 
		 * 
		 * $('#customCursor').css({"backgroundColor":'transparent', "left": x
		 *           , "top": y, "display": "none", "width":'0px',
		 *            "height":'0px'});
		 */
	}

	@Override
	public void dispose() {
		if (mouseController != null) {
			removeMouseListener(mouseController);
			removeMouseMotionListener(mouseController);
			mouseController = null;
		}
		if (optionController != null) {
			removeMouseListener(optionController);
			removeMouseMotionListener(optionController);
			optionController = null;
		}
		super.dispose();
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
 * Copyright (c) 2026  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
