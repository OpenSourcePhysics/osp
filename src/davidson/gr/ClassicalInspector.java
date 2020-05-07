package davidson.gr;
import org.opensourcephysics.numerics.Function;
import org.opensourcephysics.frames.*;
import org.opensourcephysics.display.*;
import java.awt.*;
import javax.swing.JFrame;
import org.opensourcephysics.controls.*;
import javax.swing.event.SwingPropertyChangeSupport;
import java.beans.*;


/**
 * ClassicalInspector displays the effective potential for a classical particle acted on by an inverse square law force.
 */
public class ClassicalInspector {

   PropertyChangeSupport support = new SwingPropertyChangeSupport(this);
   PlotFrame plot = new PlotFrame("r", "U(r)", "Newtonian Effective Potential");
   FunctionDrawer functionDrawer = new FunctionDrawer(new EffectivePotential());
   OSPControlTable controlTable = new OSPControlTable();
   double E = -0.5, L = 0.5, r = 1.0, M = 1;
   AbstractSimulation simulation;
   Mass mass = new Mass();
   EnergyLine energyLine = new EnergyLine();
   volatile boolean newInspectorData = false;  //set when a user changes data within the inspector using a mouse or keyboard action


   /**
    * Creates an inspector for the given model.
    *
    * @param app ClassicalApp
    */
   public ClassicalInspector(AbstractSimulation app, ClassicalTrajectory trajectory) {
      double drdt = 0, dphidt = 0, phi = 0;
      mass.color=Color.MAGENTA;
      if(app!=null) {
         simulation = app;
         // state values = {r, dr/dt, phi, dphi/dt, t}
         r = trajectory.state[0];
         drdt = trajectory.state[1];
         phi = trajectory.state[2];
         phi = (Math.PI+trajectory.state[2])%(2*Math.PI)-Math.PI;
         dphidt = trajectory.state[3];
         L = r*r*trajectory.state[3];
         E = trajectory.state[1]*trajectory.state[1]/2+L*L/2/r/r-M/r;
      }
      controlTable.setPreferredSize(new Dimension(100, 100));
      controlTable.setDecimalFormat("#0.0000");
      controlTable.setEditable(false);
      controlTable.addControlListener("propertyChange", this);
      controlTable.setValue("r", r);
      controlTable.setValue(TeXParser.parseTeX("$\\phi$"), phi);
      controlTable.setValue("E", E);
      controlTable.setValue("L", L);
      controlTable.setValue("dr/dt", drdt);
      controlTable.setValue(ClassicalApp.PHIRATE, dphidt);
      plot.getDrawingPanel().getCoordinateStringBuilder().setCoordinateLabels("r=", " E=");
      plot.setJMenuBar(null);
      plot.addDrawable(functionDrawer);
      plot.addDrawable(energyLine);
      plot.addDrawable(mass);
      setScale();
      plot.setSize(500, 300);
      controlTable.setBackground(plot.getDrawingPanel().getBackground());
      // TableColumn col=table.getColumn("Name");
      // col.setMaxWidth(40);
      plot.getContentPane().add(controlTable, BorderLayout.WEST);
      //plot.setVisible(true);
   }

   void enableInteraction(boolean enabled) {
      //controlTable.setEditable(enabled);
      energyLine.setEnabled(enabled);
      mass.setEnabled(enabled);
   }

   /**
    * Adds a PropertyChangeListener.
    *
    * @param listener the object requesting property change notification
    */
   public void addPropertyChangeListener(PropertyChangeListener listener) {
      support.addPropertyChangeListener(listener);
   }

   void setScale() {
      if((r<=Float.MIN_VALUE)||Double.isNaN(r)) {
         r = Float.MIN_VALUE;
      }
      double ymin = (L==0) ? -100 : (-M*M)/2/L/L;
      ymin = Math.min(ymin, 1.1*E);
      double ymax = -2*ymin;
      ymax = Math.max(ymax, 1.1*E);
      double disc = M*M/E/E+4*L*L/2/E;
      if(E==0) {
         r = Math.max(r, L*L/2/M);
      } else if(disc<=0) {
         r = (L*L)/M;
      } else if(E>=0) {
         r = Math.max(r, ((-M/E)+Math.sqrt(disc))/2);
      } else {
         double root1 = ((-M/E)-Math.sqrt(disc))/2.0;
         double root2 = ((-M/E)+Math.sqrt(disc))/2.0;
         if(r<root1) {
            r = root1;
         } else if(r>root2) {
            r = root2;
         }
      }
      plot.setPreferredMinMax(0, Math.max(4, 1.1*r), ymin, ymax);
      double drdt = Math.sqrt(Math.max(0, (2*E+2*M/r-L*L/r/r)));
      functionDrawer.functionChanged = true;
      controlTable.setValue("r", r);
      controlTable.setValue("dr/dt", drdt);
      controlTable.setValue(ClassicalApp.PHIRATE, L/r/r);
      controlTable.refresh();
      plot.repaint();
   }

