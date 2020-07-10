package davidson.qm;
import org.opensourcephysics.controls.*;
import org.opensourcephysics.display.*;
import org.opensourcephysics.numerics.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import org.opensourcephysics.ejs.control.EjsControlFrame;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowListener;

/**
 * QMSuperpositionApp creates a linear superposition of quantum eigenstates.
 * @version 1.0
 * @author W. Christian
 */
public class QMSuperpositionApp extends AbstractAnimation implements PropertyChangeListener {

   String intialRe = "{0.707,0.707,0,0,0,0}";
   String intialIm = "{0,0,0,0,0,0}";
   String potentialStr = "x*x/2";
   Function potential;
   PlottingPanel dataPanel, psiPanel = new PlottingPanel("x", "|Psi|", "Psi(x)");
   DrawingFrame psiFrame = new DrawingFrame(psiPanel);
   OSPFrame dataFrame;
   DoubleArray recoef = new DoubleArray(intialRe);
   DoubleArray imcoef = new DoubleArray(intialIm);
   ComplexDataset psiDataset = new ComplexDataset();
   QMSuperposition superposition;
   double time = 0, dt;
   boolean showDataPanelTime=true;
   boolean centeredPhase=false;
   boolean parseError=false;
     
   public QMSuperpositionApp() {
	  
	   // BH 2020.07.09 set testApplet.app to be this class
	   // we use @j2sAlias to allow unqualified methods
	   
	  OSPRuntime.setAppClass(this);
	  
	  
      psiFrame.setTitle("QM Position Space Wave Function");
      psiPanel.limitAutoscaleY(-0.05, 0.05);
      psiPanel.addDrawable(psiDataset);
      psiFrame.setLocation(0,0);
      psiDataset.setXYColumnNames("x", "Re[$\\Psi$]", "Im[$\\Psi$]", "$\\Psi$(x,t)");
   }

   /**
    * Start or stop the animation.
    * 
    * see _embedded_example.html
    * 
    * 
    * @j2sAlias startStopAnimation
    * 
    * @param start
    */
   public void startStopAnimation(boolean start) {
	  if (start)
		  startAnimation();
	  else
		  stopAnimation();
   }
   
   @Override
public void initializeAnimation() {
      super.initializeAnimation();
      if(control.getBoolean("hide frame")) {
         psiFrame.setKeepHidden(true);
      } else {
         if(psiFrame.getDrawingPanel()!=null) {
            psiFrame.setKeepHidden(false);
         }
         //psiFrame.setVisible(true);
      }
      if(control.getObject("time")==null){
    	  time = 0;  
      }else{
    	  time = control.getDouble("time");  
      }
      if(control.getObject("psi title")==null){
    	  psiPanel.setTitle("");
      }else{
    	  psiPanel.setTitle(control.getString("psi title"));
      }
      if(control.getObject("data title")==null){
    	  if(dataPanel!=null) dataPanel.setTitle("");
      }else{
    	  if(dataPanel!=null) dataPanel.setTitle(control.getString("psi title"));
      }
      String str = control.getString("dt");
      String tformat = control.getString("time format");
      sciFormat = org.opensourcephysics.numerics.Util.newDecimalFormat(tformat); // display format for messages
      double val = Util.evalMath(str);
      if(Double.isNaN(val)) {
         control.println("Error reading dt.");
      } else {
         dt = val;
      }
      double xmin = psiPanel.getPreferredXMin();
      str = control.getString("x min");
      val = Util.evalMath(str);
      if(Double.isNaN(val)) {
         control.println("Error reading xmin.");
      } else {
         xmin = val;
      }
      double xmax = psiPanel.getPreferredXMax();
      str = control.getString("x max");
      val = Util.evalMath(str);
      if(Double.isNaN(val)) {
         control.println("Error reading xmax.");
      } else {
         xmax = val;
      }
      str = control.getString("energy scale");
      val = Util.evalMath(str);
      double energyScale = 1;
      if(Double.isNaN(val)) {
         control.println("Error reading energy scale.");
      } else {
         energyScale = val;
      }
      int numpts = control.getInt("numpts");
      DoubleArray newCoef;
      try {
         newCoef = new DoubleArray(control.getString("re coef"));
         recoef = newCoef;
      } catch(NumberFormatException ex) {
         control.println("Invalid real coefficient values.");
         control.setValue("re coef", recoef.getDefault());
      }
      try {
         newCoef = new DoubleArray(control.getString("im coef"));
         imcoef = newCoef;
      } catch(NumberFormatException ex) {
         control.println("Invalid imaginary coefficient values.");
         control.setValue("im coef", imcoef.getDefault());
      }
      parseError=false;
      if(control.getString("V(x)").trim().equals("ring")) {
         superposition = new EigenstateRingSuperposition(numpts, xmin, xmax);
      } else if(control.getString("V(x)").trim().equals("well")) {
         superposition = new EigenstateWellSuperposition(numpts, xmin, xmax);
      } else if(control.getString("V(x)").trim().equals("sho")) {
         superposition = new EigenstateSHOSuperposition(numpts, xmin, xmax);
      } else {
         try {
           potential = new ParsedFunction(control.getString("V(x)"));
        } catch(ParserException ex) {
        	parseError=true;
            control.println("Error parsing potential function. Potential set to zero.");
            potential = Util.constantFunction(0);
         }
         if(control.getObject("shooting tolerance")!=null) {
            double tol = control.getDouble("shooting tolerance");
            superposition = new EigenstateShootingSuperposition(potential, numpts, xmin, xmax, tol, tol);
        } else {
            superposition = new EigenstateShootingSuperposition(potential, numpts, xmin, xmax);
        }
      }
      if(!superposition.setCoef(recoef.getArray(), imcoef.getArray())) {
         control.println("Eigenfunction did not converge.");
      }
      superposition.setEnergyScale(energyScale);
      superposition.update(time);
      psiDataset.setCentered(true);
      double dy = control.getDouble("psi range");
      psiPanel.limitAutoscaleY(-dy, dy);
      centeredPhase=false;
      String style = control.getString("style").toLowerCase();
      if((style!=null)&&style.equals("reim")) {
         psiDataset.setMarkerShape(ComplexDataset.RE_IM_CURVE);
         psiPanel.setYLabel("Re(Psi) & Im(Psi)");
      } else if((style!=null)&&style.equals("ampwithphase")) {
         psiDataset.setMarkerShape(ComplexDataset.PHASE_CURVE);
         psiDataset.setCentered(false);
         psiPanel.limitAutoscaleY(0, dy);
         psiPanel.setYLabel("|Psi|");
      } else {
         psiDataset.setMarkerShape(ComplexDataset.PHASE_CURVE);
         psiPanel.setYLabel("|Psi|");
         psiPanel.limitAutoscaleY(-dy/2, dy/2);
         centeredPhase=true;
      }
      superposition.getPsi(psiDataset);
      psiPanel.setMessage("t="+sciFormat.format(time));
      if(dataPanel!=null && showDataPanelTime) {
         dataPanel.setMessage("t="+sciFormat.format(time));
      }
   }

