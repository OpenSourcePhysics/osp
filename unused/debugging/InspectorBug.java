package debugging;
import org.opensourcephysics.controls.OSPControlTable;
import org.opensourcephysics.frames.PlotFrame;
import java.awt.*;
import javax.swing.JFrame;


/**
 * Inspector displays a graph.
 */
public class InspectorBug {

   PlotFrame plot = new PlotFrame("r", "U(r)", "Newtonian Effective Potential");
   OSPControlTable controlTable = new OSPControlTable();
   double E = -0.5, L = 0.5, r = 1.0, M = 1;
   volatile boolean newInspectorData = false;  //set when a user changes data within the inspector using a mouse or keyboard action


   /**
    * Creates an inspector for the given model.
    *
    * @param app ClassicalApp
    */
   public InspectorBug() {
      controlTable.setPreferredSize(new Dimension(100, 100));
      controlTable.setDecimalFormat("#0.0000");
      controlTable.setEditable(false);
      controlTable.setValue("r", r);
      controlTable.setValue("E", E);
      controlTable.setValue("L", L);
      controlTable.setValue("dr/dt", 0);
      controlTable.setValue("rate", 0);
      plot.setJMenuBar(null);
      plot.setSize(500, 300);
      controlTable.setBackground(Color.BLUE);
      plot.getContentPane().add(controlTable, BorderLayout.WEST); //WC:  Bug when adding to content panel
   }

   
   public void show() {
      plot.setVisible(true);
      plot.toFront();
   }

   public static void main(String[] args) {
	   InspectorBug app = new InspectorBug();
      app.controlTable.setEditable(true);
      app.plot.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      app.show();
   }
}
