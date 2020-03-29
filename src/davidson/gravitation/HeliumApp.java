/*These examples are proof-of-concept only.
 *
 * Copyright (c) 2005 W. Christian.
 */

package davidson.gravitation;

import org.opensourcephysics.controls.*;
import org.opensourcephysics.display.*;

/**
 * HeliumApp models a classical Helium atom by extending AbstractAnimation and implementing the doStep method.
 *
 * @author W. Christian
 * @version 1.0
 */
public class HeliumApp extends AbstractAnimation {
  PlottingPanel plottingPanel = new PlottingPanel("x","y","Planetary Orbits");
  DrawingFrame drawingFrame = new DrawingFrame("Planet App",plottingPanel);
  Helium trajectory= new Helium();

  /**
   * Constructs HeliumApp and initializes the drawing.
   */
  public HeliumApp() {
    plottingPanel.addDrawable(trajectory);
    plottingPanel.setSquareAspect(true);
    drawingFrame.setSize(450, 450);
    drawingFrame.setVisible(true);
  }

  /**
   * Does an animation step.
   */
  protected void doStep() {
    trajectory.stepTime();
    plottingPanel.setMessage("t="+sciFormat.format(trajectory.state[4]));
    plottingPanel.repaint();
  }

  /**
   * Resets the animation into a predefined state.
   */
  public void resetAnimation() {
    super.resetAnimation();
    control.setValue("x1_init",3);
    control.setValue("vy1_init",0.4);
    control.setValue("x2_init",1);
    control.setValue("vy2_init",-1);
    control.setValue("dt",0.2);
    initializeAnimation();
  }

  /**
   * Initializes the animation;
   */
  public void initializeAnimation(){
    super.initializeAnimation();
    double x1=control.getDouble("x1_init");  // x
    double vy1=control.getDouble("vy1_init");  // vx
    double x2=control.getDouble("x2_init");  // y
    double vy2=control.getDouble("vy2_init");  // vy
    trajectory.ode_solver.setStepSize(control.getDouble("dt"));
    trajectory.initialize(x1, vy1, x2, vy2);
    plottingPanel.setPreferredMinMax( -4,4,-4,4);
    plottingPanel.repaint();
  }


  /**
   * Starts the Java application.
   * @param args  command line parameters
   */
  public static void main(String[] args) {
    AnimationControl.createApp(new HeliumApp(),args);
  }
}
