/*
 * Physics proof-of-concept examples.
 *
 * Copyright (c) 2001  W. Christian.
 */
package debugging.physicsapps;

/**
 * Computer Simulation Methods: Ising Model
 * Copyright:    Copyright (c) 2001
 * @author Wolfgang Christian
 * @version 1.0
 */
import java.text.DecimalFormat;
import org.opensourcephysics.display.*;
import org.opensourcephysics.display2d.*;
import org.opensourcephysics.controls.*;
import debugging.applets.AbstractEmbeddableAnimation;

/**
 * The Ising model.
 */
public class IsingApp extends AbstractEmbeddableAnimation implements Animation {

   DrawingPanel drawingPanel = new DrawingPanel();
   DrawingFrame drawingFrame = new DrawingFrame(drawingPanel);
   PlottingPanel magPanel = new PlottingPanel("Time", "Magnetization", "<M> =");
   DrawingFrame magFrame = new DrawingFrame(magPanel);
   PlottingPanel enPanel = new PlottingPanel("Time", "Energy", "<E> =");
   DrawingFrame enFrame = new DrawingFrame(enPanel);
   Stripchart enData = new Stripchart(10, 100);
   Stripchart magData = new Stripchart(10, 100);
   DecimalFormat format = new DecimalFormat("0.00E0");
   BinaryLattice lattice;
   int[][] spinData;
   int size = 32;
   int J = 1;
   double E;
   int M;
   int flipsPerStep = 1;
   double T, H = 0;
   double newH;
   int time_counter = 0;
   boolean fieldChanged = false;

   /**
    * Constructor IsingApp
    *
    */
   public IsingApp() {
      spinData = new int[size][size];
      lattice = new BinaryLattice(size, size);
      drawingPanel.setBuffered(true);
      drawingPanel.addDrawable(lattice);
      drawingPanel.setAutoscaleX(true);
      drawingPanel.setAutoscaleY(true);
      drawingPanel.setSquareAspect(true);
      enData.setConnected(true);
      enData.setMarkerShape(Dataset.NO_MARKER);
      enPanel.addDrawable(enData);
      enFrame.setTitle("Ising Energy");
      magData.setConnected(true);
      magData.setMarkerShape(Dataset.NO_MARKER);
      magPanel.addDrawable(magData);
      magFrame.setTitle("Ising Magnetization");
   }

   /**
    * Sets the Control for this model.
    * @param control
    */
   public void setControl(Control control) {
      stopAnimation();
      this.control = control;
      if(control==null) {
         return;
      }
      initMyControl();
   }

   /**
    * Initializes this model and its control.
    *
    * Invoked after the control is set.
    */
   protected void initMyControl() {
      control.setValue("grid size", size);
      control.setValue("T", T);
      control.setValue("H", H);
      initializeAnimation(); // initialize the Ising model
      randomizeCells();      // start with random spins
      resetAnimation();      // make sure this model is in the default state
   }

   /**
    * Resets the calculation.
    */
   public void resetAnimation() {
      stopAnimation();
      control.clearMessages();
      drawingPanel.invalidateImage();
      drawingPanel.repaint();
      enPanel.repaint();
      magPanel.repaint();
   }

   /**
    * Method randomize
    *
    */
   public void randomize() {
      boolean isRunning = animationThread!=null;
      stopAnimation();
      randomizeCells();
      if(isRunning) {
         startAnimation();
      } else {
         enData.clear();
         magData.clear();
         time_counter = 0;
         drawingPanel.invalidateImage();
         drawingPanel.repaint();
         enPanel.repaint();
         magPanel.repaint();
      }
   };

   /**
    * Custom randomizes the spins
    */
   private void randomizeCells() {
      for(int i = 0; i<size; i++) {
         for(int j = 0; j<size; j++) {
            if(Math.random()>0.5) {
               spinData[i][j] = 1;
            } else {
               spinData[i][j] = -1;
            }
         }
      }
      lattice.setBlock(0, 0, spinData);
      getME();
   }

