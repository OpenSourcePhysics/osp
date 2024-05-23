package test;

import org.opensourcephysics.display.DrawingFrame;
import org.opensourcephysics.display.PlottingPanel;
import org.opensourcephysics.display2d.ArrayData;
import org.opensourcephysics.display2d.ComplexCarpet;

public class BandedSampleTest {
	ComplexCarpet carpet = new ComplexCarpet(null);
	PlottingPanel plottingPanel = new PlottingPanel("x", "t", null);
	DrawingFrame drawingFrame = new DrawingFrame(plottingPanel);

	int numpts=128;
	int numdt=64;
	ArrayData griddata = new ArrayData(numpts, numdt, 3);

	BandedSampleTest() {
		plottingPanel.addDrawable(carpet);
		plottingPanel.setAutoscaleX(true);
		plottingPanel.setAutoscaleY(true);
		griddata.setScale(0, numpts, 0,numdt);
    carpet.setAutoscaleZ(false, 1);
    carpet.setGridData(griddata);
    carpet.clearData();
    drawingFrame.setVisible(true);
    new Thread(new Runnable() {
  		@Override
		public void run() {
  	      initCarpet();
          plottingPanel.repaint();    	
		}
    }).start();
	}
	
	void initCarpet() {
	   double[][] row = new double[3][numpts];
		 for(int i=0; i<numdt/2; i++) {
			 for(int j=0; j<numpts; j++) {
         row[0][j] = Math.random();
         row[1][j] = 0;
         row[2][j] = 1;
			 }
			 carpet.setTopRow(row);
			 plottingPanel.render();
		 }
	}


	public static void main(String[] args) {
		new BandedSampleTest();
	}

}
