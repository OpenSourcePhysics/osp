/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.controls;
import java.awt.Dimension;
import java.awt.Frame;
import java.text.DecimalFormat;
import java.util.Collection;

import javax.swing.JFrame;

import org.opensourcephysics.display.OSPFrame;
import org.opensourcephysics.display.OSPRuntime;

import javajs.async.SwingJSUtils;
import javajs.async.SwingJSUtils.StateHelper;
import javajs.async.SwingJSUtils.StateMachine;

/**
 * AbstractAnimation is a template for simple animations.
 *
 * Implement the doStep method to create an animation.  This method is called from the run method and when
 * the stepAnimation button is pressed.
 *
 * @author       Wolfgang Christian
 * @version 1.0
 */
public abstract class AbstractAnimation implements Animation, Runnable, StateMachine {
  protected OSPFrame mainFrame;                                        // the main frame that closed the program
  protected Control control;                                           // the model's control
  protected volatile Thread animationThread;
  protected int delayTime = 100;                                       // time between animation steps in milliseconds
  long t0 = System.currentTimeMillis();                                // system clock at start of last time step

  /** Field decimalFormat can be used to display time and other numeric values. */
  protected DecimalFormat sciFormat = org.opensourcephysics.numerics.Util.newDecimalFormat("0.00E0"); // default numeric format for messages //$NON-NLS-1$
  /** Field decimalFormat can be used to display time and other numeric values. */
  protected DecimalFormat decimalFormat = sciFormat; // default numeric format for messages 
  /**
   * Sets the Control for this model and initializes the control's values.
   *
   * @param control
   */
  @Override
public void setControl(Control control) {
    this.control = control;
    mainFrame = null;
    if(control!=null) {
      if(control instanceof MainFrame) {
        mainFrame = ((MainFrame) control).getMainFrame();
      }
      control.setLockValues(true);
      resetAnimation(); // sets the control's default values
      control.setLockValues(false);
      if(control instanceof Frame) {
        ((Frame) control).pack();
      }
    }
  }

  /**
   * Sets the preferred delay time in ms between animation steps.
   * @param delay
   */
  public void setDelayTime(int delay) {
    delayTime = delay;
  }

  /**
   * Gets the preferred delay time in ms between animation steps.
   * @return
   */
  public int getDelayTime() {
    return delayTime;
  }


  /**
   * Gets the main OSPFrame.  The main frame will usually exit program when it is closed.
   * 
   *  @j2sAlias getMainFrame
   * 
   * @return OSPFrame
   */
  public OSPFrame getMainFrame() {
    return mainFrame;
  }
  
  /**
   * Gets the Main Frame size. 
   * 
   * @j2sAlias getMainFrameSize
   * 
   */
  public int[] getMainFrameSize(){
 	 Dimension d=mainFrame.getSize();
 	 return new int[] {d.width,d.height};
  }

  /**
   * Gets the main OSPFrame.  The main frame will usually exit program when it is closed.
   * @return OSPFrame
   */
  public OSPApplication getOSPApp() {
    if(control instanceof MainFrame) {
      return((MainFrame) control).getOSPApp();
    }
    return null;
  }

  /**
   * Adds a child frame that depends on the main frame.
   * Child frames are closed when this frame is closed.
   *
   * @param frame JFrame
   */
  public void addChildFrame(JFrame frame) {
    if((mainFrame==null)||(frame==null)) {
      return;
    }
    mainFrame.addChildFrame(frame);
  }

  /**
   * Clears the child frames from the main frame.
   */
  public void clearChildFrames() {
    if(mainFrame==null) {
      return;
    }
    mainFrame.clearChildFrames();
  }

  /**
   * Gets a copy of the ChildFrames collection.
   * @return Collection
   */
  public Collection<JFrame> getChildFrames() {
    return mainFrame.getChildFrames();
  }

  /**
   * Gets the Control.
   *
   * @return the control
   */
  public Control getControl() {
    return control;
  }

  /**
   * Initializes the animation by reading parameters from the control.
   */
  @Override
public void initializeAnimation() {
    control.clearMessages();
  }

  /**
   * Does an animation step.
   */
  abstract protected void doStep();

