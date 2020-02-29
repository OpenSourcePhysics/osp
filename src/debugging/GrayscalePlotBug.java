package debugging;

import org.opensourcephysics.display.DrawingFrame;
import org.opensourcephysics.display.PlottingPanel;
import org.opensourcephysics.display2d.ArrayData;
import org.opensourcephysics.display2d.GrayscalePlot;

public class GrayscalePlotBug {
	int numpts=16;
	ArrayData griddata = new ArrayData(numpts, numpts, 3);
	GrayscalePlot grayscalePlot = new GrayscalePlot(griddata);
	PlottingPanel plottingPanel = new PlottingPanel("x", "t", null);
	DrawingFrame drawingFrame = new DrawingFrame(plottingPanel);


	GrayscalePlotBug() {
		griddata.setScale( -1.0, 1.0, 0, 1.0);
		grayscalePlot.setAutoscaleZ(true, 0, 1);
		plottingPanel.addDrawable(grayscalePlot);
		plottingPanel.setAutoscaleX(true);
		plottingPanel.setAutoscaleY(true);
		griddata.setScale(0, numpts, 0,numpts);
    drawingFrame.setVisible(true);
		setRandomVals();
    plottingPanel.repaint();
	}
	
	public void setRandomVals() {
		double[][][] data=griddata.getData();
		double[][] dataR = data[0];
		double[][] dataG = data[1];
		double[][] dataB = data[2];
		for(int i=0; i<numpts; i++ ) {
			for(int j=0; j<numpts; j++ ) {
				dataR[i][j]=255*Math.random();
				dataG[i][j]=255*Math.random();
				dataB[i][j]=255*Math.random();
			}
		}
		grayscalePlot.update();
	}
	


	public static void main(String[] args) {
		new GrayscalePlotBug();
	}

}
