/*
 * The org.opensourcephysics.controls package defines the framework for building
 * user interface controls for the book Simulations in Physics.
 * Copyright (c) 2005  H. Gould, J. Tobochnik, and W. Christian.
 */
package debugging.applets;
import org.opensourcephysics.controls.*;

/**
 * AbstractEmbeddableAnimation is a template for embeddable animations.
 *
 * Implement the doStep method to create an animation.  This method is called from the run method and when
 * the stepAnimation button is pressed.
 *
 * @author       Wolfgang Christian
 * @version 1.0
 */
public abstract class AbstractEmbeddableSimulation  extends AbstractSimulation implements Embeddable {

  protected ObjectManager objectManager = new ObjectManager();
  protected double timeMax=Float.MAX_VALUE;
  protected String timeMsg="Done";

  @Override
public Control getControl() {
    return control;
  }

  @Override
public ObjectManager getManager() {
    return objectManager;
  }

  /**
 * Sets the Control for this model and initializes the control's values.
 *
 * @param control
 */
@Override
public void setControl (Control control) {
  if(this.control!=null)stopSimulation();
  super.setControl(control);
}

/**
 *  Sets the maximum animation time and the display message when this time is reached.
 *
 * @param time double
 * @param msg String
 */
public void setMaxTime(double time, String msg){
  timeMax=time;
  timeMsg=msg;
}



  /**
   * Starts and stops the animation thread.
   */
  public synchronized void startStop() {
    if(animationThread==null) startSimulation();
      else stopSimulation();
  }


}