   void normCoef() {
      double[] reCoef = superposition.getReCoef();
      double[] imCoef = superposition.getImCoef();
      double norm = 0;
      for(int i = 0, n = reCoef.length; i<n; i++) {
         norm += (reCoef[i]*reCoef[i]+imCoef[i]*imCoef[i]);
      }
      if(norm==0) { // all coefficients are zero so put system into ground state
         norm = 1;
         reCoef[0] = 1;
         imCoef[0] = 0;
         return;
      }
      norm = 1/Math.sqrt(norm);
      for(int i = 0, n = reCoef.length; i<n; i++) {
         reCoef[i] *= norm;
         imCoef[i] *= norm;
      }
   }

   @Override
public void doStep() {
      time += dt;
      superposition.update(time);
      superposition.getPsi(psiDataset);
      psiPanel.setMessage("t="+sciFormat.format(time));
      if(dataPanel!=null && showDataPanelTime) {
         dataPanel.setMessage("t="+sciFormat.format(time));
      }
      psiPanel.render();
      if(time>=Float.MAX_VALUE) {
         control.calculationDone("Done");
      }
   }

   @Override
public void resetAnimation() {
      super.resetAnimation();
      setValues();
      if(control instanceof EjsControlFrame) {
         ((EjsControlFrame) control).loadDefaultXML();
      }
      initializeAnimation();
   }

   void setValues() {
      control.setValue("numpts", 300);
      control.setValue("psi range", 1);
      control.setValue("dt", 0.1);
      control.setValue("x min", -5);
      control.setValue("x max", 5);
      control.setValue("re coef", intialRe);
      control.setValue("im coef", intialIm);
      control.setValue("V(x)", "x*x/2");
      control.setValue("energy scale", 1);
      control.setValue("time format", "0.00");
      control.setValue("shooting tolerance", 1.0e-4);
      control.setValue("style", "ampwithphase");
      centeredPhase=false;
      control.setValue("hide frame", false);
      control.setValue("psi title", "Harmonic Oscillator");
      control.setValue("data title", "");
   }

   /**
    * Returns an XML.ObjectLoader to save and load data for this program.
    *
    * @return the object loader
    */
   public static XML.ObjectLoader getLoader() {
      return new QMSuperpositionLoader();
   }

   @Override
public void propertyChange(PropertyChangeEvent evt) {
      boolean running = isRunning();
      if(running) {
         stopAnimation();
      }
      initializeAnimation();
      if(running) {
         startAnimation();
      }
   }

   /**
    * Switches to another graphical user interface.
    */
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
         QMSuperpositionWRApp app = new QMSuperpositionWRApp();
         QMSuperpositionStyleControl c = new QMSuperpositionStyleControl(app, null);
         c.getMainFrame().setDefaultCloseOperation(closeOperation);
         for (int i = 0, n = listeners.length; i < n; i++) {
           if (listeners[i].getClass().getName().equals("org.opensourcephysics.tools.Launcher$FrameCloser")) {
             c.getMainFrame().addWindowListener(listeners[i]);
           }
         }
         c.loadXML(xml,true);
         app.customize();
         if(c.getString("style").toLowerCase().equals("reim")){
           c.getControl("checkBox").setProperty("selected", "false");
         }else{
           c.getControl("checkBox").setProperty("selected", "true"); 
         }
         System.gc();
         OSPRuntime.disableAllDrawing=false;
         GUIUtils.repaintOSPFrames();
       }
     };
     Thread t = new Thread(runner);
     t.start();
   }

   void customize() {
      OSPFrame f = getMainFrame();
      if(f==null || !f.isDisplayable()) return;
      JMenu menu = f.getMenu("Display");
      JMenuItem item = new JMenuItem("Switch GUI");
      item.addActionListener(new ActionListener() {

         @Override
		public void actionPerformed(ActionEvent e) {
            switchGUI();
         }
      });
      menu.add(item);
      addChildFrame(psiFrame);
      addChildFrame(dataFrame);
   }


   /**
    * Starts the program and loads an optional XML file.
    * @param args String[]
    */
   public static void main(String[] args) {
      final QMSuperpositionApp app = new QMSuperpositionApp();
      AnimationControl.createApp(app,args);
      app.customize();
   }
}
