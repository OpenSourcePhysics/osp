package debugging;

import org.opensourcephysics.controls.AbstractAnimation;
import org.opensourcephysics.controls.SimulationControl;
import org.opensourcephysics.display.DrawingFrame;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.display2d.BinaryLattice;

public class UnsupportedTransformBug {

	DrawingPanel drawingPanel = new DrawingPanel();
	DrawingFrame drawingFrame = new DrawingFrame("Draw Lattice", drawingPanel);
	BinaryLattice lattice;
	int[][] spinData;
	int size = 32;

	UnsupportedTransformBug() {
		spinData = new int[size][size];
		lattice = new BinaryLattice(size, size);
		drawingPanel.setBuffered(true);
		drawingPanel.addDrawable(lattice);
		drawingPanel.setAutoscaleX(true);
		drawingPanel.setAutoscaleY(true);
		drawingPanel.setSquareAspect(true);
		randomizeCells();
		drawingPanel.setSize(500, 600);
		drawingFrame.setVisible(true);
	}
	
	   
	   /**
	    * Randomizes the cells
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
	      drawingPanel.invalidateImage();
	      drawingPanel.repaint();
	   }


	public static void main(String[] args) {
		new UnsupportedTransformBug();

	}

}
