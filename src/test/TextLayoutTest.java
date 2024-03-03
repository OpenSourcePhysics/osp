package test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JTextField;

import org.opensourcephysics.display.DrawingFrame;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.display.PlottingPanel;
import org.opensourcephysics.display.OSPRuntime.TextLayout;
import org.opensourcephysics.display2d.ArrayData;
import org.opensourcephysics.display2d.GrayscalePlot;

public class TextLayoutTest {

	public static Font textLayoutFont = new JTextField().getFont().deriveFont(0, 16);

	int numpts=128;
	ArrayData griddata = new ArrayData(numpts, numpts, 3);
	GrayscalePlot grayscalePlot = new GrayscalePlot(griddata) {
		@Override
		public void draw(DrawingPanel panel, Graphics g) {
			
			setRandomVals();
			super.draw(panel, g);
			g.setColor(Color.blue);
			String str = "" + textLayoutFont.getSize2D();
			TextLayout tl = new TextLayout(str, textLayoutFont);
			System.out.println("TLT.tl " + tl.getBounds());
			System.out.println("TLT.fs " + textLayoutFont.getStringBounds(str, ((Graphics2D)g).getFontRenderContext()));
			float w = (float) tl.getBounds().getWidth();
			float h = (float) tl.getBounds().getHeight();
			h = 0;
			System.out.println("TLT.draw w=" + w + " h=" + h);
			tl.draw(g, (103-w/2) , (112 - h/2));

		}
	};
	PlottingPanel plottingPanel = new PlottingPanel("x", "t", null);
	DrawingFrame drawingFrame = new DrawingFrame(plottingPanel);


	TextLayoutTest() {
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
		double[][][] data=griddata.getData();
		double[][] dataR = data[0];
		double r = Math.random();
		for(int i=0; i<numpts; i++ ) {
			for(int j=0; j<numpts; j++ ) {
				dataR[i][j]=(r > 0.7 ? 255.0 * i/ numpts : r > 0.5 ? 255.0 * j /numpts : 255.0 * i/ numpts * j /numpts);
			}
		}
		grayscalePlot.update();
	}
	


	public static void main(String[] args) {
		//textLayoutFont = textLayoutFont.deriveFont(Font.PLAIN, 12f);
		new TextLayoutTest();
	}

}
