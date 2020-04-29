package test;

import org.opensourcephysics.display.DrawingFrame;
import org.opensourcephysics.display.PlottingPanel;
import org.opensourcephysics.display2d.ArrayData;
import org.opensourcephysics.display2d.BinaryLattice;


public class BinaryLatticeTest {
	int numpts=128;
	ArrayData griddata = new ArrayData(numpts, numpts, 3);
  BinaryLattice lattice;
  int[][] spinData;
  int size = 16;

	PlottingPanel plottingPanel = new PlottingPanel("x", "t", null);
	DrawingFrame drawingFrame = new DrawingFrame(plottingPanel);


	BinaryLatticeTest() {
    spinData = new int[size][size];
    lattice = new BinaryLattice(size, size);
    randomizeCells();
		plottingPanel.addDrawable(lattice);
		plottingPanel.setAutoscaleX(true);
		plottingPanel.setAutoscaleY(true);
		griddata.setScale(0, numpts, 0,numpts);
		drawingFrame.setVisible(true);
		plottingPanel.repaint();
	}
	
  /**
   * Custom randomizes the spins
   */
  private void randomizeCells() {
     for(int i = 0; i<size; i++) {
        for(int j = 0; j<size; j++) {
           if(j < i) {//Math.random()>0.5) {
              spinData[i][j] = 1;
           } else {
              spinData[i][j] = -1;
           }
        }
     }
     lattice.setBlock(0, 0, spinData);
  }
	


	public static void main(String[] args) {
		new BinaryLatticeTest();
	}

}
