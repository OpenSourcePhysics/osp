package davidson.gr;
import org.opensourcephysics.display.InteractiveCircle;
import org.opensourcephysics.numerics.ODEAdaptiveSolver;
import org.opensourcephysics.display.Trail;
import org.opensourcephysics.display.DrawingPanel;
import java.awt.Graphics;
import java.awt.Color;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.OSPTableInspector;
import java.util.ArrayList;
import java.text.DecimalFormat;
import org.opensourcephysics.display.InteractiveLabel;
import org.opensourcephysics.display.Interactive;

public abstract class AbstractTrajectory extends InteractiveCircle implements Trajectory {
   static int nextID=0;
   static final String tauStr = "$\\tau$=";
   public int id=0;
   public double state[];    // break encapsulation for ease of use and speed
   public double M = 1.0;
   public double minR=0;
   public String label;
   public int trackPoints = 0;
   public double tau = 0; //  proper time
   public double tol = 1.0e-5;
   public double[] stateInitial;
   public ODEAdaptiveSolver ode_solver;
   public boolean showTau = false;
   public Trail trail = new Trail();
   public boolean showTrail = true;
   public boolean highlight = false;
   public boolean dragPosition = true;
   public Color highlightColor = Color.PINK.darker();
   public ArrayList<String> colNames = new ArrayList<>();
   public DecimalFormat f = new DecimalFormat("#0.00");
   public InteractiveLabel tauBox = new InteractiveLabel(tauStr+f.format(0));
   public ArrayList<AbstractTrajectory> cluster=new ArrayList<>();

   public AbstractTrajectory(){
     id=nextID;
     nextID++;
     cluster.add(this);
     tauBox.setConnectionPoint(InteractiveLabel.CENTER_LOCATION);
     tauBox.setOffsetY(-16-this.pixRadius);

   }
   /**
    * Sets the proper time.
    * @param tau double
    */
   public void setTau(double tau){
     this.tau=tau;
   }

   /**
    * Sets the proper time.
    * @param tau double
    */
   public double getTau() {
     return tau;
   }


   /**
    * Sets an id that can be used to identify the object.
    *
    * @param id int
    */
   public void setId(int id){
     this.id=id;
   }

   /**
    * Gets the object's id.
    * @return int
    */
   public int getId(){
     return id;
   }

   /**
    * Gets the cluster to which this trajectory belongs.
    * @return ArrayList
    */
   public ArrayList<AbstractTrajectory> getCluster(){
     return cluster;
   }

   /**
    * Sets the cluster to which this trajectory belongs.
    * @return ArrayList
    */
   public void setCluster(ArrayList<AbstractTrajectory> cluster) {
      this.cluster=cluster;
   }


   /**
    * Initializes the state array.
    * @param initialState double[]
    */
   @Override
public void initialize(double[] initialState) {
      if((state==null)||(state.length!=initialState.length)) { // state arrays should be allocated in constructor.
         throw(new IllegalArgumentException());
      }
      if(initialState!=state) {
         System.arraycopy(initialState, 0, state, 0, initialState.length);
      }
      System.arraycopy(initialState, 0, stateInitial, 0, initialState.length);
      trail.clear();
      computeTrail();
      setMeasured(true);
   }

   /**
    * Resets the trajectory to its initial state.
    */
   @Override
public void resetInitial() {
      initialize(stateInitial);
   }

   /**
    * Sets the x and y coordinates.
    *
    * @param x
    * @param y
   */
   @Override
public void setXY(double x, double y) {
     double r = Math.sqrt(x * x + y * y);
     if (r==0 && minR>0) {
       super.setXY(minR, 0);
     } else if (r < minR) {
       super.setXY(x * minR / r, y * minR / r);
     } else {
       super.setXY(x, y);
     }
   }

   public void setMinR(double r){
     minR=r;
   }


   /**
    * Appends the current state value to a datatable.
    */
   public void appendDataToTable() {}

   public int getErrorCode() {
      return ode_solver.getErrorCode();
   }

   /**
    * Computes a trail whenever particle moves.
    */
   protected void computeTrail() {
      trail.clear();           // clears old trail
      trail.moveToPoint(x, y); // add first point to trail
      if(trackPoints>0) {
         for(int i = trackPoints; i>0; i--) {
            stepTime();
         }
         System.arraycopy(stateInitial, 0, state, 0, state.length);
         x = state[0]*Math.cos(state[2]);
         y = state[0]*Math.sin(state[2]);
         trail.moveToPoint(x, y);
      }
   }

   /**
    * Sets the initial step size.
    *
    * @param dt
    */
   public void setStepSize(double dt) {
      ode_solver.setStepSize(dt);
   }

   /**
    * Sets the solution tolerance.
    * @param tol double
    */
   @Override
public void setTolerance(double tol) {
      this.tol = tol;
   }

   @Override
public abstract void stepTime();

   @Override
public double[] getState() {
      return state;
   }

   public double[] getInitialState() {
      return stateInitial;
   }

   protected abstract OSPTableInspector edit();

   @Override
public abstract void getRate(double[] state, double[] rate);

   /**
    * Sets the highlight property.
    * Highlighted particles are drawn slightly larger.
    * @param b boolean
    */
   public void setHighlight(boolean b) {
      highlight = b;
   }

   /**
    * Enables the drag position flag.
    *
    * @param drag boolean
    */
   public void setEnableDrag(boolean drag){
     dragPosition=drag;
   }

   /**
    * Gets the drag postion flag.
    *
    * @return boolean
    */
   public boolean isEnableDrag() {
     return dragPosition;
   }


