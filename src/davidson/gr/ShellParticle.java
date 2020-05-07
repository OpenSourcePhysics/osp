package davidson.gr;
import org.opensourcephysics.numerics.*;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.display.DataTableFrame;
import org.opensourcephysics.display.DataTable;
import javax.swing.JFrame;
import org.opensourcephysics.display.DatasetManager;
import java.awt.Color;
import org.opensourcephysics.controls.Control;
import org.opensourcephysics.controls.OSPTableInspector;
import java.util.Iterator;

/**
 * ShellParticle models a particle attached to a shell near a spherically symmetric gravitational field
 * using Schwarzschild coordinates.
 *
 * @author Wolfgang Christian
 * @version 1.0
 */
public class ShellParticle extends AbstractTrajectory {

   protected DataTableFrame tableFrame;
   protected DatasetManager datasets;
   protected DataTable dataTable;
   double E;
   double x0,y0;

   /**
    * Constructs a ShellParticle.
    */
   public ShellParticle() {
      this(4,0);
   }

   /**
    * Constructs a ShellParticle with the given Cartesian coordinates.
    *
    * @param x0 double
    * @param y0 double
    */
   public ShellParticle(double x0, double y0) {
     // initial state values = {t, tau}
     state = new double[] {0.0, 0.0};
     stateInitial = new double[] { 0.0, 0.0};
     ode_solver = new RK45MultiStep(this);
     pixRadius = 4;
     this.x0= x = x0;
     this.y0= y = y0;
     color = Color.BLACK;
     showTrail = false;
   }

   /**
    * Resets the trajectory to its initial state.
    */
   @Override
public void resetInitial() {
     super.resetInitial();
     x=x0;
     y=y0;
   }



   /**
    * Initializes the particle's state using the given state array.
    * The state array = {t, tau}
    *
    * @param newState state array
    *
    */
   @Override
public void initialize(double[] newState) {
      super.initialize(newState);
      if((datasets!=null)||(dataTable!=null)) {
         datasets.clear();
         dataTable.refreshTable();
      }
      E = computeE(state);
      tau=state[1];
   }

   @Override
public void setXY(double x, double y){
     double x0=this.x, y0=this.y; // save current value
     super.setXY(x,y); // set value
     double dx=this.x-x0, dy=this.y-y0;
     Iterator it = cluster.iterator();
     while (it.hasNext()) {
       AbstractTrajectory t = (AbstractTrajectory) it.next();
       if (t != this) {
         t.setX(t.getX()+dx);
         t.setY(t.getY()+dy);
       }
     }
   }

   /**
    * Displays the shell particle's initial conditions in an editor.
    * @return OSPControl
    */
   @Override
public OSPTableInspector edit() {
      OSPTableInspector inspector = new OSPTableInspector(true, true);
      Control control=inspector.getControl();
      inspector.setTitle("Shell Particle");
      inspector.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      control.setValue("r", 5.0);
      control.setValue(PHI, Math.PI/2);
      control.setValue("show tau", false);
      control.setValue("draggable state", false);
      control.setValue("label", "");
      inspector.setSize(160, 170);
      return inspector;
   }

   /**
    * Calculates the rate array using the given state.
    *
    * Values in the rate array are overwritten.
    *
    * @param state  the state
    * @param rate   the rate
    */
   @Override
public void getRate(double[] state, double[] rate) {
     double r=Math.sqrt(x*x+y*y);
      if(r<=2*M) { // too close to horizon to compute
         return;
      }
      rate[0] = 1;                    // dt/dt = 1
      rate[1] = Math.sqrt((1-2*M/r)); // dtau/dt
   }

   /**
    * Gets the energy per unit mass.
    * E is computed during initialization.
    * @return double
    */
   public double getE() {
      return E;
   }

   /**
    * Gets the energy per unit mass from the given state.
    * @return double
    */
   protected double computeE(double[] state) {
      double r = Math.sqrt(x*x+y*y);
      return Math.sqrt(1-2*M/r);
   }

   /**
    * Steps the time using an ode solver.
    */
   @Override
public void stepTime() {
      ode_solver.step();
      tau=state[1];
   }

   public void disposeDataTable() {
      if(tableFrame!=null) {
         datasets.clear();
         tableFrame.setVisible(false);
         tableFrame.dispose();
      }
   }

   public void clearDataTable() {
      if(tableFrame!=null) {
         datasets.clear();
         dataTable.refreshTable();
      }
   }

   public JFrame createDataTable(int stride) {
      datasets = new DatasetManager();
      datasets.setXPointsLinked(true); // hide x points of all datasets except 0th dataset
      datasets.setXYColumnNames(0, "t", "tau");
      dataTable = new DataTable();
      tableFrame = new DataTableFrame("Data Table", dataTable);
      dataTable.add(datasets);
      dataTable.setRowNumberVisible(false);
      datasets.append(0, state[0], state[1]); // tau
      tableFrame.setVisible(true);
      tableFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      dataTable.refreshTable();
      datasets.setStride(stride);
      return tableFrame;
   }

   public static XML.ObjectLoader getLoader() {
      return new ShellParticleLoader();
   }

   /**
    * A class to save and load InteractiveCircle data in an XMLControl.
    */
   static class ShellParticleLoader extends AbstractTrajectoryLoader {

      /**
       * Saves the data in the xml control.
       * @param control XMLControl
       * @param obj Object
       */
      @Override
	public void saveObject(XMLControl control, Object obj) {
         super.saveObject(control, obj);
         //ShellParticle trajectory = (ShellParticle) obj;
      }

      /**
       * Creates a default InteractiveCircle.
       * @param control XMLControl
       * @return Object
       */
      @Override
	public Object createObject(XMLControl control) {
         return new ShellParticle();
      }

      /**
       * Loads data from the xml control into the TrajectoryClassical object.
       * @param control XMLControl
       * @param obj Object
       * @return Object
       */
      @Override
	public Object loadObject(XMLControl control, Object obj) {
         super.loadObject(control, obj);
         //ShellParticle trajectory = (ShellParticle) obj;
         return obj;
      }
   }
}