   /**
    * Set the parameters in the calculation.
    */
   public void initializeAnimation() {
      stopAnimation();
      int newSize = control.getInt("grid size");
      if(newSize!=size) {
         size = newSize;
         spinData = new int[size][size];
         lattice = new BinaryLattice(size, size);
         drawingPanel.clear();
         drawingPanel.addDrawable(lattice);
         randomizeCells();
      }
      flipsPerStep = size*size;
      if(T<0) {
         System.out.println("\nTemperature is negative, automatically negated!");
         T *= -1;
      }
      T = control.getDouble("T");
      H = control.getDouble("H");
      time_counter = 0;
      getME();
      enData.clear();
      magData.clear();
      enData.append(time_counter/10.0, E);
      magData.append(time_counter/10.0, M);
      double ave = org.opensourcephysics.numerics.Util.computeAverage(magData.getYPoints(), 0, magData.getIndex());
      magPanel.setTitle("<M> = "+format.format((float) ave));
      ave = org.opensourcephysics.numerics.Util.computeAverage(enData.getYPoints(), 0, enData.getIndex());
      enPanel.setTitle("<E> = "+format.format((float) ave));
      enPanel.repaint();
      magPanel.repaint();
      drawingPanel.invalidateImage();
      drawingPanel.repaint();
   }

   /**
    * Calculate the total energy and magnetization by summing over all spin states.
    */
   synchronized void getME() {
      newH = H; // force the two values of H to be equal since we are about to recalculte M and E.
      fieldChanged = false;
      int nn_sum = 0;
      M = 0;
      for(int i = 0; i<size; i++) {
         for(int j = 0; j<size; j++) {
            M += spinData[i][j];
            if(spinData[i][(j+1)%size]==spinData[i][j]) {
               nn_sum++;
            } else {
               nn_sum--;
            }
            if(spinData[(i+1)%size][j]==spinData[i][j]) {
               nn_sum++;
            } else {
               nn_sum--;
            }
         }
      }
      E = -J*nn_sum-H*M;
   }

   /**
    * Adjust the energy to account for changes in the external field.
    */
   synchronized void adjustE() {
      E -= (newH-H)*M;
      H = newH;
      fieldChanged = false;
   }

   private boolean flip() {
      int x = (int) (Math.random()*size);
      int y = (int) (Math.random()*size);
      int delta_M = -2*spinData[x][y];
      int delta_nn_sum = spinData[(x-1+size)%size][y]+spinData[(x+1)%size][y]+spinData[x][(y-1+size)%size]
                         +spinData[x][(y+1)%size];
      delta_nn_sum = -2*spinData[x][y]*delta_nn_sum;
      double delta_E = -J*delta_nn_sum-H*delta_M;
      if((delta_E<=0)||(Math.random()<Math.exp(-delta_E/T))) {
         spinData[x][y] *= -1;
         M += delta_M;
         E += delta_E;
         return true;
      } else {
         return false;
      }
   }

   /**
    * Steps the ising model forward in time.
    *
    */
   public void stepIsingModel() {
      if(fieldChanged) {
         this.adjustE();
      }
      for(int i = 0; i<flipsPerStep; i++) {
         flip();
      }
      time_counter++;
      lattice.setBlock(0, 0, spinData);
      if(fieldChanged) {
         this.adjustE();
      }
      enData.append(time_counter/10.0, E);
      magData.append(time_counter/10.0, M);
      double ave = org.opensourcephysics.numerics.Util.computeAverage(magData.getYPoints(), 0, magData.getIndex());
      magPanel.setTitle("<M> = "+format.format((float) ave));
      ave = org.opensourcephysics.numerics.Util.computeAverage(enData.getYPoints(), 0, enData.getIndex());
      enPanel.setTitle("<E> = "+format.format((float) ave));
   }

   /**
    *  Performs the animation in response to timer events.
    *
    * @param  evt
    */
   protected void doStep() {
      stepIsingModel();
      drawingPanel.render();
      enPanel.render();
      magPanel.render();
   }

   /**
    * Start the Java application.
    * @param args  command line parameters
    */
   public static void main(String[] args) {
      Animation model = new IsingApp();
      AnimationControl control = new AnimationControl(model);
      control.addButton("randomize", "Randomize"); // optional custom action
      model.setControl(control);
   }
}