   /**
    * Gets the highlight property.
    * @return boolean
    */
   public boolean isHighlight() {
      return highlight;
   }

   @Override
public Interactive findInteractive(DrawingPanel panel, int xpix, int ypix) {
     if(dragPosition){
       Interactive interactive = super.findInteractive(panel, xpix, ypix);
       if (interactive != null)return interactive;
     }
     return tauBox.findInteractive(panel,  xpix, ypix);
   }


   /**
    * Draws the circle and the trail.
    *
    * @param panel
    * @param g
    */
   @Override
public void draw(DrawingPanel panel, Graphics g) {
      if(showTrail) {
         trail.draw(panel, g);
      }
      if(highlight) {
         int xpix = panel.xToPix(x)-pixRadius;
         int ypix = panel.yToPix(y)-pixRadius;
         g.setColor(highlightColor);
         g.fillOval(xpix-2, ypix-2, 2*pixRadius+4, 2*pixRadius+4);
         g.setColor(color);
         g.fillOval(xpix, ypix, 2*pixRadius, 2*pixRadius); // draw the circle onto the screen
      } else {
         super.draw(panel, g);
      }
      paintLabel(panel, g);
      paintTau(panel, g);
   }

   /**
    * Paints a text label near the trajectory.
    * @param panel DrawingPanel
    * @param g Graphics
    */
   protected void paintLabel(DrawingPanel panel, Graphics g) {
      if((label!=null)&&!label.trim().equals("")) {
         g.setColor(Color.BLACK);
         int xpix = panel.xToPix(x)-g.getFontMetrics(g.getFont()).stringWidth(label)/2;
         int ypix = (showTau)?
             panel.yToPix(y)-pixRadius-4:
             panel.yToPix(y)+pixRadius+g.getFontMetrics(g.getFont()).getHeight()+1;
         g.drawString(label, xpix, ypix);
      }
   }

   /**
    * Sets the show tau property.
    * @param show boolean
    */
   public void setShowTau(boolean show) {
      showTau = show;
   }

   /**
    * Sets the show trail property.
    * @param show boolean
    */
   public void setShowTrail(boolean show) {
     showTrail = show;
   }


   /**
    * Paints the proper time.
    *
    * @param panel DrawingPanel
    * @param g Graphics
    */
   protected void paintTau(DrawingPanel panel, Graphics g) {
      if(!showTau) {
         return;
      }
      tauBox.setText(tauStr+f.format(tau), x, y);
      tauBox.draw(panel, g);
   }

   /**
    * Gets the minimum x needed to draw this object.
    * @return minimum
    */
   @Override
public double getXMin() {
      return Math.min(super.getXMin(), trail.getXMin());
   }

   /**
    * Gets the maximum x needed to draw this object.
    * @return maximum
    */
   @Override
public double getXMax() {
      return Math.max(super.getXMax(), trail.getXMax());
   }

   /**
    * Gets the minimum y needed to draw this object.
    * @return minimum
    */
   @Override
public double getYMin() {
      return Math.min(super.getYMin(), trail.getYMin());
   }

   /**
    * Gets the maximum y needed to draw this object.
    * @return minimum
    */
   @Override
public double getYMax() {
      return Math.max(super.getYMax(), trail.getYMax());
   }

   public static XML.ObjectLoader getLoader() {
      return new AbstractTrajectoryLoader();
   }

   /**
    * A class to save and load InteractiveCircle data in an XMLControl.
    */
   static class AbstractTrajectoryLoader extends InteractiveCircleLoader {

      /**
       * Saves the data in the xml control.
       * @param control XMLControl
       * @param obj Object
       */
      @Override
	public void saveObject(XMLControl control, Object obj) {
         super.saveObject(control, obj);
         AbstractTrajectory trajectory = (AbstractTrajectory) obj;
         control.setValue("M", trajectory.M);
         control.setValue("tolerance", trajectory.tol);
         control.setValue("show_trail", trajectory.showTrail);
         control.setValue("show_tau", trajectory.showTau);
         control.setValue("highlight", trajectory.highlight);
         control.setValue("track_points", trajectory.trackPoints);
         control.setValue("minimum_r", trajectory.minR);
         control.setValue("label", trajectory.label);
         control.setValue("id", trajectory.id);
         control.setValue("ode_step_size", trajectory.ode_solver.getStepSize());
         control.setValue("state", trajectory.state);
      }

      /**
       * Creates object..
       * @param control XMLControl
       * @return Object
       */
      @Override
	public Object createObject(XMLControl control) {
         return null; // cannot create an abstract object.
      }

      /**
       * Loads data from the xml control into the trajectory.
       * @param control XMLControl
       * @param obj Object
       * @return Object
       */
      @Override
	public Object loadObject(XMLControl control, Object obj) {
         super.loadObject(control, obj);
         AbstractTrajectory trajectory = (AbstractTrajectory) obj;
         trajectory.M = control.getDouble("M");
         trajectory.id = control.getInt("id");
         trajectory.tol = control.getDouble("tolerance");
         trajectory.showTau = control.getBoolean("show_tau");
         trajectory.showTrail = control.getBoolean("show_trail");
         trajectory.highlight = control.getBoolean("highlight");
         trajectory.trackPoints = control.getInt("track_points");
         trajectory.minR=control.getDouble("minimum_r");
         trajectory.label = control.getString("label");
         trajectory.ode_solver.setStepSize(control.getDouble("ode_step_size"));
         trajectory.initialize((double[]) control.getObject("state"));
         return obj;
      }
   }
}
