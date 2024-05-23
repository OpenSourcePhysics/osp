package davidson.qm;
import org.opensourcephysics.ejs.control.*;
import org.opensourcephysics.display.*;
import org.opensourcephysics.display.GUIUtils;
import javax.swing.border.EtchedBorder;

/**
 * An EJS control object for the QMSuperpositionApp.
 * @author Wolfgang Christian
 * @version 1.0
 */
public class QMWignerControl extends EjsControlFrame{
	java.awt.Container cont=null;
	QMSuperpositionWignerApp model;

   public QMWignerControl( QMSuperpositionWignerApp model,String[] args ){
    super(model, "name=controlFrame;title=Wigner Function;location=400,0;layout=border;exit=true; visible=false");
    this.model = model;
    addTarget("control", this);
    addTarget("model", model);
    DrawingPanel p=model.wigner.wignerFrame.getDrawingPanel();
	  addObject (p, "Panel", "name=drawingPanel; parent=controlFrame; position=center");
	  //cont = (java.awt.Container) getElement ("drawingPanel").getComponent ();
	  cont=this.getMainFrame();
	  model.wigner.wignerFrame.dispose();
    add ("Panel", "name=controlPanel; parent=controlFrame; layout=border; position=south");
    add ("Panel", "name=buttonPanel;position=west;parent=controlPanel;layout=flow");
    add("Button", "parent=buttonPanel;tooltip=Start and stop time evolution.;image=/org/opensourcephysics/resources/controls/images/play.gif; action=control.runAnimation();name=runButton");
    add("Button", "parent=buttonPanel;tooltip=Step simulation;image=/org/opensourcephysics/resources/controls/images/step.gif; action=control.stepAnimation()");
    add("Button", "parent=buttonPanel; tooltip=Reset simulation;image=/org/opensourcephysics/resources/controls/images/reset.gif; action=control.resetAnimation()");

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
   // getMainFrame().pack();
    getMainFrame().doLayout();
    getMainFrame().setSize(400, 500);
    GUIUtils.showDrawingAndTableFrames();
   }
   
   protected void customize() {
	    add("Panel", "name=tPanel;parent=controlPanel;position=east;layout=flow");
	    add("Label", "position=west; parent=tPanel;text= t = ;horizontalAlignment=right;tooltip=Time");
	    add("NumberField", "parent=tPanel;variable=time;action=model.setTime();format=0.0000;size=50,22;tooltip=Time");
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
