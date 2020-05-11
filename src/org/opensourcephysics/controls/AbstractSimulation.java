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
 * For additional information and documentation on Open Source Physics,
 * please see <http://www.opensourcephysics.org/>.
 */

package org.opensourcephysics.controls;
import java.awt.Frame;
import java.util.Collection;

import org.opensourcephysics.display.GUIUtils;

import javajs.async.SwingJSUtils;
import javajs.async.SwingJSUtils.StateHelper;
import javajs.async.SwingJSUtils.StateMachine;

/**
 * AbstractSimulation is a template for SIP simulations.
 *
 * AbstractSimulation creates and manages an animation thread that
 * invokes the abstract "doStep()" method every 1/10 second.  The doStep method is also called when
 * the stepAnimation button is pressed.
 *
 * Implement the doStep method to create a concrete simulation.
 *
 * @author       Wolfgang Christian
 * @version 1.0
 */
abstract public class AbstractSimulation extends AbstractAnimation implements Simulation, StateMachine {
  protected SimControl control; // shadows superclass field
  protected boolean showStepsPerDisplay = false;
  protected int stepsPerDisplay = 1;
  protected int stepCounter = 0;

  /**
   * Sets the Control for this model and initializes the control's values.
   *
   * @param control
   */
  @Override
public void setControl(Control control) {
    if(control instanceof SimControl) {
      this.control = (SimControl) control;
    } else {
      this.control = new ShadowControl(control);
    }
    super.control = control;
    mainFrame = null;
    if(control!=null) {
      if(control instanceof MainFrame) {
        mainFrame = ((MainFrame) control).getMainFrame();
      }
      control.setLockValues(true);
      resetAnimation();
      control.setLockValues(false);
      if(control instanceof Frame) {
        ((Frame) control).pack();
      }
    }
  }

  /**
   * Gets this simulation's control.
   * @return Control
   */
  @Override
public Control getControl() {
    return control;
  }

  /**
   * Performs an action before executing one or more animation steps.
   */
  public void startRunning() {}

  /**
   * Performs an action after executing one or more animation steps.
   */
  public void stopRunning() {}

  /**
   * Invokes the simulation's start method and then starts the animation thread.
   * Simulations should not override this method.  Override the start method to perform
   * custom actions just before a thread starts.
   *
   * @deprecated
   */
  @Deprecated
@Override
public void startAnimation() {
    if(showStepsPerDisplay) {
      stepsPerDisplay = control.getInt("steps per display"); //$NON-NLS-1$
    }
    start();
    startRunning();
    super.startAnimation();
  }

  /**
   * Starts the simulation thread.  Unlike the startAnimation method cannot be overridden so it
   * is not deprecated.
   */
  @Override
final public void startSimulation() {
    startAnimation();
  }

  /**
   * Starts the simulation.
   *
   * Override this method to perform custom actions before the animation thread begins running.
   */
  public void start() {}

  /**
   * Stops the animation thread and then invokes the simulations stop method.\
   * Simulations should not override this method.  They should override the stop method.
   *
   * @deprecated
   */
  @Deprecated
@Override
public void stopAnimation() {
    super.stopAnimation();
    stopRunning();
    stop();
  }

  /**
   * Stops the simulation thread.  This method cannot be overridden so it
   * is not deprecated.
   */
  @Override
final public void stopSimulation() {
    stopAnimation();
  }

  /**
   * Stops the simulation.
   *
   * Override this method to perform custom actions after the animation thread stops running.
   */
  public void stop() {}

  /**
   * Steps the simulation.
   *
   * This method is final in order to insure that all AbsractSimulations invoke startRunning(), doStep(), stepCounter++
   * and stopRunning() in the correct order.
   *
   */
  @Override
public final void stepAnimation() {
    if(showStepsPerDisplay) {
      stepsPerDisplay = control.getInt("steps per display"); //$NON-NLS-1$
    }
    startRunning();
    super.stepAnimation();
    stepCounter++;
    stopRunning();
    org.opensourcephysics.display.GUIUtils.repaintAnimatedFrames();
  }

  /**
   * Initializes the animation.
   * Simulations should invoke the initialize method.
   *
   * @deprecated
   */
  @Deprecated
@Override
public void initializeAnimation() {
    if(control==null) {
      return; // control can be null in applet mode so check for this
    }
    super.initializeAnimation();
    initialize();
    stepCounter = 0;
  }

  /**
   * Gets number of animation steps that have been performed since the last initializeAnimation.
   *
   * @return stepCounter
   */
  public int getStepCounter() {
    return stepCounter;
  }

  /**
   * Initializes the simulation.
   *
   * Override this method to initialize a concrete simulation.
   */
  public void initialize() {}

  /**
   * Resets the animation to its default condition.
   * Simulations should invoke the reset method.
   *
   * @deprecated
   */
  @Deprecated
@Override
public void resetAnimation() {
    if(control==null) {
      return; // control can be null in applet mode so check for this
    }
    super.resetAnimation();
    stepsPerDisplay = 1;
    if(showStepsPerDisplay) {
      control.setAdjustableValue("steps per display", stepsPerDisplay); //$NON-NLS-1$
    }
    reset();
  }

  /**
   * Enables the steps per display variable in the control;
   * @param enable boolean
   */
  public void enableStepsPerDisplay(boolean enable) {
    showStepsPerDisplay = enable;
    if(showStepsPerDisplay) {
      control.setAdjustableValue("steps per display", stepsPerDisplay); //$NON-NLS-1$
    } else {
      control.removeParameter("steps per display");                     //$NON-NLS-1$
    }
  }

