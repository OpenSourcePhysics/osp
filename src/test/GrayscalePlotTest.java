package test;

import java.awt.Graphics;

import org.opensourcephysics.display.DrawingFrame;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.display.PlottingPanel;
import org.opensourcephysics.display2d.ArrayData;
import org.opensourcephysics.display2d.GrayscalePlot;

public class GrayscalePlotTest {
	int numpts=128;
	ArrayData griddata = new ArrayData(numpts, numpts, 3);
	GrayscalePlot grayscalePlot = new GrayscalePlot(griddata) {
		@Override
		public void draw(DrawingPanel panel, Graphics g) {
			setRandomVals();
			super.draw(panel, g);
		}
	};
	PlottingPanel plottingPanel = new PlottingPanel("x", "t", null);
	DrawingFrame drawingFrame = new DrawingFrame(plottingPanel);


	GrayscalePlotTest() {
		griddata.setScale( -1.0, 1.0, 0, 1.0);
		grayscalePlot.setAutoscaleZ(true, 0, 1);
		plottingPanel.addDrawable(grayscalePlot);
		plottingPanel.setAutoscaleX(true);
		plottingPanel.setAutoscaleY(true);
		griddata.setScale(0, numpts, 0,numpts);
		drawingFrame.setVisible(true);
		plottingPanel.repaint();
	}
	
	public void setRandomVals() {
		System.out.println("GSPlotTest.setRandomVals");
		double[][][] data=griddata.getData();
		double[][] dataR = data[0];
//		double[][] dataG = data[1];
//		double[][] dataB = data[2];
		double r = Math.random();
		for(int i=0; i<numpts; i++ ) {
			for(int j=0; j<numpts; j++ ) {
//				dataR[i][j]=255*Math.random();
//				dataG[i][j]=255*Math.random();
//				dataB[i][j]=255*Math.random();
				dataR[i][j]=(r > 0.7 ? 255.0 * i/ numpts : r > 0.5 ? 255.0 * j /numpts : 255.0 * i/ numpts * j /numpts);
				// green and blue are not used in this -- just amplitude (R)
			}
		}
		grayscalePlot.update();
	}
	


	public static void main(String[] args) {
		new GrayscalePlotTest();
	}

}