  /**
   * Stops the animation.
   *
   * Sets animationThread to null and waits for a join with the animation thread.
   */
  @Override
public synchronized void stopAnimation() {
  	if(stateHelper!=null)stateHelper.setState(STATE_DONE);
    if(animationThread==null) { // animation thread is already dead
      return;
    }
    Thread tempThread = animationThread; // local reference
    animationThread = null; // signal the animation to stop
    if(stateHelper!=null)stateHelper.setState(STATE_DONE);
	
    if(Thread.currentThread()==tempThread) {
      return; // cannot join with own thread so return
    }         // another thread has called this method in order to stop the animation thread
    try {                     // guard against an exception in applet mode
      tempThread.interrupt(); // get out of a sleep state
      if(!OSPRuntime.isJS)tempThread.join(1000);  // wait up to 1 second for animation thread to stop
    } catch(Exception e) {
      // System.out.println("excetpion in stop animation"+e);
    }
  }

  /**
   * Determines if the animation is running.
   *
   * @return boolean
   */
  public final boolean isRunning() {
    return animationThread!=null;
  }

  /**
   * Steps the animation.
   */
  @Override
public synchronized void stepAnimation() {
    if(animationThread!=null) {
      stopAnimation();
      return;
    }
    doStep();
  }
  

  /**
   * Starts the animation.
   *
   * Use this method to start a timer or a thread.
   */
  @Override
public synchronized void startAnimation() {
    if(animationThread!=null) {
      return; // animation is running
    }
    animationThread = new Thread(this);
    
    animationThread.setPriority(Thread.NORM_PRIORITY);
    //animationThread.setPriority(Thread.MAX_PRIORITY);   // for testing
    //animationThread.setPriority(Thread.MIN_PRIORITY);   // for testing
    animationThread.setDaemon(true);
    animationThread.start(); // start the animation
  }

  /**
   * Resets the animation to a predefined state.
   */
  @Override
public void resetAnimation() {
    if(animationThread!=null) {
      stopAnimation(); // make sure animation is stopped
    }
    control.clearMessages();
  }
  
   StateHelper stateHelper;
	//private int delay = (/** @j2sNative 20 || */ 20);
	private final static int STATE_INIT = 0;
	private final static int STATE_LOOP = 1;
  final static int STATE_DONE = 2;
  
	@Override
	public boolean stateLoop() {
		while (animationThread != null && !animationThread.isInterrupted() && stateHelper.isAlive()) {
			switch (stateHelper.getState()) {
			default:
			case STATE_INIT:
				stateHelper.setState(STATE_LOOP);
				stateHelper.sleep(delayTime);
				return true;
			case STATE_LOOP:
				long currentTime = System.currentTimeMillis();
				doStep();
				int sleepTime = (int)Math.max(10, delayTime-(System.currentTimeMillis()-currentTime));
				stateHelper.sleep(sleepTime);
				return true;
			case STATE_DONE:
				return false;
			}
		}
		return false;
	}

  /**
   * Implementation of Runnable interface.  DO NOT access this method directly.
   */
  @Override
public void run() {

  	stateHelper = new SwingJSUtils.StateHelper(this);  
  	stateHelper.setState(STATE_INIT);
  	stateHelper.sleep(0);
  	/*
    long sleepTime = delayTime;
    while(animationThread==Thread.currentThread()) {
      long currentTime = System.currentTimeMillis();
      doStep();
      // adjust the sleep time to try and achieve a constant animation rate
      // some VMs will hang if sleep time is less than 10
      sleepTime = Math.max(10, delayTime-(System.currentTimeMillis()-currentTime));
      try {
        Thread.sleep(sleepTime);
      } catch(InterruptedException ie) {}
    }*/
  }

  /**
   * Returns an XML.ObjectLoader to save and load data for this object.
   *
   * @return the object loader
   */
  public static XML.ObjectLoader getLoader() {
    return new OSPAnimationLoader();
  }

  /**
   * Default XMLLoader to save and load data for Simulations.
   */
  static class OSPAnimationLoader extends XMLLoader {
    /**
     * Performs the calculate method when a Calculation is loaded.
     *
     * @param control the control
     * @param obj the object
     */
    @Override
	public Object loadObject(XMLControl control, Object obj) {
      ((Animation) obj).initializeAnimation();
      return obj;
    }

  }

}

/*
 * Open Source Physics software is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.
 *
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
