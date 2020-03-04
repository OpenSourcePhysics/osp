
package davidson.qm;
import org.opensourcephysics.ejs.control.EjsControlFrame;
import org.opensourcephysics.controls.XMLControlElement;
import javax.swing.JFrame;
import org.opensourcephysics.controls.AnimationControl;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JPanel;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.opensourcephysics.display.ComplexDataset;
import org.opensourcephysics.display.GUIUtils;
import org.opensourcephysics.display.OSPFrame;
import org.opensourcephysics.display.OSPRuntime;

import java.awt.event.WindowListener;

public class QMSuperpositionWRApp extends QMSuperpositionApp {
  public void resetAnimation() {
    super.resetAnimation();
    if(control instanceof EjsControlFrame) {
      ((EjsControlFrame) control).loadDefaultXML();
    }
    initializeAnimation();
    if(!(control instanceof EjsControlFrame))return;
    if(control.getString("style").toLowerCase().equals("reim")) {
      ((EjsControlFrame) control).getControl("checkBox").setProperty("selected", "false");
    } else {
      ((EjsControlFrame) control).getControl("checkBox").setProperty("selected", "true");
    }
  }

  /**
   * Switch to the WRApp user interface.
   */
  public void switchGUI() {
    stopAnimation();
    Runnable runner = new Runnable() {
      public synchronized void run() {
        OSPRuntime.disableAllDrawing = true;
        EjsControlFrame ejsFrame = ((EjsControlFrame) control);
        XMLControlElement xml = new XMLControlElement(ejsFrame.getOSPApp());
        WindowListener[] listeners = ejsFrame.getMainFrame().getWindowListeners();
        int closeOperation = ejsFrame.getMainFrame().getDefaultCloseOperation();
        ejsFrame.getMainFrame().setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        ejsFrame.getMainFrame().setKeepHidden(true);
        ejsFrame.getMainFrame().dispose();
        QMSuperpositionApp app = new QMSuperpositionApp();
        AnimationControl c = AnimationControl.createApp(app);
        c.setDefaultCloseOperation(closeOperation);
        for(int i = 0, n = listeners.length; i<n; i++) {
          if(listeners[i].getClass().getName().equals("org.opensourcephysics.tools.Launcher$FrameCloser")) {
            c.addWindowListener(listeners[i]);
          }
        }
        c.loadXML(xml,true);
        app.customize();
        System.gc();
        OSPRuntime.disableAllDrawing = false;
        GUIUtils.repaintOSPFrames();
      }

    };
    Thread t = new Thread(runner);
    t.start();
  }

  void customize() {
    QMSuperpositionControl c = (QMSuperpositionControl) control;
    JMenu menu = c.getMainFrame().getMenu("Display");
    JMenuItem item = new JMenuItem("Switch GUI");
    menu.add(item);
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        switchGUI();
      }
    });
  }

  public void changeOn() {
    double dy = control.getDouble("psi range");
    psiDataset.setCentered(centeredPhase);
    if(centeredPhase) {
      psiPanel.limitAutoscaleY(-dy/2, dy/2);
      control.setValue("style", "phase");
    } else {
      psiPanel.limitAutoscaleY(0, dy);
      control.setValue("style", "ampwithphase");
    }
    psiDataset.setMarkerShape(ComplexDataset.PHASE_CURVE);
    psiPanel.setYLabel("|Psi|");
    psiPanel.repaint();
  }

  public void changeOff() {
    psiDataset.setCentered(true);
    psiDataset.setMarkerShape(ComplexDataset.RE_IM_CURVE);
    control.setValue("style", "reim");
    psiPanel.setYLabel("Re(Psi) & Im(Psi)");
    double dy = control.getDouble("psi range");
    psiPanel.limitAutoscaleY(-dy, dy);
    psiPanel.repaint();
  }

  /**
   * Starts the program and loads an optional arg[0] XML file.
   * @param args String[]
   */
  public static void main(String[] args) {
    final QMSuperpositionWRApp app = new QMSuperpositionWRApp();
    new QMSuperpositionStyleControl(app, args);
    app.customize();
  }
  
	/**
	 * Proposed method for EJS models.
	 * @param xmlData  Data file that is passed to the model.
	 * @return
	 */
  public static JComponent getModelPane(String[] args, JFrame parent){
		QMSuperpositionWRApp model=new QMSuperpositionWRApp();
		OSPFrame frame= model.getMainFrame();
		JComponent pane=(JComponent)frame.getContentPane();
		 frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		 frame.setContentPane(new JPanel());
		 frame.setVisible(false);
		 pane.setPreferredSize(new Dimension(300,300));
		 return pane;
	}

}

class QMSuperpositionStyleControl extends QMSuperpositionControl {
  QMSuperpositionStyleControl(QMSuperpositionWRApp model, String[] args) {
    super(model, args);
  }

  protected void customize() {
    add("Panel", "name=checkPanel;parent=controlPanel;position=east;layout=flow:left,0,0");
    add("Label",
      "position=west; parent=checkPanel;text= Phase Color = ;horizontalAlignment=right;tooltip=Change wave function representation.");
    add("CheckBox",
      "parent=checkPanel;name=checkBox;actionon=model.changeOn();actionoff=model.changeOff();selected=true;tooltip=Change wave function representation.");
  }

}

