package davidson.gr;
import org.opensourcephysics.ejs.control.EjsControlFrame;
import org.opensourcephysics.display.DrawingPanel;
import java.awt.event.WindowAdapter;
import org.opensourcephysics.display.GUIUtils;
import java.awt.event.WindowEvent;
import javax.swing.border.EtchedBorder;
import java.awt.Color;

public class ClassicalWRApp extends EjsControlFrame {

   ClassicalApp model;
   DrawingPanel drawingPanel;

   public ClassicalWRApp(ClassicalApp model, String[] args) {
      super(model,
            "name=controlFrame;title=Newtonian Particle;location=400,0;size=350,450;layout=border;exit=true;visible=false");
      this.model = model;
      this.drawingPanel = model.plottingPanel;
      model.drawingFrame.setDrawingPanel(null);
      model.drawingFrame.dispose();     // don't need this frame any more
      addTarget("control", this);
      addObject(drawingPanel, "Panel", "name=drawingPanel; parent=controlFrame; position=center");
      // create a panel for the control objects
      add("Panel", "name=controlPanel; parent=controlFrame; layout=border; position=south");
      ((javax.swing.JPanel) getElement("controlPanel").getComponent()).setBorder(new EtchedBorder());
      add("Panel", "name=buttonPanel;position=south;parent=controlPanel;layout=flow");
      add("Button", "parent=buttonPanel; text=Run; name=runButton; action=control.runSimulation");
      add("Button", "parent=buttonPanel; text=Step; action=control.stepSimulation");
      add("Button", "parent=buttonPanel; text=Reset; action=control.resetSimulation");
      add("CheckBox",
          "parent=buttonPanel;variable=showInspector;text=U(r);selected=false;action=control.showInspector;");
      getMainFrame().setAnimated(true);
      model.setControl(this);
      createDefaultParticle();
      loadXML(args);
      getMainFrame().pack();
      addPropertyChangeListener(model); // loading an XML data file will a fire property change event
      getMainFrame().addWindowListener(new WindowAdapter() {

         @Override
		public final void windowClosing(WindowEvent e) {
            ClassicalWRApp.this.model.stopSimulation();
            getControl("runButton").setProperty("text", "Start");
         }
      });
      if(!org.opensourcephysics.display.OSPRuntime.appletMode) {
    	  getMainFrame().setVisible(true);
      }
      model.inspector = new ClassicalInspector(model, model.defaultTrajectory);
      model.inspector.enableInteraction(getBoolean("editable inspector"));
      model.inspector.addPropertyChangeListener(model);
      model.inspector.plot.addWindowListener(new WindowAdapter() {
         @Override
		public void windowClosing(WindowEvent e) {
            setValue("showInspector", false);
         }
         @Override
		public void windowOpened(WindowEvent e) {
            setValue("showInspector", true);
         }
      });
   }

   /**
    * Renders the drawing panel.
    */
   @Override
public void render() {
      drawingPanel.render();
   }

   public void showInspector() {
      if(getBoolean("showInspector")) {
         model.defaultTrajectory.color = Color.MAGENTA;
         model.plottingPanel.repaint();
         model.inspector.show();
      } else {
         model.inspector.plot.setVisible(false);
      }
   }
   
   /**
    * Clears the current XML default.
    */
   @Override
public void clearDefaultXML() {
 	  if(xmlDefault==null || model==null) return;
       xmlDefault = null;
       clearItem.setEnabled(false);
       resetSimulation();
       GUIUtils.repaintOSPFrames();
   } 

   public void resetSimulation() {
      model.stopSimulation();
      getControl("runButton").setProperty("text", "Start");
      model.reset();
      createDefaultParticle();
      loadDefaultXML();
      if(getBoolean("showInspector")&&(model.defaultTrajectory!=null)&&(model.inspector!=null)) {
         model.inspector.show();
      }
      model.plottingPanel.repaint();
   }

   void createDefaultParticle(){
     model.createDefaultParticle();
   }

   /**
    * Stops the animation and prints a message.
    * @param message String
    */
   @Override
public void calculationDone(String message) {
     model.stopSimulation();
     getControl("runButton").setProperty("text", "Start");
     super.calculationDone(message);
   }


   public void stepSimulation() {
      model.stopSimulation();
      if (model.time >= getDouble("maximum time")) {
        return;
      }
      getControl("runButton").setProperty("text", "Start");
      model.stepAnimation();
      GUIUtils.repaintAnimatedFrames();
   }

   /**
    *  Switches the text on the run button
    */
   public void runSimulation() {
     if (model.time >= getDouble("maximum time")) {
       return;
     }
      if(model.isRunning()) {
         model.stopSimulation();
         getControl("runButton").setProperty("text", "Start");
      } else {
         getControl("runButton").setProperty("text", "Stop");
         model.startSimulation();
      }
   }

   /**
    * Starts the Java application.
    * @param args  command line parameters
    */
   public static void main(String[] args) {
      new ClassicalWRApp(new ClassicalApp(), args);
   }
}
