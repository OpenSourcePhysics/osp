/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package davidson.stp;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.text.NumberFormat;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.opensourcephysics.controls.*;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.display.GUIUtils;
import org.opensourcephysics.display.OSPFrame;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.frames.*;

public class Ising2dApp extends AbstractSimulation {
  Ising2d ising;
  DisplayFrame displayFrame = new DisplayFrame("Spin Configuration");
  DrawingPanel displayPanel;
  PlotFrame plotFrame = new PlotFrame("time", "E and M", "Thermodynamic Quantities");
  NumberFormat nf;
  double bondProbability;
  boolean metropolis = true;

  /**
   * Constructor Ising2DApp
   */
  public Ising2dApp() {
  	OSPRuntime.setAppClass(this);
    ising = new Ising2d();
    plotFrame.setPreferredMinMaxX(0, 10);
    plotFrame.setAutoscaleX(true);
    plotFrame.setAutoscaleY(true);
    displayFrame.addDrawable(ising);
    nf = NumberFormat.getInstance();
    nf.setMaximumFractionDigits(3);
    displayPanel = displayFrame.getDrawingPanel();
  }

  public void initialize() {
    ising.initialize(control.getInt("Length"), control.getDouble("Temperature"), control.getDouble("External field"));
    this.bondProbability = bondProbability(ising.J, ising.T);
    if(control.getString("Dynamics").equals("Metropolis")) {
      metropolis = true;
    } else {
      metropolis = false;
    }
    displayPanel.setPreferredMinMax(-5, ising.L+5, -5, ising.L+5);
    control.clearMessages();
    zeroAverages();
    stopRunning();
  }

  public double bondProbability(double J, double T) {
    return 1-Math.exp(-2*J/T);
  }

  public void doStep() {
    if(metropolis) {
      ising.doOneMCStep();
    } else {
      ising.doOneWolffStep(bondProbability);
    }
    plotFrame.append(0, ising.mcs, (double) ising.M/ising.N);
    plotFrame.append(1, ising.mcs, (double) ising.E/ising.N);
  }
  
  public void startRunning() {
	 ising.setTemperature(control.getDouble("Temperature"));
	 ising.setExternalField(control.getDouble("External field"));
  }

  public void stopRunning() {
    double norm = (ising.mcs==0)
                  ? 1
                  : 1.0/(ising.mcs*ising.N);
    control.println("mcs = "+ising.mcs);
    control.println("<E> = "+nf.format(ising.E_acc*norm));
    control.println("Specific heat = "+nf.format(ising.specificHeat()));
    control.println("<M> = "+nf.format(ising.M_acc*norm));
    control.println("<|M|>="+nf.format(Math.abs(ising.absM_acc*norm)));
    control.println("Susceptibility = "+nf.format(ising.susceptibility()));
    if(metropolis) {
      control.println("Acceptance ratio = "+nf.format(ising.acceptedMoves*norm));
    }
    control.println();
  }

  public void reset() {
    control.setValue("Length", 32);
    control.setAdjustableValue("Temperature", nf.format(Ising2d.criticalTemperature));
    control.setAdjustableValue("External field", 0);
    OSPCombo combo = new OSPCombo(new String[] {"Metropolis", "Wolff"}, 0); // second argument is default
    control.setValue("Dynamics", combo);
    enableStepsPerDisplay(true);
  }

  public void zeroAverages() {
    control.clearMessages();
    ising.resetData();
    stopRunning();
    plotFrame.clearData();
    plotFrame.repaint();
  }

  

  void customize() {
    OSPFrame f = getMainFrame();
    if((f==null)||!f.isDisplayable()) {
      return;
    }
    JMenu menu = f.getMenu("Display");
    JMenuItem item = new JMenuItem("Switch GUI");
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        //switchGUI();
      }

    });
    //menu.add(item); //not supported in stpbook
    addChildFrame(displayFrame);
    addChildFrame(plotFrame);
  }

  public static void main(String[] args) {
    Ising2dApp app = new Ising2dApp();
    SimulationControl control = SimulationControl.createApp(app, args);
    control.addButton("zeroAverages", "Zero averages");
    app.customize();
  }

}

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
 * Copyright (c) 2007  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