  /**
   * Sets the number of animation steps before animated drawing panels are rendered.
   *
   * The default steps per animation is 1.  Increase this number if frequent rendering
   * causes sluggish behavior.
   *
   * @param num int
   */
  public void setStepsPerDisplay(int num) {
    stepsPerDisplay = Math.max(num, 1);
    if(showStepsPerDisplay) {
      control.setAdjustableValue("steps per display", stepsPerDisplay); //$NON-NLS-1$
    }
  }

  /**
   * Gets the number of animation steps before animated drawing panels are rendered.
   *
   * @param num int
   */
  public int getStepsPerDisplay() {
    return stepsPerDisplay;
  }

  /**
   * Resets the simulation to its default state.
   *
   * Override this method to set the simulation's parameters.
   */
  public void reset() {}
	
	private StateHelper stateHelper;
	//private int delay = (/** @j2sNative 20 || */ 20);
	private final static int STATE_INIT = 0;
	private final static int STATE_LOOP = 1;
	private final static int STATE_DONE = 2;
  
	@Override
	public boolean stateLoop() {
		while (animationThread != null && !animationThread.isInterrupted() && stateHelper.isAlive()) {
			switch (stateHelper.getState()) {
			default:
			case STATE_INIT:
				GUIUtils.setAnimatedFrameIgnoreRepaint(true); // animated frames are updated by this thread so no need to repaint
				stateHelper.setState(STATE_LOOP);
				stateHelper.sleep(delayTime);
				return true;
			case STATE_LOOP:
				long currentTime = System.currentTimeMillis();
				doStep();
				int sleepTime = (int)Math.max(10, delayTime-(System.currentTimeMillis()-currentTime));
				org.opensourcephysics.display.GUIUtils.renderAnimatedFrames(); // repaints all animated frames
				stateHelper.sleep(sleepTime);
				return true;
			case STATE_DONE:
				GUIUtils.setAnimatedFrameIgnoreRepaint(false); // updated view at end of animation
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
    GUIUtils.setAnimatedFrameIgnoreRepaint(true); // animated frames are updated by this thread so no need to repaint
    long sleepTime = delayTime;
    while(animationThread==Thread.currentThread()) {
      long currentTime = System.currentTimeMillis();
      for(int i = 0; i<stepsPerDisplay; i++) {
        doStep();
        stepCounter++;
        if(animationThread!=Thread.currentThread()) {
          break;        // check for stop condition
        }
        Thread.yield(); // give other threads a chance to run if needed
      }
      org.opensourcephysics.display.GUIUtils.renderAnimatedFrames();
      // adjust the sleep time to try and achieve a constant animation rate
      // some VMs will hang if sleep time is less than 10
      sleepTime = Math.max(10, delayTime-(System.currentTimeMillis()-currentTime));
      try {
        Thread.sleep(sleepTime);
      } catch(InterruptedException ie) {}
    }
    GUIUtils.setAnimatedFrameIgnoreRepaint(false); // updated view at end of animation
    */
  }

  // Inner class that lets any control act as a SimControl.
  private class ShadowControl implements SimControl {
    Control control; // shadows AbstractSimulation field

    ShadowControl(Control control) {
      this.control = control;
    }

    @Override
	public void setAdjustableValue(String name, boolean val) {
      control.setValue(name, val);
    }

    @Override
	public void setAdjustableValue(String name, double val) {
      control.setValue(name, val);
    }

    @Override
	public void setAdjustableValue(String name, int val) {
      control.setValue(name, val);
    }

    @Override
	public void setAdjustableValue(String name, Object val) {
      control.setValue(name, val);
    }

    @Override
	public void removeParameter(String name) {
      // not implemented
    }

    @Override
	public void setLockValues(boolean lock) {
      control.setLockValues(lock);
    }

    @Override
	public void setValue(String name, Object val) {
      control.setValue(name, val);
    }

    @Override
	public void setValue(String name, double val) {
      control.setValue(name, val);
    }

    @Override
	public void setValue(String name, int val) {
      control.setValue(name, val);
    }

    @Override
	public void setValue(String name, boolean val) {
      control.setValue(name, val);
    }

    @Override
	public int getInt(String name) {
      return control.getInt(name);
    }

    @Override
	public double getDouble(String name) {
      return control.getDouble(name);
    }

    @Override
	public Object getObject(String name) {
      return control.getObject(name);
    }

    @Override
	public String getString(String name) {
      return control.getString(name);
    }

    @Override
	public boolean getBoolean(String name) {
      return control.getBoolean(name);
    }

    @Override
	public Collection<String> getPropertyNames() {
      return control.getPropertyNames();
    }

    @Override
	public Collection<String> getPropertyNamesRaw() {
      return control.getPropertyNamesRaw();
    }

    @Override
	public void println(String s) {
      control.println(s);
    }

    @Override
	public void println() {
      control.println();
    }

    @Override
	public void print(String s) {
      control.print(s);
    }

    @Override
	public void clearMessages() {
      control.clearMessages();
    }

    @Override
	public void clearValues() {
      control.clearValues();
    }

    @Override
	public void calculationDone(String message) {
      control.calculationDone(message);
    }

    @Override
	public void setParameterToFixed(String name, boolean fixed) {
      // not implemented
    }

  }

  /**
   * Returns an XML.ObjectLoader to save and load data for this object.
   *
   * @return the object loader
   */
  public static XML.ObjectLoader getLoader() {
    return new OSPSimulationLoader();
  }

  /**
   * Default XMLLoader to save and load data for Simulations.
   */
  static class OSPSimulationLoader extends XMLLoader {
    /**
     * Performs the calculate method when a Calculation is loaded.
     *
     * @param control the control
     * @param obj the object
     */
    @Override
	public Object loadObject(XMLControl control, Object obj) {
      ((Simulation) obj).initializeAnimation();
      return obj;
    }

  }

}
/*
 * Copyright (c) 2017  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */

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
