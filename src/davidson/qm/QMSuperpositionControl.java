package davidson.qm;
import org.opensourcephysics.ejs.control.*;
import org.opensourcephysics.display.DrawingFrame;
import org.opensourcephysics.display.GUIUtils;
import javax.swing.border.EtchedBorder;

/**
 * An EJS control object for the QMSuperpositionApp.
 * @author Wolfgang Christian
 * @version 1.0
 */
public class QMSuperpositionControl extends EjsControlFrame{
   QMSuperpositionApp model;

   public QMSuperpositionControl( QMSuperpositionApp model,String[] args ){
    super(model, "name=controlFrame;title=QM Position Space Wave Function;location=400,0;layout=border;exit=true; visible=false");
    this.model = model;
    addTarget("control", this);
    addTarget("model", model);
    if (model.dataPanel==null){
       addObject (model.psiPanel, "Panel", "name=drawingPanel; parent=controlFrame; position=center");
       model.psiFrame.setDrawingPanel(null);
       model.psiFrame.dispose();
    }else{
       addObject (model.dataPanel, "Panel", "name=drawingPanel; parent=controlFrame; position=center");
       model.dataFrame.dispose();
       if(model.dataFrame instanceof DrawingFrame){
         ( (DrawingFrame) model.dataFrame).setDrawingPanel(null);
       }
    }
    add ("Panel", "name=controlPanel; parent=controlFrame; layout=border;position=south");
    add ("Panel", "name=buttonPanel;position=west;parent=controlPanel;layout=flow");
    //add ("Button", "parent=buttonPanel; text=Start; action=control.runAnimation();name=runButton");
    //add ("Button", "parent=buttonPanel; text=Step; action=control.stepAnimation()");
    //add ("Button", "parent=buttonPanel; text=Reset; action=control.resetAnimation()");
    add("Button", "parent=buttonPanel;tooltip=Start and stop time evolution.;image=/org/opensourcephysics/resources/controls/images/play.gif; action=control.runAnimation();name=runButton");
    add("Button", "parent=buttonPanel;tooltip=Step simulation;image=/org/opensourcephysics/resources/controls/images/step.gif; action=control.stepAnimation();name=stepButton");
    add("Button", "parent=buttonPanel; tooltip=Reset simulation;image=/org/opensourcephysics/resources/controls/images/reset.gif; action=control.resetAnimation();name=resetButton");

    ((javax.swing.JPanel) getElement("controlPanel").getComponent()).setBorder(new EtchedBorder() );
    customize();
    model.setControl(this);
    loadXML(args);
    //model.initializeAnimation();
    java.awt.Container cont=(java.awt.Container) getElement("controlFrame").getComponent();
    if(!org.opensourcephysics.display.OSPRuntime.appletMode){
      cont.setVisible(true);
    }
    addPropertyChangeListener(model);
    getMainFrame().pack();
    getMainFrame().doLayout();
    GUIUtils.showDrawingAndTableFrames();
   }
   
   /**
    * Override this method to customize the EjsSimulationControl.
    */
   protected void customize() {	  
   }

   /**
    * the runAnimation switches the text on the run button
    */
   public void resetAnimation() {
     model.resetAnimation();
     getControl("runButton").setProperty("image", "/org/opensourcephysics/resources/controls/images/play.gif");//setProperty("text", "Start");
     GUIUtils.showDrawingAndTableFrames();
   }

   public void stepAnimation() {
     model.stopAnimation();
     getControl("runButton").setProperty("image", "/org/opensourcephysics/resources/controls/images/play.gif");//setProperty("text", "Start");
     model.stepAnimation();
     GUIUtils.repaintAnimatedFrames();
   }

   /**
    * the runAnimation switches the text on the run button
    */
   public void runAnimation(){
      if (model.isRunning()){
         model.stopAnimation();
         getControl("runButton").setProperty("image", "/org/opensourcephysics/resources/controls/images/play.gif");//setProperty("text", "Start");
      } else{
         getControl("runButton").setProperty("image", "/org/opensourcephysics/resources/controls/images/pause.gif");//setProperty("text", "Stop");
         model.startAnimation();
      }
   }


}
