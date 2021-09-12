
package davidson.qm;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.opensourcephysics.controls.AnimationControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.display.GUIUtils;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.ejs.control.EjsControlFrame;

public class QMSuperpositionWignerWRApp extends QMSuperpositionWignerApp {
	public static Container frame = null;
	 
  public QMSuperpositionWignerWRApp() {
    showDataPanelTime = false;
  }

  /**
   * Switch to the App user interface.
   */
  @Override
public void switchGUI() {
    stopAnimation();
    Runnable runner = new Runnable() {
      @Override
	public synchronized void run() {
        OSPRuntime.disableAllDrawing = true;
        EjsControlFrame ejsFrame = ((EjsControlFrame) control);
        control.setValue("time", 0);
        XMLControlElement xml = new XMLControlElement(ejsFrame.getOSPApp());
        WindowListener[] listeners = ejsFrame.getMainFrame().getWindowListeners();
        int closeOperation = ejsFrame.getMainFrame().getDefaultCloseOperation();
        ejsFrame.getMainFrame().setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        ejsFrame.getMainFrame().setKeepHidden(true);
        ejsFrame.getMainFrame().dispose();
        QMSuperpositionWignerApp app = new QMSuperpositionWignerApp();
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
        app.wigner.wignerFrame.setVisible(true);
        GUIUtils.repaintOSPFrames();
      }

    };
    Thread t = new Thread(runner);
    t.start();
  }

  @Override
void customize() {
	QMWignerControl c = (QMWignerControl) control;
    JMenu menu = c.getMainFrame().getMenu("Display");
    JMenuItem item = new JMenuItem("Switch GUI");
    item.addActionListener(new ActionListener() {
      @Override
	public void actionPerformed(ActionEvent e) {
        switchGUI();
      }

    });
    menu.add(item);
    c.addChildFrame(dataFrame);
    c.addChildFrame(psiFrame);
    JMenu jm= wigner.wignerFrame.getMenu("Views");
    c.getMainFrame().getJMenuBar().add(jm);
    c.getMainFrame().setTitle("Wigner Function");
    getMainFrame().doLayout();
    getMainFrame().setSize(400, 500);
  }

  public void setTime() {
    time = control.getDouble("time");
    superposition.update(time);
    superposition.getPsi(psiDataset);
    dataPanel.render();
    wigner.time = time;
    wigner.doStep(superposition);
    GUIUtils.repaintOSPFrames();
  }

  @Override
public void doStep() {
    super.doStep();
    control.setValue("time", time);
  }

	/**
	 * Starts the program and loads an optional XML file.
	 * 
	 * @param args String[]
	 */
	public static void main(String[] args) {
		int val = 1;
		if (args == null || args.length == 0) {
			// BH 2020.03.04 "_dav" here to indicate to use davison, not
			// org.opersourcephysics.davidson
			switch (val) {
			// default:
			case 1:
				args = new String[] { "wigner/isw_wigner_p0_10pi.xml" };
				break;
			case 2:
				args = new String[] { "wigner/sho_wigner_dav.xml" };
				break;
			case 3:
				args = new String[] { "wigner/isw_wigner_5_dav.xml" };
				break;
			}

		}

		OSPRuntime.disableAllDrawing = true;
		QMSuperpositionWignerWRApp app = new QMSuperpositionWignerWRApp();
		QMWignerControl c = new QMWignerControl(app, args);
		frame = c.cont;
		app.customize();
		if (args == null || args.length == 0) {
			c.loadXML("/davidson/qm/wigner_default.xml");
		}
		OSPRuntime.disableAllDrawing = false;
		GUIUtils.repaintOSPFrames();
	}

}
