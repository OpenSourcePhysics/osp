/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.event.MouseInputAdapter;

/**
 * InteractivePanel is a drawing panel that invokes the handleMouseAction method in
 * Interactive objects.
 * @author Wolfgang Christian
 * @author Francisco Esquembre
 * @version 1.0
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
  protected boolean containsInteractive = false;
  protected int mouseAction = 0;
  protected MouseEvent mouseEvent = null;
  protected InteractiveMouseHandler interactive = null;
  private Interactive iaDraggable = null; // interactive object that is being dragged
  private Selectable iaSelectable = null; // interactive object that has been selected
	Object customCursor;
	boolean isMobile=OSPRuntime.isJS;  // for testing

  /**
   * Constructs an InteractivePanel with the given handler.
   * @param in InteractiveMouseHandler
   */
  public InteractivePanel(InteractiveMouseHandler in) {
    this();
    interactive = in;
  }

	/**
	 * Constructs an InteractivePanel with an internal handler.
	 */
	public InteractivePanel() {
		super();
		isInteractive = true;
		interactive = this; // this panel is the default handler
		customCursor = /** @j2sNative $('#customCursor')[0] || */
			null;
	}
	
  /**
   * Moves the custom JavaScript cursor when running on a mobile device.
   * @param e  MouseEvent
   * @param mode  String 
   */
	@SuppressWarnings("unused")
	protected void moveJSCursor(MouseEvent e, String mode) {
		if(!isMobile) return;
		// Function to move the custom cursor
		if (customCursor != null) {	
			String display = "block";
			String background = "rgba(0,255,0,0.3)";
			switch (mode) {
			case "move":
			case "enter":
				background = "transparent";
				break;
			case "drag":
			case "down":
				break;
			case "up":
			case "exit":
				background = "transparent";
				display = "none";
				break;
			}
			Point pt = e.getLocationOnScreen();
			/** @j2sNative 
			  this.customCursor.style.left = pt.x + "px";
			  this.customCursor.style.top = pt.y + "px";
        background && (this.customCursor.style.backgroundColor = background);
		    display && (this.customCursor.style.display = display);
			 * 
			 */
		}
	}
	
  /**
   * Sets the JavaScript cursor color when running on a mobile device.
   * @param e  MouseEvent
   * @param mode  String 
   */
	@SuppressWarnings("unused")
	protected void setCustomCursorColor(String color) {
		if(!isMobile) return;
		// Function to move the custom cursor
		if (customCursor != null) {	
			/** @j2sNative 
        this.customCursor.style.backgroundColor = color;
			 * 
			 */
		}
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
   * @param drawable
   */
  @Override
public void addDrawable(Drawable drawable) {
    super.addDrawable(drawable);
    if(drawable.isInteractive()) {
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
   * Sets the x axis scale based on the max and min values of all measurable objects.
   * Autoscale flag is not respected.
   */
  @Override
protected void scaleX(ArrayList<Drawable> tempList) {
    double tempmin = xminPreferred;
    double tempmax = xmaxPreferred;
    super.scaleX(tempList);
    // increase but do not decrease if dragging and autoscaling
    if(autoscaleX&&(mouseAction==MOUSE_DRAGGED)) {
      if(xminPreferred>tempmin) {
        xminPreferred = tempmin;
      }
      if(xmaxPreferred<tempmax) {
        xmaxPreferred = tempmax;
      }
    }
  }

  /**
   * Sets the y axis scale based on the max and min values of all measurable objects.
   * Autoscale flag is not respected.
   */
  @Override
protected void scaleY(ArrayList<Drawable> tempList) {
    double tempmin = yminPreferred;
    double tempmax = ymaxPreferred;
    super.scaleY(tempList);
    // increase but do not decrease if dragging and autoscaling
    if(autoscaleY&&(mouseAction==MOUSE_DRAGGED)) {
      if(yminPreferred>tempmin) {
        yminPreferred = tempmin;
      }
      if(ymaxPreferred<tempmax) {
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
    interactive = handler;
  }

  /**
   * Handles mouse actions by dragging the current interactive drawable object.
   *
   * @param panel
   * @param evt
   */
  @Override
public void handleMouseAction(InteractivePanel panel, MouseEvent evt) {
    switch(panel.getMouseAction()) {
       case InteractivePanel.MOUSE_CLICKED :
      	 //moveJSCursor(evt, "down");
         Interactive clickedIA = getInteractive();
         if((panel.getMouseClickCount()<2)||(clickedIA==null)||!(clickedIA instanceof Selectable)) {
           return;
         }
         if((iaSelectable!=null)&&(iaSelectable!=clickedIA)) {
           iaSelectable.setSelected(false);
         }
         iaSelectable = ((Selectable) clickedIA);
         iaSelectable.toggleSelected();
         invalidateImage();       //validImage = false;
         if(!getIgnoreRepaint()) {
           panel.repaint();
         }
         break;
       case InteractivePanel.MOUSE_DRAGGED :
      	 moveJSCursor(evt, "drag");
         if(iaDraggable==null) {
           return;                // nothing to drag
         }
         double x = panel.getMouseX();
         double y = panel.getMouseY();
         if(!autoscaleX&&(evt.getX()<1+leftGutter)) {
           x = panel.pixToX(1+leftGutter);
         }
         if(!autoscaleX&&(evt.getX()>panel.getWidth()-1-rightGutter)) {
           x = panel.pixToX(panel.getWidth()-1-rightGutter);
         }
         if(!autoscaleY&&(evt.getY()<1+topGutter)) {
           y = panel.pixToY(1+topGutter);
         }
         if(!autoscaleY&&(evt.getY()>panel.getHeight()-1-bottomGutter)) {
           y = panel.pixToY(panel.getHeight()-1-bottomGutter);
         }
         iaDraggable.setXY(x, y); // drag the interactive object
         invalidateImage();       //validImage = false;
         if(!getIgnoreRepaint()) {
           panel.repaint();       // repaint to keep the screen up to date
         }
         break;
       case InteractivePanel.MOUSE_RELEASED :
      	 moveJSCursor(evt, "up");
         if((autoscaleX||autoscaleY)&&!getIgnoreRepaint()) {
           panel.repaint();       // repaint to keep the screen up to date
         }
         break;
    }
  }

  /**
   * Get the Interactive object that is currently being dragged.
   * @return Interactive
   */
  public Interactive getCurrentDraggable() {
    return iaDraggable;
  }

//  private Drawable[] darray;

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
   * @return int
   */
  public int getMouseButton() {
    switch(mouseEvent.getModifiers()) {
       case java.awt.event.InputEvent.BUTTON1_MASK :
         return 1;
       case java.awt.event.InputEvent.BUTTON2_MASK :
         return 2;
       case java.awt.event.InputEvent.BUTTON3_MASK :
         return 3;
       default :
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
   * @return int
   */
  public int getMouseAction() {
    return mouseAction;
  }

  /**
   * Gets the x pixel coordinate of the last mouse event.
   * @return pixel
   */
  public int getMouseIntX() {
    return mouseEvent.getX();
  }

  /**
   * Gets the y pixel coordinate of the last mouse event.
   * @return pixel
   */
  public int getMouseIntY() {
    return mouseEvent.getY();
  }

  /**
   * Gets the x world coordinate of the last mouse event.
   * @return coordiante
   */
  public double getMouseX() {
    return pixToX(mouseEvent.getX());
  }

  /**
   * Gets the y world coordinate of the last moust event
   * @return coordinate
   */
  public double getMouseY() {
    return pixToY(mouseEvent.getY());
  }

  /**
   * Saves the last mouse event.
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
    /**
     * Handle the mouse pressed event.
     * @param e
     */
  @SuppressWarnings("unused")
	@Override
	public void mousePressed(MouseEvent e) {
      moveJSCursor(e, "down");
      mouseEvent = e;
      mouseAction = MOUSE_PRESSED;
      if(interactive!=null) { // is there an object available to handle the mouse event
        interactive.handleMouseAction(InteractivePanel.this, e);
        iaDraggable = null;   // force the panel to search all drawables
        iaDraggable = getInteractive();
        if(iaDraggable!=null) {
          if(iaDraggable instanceof Selectable) {
            setMouseCursor(((Selectable) iaDraggable).getPreferredCursor());
          } else {
            setMouseCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          }
          setCustomCursorColor("rgba(255,255,0,0.5)");
        }
      }
      if(isShowCoordinates()) {
        String s = coordinateStrBuilder.getCoordinateString(InteractivePanel.this, e);
        if (setMessage(s, MessageDrawable.BOTTOM_LEFT) && !messagesAsJLabels)
        	repaint();
      }
    }

    /**
     * Handles the mouse released event.
     * @param e
     */
    @SuppressWarnings("unused")
	@Override
	public void mouseReleased(MouseEvent e) {
    	moveJSCursor(e, "up");
      mouseEvent = e;
      mouseAction = MOUSE_RELEASED;
      if(interactive!=null) {
        interactive.handleMouseAction(InteractivePanel.this, e);
      }
      iaDraggable = null;
      if(isShowCoordinates()) {
        if (setMessage(null, MessageDrawable.BOTTOM_LEFT) && !messagesAsJLabels)
        	repaint();
      }
      setMouseCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    /**
     * Handles the mouse entered event.
     * @param e
     */
    @Override
	public void mouseEntered(MouseEvent e) {
    	moveJSCursor(e, "enter");
      if(isShowCoordinates()) {
        setMouseCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
      }
      mouseEvent = e;
      mouseAction = MOUSE_ENTERED;
      if(interactive!=null) {
        interactive.handleMouseAction(InteractivePanel.this, e);
      }
    }

    /**
     * Handles the mouse exited event.
     * @param e
     */
    @Override
	public void mouseExited(MouseEvent e) {
    	moveJSCursor(e, "exit");
      setMouseCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      mouseEvent = e;
      mouseAction = MOUSE_EXITED;
      if(interactive!=null) {
        interactive.handleMouseAction(InteractivePanel.this, e);
      }
    }

    /**
     * Handles the mouse clicked event.
     * @param e
     */
    @Override
	public void mouseClicked(MouseEvent e) {
    	//moveJSCursor(e, "down");
      mouseEvent = e;
      mouseAction = MOUSE_CLICKED;
      if(interactive==null) {
        return;
      }
      interactive.handleMouseAction(InteractivePanel.this, e);
    }

    /**
     * Handles the mouse dragged event.
     * @param e
     */
    @SuppressWarnings("unused")
	@Override
	public void mouseDragged(MouseEvent e) {
    	moveJSCursor(e, "dragged");
      mouseEvent = e;
      mouseAction = MOUSE_DRAGGED;
      if(interactive!=null) {
        interactive.handleMouseAction(InteractivePanel.this, e);
      }
      if(isShowCoordinates()) {
        String s = coordinateStrBuilder.getCoordinateString(InteractivePanel.this, e);
        if (setMessage(s, 0) && !messagesAsJLabels)
        	repaint();
      }
    }

    /**
     * Handles the mouse moved event.
     * @param e
     */
    @Override
	public void mouseMoved(MouseEvent e) {
    	moveJSCursor(e, "move");
      mouseEvent = e;
      mouseAction = MOUSE_MOVED;
      iaDraggable = null;
      if(interactive!=null) { // check to see if there is an interactive object
        interactive.handleMouseAction(InteractivePanel.this, e);
        Interactive iad = getInteractive();
        if(iad==null) {
          setMouseCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
          if(iad instanceof Selectable) {
            setMouseCursor(((Selectable) iad).getPreferredCursor());
          } else {
            setMouseCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          }
          setCustomCursorColor("rgba(255,255,0, 0.5)");// yellow
        }
      }
    }

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
 * Copyright (c) 2024  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