   /**
    * Updates the inspector's data and scales the radius scale.
    * @param trajectory TrajectoryClassical
    */
   void updateDataAndScale(ClassicalTrajectory trajectory) {
      if(trajectory==null) {
         return;
      }
      // state values = {r, dr/dt, phi, dphi/dt, t}
      r = trajectory.state[0];
      double phi = (Math.PI+trajectory.state[2])%(2*Math.PI)-Math.PI;
      L = r*r*trajectory.state[3];
      E = trajectory.state[1]*trajectory.state[1]/2+L*L/2/r/r-M/r;
      controlTable.setValue(TeXParser.parseTeX("$\\phi$"), phi);
      controlTable.setValue("E", E);
      controlTable.setValue("L", L);
      setScale(); // sets dr/dt and dphi/dt and updates table and plot
   }

   /**
    * Updates the inspector's data by reading the model's state.
    */
   void updateData(ClassicalTrajectory trajectory) {
      if(trajectory==null) {
         return;
      }
      // state values = {r, dr/dt, phi, dphi/dt, t}
      r = trajectory.state[0];
      double drdt = trajectory.state[1];
      double phi = (Math.PI+trajectory.state[2])%(2*Math.PI)-Math.PI;
      double dphidt = trajectory.state[3];
      controlTable.setValue(TeXParser.parseTeX("$\\phi$"), phi);
      controlTable.setValue("r", r);
      controlTable.setValue("dr/dt", drdt);
      controlTable.setValue(ClassicalApp.PHIRATE, dphidt);
      // L and E do not change
      controlTable.refresh();
      plot.repaint();
   }

   /**
    * Initializes the given particle using the inspector's data.
    */
   protected void initializeParticle(ClassicalTrajectory particle) {
     if (particle == null) {
       return;
     }
     newInspectorData = false;
     double[] state = particle.state;
     state[0] = r;
     state[1] = controlTable.getDouble("dr/dt");
     state[3] = controlTable.getDouble(Trajectory.PHIRATE);
     particle.initialize(state);
   }


   public void propertyChange(String par) {
      //System.out.println("property: " + controlTable.getDouble(par));
      if(par.equals("L")) {                         // E stays constant if E>= V_min
         L = controlTable.getDouble("L");
         if(controlTable.inputError("L")) {
            return;
         }
         if(E<-M*M/2/L/L) {
            E = -M*M/2/L/L;
            controlTable.setValue("E", E);
         }
      } else if(par.equals("E")) {                  // L stays constant
         E = controlTable.getDouble("E");
         if(controlTable.inputError("E")) {
            return;
         }
         if(E<-M*M/2/L/L) {
            E = -M*M/2/L/L;
            controlTable.setValue("E", E);
         }
      } else if(par.equals("dr/dt")) {
         double drdt = controlTable.getDouble("dr/dt");
         if(controlTable.inputError("dr/dt")) {
            return;
         }
         E = drdt*drdt/2+L*L/2/r/r-M/r;
         E = Math.max(E, (-M*M)/2/L/L);
         controlTable.setValue("E", E);
      } else if(par.equals(ClassicalApp.PHIRATE)) { // d phi /dt
         double dphidt = controlTable.getDouble(ClassicalApp.PHIRATE);
         if(controlTable.inputError(ClassicalApp.PHIRATE)) {
            return;
         }
         L = r*r*dphidt;
         double newE = (L==0) ? E : Math.max(E, (-M*M)/2/L/L);
         if(newE!=E) {
            E = newE;
            controlTable.setValue("E", E);
         }
         controlTable.setValue("L", L);
      } else if(par.equals("r")) {
         r = controlTable.getDouble("r");
         if(controlTable.inputError("r")) {
            return;
         }
         if(r<Float.MIN_VALUE) {                    // don't let r get too close to singularity
            r = Float.MIN_VALUE;
            controlTable.setValue("r", r);
         }
      }
      setScale();
      newInspectorData = true;
      support.firePropertyChange("inspectorChange", null, null);
   }

   class EffectivePotential implements Function {

      @Override
	public double evaluate(double r) {
         return(r==0) ? 0 : ((-M/r)+((L*L)/(2*r*r)));
      }
   }

   class Mass extends InteractiveCircle {

      @Override
	public void draw(DrawingPanel panel, Graphics g) {
         x = r;
         y = E;
         super.draw(panel, g);
      }

      @Override
	public void setXY(double rr, double pot) {
         E = (L==0) ? pot : Math.max(pot, (-M*M)/2/L/L);
         controlTable.setValue("E", E);
         r = Math.max(rr, Float.MIN_VALUE);
         controlTable.setValue("r", r);
         setScale();
         newInspectorData = true;
         support.firePropertyChange("inspectorChange", null, null);
      }
   }

   class EnergyLine extends AbstractInteractive {

      @Override
	public void draw(DrawingPanel panel, Graphics g) {
         g.setColor(Color.RED);
         int ypix = panel.yToPix(E);
         g.drawLine(0, ypix, panel.getWidth(), ypix);
      }

      @Override
	public void setXY(double r, double pot) {
         E = Math.max(pot, (-M*M)/2/L/L);
         controlTable.setValue("E", E);
         setScale();
         newInspectorData = true;
         support.firePropertyChange("inspectorChange", null, null);
      }

      @Override
	public boolean isInside(DrawingPanel panel, int xpix, int ypix) {
         int pix = panel.yToPix(E);
         if(Math.abs(ypix-pix)<3) {
            return true;
         }
         return false;
      }
   }

   public void show() {
      plot.setVisible(true);
      plot.toFront();
   }

   public static void main(String[] args) {
      ClassicalInspector app = new ClassicalInspector(null, null);
      app.controlTable.setEditable(true);
      app.plot.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      app.show();
   }
}
