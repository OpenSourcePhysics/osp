/*
 * Physics proof-of-concept examples.
 *
 * Copyright (c) 2001  W. Christian.
 */
package debugging.physicsapps;

import debugging.applets.*;
import org.opensourcephysics.ejs.control.*;
import javax.swing.border.EtchedBorder;

/**
 * An EJS control object for the IsingApp
 * @author Wolfgang Christian
 * @version 1.0
 */
public class IsingControl extends EjsControlFrame {

  /**
   * Constructor IsingControl
   *
   * @param model
   */
  public IsingControl (IsingWRApp model) {
    super (model,  "name=controlFrame;title=Control Frame;location=400,0;layout=border;exit=true; visible=false");
    add ("Panel", "name=contentPanel; parent=controlFrame; layout=border; position=center");
    java.awt.Container cont = (java.awt.Container) getElement ("contentPanel").getComponent ();
    cont.add (model.drawingPanel, java.awt.BorderLayout.CENTER);
    model.drawingFrame.setKeepHidden (true);
    model.drawingFrame.setContentPane( new EmptyPanel());

    // create a panel for the control objects
    add ("Panel", "name=controlPanel; parent=contentPanel; layout=border; position=south");
    ((javax.swing.JPanel) getElement ("controlPanel").getComponent ()).setBorder (new EtchedBorder ());
    add ("Panel", "name=sliderPanel;position=north;parent=controlPanel;layout=vbox");
    add ("Slider", "parent=sliderPanel;variable=T;minimum=0;maximum=5;format=Temperature=0.0;ticks=0;ticksFormat=0;action=sliderMoved");
    add ("Slider", "parent=sliderPanel;variable=H;minimum=0;maximum=5;format=Field=0.0;ticks=0;ticksFormat=0;action=sliderMoved");
    add ("Panel", "name=buttonPanel;position=south;parent=controlPanel;layout=flow");
    add ("Button", "parent=buttonPanel; text=Run; action=startAnimation");
    add ("Button", "parent=buttonPanel; text=Stop; action=stopAnimation");
    add ("Button", "parent=buttonPanel; text=Randomize; action=randomize");
    getControl ("controlFrame").setProperties ("size=pack");

    // set the control's views for capture by the WrapperApplet;  additional views may be set by the model.
    model.viewManager.clearViews ();
    cont = (java.awt.Container) getElement ("controlPanel").getComponent ();
    model.viewManager.addView ("controlPanel", cont);
    cont = (java.awt.Container) getElement ("contentPanel").getComponent ();
    model.viewManager.addView ("contentPanel", cont);
    cont = (java.awt.Container) getElement ("controlFrame").getComponent ();
    model.viewManager.addView ("controlFrame", cont);
    if (!org.opensourcephysics.display.OSPRuntime.appletMode) {
      cont.setVisible (true);  // make the control frame visible if we are not in applet mode
    }
  }
}
