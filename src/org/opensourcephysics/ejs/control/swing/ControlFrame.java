/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.ejs.control.swing;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.opensourcephysics.ejs.control.ControlElement;
import org.opensourcephysics.ejs.control.value.BooleanValue;
import org.opensourcephysics.ejs.control.value.Value;

/**
 * A configurable Frame. It has no internal value, nor can trigger
 * any action.
 */
public class ControlFrame extends ControlWindow {
  static private final int MODE_TITLE = 0;
  static private final int MODE_SET_RESIZE = 1;
  static private final int MODE_EXIT = 2;
  static private final int MODE_ONEXIT = 3;
  static private final int NAME = ControlWindow.NAME+4; // shadows superclass field
  protected JFrame frame;

  // ------------------------------------------------
  // Visual component
  // ------------------------------------------------

  /**
   * Constructor ControlFrame
   * @param _visual
   */
  public ControlFrame(Object _visual) {
    super(_visual);
  }

  @Override
protected java.awt.Component createVisual(Object _visual) {
    startingup = true;
    if(_visual instanceof JFrame) {
      frame = (JFrame) _visual;
    } else {
      frame = new JFrame();
      frame.getContentPane().setLayout(new java.awt.BorderLayout());
    }
    frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    internalValue = new BooleanValue(true);
    // setProperty ("visible","true");
    frame.addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
	public void windowClosing(java.awt.event.WindowEvent evt) {
        internalValue.setValue(false);
        variableChanged(ControlWindow.VISIBLE+4, internalValue);
        if(frame.getDefaultCloseOperation()==JFrame.EXIT_ON_CLOSE) {
          // System.out.println (frame.getName()+" is closing");
          invokeActions();
        }
      }

    });
    return frame.getContentPane();
  }

  @Override
public java.awt.Component getComponent() {
    return frame;
  }

  @Override
public java.awt.Container getContainer() {
    return frame.getContentPane();
  }

  // ------------------------------------------------
  // Properties
  // ------------------------------------------------
  static private java.util.ArrayList<String> infoList = null;

  @Override
public java.util.ArrayList<String> getPropertyList() {
    if(infoList==null) {
      infoList = new java.util.ArrayList<String>();
      infoList.add("title");     //$NON-NLS-1$
      infoList.add("resizable"); //$NON-NLS-1$
      infoList.add("exit");      //$NON-NLS-1$
      infoList.add("onExit");    //$NON-NLS-1$
      infoList.addAll(super.getPropertyList());
    }
    return infoList;
  }

  @Override
public String getPropertyInfo(String _property) {
    if(_property.equals("title")) { //$NON-NLS-1$
      return "String TRANSLATABLE"; //$NON-NLS-1$
    }
    if(_property.equals("resizable")) { //$NON-NLS-1$
      return "boolean BASIC";           //$NON-NLS-1$
    }
    if(_property.equals("exit")) {      //$NON-NLS-1$
      return "boolean CONSTANT HIDDEN"; //$NON-NLS-1$
    }
    if(_property.equals("onExit")) {   //$NON-NLS-1$
      return "Action CONSTANT HIDDEN"; //$NON-NLS-1$
    }
    return super.getPropertyInfo(_property);
  }

  // ------------------------------------------------
  // Set and Get the values of the properties
  // ------------------------------------------------
  @Override
public void setValue(int _index, Value _value) {
    switch(_index) {
       case MODE_TITLE:                                                      // title
         String ejsWindow = getProperty("_ejs_window_");             //$NON-NLS-1$
         if(ejsWindow!=null) {
           frame.setTitle(_value.getString()+" "+ejsWindow);         //$NON-NLS-1$
         } else {
           frame.setTitle(_value.getString());
         }
         break;
       case MODE_SET_RESIZE:
         frame.setResizable(_value.getBoolean());
         break;
       case MODE_EXIT:                                                      // exit
         if(getProperty("_ejs_")==null) {                            //$NON-NLS-1$
           if(_value.getBoolean()) {
             frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
           } else {
             frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
           }
         }
         break;
       case 3 :                                                      // action
         removeAction(ControlElement.ACTION, getProperty("onExit")); //$NON-NLS-1$
         addAction(ControlElement.ACTION, _value.getString());
         break;
       case NAME:                                                   // Overrides ControlElement's 'name'
         super.setValue(ControlWindow.NAME, _value);
         if((getGroup()!=null)&&(getGroup().getOwnerFrame()==getComponent())) {
           String replacement = getGroup().getReplaceOwnerName();
           if((replacement!=null)&&replacement.equals(_value.getString())) {
             getGroup().setOwnerFrame(getGroup().getReplaceOwnerFrame());
           } else {
             getGroup().setOwnerFrame(frame);
           }
         }
         break;
       default :
         super.setValue(_index-4, _value);
         break;
    }
  }

  @Override
public void setDefaultValue(int _index) {
    switch(_index) {
       case MODE_TITLE:                                                      // title
         String ejsWindow = getProperty("_ejs_window_");             //$NON-NLS-1$
         if(ejsWindow!=null) {
           frame.setTitle(ejsWindow);
         } else {
           frame.setTitle("");                                       //$NON-NLS-1$
         }
         break;
       case MODE_SET_RESIZE:
         frame.setResizable(true);
         break;
       case MODE_EXIT:                                                      // exit
         if(getProperty("_ejs_")==null) {                            //$NON-NLS-1$
           frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
         }
         break;
       case MODE_ONEXIT:
         removeAction(ControlElement.ACTION, getProperty("onExit")); //$NON-NLS-1$
         break;
       case NAME:
    	   // Overrides ControlElement's 'name'
         super.setDefaultValue(ControlWindow.NAME);
         if((getGroup()!=null)&&(getGroup().getOwnerFrame()==getComponent())) {
           getGroup().setOwnerFrame(frame);
         }
         // BH! break missing here. 
         break;
		//$FALL-THROUGH$
       default :
         super.setDefaultValue(_index-4);
         break;
    }
  }

	@Override
	public Value getValue(int _index) {
		switch (_index) {
		case MODE_TITLE:
		case MODE_SET_RESIZE:
		case MODE_EXIT:
		case MODE_ONEXIT:
			return null;
		default:
			return super.getValue(_index - 4);
		}
	}

} // End of 2class

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
