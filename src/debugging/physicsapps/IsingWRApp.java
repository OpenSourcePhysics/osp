/*
 * Physics proof-of-concept examples.
 *
 * Copyright (c) 2001  W. Christian.
 */
package debugging.physicsapps;


import org.opensourcephysics.controls.*;
import debugging.applets.Embeddable;
import debugging.applets.ObjectManager;


/**
 * A wrapper that allows the IsingApp to be run inside a WrapperApplet.
 * @author Wolfgang Christian
 * @version 1.0
 */
public class IsingWRApp extends IsingApp implements Embeddable{
    ObjectManager viewManager = new ObjectManager();

  /**
   * Method sliderMoved
   *
   */
  public void sliderMoved () {
    T = control.getDouble ("T");
    newH = control.getDouble ("H");
    if(newH!=H)fieldChanged=true;
  }

    /**
     * Sets the Control.
     *
     * A null value is sent by an applet's destroy method.
     *
     * @param control
     */
    @Override
	public void setControl(Control control){
        stopAnimation();  // make sure the animation is stopped.
        this.control=control;
        if(this.control==null) return;
        viewManager.addView("drawingFrame",drawingFrame);
        viewManager.addView("drawingPanel",drawingPanel);
        viewManager.addView("energyFrame",enFrame);
        viewManager.addView("energyPanel",enPanel);
        viewManager.addView("magnetizationFrame",magFrame);
        viewManager.addView("magnetizationPanel",magPanel);
        initMyControl ();  // store initial values in the control
    }

    /**
     * Gets the Control for this applet.
     */
    @Override
	public Control getControl(){
        return control;
    }

        /**
   * Gets the ObjectManager.  Implementation of Embeddable.
   *
   * @return
   */
  @Override
public ObjectManager getManager(){ return viewManager;}

  /**
   * The main entry point for the program.
   *
   * @param args
   */
  public static void main (String[] args) {
    IsingWRApp model   = new IsingWRApp ();
    Control    control = new IsingTempControl (model);
    model.setControl (control);
  }
}
