package davidson.qm;

import java.awt.event.WindowListener;

import javax.swing.JFrame;

import org.opensourcephysics.controls.*;
import org.opensourcephysics.display.GUIUtils;
import org.opensourcephysics.display.OSPRuntime;
//import org.opensourcephysics.display.PlottingPanel;
/**
 * QMSuperpositionCarpetApp displays a linear superposition of quantum eigenstates and the associated quantum carpet.
 * @version 1.0
 * @author W. Christian
 */
public class QMSuperpositionWignerApp extends QMSuperpositionApp {
   WignerISW        wigner = new WignerISW();

  public QMSuperpositionWignerApp() {
    super();
    wigner.wignerFrame.setTitle("Wigner Function");
  }

  @Override
void setValues() {
  control.setValue("numpts", 48);
  control.setValue("gutter points", 24);        // number of extra points
  control.setValue("psi range", 1.0);
  control.setValue("p range", 4);
  control.setValue("dt", 0.1);
  control.setValue("x min", "-pi");
  control.setValue("x max", "pi");
  control.setValue("re coef", intialRe);
  control.setValue("im coef", intialIm);
  control.setValue("V(x)", "well");
  control.setValue("energy scale", 1);
  control.setValue("time", "0.00");
  control.setValue("time format", "0.00");
  control.setValue("shooting tolerance", 1.0e-4);
  control.setValue("style", "phase");
  control.setValue("hide frame", false);
  control.setValue("psi title", "");
  control.setValue("data title", "");
}


  @Override
public void initializeAnimation() {
    super.initializeAnimation();
    wigner.decimalFormat=sciFormat;
    wigner.prange=control.getDouble("p range");
    wigner.time=time;
    wigner.initialize(superposition, control.getInt("gutter points"));
  }

  @Override
public void doStep() {
    super.doStep();
    if(dataPanel!=null && showDataPanelTime) {
        dataPanel.setMessage("t="+sciFormat.format(time));
    }
    wigner.time=time;
    wigner.doStep(superposition);
  }
  
  /**
   * Switch to the WRApp user interface.
   */
  @Override
public void switchGUI() {
    stopAnimation();
    Runnable runner = new Runnable() {
      @Override
	public synchronized void run() {
        OSPRuntime.disableAllDrawing=true;
        ControlFrame controlFrame = ( (ControlFrame) control);
        XMLControlElement xml = new XMLControlElement(controlFrame.getOSPApp());
        WindowListener[]  listeners= controlFrame.getWindowListeners();
        int closeOperation = controlFrame.getDefaultCloseOperation();
        controlFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        controlFrame.setKeepHidden(true);
        controlFrame.dispose();
        QMSuperpositionWignerWRApp app = new QMSuperpositionWignerWRApp();
        QMWignerControl c = new QMWignerControl(app, null);
        c.getMainFrame().setDefaultCloseOperation(closeOperation);
        for (int i = 0, n = listeners.length; i < n; i++) {
          if (listeners[i].getClass().getName().equals("org.opensourcephysics.tools.Launcher$FrameCloser")) {
            c.getMainFrame().addWindowListener(listeners[i]);
          }
        }
        c.loadXML(xml,true);
        app.customize();
        System.gc();
        OSPRuntime.disableAllDrawing=false;
        GUIUtils.repaintOSPFrames();
      }
    };
    Thread t = new Thread(runner);
    t.start();
  }
  
  @Override
void customize() {
	  super.customize();
	  addChildFrame(wigner.wignerFrame);
  }


  /**
   * Starts the program and loads an optional XML file.
   * @param args String[]
   */
  public static void main(String[] args) {
	QMSuperpositionWignerApp app=new QMSuperpositionWignerApp();
    AnimationControl.createApp(app, args);
    app.customize();
  }
}
